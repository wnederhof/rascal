package org.rascalmpl.interpreter.sugar;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;

public class Resugar {
	private Expression surfacePattern;
	private Expression corePattern;
	private IEvaluator<Result<IValue>> eval;
	private boolean repeatMode;
	private ResugarTransformer<RuntimeException> resugarTransformer;

	private IValue getVariable(IVarPattern varPattern) {
		return getVariable(varPattern.name());
	}
	
	private IValue getVariable(String name) {
		return eval.getCurrentEnvt().getVariable(name).getValue();
	}
	
	private void setVariable(String name, IValue value) {
		eval.getCurrentEnvt().storeLocalVariable(name, makeResult(value));
	}
	
	private Result<IValue> makeResult(IValue v) {
		return ResultFactory.makeResult(v.getType(), v, eval);
	}

	private void ensureNoVariablesLeak(Environment old) {
		eval.setCurrentEnvt(new StayInScopeEnvironment(old));
	}
	
	private Result<IValue> resugarTerm() {
		// TODO substitution.
		Result<IValue> surfaceTerm = surfacePattern.interpret(eval);
		return surfaceTerm;
	}
	
	private IValue stripSugarKeywordsLayer(IValue value) {
		return SugarParameters.stripSugarKeywordsLayer(value);
	}

	// unexp rs (i, R'rs T') σ
	private Result<IValue> transformAndResugarTerm(Result<IValue> toResugar) {
		IValue toResugarStripped = stripSugarKeywordsLayer(toResugar.getValue());
		toResugarStripped = toResugarStripped.accept(resugarTransformer);
		return resugarTerm();
	}
	
	private IValue resugarTransform(IValue subject) {
		return subject.accept(resugarTransformer);
	}

	private void resugarPatternVariables() {
		// We only need to resugar the core variables, since only the surface variables are desugared.
		for (IVarPattern varPattern : corePattern.buildMatcher(eval).getVariables()) {
			setVariable(varPattern.name(), resugarTransform(getVariable(varPattern)));
		}
	}

	public Result<IValue> resugar(Result<IValue> toResugar, IValue original) {
		Environment old = eval.getCurrentEnvt();
		try {
			ensureNoVariablesLeak(old);
			/* In repeat mode, variables are not resugared,
			 * but ResugarTransformer is used to desugar top-down,
			 * similar to Confection's resugaring technique. */
			if (repeatMode) {
				resugarPatternVariables();
				return resugarTerm();
			}
			return transformAndResugarTerm(toResugar);
		} finally {
			eval.unwind(old);
		}
	}

	public Resugar(Expression surfacePattern, Expression corePattern, ResugarTransformer<RuntimeException> resugarTransformer, IEvaluator<Result<IValue>> eval, boolean repeatMode) {
		this.resugarTransformer = resugarTransformer;
		this.surfacePattern = surfacePattern;
		this.corePattern = corePattern;
		this.eval = eval;
		this.repeatMode = repeatMode;
	}
}
