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
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.interpreter.matching;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.visitor.IValueMatchingResultVisitor;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.types.NonTerminalType;
import org.rascalmpl.values.uptr.ITree;
import org.rascalmpl.values.uptr.ProductionAdapter;
import org.rascalmpl.values.uptr.RascalValueFactory;
import org.rascalmpl.values.uptr.SymbolAdapter;
import org.rascalmpl.values.uptr.TreeAdapter;

public class ConcreteListPattern extends AbstractMatchingResult {
	private ListPattern pat;
	private Expression callOrTree;

	public ConcreteListPattern(IEvaluatorContext ctx, Expression x, List<IMatchingResult> list) {
		super(ctx, x);
		
		callOrTree = x;
		initListPatternDelegate(list);
		//System.err.println("ConcreteListPattern");
	}

	private void initListPatternDelegate(List<IMatchingResult> list) {
		Type type = getType(null, null);
		
		if (type instanceof NonTerminalType) {
			IConstructor rhs = ((NonTerminalType) type).getSymbol();

		   if (SymbolAdapter.isIterPlus(rhs) || SymbolAdapter.isIterStar(rhs)) {
				pat = new ListPattern(ctx, callOrTree, list, 1);
			}
			else if (SymbolAdapter.isIterPlusSeps(rhs) || SymbolAdapter.isIterStarSeps(rhs)) {
				pat = new ListPattern(ctx, callOrTree, list, SymbolAdapter.getSeparators(rhs).length() + 1);
			}
			else {
				throw new ImplementationError("crooked production: non (cf or lex) list symbol: " + rhs);
			}
			return;
		}
		throw new ImplementationError("should not get here if we don't know that its a proper list");
	}

	@Override
	public void initMatch(Result<IValue> subject) {
		super.initMatch(subject);
		if (!subject.getType().isSubtypeOf(RascalValueFactory.Tree)) {
			hasNext = false;
			return;
		}
		org.rascalmpl.values.uptr.ITree tree = (org.rascalmpl.values.uptr.ITree) subject.getValue();
		
		if (!tree.isAppl()) {
			hasNext = false;
			return;
		}
		pat.initMatch(ResultFactory.makeResult(RascalValueFactory.Args, TreeAdapter.getArgs(tree), ctx));
		hasNext = true;
	}
	
	@Override
	public Type getType(Environment env, HashMap<String,IVarPattern> patternVars) {
		return callOrTree.getConcreteSyntaxType();
	}

	@Override
	public boolean hasNext() {
		if (!hasNext) {
			return false;
		}
		return pat.hasNext();
	}
	
	@Override
	public boolean next() {
		if (!hasNext()) {
			return false;
		}
		return pat.next();
		
	}

	@Override
	public List<IVarPattern> getVariables() {
		return pat.getVariables();
	}
	
	@Override
	public String toString() {
	  return "concrete: " + pat.toString();
	}
	
	private int getDelta(IConstructor prod) {
		IConstructor rhs = ProductionAdapter.getType(prod);

		if (SymbolAdapter.isIterPlusSeps(rhs) || SymbolAdapter.isIterStarSeps(rhs)) {
			return SymbolAdapter.getSeparators(rhs).length();
		}

		return 0;
	}
	
	private void appendPreviousSeparators(IList args, IListWriter result, int delta, int i, boolean previousWasEmpty) {
		if (!previousWasEmpty) {
			for (int j = i - delta; j > 0 && j < i; j++) {
				result.append(args.get(j));
			}
		}
	}
	
	private IList flatten(IList args, int delta, IConstructor production) {
		IListWriter result = VF.listWriter();
		boolean previousWasEmpty = false;

		for (int i = 0; i < args.length(); i+=(delta+1)) {
			org.rascalmpl.values.uptr.ITree tree = (org.rascalmpl.values.uptr.ITree) args.get(i);

			if (TreeAdapter.isList(tree) && ProductionAdapter.shouldFlatten(production, TreeAdapter.getProduction(tree))) {
				IList nestedArgs = TreeAdapter.getArgs(tree);
				if (nestedArgs.length() > 0) {
					appendPreviousSeparators(args, result, delta, i, previousWasEmpty);
					result.appendAll(nestedArgs);
				}
				else {
					previousWasEmpty = true;
				}
			}
			else {
				appendPreviousSeparators(args, result, delta, i, previousWasEmpty);
				result.append(tree);
				previousWasEmpty = false;
			}
		}

		return result.done();
	}

	@Override
	public List<IValue> accept(IValueMatchingResultVisitor callback) {
		IListWriter w = ctx.getValueFactory().listWriter();
		for (IValue arg : pat.acceptChildren(callback)) {
			w.append(arg);
		}
		IConstructor production = ((org.rascalmpl.values.uptr.ITree) subject.getValue()).getProduction();
		int delta = getDelta(production);
		return Arrays.asList(
				((org.rascalmpl.values.uptr.ITree) subject.getValue()).set(1, flatten(w.done(), delta, production)));
	}
}
