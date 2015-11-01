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
*******************************************************************************/
package org.rascalmpl.interpreter.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.TypeReifier;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.visitor.IValueMatchingResultVisitor;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.staticErrors.UnexpectedType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.values.uptr.IRascalValueFactory;
import org.rascalmpl.values.uptr.RascalValueFactory;

public class ReifiedTypePattern extends AbstractMatchingResult {
	private final NodePattern nodePattern;
	
	private static final Type defType = TypeFactory.getInstance().mapType(RascalValueFactory.Symbol, RascalValueFactory.Production);
	private static final TypeFactory TF = TypeFactory.getInstance();
	private static final IRascalValueFactory VF = IRascalValueFactory.getInstance();
	
	private IMatchingResult _symbol, _definitions;

	public ReifiedTypePattern(IEvaluatorContext ctx, Expression x, IMatchingResult symbol, IMatchingResult def) {
		super(ctx, x);
		List<IMatchingResult> arguments = new ArrayList<IMatchingResult>(2);
		this._symbol = symbol;
		this._definitions = def;
		arguments.add(symbol);
		arguments.add(def);
        this.nodePattern = new NodePattern(ctx, x, new LiteralPattern(ctx, x, ctx.getValueFactory().string("type")), null, RascalValueFactory.Type_Reified, arguments, Collections.<String,IMatchingResult>emptyMap());
	}

	@Override
	public Type getType(Environment env, HashMap<String,IVarPattern> patternVars) {
		// TODO: check if this would do it
		return RascalTypeFactory.getInstance().reifiedType(tf.valueType());
	}

	@Override
	public void initMatch(Result<IValue> subject) {
		super.initMatch(subject);
		nodePattern.initMatch(subject);
		hasNext = nodePattern.hasNext();
	}
	
	@Override
	public List<IVarPattern> getVariables() {
		return nodePattern.getVariables();
	}
	
	@Override
	public boolean next() {
		if (hasNext()) {
			boolean result = nodePattern.next();
			hasNext = nodePattern.hasNext();
			return result;
		}
		return false;
	}

	@Override
	public List<IValue> accept(IValueMatchingResultVisitor callback) {
		// We don't get a list here.
		IValue symbol = _symbol.accept(callback).get(0);
		IValue declarations = _definitions.accept(callback).get(0);
		
		if (!symbol.getType().isSubtypeOf(RascalValueFactory.Symbol)) {
			throw new UnexpectedType(RascalValueFactory.Symbol, symbol.getType(), _symbol.getAST());
		}
		
		if (!declarations.getType().isSubtypeOf(defType)) {
			throw new UnexpectedType(defType, declarations.getType(), _definitions.getAST());
		}
		
		java.util.Map<Type,Type> bindings = new HashMap<Type,Type>();
		bindings.put(RascalValueFactory.TypeParam, new TypeReifier(VF).symbolToType((IConstructor) symbol, (IMap) declarations));
		
		IValue val = VF.constructor(RascalValueFactory.Type_Reified.instantiate(bindings), symbol, declarations);
		
		bindings.put(RascalValueFactory.TypeParam, TF.valueType());
		// Type typ = RascalValueFactory.Type.instantiate(bindings);
		
		return callback.visit(this, Arrays.asList(val));
	}
}
