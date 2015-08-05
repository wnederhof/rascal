/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Emilie Balland - (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.interpreter.matching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.ast.QualifiedName;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.result.Result;

/* package */ class MapPattern extends AbstractMatchingResult {
	private java.util.List<IMatchingResult> children;
	
	MapPattern(IEvaluatorContext ctx, Expression.Map x, java.util.List<IMatchingResult> children){
		super(ctx, x);
		
		this.children = children;
	}
	
	@Override
	public List<IVarPattern> getVariables(){
		java.util.LinkedList<IVarPattern> res = new java.util.LinkedList<IVarPattern> ();
		for (int i = 0; i < children.size(); i++) {
			res.addAll(children.get(i).getVariables());
		 }
		return res;
	}
	
	@Override
	public Type getType(Environment env, HashMap<String,IVarPattern> patternVars) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean next(){
		checkInitialized();
		throw new ImplementationError("AbstractPatternMap.match not implemented");
	}

	@Override
	public List<Result<IValue>> substitute(Map<String, Result<IValue>> substitutionMap) {
		throw new ImplementationError("AbstractPatternMap.substitute not implemented");
	}
}
