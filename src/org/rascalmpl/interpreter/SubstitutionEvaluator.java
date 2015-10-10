package org.rascalmpl.interpreter;

import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.EmptyVariablesEnvironment;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;

public class SubstitutionEvaluator {
	
	public static Result<IValue> substitute(Expression pattern, IValue subject, IEvaluator<Result<IValue>> eval, Map<String, Result<IValue>> variables) {
		Environment old = eval.getCurrentEnvt();
		try {
			EmptyVariablesEnvironment eve = new EmptyVariablesEnvironment(old);
			eval.setCurrentEnvt(eve);
			IMatchingResult r = pattern.buildMatcher(eval);
			r.initMatch(ResultFactory.makeResult(subject.getType(), subject, eval));
			while (r.hasNext() && r.next()) {
				IValue subs = r.substitute(eve.getVariables()).get(0);
				if (ResultFactory.makeResult(subject.getType(), subject, eval)
						.equals(ResultFactory.makeResult(subs.getType(), subs, eval)).isTrue()) {
					IValue substituted = r.substitute(variables).get(0);
					// TODO this may not work if substituted is IList.
					return ResultFactory.makeResult(substituted.getType(), substituted, eval);
				}
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}

	public static Result<IValue> substitute(Expression pattern, IValue subject, IEvaluator<Result<IValue>> eval) {
		return substitute(pattern, subject, eval, eval.getCurrentEnvt().getVariables());
	}
	
	public static Map<String, Result<IValue>> getPatternVariableMap(Expression pattern, Result<IValue> subject, IEvaluator<Result<IValue>> eval) {
		Environment old = eval.getCurrentEnvt();
		try {
			EmptyVariablesEnvironment eve = new EmptyVariablesEnvironment(old);
			eval.setCurrentEnvt(eve);
			IMatchingResult m = pattern.buildMatcher(eval);
			m.initMatch(subject);
			if (m.hasNext() && m.next()) {
				return eve.getVariables();
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}
	
	private SubstitutionEvaluator() {}

}
