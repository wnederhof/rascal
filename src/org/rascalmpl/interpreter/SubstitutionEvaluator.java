/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
 *   * Emilie Balland - (CWI)
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.interpreter;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.EmptyVariablesEnvironment;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.values.uptr.TreeAdapter;

public class SubstitutionEvaluator {

	public static Result<IValue> substitute(Expression pattern, IValue subject, IEvaluator<Result<IValue>> eval) {
		// TO DISABLE: if(1==1) return ResultFactory.makeResult(subject.getType(), subject, eval);
		Environment old = eval.getCurrentEnvt();
		int i = 0;
		try {
			EmptyVariablesEnvironment eve = new EmptyVariablesEnvironment(old);
			eval.setCurrentEnvt(eve);
			
			IMatchingResult r = pattern.buildMatcher(eval);
			
			r.initMatch(ResultFactory.makeResult(subject.getType(), subject, eval));
			
			while (r.hasNext() && r.next()) {
				// We check if the original subject is equal to the substitution of itself.
				// TODO: We could also first match in the environment of subject.
				IValue subs = r.substitute(eve.getVariables()).get(0);
				//boolean b = (IBool) ResultFactory.makeResult(subs.getType(), subs, eval)).getValue();
				if (ResultFactory.makeResult(subject.getType(), subject, eval)
						.equals(ResultFactory.makeResult(subs.getType(), subs, eval)).isTrue()) {
					IValue substituted = r.substitute(old.getVariables()).get(0);
					// TODO this may not work if substituted is IList.
					return ResultFactory.makeResult(substituted.getType(), substituted, eval);
				} else {
					System.out.println("Subject: " + TreeAdapter.yield((IConstructor) subject, 1000));
					System.out.println("Substituted: " + TreeAdapter.yield((IConstructor) subs, 1000));
				}
			}
			throw new MatchFailed();
			
			/*
			EmptyVariablesEnvironment eve = new EmptyVariablesEnvironment(old);
			eval.setCurrentEnvt(eve);
			IMatchingResult r = pattern.buildMatcher(eval);
			r.initMatch(ResultFactory.makeResult(subject.getType(), subject, eval));
			// First match fills the variables in the mock environment.
			if (r.hasNext() && r.next()) {
				r = pattern.buildMatcher(eval);
				r.initMatch(ResultFactory.makeResult(subject.getType(), subject, eval));
				// Second match: matches against the variables of subject to eliminate variety.
				if (r.hasNext() && r.next()) {
					IValue substituted = r.substitute(old.getVariables()).get(0);
					// TODO this may not work if substituted is IList.
					// Now we can substitute.
					return ResultFactory.makeResult(substituted.getType(), substituted, eval);
				}
			}
			throw new MatchFailed();*/
		} finally {
			eval.unwind(old);
		}
	}
	
	private SubstitutionEvaluator() {}

}
