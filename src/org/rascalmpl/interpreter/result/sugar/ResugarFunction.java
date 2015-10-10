package org.rascalmpl.interpreter.result.sugar;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.IdentityVisitor;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.NamedFunction;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.sugar.Resugar;
import org.rascalmpl.interpreter.sugar.ResugarTransformer;
import org.rascalmpl.interpreter.sugar.SugarParameters;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;

// TODO: compute outermost label, production and implement cacheFormals for improved efficiency.
public class ResugarFunction extends NamedFunction {
	private FunctionDeclaration functionDeclaration;
	private IValue originalTerm;

	private static FunctionType createResugarFunctionType(FunctionDeclaration functionDeclaration,
			IEvaluator<Result<IValue>> eval, Environment env) {
		RascalTypeFactory RTF = org.rascalmpl.interpreter.types.RascalTypeFactory.getInstance();
		Type returnType = functionDeclaration.getTypeLhs().typeOf(env, true, eval);
		Type argType = functionDeclaration.getPatternRhs().typeOf(env, true, eval);
		Type argTypes = TypeFactory.getInstance().tupleType(argType);
		return (FunctionType) RTF.functionType(returnType, argTypes, TF.voidType());
	}

	public ResugarFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, FunctionDeclaration functionDeclaration,
			String name, Environment env, IValue originalTerm) {
		// TODO functionDeclaration used instead of ast (... because of the tags).
		super(functionDeclaration, eval, createResugarFunctionType(functionDeclaration, eval, env), new LinkedList<>(), name, false,
				false, false, env);
		this.functionDeclaration = functionDeclaration;
		this.originalTerm = originalTerm;
	}

	@Override
	public ICallableValue cloneInto(Environment env) {
		return new ResugarFunction(ast, eval, functionDeclaration, name, env, originalTerm);
	}

	@Override
	public boolean isStatic() {
		return getEnv().isRootScope() && eval.__getRootScope() != getEnv();
	}

	private Result<IValue> resugar(ISourceLocation src, Result<IValue> resultToDesugar) {
		IValueVisitor<IValue, RuntimeException> identityVisitor = new IdentityVisitor<RuntimeException>() {};
		ResugarTransformer<RuntimeException> desugarTransformer = new ResugarTransformer<RuntimeException>(
				identityVisitor, vf, eval);
		Resugar resugar = new Resugar(
				functionDeclaration.getPatternLhs(), // surface
				functionDeclaration.getPatternRhs(), // core
				desugarTransformer,
				eval,
				repeatMode());
		return resugar.resugar(resultToDesugar, originalTerm);
	}
	
	private void ensureNoVariablesLeak(Environment old) {
		eval.setCurrentEnvt(new StayInScopeEnvironment(old));
	}

	private Result<IValue> makeResult(IValue v) {
		return ResultFactory.makeResult(v.getType(), v, eval);
	}
	
	private IValue callInnerSugarsFirst(Type actualType, IValue termToDesugar) {
		if (SugarParameters.hasMultipleLayers(termToDesugar)) {
			termToDesugar = SugarParameters.peelSugarKeywordsLayer(termToDesugar);
			Result<IValue> resugarFunction = makeResult(SugarParameters.getTopMostResugarFunction(termToDesugar));
			return resugarFunction.call(new Type[] { actualType }, new IValue[] { termToDesugar }, null)
					.getValue();
		}
		return termToDesugar;
	}	
	
	/**
	 * Test for expression in which the only argument is equal to the interpretation, e.g. (Exp)`<Exp e>`.
	 * @return core is equal to its only argument.
	 */
	private boolean coreIsEqualToArgument() {
		IMatchingResult m = functionDeclaration.getPatternRhs().buildMatcher(eval);
		return m instanceof IVarPattern;
	}
	
	private void resugarOnlyArgument() {
		IMatchingResult m = functionDeclaration.getPatternRhs().buildMatcher(eval);
		String onlyVariable = m.getVariables().get(0).name();
		Result<IValue> onlyVariableResult = eval.getCurrentEnvt().getVariable(onlyVariable);
		onlyVariableResult = makeResult(callInnerSugarsFirst(onlyVariableResult.getType(), onlyVariableResult.getValue()));
		eval.getCurrentEnvt().storeLocalVariable(onlyVariable, onlyVariableResult);
	}
	
	private boolean repeatMode() {
		return tags.containsKey("repeatMode");
	}
	
	/**
	 * This function allows the execution of stacked resugar functions to execute in the right order.
	 * 
	 * When a resugaring takes place in the normal execution mode, then core patterns of the form:
	 * 
	 * (Exp)`<Exp e>`
	 * 
	 * Will lead to errors if no "disassembly" takes place, since e is equivalent to
	 * the term to resugar. Therefore, we replace the e with the resugared version of e.
	 * 
	 * For other cases, both in normal execution mode and repeated execution mode, if there are multiple
	 * resugar functions stored on one annotation, the inner-most function must be called first.
	 * 
	 * @param termToResugar the term to resugar.
	 * @return the resugared term.
	 */
	private Result<IValue> disassembleSugarParametersAndResugar(IValue termToResugar) {
		Result<IValue> resultToDesugar = makeResult(termToResugar);
		IMatchingResult matcher;
		if (!repeatMode() && coreIsEqualToArgument()) {
			matcher = functionDeclaration.getPatternRhs().buildMatcher(eval);
			matcher.initMatch(resultToDesugar);
			resugarOnlyArgument();
		} else {
			termToResugar = callInnerSugarsFirst(termToResugar.getType(), termToResugar);
			matcher = functionDeclaration.getPatternRhs().buildMatcher(eval);
			matcher.initMatch(resultToDesugar);
		}
		ISourceLocation src = eval.getCurrentAST().getLocation();
		if (matcher.next()) {
			Result<IValue> r = resugar(src, resultToDesugar);
			return r;
		}
		throw new MatchFailed();
	}

	@Override
	public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
		if (keyArgValues != null && (!keyArgValues.isEmpty() || actualTypes.length != 1 || actuals.length != 1))
			throw new MatchFailed();
		Environment old = ctx.getCurrentEnvt();
		try {
			IValue termToResugar = actuals[0];
			ensureNoVariablesLeak(declarationEnvironment);
			return disassembleSugarParametersAndResugar(termToResugar);
		} finally {
			eval.unwind(old);
		}
	}
}
