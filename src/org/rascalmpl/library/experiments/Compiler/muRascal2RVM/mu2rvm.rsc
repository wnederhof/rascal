module experiments::Compiler::muRascal2RVM::mu2rvm

import IO;
import Type;
import List;
import ListRelation;
import Node;
import Message;
import String;
import ToString;
import experiments::Compiler::RVM::AST;

import experiments::Compiler::muRascal::Syntax;
import experiments::Compiler::muRascal::AST;
//import experiments::Compiler::muRascal::Implode;

//import experiments::Compiler::Rascal2muRascal::RascalModule;
//import experiments::Compiler::Rascal2muRascal::RascalExpression;

import experiments::Compiler::Rascal2muRascal::TypeUtils;
import experiments::Compiler::Rascal2muRascal::TypeReifier;
import experiments::Compiler::muRascal2RVM::ToplevelType;
import experiments::Compiler::muRascal2RVM::PeepHole;
import experiments::Compiler::muRascal2RVM::StackValidator;


alias INS = list[Instruction];

// Unique label generator

private int nlabel = -1;
private str nextLabel() { nlabel += 1; return "L<nlabel>"; }

private str functionScope = "";					// scope name of current function, used to distinguish local and non-local variables
private str surroundingFunctionScope = "";		// scope name of surrounding function

public void setFunctionScope(str scopeId){
	functionScope = scopeId;
}

private map[str,int] nlocal = ();						// number of local per scope

private map[str,str] scopeIn = ();						// scope nesting

int get_nlocals() = nlocal[functionScope];		

void set_nlocals(int n) {
	nlocal[functionScope] = n;
}

// Map names of <fuid, pos> pairs to local variable names ; Note this info could also be collected in Rascal2muRascal

private map[int, str] localNames = ();

// Systematic label generation related to loops

str mkContinue(str loopname) = "CONTINUE_<loopname>";
str mkBreak(str loopname) = "BREAK_<loopname>";
str mkFail(str loopname) = "FAIL_<loopname>";
str mkElse(str branchname) = "ELSE_<branchname>";

// Exception handling: labels to mark the start and end of 'try', 'catch' and 'finally' blocks 
str mkTryFrom(str label) = "TRY_FROM_<label>";
str mkTryTo(str label) = "TRY_TO_<label>";
str mkCatchFrom(str label) = "CATCH_FROM_<label>";
str mkCatchTo(str label) = "CATCH_TO_<label>";
str mkFinallyFrom(str label) = "FINALLY_FROM_<label>";
str mkFinallyTo(str label) = "FINALLY_TO_<label>";

// Manage locals

int newLocal() {
    n = nlocal[functionScope];
    nlocal[functionScope] = n + 1;
    return n;
}

int newLocal(str fuid) {
    n = nlocal[fuid];
    nlocal[fuid] = n + 1;
    return n;
}

// Manage temporaries

map[tuple[str,str],int] temporaries = ();
str asUnwrappedThrown(str name) = name + "_unwrapped";

int getTmp(str name, str fuid){
   if(temporaries[<name,fuid>]?)
   		return temporaries[<name,fuid>];
   n = newLocal(fuid);
   temporaries[<name,fuid>] = n;
   return n;		
}

// Does an expression produce a value? (needed for cleaning up the stack)

//bool producesValue(muLab(_)) = false;

bool producesValue(muWhile(str label, MuExp cond, list[MuExp] body)) = false;

bool producesValue(muBreak(_)) = false;
bool producesValue(muContinue(_)) = false;
bool producesValue(muFail(_)) = false;
bool producesValue(muFailReturn()) = false;

bool producesValue(muReturn0()) = false;
bool producesValue(muGuard(_)) = false;
//bool producesValue(muYield0()) = false;
//bool producesValue(muExhaust()) = false;

bool producesValue(muNext1(MuExp coro)) = false;
default bool producesValue(MuExp exp) = true;

// Management needed to compute exception tables

// An EEntry's handler is defined for ranges 
// this is needed to inline 'finally' blocks, which may be defined in different 'try' scopes, 
// into 'try', 'catch' and 'finally' blocks
alias EEntry = tuple[lrel[str,str] ranges, Symbol \type, str \catch, MuExp \finally];

// Stack of 'try' blocks (needed as 'try' blocks may be nested)
list[EEntry] tryBlocks = [];
list[EEntry] finallyBlocks = [];

// Functions to manage the stack of 'try' blocks
void enterTry(str from, str to, Symbol \type, str \catch, MuExp \finally) {
	tryBlocks = <[<from, to>], \type, \catch, \finally> + tryBlocks;
	finallyBlocks = <[<from, to>], \type, \catch, \finally> + finallyBlocks;
}
void leaveTry() {
	tryBlocks = tail(tryBlocks);
}
void leaveFinally() {
	finallyBlocks = tail(finallyBlocks);
}

// Get the label of a top 'try' block
EEntry topTry() = top(tryBlocks);

// 'Catch' blocks may also throw an exception, which must be handled by 'catch' blocks of surrounding 'try' block
list[EEntry] catchAsPartOfTryBlocks = [];

void enterCatchAsPartOfTryBlock(str from, str to, Symbol \type, str \catch, MuExp \finally) {
	catchAsPartOfTryBlocks = <[<from, to>], \type, \catch, \finally> + catchAsPartOfTryBlocks;
}
void leaveCatchAsPartOfTryBlocks() {
	catchAsPartOfTryBlocks = tail(catchAsPartOfTryBlocks);
}

EEntry topCatchAsPartOfTryBlocks() = top(catchAsPartOfTryBlocks);


// Instruction block of all the 'catch' blocks within a function body in the same order in which they appear in the code
list[INS] catchBlocks = [[]];
int currentCatchBlock = 0;

INS finallyBlock = [];

// As we use label names to mark try blocks (excluding 'catch' clauses)
list[EEntry] exceptionTable = [];

// Specific to delimited continuations (experimental)
public map[str,Declaration] shiftClosures = ();

private int shiftCounter = -1;

int getShiftCounter() {
    shiftCounter += 1;
    return shiftCounter;
}

void resetShiftCounter() {
    shiftCounter = -1;
}

/*********************************************************************/
/*      Translate a muRascal module                                  */
/*********************************************************************/

// Translate a muRascal module

RVMProgram mu2rvm(muModule(str module_name, 
						   map[str,str] tags,
						   set[Message] messages, 
						   list[str] imports,
						   list[str] extends, 
						   map[str,Symbol] types,  
						   map[Symbol, Production] symbol_definitions,
                           list[MuFunction] functions, list[MuVariable] variables, list[MuExp] initializations, 
                           int nlocals_in_initializations,
                           map[str,int] resolver,
                           lrel[str name, Symbol funType, str scope, list[str] ofunctions, list[str] oconstructors] overloaded_functions, 
                           map[Symbol, Production] grammar, 
                           rel[str,str] importGraph,
                           loc src), 
                  bool listing=false){
 
  if(any(m <- messages, error(_,_) := m)){
    return errorRVMProgram(module_name, messages, src);
  }
 
  main_fun = getUID(module_name,[],"MAIN",2);
  module_init_fun = getUID(module_name,[],"#<module_name>_init",2);
  ftype = Symbol::func(Symbol::\value(),[Symbol::\list(Symbol::\value())]);
  fun_names = { fun.qname | MuFunction fun <- functions };
  if(main_fun notin fun_names) {
  	 main_fun = getFUID(module_name,"main",ftype,0);
  	 module_init_fun = getFUID(module_name,"#<module_name>_init",ftype,0);
  }
 
  funMap = ();
  nlabel = -1;
  nlocal =   ( fun.qname : fun.nlocals | MuFunction fun <- functions ) 
           + ( module_init_fun : 2 + size(variables) + nlocals_in_initializations); 	// Initialization function, 2 for arguments
  temporaries = ();
  
  // Specific to delimited continuations (experimental)
  resetShiftCounter();
  shiftClosures = ();
  
  // Due to nesting, pre-compute positions of temporaries
  visit(<initializations,functions>) {
      case muTmp(str id, str fuid): getTmp(id,fuid);
      case muTmpRef(str id, str fuid): getTmp(id,fuid);
      case muAssignTmp(str id, str fuid, _): getTmp(id,fuid);
  }
    
  println("mu2rvm: Compiling module <module_name>");
  
  for(fun <- functions) {
    scopeIn[fun.qname] = fun.scopeIn;
  }
 
  for(fun <- functions){
    functionScope = fun.qname;
    surroundingFunctionScope = fun.scopeIn;
    localNames = ();
    exceptionTable = [];
    catchBlocks = [[]];
    
    // Append catch blocks to the end of the function body code
    // code = tr(fun.body) + [ *catchBlock | INS catchBlock <- catchBlocks ];
    
    code = tr(fun.body);
    //if(!contains(fun.qname, "_testsuite")){
    	code = peephole(code);
    //}
    
    catchBlockCode = [ *catchBlock | INS catchBlock <- catchBlocks ];
    
    code = code /*+ [LABEL("FAIL_<fun.uqname>"), FAILRETURN()]*/ + catchBlockCode;
    
    // Debugging exception handling
    // println("FUNCTION BODY:");
    // for(ins <- code) {
    //	 println("	<ins>");
    // }
     //println("EXCEPTION TABLE:");
     //for(entry <- exceptionTable) {
    	// println("	<entry>");
     //}
    
     lrel[str from, str to, Symbol \type, str target, int fromSP] exceptions = 
    	[ <range.from, range.to, entry.\type, entry.\catch, 0>
    	| tuple[lrel[str,str] ranges, Symbol \type, str \catch, MuExp _] entry <- exceptionTable, 									 
    	  tuple[str from, str to] range <- entry.ranges
    	];
  
    <maxStack, exceptions> = validate(fun.src, code, exceptions);
    required_frame_size = nlocal[functionScope] + maxStack; // estimate_stack_size(fun.body);
    
    funMap += (fun is muCoroutine) ? (fun.qname : COROUTINE(fun.qname, 
                                                            fun.uqname, 
                                                            fun.scopeIn, 
                                                            fun.nformals, 
                                                            nlocal[functionScope], 
                                                            localNames, 
                                                            fun.refs, 
                                                            fun.src, 
                                                            required_frame_size, 
                                                            code, 
                                                            exceptions))
    							   : (fun.qname : FUNCTION(fun.qname, 
    							   						   fun.uqname, 
    							   						   fun.ftype, 
    							   						   fun.scopeIn, 
    							   						   fun.nformals, 
    							   						   nlocal[functionScope], 
    							   						   localNames, 
    							   						   fun.isVarArgs, 
    							   						   fun.isPublic,
    							   						   "default" in fun.modifiers,
    							   						   fun.src, 
    							   						   required_frame_size, 
    							   						   fun.isConcreteArg,
    							   						   fun.abstractFingerprint,
    							   						   fun.concreteFingerprint,
    							   						   code, 
    							   						   exceptions));
  
  	if(listing){
  		println("===================== <fun.qname>");
  		iprintln(fun);
  		println("--------------------- <fun.qname>");
  		iprintln(funMap[fun.qname]);
  	}
  }
  
  functionScope = module_init_fun;
  code = trvoidblock(initializations); // compute code first since it may generate new locals!
  <maxSP, dummy_exceptions> = validate(|init:///|, code, []);
  funMap += ( module_init_fun : FUNCTION(module_init_fun, "init", ftype, "" /*in the root*/, 2, nlocal[module_init_fun], (), false, true, false, src, maxSP + nlocal[module_init_fun],
  										 false, 0, 0,
  								    [*code, 
  								     LOADCON(true),
  								     RETURN1(1),
  								     HALT()
  								    ],
  								    []));
 
  if(listing){
  	println("===================== INIT: (nlocals_in_initializations = <nlocals_in_initializations>):");
  	iprintln(initializations);
  	println("--------------------- INIT");
  	iprintln(funMap[module_init_fun]);
  }
  
  main_testsuite = getUID(module_name,[],"TESTSUITE",1);
  module_init_testsuite = getUID(module_name,[],"#module_init_testsuite",1);
  if(!funMap[main_testsuite]?) { 						
  	 main_testsuite = getFUID(module_name,"testsuite",ftype,0);
  	 module_init_testsuite = getFUID(module_name,"#module_init_testsuite",ftype,0);
  }
  
  // Specific to delimited continuations (experimental)
  funMap = funMap + shiftClosures;
  
  res = rvm(module_name, tags, messages, imports, extends, types, symbol_definitions, funMap, [], resolver, overloaded_functions, importGraph, src);
  return res;
}

/*********************************************************************/
/*      Translate lists of muRascal expressions                      */
/*********************************************************************/


INS tr(list[MuExp] exps) = [ *tr(exp) | exp <- exps ];

INS tr_and_pop(muBlock([])) = [];

default INS tr_and_pop(MuExp exp) = producesValue(exp) ? [*tr(exp), POP()] : tr(exp);

INS trblock(list[MuExp] exps) {
  if(size(exps) == 0){
     return [LOADCON(666)]; // TODO: throw "Non void block cannot be empty";
  }
  ins = [*tr_and_pop(exp) | exp <- exps[0..-1]];
  ins += tr(exps[-1]);
  if(!producesValue(exps[-1])){
  	ins += LOADCON(666);
  }
  return ins;
}

//default INS trblock(MuExp exp) = tr(exp);

INS trvoidblock(list[MuExp] exps){
  if(size(exps) == 0)
     return [];
  ins = [*tr_and_pop(exp) | exp <- exps];
  return ins;
}

INS tr(muBlock([MuExp exp])) = tr(exp);
default INS tr(muBlock(list[MuExp] exps)) = trblock(exps);


/*********************************************************************/
/*      Translate a single muRascal expression                       */
/*********************************************************************/

// Literals and type constants

INS tr(muBool(bool b)) = [LOADBOOL(b)];

INS tr(muInt(int n)) = [LOADINT(n)];

/*default*/ INS tr(muCon(value c)) =
	[LOADCON(c)];	

INS tr(muTypeCon(Symbol sym)) = [LOADTYPE(sym)];

// muRascal functions

INS tr(muFun1(str fuid)) = [LOADFUN(fuid)];
INS tr(muFun2(str fuid, str scopeIn)) = [LOAD_NESTED_FUN(fuid, scopeIn)];

// Rascal functions

INS tr(muOFun(str fuid)) = [ LOADOFUN(fuid) ];

INS tr(muConstr(str fuid)) = [LOADCONSTR(fuid)];

// Variables and assignment

INS tr(muVar(str id, str fuid, int pos)) {
   
    if(fuid == functionScope){
       localNames[pos] = id;
       return [ LOADLOC(pos) ];
    } else {
       return [ LOADVAR(fuid, pos) ];
    }
}

INS tr(muLoc(str id, int pos)) { localNames[pos] = id; return [LOADLOC(pos)];}

INS tr(muResetLocs(list[int] positions)) { return [RESETLOCS(positions)];}

INS tr(muTmp(str id,str fuid)) = [fuid == functionScope ? LOADLOC(getTmp(id,fuid)) : LOADVAR(fuid,getTmp(id,fuid))];

INS tr(muLocKwp(str name)) = [ LOADLOCKWP(name) ];
INS tr(muVarKwp(str fuid, str name)) = [ fuid == functionScope ? LOADLOCKWP(name) : LOADVARKWP(fuid, name) ];

INS tr(muLocDeref(str name, int pos)) = [ LOADLOCDEREF(pos) ];
INS tr(muVarDeref(str name, str fuid, int pos)) = [ fuid == functionScope ? LOADLOCDEREF(pos) : LOADVARDEREF(fuid, pos) ];

INS tr(muLocRef(str name, int pos)) = [ LOADLOCREF(pos) ];
INS tr(muVarRef(str name, str fuid, int pos)) = [ fuid == functionScope ? LOADLOCREF(pos) : LOADVARREF(fuid, pos) ];
INS tr(muTmpRef(str name, str fuid)) = [ fuid == functionScope ? LOADLOCREF(getTmp(name,fuid)) : LOADVARREF(fuid,getTmp(name,fuid)) ];

INS tr(muAssignLocDeref(str id, int pos, MuExp exp)) = [ *tr(exp), STORELOCDEREF(pos) ];
INS tr(muAssignVarDeref(str id, str fuid, int pos, MuExp exp)) = [ *tr(exp), fuid == functionScope ? STORELOCDEREF(pos) : STOREVARDEREF(fuid, pos) ];

INS tr(muAssign(str id, str fuid, int pos, MuExp exp)) { 
     if(fuid == functionScope){
        localNames[pos] = id; 
        return [ *tr(exp), STORELOC(pos) ];
     } else {
        return [*tr(exp), STOREVAR(fuid, pos)];
     }
}     
INS tr(muAssignLoc(str id, int pos, MuExp exp)) { 
    localNames[pos] = id;
    return [*tr(exp), STORELOC(pos) ];
}
INS tr(muAssignTmp(str id, str fuid, MuExp exp)) = [*tr(exp), fuid == functionScope ? STORELOC(getTmp(id,fuid)) : STOREVAR(fuid,getTmp(id,fuid)) ];

INS tr(muAssignLocKwp(str name, MuExp exp)) = [ *tr(exp), STORELOCKWP(name) ];
INS tr(muAssignKwp(str fuid, str name, MuExp exp)) = [ *tr(exp), fuid == functionScope ? STORELOCKWP(name) : STOREVARKWP(fuid,name) ];

// Calls

// Constructor

INS tr(muCallConstr(str fuid, list[MuExp] args)) = [ *tr(args), CALLCONSTR(fuid, size(args)) ];

// muRascal functions

INS tr(muCall(muFun1(str fuid), list[MuExp] args)) = [*tr(args), CALL(fuid, size(args))];
INS tr(muCall(muConstr(str fuid), list[MuExp] args)) = [*tr(args), CALLCONSTR(fuid, size(args))];
default INS tr(muCall(MuExp fun, list[MuExp] args)) = [*tr(args), *tr(fun), CALLDYN(size(args))];

// Partial application of muRascal functions

INS tr(muApply(muFun1(str fuid), [])) = [ LOADFUN(fuid) ];
INS tr(muApply(muFun1(str fuid), list[MuExp] args)) = [ *tr(args), APPLY(fuid, size(args)) ];
INS tr(muApply(muConstr(str fuid), list[MuExp] args)) { throw "Partial application is not supported for constructor calls!"; }
INS tr(muApply(muFun2(str fuid, str scopeIn), [])) = [ LOAD_NESTED_FUN(fuid, scopeIn) ];
default INS tr(muApply(MuExp fun, list[MuExp] args)) = [ *tr(args), *tr(fun), APPLYDYN(size(args)) ];

// Rascal functions

INS tr(muOCall3(muOFun(str fuid), list[MuExp] args, loc src)) = [*tr(args), OCALL(fuid, size(args), src)];
INS tr(muOCall4(MuExp fun, Symbol types, list[MuExp] args, loc src)) 
	= [ *tr(args),
	    *tr(fun), 
		OCALLDYN(types, size(args), src)];
		
// Visit
INS tr(muVisit(bool direction, bool fixedpoint, bool progress, bool rebuild, MuExp descriptor, MuExp phi, MuExp subject, MuExp refHasMatch, MuExp refBeenChanged, MuExp refLeaveVisit, MuExp refBegin, MuExp refEnd))
	= [ *tr(phi),
	    *tr(subject),
	    *tr(refHasMatch),
	    *tr(refBeenChanged),
	    *tr(refLeaveVisit),
	    *tr(refBegin),
	    *tr(refEnd),
	    *tr(descriptor),
	    VISIT(direction, fixedpoint, progress, rebuild)
	  ];


// Calls to Rascal primitives

INS tr(muCallPrim3("println", list[MuExp] args, loc src)) = [*tr(args), PRINTLN(size(args))];
INS tr(muCallPrim3("subtype", list[MuExp] args, loc src)) = [*tr(args), SUBTYPE()];
INS tr(muCallPrim3("typeOf", list[MuExp] args, loc src)) = [*tr(args), TYPEOF()];

default INS tr(muCallPrim3(str name, list[MuExp] args, loc src)) = (name == "println") ? [*tr(args), PRINTLN(size(args))] : [*tr(args), CALLPRIM(name, size(args), src)];

// Calls to MuRascal primitives that are directly translated to RVM instructions

INS tr(muCallMuPrim("println", list[MuExp] args)) = [*tr(args), PRINTLN(size(args))];
INS tr(muCallMuPrim("subscript_array_mint", list[MuExp] args)) = [*tr(args), SUBSCRIPTARRAY()];
INS tr(muCallMuPrim("subscript_list_mint", list[MuExp] args)) = [*tr(args), SUBSCRIPTLIST()];
INS tr(muCallMuPrim("less_mint_mint", list[MuExp] args)) = [*tr(args), LESSINT()];
INS tr(muCallMuPrim("greater_equal_mint_mint", list[MuExp] args)) = [*tr(args), GREATEREQUALINT()];
INS tr(muCallMuPrim("addition_mint_mint", list[MuExp] args)) = [*tr(args), ADDINT()];
INS tr(muCallMuPrim("subtraction_mint_mint", list[MuExp] args)) = [*tr(args), SUBTRACTINT()];
INS tr(muCallMuPrim("and_mbool_mbool", list[MuExp] args)) = [*tr(args), ANDBOOL()];
INS tr(muCallMuPrim("check_arg_type_and_copy", [muCon(int pos1), muTypeCon(Symbol tp), muCon(int pos2)])) = [CHECKARGTYPEANDCOPY(pos1, tp, pos2)];

default INS tr(muCallMuPrim(str name, list[MuExp] args)) = [*tr(args), CALLMUPRIM(name, size(args))];

default INS tr(muCallJava(str name, str class, Symbol parameterTypes, Symbol keywordTypes, int reflect, list[MuExp] args)) = 
	[ *tr(args), CALLJAVA(name, class, parameterTypes, keywordTypes, reflect) ];

// Return

INS tr(muReturn0()) = [RETURN0()];
INS tr(muReturn1(MuExp exp)) {
	if(muTmp(_,_) := exp) {
		inlineMuFinally();
		return [*finallyBlock, *tr(exp), RETURN1(1)];
	}
	return [*tr(exp), RETURN1(1)];
}
INS tr(muReturn2(MuExp exp, list[MuExp] exps))
	= [*tr(exp), *tr(exps), RETURN1(size(exps) + 1)];

INS tr(muFailReturn()) = [ FAILRETURN() ];

INS tr(muFilterReturn()) = [ FILTERRETURN() ];

// Coroutines

INS tr(muCreate1(muFun1(str fuid))) = [ CREATE(fuid, 0) ];
INS tr(muCreate1(MuExp exp)) = [ *tr(exp), CREATEDYN(0) ];
INS tr(muCreate2(muFun1(str fuid), list[MuExp] args)) = [ *tr(args), CREATE(fuid, size(args)) ];
INS tr(muCreate2(MuExp coro, list[MuExp] args)) = [ *tr(args), *tr(coro),  CREATEDYN(size(args)) ];  // order! 

// Delimited continuations (experimental)
// INS tr(muCreate(MuExp exp)) = tr(muReset(exp));
// INS tr(muCreate(MuExp coro, list[MuExp] args)) = tr(muReset(muApply(coro,args)));  // order!

//INS tr(muContVar(str fuid)) = [ LOADCONT(fuid) ];
//INS tr(muReset(MuExp fun)) = [ *tr(fun), RESET() ];
//INS tr(muShift(MuExp body)) {
//    str fuid = functionScope + "/shift_<getShiftCounter()>(1)";
//    prevFunctionScope = functionScope;
//    functionScope = fuid;
//    shiftClosures += ( fuid : FUNCTION(fuid, Symbol::func(Symbol::\value(),[Symbol::\value()]), functionScope, 1, 1, false, |unknown:///|, estimate_stack_size(body), 
//										 false, 0, 0,
//                                       [ *tr(visit(body) { case muContVar(prevFunctionScope) => muContVar(fuid) }), RETURN1(1) ], []) );
//    functionScope = prevFunctionScope; 
//    return [ LOAD_NESTED_FUN(fuid, functionScope), SHIFT() ];
//}

INS tr(muNext1(MuExp coro)) = [*tr(coro), NEXT0()];
INS tr(muNext2(MuExp coro, list[MuExp] args)) = [*tr(args), *tr(coro),  NEXT1()]; // order!

INS tr(muYield0()) = [YIELD0()];
INS tr(muYield1(MuExp exp)) = [*tr(exp), YIELD1(1)];
INS tr(muYield2(MuExp exp, list[MuExp] exps)) = [ *tr(exp), *tr(exps), YIELD1(size(exps) + 1) ];

INS tr(experiments::Compiler::muRascal::AST::muExhaust()) = [ EXHAUST() ];

INS tr(muGuard(MuExp exp)) = [ *tr(exp), GUARD() ];

// Exceptions

INS tr(muThrow(MuExp exp, loc src)) = [ *tr(exp), THROW(src) ];

// Temporary fix for Issue #781
Symbol filterExceptionType(Symbol s) = s == adt("RuntimeException",[]) ? Symbol::\value() : s;

INS tr(muTry(MuExp exp, MuCatch \catch, MuExp \finally)) {
	// Mark the begin and end of the 'try' and 'catch' blocks
	str tryLab = nextLabel();
	str catchLab = nextLabel();
	str finallyLab = nextLabel();
	
	str try_from      = mkTryFrom(tryLab);
	str try_to        = mkTryTo(tryLab);
	str catch_from    = mkCatchFrom(catchLab); // used to jump
	str catch_to      = mkCatchTo(catchLab);   // used to mark the end of a 'catch' block and find a handler catch
	
	// Mark the begin of 'catch' blocks that have to be also translated as part of 'try' blocks 
	str catchAsPartOfTry_from = mkCatchFrom(nextLabel()); // used to find a handler catch
	
	// There might be no surrounding 'try' block for a 'catch' block
	if(!isEmpty(tryBlocks)) {
		// Get the outer 'try' block
		EEntry currentTry = topTry();
		// Enter the current 'catch' block as part of the outer 'try' block
		enterCatchAsPartOfTryBlock(catchAsPartOfTry_from, catch_to, currentTry.\type, currentTry.\catch, currentTry.\finally);
	}
	
	// Enter the current 'try' block; also including a 'finally' block
	enterTry(try_from, try_to, \catch.\type, catch_from, \finally); 
	
	// Translate the 'try' block; inlining 'finally' blocks where necessary
	code = [ LABEL(try_from), *tr(exp) ];
	
	oldFinallyBlocks = finallyBlocks;
	leaveFinally();
	
	// Fill in the 'try' block entry into the current exception table
	currentTry = topTry();
	exceptionTable += <currentTry.ranges, filterExceptionType(currentTry.\type), currentTry.\catch, currentTry.\finally>;
	
	leaveTry();
	
	// Translate the 'finally' block; inlining 'finally' blocks where necessary
	code = code + [ LABEL(try_to), *trMuFinally(\finally) ];
	
	// Translate the 'catch' block; inlining 'finally' blocks where necessary
	// 'Catch' block may also throw an exception, and if it is part of an outer 'try' block,
	// it has to be handled by the 'catch' blocks of the outer 'try' blocks
	
	oldTryBlocks = tryBlocks;
	tryBlocks = catchAsPartOfTryBlocks;
	finallyBlocks = oldFinallyBlocks;
	
	trMuCatch(\catch, catch_from, catchAsPartOfTry_from, catch_to, try_to);
		
	// Restore 'try' block environment
	catchAsPartOfTryBlocks = tryBlocks;
	tryBlocks = oldTryBlocks;
	finallyBlocks = tryBlocks;
	
	// Fill in the 'catch' block entry into the current exception table
	if(!isEmpty(tryBlocks)) {
		EEntry currentCatchAsPartOfTryBlock = topCatchAsPartOfTryBlocks();
		exceptionTable += <currentCatchAsPartOfTryBlock.ranges, filterExceptionType(currentCatchAsPartOfTryBlock.\type), currentCatchAsPartOfTryBlock.\catch, currentCatchAsPartOfTryBlock.\finally>;
		leaveCatchAsPartOfTryBlocks();
	}
	
	return code;
}

void trMuCatch(muCatch(str id, str fuid, Symbol \type, MuExp exp), str from, str fromAsPartOfTryBlock, str to, str jmpto) {
    
	oldCatchBlocks = catchBlocks;
	oldCurrentCatchBlock = currentCatchBlock;
	currentCatchBlock = size(catchBlocks);
	catchBlocks = catchBlocks + [[]];
	catchBlock = [];
	
	str catchAsPartOfTryNewLab = nextLabel();
	str catchAsPartOfTryNew_from = mkCatchFrom(catchAsPartOfTryNewLab);
	str catchAsPartOfTryNew_to = mkCatchTo(catchAsPartOfTryNewLab);
	
	// Copy 'try' block environment of the 'catch' block; needed in case of nested 'catch' blocks
	catchAsPartOfTryBlocks = [ < [<catchAsPartOfTryNew_from, catchAsPartOfTryNew_to>],
								 entry.\type, entry.\catch, entry.\finally > | EEntry entry <- catchAsPartOfTryBlocks ];
	
	if(muBlock([]) := exp) {
		catchBlock = [ LABEL(from), POP(), LABEL(to), JMP(jmpto) ];
	} else {
		catchBlock = [ LABEL(from), 
					   // store a thrown value
					   fuid == functionScope ? STORELOC(getTmp(id,fuid)) : STOREVAR(fuid,getTmp(id,fuid)), POP(),
					   // load a thrown value,
					   fuid == functionScope ? LOADLOC(getTmp(id,fuid))  : LOADVAR(fuid,getTmp(id,fuid)),
					   // unwrap it and store the unwrapped one in a separate local variable 
					   fuid == functionScope ? UNWRAPTHROWNLOC(getTmp(asUnwrappedThrown(id),fuid)) : UNWRAPTHROWNVAR(fuid,getTmp(asUnwrappedThrown(id),fuid)),
					   *tr(exp), LABEL(to), JMP(jmpto) ];
	}
	
	if(!isEmpty(catchBlocks[currentCatchBlock])) {
		catchBlocks[currentCatchBlock] = [ LABEL(catchAsPartOfTryNew_from), *catchBlocks[currentCatchBlock], LABEL(catchAsPartOfTryNew_to) ];
		for(currentCatchAsPartOfTryBlock <- catchAsPartOfTryBlocks) {
			exceptionTable += <currentCatchAsPartOfTryBlock.ranges, filterExceptionType(currentCatchAsPartOfTryBlock.\type), currentCatchAsPartOfTryBlock.\catch, currentCatchAsPartOfTryBlock.\finally>;
		}
	} else {
		catchBlocks = oldCatchBlocks;
	}
	
	currentCatchBlock = oldCurrentCatchBlock;
	
	// 'catchBlock' is always non-empty 
	catchBlocks[currentCatchBlock] = [ LABEL(fromAsPartOfTryBlock), *catchBlocks[currentCatchBlock], *catchBlock ];
		
}

// TODO: Re-think the way empty 'finally' blocks are translated
INS trMuFinally(MuExp \finally) = (muBlock([]) := \finally) ? [ LOADCON(666), POP() ] : tr(\finally);

void inlineMuFinally() {
	
	finallyBlock = [];

	str finallyLab   = nextLabel();
	str finally_from = mkFinallyFrom(finallyLab);
	str finally_to   = mkFinallyTo(finallyLab);
	
	// Stack of 'finally' blocks to be inlined
	list[MuExp] finallyStack = [ entry.\finally | EEntry entry <- finallyBlocks ];
	
	// Make a space (hole) in the current (potentially nested) 'try' blocks to inline a 'finally' block
	if(isEmpty([ \finally | \finally <- finallyStack, !(muBlock([]) := \finally) ])) {
		return;
	}
	tryBlocks = [ <[ *head, <from,finally_from>, <finally_to + "_<size(finallyBlocks) - 1>",to>], 
				   tryBlock.\type, tryBlock.\catch, tryBlock.\finally> | EEntry tryBlock <- tryBlocks, 
				   														 [ *tuple[str,str] head, <from,to> ] := tryBlock.ranges ];
	
	oldTryBlocks = tryBlocks;
	oldCatchAsPartOfTryBlocks = catchAsPartOfTryBlocks;
	oldFinallyBlocks = finallyBlocks;
	oldCurrentCatchBlock = currentCatchBlock;
	oldCatchBlocks = catchBlocks;
	
	// Translate 'finally' blocks as 'try' blocks: mark them with labels
	tryBlocks = [];	
	for(int i <- [0..size(finallyStack)]) {
		// The last 'finally' does not have an outer 'try' block
		if(i < size(finallyStack) - 1) {
			EEntry outerTry = finallyBlocks[i + 1];
			tryBlocks = tryBlocks + [ <[<finally_from, finally_to + "_<i>">], outerTry.\type, outerTry.\catch, outerTry.\finally> ];
		}
	}
	finallyBlocks = tryBlocks;
	catchAsPartOfTryBlocks = [];
	currentCatchBlock = size(catchBlocks);
	catchBlocks = catchBlocks + [[]];
	
	finallyBlock = [ LABEL(finally_from) ];
	for(int i <- [0..size(finallyStack)]) {
		finallyBlock = [ *finallyBlock, *trMuFinally(finallyStack[i]), LABEL(finally_to + "_<i>") ];
		if(i < size(finallyStack) - 1) {
			EEntry currentTry = topTry();
			// Fill in the 'catch' block entry into the current exception table
			exceptionTable += <currentTry.ranges, filterExceptionType(currentTry.\type), currentTry.\catch, currentTry.\finally>;
			leaveTry();
			leaveFinally();
		}
	}
	
	tryBlocks = oldTryBlocks;
	catchAsPartOfTryBlocks = oldCatchAsPartOfTryBlocks;
	finallyBlocks = oldFinallyBlocks;
	if(isEmpty(catchBlocks[currentCatchBlock])) {
		catchBlocks = oldCatchBlocks;
	}
	currentCatchBlock = oldCurrentCatchBlock;
	
}

// Control flow

// If

INS tr(muIfelse(str label, MuExp cond, list[MuExp] thenPart, list[MuExp] elsePart)) {
    if(label == "") {
    	label = nextLabel();
    };
    elseLab = mkElse(label);
    continueLab = mkContinue(label);
    return [ *tr_cond(cond, nextLabel(), mkFail(label), elseLab), 
             *(isEmpty(thenPart) ? [LOADCON(111)] : trblock(thenPart)),
             JMP(continueLab), 
             LABEL(elseLab),
             *(isEmpty(elsePart) ? [LOADCON(222)] : trblock(elsePart)),
             LABEL(continueLab)
           ];
}

// While

INS tr(muWhile(str label, MuExp cond, list[MuExp] body)) {
    if(label == ""){
    	label = nextLabel();
    }
    continueLab = mkContinue(label);
    failLab = mkFail(label);
    breakLab = mkBreak(label);
    return [ *tr_cond(cond, continueLab, failLab, breakLab), 	 					
    		 *trvoidblock(body),			
    		 JMP(continueLab),
    		 LABEL(breakLab)		
    		];
}

INS tr(muBreak(str label)) = [ JMP(mkBreak(label)) ];
INS tr(muContinue(str label)) = [ JMP(mkContinue(label)) ];
INS tr(muFail(str label)) = [ JMP(mkFail(label)) ];


INS tr(muTypeSwitch(MuExp exp, list[MuTypeCase] cases, MuExp defaultExp)){
   defaultLab = nextLabel();
   continueLab = mkContinue(defaultLab);
   labels = [defaultLab | i <- index(toplevelTypes) ];
   caseCode =  [];
	for(cs <- cases){
		caseLab = defaultLab + "_" + cs.name;
		labels[getToplevelType(cs.name)] = caseLab;
		caseCode += [ LABEL(caseLab), *tr(cs.exp), JMP(continueLab) ];
	 };
   caseCode += [LABEL(defaultLab), *tr(defaultExp), JMP(continueLab) ];
   return [ *tr(exp), TYPESWITCH(labels), *caseCode, LABEL(continueLab) ];
}
	
INS tr(muSwitch(MuExp exp, bool useConcreteFingerprint, list[MuCase] cases, MuExp defaultExp, MuExp result)){
   defaultLab = nextLabel();
   continueLab = mkContinue(defaultLab);
   labels = ();
   caseCode =  [];
   for(cs <- cases){
		caseLab = defaultLab + "_<cs.fingerprint>";
		labels[cs.fingerprint] = caseLab;
		caseCode += [ LABEL(caseLab), POP(), *tr(cs.exp), JMP(defaultLab) ];
   }
   INS defaultCode = tr(defaultExp);
   if(defaultCode == []){
   		defaultCode = [LOADCON(666)];
   }
   if(size(cases) > 0){ 
   		caseCode += [LABEL(defaultLab), JMPTRUE(continueLab), *defaultCode, POP() ];
   		return [ LOADCON(false), *tr(exp), SWITCH(labels, defaultLab, useConcreteFingerprint), *caseCode, LABEL(continueLab), *tr(result) ];
   	} else {
   		return [ *tr(exp), POP(), *defaultCode, POP(), *tr(result) ];
   	}	
}

// Multi/One/All/Or outside conditional context
    
INS tr(e:muMulti(MuExp exp)) =
     [ *tr(exp),
       CREATEDYN(0),
       NEXT0()
     ];

INS tr(e:muOne1(MuExp exp)) =
    [ *tr(exp),
       CREATEDYN(0),
       NEXT0()
     ];

// The above list of muExps is exhaustive, no other cases exist

default INS tr(MuExp e) { throw "mu2rvm: Unknown node in the muRascal AST: <e>"; }

/*********************************************************************/
/*      End of muRascal expressions                                  */
/*********************************************************************/


/*********************************************************************/
/*      Translate conditions                                         */
/*********************************************************************/

/*
 * The contract of tr_cond is as follows:
 * - continueLab: continue searching for more solutions for this condition
 *   (is created by the caller, but inserted in the code generated by tr_cond)
 * - failLab: continue searching for more solutions for this condition (multi expressions) or jump to falseLab when no more solutions exist (backtrack-free expressions).
 * - falseLab: location to jump to when no more solutions exist.
 *   (is created by the caller and only jumped to by code generated by tr_cond.)
 *
 * The generated code falls through to subsequent instructions when the condition is true, and jumps to falseLab otherwise.
 */

// muOne: explore one successful evaluation

INS tr_cond(muOne1(MuExp exp), str continueLab, str failLab, str falseLab) =
      [ LABEL(continueLab), LABEL(failLab) ]
    + [ *tr(exp), 
        CREATEDYN(0), 
        NEXT0(), 
        JMPFALSE(falseLab)
      ];

// muMultiL explore all successful evaluations

INS tr_cond(muMulti(MuExp exp), str continueLab, str failLab, str falseLab) {
    co = newLocal();
    return [ *tr(exp),
             CREATEDYN(0),
             STORELOC(co),
             POP(),
             *[ LABEL(continueLab), LABEL(failLab) ],
             LOADLOC(co),
             NEXT0(),
             JMPFALSE(falseLab)
           ];
}

// Specific to delimited continuations (experimental)
//INS tr_cond(muMulti(MuExp exp), str continueLab, str failLab, str falseLab) {
//    co = newLocal();
//    return [ *tr(exp),
//             RESET(),
//             STORELOC(co),
//             POP(),
//             *[ LABEL(continueLab), LABEL(failLab) ],
//             LOADLOC(co),
//             CALL("Library/NEXT(1)",1),
//             JMPFALSE(falseLab),
//             LOADLOC(co),
//             LOADCON("cont"),
//             CALLPRIM("adt_field_access",2),
//             CALLDYN(0),
//             STORELOC(co),
//             POP()
//           ];
//}

default INS tr_cond(MuExp exp, str continueLab, str failLab, str falseLab) 
	= [ JMP(continueLab), LABEL(failLab), JMP(falseLab), LABEL(continueLab), *tr(exp), JMPFALSE(falseLab) ];
    
