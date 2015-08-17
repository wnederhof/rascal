package org.rascalmpl.interpreter.result;

import static org.rascalmpl.interpreter.result.ResultFactory.makeResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
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
import org.rascalmpl.interpreter.SubstitutionEvaluator;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.control_exceptions.Failure;
import org.rascalmpl.interpreter.control_exceptions.InterruptException;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.control_exceptions.Return;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.staticErrors.UnguardedFail;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.NonTerminalType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.values.uptr.IRascalValueFactory;
import org.rascalmpl.values.uptr.SymbolAdapter;

public class UnexpandFunction extends CustomNamedFunction {
	protected static final TypeFactory TF = TypeFactory.getInstance();
	protected static final RascalTypeFactory RTF = RascalTypeFactory.getInstance();
	protected static final IRascalValueFactory VF = IRascalValueFactory.getInstance();
	
	private FunctionDeclaration func;
	private boolean varargs;
	private Result<IValue> patternLhsResult;
	private List<String> extraParameters;
	private Map<String, String> uuidMap;
	
	private static org.rascalmpl.ast.Parameters createParameters(ISourceLocation src, org.rascalmpl.ast.Expression signaturePattern) {
		OptionalComma optionalComma = new OptionalComma.Lexical(src, null, ",");
		List<org.rascalmpl.ast.Expression> listOfFormals = new LinkedList<>();
		listOfFormals.add(signaturePattern);
		
		org.rascalmpl.ast.Formals formals = ASTBuilder.make("Formals", "Default", src, listOfFormals);
		
		org.rascalmpl.ast.KeywordFormals keywordFormals = ASTBuilder.make("KeywordFormals", "Default", src,
				optionalComma, new LinkedList<org.rascalmpl.ast.KeywordFormal>());
		
		
		return ASTBuilder.make("Parameters", "Default",
				src, formals, keywordFormals);
	}
	
	// This function is used to create a signature out of a sugar function declaration.
	private static Signature createUnexpandSignature(ISourceLocation src, org.rascalmpl.ast.Type type, org.rascalmpl.ast.Expression signaturePattern, Name name) {
		org.rascalmpl.ast.FunctionModifiers modifiers = ASTBuilder.make("FunctionModifiers", "Modifierlist",
				src, new LinkedList<>());
		
		org.rascalmpl.ast.Signature signature =  
				ASTBuilder.make("Signature", "NoThrows",
						src, modifiers,
						type, name, createParameters(src, signaturePattern));
		return signature;
	}
	
	private static FunctionType createFunctionType(FunctionDeclaration func, IEvaluator<Result<IValue>> eval, Environment env, Name name) {
		return (FunctionType) createUnexpandSignature(func.getLocation(), func.getTypeLhs(), func.getPatternRhs(), name).typeOf(env, true, eval);
	}
	
	// We don't use keyword formals.
	private static List<KeywordFormal> createFormals(FunctionDeclaration func) {
		return new LinkedList<KeywordFormal>();
	}
	
	public UnexpandFunction(IEvaluator<Result<IValue>> eval, FunctionDeclaration func, boolean varargs, Environment env,
			Stack<Accumulator> accumulators, Result<IValue> patternLhsResult,
			List<String> extraParameters, Map<String, String> uuidMap) {
		this(func, eval,
				Names.name(func.getName()),
				createFunctionType(func, eval, env, func.getName()),
				createFormals(func),
				varargs, false, false,
				env, accumulators);
		this.func = func;
		this.varargs = varargs;
		this.patternLhsResult = patternLhsResult;
		this.extraParameters = extraParameters;
		this.uuidMap = uuidMap;
		
		assert (!(func instanceof FunctionDeclaration) &&
			    !(func instanceof FunctionDeclaration.SugarExtra));
	}

	UnexpandFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, String name, FunctionType functionType,
			List<KeywordFormal> initializers, boolean varargs, boolean isDefault, boolean isTest, Environment env,
			Stack<Accumulator> accumulators) {
		super(ast, eval, name, functionType, initializers, varargs, isDefault, isTest, env, accumulators);
	}
	

	@Override
	public UnexpandFunction cloneInto(Environment env) {
		// TODO Is this necessary? 
		FunctionDeclaration clone = (FunctionDeclaration) func.clone();
		UnexpandFunction rf = new UnexpandFunction(eval, clone, varargs, env, accumulators, patternLhsResult, extraParameters, uuidMap);
		rf.setPublic(isPublic()); // TODO: should be in constructors
		return rf;
	}

	@SuppressWarnings({ "deprecation" })
	@Override
	public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
		Result<IValue> result = getMemoizedResult(actuals, keyArgValues);
		if (result != null) {
			return result;
		}

		Environment old = ctx.getCurrentEnvt();
		AbstractAST currentAST = ctx.getCurrentAST();
		AbstractAST oldAST = currentAST;
		Stack<Accumulator> oldAccus = ctx.getAccumulators();

		try {
			if (callTracing) {
				printStartTrace(actuals);
			}
			String label = isAnonymous() ? "Anonymous Function" : name;
			Environment environment = new Environment(declarationEnvironment, ctx.getCurrentEnvt(),
					currentAST != null ? currentAST.getLocation() : null, ast.getLocation(), label);
			ctx.setCurrentEnvt(environment);
			IMatchingResult matcher = func.getPatternRhs().buildMatcher(ctx);
			matcher.initMatch(ResultFactory.makeResult(actualTypes[0], actuals[0], ctx)); 
			ctx.setAccumulators(accumulators);
			ctx.pushEnv();
			while (matcher.hasNext() && matcher.next()) {
				boolean success = true;
				// We match flexibly. UUIDs are optional.
				for (Entry<String, String> entry : uuidMap.entrySet()) {
					IValue val = ctx.getCurrentEnvt().getVariable(entry.getKey()).getValue();
					System.out.println("For annotation of variable: " + entry.getKey());
					if (val.isAnnotatable()) {
						try {
							if (val.getType() instanceof NonTerminalType) {
								IConstructor declaredListType = ((NonTerminalType) val.getType()).getSymbol();
								if (SymbolAdapter.isStarList(declaredListType) ||
										SymbolAdapter.isPlusList(declaredListType)) {
									continue;
								}
							}
							if (!val.asAnnotatable().hasAnnotation("__SUGAR_UUID")) {
								System.out.println("Opposite has no annotation.");
								success = false;
								break;
							} else {
								IValue uuidVal = val.asAnnotatable().getAnnotation("__SUGAR_UUID");
								if (uuidVal instanceof IString) {
									String uuid = ((IString) uuidVal).getValue();
									System.out.println("Other UUID: " + uuid);
									if (!uuid.equals(entry.getValue())) {
										success = false;
										break;
									}
								} else {
									System.out.println("UUID is not a string. (HUH!?!?) Type: " + uuidVal.getType());
									success = false;
									break;
								}
							}
						} catch(FactTypeUseException e) {
							System.out.println("FactTypeUseException.");
							success = false;
							break;
						}
					}
				}
				if (success) {
					return run();
				}
			}
			throw new MatchFailed();
		} finally {
			if (callTracing) {
				callNesting--;
			}
			ctx.setCurrentEnvt(old);
			ctx.setAccumulators(oldAccus);
			ctx.setCurrentAST(oldAST);
		}
	}
	
	
	
	
	
	
	@Override @SuppressWarnings("deprecation")
	Result<IValue> run() {
		// Here comes the cool stuff.
		// Alright, we got: TypeRHS name (patternLhsResult: PatLHS) => TypeLHS (PatRHS)
		// We want to return PatRHS PatLHS <<< patternLhsResult
		
		Result<IValue> x = SubstitutionEvaluator.substitute(func.getPatternLhs(), patternLhsResult.getValue(), eval);
		
		// From the "mock" environment, we extract the variables used on the RHS, but not on the LHS' first parameter.
		if (extraParameters == null) {
			return x;
		}
		
		if (!x.getValue().asAnnotatable().hasAnnotation("desugarVariables")) {
			x = ResultFactory.makeResult(x.getType(), x.getValue().asAnnotatable().setAnnotation("desugarVariables", VF.list()), eval);
		}
		IList desugarVariables = (IList) x.getValue().asAnnotatable().getAnnotation("desugarVariables");
		IMapWriter desugarVariablesMapWriter = VF.mapWriter();
		for (String s : extraParameters) {
			System.out.println("Extra parameter: " + s + ", value: " + getEnv().getVariable(s));
			if (getEnv().getVariables().get(s) != null) {
				System.out.println("This works.");
				desugarVariablesMapWriter.put(VF.string(s), getEnv().getVariable(s).getValue());
			}
		}
		desugarVariables = desugarVariables.append(desugarVariablesMapWriter.done());
		return x.setAnnotation("desugarVariables",
				ResultFactory.makeResult(desugarVariables.getType(), desugarVariables, eval),
				eval.getCurrentEnvt());
	}
	
	@Override
	List<Expression> cacheFormals() throws ImplementationError {
		// This function is called before func has been initialized. Thus:
		FunctionDeclaration func = (FunctionDeclaration) ast;
		return cacheFormals(createParameters(ast.getLocation(), func.getPatternRhs()));
	}
	
}
