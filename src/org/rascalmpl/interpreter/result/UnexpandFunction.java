package org.rascalmpl.interpreter.result;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.ast.Formals;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.KeywordFormal;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.OptionalComma;
import org.rascalmpl.ast.Signature;
import org.rascalmpl.interpreter.Accumulator;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.SubstitutionEvaluator;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.staticErrors.UnsupportedOperation;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.values.uptr.IRascalValueFactory;

public class UnexpandFunction extends CustomNamedFunction {
	protected static final TypeFactory TF = TypeFactory.getInstance();
	protected static final RascalTypeFactory RTF = RascalTypeFactory.getInstance();
	protected static final IRascalValueFactory VF = IRascalValueFactory.getInstance();
	
	private FunctionDeclaration.Sugar func;
	private boolean varargs;
	private Result<IValue> patternLhsResult;
	
	private static org.rascalmpl.ast.Parameters createParameters(ISourceLocation src, org.rascalmpl.ast.Expression signaturePattern, Formals extraFormals) {
		OptionalComma optionalComma = new OptionalComma.Lexical(src, null, ",");
		List<org.rascalmpl.ast.Expression> listOfFormals = new LinkedList<>();
		listOfFormals.add(signaturePattern);
		
		for (Expression f : extraFormals.getFormals()) {
			listOfFormals.add(f);
		}
		
		org.rascalmpl.ast.Formals formals = ASTBuilder.make("Formals", "Default", src, listOfFormals);
		
		org.rascalmpl.ast.KeywordFormals keywordFormals = ASTBuilder.make("KeywordFormals", "Default", src,
				optionalComma, new LinkedList<org.rascalmpl.ast.KeywordFormal>());
		
		
		return ASTBuilder.make("Parameters", "Default",
				src, formals, keywordFormals);
	}
	
	// This function is used to create a signature out of a sugar function declaration.
	private static Signature createUnexpandSignature(ISourceLocation src, org.rascalmpl.ast.Type type, org.rascalmpl.ast.Expression signaturePattern, Name name, Formals extraFormals) {
		org.rascalmpl.ast.FunctionModifiers modifiers = ASTBuilder.make("FunctionModifiers", "Modifierlist",
				src, new LinkedList<>());
		
		org.rascalmpl.ast.Signature signature =  
				ASTBuilder.make("Signature", "NoThrows",
						src, modifiers,
						type, name, createParameters(src, signaturePattern, extraFormals));
		return signature;
	}
	
	private static FunctionType createFunctionType(FunctionDeclaration.Sugar func, IEvaluator<Result<IValue>> eval, Environment env, Name name, Formals extraFormals) {
		return (FunctionType) createUnexpandSignature(func.getLocation(), func.getTypeLhs(), func.getPatternRhs(), name, extraFormals).typeOf(env, true, eval);
	}
	
	// We don't use keyword formals.
	private static List<KeywordFormal> createFormals(FunctionDeclaration.Sugar func) {
		return new LinkedList<KeywordFormal>();
	}
	
	public UnexpandFunction(IEvaluator<Result<IValue>> eval, FunctionDeclaration.Sugar func, boolean varargs, Environment env,
			Stack<Accumulator> accumulators, Result<IValue> patternLhsResult) {
		this(func, eval,
				Names.name(func.getName()),
				createFunctionType(func, eval, env, func.getName(), func.get()),
				createFormals(func),
				varargs, false, false,
				env, accumulators);
		this.func = func;
		this.varargs = varargs;
		this.patternLhsResult = patternLhsResult;
		for (Expression e : func.getFormals().getFormals()) {
			if (!(e instanceof Expression.QualifiedName)) {
				throw new UnsupportedOperation("Parameter must be a qualified name.", e);
			}
		}
	}

	UnexpandFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, String name, FunctionType functionType,
			List<KeywordFormal> initializers, boolean varargs, boolean isDefault, boolean isTest, Environment env,
			Stack<Accumulator> accumulators) {
		super(ast, eval, name, functionType, initializers, varargs, isDefault, isTest, env, accumulators);
	}
	

	@Override
	public UnexpandFunction cloneInto(Environment env) {
		// TODO Is this necessary? 
		FunctionDeclaration.Sugar clone = (FunctionDeclaration.Sugar) func.clone();
		UnexpandFunction rf = new UnexpandFunction(eval, clone, varargs, env, accumulators, patternLhsResult);
		rf.setPublic(isPublic()); // TODO: should be in constructors
		return rf;
	}

	@Override
	Result<IValue> run() {
		// Here comes the cool stuff.
		// Alright, we got: TypeRHS name (patternLhsResult: PatLHS) => TypeLHS (PatRHS)
		// We want to return PatRHS PatLHS <<< patternLhsResult
		
		Result<IValue> x = SubstitutionEvaluator.substitute(func.getPatternLhs(), patternLhsResult.getValue(), eval);
		return x;
	}
	
	@Override
	List<Expression> cacheFormals() throws ImplementationError {
		// This function is called before func has been initialized. Thus:
		FunctionDeclaration.Sugar func = (FunctionDeclaration.Sugar) ast;
		return cacheFormals(createParameters(ast.getLocation(), func.getPatternRhs(), func.getFormals()));
	}
	
}
