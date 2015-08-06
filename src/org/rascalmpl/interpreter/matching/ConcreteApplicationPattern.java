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
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.interpreter.matching;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IAnnotatable;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IWithKeywordParameters;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.semantics.dynamic.Tree;
import org.rascalmpl.values.uptr.IRascalValueFactory;
import org.rascalmpl.values.uptr.ITree;
import org.rascalmpl.values.uptr.ProductionAdapter;
import org.rascalmpl.values.uptr.SymbolAdapter;
import org.rascalmpl.values.uptr.TreeAdapter;

public class ConcreteApplicationPattern extends AbstractMatchingResult {
	private static final IRascalValueFactory VF = IRascalValueFactory.getInstance();
	private IList subjectArgs;
	private final IMatchingResult tupleMatcher;
	private IConstructor production;
	private final ITuple tupleSubject;
	private final Type myType;
	private boolean isLiteral;
	private List<IMatchingResult> list;

	public ConcreteApplicationPattern(IEvaluatorContext ctx, Tree.Appl x, List<IMatchingResult> list) {
		super(ctx, x);
		
		this.list = list;
		
		// retrieve the static value of the production of this pattern
		this.production = x.getProduction();
		
		// use a tuple pattern to match the children of this pattern
		this.tupleMatcher = new TuplePattern(ctx, x, list);
		
		// this prototype can be used for every subject that comes through initMatch
		this.tupleSubject = new TreeAsTuple();
		
		// save the type of this tree
		this.myType = x.getConcreteSyntaxType();
	}
	
	public List<IVarPattern> getVariables() {
		return tupleMatcher.getVariables();
	}
	
	private class TreeAsTuple implements ITuple {
		
		public int arity() {
			return subjectArgs.length();
		}

		public IValue get(int i) throws IndexOutOfBoundsException {
			return subjectArgs.get(i);
		}

		public Type getType() {
			Type[] fields = new Type[arity()];
			for (int i = 0; i < fields.length; i++) {
				fields[i] = get(i).getType();
			}
			return tf.tupleType(fields);
		}

		public boolean isEqual(IValue other) {
			if (!(other instanceof ITuple)) {
				return false;
			}
			
			if (arity() != ((ITuple) other).arity()) {
				return false;
			}
			
			int i = 0;
			for (IValue otherChild : (ITuple) other) {
				if (!get(i++).isEqual(otherChild)) {
					return false;
				}
			}
			
			return true;
		}

		
		public Iterator<IValue> iterator() {
			return new Iterator<IValue>() {
				int currentIndex = 0;

				public boolean hasNext() {
					return currentIndex < arity();
				}

				public IValue next() {
					return get(currentIndex++);
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		public IValue get(String label) throws FactTypeUseException {
			throw new UnsupportedOperationException();
		}

		public IValue select(int... fields) throws IndexOutOfBoundsException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IValue selectByFieldNames(String... fields) throws FactTypeUseException {
			throw new UnsupportedOperationException();
		}

		public ITuple set(int i, IValue arg) throws IndexOutOfBoundsException {
			throw new UnsupportedOperationException();
		}

		public ITuple set(String label, IValue arg) throws FactTypeUseException {
			throw new UnsupportedOperationException();
		}

		public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isAnnotatable() {
			return false;
		}

		@Override
		public IAnnotatable<? extends IValue> asAnnotatable() {
			throw new IllegalOperationException(
					"Cannot be viewed as annotatable.", getType());
		}
		
	  @Override
	  public boolean mayHaveKeywordParameters() {
	    return false;
	  }
	  
	  @Override
	  public IWithKeywordParameters<? extends IValue> asWithKeywordParameters() {
	    throw new IllegalOperationException(
	        "Cannot be viewed as with keyword parameters", getType());
	  }

	}
	

	@Override
	public void initMatch(Result<IValue> subject) {
		hasNext = false;
		Type subjectType = subject.getValue().getType();
		super.initMatch(subject);

		if(subjectType.isAbstractData() && subject.getValue() instanceof ITree) {
			org.rascalmpl.values.uptr.ITree treeSubject = (org.rascalmpl.values.uptr.ITree)subject.getValue();
		
			if (!TreeAdapter.isAppl(treeSubject)) {
				// fail early if the subject is an ambiguity cluster
				hasNext = false;
				return;
			}

			if (!TreeAdapter.getProduction(treeSubject).isEqual(production)) {
				// fail early if the subject's production is not the same
				hasNext = false;
				return;
			}
			
			if (!SymbolAdapter.isLiteral(ProductionAdapter.getType(production))) {
				this.subjectArgs = TreeAdapter.getNonLayoutArgs(treeSubject);
				tupleMatcher.initMatch(ResultFactory.makeResult(tupleSubject.getType(), tupleSubject, ctx));
			
				hasNext = tupleMatcher.hasNext();
				isLiteral = false;
			}
			else {
				isLiteral = true;
				hasNext = true;
			}
		}
	}
	
	@Override
	public boolean hasNext() {
		if (!isLiteral) {
			return tupleMatcher.hasNext();
		}
		
		return true;
	}
	
	@Override
	public boolean next(){
		checkInitialized();
		
		if (hasNext && !isLiteral) {
			return tupleMatcher.next();
		}
		else if (hasNext){
			hasNext = false;
			return true;
		}
		
		return false;
	}

	@Override
	public Type getType(Environment env,
			HashMap<String, IVarPattern> patternVars) {
		return myType;
	}
	
	@Override
	public String toString() {
	  return production.toString();
	}

	private <T extends IValue> Result<T> makeResult(Type declaredType, IValue value) {
		return ResultFactory.makeResult(declaredType, value, ctx);
	}
	
	@Override
	public List<Result<IValue>> substitute(Map<String, Result<IValue>> substitutionMap) {
		IListWriter w = ctx.getValueFactory().listWriter();
		for (IMatchingResult arg : list) {
			w.append(arg.substitute(substitutionMap).get(0).getValue());
		}

		Type type = RascalTypeFactory.getInstance().nonTerminalType(production);
		
		Map<String, IValue> annos = ((ITree) subject).asAnnotatable().getAnnotations();
		
		if (!annos.isEmpty()) {
			return Arrays.asList(makeResult(type, VF.appl(annos, production, w.done())));
		}
		else {
			return Arrays.asList(makeResult(type, VF.appl(production, w.done())));
		}
	}
	
}
