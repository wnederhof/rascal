module experiments::Compiler::muRascal::AST

import Prelude;

/*
 * Abstract syntax for muRascal.
 * 
 * Position in the compiler pipeline: Rascal -> muRascal -> RVM
 */

// All information related to one Rascal module

public data MuModule =											
              muModule(str name, list[loc] imports,
              					 map[str,Symbol] types, 
              					 map[Symbol, Production] symbol_definitions,
                                 list[MuFunction] functions, 
                                 list[MuVariable] variables, 
                                 list[MuExp] initialization,
                                 int nlocals_in_initializations,
                                 map[str,int] resolver,
                                 lrel[str,list[str],list[str]] overloaded_functions,
                                 map[Symbol, Production] grammar)
            ;
          
// All information related to a function declaration. This can be a top-level
// function, or a nested or anomyous function inside a top level function. 
         
public data MuFunction =					
                muFunction(str qname, str uqname, Symbol ftype, str scopeIn, int nformals, int nlocals, bool isVarArgs, 
                           loc src, list[str] modifiers, map[str,str] tags,
                           MuExp body)
              | muCoroutine(str qname, str uqname, str scopeIn, int nformals, int nlocals, loc src, list[int] refs, MuExp body)
          ;
          
// A global (module level) variable.
          
public data MuVariable =
            muVariable(str name)
          ;
          
// A declared Rascal type
          
//public data MuType =
//            muType(list[Symbol] symbols)  
//          ;

// All executable Rascal code is tranlated to the following muExps.
          
public data MuExp = 
			// Elementary expressions
			muBool(bool b)										// muRascal Boolean constant
		  | muInt(int n)										// muRascal integer constant
          | muCon(value c)										// Rascal Constant: an arbitrary IValue
            													// Some special cases are handled by preprocessor, see below.
          | muLab(str name)										// Label
          
          | muFun(str fuid)							            // *muRascal function constant: functions at the root
          | muFun(str fuid, str scopeIn)                        // *muRascal function constant: nested functions and closures
          
          | muOFun(str fuid)                                    // *Rascal functions, i.e., overloaded function at the root
          
          | muConstr(str fuid) 									// Constructors
          
          	// Variables
          | muLoc(str name, int pos)							// Local variable, with position in current scope
          | muVar(str name, str fuid, int pos)					// Variable: retrieve its value
          | muTmp(str name, str fuid)							// Temporary variable introduced by front-end
          
          | muLocDeref(str name, int pos) 				        // Call-by-reference: a variable that refers to a value location
          | muVarDeref(str name, str fuid, int pos)
          
          | muLocRef(str name, int pos) 				        // Call-by-reference: expression that returns a value location
          | muVarRef(str name, str fuid, int pos)
          | muTmpRef(str name, str fuid)
          
          // Keyword parameters
          | muLocKwp(str name)                                  // Local keyword parameter
          | muVarKwp(str fuid, str name)                        // Keyword parameter
             
          | muTypeCon(Symbol tp)								// Type constant
          
          // Call/Apply/return    		
          | muCall(MuExp fun, list[MuExp] args)                 // Call a *muRascal function
          | muApply(MuExp fun, list[MuExp] args)                // Partial *muRascal function application
          
          | muOCall(MuExp fun, list[MuExp] args, loc src)       // Call a declared *Rascal function

          | muOCall(MuExp fun, Symbol types,                    // Call a dynamic *Rascal function
          					   list[MuExp] args, loc src)
          
          | muCallConstr(str fuid, list[MuExp] args /*, loc src*/)	// Call a constructor
          
          | muCallPrim(str name, loc src)                       // Call a Rascal primitive function (with empty list of arguments)
          | muCallPrim(str name, list[MuExp] exps, loc src)		// Call a Rascal primitive function
          
          | muCallMuPrim(str name, list[MuExp] exps)			// Call a muRascal primitive function
          | muCallJava(str name, str class, 
          			   Symbol parameterTypes,
          			   Symbol keywordTypes,
          			   int reflect,
          			   list[MuExp] args)						// Call a Java method in given class
 
          | muReturn()											// Return from a function without value
          | muReturn(MuExp exp)									// Return from a function with value
          | muReturn(MuExp exp, list[MuExp] exps)               // Return from a coroutine with multiple values
          
          | muFilterReturn()									// Return for filer statement
              
           // Assignment, If and While
              
          | muAssignLoc(str name, int pos, MuExp exp)			// Assign a value to a local variable
          | muAssign(str name, str fuid, int pos, MuExp exp)	// Assign a value to a variable
          | muAssignTmp(str name, str fuid, MuExp exp)			// Assign to temporary variable introduced by front-end
          
          // Keyword parameters
          | muAssignLocKwp(str name, MuExp exp)
          | muAssignKwp(str fuid, str name, MuExp exp)
          
          | muAssignLocDeref(str name, int pos, MuExp exp)      // Call-by-reference assignment:
          | muAssignVarDeref(str name, str fuid, 
          					 int pos, MuExp exp) 	            // the left-hand side is a variable that refers to a value location
          														
          | muIfelse(str label, MuExp cond,                     // If-then-else expression
          						list[MuExp] thenPart,			
          						list[MuExp] elsePart)
          						 
          | muWhile(str label, MuExp cond, list[MuExp] body)	// While-Do expression
          
          | muTypeSwitch(MuExp exp, list[MuTypeCase] cases, MuExp \default)		// switch over cases for specific type
          
		  | muBreak(str label)									// Break statement
		  | muContinue(str label)								// Continue statement
		  | muFail(str label)									// Fail statement
		  | muFailReturn()										// Failure from function body
          
            // Coroutines
          
          | muCreate(MuExp coro)								// Creates a coroutine instance, no arguments
          | muCreate(MuExp coro, list[MuExp] args)				// Creates a coroutine instance, with arguments
          
          | muNext(MuExp exp)									// Next on coroutine, no arguments
          | muNext(MuExp exp1, list[MuExp] args)				// Next on coroutine, with arguments
          
          | muYield()											// Yield from a coroutine without value
          | muYield(MuExp exp)									// Yield from a coroutine with value
          | muYield(MuExp exp, list[MuExp] exps)                // Yield from a coroutine with multiple values
          
          | muExhaust()                                         // Signal a failure and return from the coroutine disallowing further resumption 

          | muGuard(MuExp exp)                                  // Specifies a condition of suspending a coroutine instance during initialization
          
           // Multi-expressions
          
          | muBlock(list[MuExp] exps)  							// A list of expressions, only last value remains
          | muMulti(MuExp exp)		 							// Expression that can produce multiple values
          | muOne(MuExp exp)                                    // Expression that always produces only the first value
          | muOne(list[MuExp] exps)								// Compute one result for a list of boolean expressions
          | muAll(list[MuExp] exps)								// Compute all results for a list of boolean expressions
          | muOr(list[MuExp] exps)        						// Compute the or of a list of Boolean expressions.
          
          // Exceptions
          
          | muThrow(MuExp exp, loc src)
          
          // Exception handling try/catch
          
          | muTry(MuExp exp, MuCatch \catch, MuExp \finally)
          
          // Delimited continuations (experimental)
          
          | muContVar(str fuid)
          | muReset(MuExp fun)
          | muShift(MuExp exp)
          ;
          
public MuExp muMulti(muOne(MuExp exp)) = muOne(exp);
public MuExp muOne(muMulti(MuExp exp)) = muOne(exp);

anno loc MuExp@\loc;
 
data MuCatch = muCatch(str id, str fuid, Symbol \type, MuExp body);    

data MuTypeCase = muTypeCase(str name, MuExp exp);	  
       	  
// Auxiliary constructors that are removed by the preprocessor: parse tree -> AST.
// They will never be seen by later stages of the compiler.

data Identifier =
				  fvar(str var)
				| ivar(str var)
				| rvar(str var)
				| mvar(str var)
				;

public data Module =
            preMod(str name, list[TypeDeclaration] types, list[Function] functions)
          ;

public data TypeDeclaration = preTypeDecl(str \type);

public data VarDecl = preVarDecl(Identifier id)
                    | preVarDecl(Identifier id, MuExp initializer)
                    ;
                    
public data Guard = preGuard(MuExp exp)
                  | preGuard(list[VarDecl] locals, str sep, MuExp exp)
                  ;

/*
 * The field 'comma' is a work around given the current semantics of implode 
 */
public data Function =				
               preFunction(lrel[str,int] funNames, str name, list[Identifier] formals, 
                           lrel[list[VarDecl] vardecls, str s] locals, list[MuExp] body, bool comma)
             | preCoroutine(lrel[str s,int i] funNames, str name, list[Identifier] formals, 
                            list[Guard] guard, lrel[list[VarDecl] vardecls, str s] locals, list[MuExp] body, bool comma)
          ;


public data MuExp =
              preIntCon(str txt)
            | preStrCon(str txt)  
            | preTypeCon(str txt)
            | preVar(Identifier id)
            | preVar(lrel[str name,int formals] funNames, Identifier id)
            // Specific to delimited continuations (experimental)
            | preContLoc()
            | preContVar(lrel[str,int] funNames)
            | preFunNN(str modName, str name, int nformals)
            | preFunN(lrel[str,int] funNames, str name, int nformals)
            | preList(list[MuExp] exps)
            | preAssignLoc(Identifier id, MuExp exp)
            | preAssign(lrel[str,int] funNames, Identifier id, MuExp exp)
            | preAssignLocList(Identifier id1, Identifier id2, MuExp exp)
            | preIfthen(MuExp cond, list[MuExp] thenPart, bool comma)
            
            | preMuCallPrim(str name)                                // Call a Rascal primitive function (with empty list of arguments)
            | preMuCallPrim(str name, list[MuExp] exps)				// Call a Rascal primitive function
            | preThrow(MuExp exp)
            
            | preAddition(MuExp lhs, MuExp rhs)
            | preSubtraction(MuExp lhs, MuExp rhs)
            | preMultiplication(MuExp lhs, MuExp rhs)
            | preDivision(MuExp lhs, MuExp rhs)
            | preModulo(MuExp lhs, MuExp rhs)
            | prePower(MuExp lhs, MuExp rhs)
                 
            | preLess(MuExp lhs, MuExp rhs)
            | preLessEqual(MuExp lhs, MuExp rhs)
            | preEqual(MuExp lhs, MuExp rhs)
            | preNotEqual(MuExp lhs, MuExp rhs)
            | preGreater(MuExp lhs, MuExp rhs)
            | preGreaterEqual(MuExp lhs, MuExp rhs)
            | preAnd(MuExp lhs, MuExp rhs)
            | preOr(MuExp lhs, MuExp rhs)
       
            | preIs(MuExp exp, str typeName)
            
            | preLocDeref(Identifier id)
            | preVarDeref(lrel[str s,int i] funNames, Identifier id)
            | preLocRef(Identifier id)
            | preVarRef(lrel[str,int] funNames, Identifier id)
            
            | preAssignLocDeref(Identifier id, MuExp exp)
            | preAssignVarDeref(lrel[str,int] funNames, Identifier id, MuExp exp)
            
            | preIfelse(MuExp cond, list[MuExp] thenPart, bool comma1, list[MuExp] elsePart, bool comma2)
            | preWhile(MuExp cond, list[MuExp] body, bool comma)
            | preIfelse(str label, MuExp cond, list[MuExp] thenPart, bool comma1, list[MuExp] elsePart, bool comma2)
            | preWhile(str label, MuExp cond, list[MuExp] body, bool comma)
            | preTypeSwitch(MuExp exp, lrel[MuTypeCase,bool] sepCases, MuExp \default, bool comma)
            | preBlock(list[MuExp] exps, bool comma)
            
            | preSubscript(MuExp arr, MuExp index)
            | preAssignSubscript(MuExp arr, MuExp index, MuExp exp)
           ;
           
public bool isOverloadedFunction(muOFun(str _)) = true;
//public bool isOverloadedFunction(muOFun(str _, str _)) = true;
public default bool isOverloadedFunction(MuExp _) = false;

MuExp muCallPrim(str name) = muCallPrim(name, |unknown:///no-location-available|);
MuExp muCallPrim(str name, list[MuExp] exps) = muCallPrim(name, exps, |unknown:///no-location-available|);


//--------------- constant folding rules ----------------------------------------
// These rules should go to experiments::Compiler::RVM::Interpreter::ConstantFolder.rsc


bool allConstant(list[MuExp] args) { b = isEmpty(args) || all(a <- args, muCon(_) := a); /*println("allConstant: <args> : <b>"); */return b; }

// muBool, muInt?        
          
// Rascal primitives

// Integer addition

MuExp muCallPrim("int_add_int", [muCon(int n1), muCon(int n2)], loc src) = muCon(n1 + n2);

MuExp muCallPrim("int_add_int", [muCallPrim("int_add_int", [MuExp e, muCon(int n1)], loc src1), muCon(int n2)], loc src2) =
      muCallPrim("int_add_int", [e, muCon(n1 + n2)], src2);

MuExp muCallPrim("int_add_int", [muCon(int n1), muCallPrim("int_add_int", [muCon(int n2), MuExp e], loc src1)], loc src2)  =
      muCallPrim("int_add_int", [muCon(n1 + n2), e], src2);
      

// Integer multiplication

MuExp muCallPrim("int_product_int", [muCon(int n1), muCon(int n2)], loc src) = muCon(n1 * n2);

MuExp muCallPrim("int_product_int", [muCallPrim("int_product_int", [MuExp e, muCon(int n1)], loc src1), muCon(int n2)], loc src2) =
      muCallPrim("int_product_int", [e, muCon(n1 * n2)], src2);

MuExp muCallPrim("int_product_int", [muCon(int n1), muCallPrim("int_product_int", [muCon(int n2), MuExp e], loc src1)], loc src2)  =
      muCallPrim("int_product_int", [muCon(n1 * n2), e], src2);

// String concatenation

MuExp muCallPrim("str_add_str", [muCon(str s1), muCon(str s2)], loc src) = muCon(s1 + s2);

MuExp muCallPrim("str_add_str", [muCallPrim("str_add_str", [MuExp e, muCon(str s1)], loc src1), muCon(str s2)], loc src2) =
      muCallPrim("str_add_str", [e, muCon(s1 + s2)], src2);

MuExp muCallPrim("str_add_str", [muCon(str s1), muCallPrim("str_add_str", [muCon(str s2), MuExp e], loc src1)], loc src2)  =
      muCallPrim("str_add_str", [muCon(s1 + s2), e], src2);

// Composite datatypes

MuExp muCallPrim("list_create", list[MuExp] args, loc src) = muCon([a | muCon(a) <- args]) 
      when allConstant(args);

MuExp muCallPrim("set_create", list[MuExp] args, loc src) = muCon({a | muCon(a) <- args}) 
      when allConstant(args);
      
MuExp muCallPrim("map_create", list[MuExp] args, loc src) = muCon((args[i].c : args[i+1].c | int i <- [0, 2 .. size(args)]))
      when allConstant(args);
      
MuExp muCallPrim("tuple_create", [muCon(v1)], loc src) = muCon(<v1>);
MuExp muCallPrim("tuple_create", [muCon(v1), muCon(v2)], loc src) = muCon(<v1, v2>);
MuExp muCallPrim("tuple_create", [muCon(v1), muCon(v2), muCon(v3)], loc src) = muCon(<v1, v2, v3>);
MuExp muCallPrim("tuple_create", [muCon(v1), muCon(v2), muCon(v3), muCon(v4)], loc src) = muCon(<v1, v2, v3, v4>);
MuExp muCallPrim("tuple_create", [muCon(v1), muCon(v2), muCon(v3), muCon(v4), muCon(v5)], loc src) = muCon(<v1, v2, v3, v4, v5>);

MuExp muCallPrim("node_create", [muCon(str name), *MuExp args, muCallMuPrim("make_mmap", [])], loc src) = muCon(makeNode(name, [a | muCon(a) <- args]))  
      when allConstant(args);
      
MuExp muCallPrim("appl_create", [muCon(prod), muCon(args)], loc src) = muCon(makeNode("appl", prod, args));

// muRascal primitives

//MuExp muCallMuPrim(str name, list[MuExp] exps) = x;
