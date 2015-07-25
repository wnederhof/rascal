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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.FunctionBody;
import org.rascalmpl.ast.FunctionModifier;
import org.rascalmpl.ast.Name;
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
		
		public Sugar(ISourceLocation src, IConstructor node, Tags tags, Visibility visibility, Name name,
				org.rascalmpl.ast.Type typeLhs, org.rascalmpl.ast.Expression patternLhs, org.rascalmpl.ast.Type typeRhs,
				org.rascalmpl.ast.Expression patternRhs) {
			super(src, node, tags, visibility, name, typeLhs, patternLhs, typeRhs, patternRhs);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			// Yeehaw!! :-D
			System.out.println("Yolo!");
			AbstractFunction lambdaDesugar = constructTransformationFunction(__eval, getTypeRhs(), getPatternLhs(), getPatternRhs());
			
			// Resugaring.
			//constructTransformationFunction(__eval, getTypeLhs(), getPatternRhs(), getPatternLhs());
			return lambdaDesugar;
		}
		
		private RascalFunction declareFunction(IEvaluator<Result<IValue>> __eval, RascalFunction lambda) {
			lambda.setPublic(this.getVisibility().isPublic() || this.getVisibility().isDefault());
			__eval.getCurrentEnvt().markNameFinal(lambda.getName());
			__eval.getCurrentEnvt().markNameOverloadable(lambda.getName());
			__eval.getCurrentEnvt().storeFunction(lambda.getName(), lambda);
			return lambda;
		}
		
		private Signature createSignature(org.rascalmpl.ast.Type type, org.rascalmpl.ast.Expression signaturePattern) {
			
			// -.-'
			org.rascalmpl.ast.OptionalComma optionalComma = null; //ASTBuilder.make("OptionalComma", "Lexical", getSignature().getLocation(), "");
			
			List<org.rascalmpl.ast.Expression> listOfFormals = new LinkedList<>();
			listOfFormals.add(signaturePattern);
			
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
		private Set<String> findVariables(org.rascalmpl.ast.Expression p) {
			Set<String> vars = new HashSet<String>();
			
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
			
			if (p.hasQualifiedName()) {
				vars.add(org.rascalmpl.interpreter.utils.Names.fullName(p.getQualifiedName()));
			}
			return vars;
		}
		
		private AbstractFunction constructTransformationFunction(
				IEvaluator<Result<IValue>> __eval, org.rascalmpl.ast.Type type,
				org.rascalmpl.ast.Expression signaturePattern,
				org.rascalmpl.ast.Expression expression) {
			ISourceLocation src = this.getLocation();
			
			Signature signature = createSignature(type, signaturePattern);
			
			Set<String> varsSig = findVariables(signaturePattern);
			Set<String> varsPat = findVariables(expression);
			
			List<org.rascalmpl.ast.Mapping_Expression> mappingExpressions = new LinkedList<>();
			
			for (String s : varsSig) {
				if (!varsPat.contains(s)) {
					System.out.println("Found one! " + s);
					
					org.rascalmpl.ast.Expression from = ASTBuilder.makeExp("Literal", src,
						ASTBuilder.make("Literal", "String", src, 
								ASTBuilder.make("StringLiteral", "NonInterpolated", src,
										ASTBuilder.make("StringConstant", "Lexical", src, "\"" + s + "\""))));
					//if (1==1) return null;
					org.rascalmpl.ast.Expression to = ASTBuilder.makeExp("QualifiedName", src, org.rascalmpl.interpreter.utils.Names.toQualifiedName(s, src));
					
					mappingExpressions.add(ASTBuilder.make("Mapping_Expression", "Default", src, from, to));
				}
			}
			
			// Annotate ...
			// AbstractAST anno = ASTBuilder.makeStat(cons, src, args)
			
			// makeExp was makeStat??
			AbstractAST ret = ASTBuilder.makeStat("Return", src, 
					ASTBuilder.makeStat("Expression", src,
						ASTBuilder.makeExp("SetAnnotation", src, expression, Names.toName("unusedVariables", src),
								ASTBuilder.makeExp("Map", src, mappingExpressions))));
			
			List<AbstractAST> sl = Arrays.<AbstractAST>asList(ret);
			AbstractAST body = ASTBuilder.make("FunctionBody", "Default", src, sl);
			
			FunctionDeclaration.Default func = ASTBuilder.make("FunctionDeclaration", "Default", src, getTags(), getVisibility(), signature, body);
			RascalFunction lambda = new RascalFunction(__eval, func, false, __eval
					.getCurrentEnvt(), __eval.__getAccumulators());
			
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
