package org.rascalmpl.interpreter.result;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.KeywordFormal;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.OptionalComma;
import org.rascalmpl.ast.Signature;
import org.rascalmpl.interpreter.Accumulator;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.TraversalEvaluator;
import org.rascalmpl.interpreter.TraversalEvaluator.CaseBlockList;
import org.rascalmpl.interpreter.TraversalEvaluator.DIRECTION;
import org.rascalmpl.interpreter.TraversalEvaluator.FIXEDPOINT;
import org.rascalmpl.interpreter.TraversalEvaluator.PROGRESS;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.EmptyVariablesEnvironment;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.staticErrors.UnsupportedOperation;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Cases.CaseBlock;
import org.rascalmpl.interpreter.utils.Cases.SugarBlock;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.values.uptr.IRascalValueFactory;
import org.rascalmpl.values.uptr.ITree;

public class ExpandFunction extends CustomNamedFunction {
	protected static final TypeFactory TF = TypeFactory.getInstance();
	protected static final RascalTypeFactory RTF = RascalTypeFactory.getInstance();
	protected static final IRascalValueFactory VF = IRascalValueFactory.getInstance();
	
	private FunctionDeclaration func;
	private boolean varargs;
	private List<String> extraParameters;

	private static org.rascalmpl.ast.Parameters createParameters(ISourceLocation src, org.rascalmpl.ast.Expression signaturePattern, List<org.rascalmpl.ast.Expression> extraParameters) {
		OptionalComma optionalComma = new OptionalComma.Lexical(src, null, ",");
		List<org.rascalmpl.ast.Expression> listOfFormals = new LinkedList<>();
		listOfFormals.add(signaturePattern);
		
		if (extraParameters != null) {
			listOfFormals.addAll(extraParameters);
		}
		
		org.rascalmpl.ast.Formals formals = ASTBuilder.make("Formals", "Default", src, listOfFormals);
		
		org.rascalmpl.ast.KeywordFormals keywordFormals = ASTBuilder.make("KeywordFormals", "Default", src,
				optionalComma, new LinkedList<org.rascalmpl.ast.KeywordFormal>());
		
		return ASTBuilder.make("Parameters", "Default",
				src, formals, keywordFormals);
	}
	
	// This function is used to create a signature out of a sugar function declaration.
	private static Signature createExpandSignature(ISourceLocation src, org.rascalmpl.ast.Type type, org.rascalmpl.ast.Expression signaturePattern, Name name, List<Expression> extraParameters) {
		org.rascalmpl.ast.FunctionModifiers modifiers = ASTBuilder.make("FunctionModifiers", "Modifierlist",
				src, new LinkedList<>());
		
		org.rascalmpl.ast.Signature signature =  
				ASTBuilder.make("Signature", "NoThrows",
						src, modifiers,
						type, name, createParameters(src, signaturePattern, extraParameters));
		return signature;
	}
	
	private static FunctionType createFunctionType(FunctionDeclaration func, IEvaluator<Result<IValue>> eval, Environment env, Name name, List<Expression> extraParameters) {
		return (FunctionType) createExpandSignature(func.getLocation(), func.getTypeRhs(), func.getPatternLhs(), name, extraParameters).typeOf(env, true, eval);
	}
	
	// We don't use keyword formals.
	private static List<KeywordFormal> createFormals(FunctionDeclaration func) {
		return new LinkedList<KeywordFormal>();
	}
	
	public ExpandFunction(IEvaluator<Result<IValue>> eval, FunctionDeclaration func, boolean varargs, Environment env,
			Stack<Accumulator> accumulators) {
		this(func, eval,
				Names.name(func.getName()),
				createFunctionType(func, eval, env, func.getName(), func.hasExtraParameters() ? func.getExtraParameters() : null),
				createFormals(func),
				varargs, false, false,
				env, accumulators);
		this.func = func;
		this.varargs = varargs;

		Type onType = func.getTypeRhs().typeOf(eval.getCurrentEnvt(), true, eval);
		Type onTypeLhs = func.getTypeLhs().typeOf(eval.getCurrentEnvt(), true, eval);
		eval.getCurrentModuleEnvironment().declareAnnotation(onType,
				"unexpandFn", TF.valueType());
		eval.getCurrentModuleEnvironment().declareAnnotation(onTypeLhs,
				"unexpansionFailed", TF.valueType());
		eval.getCurrentModuleEnvironment().declareAnnotation(onType,
				"desugarVariables", TF.listType(TF.mapType(TF.stringType(), TF.valueType())));

		
		assert (!(func instanceof FunctionDeclaration) &&
			    !(func instanceof FunctionDeclaration.SugarExtra));
		
		if (func.hasExtraParameters()) {
			extraParameters = new LinkedList<>();
			for (Expression e : func.getExtraParameters()) {
				if (!(e instanceof Expression.TypedVariable) && !(e instanceof Expression.QualifiedName)) {
					throw new UnsupportedOperation("Every argument after the first must be a (typed) variable.", getAst());
				}
				assert !Names.fullName(e.getQualifiedName()).contains(":");
				extraParameters.add(Names.fullName(e.getQualifiedName()));
			}
		}
	}

	ExpandFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, String name, FunctionType functionType,
			List<KeywordFormal> initializers, boolean varargs, boolean isDefault, boolean isTest, Environment env,
			Stack<Accumulator> accumulators) {
		super(ast, eval, name, functionType, initializers, varargs, isDefault, isTest, env, accumulators);
	}

	@Override
	public ExpandFunction cloneInto(Environment env) {
		// TODO Is this necessary? -- AbstractAST clone = cloneAst();
		FunctionDeclaration clone = (FunctionDeclaration) func.clone();
		ExpandFunction rf = new ExpandFunction(eval, clone, varargs, env, accumulators);
		rf.setPublic(isPublic()); // TODO: should be in constructors
		return rf;
	}
	
	@SuppressWarnings("deprecation")
	private String sugarUUIDAsString(IValue v, String key) {
		if (v.asAnnotatable().hasAnnotation("__SUGAR_UUID")) {
			IValue sugarUuidValue = v.asAnnotatable().getAnnotation("__SUGAR_UUID");
			return ((IString) sugarUuidValue).getValue();
		}
		return null;
	}
	
	private void desugarAndStoreLocalVariableWithUUID(IVarPattern localVariable, Map<String, String> uuidMap) {
		Environment old = eval.getCurrentEnvt();
		IValue myVar = ctx.getCurrentEnvt().getVariable(localVariable.name()).getValue();
		IValue resVal;
		try {
			eval.setCurrentEnvt(new EmptyVariablesEnvironment(old));
			resVal = ctx.getEvaluator().call("desugar", myVar); // TODO get actual name.
		} catch(MatchFailed e) {
			visitAndDesugar(localVariable, uuidMap);
			return;
		} finally {
			eval.unwind(old);
		}
		uuidMap.put(localVariable.name(), sugarUUIDAsString(resVal, localVariable.name()));
		ctx.getCurrentEnvt().storeLocalVariable(localVariable.name(),
				ResultFactory.makeResult(resVal.getType(), resVal, ctx));
	}
	
	private UnexpandFunction createResugarFunction(Map<String, String> uuidMap) {
		Environment old = eval.getCurrentEnvt();
		//Result<IValue> originalNode = eval.getCurrentEnvt().getSimpleVariable("__DESUGAR_ORIGINAL_NODE");
		try {
			// eval.setCurrentEnvt(new EmptyVariablesEnvironment(old));
			System.out.println("Created unexpand function.");
			System.out.println(_actuals[0].getType());
			UnexpandFunction unexpandFn = new UnexpandFunction(eval, func, varargs, eval.getCurrentEnvt(),
					eval.__getAccumulators(), makeResult(_actuals[0]), // TODO
					extraParameters, uuidMap);
			System.out.println(unexpandFn);
			return unexpandFn;
		} finally {
			eval.unwind(old);
		}
	}
	
	private Result<IValue> createSugarUUID() {
		return ResultFactory.makeResult(TF.stringType(), VF.string(String.valueOf(UUID.randomUUID())), ctx);		
	}
	
	private void visitAndDesugar(IVarPattern localVariable, Map<String, String> uuidMap) {
		// TODO ...
		/*List<CaseBlock> cb = new LinkedList<>();
		
		CaseBlock SugarBlock = new SugarBlock(c);
		cb.add(SugarBlock);
		
		CaseBlockList caseBlockList = new CaseBlockList(cases);
		
		new TraversalEvaluator(eval).traverse(localVariable.getValue().getValue(),
				new CaseBlockList(caseBlocks), DIRECTION.TopDown, PROGRESS.Breaking, FIXEDPOINT.No);*/
	}
	
	Result<IValue> makeResult(IValue v) {
		return ResultFactory.makeResult(v.getType(), v, ctx);
	}
	
	@Override
	Result<IValue> run() {
		Map<String, String> uuidMap = new HashMap<String, String>();
		// TODO: REMOVE THE ... FUNCTION LOCAL VARIABLES THINGY!!!!
		List<IVarPattern> vars = func.getPatternLhs().buildMatcher(eval).getVariables();
		for (IVarPattern localVariable : vars) {
			desugarAndStoreLocalVariableWithUUID(localVariable, new HashMap<>());//uuidMap);
		}
		Result<IValue> desugaredValue = func.getPatternRhs().interpret(eval);
		UnexpandFunction unexpandFn = createResugarFunction(uuidMap);
		for (IVarPattern localVariable : vars) {
			if (eval.getCurrentEnvt().getVariable(localVariable.name()).getValue().asAnnotatable().hasAnnotation("unexpandFn")) {
				System.out.println("Has unexpansion function." + eval.getCurrentEnvt().getVariable(localVariable.name()).getValue().asAnnotatable().getAnnotation("unexpandFn"));
			}
		}
		return desugaredValue
				.setAnnotation("unexpandFn", unexpandFn, eval.getCurrentEnvt())
				.setAnnotation("__SUGAR_UUID", createSugarUUID(), eval.getCurrentEnvt());
	}
	
	@Override
	List<Expression> cacheFormals() throws ImplementationError {
		// This function is called before func has been initialized. Thus:
		FunctionDeclaration func = (FunctionDeclaration) ast;
		return cacheFormals(createParameters(ast.getLocation(), func.getPatternLhs(), func.hasExtraParameters() ? func.getExtraParameters() : null));
	}

}
