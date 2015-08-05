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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;

public class SubstitutionEvaluator {

	private IValue substitute(Expression pattern, IValue subject, IEvaluator<Result<IValue>> eval) {
		Environment old = eval.getCurrentEnvt();
		try {
			eval.pushEnv();
			
			IMatchingResult r = pattern.buildMatcher(eval);
			
			r.initMatch(ResultFactory.makeResult(subject.getType(), subject, eval));
			
			if (r.hasNext() && r.next()) {
				return r.substitute(old.getVariables()).get(0).getValue();
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}

}
