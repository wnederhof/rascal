package org.rascalmpl.interpreter.sugar;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.result.sugar.ResugarFunction;
import org.rascalmpl.interpreter.utils.Names;

public class Desugar {
	private Expression corePattern;
	private DesugarTransformer<RuntimeException> desugarTransformer;
	private IEvaluator<Result<IValue>> eval;
	private boolean repeatMode;
	private FunctionDeclaration functionDeclaration;
	
	private Result<IValue> makeResult(IValue v) {
		return ResultFactory.makeResult(v.getType(), v, eval);
	}
	
	private IValue getVariable(IVarPattern varPattern) {
		return getVariable(varPattern.name());
	}
	
	private IValue getVariable(String name) {
		return eval.getCurrentEnvt().getVariable(name).getValue();
	}
	
	private void setVariable(String name, IValue value) {
		eval.getCurrentEnvt().storeLocalVariable(name, makeResult(value));
	}
	
	private Result<IValue> attachResugarFunction(Result<IValue> coreTerm, IValue originalTerm) {
		new ResugarFunction(eval.getCurrentAST(), eval, functionDeclaration,
				Names.name(functionDeclaration.getName()), eval.getCurrentEnvt(), originalTerm);
		return coreTerm;
	}

	private IValue desugarTransform(Result<IValue> subject) {
		return desugarTransform(subject.getValue());
	}
	
	private IValue desugarTransform(IValue subject) {
		return subject.accept(desugarTransformer);
	}

	private void desugarPatternVariables() {
		// We only need to desugar the variables in the core pattern.
		for (IVarPattern varPattern : corePattern.buildMatcher(eval).getVariables()) {
			setVariable(varPattern.name(), desugarTransform(getVariable(varPattern)));
		}
	}

	private Result<IValue> desugarTerm(IValue originalTerm) {
		Result<IValue> coreTerm = corePattern.interpret(eval);
		return attachResugarFunction(coreTerm, originalTerm);
	}

	private Result<IValue> desugarTermAndTransform(IValue originalTerm) {
		Result<IValue> desugaredTerm = desugarTerm(originalTerm);
		IValue transformedTerm = desugarTransform(desugaredTerm);
		return makeResult(transformedTerm);
	}
	
	private void ensureNoVariablesLeak(Environment old) {
		eval.setCurrentEnvt(new StayInScopeEnvironment(old));
	}
	
	public Result<IValue> desugar(Result<IValue> toDesugar) {
		Environment old = eval.getCurrentEnvt();
		try {
			ensureNoVariablesLeak(old);
			/* In repeat mode, variables are not desugared,
			 * but DesugarTransformer is used to desugar top-down,
			 * similar to Confection's desugaring technique. */
			if (!repeatMode) {
				desugarPatternVariables();
				return desugarTerm(toDesugar.getValue());
			}
			return desugarTermAndTransform(toDesugar.getValue());
		} finally {
			eval.unwind(old);
		}
	}

	public Desugar(DesugarTransformer<RuntimeException> desugarTransformer,
			IEvaluator<Result<IValue>> eval, boolean repeatMode, FunctionDeclaration functionDeclaration) {
		this.corePattern = functionDeclaration.getPatternRhs();
		this.desugarTransformer = desugarTransformer;
		this.eval = eval;
		this.repeatMode = repeatMode;
		this.functionDeclaration = functionDeclaration;
	}
}
