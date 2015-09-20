/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.semantics.dynamic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.CommonKeywordParameters;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.Tags;
import org.rascalmpl.ast.UserType;
import org.rascalmpl.ast.Variant;
import org.rascalmpl.ast.Visibility;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.staticErrors.RedeclaredVariable;
import org.rascalmpl.interpreter.staticErrors.UnexpectedType;
import org.rascalmpl.interpreter.staticErrors.UnsupportedOperation;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;

public abstract class Declaration extends org.rascalmpl.ast.Declaration {

	static public class Alias extends org.rascalmpl.ast.Declaration.Alias {

		public Alias(ISourceLocation __param1, IConstructor tree, Tags __param2, Visibility __param3,
				UserType __param4, org.rascalmpl.ast.Type __param5) {
			super(__param1, tree, __param2, __param3, __param4, __param5);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.__getTypeDeclarator().declareAlias(this,
					__eval.getCurrentEnvt());
			return org.rascalmpl.interpreter.result.ResultFactory.nothing();

		}

	}

	static public class Annotation extends
			org.rascalmpl.ast.Declaration.Annotation {

		public Annotation(ISourceLocation __param1, IConstructor tree, Tags __param2, Visibility __param3,
				org.rascalmpl.ast.Type __param4,
				org.rascalmpl.ast.Type __param5, Name __param6) {
			super(__param1, tree, __param2, __param3, __param4, __param5, __param6);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			Type annoType = getAnnoType().typeOf(__eval.getCurrentEnvt(), true, __eval);
			String name = org.rascalmpl.interpreter.utils.Names.name(this
					.getName());

			Type onType = getOnType().typeOf(__eval.getCurrentEnvt(), true, __eval);
			
			if (onType.isAbstractData() || onType.isConstructor() || onType.isNode()) {
				__eval.getCurrentModuleEnvironment().declareAnnotation(onType,
						name, annoType);
			} else {
				throw new UnsupportedOperation("Can only declare annotations on node and ADT types",getOnType());
			}

			return org.rascalmpl.interpreter.result.ResultFactory.nothing();
		}

	}
	
	static public class Resugarable extends
			org.rascalmpl.ast.Declaration.Resugarable {
		
		public Resugarable(ISourceLocation src, IConstructor node, Tags tags, Visibility visibility,
				org.rascalmpl.ast.Type onType) {
			super(src, node, tags, visibility, onType);
			// TODO Auto-generated constructor stub
		}
		
		public static void declareSugarAnnotations(IEvaluator<Result<IValue>> __eval, org.rascalmpl.ast.Type onTypeAst, ISourceLocation loc) {
			Type onType = onTypeAst.typeOf(__eval.getCurrentEnvt(), true, __eval);
			if (onType.isAbstractData() || onType.isConstructor() || onType.isNode()) {
				__eval.getCurrentModuleEnvironment().declareAnnotation(onType,
						"unexpandFn", TF.valueType());
				__eval.getCurrentModuleEnvironment().declareAnnotation(onType,
						"unexpansionFailed", TF.valueType());
				__eval.getCurrentModuleEnvironment().declareAnnotation(onType,
						"desugarVariables", TF.listType(TF.mapType(TF.stringType(), TF.valueType())));
				__eval.getCurrentModuleEnvironment().declareAnnotation(onType,
						"__SUGAR_UUID", TF.stringType());
			} else {
				throw new UnsupportedOperation("Can only declare annotations on node and ADT types", onTypeAst);
			}
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			declareSugarAnnotations(__eval, getOnType(), this.getLocation());
			return org.rascalmpl.interpreter.result.ResultFactory.nothing();
		}
		
	}

	static public class Data extends org.rascalmpl.ast.Declaration.Data {

		public Data(ISourceLocation __param1, IConstructor tree, Tags __param2, Visibility __param3,
				UserType __param4, CommonKeywordParameters __param5, List<Variant> __param6) {
			super(__param1, tree, __param2, __param3, __param4, __param5, __param6);
		}
		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			__eval.__getTypeDeclarator().declareConstructor(this, __eval.getCurrentEnvt());
			return ResultFactory.nothing();
		}

	}

	static public class DataAbstract extends
			org.rascalmpl.ast.Declaration.DataAbstract {

		public DataAbstract(ISourceLocation __param1, IConstructor tree, Tags __param2, Visibility __param3,
				UserType __param4, CommonKeywordParameters __param5) {
			super(__param1, tree, __param2, __param3, __param4, __param5);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			eval.__getTypeDeclarator().declareAbstractADT(this,
					eval.getCurrentEnvt());
			return org.rascalmpl.interpreter.result.ResultFactory.nothing();
		}

	}

	static public class Function extends org.rascalmpl.ast.Declaration.Function {

		public Function(ISourceLocation __param1, IConstructor tree, FunctionDeclaration __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			return this.getFunctionDeclaration().interpret(__eval);

		}

	}

	static public class Variable extends org.rascalmpl.ast.Declaration.Variable {

		public Variable(ISourceLocation __param1, IConstructor tree, Tags __param2, Visibility __param3,
				org.rascalmpl.ast.Type __param4,
				List<org.rascalmpl.ast.Variable> __param5) {
			super(__param1, tree, __param2, __param3, __param4, __param5);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			Result<IValue> r = ResultFactory.nothing();
			eval.setCurrentAST(this);

			for (org.rascalmpl.ast.Variable var : this.getVariables()) {
				Type declaredType = getType().typeOf(eval.getCurrentEnvt(), true, eval);

				if (var.isInitialized()) {
					Result<IValue> v = var.getInitial().interpret(eval);

					if (!eval.getCurrentEnvt().declareVariable(declaredType, var.getName())) {
						throw new RedeclaredVariable(Names.name(var.getName()), var);
					}

					if (v.getType().isSubtypeOf(declaredType)) {
						// TODO: do we actually want to instantiate the locally
						// bound type parameters?
						Map<Type, Type> bindings = new HashMap<Type, Type>();
						declaredType.match(v.getType(), bindings);
						declaredType = declaredType.instantiate(bindings);
						r = ResultFactory.makeResult(declaredType, v.getValue(), eval);
						eval.getCurrentModuleEnvironment().storeVariable(var.getName(), r);
					} else {
						throw new UnexpectedType(declaredType,
								v.getType(), var);
					}
				} else {
					eval.getCurrentModuleEnvironment().storeVariable(var.getName(), ResultFactory.nothing(declaredType));
				}
			}

			r.setPublic(this.getVisibility().isPublic());
			return r;
		}
	}

	public Declaration(ISourceLocation __param1, IConstructor tree) {
		super(__param1, tree);
	}
}
