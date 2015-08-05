/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Bas Basten - Bas.Basten@cwi.nl (CWI)
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
 *   * Anastasia Izmaylova - A.Izmaylova@cwi.nl - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
*******************************************************************************/
package org.rascalmpl.semantics.dynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.Case;
import org.rascalmpl.ast.Field;
import org.rascalmpl.ast.KeywordArgument_Expression;
import org.rascalmpl.ast.KeywordArguments_Expression;
import org.rascalmpl.ast.KeywordFormal;
import org.rascalmpl.ast.Label;
import org.rascalmpl.ast.Mapping_Expression;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.Parameters;
import org.rascalmpl.ast.Statement;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.SubstitutionEvaluator;
import org.rascalmpl.interpreter.TraversalEvaluator;
import org.rascalmpl.interpreter.TraversalEvaluator.CaseBlockList;
import org.rascalmpl.interpreter.TraversalEvaluator.DIRECTION;
import org.rascalmpl.interpreter.TraversalEvaluator.FIXEDPOINT;
import org.rascalmpl.interpreter.TraversalEvaluator.PROGRESS;
import org.rascalmpl.interpreter.TypeDeclarationEvaluator;
import org.rascalmpl.interpreter.TypeReifier;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.callbacks.IConstructorDeclared;
import org.rascalmpl.interpreter.control_exceptions.InterruptException;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.control_exceptions.Throw;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.AndResult;
import org.rascalmpl.interpreter.matching.AntiPattern;
import org.rascalmpl.interpreter.matching.BasicBooleanResult;
import org.rascalmpl.interpreter.matching.ConcreteListVariablePattern;
import org.rascalmpl.interpreter.matching.DescendantPattern;
import org.rascalmpl.interpreter.matching.EnumeratorResult;
import org.rascalmpl.interpreter.matching.EquivalenceResult;
import org.rascalmpl.interpreter.matching.GuardedPattern;
import org.rascalmpl.interpreter.matching.IBooleanResult;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.ListPattern;
import org.rascalmpl.interpreter.matching.MatchResult;
import org.rascalmpl.interpreter.matching.MultiVariablePattern;
import org.rascalmpl.interpreter.matching.NegativePattern;
import org.rascalmpl.interpreter.matching.NodePattern;
import org.rascalmpl.interpreter.matching.NotResult;
import org.rascalmpl.interpreter.matching.OrResult;
import org.rascalmpl.interpreter.matching.QualifiedNamePattern;
import org.rascalmpl.interpreter.matching.ReifiedTypePattern;
import org.rascalmpl.interpreter.matching.SetPattern;
import org.rascalmpl.interpreter.matching.TuplePattern;
import org.rascalmpl.interpreter.matching.TypedMultiVariablePattern;
import org.rascalmpl.interpreter.matching.TypedVariablePattern;
import org.rascalmpl.interpreter.matching.VariableBecomesPattern;
import org.rascalmpl.interpreter.result.AbstractFunction;
import org.rascalmpl.interpreter.result.BoolResult;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.RascalFunction;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.staticErrors.ArgumentsMismatch;
import org.rascalmpl.interpreter.staticErrors.NonVoidTypeRequired;
import org.rascalmpl.interpreter.staticErrors.SyntaxError;
import org.rascalmpl.interpreter.staticErrors.UndeclaredVariable;
import org.rascalmpl.interpreter.staticErrors.UnexpectedType;
import org.rascalmpl.interpreter.staticErrors.UnguardedIt;
import org.rascalmpl.interpreter.staticErrors.UninitializedPatternMatch;
import org.rascalmpl.interpreter.staticErrors.UninitializedVariable;
import org.rascalmpl.interpreter.staticErrors.UnsupportedOperation;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.NonTerminalType;
import org.rascalmpl.interpreter.types.OverloadedFunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Cases;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.interpreter.utils.RuntimeExceptionFactory;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.parser.gtd.exception.ParseError;
import org.rascalmpl.semantics.dynamic.QualifiedName.Default;
import org.rascalmpl.values.uptr.RascalValueFactory;
import org.rascalmpl.values.uptr.SymbolAdapter;

public abstract class Expression extends org.rascalmpl.ast.Expression {
  private static final Name IT = ASTBuilder.makeLex("Name", null, "<it>");
  
  	/**
  	 * This function creates the CaseBlockList for desugaring and resugaring.
  	 */
	private static CaseBlockList createSugarMatchingBlockList(ISourceLocation src, org.rascalmpl.ast.QualifiedName qualifiedName, org.rascalmpl.ast.Expression replacementFn) {
		org.rascalmpl.ast.Expression pattern = 
				ASTBuilder.makeExp("QualifiedName", src, Names.toQualifiedName("_ANY", src));
		org.rascalmpl.ast.Replacement replacement = ASTBuilder.make("Replacement", "Unconditional", src,
				replacementFn);
		Case c = ASTBuilder.make("Case", "PatternWithAction", src, 
				ASTBuilder.make("PatternWithAction", "Replacing", src, pattern, replacement));
		return new CaseBlockList(Arrays.asList(new Cases.SugarBlock(c)));
	}
	
	static public class Addition extends org.rascalmpl.ast.Expression.Addition {

		public Addition(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}
		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.add(right);

		}

	}
	
	static public class Resugar extends org.rascalmpl.ast.Expression.Resugar {

		public Resugar(ISourceLocation src, IConstructor node, org.rascalmpl.ast.Expression expression) {
			super(src, node, expression);
		}
		
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);
			
			ISourceLocation src = this.getLocation();
			CaseBlockList blocks = createSugarMatchingBlockList(src, Names.toQualifiedName("_ANY", src), 
					ASTBuilder.makeExp("Unexpand", src,
							ASTBuilder.makeExp("QualifiedName", src,
									Names.toQualifiedName("_ANY", src))));
			Result<IValue> subject = this.getExpression().interpret(__eval);
			TraversalEvaluator te = new TraversalEvaluator(__eval);
			try {
				__eval.__pushTraversalEvaluator(te);
				IValue val = te.traverse(subject.getValue(),
						blocks, DIRECTION.BottomUp,
						PROGRESS.Continuing, FIXEDPOINT.No);
				if (!val.getType().isSubtypeOf(subject.getType())) {
				  // this is not a static error but an extra run-time sanity check
				  throw new ImplementationError("this should really never happen",
				      new UnexpectedType(subject.getType(), val.getType(), this));
				}
				return org.rascalmpl.interpreter.result.ResultFactory.makeResult(subject.getType(),
						val, __eval);
			} finally {
				__eval.__popTraversalEvaluator();
			}
		}
		
	}
	
	static public class Desugar extends org.rascalmpl.ast.Expression.Desugar {

		public Desugar(ISourceLocation src, IConstructor node, org.rascalmpl.ast.QualifiedName unexpandFn,
				org.rascalmpl.ast.Expression expression) {
			super(src, node, unexpandFn, expression);
		}
		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);
			
			ISourceLocation src = this.getLocation();
			CaseBlockList blocks = createSugarMatchingBlockList(src, Names.toQualifiedName("_ANY", src), 
				ASTBuilder.makeExp("CallOrTree", src,
						ASTBuilder.makeExp("QualifiedName", src, getUnexpandFn()),
						Arrays.asList(ASTBuilder.makeExp("QualifiedName", src, Names.toQualifiedName("_ANY", src))),
						ASTBuilder.make("KeywordArguments_Expression", src, null, Arrays.asList())));
						
			Result<IValue> subject = this.getExpression().interpret(__eval);
			TraversalEvaluator te = new TraversalEvaluator(__eval);
			try {
				__eval.__pushTraversalEvaluator(te);
				IValue val = te.traverse(subject.getValue(),
						blocks, DIRECTION.TopDown,
						PROGRESS.Continuing, FIXEDPOINT.No);
				if (!val.getType().isSubtypeOf(subject.getType())) {
				  // this is not a static error but an extra run-time sanity check
				  throw new ImplementationError("this should really never happen",
				      new UnexpectedType(subject.getType(), val.getType(), this));
				}
				return org.rascalmpl.interpreter.result.ResultFactory.makeResult(subject.getType(),
						val, __eval);
			} finally {
				__eval.__popTraversalEvaluator();
			}
		}
	}
	
	static public class Substitute extends org.rascalmpl.ast.Expression.Substitute {

		public Substitute(ISourceLocation src, IConstructor node, org.rascalmpl.ast.Expression pattern,
				org.rascalmpl.ast.Expression expression) {
			super(src, node, pattern, expression);
		}
		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			return SubstitutionEvaluator.substitute(getPattern(), getExpression().interpret(eval).getValue(), eval);
		}
		
	}
	
	static public class Unexpand extends org.rascalmpl.ast.Expression.Unexpand {

		public Unexpand(ISourceLocation src, IConstructor node, org.rascalmpl.ast.Expression expression) {
			super(src, node, expression);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);
			
			Result<IValue> expressionValue = getExpression().interpret(__eval);
			Result<AbstractFunction> lambda;
			try {
				lambda = expressionValue.fieldAccess("unexpandFn", __eval.getCurrentEnvt().getStore());
			} catch(Throw e) {
				//e.printStackTrace(); // TODO remove.
				return expressionValue;
			} catch(Exception e) {
				e.printStackTrace();
				return expressionValue;
			}
			java.util.List<Type> mTypes = new LinkedList<Type>();
			java.util.List<IValue> mValues = new LinkedList<IValue>();
			mTypes.add(expressionValue.getValue().getType());
			mValues.add(expressionValue.getValue());
			/* org.rascalmpl.interpreter.result.ListResult vals = (org.rascalmpl.interpreter.result.ListResult) (Result<?>) expressionValue.getAnnotation("unusedVariables", __eval.getCurrentEnvt());
			for (IValue v : vals.getValue()) {
				mTypes.add(v.getType());
				mValues.add(v);
			}*/
			try {
				return lambda.getValue().call(
						mTypes.toArray(new Type[mTypes.size()]),
						mValues.toArray(new IValue[mValues.size()]),
						new HashMap<String, IValue>());
			} catch(Throw e) {
				// TO DO: Add [@unexpansionFailed = true]?
				// e.printStackTrace(); // TODO remove.
				return expressionValue;
			} catch(Exception e) {
				e.printStackTrace();
				return expressionValue;
			}
		}
	}

	static public class All extends org.rascalmpl.ast.Expression.All {

		public All(ISourceLocation __param1, IConstructor tree,
				java.util.List<org.rascalmpl.ast.Expression> __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			return new BasicBooleanResult(__eval, this);

		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Result interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);	
			
			java.util.List<org.rascalmpl.ast.Expression> producers = this
					.getGenerators();
			int size = producers.size();
			IBooleanResult[] gens = new IBooleanResult[size];
			Environment[] olds = new Environment[size];
			Environment old = __eval.getCurrentEnvt();
			int i = 0;

			try {
				olds[0] = __eval.getCurrentEnvt();
				__eval.pushEnv();
				gens[0] = producers.get(0).getBacktracker(__eval);
				gens[0].init();

				while (i >= 0 && i < size) {
					if (__eval.isInterrupted()) {
						throw new InterruptException(__eval.getStackTrace(), __eval.getCurrentAST().getLocation());
					}
					if (gens[i].hasNext()) {
						if (!gens[i].next()) {
							return new BoolResult(TF.boolType(), __eval
									.__getVf().bool(false), __eval);
						}

						if (i == size - 1) {
							__eval.unwind(olds[i]);
							__eval.pushEnv();
						} else {
							i++;
							gens[i] = producers.get(i).getBacktracker(__eval);
							gens[i].init();
							olds[i] = __eval.getCurrentEnvt();
							__eval.pushEnv();
						}
					} else {
						__eval.unwind(olds[i]);
						i--;
					}
				}
			} finally {
				__eval.unwind(old);
			}

			return new BoolResult(TF.boolType(), __eval.__getVf().bool(true),
					__eval);

		}

	}

	static public class And extends org.rascalmpl.ast.Expression.And {

		public And(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new AndResult(__eval, this.getLhs().buildBacktracker(__eval), this.getRhs().buildBacktracker(__eval));
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			return evalBooleanExpression(this, __eval);
		}

	}

	static public class Anti extends org.rascalmpl.ast.Expression.Anti {

		public Anti(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext __eval) {
			IMatchingResult absPat = getPattern().buildMatcher(__eval);
			return new AntiPattern(__eval, this, absPat);
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return TypeFactory.getInstance().voidType();
		}
	}

	static public class Any extends org.rascalmpl.ast.Expression.Any {

		public Any(ISourceLocation __param1, IConstructor tree,
				java.util.List<org.rascalmpl.ast.Expression> __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Result interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			java.util.List<org.rascalmpl.ast.Expression> generators = this
					.getGenerators();
			int size = generators.size();
			IBooleanResult[] gens = new IBooleanResult[size];

			int i = 0;
			gens[0] = generators.get(0).getBacktracker(__eval);
			gens[0].init();
			while (i >= 0 && i < size) {
				if (__eval.isInterrupted()) {
					throw new InterruptException(__eval.getStackTrace(), __eval.getCurrentAST().getLocation());
				}
				if (gens[i].hasNext() && gens[i].next()) {
					if (i == size - 1) {
						return new BoolResult(TF.boolType(), __eval.__getVf()
								.bool(true), __eval);
					}

					i++;
					gens[i] = generators.get(i).getBacktracker(__eval);
					gens[i].init();
				} else {
					i--;
				}
			}
			return new BoolResult(TF.boolType(), __eval.__getVf().bool(false),
					__eval);

		}

	}

	static public class Bracket extends org.rascalmpl.ast.Expression.Bracket {

		public Bracket(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return this.getExpression().buildBacktracker(__eval);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext __eval) {
			return this.getExpression().buildMatcher(__eval);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return this.getExpression().interpret(__eval);
		}
	}

	static public class CallOrTree extends
			org.rascalmpl.ast.Expression.CallOrTree {

		private Result<IValue> cachedPrefix = null;
		private boolean registeredCacheHandler = false;
		private Type cachedConstructorType = null;
		private boolean registeredTypeCacheHandler = false;

		public CallOrTree(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				java.util.List<org.rascalmpl.ast.Expression> __param3, KeywordArguments_Expression __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			org.rascalmpl.ast.Expression nameExpr = getExpression();
		
			if (nameExpr.isQualifiedName()) {
				if (cachedConstructorType == null) {
					registerTypeCacheHandler(eval);
					cachedConstructorType  = computeConstructorType(eval, nameExpr);
				}
				 
				return new NodePattern(eval, this, null, nameExpr.getQualifiedName(), cachedConstructorType, visitArguments(eval), visitKeywordArguments(eval));
			}

			return new NodePattern(eval, this, nameExpr.buildMatcher(eval), null, TF.nodeType(), visitArguments(eval), visitKeywordArguments(eval));
		}

		private java.util.Map<String, IMatchingResult> visitKeywordArguments(IEvaluatorContext eval) {
			java.util.Map<String,IMatchingResult> result = new HashMap<>();
			KeywordArguments_Expression keywordArgs;

			if (hasKeywordArguments() && (keywordArgs = getKeywordArguments()).isDefault()) {
				for (KeywordArgument_Expression kwa : keywordArgs.getKeywordArgumentList()) {
					result.put(Names.name(kwa.getName()), kwa.getExpression().buildMatcher(eval));
				}
			}

			return result;
		}

    private Type computeConstructorType(IEvaluatorContext eval,
				org.rascalmpl.ast.Expression nameExpr) {
			java.util.List<AbstractFunction> functions = new LinkedList<AbstractFunction>();
			String cons = Names.consName(nameExpr.getQualifiedName());
			Type adt = eval.getCurrentEnvt().lookupAbstractDataType(Names.moduleName(nameExpr.getQualifiedName()));
			if (adt != null) {
				eval.getCurrentEnvt().getAllFunctions(adt, cons, functions);
			}
			else {
			  eval.getCurrentEnvt().getAllFunctions(cons, functions);
			}
			
			if (functions.isEmpty()) {
			  return null;
//				throw new UndeclaredVariable(Names.fullName(nameExpr.getQualifiedName()), this);
			}
			
			Type signature = getArgumentTypes(eval);
			Type constructorType = TF.nodeType();
			
			for (AbstractFunction candidate : functions) {
				if (candidate.getReturnType().isAbstractData() && !candidate.getReturnType().isBottom() && candidate.match(signature)) {
					Type decl = eval.getCurrentEnvt().getConstructor(candidate.getReturnType(), cons, signature);
					if (decl != null) {
						constructorType = decl;
					}
				}
			}
			return constructorType;
		}

		private Type getArgumentTypes(IEvaluatorContext eval) {
			java.util.List<IMatchingResult> args = visitArguments(eval);
			Type[] argTypes = new Type[args.size()];
			for (int i = 0; i < argTypes.length; i++) {
				argTypes[i] = args.get(i).getType(eval.getCurrentEnvt(), null);
			}
			Type signature = TF.tupleType(argTypes);
			return signature;
		}

		private void registerCacheHandler(IEvaluatorContext eval) {
			if (!registeredCacheHandler) {
				eval.getEvaluator().registerConstructorDeclaredListener(
						new IConstructorDeclared() {
							public void handleConstructorDeclaredEvent() {
								cachedPrefix = null;
								registeredCacheHandler = false;
							}
						});
				registeredCacheHandler = true;
			}
		}
		
		private void registerTypeCacheHandler(IEvaluatorContext eval) {
			if (!registeredTypeCacheHandler) {
				eval.getEvaluator().registerConstructorDeclaredListener(
						new IConstructorDeclared() {
							public void handleConstructorDeclaredEvent() {
								cachedConstructorType = null;
								registeredTypeCacheHandler = false;
							}
						});
				registeredTypeCacheHandler = true;
			}
		}
		
		private java.util.List<IMatchingResult> visitArguments(IEvaluatorContext eval) {
			return buildMatchers(getArguments(), eval);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			eval.setCurrentAST(this);
			eval.notifyAboutSuspension(this);			

			try {
				if (eval.isInterrupted()) {
					throw new InterruptException(eval.getStackTrace(), eval.getCurrentAST().getLocation());
				}

				eval.setCurrentAST(this);

				Result<IValue> function = this.cachedPrefix;

				// If the name expression is just a name, enable caching of the name lookup result.
				// Also, if we have not yet registered a handler when we cache the result, do so now.
				if (function == null) {
					function = this.getExpression().interpret(eval);
					
					if (this.getExpression().isQualifiedName() && function instanceof ICallableValue && ((ICallableValue) function).isStatic()) {
						org.rascalmpl.ast.QualifiedName qname = this.getExpression().getQualifiedName();
						
						if (eval.getCurrentEnvt().isNameFinal(qname)) {
							this.cachedPrefix = function;
							registerCacheHandler(eval);
						}
					}
					else {
						cachedPrefix = null;
					}
				}

				java.util.List<org.rascalmpl.ast.Expression> args = this.getArguments();

				IValue[] actuals = new IValue[args.size()];
				Type[] types = new Type[args.size()];
				for (int i = 0; i < args.size(); i++) {
					Result<IValue> resultElem = args.get(i).interpret(eval);
					types[i] = resultElem.getValue().getType();
					if (types[i].isBottom()) {
						throw new UninitializedPatternMatch("The argument is of the type 'void'", args.get(i));
					}
					actuals[i] = resultElem.getValue();
				}
				
			  java.util.Map<String,IValue> kwActuals = Collections.<String,IValue>emptyMap();
			  
				if (hasKeywordArguments()) {
				  KeywordArguments_Expression keywordArgs = this.getKeywordArguments();
				  Type kwFormals = function.getKeywordArgumentTypes(eval.getCurrentEnvt());
				
				  if (keywordArgs.isDefault()){
				    kwActuals = new HashMap<String,IValue>();

				    for (KeywordArgument_Expression kwa : keywordArgs.getKeywordArgumentList()){
				      Result<IValue> val = kwa.getExpression().interpret(eval);
				      String name = Names.name(kwa.getName());

				      if (kwFormals != null) {
				        if (kwFormals.hasField(name)) {
				          if (!val.getValue().getType().isSubtypeOf(kwFormals.getFieldType(name))) {
				            throw new UnexpectedType(kwFormals.getFieldType(name), val.getType(), this);
				          }
				        }
				        else {
				          eval.getMonitor().warning("calling function with extra unknown keyword argument: " +  name, getLocation());
				        }
				      }

				      kwActuals.put(name, val.getValue());
				    }
				  }
				}
				Result<IValue> res = null;
				try {
					res = function.call(types, actuals, kwActuals);
				}
				catch (MatchFailed e) {
				  throw new ArgumentsMismatch(function, types, this);
				}
				return res;
			}
			catch (StackOverflowError e) {
				throw RuntimeExceptionFactory.stackOverflow(this, eval.getStackTrace());
			}
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			Type lambda = getExpression().typeOf(env, instantiateTypeParameters, eval);

			if (lambda.isString()) {
				return TF.nodeType();
			}
			
			if (lambda.isSourceLocation()) {
				return lambda;
			}
			
			if (lambda.isExternalType()) {
				if (lambda instanceof FunctionType) {
					return ((FunctionType) lambda).getReturnType();
				}
				
				if (lambda instanceof OverloadedFunctionType) {
					return ((OverloadedFunctionType) lambda).getReturnType();
				}
			}

			return TF.nodeType();
		}
	}

	static public class Closure extends org.rascalmpl.ast.Expression.Closure {

		public Closure(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Type __param2,
				Parameters __param3, java.util.List<Statement> __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Environment env = __eval.getCurrentEnvt();
			Parameters parameters = getParameters();
			Type formals = parameters.typeOf(env, true, __eval);
			Type returnType = typeOf(env, true, __eval);
			RascalTypeFactory RTF = RascalTypeFactory.getInstance();

			Type kwParams = TF.voidType();

			java.util.List<KeywordFormal> kwd = parameters.getKeywordFormals().hasKeywordFormalList() ? parameters.getKeywordFormals().getKeywordFormalList() : Collections.<KeywordFormal>emptyList();
			
			if (parameters.hasKeywordFormals() && parameters.getKeywordFormals().hasKeywordFormalList()) {
				kwParams = TypeDeclarationEvaluator.computeKeywordParametersType(kwd, __eval);
			}

			return new RascalFunction(this, __eval, null,
					(FunctionType) RTF.functionType(returnType, formals, kwParams),
					kwd,
					this.getParameters()
					.isVarArgs(), false, false, this.getStatements(), env, __eval.__getAccumulators());
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return getType().typeOf(env, instantiateTypeParameters, eval);
		}

	}

	static public class Composition extends
			org.rascalmpl.ast.Expression.Composition {

		public Composition(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);						
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.compose(right);

		}

	}

	static public class Comprehension extends
			org.rascalmpl.ast.Expression.Comprehension {

		public Comprehension(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Comprehension __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

	

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
						
			return this.getComprehension().interpret(__eval);

		}

	}

	static public class Concrete extends org.rascalmpl.ast.Expression.Concrete {
  
    public Concrete(ISourceLocation src, IConstructor node, org.rascalmpl.ast.Concrete concrete) {
      super(src, node, concrete);
    }
    
    @Override
    public Type typeOf(Environment env, boolean instantiateTypeParameters,
    		IEvaluator<Result<IValue>> eval) {
       return RascalValueFactory.Tree;
    }

    @Override
    public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
      throw new SyntaxError("concrete syntax fragment", getLocation());
    }
    
    @Override
    public IMatchingResult buildMatcher(IEvaluatorContext eval) {
      throw new SyntaxError("concrete syntax fragment", getLocation());
    }
  }
	
	static public class Descendant extends
			org.rascalmpl.ast.Expression.Descendant {

		public Descendant(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			IMatchingResult absPat = this.getPattern().buildMatcher(eval);
			return new DescendantPattern(eval, this, absPat);
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return TypeFactory.getInstance().valueType();
		}

	}

	static public class Division extends org.rascalmpl.ast.Expression.Division {

		public Division(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2, org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			throw new UnexpectedType(TF.boolType(), interpret(eval.getEvaluator()).getType(), this);
		}

	

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);		
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.divide(right);

		}

	}

	static public class Enumerator extends
			org.rascalmpl.ast.Expression.Enumerator {

		public Enumerator(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new EnumeratorResult(eval, getPattern().buildMatcher(eval.getEvaluator()), getExpression());
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Environment old = __eval.getCurrentEnvt();
			try {
				__eval.pushEnv();
				IBooleanResult gen = this.getBacktracker(__eval);
				gen.init();
				if (gen.hasNext() && gen.next()) {
					return org.rascalmpl.interpreter.result.ResultFactory.makeResult(TF.boolType(),
							VF.bool(true), __eval);
				}
				return org.rascalmpl.interpreter.result.ResultFactory.makeResult(TF.boolType(),
						VF.bool(false), __eval);
			} finally {
				__eval.unwind(old);
			}

		}

	}

	static public class Equals extends org.rascalmpl.ast.Expression.Equals {

		public Equals(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

	
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = left.equals(right);
			return makeResult(TF.boolType(), result.getValue(), __eval);
		}

	}

	static public class Equivalence extends
			org.rascalmpl.ast.Expression.Equivalence {

		public Equivalence(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new EquivalenceResult(eval, getLhs().buildBacktracker(eval), getRhs().buildBacktracker(eval));
		}


		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return evalBooleanExpression(this, __eval);
		}

	}

	static public class FieldAccess extends
			org.rascalmpl.ast.Expression.FieldAccess {

		public FieldAccess(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2, Name __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

	

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> expr = this.getExpression().interpret(__eval);
			String field = org.rascalmpl.interpreter.utils.Names.name(this
					.getField());

			return expr.fieldAccess(field, __eval.getCurrentEnvt().getStore());

		}

	}

	static public class FieldProject extends
			org.rascalmpl.ast.Expression.FieldProject {

		public FieldProject(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				java.util.List<Field> __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

	

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> base = this.getExpression().interpret(__eval);
			java.util.List<Field> fields = this.getFields();
			return base.fieldSelect(fields.toArray(new Field[0]));
		}

	}

	static public class FieldUpdate extends
			org.rascalmpl.ast.Expression.FieldUpdate {

		public FieldUpdate(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2, Name __param3,
				org.rascalmpl.ast.Expression __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> expr = this.getExpression().interpret(__eval);
			Result<IValue> repl = this.getReplacement().interpret(__eval);
			String name = org.rascalmpl.interpreter.utils.Names.name(this
					.getKey());
			return expr.fieldUpdate(name, repl, __eval.getCurrentEnvt()
					.getStore());

		}

	}

	static public class GetAnnotation extends
			org.rascalmpl.ast.Expression.GetAnnotation {

		public GetAnnotation(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2, Name __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> base = this.getExpression().interpret(__eval);
			String annoName = org.rascalmpl.interpreter.utils.Names.name(this
					.getName());
			return base.getAnnotation(annoName, __eval.getCurrentEnvt());

		}

	}

	static public class GreaterThan extends
			org.rascalmpl.ast.Expression.GreaterThan {

		public GreaterThan(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = left.greaterThan(right);
			return makeResult(TF.boolType(), result.getValue(), __eval);
		}

	}

	static public class GreaterThanOrEq extends
			org.rascalmpl.ast.Expression.GreaterThanOrEq {

		public GreaterThanOrEq(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = left.greaterThanOrEqual(right);
			return makeResult(TF.boolType(), result.getValue(), __eval);

		}

	}

	static public class AsType extends org.rascalmpl.ast.Expression.AsType {

		public AsType(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Type __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			Type type = getType().typeOf(eval.getCurrentEnvt(), true, eval.getEvaluator());
			IMatchingResult absPat = this.getArgument().buildMatcher(eval);
			return new GuardedPattern(eval, this, type, absPat);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> result = this.getArgument().interpret(__eval);
			Type expected = getType().typeOf(__eval.getCurrentEnvt(), true, __eval);

			if (!(expected instanceof NonTerminalType)) {
				throw new UnsupportedOperation("inline parsing", expected, this);
			}
			
			if (!result.getType().isSubtypeOf(TF.stringType()) && !result.getType().isSubtypeOf(TF.sourceLocationType())) {
				throw new UnsupportedOperation("inline parsing", result.getType(), this);
			}
			
			IConstructor symbol = ((NonTerminalType) expected).getSymbol();
			if (!SymbolAdapter.isSort(symbol) && !SymbolAdapter.isLex(symbol) && !SymbolAdapter.isLayouts(symbol) && !SymbolAdapter.isStartSort(symbol)) {
				throw new UnsupportedOperation("inline parsing", expected, this);
			}

			__eval.__setInterrupt(false);
			try {
				IConstructor tree = null;
				
				if (result.getType().isString()) {
					tree = __eval.parseObject(symbol, VF.mapWriter().done(),
						this.getLocation(),
						((IString) result.getValue()).getValue().toCharArray());
				}
				else if (result.getType().isSourceLocation()) {
					tree = __eval.parseObject(__eval, symbol, VF.mapWriter().done(),
							((ISourceLocation) result.getValue()));
				}
				
				assert tree != null; // because we checked earlier

				return org.rascalmpl.interpreter.result.ResultFactory
						.makeResult(expected, tree, __eval);
			}
			catch (ParseError e) {
				throw RuntimeExceptionFactory.parseError(getLocation(), this, __eval.getStackTrace());
			}
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return getType().typeOf(env, instantiateTypeParameters, eval);
		}

	}

	static public class Has extends org.rascalmpl.ast.Expression.Has {

		public Has(ISourceLocation src, IConstructor node, org.rascalmpl.ast.Expression expression,
				Name name) {
			super(src, node, expression, name);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IBool> result = getExpression().interpret(__eval).has(getName());
			return makeResult(TF.boolType(), result.getValue(), __eval);
		}
	}

	static public class IfDefinedOtherwise extends
			org.rascalmpl.ast.Expression.IfDefinedOtherwise {

		public IfDefinedOtherwise(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);	
			
			try {
				return this.getLhs().interpret(__eval);
			} catch (UninitializedVariable e) {
				return this.getRhs().interpret(__eval);
			} catch (Throw e) {
				// TODO For now we __evaluate any Throw here, restrict to
				// NoSuchKey and NoSuchAnno?
				return this.getRhs().interpret(__eval);
			}

		}

	}

	static public class IfThenElse extends
			org.rascalmpl.ast.Expression.IfThenElse {

		public IfThenElse(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3,
				org.rascalmpl.ast.Expression __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Environment old = __eval.getCurrentEnvt();
			__eval.pushEnv();

			try {
				Result<IValue> cval = this.getCondition().interpret(__eval);

				if (!cval.getType().isBool()) {
					throw new UnexpectedType(TF.boolType(),
							cval.getType(), this);
				}

				if (cval.isTrue()) {
					return this.getThenExp().interpret(__eval);
				}

				return this.getElseExp().interpret(__eval);
			} finally {
				__eval.unwind(old);
			}

		}

	}

	static public class Implication extends
			org.rascalmpl.ast.Expression.Implication {

		public Implication(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new OrResult(eval, new NotResult(eval, getLhs().buildBacktracker(eval)), getRhs().buildBacktracker(eval));
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return evalBooleanExpression(this, __eval);
		}

	}

	static public class In extends org.rascalmpl.ast.Expression.In {

		public In(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			return new BasicBooleanResult(__eval, this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = right.in(left);
			return makeResult(TF.boolType(), result.getValue(), __eval);
		}

	}

	static public class Intersection extends
			org.rascalmpl.ast.Expression.Intersection {

		public Intersection(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.intersect(right);

		}

	}

	static public class Is extends org.rascalmpl.ast.Expression.Is {

		public Is(ISourceLocation src, IConstructor node, org.rascalmpl.ast.Expression expression, Name name) {
			super(src, node, expression, name);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IBool> result = getExpression().interpret(__eval).is(getName());
			return makeResult(TF.boolType(), result.getValue(), __eval);
		}
	}

	static public class IsDefined extends
			org.rascalmpl.ast.Expression.IsDefined {

		public IsDefined(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			try {
				this.getArgument().interpret(__eval); // wait for exception
				return org.rascalmpl.interpreter.result.ResultFactory
						.makeResult(TF.boolType(), __eval.__getVf().bool(true),
								__eval);

			} catch (Throw e) {
				// TODO For now we __evaluate any Throw here, restrict to
				// NoSuchKey and NoSuchAnno?
				return org.rascalmpl.interpreter.result.ResultFactory
						.makeResult(TF.boolType(),
								__eval.__getVf().bool(false), __eval);
			}

		}

	}

	static public class It extends org.rascalmpl.ast.Expression.It {

		public It(ISourceLocation __param1, IConstructor tree) {
			super(__param1, tree);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> v = __eval.getCurrentEnvt().getVariable(IT);
			if (v == null) {
				throw new UnguardedIt(this);
			}
			return v;

		}
		
		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

	}

	static public class Join extends org.rascalmpl.ast.Expression.Join {

		public Join(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.join(right);

		}

	}

	static public class LessThan extends org.rascalmpl.ast.Expression.LessThan {

		public LessThan(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = left.lessThan(right);
			return makeResult(TF.boolType(), result.getValue(), __eval);
		}
	}

	static public class LessThanOrEq extends
			org.rascalmpl.ast.Expression.LessThanOrEq {

		public LessThanOrEq(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = left.lessThanOrEqual(right);
			return ResultFactory.makeResult(result.getType(), result.getValue(),__eval);
		}

	}

	static public class List extends org.rascalmpl.ast.Expression.List {

		public List(ISourceLocation __param1, IConstructor tree,
				java.util.List<org.rascalmpl.ast.Expression> __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			return new ListPattern(eval, this, buildMatchers(getElements0(), eval));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			java.util.List<org.rascalmpl.ast.Expression> elements = getElements0();

			Type elementType = TF.voidType();
			java.util.List<IValue> results = new ArrayList<IValue>();

			for (org.rascalmpl.ast.Expression expr : elements) {
				boolean isSplicedElem = expr.isSplice() || expr.isSplicePlus();
				
				Result<IValue> resultElem = null;

				if (!isSplicedElem) {
					resultElem = expr.interpret(__eval);

					if (resultElem.getType().isBottom()) {
						throw new NonVoidTypeRequired(expr);
					}

				}
			
				if (isSplicedElem){
				  resultElem = expr.getArgument().interpret(__eval);
				  if (resultElem.getType().isBottom()) {
				    throw new NonVoidTypeRequired(expr);
				  }

				  if(resultElem.getType().isList()|| resultElem.getType().isSet()){
				    /*
				     * Splice elements in list
				     */
					elementType = elementType.lub(resultElem.getType().getElementType());
				    for (IValue val : (Iterable<IValue>) resultElem.getValue()) {
				      results.add(val);
				    }
				    continue;
				  } 
				}

				elementType = elementType.lub(resultElem.getType());
				results.add(results.size(), resultElem.getValue());
			}

			Type resultType = TF.listType(elementType);
			IListWriter w = __eval.__getVf().listWriter();
			w.appendAll(results);
			return org.rascalmpl.interpreter.result.ResultFactory.makeResult(resultType, w.done(), __eval);
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			Type elementType = TF.voidType();

			for (org.rascalmpl.ast.Expression elt : getElements0()) {
				elementType = elementType.lub(elt.typeOf(env, instantiateTypeParameters, eval));
			}

			return TF.listType(elementType);
		}

	}

	static public class Literal extends org.rascalmpl.ast.Expression.Literal {

		public Literal(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Literal __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			if (this.getLiteral().isBoolean()) {
				return new BasicBooleanResult(eval, this);
			}
			throw new UnexpectedType(TF.boolType(), interpret(eval.getEvaluator()).getType(), this);
		}

		
		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext __eval) {
			return this.getLiteral().buildMatcher(__eval);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return this.getLiteral().interpret(__eval);
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return getLiteral().typeOf(env, instantiateTypeParameters, eval);
		}

	}

	static public class Map extends org.rascalmpl.ast.Expression.Map {

		public Map(ISourceLocation __param1, IConstructor tree, java.util.List<Mapping_Expression> __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext __eval) {
			throw new ImplementationError("Map in pattern not yet implemented");
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			java.util.List<Mapping_Expression> mappings = this.getMappings();
			java.util.Map<IValue, IValue> result = new HashMap<IValue, IValue>();
			Type keyType = TF.voidType();
			Type valueType = TF.voidType();

			for (Mapping_Expression mapping : mappings) {
				Result<IValue> keyResult = mapping.getFrom().interpret(__eval);
				Result<IValue> valueResult = mapping.getTo().interpret(__eval);

				if (keyResult.getType().isBottom()) {
					throw new NonVoidTypeRequired(mapping.getFrom());
				}

				if (valueResult.getType().isBottom()) {
					throw new NonVoidTypeRequired(mapping.getTo());
				}

				keyType = keyType.lub(keyResult.getType());
				valueType = valueType.lub(valueResult.getType());

				IValue keyValue = result.get(keyResult.getValue());
				if (keyValue != null) {
					throw org.rascalmpl.interpreter.utils.RuntimeExceptionFactory
							.MultipleKey(keyValue, mapping.getFrom(), __eval
									.getStackTrace());
				}

				result.put(keyResult.getValue(), valueResult.getValue());
			}

			Type type = TF.mapType(keyType, valueType);
			IMapWriter w = __eval.__getVf().mapWriter();
			w.putAll(result);

			return org.rascalmpl.interpreter.result.ResultFactory.makeResult(
					type, w.done(), __eval);

		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			Type keyType = TF.voidType();
			Type valueType = TF.valueType();
			
			for (Mapping_Expression me : getMappings()) {
				keyType = keyType.lub(me.getFrom().typeOf(env, instantiateTypeParameters, eval));
				valueType = valueType.lub(me.getTo().typeOf(env, instantiateTypeParameters, eval));
			}
			
			return TF.mapType(keyType, valueType);
		}

	}

	static public class Match extends org.rascalmpl.ast.Expression.Match {

		public Match(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new MatchResult(eval, getPattern(), true, getExpression());
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			return evalBooleanExpression(this, __eval);
		}

	}

	static public class Modulo extends org.rascalmpl.ast.Expression.Modulo {

		public Modulo(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			
			return left.modulo(right);

		}

	}
	
	static public class Remainder extends org.rascalmpl.ast.Expression.Remainder {

		public Remainder(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.remainder(right);

		}

	}

	static public class MultiVariable extends
			org.rascalmpl.ast.Expression.MultiVariable {

		public MultiVariable(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.QualifiedName __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			return new MultiVariablePattern(eval, this, getQualifiedName());
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);		
			__eval.warning("Var* is deprecated, use *Var or *Type Var instead", this.getLocation());
			System.err.println(this.getLocation() + ": Var* is deprecated, use *Var instead");
			
			Name name = this.getName();
			Result<IValue> variable = __eval.getCurrentEnvt().getVariable(name);

			if (variable == null) {
				throw new UndeclaredVariable(
						org.rascalmpl.interpreter.utils.Names.name(name), name);
			}

			if (variable.getValue() == null) {
				throw new UninitializedVariable(
						org.rascalmpl.interpreter.utils.Names.name(name), name);
			}

			return variable;
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			// we return the element type here, such that lub at a higher level
			// does the right thing!
			return getQualifiedName().typeOf(env, instantiateTypeParameters, eval);
		}

	}
	
	static public class Splice extends
	org.rascalmpl.ast.Expression.Splice {

		public Splice(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			org.rascalmpl.ast.Expression arg = this.getArgument();
			if (arg.hasType() && arg.hasName()) {
				Environment env = eval.getCurrentEnvt();
				Type type = arg.getType().typeOf(env, true, eval.getEvaluator());
				type = type.instantiate(env.getTypeBindings());
				
				// TODO: Question, should we allow non terminal types in splices?
				if (type instanceof NonTerminalType) {
					throw new UnsupportedOperation("splicing match", type, this);
//					throw new ImplementationError(null);
				}				
				return new TypedMultiVariablePattern(eval, this, type, arg.getName());
			}
			if(arg.hasQualifiedName()){
				return new MultiVariablePattern(eval, this, arg.getQualifiedName());
			}
			throw new ImplementationError(null);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Name name = this.getName();
			Result<IValue> variable = __eval.getCurrentEnvt().getVariable(name);

			if (variable == null) {
				throw new UndeclaredVariable(
						org.rascalmpl.interpreter.utils.Names.name(name), name);
			}

			if (variable.getValue() == null) {
				throw new UninitializedVariable(
						org.rascalmpl.interpreter.utils.Names.name(name), name);
			}

			return variable;
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			// we return the element type here, such that lub at a higher level
			// does the right thing!
			org.rascalmpl.ast.Expression arg = this.getArgument();
			if (arg.hasType() && arg.hasName()) {
				return arg.getType().typeOf(env, instantiateTypeParameters, eval);
			}
			if(arg.hasQualifiedName()){
				return arg.getQualifiedName().typeOf(env, instantiateTypeParameters, eval);
			}
			throw new ImplementationError(null);
		}

	}

	static public class Negation extends org.rascalmpl.ast.Expression.Negation {

		public Negation(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new NotResult(eval, getArgument().buildBacktracker(eval));
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return evalBooleanExpression(this, __eval);
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return TF.boolType();
		}

	}

	static public class Negative extends org.rascalmpl.ast.Expression.Negative {

		public Negative(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);
		}
		
		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext __eval) {
			return new NegativePattern(__eval, this, getArgument().buildMatcher(__eval));
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> arg = this.getArgument().interpret(__eval);
			return arg.negative();
		}
	}

	static public class NoMatch extends org.rascalmpl.ast.Expression.NoMatch {

		public NoMatch(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new MatchResult(eval, getPattern(), false, getExpression());
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return evalBooleanExpression(this, __eval);
		}

	}

	static public class NonEmptyBlock extends
			org.rascalmpl.ast.Expression.NonEmptyBlock {
		public NonEmptyBlock(ISourceLocation __param1, IConstructor tree, java.util.List<Statement> __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return ASTBuilder.make("Statement", "NonEmptyBlock", this.getLocation(),
					ASTBuilder.make("Label", "Empty", this.getLocation()),
					this.getStatements()).interpret(__eval);
		}
	}

	static public class NonEquals extends
			org.rascalmpl.ast.Expression.NonEquals {

		public NonEquals(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = left.nonEquals(right);
			return makeResult(result.getType(), result.getValue(), __eval);
		}

	}

	static public class NotIn extends org.rascalmpl.ast.Expression.NotIn {

		public NotIn(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			Result<IBool> result = right.notIn(left);
			return makeResult(result.getType(), result.getValue(), __eval);

		}

	}

	static public class Or extends org.rascalmpl.ast.Expression.Or {

		public Or(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new OrResult(eval, this.getLhs().buildBacktracker(eval), this.getRhs().buildBacktracker(eval));
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			return evalBooleanExpression(this, __eval);
		}
	}

	static public class Product extends org.rascalmpl.ast.Expression.Product {

		public Product(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.multiply(right);
		}

	}

	static public class QualifiedName extends
			org.rascalmpl.ast.Expression.QualifiedName {

		public QualifiedName(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.QualifiedName __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {

			org.rascalmpl.ast.QualifiedName name = this.getQualifiedName();
//			Type signature = TF.tupleType(new Type[0]);

			Result<IValue> r = eval.getEvaluator().getCurrentEnvt().getSimpleVariable(name);

			if (r != null) {
				if (r.getValue() != null) {
					// Previously declared and initialized variable
					return new QualifiedNamePattern(eval, this, name);
				}

				Type type = r.getType();
				if (type instanceof NonTerminalType) {
					NonTerminalType cType = (NonTerminalType) type;
					if (cType.isConcreteListType()) {
						return new ConcreteListVariablePattern(eval, this, type, ((Default) name).lastName());
					}
				}

				return new QualifiedNamePattern(eval, this, name);
			}

			// TODO: I don't understand which feature this code implements
//			if (eval.getCurrentEnvt().isTreeConstructorName(name, signature)) {
//				return new NodePattern(eval, this, null, name, new ArrayList<IMatchingResult>());
//			}

			// Completely fresh variable
			return new QualifiedNamePattern(eval, this, name);
			// return new AbstractPatternTypedVariable(vf, env,
			// ev.tf.valueType(), name);

		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			org.rascalmpl.ast.QualifiedName name = this.getQualifiedName();
			Result<IValue> variable = __eval.getCurrentEnvt().getVariable(name);

			if (variable == null) {
				throw new UndeclaredVariable(
						org.rascalmpl.interpreter.utils.Names.fullName(name),
						name);
			}

			if (variable.getValue() == null) {
				throw new UninitializedVariable(
						org.rascalmpl.interpreter.utils.Names.fullName(name),
						name);
			}

			return variable;

		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return getQualifiedName().typeOf(env, instantiateTypeParameters, eval);
		}

	}

	static public class Range extends org.rascalmpl.ast.Expression.Range {

		public Range(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			// IListWriter w = vf.listWriter(tf.integerType());
			Result<IValue> from = this.getFirst().interpret(__eval);
			Result<IValue> to = this.getLast().interpret(__eval);
			return from.makeRange(to);

		}

	}

	static public class Reducer extends org.rascalmpl.ast.Expression.Reducer {

		public Reducer(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3,
				java.util.List<org.rascalmpl.ast.Expression> __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult getBacktracker(IEvaluatorContext ctx) {
		  return new BasicBooleanResult(ctx, this);
		}
		
		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return getBacktracker(eval);
		}
		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			org.rascalmpl.ast.Expression init = getInit();
			org.rascalmpl.ast.Expression result = getResult();
			java.util.List<org.rascalmpl.ast.Expression> generators = getGenerators();
			int size = generators.size();
			IBooleanResult[] gens = new IBooleanResult[size];
			Environment[] olds = new Environment[size];
			Environment old = __eval.getCurrentEnvt();
			int i = 0;

			Result<IValue> it = init.interpret(__eval);

			try {
				olds[0] = __eval.getCurrentEnvt();
				__eval.pushEnv();
				gens[0] = generators.get(0).getBacktracker(__eval);
				gens[0].init();

				while (i >= 0 && i < size) {
					if (__eval.isInterrupted())
						throw new InterruptException(__eval.getStackTrace(), __eval.getCurrentAST().getLocation());
					if (gens[i].hasNext() && gens[i].next()) {
						if (i == size - 1) {
							__eval.getCurrentEnvt().storeVariable(IT, it);
							it = result.interpret(__eval);
							__eval.unwind(olds[i]);
							__eval.pushEnv();
						} else {
							i++;
							gens[i] = generators.get(i).getBacktracker(__eval);
							gens[i].init();
							olds[i] = __eval.getCurrentEnvt();
							__eval.pushEnv();
						}
					} else {
						__eval.unwind(olds[i]);
						i--;
					}
				}
			} finally {
				__eval.unwind(old);
			}
			return it;

		}

	}

	static public class ReifiedType extends
			org.rascalmpl.ast.Expression.ReifiedType {
		private static final Type defType = TypeFactory.getInstance().mapType(RascalValueFactory.Symbol, RascalValueFactory.Production);
		
		public ReifiedType(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3
				) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			return new ReifiedTypePattern(eval, this, getSymbol().buildMatcher(eval), getDefinitions().buildMatcher(eval));
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> symbol = getSymbol().interpret(__eval);
			Result<IValue> declarations = getDefinitions().interpret(__eval);
			
			if (!symbol.getType().isSubtypeOf(RascalValueFactory.Symbol)) {
				throw new UnexpectedType(RascalValueFactory.Symbol, symbol.getType(), getSymbol());
			}
			
			if (!declarations.getType().isSubtypeOf(defType)) {
				throw new UnexpectedType(defType, declarations.getType(), getSymbol());
			}
			
			java.util.Map<Type,Type> bindings = new HashMap<Type,Type>();
			bindings.put(RascalValueFactory.TypeParam, new TypeReifier(VF).symbolToType((IConstructor) symbol.getValue(), (IMap) declarations.getValue()));
			
			IValue val = VF.constructor(RascalValueFactory.Type_Reified.instantiate(bindings), symbol.getValue(), declarations.getValue());
			
			bindings.put(RascalValueFactory.TypeParam, TF.valueType());
			Type typ = RascalValueFactory.Type.instantiate(bindings);
			
			return ResultFactory.makeResult(typ, val, __eval);
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			// TODO: check if this would do it?
			return RascalTypeFactory.getInstance().reifiedType(TF.valueType());
		}

	}

	static public class ReifyType extends
			org.rascalmpl.ast.Expression.ReifyType {

		public ReifyType(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Type __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			eval.setCurrentAST(this);
			eval.notifyAboutSuspension(this);			

			Type t = getType().typeOf(eval.getCurrentEnvt(), false, eval);
			return new TypeReifier(eval.__getVf()).typeToValue(t, eval);
		}
	}

	static public class Set extends org.rascalmpl.ast.Expression.Set {

		public Set(ISourceLocation __param1, IConstructor tree,
				java.util.List<org.rascalmpl.ast.Expression> __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			return new SetPattern(eval, this, buildMatchers(this.getElements0(), eval));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			java.util.List<org.rascalmpl.ast.Expression> elements = this
					.getElements0();

			Type elementType = TF.voidType();
			java.util.List<IValue> results = new ArrayList<IValue>();

			for (org.rascalmpl.ast.Expression expr : elements) {
				Result<IValue> resultElem;
				
				if(expr.isSplice() || expr.isSplicePlus()){
					resultElem = expr.getArgument().interpret(__eval);
					if (resultElem.getType().isBottom()) {
						throw new NonVoidTypeRequired(expr.getArgument());
					}

					if (resultElem.getType().isSet() || resultElem.getType().isList()){
						/*
						 * Splice the elements in the set
						 * __eval.
						 */
						elementType = elementType.lub(resultElem.getType().getElementType());
						for (IValue val : (Iterable<IValue>) resultElem.getValue()) {
							results.add(val);
						}
					continue;
					}
				} else {
					resultElem = expr.interpret(__eval);
					if (resultElem.getType().isBottom()) {
						throw new NonVoidTypeRequired(expr);
					}
				}
				elementType = elementType.lub(resultElem.getType());
				results.add(results.size(), resultElem.getValue());
			}
			Type resultType = TF.setType(elementType);
			ISetWriter w = __eval.__getVf().setWriter();
			w.insertAll(results);
			// Was: return makeResult(resultType, applyRules(w.done()));
			return org.rascalmpl.interpreter.result.ResultFactory.makeResult(
					resultType, w.done(), __eval);

		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			Type elementType = TF.voidType();

			for (org.rascalmpl.ast.Expression elt : getElements0()) {
				Type eltType = elt.typeOf(env, instantiateTypeParameters, eval);
				
				// TODO: here we need to properly deal with splicing operators!!!
				if (eltType.isSet()) {
				  eltType = eltType.getElementType();
				}
        elementType = elementType.lub(eltType);
			}

			return TF.setType(elementType);
		}

	}

	static public class SetAnnotation extends
			org.rascalmpl.ast.Expression.SetAnnotation {

		public SetAnnotation(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2, Name __param3,
				org.rascalmpl.ast.Expression __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> base = this.getExpression().interpret(__eval);
			String annoName = org.rascalmpl.interpreter.utils.Names.name(this
					.getName());
			Result<IValue> anno = this.getValue().interpret(__eval);
			return base.setAnnotation(annoName, anno, __eval.getCurrentEnvt());
		}

	}

	static public class StepRange extends
			org.rascalmpl.ast.Expression.StepRange {

		public StepRange(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3,
				org.rascalmpl.ast.Expression __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

	

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> from = this.getFirst().interpret(__eval);
			Result<IValue> to = this.getLast().interpret(__eval);
			Result<IValue> second = this.getSecond().interpret(__eval);
			return from.makeStepRange(to, second);

		}

	}
	
	static public class Slice extends
	org.rascalmpl.ast.Expression.Slice {

		public Slice(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2, 
				org.rascalmpl.ast.OptionalExpression __param3, org.rascalmpl.ast.OptionalExpression __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> expr = this.getExpression().interpret(__eval);
					
			Result<?> first = this.getOptFirst().hasExpression() ? this.getOptFirst().getExpression().interpret(__eval) : null;
			Result<?> last = this.getOptLast().hasExpression() ? this.getOptLast().getExpression().interpret(__eval) : null;
			
			return expr.slice(first, null, last);
		}
	}
	
	static public class SliceStep extends
	org.rascalmpl.ast.Expression.SliceStep {

		public SliceStep(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2, 
				org.rascalmpl.ast.OptionalExpression __param3, org.rascalmpl.ast.Expression __param4, org.rascalmpl.ast.OptionalExpression __param5) {
			super(__param1, tree, __param2, __param3, __param4, __param5);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			Result<IValue> expr = this.getExpression().interpret(__eval);
						
			Result<?> first = this.getOptFirst().hasExpression() ? this.getOptFirst().getExpression().interpret(__eval) : null;
			Result<?> second = this.getSecond().interpret(__eval);
			Result<?> last = this.getOptLast().hasExpression() ? this.getOptLast().getExpression().interpret(__eval) : null;
			
			return expr.slice(first, second, last);
		}
	}

	static public class Subscript extends
			org.rascalmpl.ast.Expression.Subscript {

		public Subscript(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Expression __param2,
				java.util.List<org.rascalmpl.ast.Expression> __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext eval) {
			return new BasicBooleanResult(eval, this);
		}

		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> expr = this.getExpression().interpret(__eval);
			int nSubs = this.getSubscripts().size();
			Result<?> subscripts[] = new Result<?>[nSubs];
			for (int i = 0; i < nSubs; i++) {
				org.rascalmpl.ast.Expression subsExpr = this.getSubscripts()
						.get(i);
				subscripts[i] = isWildCard(subsExpr) ? null
						: subsExpr.interpret(__eval);
			}
			return expr.subscript(subscripts);

		}
		
		private boolean isWildCard(org.rascalmpl.ast.Expression fieldName) {
			if (fieldName.isQualifiedName()) {
				return ((org.rascalmpl.semantics.dynamic.QualifiedName.Default) fieldName.getQualifiedName()).lastName().equals("_");
			}
			return false;
		}

	}

	static public class Subtraction extends
			org.rascalmpl.ast.Expression.Subtraction {

		public Subtraction(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			Result<IValue> left = this.getLhs().interpret(__eval);
			Result<IValue> right = this.getRhs().interpret(__eval);
			return left.subtract(right);

		}

	}

	static public class TransitiveClosure extends
			org.rascalmpl.ast.Expression.TransitiveClosure {

		public TransitiveClosure(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);
		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			return this.getArgument().interpret(__eval).transitiveClosure();

		}

	}

	static public class TransitiveReflexiveClosure extends
			org.rascalmpl.ast.Expression.TransitiveReflexiveClosure {

		public TransitiveReflexiveClosure(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Expression __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			return this.getArgument().interpret(__eval)
					.transitiveReflexiveClosure();

		}

	}

	static public class Tuple extends org.rascalmpl.ast.Expression.Tuple {

		public Tuple(ISourceLocation __param1, IConstructor tree,
				java.util.List<org.rascalmpl.ast.Expression> __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			return new TuplePattern(eval, this, buildMatchers(this.getElements(), eval));
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			

			java.util.List<org.rascalmpl.ast.Expression> elements = this
					.getElements();

			IValue[] values = new IValue[elements.size()];
			Type[] types = new Type[elements.size()];

			for (int i = 0; i < elements.size(); i++) {
				Result<IValue> resultElem = elements.get(i).interpret(__eval);
				types[i] = resultElem.getType();
				values[i] = resultElem.getValue();
			}

			// return makeResult(tf.tupleType(types),
			// applyRules(vf.tuple(values)));
			return org.rascalmpl.interpreter.result.ResultFactory.makeResult(TF
					.tupleType(types), __eval.__getVf().tuple(values), __eval);

		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			java.util.List<org.rascalmpl.ast.Expression> fields = getElements();
			Type fieldTypes[] = new Type[fields.size()];

			for (int i = 0; i < fields.size(); i++) {
				fieldTypes[i] = fields.get(i).typeOf(env, instantiateTypeParameters, eval);
			}

			return TF.tupleType(fieldTypes);
		}
	}

	static public class TypedVariable extends
			org.rascalmpl.ast.Expression.TypedVariable {
		public TypedVariable(ISourceLocation __param1, IConstructor tree, org.rascalmpl.ast.Type __param2,
				Name __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {
			throw new UninitializedVariable(Names.name(getName()), this);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			Environment env = eval.getCurrentEnvt();
			Type type = getType().typeOf(env, true, eval.getEvaluator());

			type = type.instantiate(env.getTypeBindings());
			
			return new TypedVariablePattern(eval, this, type, getName());
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			// TODO: should allow qualified names in TypeVariables?!?
			Result<IValue> result = __eval.getCurrentEnvt().getVariable(
					org.rascalmpl.interpreter.utils.Names.name(this.getName()));

			if (result != null && result.getValue() != null) {
				return result;
			}

			throw new UninitializedVariable(
					org.rascalmpl.interpreter.utils.Names.name(this.getName()),
					this);

		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return getType().typeOf(env, instantiateTypeParameters, eval);
		}
	}

	static public class TypedVariableBecomes extends
			org.rascalmpl.ast.Expression.TypedVariableBecomes {

		public TypedVariableBecomes(ISourceLocation __param1, IConstructor tree,
				org.rascalmpl.ast.Type __param2, Name __param3,
				org.rascalmpl.ast.Expression __param4) {
			super(__param1, tree, __param2, __param3, __param4);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new SyntaxError("expression", this.getLocation());

		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			Type type = getType().typeOf(eval.getCurrentEnvt(), true, eval.getEvaluator());
			IMatchingResult pat = this.getPattern().buildMatcher(eval);
			IMatchingResult var = new TypedVariablePattern(eval, this, type, this.getName());
			return new VariableBecomesPattern(eval, this, var, pat);

		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			return this.getPattern().interpret(__eval);

		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return getType().typeOf(env, instantiateTypeParameters, eval);
		}

	}

	static public class VariableBecomes extends
			org.rascalmpl.ast.Expression.VariableBecomes {

		public VariableBecomes(ISourceLocation __param1, IConstructor tree, Name __param2,
				org.rascalmpl.ast.Expression __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IMatchingResult buildMatcher(IEvaluatorContext eval) {
			IMatchingResult pat = this.getPattern().buildMatcher(eval);
			LinkedList<Name> names = new LinkedList<Name>();
			names.add(this.getName());
			IMatchingResult var = new QualifiedNamePattern(eval, this, ASTBuilder.<org.rascalmpl.ast.QualifiedName> make("QualifiedName", "Default", this.getLocation(), names));
			return new VariableBecomesPattern(eval, this, var, pat);

		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {
			
			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			return this.getPattern().interpret(__eval);
		}

		@Override
		public Type typeOf(Environment env, boolean instantiateTypeParameters, IEvaluator<Result<IValue>> eval) {
			return getPattern().typeOf(env, instantiateTypeParameters, eval);
		}

	}

	static public class Visit extends org.rascalmpl.ast.Expression.Visit {

		public Visit(ISourceLocation __param1, IConstructor tree, Label __param2,
				org.rascalmpl.ast.Visit __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> __eval) {

			__eval.setCurrentAST(this);
			__eval.notifyAboutSuspension(this);			
			
			return this.getVisit().interpret(__eval);

		}

	}

	static public class VoidClosure extends
			org.rascalmpl.ast.Expression.VoidClosure {

		public VoidClosure(ISourceLocation __param1, IConstructor tree, Parameters __param2,
				java.util.List<Statement> __param3) {
			super(__param1, tree, __param2, __param3);
		}

		@Override
		public IBooleanResult buildBacktracker(IEvaluatorContext __eval) {

			throw new UnexpectedType(TF.boolType(), this
					.interpret(__eval.getEvaluator()).getType(),
					this);

		}

		

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {

			eval.setCurrentAST(this);
			eval.notifyAboutSuspension(this);			

			Parameters parameters = getParameters();
			Type formals = parameters.typeOf(eval.getCurrentEnvt(), true, eval);
			RascalTypeFactory RTF = RascalTypeFactory.getInstance();

			Type kwParams = TF.voidType();
			java.util.List<KeywordFormal> kws = parameters.getKeywordFormals().hasKeywordFormalList() ? parameters.getKeywordFormals().getKeywordFormalList() : Collections.<KeywordFormal>emptyList();
			
			if (parameters.hasKeywordFormals() && parameters.getKeywordFormals().hasKeywordFormalList()) {
				kwParams = TypeDeclarationEvaluator.computeKeywordParametersType(kws, eval);
			}

			return new RascalFunction(this, eval, null, (FunctionType) RTF
					.functionType(TF.voidType(), formals, kwParams), kws, this.getParameters()
					.isVarArgs(), false, false, this.getStatements0(), eval
					.getCurrentEnvt(), eval.__getAccumulators());
		}
	}

	public Expression(ISourceLocation __param1, IConstructor tree) {
		super(__param1, tree);
	}
	
	private static java.util.List<IMatchingResult> buildMatchers(java.util.List<org.rascalmpl.ast.Expression> elements, IEvaluatorContext eval) {
		ArrayList<IMatchingResult> args = new ArrayList<IMatchingResult>(elements.size());

		int i = 0;
		for (org.rascalmpl.ast.Expression e : elements) {
			args.add(i++, e.buildMatcher(eval));
		}
		
		return args;
	}
	
	private static Result<IValue> evalBooleanExpression(org.rascalmpl.ast.Expression x, IEvaluatorContext ctx) {
		IBooleanResult mp = x.getBacktracker(ctx);
		mp.init();
//		while (mp.hasNext()) {
//			if (ctx.isInterrupted())
//				throw new InterruptException(ctx.getStackTrace());
//			if (mp.next()) {
//				return ResultFactory.bool(true, ctx);
//			}
//		}
		return makeResult(TypeFactory.getInstance().boolType(), ctx.getValueFactory().bool(mp.hasNext() && mp.next()), ctx);
	}
}
