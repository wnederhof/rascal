package org.rascalmpl.interpreter.result.sugar;

import static org.rascalmpl.interpreter.result.ResultFactory.makeResult;

import java.util.HashMap;
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
import org.eclipse.imp.pdb.facts.ITuple;
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
import org.rascalmpl.ast.Tag;
import org.rascalmpl.interpreter.Accumulator;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.SubstitutionEvaluator;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.control_exceptions.Failure;
import org.rascalmpl.interpreter.control_exceptions.InterruptException;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.control_exceptions.Return;
import org.rascalmpl.interpreter.env.EmptyVariablesEnvironment;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.matching.QualifiedNamePattern;
import org.rascalmpl.interpreter.matching.TypedVariablePattern;
import org.rascalmpl.interpreter.result.CustomNamedFunction;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.staticErrors.UnguardedFail;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.NonTerminalType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.values.uptr.IRascalValueFactory;
import org.rascalmpl.values.uptr.ITree;
import org.rascalmpl.values.uptr.SymbolAdapter;
import org.rascalmpl.values.uptr.TreeAdapter;

@Deprecated
public class UnexpandFunction extends CustomNamedFunction {
	protected static final TypeFactory TF = TypeFactory.getInstance();
	protected static final RascalTypeFactory RTF = RascalTypeFactory.getInstance();
	protected static final IRascalValueFactory VF = IRascalValueFactory.getInstance();
	
	private FunctionDeclaration func;
	private boolean varargs;
	private Result<IValue> patternLhsResult;
	private List<String> extraParameters;
	private Map<String, String> uuidMap;
	private IMatchingResult _matcher;
	private String defaultFunctionType;
	
	private static org.rascalmpl.ast.Parameters createParameters(ISourceLocation src, org.rascalmpl.ast.Expression signaturePattern, boolean isDefaultFunction) {
		OptionalComma optionalComma = new OptionalComma.Lexical(src, null, ",");
		List<org.rascalmpl.ast.Expression> listOfFormals = new LinkedList<>();
		listOfFormals.add(signaturePattern);
		
		org.rascalmpl.ast.Formals formals = ASTBuilder.make("Formals", "Default", src, listOfFormals);
		
		org.rascalmpl.ast.KeywordFormals keywordFormals = ASTBuilder.make("KeywordFormals", "Default", src,
				optionalComma, createFormals(src, isDefaultFunction));
		
		
		return ASTBuilder.make("Parameters", "Default",
				src, formals, keywordFormals);
	}
	
	// This function is used to create a signature out of a sugar function declaration.
	private static Signature createUnexpandSignature(ISourceLocation src, org.rascalmpl.ast.Type type, org.rascalmpl.ast.Expression signaturePattern, Name name, boolean isDefaultFunction) {
		org.rascalmpl.ast.FunctionModifiers modifiers = ASTBuilder.make("FunctionModifiers", "Modifierlist",
				src, new LinkedList<>());
		
		org.rascalmpl.ast.Signature signature =  
				ASTBuilder.make("Signature", "NoThrows",
						src, modifiers,
						type, name, createParameters(src, signaturePattern, isDefaultFunction));
		return signature;
	}
	
	private static FunctionType createFunctionType(FunctionDeclaration func, IEvaluator<Result<IValue>> eval, Environment env, Name name, boolean isDefaultFunction) {
		return (FunctionType) createUnexpandSignature(func.getLocation(), func.getTypeLhs(), func.getPatternRhs(), name, isDefaultFunction).typeOf(env, true, eval);
	}
	
	// We don't use keyword formals.
	private static List<KeywordFormal> createFormals(ISourceLocation src, boolean isDefaultFunction) {
		if (isDefaultFunction) {
			System.out.println("is default function.");
			LinkedList<KeywordFormal> keywordFormals = new LinkedList<KeywordFormal>();
			keywordFormals.add(ASTBuilder.make("KeywordFormal", src, 
					ASTBuilder.make("Type", "Basic", src,
							ASTBuilder.make("BasicType", "String", src)),
					Names.toName("__resugarType", src),
					ASTBuilder.makeExp("Literal", src,
							ASTBuilder.make("Literal", "String", src, 
									ASTBuilder.make("StringLiteral", "NonInterpolated", src, 
											ASTBuilder.make("StringConstant", "Lexical", src, "\"\""))))));
			return keywordFormals;
		}
		return new LinkedList<KeywordFormal>();
	}
	
	public UnexpandFunction(IEvaluator<Result<IValue>> eval, FunctionDeclaration func, boolean varargs, Environment env,
			Stack<Accumulator> accumulators, Result<IValue> patternLhsResult,
			List<String> extraParameters, Map<String, String> uuidMap, String defaultFunctionType) {
		this(func, eval,
				Names.name(func.getName()),
				createFunctionType(func, eval, env, func.getName(), defaultFunctionType != null),
				createFormals(func.getLocation(), defaultFunctionType != null),
				varargs, false, false,
				env, accumulators);
		this.func = func;
		this.varargs = varargs;
		this.patternLhsResult = patternLhsResult;
		this.extraParameters = extraParameters;
		this.uuidMap = uuidMap;
		this.defaultFunctionType = defaultFunctionType;
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
		UnexpandFunction rf = new UnexpandFunction(eval, clone, varargs, env, accumulators, patternLhsResult, extraParameters, uuidMap, defaultFunctionType);
		rf.setPublic(isPublic()); // TODO: should be in constructors
		return rf;
	}
	
	@Override
	protected List<Expression> cacheFormals() throws ImplementationError {
		// This function is called before func has been initialized. Thus:
		FunctionDeclaration func = (FunctionDeclaration) ast;
		return cacheFormals(createParameters(ast.getLocation(), func.getPatternRhs(), defaultFunctionType != null));
	}
	
	@SuppressWarnings({ "deprecation" })
	@Override
	public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
		System.out.println("Calling.");
		_actuals = actuals;
		try {
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
			
			_matcher = matcher;
			matcher.initMatch(ResultFactory.makeResult(actualTypes[0], actuals[0], ctx)); 
			ctx.setAccumulators(accumulators);
			ctx.pushEnv();
			System.out.println(matcher);
			while (matcher.hasNext() && matcher.next()) {
				// 
				boolean success = true;
				// We match flexibly. UUIDs are optional.
				for (Entry<String, String> entry : uuidMap.entrySet()) {
					// if (1==1) continue;
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
					System.out.println("success.");
				  bindKeywordArgs(keyArgValues);
	              result = run();
	              storeMemoizedResult(actuals,keyArgValues, result);
	              return result;
				}
			}
			/*if (actuals[0] instanceof IConstructor) {
				System.out.println("Matcher: " + matcher);
				System.out.println("Failed to match: " + TreeAdapter.yield((IConstructor) actuals[0]));
				System.out.println("Type: " + actualTypes[0]);
			}*/
			System.out.println("Resugar failed, now fallback.");
			return fallbackResugar();
		} finally {
			if (callTracing) {
				callNesting--;
			}
			ctx.setCurrentEnvt(old);
			ctx.setAccumulators(oldAccus);
			ctx.setCurrentAST(oldAST);
		}
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	
	/*************************/
	@SuppressWarnings("deprecation")
	private void resugar(String localVariableName) {

	}
	
	private Result<IValue> fallbackResugar() {
		if (defaultFunctionType != null) {
			throw new MatchFailed();
		}
		for (Tag t : func.getTags().getTags()) {
			String name = Names.name(t.getName());
			if (name.equals("fallbackSugar")) {
				String contents = "" + t.getContents();
				contents = contents.substring(1, contents.length() - 1);
				System.out.println("Trying fallback: " + contents);
				try {
					IValue[] val = new IValue[] { _actuals[0] };
					Map<String, IValue> kwArgs = new HashMap<String, IValue>();
					// System.out.println(contents);
					kwArgs.put("__resugarType", VF.string(contents));
					System.out.println(declarationEnvironment.getRoot().getName());
					return makeResult(eval.call(
							this.getName(),
							declarationEnvironment.getRoot().getName(),
							kwArgs,
							(IValue[]) val));
				} catch(MatchFailed e) {
					continue;
				}
			}
		}
		throw new MatchFailed();
	}
	
	private boolean calledBefore;
	
	Result<IValue> makeResult(IValue v) {
		return ResultFactory.makeResult(v.getType(), v, ctx);
	}
	
	@SuppressWarnings("deprecation")
	private Result<IValue> callFunctionOnUnexpandFnAnno(IValue variable) {
		Environment old = eval.getCurrentEnvt();
		final String annotation = "unexpandFn";
		try {
			eval.setCurrentEnvt(new StayInScopeEnvironment(old));
			if (variable.asAnnotatable().hasAnnotation(annotation)) {
				if (variable.asAnnotatable().getAnnotation(annotation) instanceof ITuple) {
					return makeResult(((ITuple) variable.asAnnotatable().getAnnotation(annotation)).get(0)).call(
							new Type[] {variable.getType()}, new IValue[] {variable}, new HashMap<>());
				}
				return makeResult(variable.asAnnotatable().getAnnotation(annotation)).call(
						new Type[] {variable.getType()}, new IValue[] {variable}, new HashMap<>());
			}
		} finally {
			eval.unwind(old);
		}
		return null;
	}
	
	private IValue popTuple(ITuple tuple) {
		List<IValue> result = new LinkedList<IValue>();
		boolean first = true;
		for (IValue v : tuple) {
			if (first) {
				first = false;
			} else {
				result.add(v);
			}
		}
		return VF.tuple(result.toArray(new IValue[result.size()]));
	}
	
	@SuppressWarnings("deprecation")
	private IValue transformHiddenUnexpandFnToUnexpandFn(IValue val) {
		Map<String, IValue> annotations = new HashMap<>(val.asAnnotatable().getAnnotations());
		
		if (annotations.get("unexpandFn") instanceof ITuple) {
			if (((ITuple) annotations.get("unexpandFn")).arity() == 2) {
				annotations.put("unexpandFn", ((ITuple) annotations.get("unexpandFn")).get(1));
			} else {
				annotations.put("unexpandFn", 
						VF.tuple(((ITuple) annotations.get("unexpandFn")).get(1),
						popTuple((ITuple) annotations.get("unexpandFn"))));
			}
		}
		return val.asAnnotatable().setAnnotations(annotations);
	}
	
	private boolean hasHiddenUnexpandFn(IValue v) {
		return v.asAnnotatable().hasAnnotation("unexpandFn") &&
				v.asAnnotatable().getAnnotation("unexpandFn") instanceof ITuple;
	}
	
	private String getOnlyArgumentName(IMatchingResult matcher) {
		return matcher.getVariables().get(0).name();
	}
	
	private boolean resugarVariable(String name, Environment oldEnvironment, Result<IValue> variable) {
		Result<IValue> calledResult;
		Environment originalEnvironment = eval.getCurrentEnvt();
		try {
			eval.setCurrentEnvt(oldEnvironment);
			variable = eval.getCurrentEnvt().getSimpleVariable(name);
			calledResult = callFunctionOnUnexpandFnAnno(variable.getValue());
		} finally {
			eval.unwind(originalEnvironment);
		}
		if (calledResult != null) {
			storeLocalVariable(name, calledResult);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected Result<IValue> run() {
		System.out.println("Run!" + (func.getTags().getTags().size() > 0 ? func.getTags().getTags().get(0).getContents() : ""));
		if (defaultFunctionType != null) {
			System.out.println(eval.getCurrentEnvt().getVariable("__resugarType").getValue());
			if (eval.getCurrentEnvt().getVariable("__resugarType").getValue().isEqual(
					VF.string(defaultFunctionType)
					)) {
				
				StayInScopeEnvironment originalOld = new StayInScopeEnvironment(eval.getCurrentEnvt());
				
				// TODO Requires more testing... Especially on single-argument LHS-s.
				for (IVarPattern p : func.getPatternLhs().buildMatcher(eval).getVariables()) {
					System.out.println(p.name());
					resugarVariable(p.name(), new StayInScopeEnvironment(originalOld), null);
				}
				
				return func.getPatternLhs().interpret(eval);
			}
			throw new MatchFailed();
		}
		//if (calledBefore) {
		//	return makeResult(_actuals[0]);
		//}
		//calledBefore = true;
		Environment old = eval.getCurrentEnvt();
		FunctionDeclaration func = this.func;
		try {
			IMatchingResult matcher = _matcher;
			IValue[] myActuals = _actuals;
			eval.setCurrentEnvt(new StayInScopeEnvironment(old));
			if (myActuals[0] instanceof ITree) {
				System.out.println(TreeAdapter.yield((IConstructor) myActuals[0]));
			}
			if (hasHiddenUnexpandFn(myActuals[0])) {
				storeLocalVariable(getOnlyArgumentName(matcher),
						makeResult(transformHiddenUnexpandFnToUnexpandFn(myActuals[0])));
			}
			
			StayInScopeEnvironment originalOld = new StayInScopeEnvironment(eval.getCurrentEnvt());
			for (IVarPattern p : matcher.getVariables()) {
				System.out.println(p.name());
				resugarVariable(p.name(), new StayInScopeEnvironment(originalOld), null);
			}
			Map<String, Result<IValue>> myVariables = new HashMap<>(eval.getCurrentEnvt().getVariables());
	
			Environment oldEnvironment = eval.getCurrentEnvt();
			Result<IValue> finalResult;
			try {
				eval.setCurrentEnvt(new EmptyVariablesEnvironment(oldEnvironment));
				IMatchingResult resugarResult = func.getPatternLhs().buildMatcher(eval);
				resugarResult.initMatch(patternLhsResult);
				if (resugarResult.hasNext() && resugarResult.next()) {
					finalResult = makeResult(resugarResult.substitute(myVariables).get(0));
				} else {
					throw new RuntimeException("Has no next.");
				}
			} finally {
				eval.unwind(oldEnvironment);
			}
			if (finalResult.getValue().asAnnotatable().hasAnnotation("unexpandFn")) {
				// ...
				return callFunctionOnUnexpandFnAnno(finalResult.getValue());
			}
			return finalResult;
		} finally {
			eval.setCurrentEnvt(old);
		}
	}

	private void storeLocalVariable(String variableName, Result<IValue> result) {
		eval.getCurrentEnvt().storeLocalVariable(variableName, result);
	}

	private IValue getVariableFromEnvt(String variableName) {
		return eval.getCurrentEnvt().getVariable(variableName).getValue();
	}
	
}
