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
 *   * Wietse Venema - wietsevenema@gmail.com - CWI
 *   * Anastasia Izmaylova - A.Izmaylova@cwi.nl - CWI
 *******************************************************************************/
package org.rascalmpl.interpreter.result;

import static org.rascalmpl.interpreter.result.ResultFactory.makeResult;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.KeywordFormal;
import org.rascalmpl.ast.Statement;
import org.rascalmpl.interpreter.Accumulator;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.staticErrors.MissingReturn;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;

public class RascalFunction extends CustomNamedFunction {
	private final List<Statement> body;
	
	public RascalFunction(IEvaluator<Result<IValue>> eval, FunctionDeclaration.Default func, boolean varargs, Environment env,
				Stack<Accumulator> accumulators) {
		this(func, eval,
				Names.name(func.getSignature().getName()),
				(FunctionType) func.getSignature().typeOf(env, true, eval),
				getFormals(func),
				varargs, isDefault(func), hasTestMod(func.getSignature()),
				func.getBody().getStatements(), env, accumulators);
	}

	public RascalFunction(IEvaluator<Result<IValue>> eval, FunctionDeclaration.Expression func, boolean varargs, Environment env,
			Stack<Accumulator> accumulators) {
		this(func, eval,
				Names.name(func.getSignature().getName()),
				(FunctionType) func.getSignature().typeOf(env, true, eval), 
				getFormals(func),
				varargs, isDefault(func), hasTestMod(func.getSignature()),
				Arrays.asList(new Statement[] { ASTBuilder.makeStat("Return", func.getLocation(), ASTBuilder.makeStat("Expression", func.getLocation(), func.getExpression()))}),
				env, accumulators);
	}
	
	private List<Statement> cloneBody() {
		return getAst().clone(body);
	}
	
	@Override
	public RascalFunction cloneInto(Environment env) {
		AbstractAST clone = cloneAst();
		List<Statement> clonedBody = cloneBody();
		// TODO: accumulators are not cloned? @tvdstorm check this out:
		RascalFunction rf = new RascalFunction(clone, getEval(), getName(), getFunctionType(), initializers, hasVarArgs(), isDefault(), isTest(), clonedBody, env, accumulators);
		rf.setPublic(isPublic()); // TODO: should be in constructors
		return rf;
	}

	public RascalFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, String name, FunctionType functionType,
			List<KeywordFormal> initializers,
			boolean varargs, boolean isDefault, boolean isTest, List<Statement> body, Environment env, Stack<Accumulator> accumulators) {
		super(ast, eval, name, functionType, initializers, varargs, isDefault, isTest, env, accumulators);
		this.body = body;
	}
	
	@Override
	public <V extends IValue> Result<IBool> equals(Result<V> that) {
		return that.equalToRascalFunction(this);
	}

	@Override
	public Result<IBool> equalToRascalFunction(RascalFunction that)  {
		return ResultFactory.bool((this == that), ctx);
	}


	@Override
	protected Result<IValue> run() {
		for (Statement stat: body) {
			eval.setCurrentAST(stat);
			stat.interpret(eval);
		}

		
		if(!isVoidFunction){
			throw new MissingReturn(ast);
		}

		return makeResult(TF.voidType(), null, eval);
	}
}