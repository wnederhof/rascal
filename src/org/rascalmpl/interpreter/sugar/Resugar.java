package org.rascalmpl.interpreter.sugar;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.SubstitutionEvaluator;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.values.uptr.TreeAdapter;

public class Resugar {
	private Expression surfacePattern;
	private Expression corePattern;
	private IEvaluator<Result<IValue>> eval;
	private boolean repeatMode;
	private ResugarTransformer<RuntimeException> resugarTransformer;
	private Map<String, Integer> maxEllipsisVariablesLength;

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
	
	private IValue peelSugarKeywordsLayer(IValue value) {
		return SugarParameters.peelSugarKeywordsLayer(value);
	}
	
	// (j, T) σ = (σ · (T'/P'_j ))P_j
	private Result<IValue> unexp(IValue subject, Map<String, Result<IValue>> sigma, IValue original) {
		Map<String, Result<IValue>> T_acc_P_acc_j = SubstitutionEvaluator.getPatternVariableMap(corePattern,
				makeResult(subject), eval, maxEllipsisVariablesLength);
		HashMap<String, Result<IValue>> union = new HashMap<String, Result<IValue>>();
		union.putAll(sigma);
		union.putAll(T_acc_P_acc_j);
		return SubstitutionEvaluator.substitute(surfacePattern, original, eval, union, maxEllipsisVariablesLength);
	}
	
	// R'rs (Tag (Head i σ) T') = unexp_{rs} (i, R'rs(T')) σ
	private Result<IValue> transformAndResugarTerm(Result<IValue> toResugar, IValue original) {
		IValue T_acc = peelSugarKeywordsLayer(toResugar.getValue());
		IValue R_rs_T_acc = resugarTransform(T_acc);
		return unexp(R_rs_T_acc, eval.getCurrentEnvt().getVariables(), original); 
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
	
	/* Test for expression in which the only argument is equal to the interpretation, e.g.
	 * (Exp)`<Exp e>`. */
	private boolean coreIsEqualToArgument() {
		IMatchingResult m = corePattern.buildMatcher(eval);
		return m instanceof IVarPattern;
	}

	public Result<IValue> resugar(Result<IValue> toResugar, IValue original) {
		Environment old = eval.getCurrentEnvt();
		System.out.println("Check!");
		try {
			ensureNoVariablesLeak(old);
			/* In repeat mode, variables are not resugared,
			 * but ResugarTransformer is used to desugar top-down,
			 * similar to Confection's resugaring technique. */
			if (!repeatMode) {
				if (coreIsEqualToArgument()) {
					for (IVarPattern varPattern : corePattern.buildMatcher(eval).getVariables()) {
//						System.out.println(varPattern.name());
						setVariable(varPattern.name(),
								resugarTransform(peelSugarKeywordsLayer(getVariable(varPattern))));
					}
				} else {
					resugarPatternVariables();
				}
				return SubstitutionEvaluator.substitute(surfacePattern, original, eval, maxEllipsisVariablesLength);
			}
			return transformAndResugarTerm(toResugar, original);
		} finally {
			eval.unwind(old);
		}
	}

	public Resugar(Expression surfacePattern, Expression corePattern,
			ResugarTransformer<RuntimeException> resugarTransformer, IEvaluator<Result<IValue>> eval, boolean repeatMode, Map<String, Integer> maxEllipsisVariablesLength) {
		this.resugarTransformer = resugarTransformer;
		this.surfacePattern = surfacePattern;
		this.corePattern = corePattern;
		this.eval = eval;
		this.repeatMode = repeatMode;
		this.maxEllipsisVariablesLength = maxEllipsisVariablesLength;
	}
}
