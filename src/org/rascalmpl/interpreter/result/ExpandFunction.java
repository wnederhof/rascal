package org.rascalmpl.interpreter.result;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.staticErrors.UnsupportedOperation;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.values.uptr.IRascalValueFactory;

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
		listOfFormals.add(
				ASTBuilder.makeExp("VariableBecomes", src, Names.toName("__DESUGAR_ORIGINAL_NODE", src), signaturePattern));
		
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
	@Override
	Result<IValue> run() {
		// Return the expanded tree annotated with unexpandFn, declared in the <i>CURRENT</i> environment (very important).
		Result<IValue> patternRhsResult = func.getPatternRhs().interpret((IEvaluator<Result<IValue>>) ctx);
		// For each variable on the RHS, tag it.
		
		IMatchingResult iResult = func.getPatternRhs().getMatcher(ctx);
		iResult.initMatch(patternRhsResult);
		
		Map<String, Result<IValue>> uuidMap = new HashMap<String, Result<IValue>>();
		Map<String, String> uuidMapStrs = new HashMap<String, String>();
		
		for (Entry<String, Result<IValue>> s : eval.getCurrentEnvt().getLocalFunctionVariables().entrySet()) {
			System.out.println("Found Local Variable: " + s.getKey());
			IValue v = s.getValue().getValue();
			if (v.isAnnotatable()) {
				
				if (!v.getType().declaresAnnotation(ctx.getCurrentEnvt().getStore(), "__SUGAR_UUID")) {
					eval.getCurrentEnvt().declareAnnotation(v.getType(), "__SUGAR_UUID", TF.stringType());
				}
				if (!v.asAnnotatable().hasAnnotation("__SUGAR_UUID") || v.asAnnotatable().getAnnotation("__SUGAR_UUID") == null) {
					String uuid = String.valueOf(UUID.randomUUID());
					v = v.asAnnotatable().setAnnotation("__SUGAR_UUID", VF.string(uuid));
				}
				
				System.out.println("Getting annotation for: " + v);
				IString anno = (IString) v.asAnnotatable().getAnnotation("__SUGAR_UUID");
				System.out.println("It is set to: " + anno.getValue());
				String stringVal = anno.getValue();

				uuidMap.put(s.getKey(), ResultFactory.makeResult(v.getType(), v, ctx));
				uuidMapStrs.put(s.getKey(), stringVal);
				// We store variables that are not on the RHS of the function.
				eval.getCurrentEnvt().storeLocalVariable(s.getKey(), 
						ResultFactory.makeResult(v.getType(), v, ctx));
			}
		}
		
		if (iResult.hasNext() && iResult.next()) {
			UnexpandFunction unexpandFn = new UnexpandFunction(eval, func, varargs, eval.getCurrentEnvt(), eval.__getAccumulators(),
					eval.getCurrentEnvt().getSimpleVariable("__DESUGAR_ORIGINAL_NODE"), extraParameters, uuidMapStrs);
			IValue resVal = iResult.substitute(uuidMap).get(0);
			return ResultFactory.makeResult(resVal.getType(),
					resVal.asAnnotatable().setAnnotation("unexpandFn", unexpandFn), ctx);
		}
		throw new RuntimeException("This should never happen.");
	}
	
	@Override
	List<Expression> cacheFormals() throws ImplementationError {
		// This function is called before func has been initialized. Thus:
		FunctionDeclaration func = (FunctionDeclaration) ast;
		return cacheFormals(createParameters(ast.getLocation(), func.getPatternLhs(), func.hasExtraParameters() ? func.getExtraParameters() : null));
	}

}
