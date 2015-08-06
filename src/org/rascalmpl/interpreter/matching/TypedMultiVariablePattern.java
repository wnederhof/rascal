/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Mark Hills - Mark.Hills@cwi.nl - CWI
*******************************************************************************/
package org.rascalmpl.interpreter.matching;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;


public class TypedMultiVariablePattern extends TypedVariablePattern {

	public TypedMultiVariablePattern(IEvaluatorContext ctx, Expression x, org.eclipse.imp.pdb.facts.type.Type type, org.rascalmpl.ast.Name name) {
		super(ctx, x, type, name);		
	}
	
	public TypedMultiVariablePattern(IEvaluatorContext ctx, Expression x, org.eclipse.imp.pdb.facts.type.Type type, String name) {
		super(ctx, x, type, name);
	}
	
	public void convertToListType() {
		if (!this.alreadyStored) {
			this.declaredType = TypeFactory.getInstance().listType(/*this.declaredType.isListType() ? this.declaredType.getElementType() : */this.declaredType);
		} else {
			if(!declaredType.isList())
				throw new ImplementationError("Cannot convert a typed multi variable to a list after it has already been stored at its current type");
		}
	}

	public void covertToSetType() {
		if (!this.alreadyStored) {
			this.declaredType = TypeFactory.getInstance().setType(/*this.declaredType.isSetType() ? this.declaredType.getElementType() : */this.declaredType);
		} else {
			if(!declaredType.isSet())
				throw new ImplementationError("Cannot convert a typed multi variable to a set after it has already been stored at its current type");
		}
	}
	
	@Override
	public String toString(){
		return "*" + declaredType + " " + getName();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Result<IValue>> substitute(Map<String, Result<IValue>> substitutionMap) {
		// TO DO: Types.
		List<Result<IValue>> resultList = new LinkedList<>();
		Result<IValue> resultElem = super.substitute(substitutionMap).get(0);
		if (resultElem.getType().isList() || resultElem.getType().isSet()) {
			for (IValue val : (Iterable<IValue>) resultElem.getValue()) {
				resultList.add(ResultFactory.makeResult(val.getType(), val, ctx));
			}
		} // We follow Expression.List on this: just return an empty list if not
		  // a list or a set.
		return resultList;
	}

}
