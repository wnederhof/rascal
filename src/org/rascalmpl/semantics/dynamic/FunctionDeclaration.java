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
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
*******************************************************************************/
package org.rascalmpl.semantics.dynamic;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.Expression.Closure;
import org.rascalmpl.ast.FunctionBody;
import org.rascalmpl.ast.FunctionModifier;
import org.rascalmpl.ast.KeywordArgument_Expression;
import org.rascalmpl.ast.Mapping_Expression;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.OptionalComma;
import org.rascalmpl.ast.Signature;
import org.rascalmpl.ast.Tags;
import org.rascalmpl.ast.Visibility;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.result.AbstractFunction;
import org.rascalmpl.interpreter.result.JavaMethod;
import org.rascalmpl.interpreter.result.RascalFunction;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.staticErrors.MissingModifier;
import org.rascalmpl.interpreter.staticErrors.NonAbstractJavaFunction;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;

public abstract class FunctionDeclaration extends
		org.rascalmpl.ast.FunctionDeclaration {

	static public class Abstract extends
			org.rascalmpl.ast.FunctionDeclaration.Abstract {

		public Abstract(ISourceLocation __param1, IConstructor tree, Tags __param2, Visibility __param3,
				Signature __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			boolean varArgs = this.getSignature().getParameters().isVarArgs();

			if (!hasJavaModifier(this)) {
				throw new MissingModifier("java", this);
			}

			AbstractFunction lambda = new JavaMethod(__eval, this, varArgs,
					__eval.getCurrentEnvt(), __eval.__getJavaBridge());
			String name = org.rascalmpl.interpreter.utils.Names.name(this
					.getSignature().getName());
		
			__eval.getCurrentEnvt().storeFunction(name, lambda);
			__eval.getCurrentEnvt().markNameFinal(lambda.getName());
			__eval.getCurrentEnvt().markNameOverloadable(lambda.getName());

			lambda.setPublic(this.getVisibility().isPublic() || this.getVisibility().isDefault());
			return lambda;

		}

	}

	static public class Default extends
			org.rascalmpl.ast.FunctionDeclaration.Default {

		public Default(ISourceLocation __param1, IConstructor tree, Tags __param2, Visibility __param3,
				Signature __param4, FunctionBody __param5) {
			super(__param1, tree, __param2, __param3, __param4, __param5);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			AbstractFunction lambda;
			boolean varArgs = this.getSignature().getParameters().isVarArgs();

			if (hasJavaModifier(this)) {
				throw new NonAbstractJavaFunction(this);
			}

			if (!this.getBody().isDefault()) {
				throw new MissingModifier("java", this);
			}

			lambda = new RascalFunction(__eval, this, varArgs, __eval
					.getCurrentEnvt(), __eval.__getAccumulators());

			__eval.getCurrentEnvt().storeFunction(lambda.getName(), lambda);
			__eval.getCurrentEnvt().markNameFinal(lambda.getName());
			__eval.getCurrentEnvt().markNameOverloadable(lambda.getName());

			lambda.setPublic(this.getVisibility().isPublic() || this.getVisibility().isDefault());
			return lambda;

		}

		@Override
		public Type typeOf(Environment __eval, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return this.getSignature().typeOf(__eval, instantiateTypeParameters, eval);
		}
	}

	static public class Expression extends
			org.rascalmpl.ast.FunctionDeclaration.Expression {

		public Expression(ISourceLocation src, IConstructor node, Tags tags, Visibility visibility,
				Signature signature, org.rascalmpl.ast.Expression expression) {
			super(src, node, tags, visibility, signature, expression);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			AbstractFunction lambda;
			boolean varArgs = this.getSignature().getParameters().isVarArgs();

			if (hasJavaModifier(this)) {
				throw new NonAbstractJavaFunction(this);
			}

			lambda = new RascalFunction(__eval, this, varArgs, __eval
					.getCurrentEnvt(), __eval.__getAccumulators());
			
		
			lambda.setPublic(this.getVisibility().isPublic() || this.getVisibility().isDefault());
			__eval.getCurrentEnvt().markNameFinal(lambda.getName());
			__eval.getCurrentEnvt().markNameOverloadable(lambda.getName());
			
			__eval.getCurrentEnvt().storeFunction(lambda.getName(), lambda);
			
			return lambda;
		}

	}
	

	static public class Sugar extends
		org.rascalmpl.ast.FunctionDeclaration.Sugar {
		

		public Sugar(ISourceLocation src, IConstructor node, Tags tags, Visibility visibility,
				org.rascalmpl.ast.Type typeRhs, Name name, org.rascalmpl.ast.Expression patternLhs,
				org.rascalmpl.ast.Type typeLhs, org.rascalmpl.ast.Expression patternRhs) {
			super(src, node, tags, visibility, typeRhs, name, patternLhs, typeLhs, patternRhs);
		}
		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			// typeLhs is actually the type on the RHS, which defines the type for the expression on the LHS.
			Declaration.Resugarable.declareSugarAnnotations(__eval, getTypeRhs(), this.getLocation());
			AbstractFunction lambdaDesugar = constructExpansionFunction(__eval, getTypeRhs(), getTypeLhs(), getPatternLhs(), getPatternRhs());
			return lambdaDesugar;
		}
		
		private RascalFunction declareFunction(IEvaluator<Result<IValue>> __eval, RascalFunction lambda) {
			lambda.setPublic(this.getVisibility().isPublic() || this.getVisibility().isDefault());
			__eval.getCurrentEnvt().markNameFinal(lambda.getName());
			__eval.getCurrentEnvt().markNameOverloadable(lambda.getName());
			__eval.getCurrentEnvt().storeFunction(lambda.getName(), lambda);
			return lambda;
		}
		
		private Signature createSignature(org.rascalmpl.ast.Type type, org.rascalmpl.ast.Expression signaturePattern, Collection<org.rascalmpl.ast.Expression> additionalParameters) {
			OptionalComma optionalComma = new OptionalComma.Lexical(src, null, ",");
			List<org.rascalmpl.ast.Expression> listOfFormals = new LinkedList<>();
			listOfFormals.add(signaturePattern);
			listOfFormals.addAll(additionalParameters);
			
			org.rascalmpl.ast.Formals formals = ASTBuilder.make("Formals", "Default", getLocation(), listOfFormals);
			
			org.rascalmpl.ast.KeywordFormals keywordFormals = ASTBuilder.make("KeywordFormals", "Default", getLocation(),
					optionalComma, new LinkedList<org.rascalmpl.ast.KeywordFormal>());
			
			org.rascalmpl.ast.Parameters parameters = ASTBuilder.make("Parameters", "Default",
					getLocation(), formals, keywordFormals);
			
			org.rascalmpl.ast.FunctionModifiers modifiers = ASTBuilder.make("FunctionModifiers", "Modifierlist",
					getLocation(), new LinkedList<>());
			
			org.rascalmpl.ast.Signature signature =  
					ASTBuilder.make("Signature", "NoThrows",
							getLocation(), modifiers,
							type, getName(), parameters);
			return signature;
		}
		
		/* Visit all elements in the given expression, collecting the names. */
		private LinkedHashSet<String> findVariables(org.rascalmpl.ast.Expression p) {
			LinkedHashSet<String> vars = new LinkedHashSet<String>();
			// TODO needs appropriate checking to see if all variables all collected.
			if (p.hasElements()) {
				for (org.rascalmpl.ast.Expression pC : p.getElements()) {
					vars.addAll(findVariables(pC));
				}
			}
			if (p.hasArguments()) {
				for (org.rascalmpl.ast.Expression pC : p.getArguments()) {
					vars.addAll(findVariables(pC));
				}
			}
			if (p.hasElements0()) {
				for (org.rascalmpl.ast.Expression pC : p.getElements0()) {
					vars.addAll(findVariables(pC));
				}
			}
			if (p.hasArgument()) {
				vars.addAll(findVariables(p.getArgument()));
			}
			if (p.hasDefinitions()) {
				vars.addAll(findVariables(p.getDefinitions()));
			}
			if (p.hasMappings()) {
				for (Mapping_Expression pC : p.getMappings()) {
					if (pC.hasFrom() && pC.hasTo()) {
						vars.addAll(findVariables(pC.getFrom()));
						vars.addAll(findVariables(pC.getTo()));
					}
				}
			}
			if (p.hasKeywordArguments() && p.getKeywordArguments().hasKeywordArgumentList()) {
				for (KeywordArgument_Expression pC : p.getKeywordArguments().getKeywordArgumentList()) {
					// Note that we deliberately ignore the name of keyword argument expressions.
					vars.addAll(findVariables(pC.getExpression()));
				}
			}
			if (p.hasQualifiedName()) {
				vars.add(org.rascalmpl.interpreter.utils.Names.fullName(p.getQualifiedName()));
			}
			
			if (p.isTypedVariable()) {
				vars.add(org.rascalmpl.interpreter.utils.Names.name(p.getName()));
			}
			return vars;
		}
		
		private Closure constructUnexpansionClosure(IEvaluator<Result<IValue>> __eval, org.rascalmpl.ast.Type type,
				org.rascalmpl.ast.Expression signaturePattern, org.rascalmpl.ast.Expression expression,
				List<org.rascalmpl.ast.Expression> unusedVariableExps) {
			Signature signature = createSignature(type, signaturePattern, unusedVariableExps);
			org.rascalmpl.ast.Parameters parameters = signature.getParameters();
			AbstractAST ret = ASTBuilder.makeStat("Return", getLocation(), ASTBuilder.makeStat("Expression", src, expression));
			List<AbstractAST> statements = new LinkedList<AbstractAST>();
			statements.add(ret);
			return ASTBuilder.makeExp("Closure", getLocation(), type, parameters, statements);
		}

		private AbstractFunction constructExpansionFunction(final IEvaluator<Result<IValue>> __eval,
				org.rascalmpl.ast.Type type, org.rascalmpl.ast.Type toType,
				org.rascalmpl.ast.Expression signaturePattern, org.rascalmpl.ast.Expression expression) {
			ISourceLocation src = this.getLocation();
			
			// We write LinkedHashSet instead of Set, to stress the fact that the sets need to be ordered!
			LinkedHashSet<String> varsSig = findVariables(signaturePattern);
			LinkedHashSet<String> varsPat = findVariables(expression);
			Signature signature = createSignature(type, signaturePattern,
					new LinkedHashSet<org.rascalmpl.ast.Expression>());
			List<org.rascalmpl.ast.Expression> unusedVariableExps = new LinkedList<>();
			for (String s : varsSig) {
				if (!varsPat.contains(s)) {
					unusedVariableExps.add(ASTBuilder.makeExp("QualifiedName", src,
							org.rascalmpl.interpreter.utils.Names.toQualifiedName(s, src)));
				}
			}
			
			AbstractAST ret = ASTBuilder.makeStat("Return", src,
					ASTBuilder.makeStat("Expression", src,
							ASTBuilder.makeExp("SetAnnotation", src,
								ASTBuilder.makeExp("FieldUpdate", src, expression,
										Names.toName("unusedVariables", src),
											ASTBuilder.makeExp("List", src, unusedVariableExps)),
								Names.toName("unexpandFn", src), constructUnexpansionClosure(__eval, toType, expression,
										signaturePattern, unusedVariableExps))));
			List<AbstractAST> sl = Arrays.<AbstractAST> asList(ret);
			AbstractAST body = ASTBuilder.make("FunctionBody", "Default", src, sl);
			FunctionDeclaration.Default func = ASTBuilder.make("FunctionDeclaration", "Default", src, getTags(),
					getVisibility(), signature, body);
			final AbstractAST thisAst = this;
			
			RascalFunction lambda = new RascalFunction(__eval, func, false, __eval.getCurrentEnvt(),
					__eval.__getAccumulators()) {
				
				public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
					eval.setCurrentAST(thisAst);
					eval.notifyAboutSuspension(thisAst);	
					return super.call(actualTypes, actuals, keyArgValues);
				}
			};
			return declareFunction(__eval, lambda);
		}

	}
	
	static public class Conditional extends
	org.rascalmpl.ast.FunctionDeclaration.Conditional {

		public Conditional(ISourceLocation src, IConstructor node, Tags tags, Visibility visibility,
				Signature signature, org.rascalmpl.ast.Expression expression, java.util.List<org.rascalmpl.ast.Expression> conditions) {
			super(src, node, tags, visibility, signature, expression, conditions);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
					
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);	
			
			AbstractFunction lambda;
			boolean varArgs = this.getSignature().getParameters().isVarArgs();

			if (hasJavaModifier(this)) {
				throw new NonAbstractJavaFunction(this);
			}

			ISourceLocation src = this.getLocation();
			AbstractAST ret = ASTBuilder.makeStat("Return", src, ASTBuilder.makeStat("Expression", src, getExpression()));
			AbstractAST fail = ASTBuilder.makeStat("Fail", src, ASTBuilder.make("Target", "Labeled", src, getSignature().getName()));
			AbstractAST ite = ASTBuilder.makeStat("IfThenElse", src, ASTBuilder.make("Label", "Empty", src), getConditions(), ret, fail);
			List<AbstractAST> sl = Arrays.<AbstractAST>asList(ite);
			AbstractAST body = ASTBuilder.make("FunctionBody", "Default", src, sl);
			FunctionDeclaration.Default func = ASTBuilder.make("FunctionDeclaration", "Default", src, getTags(), getVisibility(), getSignature(), body);
			
			lambda = new RascalFunction(__eval, func, varArgs, __eval
					.getCurrentEnvt(), __eval.__getAccumulators());

			__eval.getCurrentEnvt().storeFunction(lambda.getName(), lambda);
			__eval.getCurrentEnvt().markNameFinal(lambda.getName());
			__eval.getCurrentEnvt().markNameOverloadable(lambda.getName());

			lambda.setPublic(this.getVisibility().isPublic() || this.getVisibility().isDefault());
			return lambda;
		}

}

	public static boolean hasJavaModifier(
			org.rascalmpl.ast.FunctionDeclaration func) {
		List<FunctionModifier> mods = func.getSignature().getModifiers()
				.getModifiers();
		for (FunctionModifier m : mods) {
			if (m.isJava()) {
				return true;
			}
		}

		return false;
	}
	
	public static boolean hasTestModifier(
			org.rascalmpl.ast.FunctionDeclaration func) {
		List<FunctionModifier> mods = func.getSignature().getModifiers()
				.getModifiers();
		for (FunctionModifier m : mods) {
			if (m.isTest()) {
				return true;
			}
		}

		return false;
	}

	public FunctionDeclaration(ISourceLocation __param1, IConstructor tree) {
		super(__param1, tree);
	}
}
