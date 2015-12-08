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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.matching.visitor.IValueMatchingResultVisitor;


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
	public List<IValue> accept(IValueMatchingResultVisitor callback) {
		IValue resultElem;
		resultElem = super.accept(callback).get(0);
		List<IValue> resultList = new LinkedList<>();
		
		if (resultElem.getType().isList() || resultElem.getType().isSet()) {
			for (IValue val : (Iterable<IValue>) resultElem) {
				resultList.add(val);
			}
		} // We follow Expression.List on this: just return an empty list if not
		  // a list or a set.
		return callback.visit(this, resultList);
	}

}
