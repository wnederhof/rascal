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

import static org.rascalmpl.interpreter.result.ResultFactory.makeResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.staticErrors.UnexpectedType;

public class LiteralPattern extends AbstractMatchingResult {
	private IValue literal;
	private boolean isPattern = false;
	
	public LiteralPattern(IEvaluatorContext ctx, AbstractAST x, IValue literal){
		super(ctx, x);
		this.literal = literal;
	}
	
	@Override
	public List<IVarPattern> getVariables() {
		return Collections.emptyList();
	}
	
	@Override
	public Type getType(Environment env, HashMap<String,IVarPattern> patternVars) {
			return literal.getType();
	}
	
	@Override
	public void initMatch(Result<IValue> subject) {
		super.initMatch(subject);
		isPattern = true;
	}
	
	@Override
	public boolean next(){
		checkInitialized();
		if(!hasNext)
			return false;
		hasNext = false;
	
		if (isPattern && subject.getValue().getType().comparable(literal.getType())) {
			return subject.equals(makeResult(literal.getType(), literal, ctx)).isTrue();
		}
		else if (!isPattern) {
			if (literal.getType().isBool()) {
				return ((IBool) literal).getValue(); 
			}
			
			throw new UnexpectedType(tf.boolType(), literal.getType(), ctx.getCurrentAST());
		}
		
		
		return false;
	}
	
	public IValue toIValue(Environment env){
		return literal;
	}
	
	@Override
	public String toString(){
		return "pattern: " + literal;
	}

	@Override
	public List<IValue> accept(IMatchingResultVisitor callback) {
		return callback.visit(this, Arrays.asList(literal));
	}
	
	
}
