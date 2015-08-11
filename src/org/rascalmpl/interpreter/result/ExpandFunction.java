package org.rascalmpl.interpreter.result;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.KeywordFormal;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.OptionalComma;
import org.rascalmpl.ast.Signature;
import org.rascalmpl.interpreter.Accumulator;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.types.FunctionType;
import org.rascalmpl.interpreter.types.RascalTypeFactory;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.values.uptr.IRascalValueFactory;

public class ExpandFunction extends CustomNamedFunction {
	protected static final TypeFactory TF = TypeFactory.getInstance();
	protected static final RascalTypeFactory RTF = RascalTypeFactory.getInstance();
	protected static final IRascalValueFactory VF = IRascalValueFactory.getInstance();
	
	private FunctionDeclaration.Sugar func;
	private boolean varargs;

	private static org.rascalmpl.ast.Parameters createParameters(ISourceLocation src, org.rascalmpl.ast.Expression signaturePattern) {
		OptionalComma optionalComma = new OptionalComma.Lexical(src, null, ",");
		List<org.rascalmpl.ast.Expression> listOfFormals = new LinkedList<>();
		listOfFormals.add(
				ASTBuilder.makeExp("VariableBecomes", src, Names.toName("__DESUGAR_ORIGINAL_NODE", src), signaturePattern));
		
		org.rascalmpl.ast.Formals formals = ASTBuilder.make("Formals", "Default", src, listOfFormals);
		
		org.rascalmpl.ast.KeywordFormals keywordFormals = ASTBuilder.make("KeywordFormals", "Default", src,
				optionalComma, new LinkedList<org.rascalmpl.ast.KeywordFormal>());
		
		return ASTBuilder.make("Parameters", "Default",
				src, formals, keywordFormals);
	}
	
	// This function is used to create a signature out of a sugar function declaration.
	private static Signature createExpandSignature(ISourceLocation src, org.rascalmpl.ast.Type type, org.rascalmpl.ast.Expression signaturePattern, Name name) {
		org.rascalmpl.ast.FunctionModifiers modifiers = ASTBuilder.make("FunctionModifiers", "Modifierlist",
				src, new LinkedList<>());
		
		org.rascalmpl.ast.Signature signature =  
				ASTBuilder.make("Signature", "NoThrows",
						src, modifiers,
						type, name, createParameters(src, signaturePattern));
		return signature;
	}
	
	private static FunctionType createFunctionType(FunctionDeclaration.Sugar func, IEvaluator<Result<IValue>> eval, Environment env, Name name) {
		return (FunctionType) createExpandSignature(func.getLocation(), func.getTypeRhs(), func.getPatternLhs(), name).typeOf(env, true, eval);
	}
	
	// We don't use keyword formals.
	private static List<KeywordFormal> createFormals(FunctionDeclaration.Sugar func) {
		return new LinkedList<KeywordFormal>();
	}
	
	public ExpandFunction(IEvaluator<Result<IValue>> eval, FunctionDeclaration.Sugar func, boolean varargs, Environment env,
			Stack<Accumulator> accumulators) {
		this(func, eval,
				Names.name(func.getName()),
				createFunctionType(func, eval, env, func.getName()),
				createFormals(func),
				varargs, false, false,
				env, accumulators);
		this.func = func;
		this.varargs = varargs;
		Type onType = func.getTypeRhs().typeOf(eval.getCurrentEnvt(), true, eval);
		Type onTypeLhs = func.getTypeLhs().typeOf(eval.getCurrentEnvt(), true, eval);
		eval.getCurrentModuleEnvironment().declareAnnotation(onType,
				"unexpandFn", TF.valueType());
		eval.getCurrentModuleEnvironment().declareAnnotation(onTypeLhs,
				"unexpansionFailed", TF.valueType());
	}

	ExpandFunction(AbstractAST ast, IEvaluator<Result<IValue>> eval, String name, FunctionType functionType,
			List<KeywordFormal> initializers, boolean varargs, boolean isDefault, boolean isTest, Environment env,
			Stack<Accumulator> accumulators) {
		super(ast, eval, name, functionType, initializers, varargs, isDefault, isTest, env, accumulators);
	}

	@Override
	public ExpandFunction cloneInto(Environment env) {
		// TODO Is this necessary? -- AbstractAST clone = cloneAst();
		FunctionDeclaration.Sugar clone = (FunctionDeclaration.Sugar) func.clone();
		ExpandFunction rf = new ExpandFunction(eval, clone, varargs, env, accumulators);
		rf.setPublic(isPublic()); // TODO: should be in constructors
		return rf;
	}

	@Override
	Result<IValue> run() {
		// Return the expanded tree annotated with unexpandFn, declared in the <i>CURRENT</i> environment (very important).
		Result<IValue> patternRhsResult = func.getPatternRhs().interpret(eval);
		UnexpandFunction unexpandFn = new UnexpandFunction(eval, func, varargs, eval.getCurrentEnvt(), eval.__getAccumulators(),
				eval.getCurrentEnvt().getSimpleVariable("__DESUGAR_ORIGINAL_NODE"));
		Result<IValue> val = ResultFactory.makeResult(unexpandFn.getType(), unexpandFn, eval);
		return patternRhsResult.setAnnotation("unexpandFn", val, eval.getCurrentEnvt());
	}
	
	@Override
	List<Expression> cacheFormals() throws ImplementationError {
		// This function is called before func has been initialized. Thus:
		FunctionDeclaration.Sugar func = (FunctionDeclaration.Sugar) ast;
		return cacheFormals(createParameters(ast.getLocation(), func.getPatternLhs()));
	}

}
