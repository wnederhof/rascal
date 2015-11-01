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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.Expression.CallOrTree;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.visitor.IValueMatchingResultVisitor;
import org.rascalmpl.interpreter.staticErrors.UnsupportedOperation;

class ConcreteAmbiguityPattern extends AbstractMatchingResult {

	public ConcreteAmbiguityPattern(IEvaluatorContext ctx, CallOrTree x, java.util.List<AbstractBooleanResult> args) {
		super(ctx, x);
	}

	public Type getType(Environment env, HashMap<String,IVarPattern> patternVars) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean next() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<IValue> accept(IValueMatchingResultVisitor callback) {
		// Next does not ever yield any result. Furthermore, this class is never used.
		throw new UnsupportedOperation("substitute", getAST());
	}
}
