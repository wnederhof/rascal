package org.rascalmpl.interpreter.result.sugar;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.IdentityVisitor;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.NamedFunction;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.sugar.Resugar;
import org.rascalmpl.interpreter.sugar.ResugarTransformer;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;

// TODO: compute outermost label, production and implement cacheFormals for improved efficiency.
public class ResugarFunction extends NamedFunction {
	private FunctionDeclaration functionDeclaration;
	private IValue originalTerm;

	private static FunctionType createResugarFunctionType(FunctionDeclaration functionDeclaration,
			IEvaluator<Result<IValue>> eval, Environment env) {
		RascalTypeFactory RTF = org.rascalmpl.interpreter.types.RascalTypeFactory.getInstance();
		Type returnType = functionDeclaration.getTypeLhs().typeOf(env, true, eval);
		Type argType = functionDeclaration.getPatternRhs().typeOf(env, true, eval);
		Type argTypes = TypeFactory.getInstance().tupleType(argType);
		return (FunctionType) RTF.functionType(returnType, argTypes, TF.voidType());
	}

	public ResugarFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, FunctionDeclaration functionDeclaration,
			String name, Environment env, IValue originalTerm) {
		super(ast, eval, createResugarFunctionType(functionDeclaration, eval, env), new LinkedList<>(), name, false,
				false, false, env);
		this.functionDeclaration = functionDeclaration;
		this.originalTerm = originalTerm;
	}

	@Override
	public ICallableValue cloneInto(Environment env) {
		return new ResugarFunction(ast, eval, functionDeclaration, name, env, originalTerm);
	}

	@Override
	public boolean isStatic() {
		return getEnv().isRootScope() && eval.__getRootScope() != getEnv();
	}

	private Result<IValue> resugar(ISourceLocation src, Result<IValue> resultToDesugar) {
		IValueVisitor<IValue, RuntimeException> identityVisitor = new IdentityVisitor<RuntimeException>() {};
		ResugarTransformer<RuntimeException> desugarTransformer = new ResugarTransformer<RuntimeException>(
				identityVisitor, vf, eval);
		Resugar resugar = new Resugar(
				functionDeclaration.getPatternLhs(), // surface
				functionDeclaration.getPatternRhs(), // core
				desugarTransformer,
				eval,
				tags.containsKey("repeatMode"));
		return resugar.resugar(resultToDesugar, originalTerm);
	}
	
	private void ensureNoVariablesLeak(Environment old) {
		eval.setCurrentEnvt(new StayInScopeEnvironment(old));
	}

	@Override
	public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
		if (!keyArgValues.isEmpty() || actualTypes.length != 1 || actuals.length != 1)
			throw new MatchFailed();
		IValue termToDesugar = actuals[0];
		Type termToDesugarType = actualTypes[0];
		Result<IValue> resultToDesugar = ResultFactory.makeResult(termToDesugarType, termToDesugar, ctx);
		Environment old = ctx.getCurrentEnvt();
		try {
			ensureNoVariablesLeak(declarationEnvironment);
			IMatchingResult matcher = functionDeclaration.getPatternLhs().buildMatcher(eval);
			ISourceLocation src = eval.getCurrentAST().getLocation();
			if (matcher.next()) {
				return resugar(src, resultToDesugar);
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}
}
