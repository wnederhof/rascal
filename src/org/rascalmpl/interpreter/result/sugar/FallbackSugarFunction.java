package org.rascalmpl.interpreter.result.sugar;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.Name;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.NamedFunction;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.staticErrors.UndeclaredVariable;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Names;

public class FallbackSugarFunction extends NamedFunction {

	private FunctionDeclaration functionDeclaration;
	
	private static FunctionType createFallbackSugarFunctionType(FunctionDeclaration functionDeclaration,
			IEvaluator<Result<IValue>> eval, Environment env) {
		RascalTypeFactory RTF = org.rascalmpl.interpreter.types.RascalTypeFactory.getInstance();
		Type returnType = TF.valueType(); // TODO
		Type argType = functionDeclaration.getPatternCore().typeOf(env, true, eval);
		Type argTypes = TypeFactory.getInstance().tupleType(TF.stringType(), argType);
		return (FunctionType) RTF.functionType(returnType, argTypes, TF.voidType());
	}

	public FallbackSugarFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, FunctionDeclaration functionDeclaration,
			String name, Environment env) {
		super(ast, eval, createFallbackSugarFunctionType(functionDeclaration, eval, env), new LinkedList<>(), name,
				false, false, false, env);
		this.functionDeclaration = functionDeclaration;
	}
	
	@Override
	public ICallableValue cloneInto(Environment env) {
		return new FallbackSugarFunction(ast, eval, functionDeclaration, name, env);
	}

	@Override
	public boolean isStatic() {
		return getEnv().isRootScope() && eval.__getRootScope() != getEnv();
	}
	
	private void ensureNoVariablesLeak(Environment old) {
		eval.setCurrentEnvt(new StayInScopeEnvironment(old));
	}
	
	private boolean matchesSugarType(IValue actual) {
		if (!(actual instanceof IString)) {
			return false;
		}
		String sugarType = ((IString) actual).getValue();
		for (Name n : functionDeclaration.getOptionalSugarType().getNames()) {
			String name = Names.name(n);
			if (sugarType.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
		if (keyArgValues != null && (!keyArgValues.isEmpty() || actualTypes.length != 1 || actuals.length != 1))
			throw new MatchFailed();
		
		if (!matchesSugarType(actuals[0]))
			throw new MatchFailed();
		
		IValue termToResugar = actuals[1];
		Type termToResugarType = actualTypes[1];
		Result<IValue> resultToResugar = ResultFactory.makeResult(termToResugarType, termToResugar, ctx);
		Environment old = ctx.getCurrentEnvt();
		try {
			ensureNoVariablesLeak(declarationEnvironment);
			IMatchingResult matcher = functionDeclaration.getPatternCore().buildMatcher(eval);
			matcher.initMatch(resultToResugar);
			try {
				if (matcher.next()) {
					return functionDeclaration.getPatternSurface().interpret(eval);
				}
			} catch(UndeclaredVariable e) {
				// This occurs when the surface pattern has more variables than the core pattern,
				// and the variable was not in the original term's environment.
				throw new MatchFailed();
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}

}
