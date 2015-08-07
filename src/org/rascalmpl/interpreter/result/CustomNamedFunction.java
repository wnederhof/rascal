package org.rascalmpl.interpreter.result;

import static org.rascalmpl.interpreter.result.ResultFactory.makeResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.ast.Expression.Closure;
import org.rascalmpl.ast.Expression.VoidClosure;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.FunctionDeclaration.Conditional;
import org.rascalmpl.ast.FunctionDeclaration.Default;
import org.rascalmpl.ast.KeywordFormal;
import org.rascalmpl.ast.NullASTVisitor;
import org.rascalmpl.ast.Parameters;
import org.rascalmpl.ast.Type.Structured;
import org.rascalmpl.interpreter.Accumulator;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.control_exceptions.Failure;
import org.rascalmpl.interpreter.control_exceptions.InterruptException;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.control_exceptions.Return;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.staticErrors.UnexpectedType;
import org.rascalmpl.interpreter.staticErrors.UnguardedFail;
import org.rascalmpl.interpreter.staticErrors.UnsupportedPattern;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.semantics.dynamic.Tree;
import org.rascalmpl.semantics.dynamic.Tree.Appl;

public abstract class CustomNamedFunction extends NamedFunction {
	// private final List<Statement> body;
	final Stack<Accumulator> accumulators;
	private final List<Expression> formals;
	private final String firstOutermostLabel;
	private final IConstructor firstOutermostProduction;
	
	final boolean isVoidFunction;
	final List<KeywordFormal> initializers;
	

	@SuppressWarnings("unchecked")
	public CustomNamedFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, String name, FunctionType functionType,
			List<KeywordFormal> initializers,
			boolean varargs, boolean isDefault, boolean isTest, Environment env, Stack<Accumulator> accumulators) {
		super(ast, eval, functionType, initializers, name, varargs, isDefault, isTest, env);
		this.isVoidFunction = this.functionType.getReturnType().isSubtypeOf(TF.voidType());
		this.accumulators = (Stack<Accumulator>) accumulators.clone();
		this.formals = cacheFormals();
		this.firstOutermostLabel = computeFirstOutermostLabel(ast);
		this.firstOutermostProduction = computeFirstOutermostProduction(ast);
		this.initializers = initializers;
	}
	
	@Override
	public abstract CustomNamedFunction cloneInto(Environment env);

	AbstractAST cloneAst() {
		return (AbstractAST) getAst().clone();
	}

	private String computeFirstOutermostLabel(AbstractAST ast) {
		return ast.accept(new NullASTVisitor<String>() {
			@Override
			public String visitFunctionDeclarationDefault(Default x) {
				return extract(x);
			}

			@Override
			public String visitFunctionDeclarationExpression(
					org.rascalmpl.ast.FunctionDeclaration.Expression x) {
				return extract(x);
			}
			
			
			@Override
			public String visitFunctionDeclarationConditional(Conditional x) {
				return extract(x);
			}
			
			private String extract(FunctionDeclaration x) {
				List<Expression> formals = x.getSignature().getParameters().getFormals().getFormals();
				return processFormals(formals);
			}
			
			private String processFormals(List<Expression> formals) {
				if (formals.size() > 0) {
					Expression first = formals.get(0);
					
					if (first.isAsType()) {
						first = first.getArgument();
					}
					else if (first.isTypedVariableBecomes() || first.isVariableBecomes()) {
						first = first.getPattern();
					}
					
					if (first.isCallOrTree() && first.getExpression().isQualifiedName()) {
						return ((org.rascalmpl.semantics.dynamic.QualifiedName.Default) first.getExpression().getQualifiedName()).lastName();
					}
				}

				return null;
			}
		});
	}


	@Override
	public String getFirstOutermostConstructorLabel() {
		return firstOutermostLabel;
	}
	
	@Override
	public IConstructor getFirstOutermostProduction() {
		return firstOutermostProduction;
	}
	
	final static List<Expression> cacheFormals(Parameters params) throws ImplementationError {
		List<Expression> formals;
		formals = params.getFormals().getFormals();
		
		if (params.isVarArgs() && formals.size() > 0) {
			// deal with varags, change the last argument to a list if its not a pattern
			Expression last = formals.get(formals.size() - 1);
			if (last.isTypedVariable()) {
				org.rascalmpl.ast.Type oldType = last.getType();
				ISourceLocation origin = last.getLocation();
				Structured newType = ASTBuilder.make("Type","Structured", origin, ASTBuilder.make("StructuredType",origin, ASTBuilder.make("BasicType","List", origin), Arrays.asList(ASTBuilder.make("TypeArg","Default", origin,oldType))));
				last = ASTBuilder.make("Expression","TypedVariable",origin, newType, last.getName());
				formals = replaceLast(formals, last);
			}
			else if (last.isQualifiedName()) {
				ISourceLocation origin = last.getLocation();
				org.rascalmpl.ast.Type newType = ASTBuilder.make("Type","Structured",origin, ASTBuilder.make("StructuredType",origin, ASTBuilder.make("BasicType","List", origin), Arrays.asList(ASTBuilder.make("TypeArg",origin, ASTBuilder.make("Type","Basic", origin, ASTBuilder.make("BasicType","Value", origin))))));
				last = ASTBuilder.makeExp("TypedVariable", origin, newType, Names.lastName(last.getQualifiedName()));
				formals = replaceLast(formals, last);
			}
			else {
				throw new UnsupportedPattern("...", last);
			}
		}
		
		return formals;
	}
	
	List<Expression> cacheFormals() throws ImplementationError {
		if (ast instanceof FunctionDeclaration) {
			return cacheFormals( ((FunctionDeclaration) ast).getSignature().getParameters() );
		}
		if (ast instanceof Closure) {
			return cacheFormals( ((Closure) ast).getParameters() );
		}
		if (ast instanceof VoidClosure) {
			return cacheFormals( ((VoidClosure) ast).getParameters() );
		}
		throw new ImplementationError("Unexpected kind of Rascal function: " + ast);
		
	}
	
	@Override
	public boolean isStatic() {
		return isStatic;
	}
	


	public boolean isAnonymous() {
		return getName() == null;
	}

	@Override
  public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
	  Result<IValue> result = getMemoizedResult(actuals, keyArgValues);
	  if (result != null) { 
	    return result;
	  }
	
    Environment old = ctx.getCurrentEnvt();
    AbstractAST currentAST = ctx.getCurrentAST();
    AbstractAST oldAST = currentAST;
    Stack<Accumulator> oldAccus = ctx.getAccumulators();

    try {
    	if (callTracing) {
    		printStartTrace(actuals);
    	}
    	
      String label = isAnonymous() ? "Anonymous Function" : name;
      Environment environment = new Environment(declarationEnvironment, ctx.getCurrentEnvt(), currentAST != null ? currentAST.getLocation() : null, ast.getLocation(), label);
      ctx.setCurrentEnvt(environment);
      
      IMatchingResult[] matchers = prepareFormals(ctx);
      ctx.setAccumulators(accumulators);
      ctx.pushEnv();

      Type actualTypesTuple = TF.tupleType(actualTypes);
      if (hasVarArgs) {
        actuals = computeVarArgsActuals(actuals, getFormals());
        actualTypesTuple = computeVarArgsActualTypes(actualTypes, getFormals());
      }

      int size = actuals.length;
      Environment[] olds = new Environment[size];
      int i = 0;
      
      
      if (!hasVarArgs && size != this.formals.size()) {
        throw new MatchFailed();
      }

      if (size == 0) {
        try {
          bindKeywordArgs(keyArgValues);
          
          result = run();
          storeMemoizedResult(actuals,keyArgValues, result);
          if (callTracing) {
        	  printEndTrace(result.getValue());
          }
          return result;
        }
        catch (Return e) {
          result = computeReturn(e);
          storeMemoizedResult(actuals,keyArgValues, result);
          return result;
        }
      }
      
      matchers[0].initMatch(makeResult(actualTypesTuple.getFieldType(0), actuals[0], ctx));
      olds[0] = ctx.getCurrentEnvt();
      ctx.pushEnv();

      // pattern matching requires backtracking due to list, set and map matching and
      // non-linear use of variables between formal parameters of a function...
      
      while (i >= 0 && i < size) {
        if (ctx.isInterrupted()) { 
          throw new InterruptException(ctx.getStackTrace(), currentAST.getLocation());
        }
        if (matchers[i].hasNext() && matchers[i].next()) {
          if (i == size - 1) {
            // formals are now bound by side effect of the pattern matcher
            try {
              bindKeywordArgs(keyArgValues);
              result = run();
              storeMemoizedResult(actuals,keyArgValues, result);
              return result;
            }
            catch (Failure e) {
              // backtrack current pattern assignment
              if (!e.hasLabel() || e.hasLabel() && e.getLabel().equals(getName())) {
                continue;
              }
              else {
                throw new UnguardedFail(getAst(), e);
              }
            }
          }
          else {
            i++;
            matchers[i].initMatch(makeResult(actualTypesTuple.getFieldType(i), actuals[i], ctx));
            olds[i] = ctx.getCurrentEnvt();
            ctx.pushEnv();
          }
        } else {
          ctx.unwind(olds[i]);
          i--;
          ctx.pushEnv();
        }
      }
      
      // backtrack to other function body
      throw new MatchFailed();
    }
    catch (Return e) {
      result = computeReturn(e);
      storeMemoizedResult(actuals,keyArgValues, result);
      if (callTracing) {
    	  printEndTrace(result.getValue());
      }
      return result;
    } 
    catch (Throwable e) {
    	if (callTracing) {
            printExcept(e);
    	}
    	throw e;
    }
    finally {
    	if (callTracing) {
    		callNesting--;
    	}
      ctx.setCurrentEnvt(old);
      ctx.setAccumulators(oldAccus);
      ctx.setCurrentAST(oldAST);
    }
  }

	abstract Result<IValue> run();


	private Result<IValue> computeReturn(Return e) {
		Result<IValue> result = e.getValue();

		Type returnType = getReturnType();
		Type instantiatedReturnType = returnType.instantiate(ctx.getCurrentEnvt().getTypeBindings());

		if(!result.getType().isSubtypeOf(instantiatedReturnType)){
			throw new UnexpectedType(instantiatedReturnType, result.getType(), e.getLocation());
		}

		if (!returnType.isBottom() && result.getType().isBottom()) {
			throw new UnexpectedType(returnType, result.getType(), e.getLocation());
		}

		return makeResult(instantiatedReturnType, result.getValue(), eval);
	}
	
	private IMatchingResult[] prepareFormals(IEvaluatorContext ctx) {
		int size = formals.size();
		IMatchingResult[] matchers = new IMatchingResult[size];
		
		for (int i = 0; i < size; i++) {
			matchers[i] = formals.get(i).getMatcher(ctx);
		}
		
		return matchers;
	}

	private static List<Expression> replaceLast(List<Expression> formals,
			Expression last) {
		List<Expression> tmp = new ArrayList<Expression>(formals.size());
		tmp.addAll(formals);
		tmp.set(formals.size() - 1, last);
		formals = tmp;
		return formals;
	}
	
	@Override
	public <V extends IValue> Result<IBool> equals(Result<V> that) {
		return ResultFactory.bool((this == that), ctx);
	}

	private IConstructor computeFirstOutermostProduction(AbstractAST ast) {
		return ast.accept(new NullASTVisitor<IConstructor>() {
			@Override
			public IConstructor visitFunctionDeclarationDefault(Default x) {
				return extract(x);
			}
	
			@Override
			public IConstructor visitFunctionDeclarationExpression(
					org.rascalmpl.ast.FunctionDeclaration.Expression x) {
				return extract(x);
			}
			
			
			@Override
			public IConstructor visitFunctionDeclarationConditional(Conditional x) {
				return extract(x);
			}
			
			private IConstructor extract(FunctionDeclaration x) {
				List<Expression> formals = x.getSignature().getParameters().getFormals().getFormals();
				return processFormals(formals);
			}
			
			private IConstructor processFormals(List<Expression> formals) {
				if (formals.size() > 0) {
					Expression first = formals.get(0);
					
					if (first.isAsType()) {
						first = first.getArgument();
					}
					else if (first.isTypedVariableBecomes() || first.isVariableBecomes()) {
						first = first.getPattern();
					}
					
					if (first instanceof Tree.Appl) {
						Tree.Appl appl = (Appl) first;
						return appl.getProduction();
					}
				}
	
				return null;
			}
		});
	}
	
	@Override
	protected Type computeVarArgsActualTypes(Type[] actualTypes, Type formals) {
		if (formals.getArity() == actualTypes.length 
				&& actualTypes[actualTypes.length - 1].isSubtypeOf(formals.getFieldType(formals.getArity() - 1))) {
			// variable length argument is provided as a list
			return TF.tupleType(actualTypes);
		}
		
		int arity = formals.getArity();
		Type[] types = new Type[arity];
		int i;
		
		for (i = 0; i < arity - 1; i++) {
			types[i] = actualTypes[i];
		}
		
		Type lub = TF.voidType();
		for (int j = i; j < actualTypes.length; j++) {
			lub = lub.lub(actualTypes[j]);
		}
		
		types[i] = TF.listType(lub);
		
		return TF.tupleType(types);
	}
}
