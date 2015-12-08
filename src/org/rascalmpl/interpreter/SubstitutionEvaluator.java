package org.rascalmpl.interpreter;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.EmptyVariablesEnvironment;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.visitor.SubstitutionVisitor;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;

public class SubstitutionEvaluator {
	
	private static Integer getLength(Result<IValue> variable) {
		IValue value = variable.getValue();
		System.out.println("ClassName: " + value.getClass());
		// TODO: Hack ^ 2.
		if (value.getClass().toString().startsWith("class org.rascalmpl.values.uptr.RascalValueFactory$")) {
			Iterator<?> it = ((Iterable<?>) value).iterator();
			it.next(); // TODO: Evil hack.
			value = (IValue) it.next();
		}
		if (value instanceof IList) {
			return ((IList) value).length();
		} else if (value instanceof ISet) {
			return ((ISet) value).size();
		} else if (value instanceof Iterable) {
			int i = 0;
			Iterator<?> iterator = ((Iterable<?>) value).iterator();
			while (iterator.hasNext()) {
				i++;
				iterator.next();
			}
			return i;
		}
		throw new MatchFailed();
	}
	
	public static Result<IValue> substitute(Expression pattern, IValue subject, IEvaluator<Result<IValue>> eval,
			Map<String, Result<IValue>> variables, Map<String, Integer> maxEllipsisVariablesLength) {
		
		Environment old = eval.getCurrentEnvt();
		try {
			EmptyVariablesEnvironment eve = new EmptyVariablesEnvironment(old);
			eval.setCurrentEnvt(eve);
			IMatchingResult r = pattern.buildMatcher(eval);
			r.initMatch(ResultFactory.makeResult(subject.getType(), subject, eval));
			while (r.hasNext() && r.next()) {
				if (maxEllipsisVariablesLength != null) {
					for (String s : maxEllipsisVariablesLength.keySet()) {
						if (!getLength(eve.getVariable(s)).equals(maxEllipsisVariablesLength.get(s))) {
							System.out.println("Continue.");
							continue;
						}
					}
				}
				IValue subs = r.accept(new SubstitutionVisitor(eval.getCurrentEnvt(), eve.getVariables())).get(0);
				System.out.println(subs.getType());
				if (ResultFactory.makeResult(subject.getType(), subject, eval)
						.equals(ResultFactory.makeResult(subs.getType(), subs, eval)).isTrue()) {
					IValue substituted = r.accept(new SubstitutionVisitor(eval.getCurrentEnvt(), variables)).get(0);
					// TODO this may not work if substituted is IList.
					return ResultFactory.makeResult(substituted.getType(), substituted, eval);
				}
				System.out.println("Continue 2.");
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}

	public static Result<IValue> substitute(Expression pattern, IValue subject, IEvaluator<Result<IValue>> eval, Map<String, Integer> maxEllipsisVariablesLength) {
		return substitute(pattern, subject, eval, eval.getCurrentEnvt().getVariables(), maxEllipsisVariablesLength);
	}
	
	public static Map<String, Result<IValue>> getPatternVariableMap(Expression pattern, Result<IValue> subject,
			IEvaluator<Result<IValue>> eval, Map<String, Integer> maxEllipsisVariablesLength) {
		Environment old = eval.getCurrentEnvt();
		try {
			EmptyVariablesEnvironment eve = new EmptyVariablesEnvironment(old);
			eval.setCurrentEnvt(eve);
			IMatchingResult m = pattern.buildMatcher(eval);
			m.initMatch(subject);
			start: while (m.hasNext() && m.next()) {
				if (maxEllipsisVariablesLength != null) {
					for (String s : maxEllipsisVariablesLength.keySet()) {
						if (!getLength(eve.getVariable(s)).equals(maxEllipsisVariablesLength.get(s))) {
							System.out.println("Continue.");
							continue start;
						}
					}
				}
				return eve.getVariables();
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}
	
	private SubstitutionEvaluator() {}

}
