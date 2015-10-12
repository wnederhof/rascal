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
import org.rascalmpl.interpreter.sugar.Desugar;
import org.rascalmpl.interpreter.sugar.DesugarTransformer;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Names;

// TODO: compute outermost label, production and implement cacheFormals for improved efficiency.
public class DesugarFunction extends NamedFunction {
	private FunctionDeclaration functionDeclaration;

	private static FunctionType createDesugarFunctionType(FunctionDeclaration functionDeclaration,
			IEvaluator<Result<IValue>> eval, Environment env) {
		RascalTypeFactory RTF = org.rascalmpl.interpreter.types.RascalTypeFactory.getInstance();
		Type returnType = functionDeclaration.getTypeCore().typeOf(env, true, eval);
		Type argType = functionDeclaration.getPatternSurface().typeOf(env, true, eval);
		Type argTypes = TypeFactory.getInstance().tupleType(argType);
		return (FunctionType) RTF.functionType(returnType, argTypes, TF.voidType());
	}

	public DesugarFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, FunctionDeclaration functionDeclaration,
			String name, Environment env) {
		super(ast, eval, createDesugarFunctionType(functionDeclaration, eval, env), new LinkedList<>(), name, false,
				false, false, env); // TODO Default.
		this.functionDeclaration = functionDeclaration;
	}

	@Override
	public ICallableValue cloneInto(Environment env) {
		return new DesugarFunction(ast, eval, functionDeclaration, name, env);
	}

	@Override
	public boolean isStatic() {
		return getEnv().isRootScope() && eval.__getRootScope() != getEnv();
	}
	
	private void ensureNoVariablesLeak(Environment old) {
		eval.setCurrentEnvt(new StayInScopeEnvironment(old));
	}
	
	private boolean repeatMode() {
		return functionDeclaration.isSugarConfection();
	}
	
	private Result<IValue> desugar(ISourceLocation src, Result<IValue> resultToDesugar) {
		IValueVisitor<IValue, RuntimeException> identityVisitor = new IdentityVisitor<RuntimeException>() {};
		DesugarTransformer<RuntimeException> desugarTransformer = new DesugarTransformer<RuntimeException>(
				identityVisitor, vf, eval, Names.toQualifiedName(name, src));
		Desugar desugar = new Desugar(
				desugarTransformer,
				eval,
				repeatMode(),
				functionDeclaration);
		return desugar.desugar(resultToDesugar);
	}

	@Override
	public Result<IValue> call(Type[] actualTypes, IValue[] actuals, Map<String, IValue> keyArgValues) {
		if (keyArgValues != null && (!keyArgValues.isEmpty() || actualTypes.length != 1 || actuals.length != 1))
			throw new MatchFailed();
		IValue termToDesugar = actuals[0];
		Type termToDesugarType = actualTypes[0];
		Result<IValue> resultToDesugar = ResultFactory.makeResult(termToDesugarType, termToDesugar, ctx);
		Environment old = ctx.getCurrentEnvt();
		try {
			ensureNoVariablesLeak(declarationEnvironment);
			IMatchingResult matcher = functionDeclaration.getPatternSurface().buildMatcher(eval);
			matcher.initMatch(resultToDesugar);
			ISourceLocation src = eval.getCurrentAST().getLocation();
			if (matcher.next()) {
				return desugar(src, resultToDesugar);
			}
			throw new MatchFailed();
		} finally {
			eval.unwind(old);
		}
	}

}