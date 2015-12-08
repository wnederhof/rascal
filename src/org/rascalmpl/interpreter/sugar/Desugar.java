package org.rascalmpl.interpreter.sugar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.ValueFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.IdentityVisitor;
import org.rascalmpl.ast.Expression;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.SugarFunctionMapping;
import org.rascalmpl.ast.Tag;
import org.rascalmpl.ast.TagString;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.StayInScopeEnvironment;
import org.rascalmpl.interpreter.matching.IMatchingResult;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.matching.visitor.IValueMatchingResultVisitor;
import org.rascalmpl.interpreter.matching.visitor.IdentityValueMatchingResultVisitor;
import org.rascalmpl.interpreter.matching.visitor.PatternNodeIDAccumulator;
import org.rascalmpl.interpreter.matching.visitor.TagDesugared;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.result.sugar.ResugarFunction;
import org.rascalmpl.interpreter.utils.Names;

public class Desugar {
	private Expression corePattern;
	private DesugarTransformer<RuntimeException> desugarTransformer;
	private IEvaluator<Result<IValue>> eval;
	private boolean repeatMode;
	private FunctionDeclaration functionDeclaration;
	private PatternNodeIDAccumulator patternNodeIdAccumulator;
	private Map<String, Integer> maxEllipsisVariablesLength;
	
	private Result<IValue> makeResult(IValue v) {
		return ResultFactory.makeResult(v.getType(), v, eval);
	}
	
	private IValue getVariable(IVarPattern varPattern) {
		return getVariable(varPattern.name());
	}
	
	private IValue getVariable(String name) {
		try {
			return eval.getCurrentEnvt().getVariable(name).getValue();
		} catch(Exception e) {
			System.out.println("Could not find variable: " + name);
			throw new MatchFailed();
		}
	}
	
	private void setVariable(String name, IValue value) {
		eval.getCurrentEnvt().storeLocalVariable(name, makeResult(value));
	}
	
	private Result<IValue> attachResugarFunction(Result<IValue> coreTerm, IValue originalTerm) {
		//System.out.println("Attach.");
		return makeResult(SugarParameters.attachSugarKeywordsLayer(coreTerm.getValue(),
				new ResugarFunction(eval.getCurrentAST(), eval, functionDeclaration,
						Names.name(functionDeclaration.getName()), eval.getCurrentEnvt(), originalTerm,
						patternNodeIdAccumulator, maxEllipsisVariablesLength)));
	}

	private Map<String, Integer> accumulateMaxEllipsisVariablesLength() {
		Map<String, Integer> maxEllipsisVariablesLength = new HashMap<String, Integer>();
		if (!functionDeclaration.hasTags() || !functionDeclaration.getTags().hasTags()) return null;
		for (Tag t : functionDeclaration.getTags().getTags()) {
			String name = Names.name(t.getName());
			if (name.equals("fixedLength")) {
				if (t.hasContents()) {
					String contents = ((TagString.Lexical) t.getContents()).getString();
					if (contents.length() > 2 && contents.startsWith("{")) {
                        contents = contents.substring(1, contents.length() - 1);
                    }
					maxEllipsisVariablesLength.put(contents, getLength(eval.getCurrentEnvt().getVariable(contents)));
				} else {
					throw new RuntimeException("No contents specified for @fixedLength tag.");
				}
			}
		}
		return maxEllipsisVariablesLength;
	}

	private Integer getLength(Result<IValue> variable) {
		IValue value = variable.getValue();
		// TODO add support for multipattern.
		// Concrete
		if (value instanceof IList) {
			return ((IList) value).length();
		} else if (value instanceof ISet) {
			return ((ISet) value).size();
		}
		System.out.println(value.getClass());
		
		// TODO: Bit hacky, but works for concrete ellipses.
		if (value instanceof Iterable) {
			Iterator<?> it = ((Iterable<?>) value).iterator();
			it.next(); // TODO: Evil hack.
			value = (IValue) it.next();
			if (value instanceof IList) {
				return ((IList) value).length();
			} else if (value instanceof ISet) {
				return ((ISet) value).size();
			} else if (value instanceof Iterable) {
				int i = 0;
				Iterator<?> iterator = ((Iterable<?>) value).iterator();
				while (iterator.hasNext()) {
					i++;
					iterator.next();
				}
				return i;
			}
		}
		throw new RuntimeException("Provided variable is not an ellipsis variable.");
	}

	private IValue desugarTransform(IValueVisitor<IValue,RuntimeException> transformer, Result<IValue> subject) {
		return desugarTransform(transformer, subject.getValue());
	}
	
	private IValue desugarTransform(IValueVisitor<IValue,RuntimeException> transformer, IValue subject) {
		return subject.accept(transformer);
	}

	private void desugarPatternVariables() {
		// We only need to desugar the variables in the core pattern.
		start: for (IVarPattern varPattern : corePattern.buildMatcher(eval).getVariables()) {
			if (functionDeclaration.hasOptionalUsing() && functionDeclaration.getOptionalUsing().isDefault()) {
				for (SugarFunctionMapping sfm : functionDeclaration.getOptionalUsing().getSugarFunctionMapping()) {
					if (Names.name(sfm.getFrom()).equals(varPattern.name())) {
						DesugarTransformer<RuntimeException> customDesugarTransformer = new DesugarTransformer<RuntimeException>(
								new IdentityVisitor<RuntimeException>() {}, ValueFactory.getInstance(), eval, sfm.getTo());
						setVariable(varPattern.name(), desugarTransform(customDesugarTransformer, getVariable(varPattern)));
						continue start;
					}
				}
			}
			setVariable(varPattern.name(), desugarTransform(desugarTransformer, getVariable(varPattern)));
		}
	}
	
	private boolean ellipsisVariablesLengthsAreCorrect() {
		// class org.rascalmpl.values.uptr.RascalValueFactory$Appl1
		for (String s : maxEllipsisVariablesLength.keySet()) {
			int i = getLength(eval.getCurrentEnvt().getVariable(s));
			if (i != maxEllipsisVariablesLength.get(s)) {
				return false;
			}
		}
		return true;
	}

	private Result<IValue> desugarTerm(IValue originalTerm) {
		Result<IValue> coreTerm = corePattern.interpret(eval);
		// if (true) return coreTerm;
		
		// imrv tags the inner nodes with a unique identifer,
		// while patternNodeIdAccumulator accumulates these
		// values for comparing node identity during resugaring.
		IMatchingResult imr = corePattern.buildMatcher(eval);
		imr.initMatch(coreTerm);
		while (imr.hasNext() && imr.next()) {
			patternNodeIdAccumulator = new PatternNodeIDAccumulator(
					new IdentityValueMatchingResultVisitor(),
					new IdentityValueMatchingResultVisitor());
			IValueMatchingResultVisitor imrv = new TagDesugared(
					patternNodeIdAccumulator,
					new IdentityValueMatchingResultVisitor());
			IValue v;
			try {
				List<IValue> vs = imr.accept(imrv);
				v = vs.get(0);
			} catch(Exception e) {
				e.printStackTrace();
				throw e;
			}
			if (!ellipsisVariablesLengthsAreCorrect())
				continue;
			return makeResult(v);
		}
		throw new MatchFailed();
	}

	private Result<IValue> desugarTermAndTransform(IValue originalTerm) {
		IValue transformedTerm;
		Result<IValue> desugaredTerm = desugarTerm(originalTerm);
		if (functionDeclaration.hasOptionalUsingOneDesugaring() && functionDeclaration.getOptionalUsingOneDesugaring().hasQualifiedName()) {
			DesugarTransformer<RuntimeException> customDesugarTransformer = new DesugarTransformer<RuntimeException>(
					new IdentityVisitor<RuntimeException>() {}, ValueFactory.getInstance(), eval,
					functionDeclaration.getOptionalUsingOneDesugaring().getQualifiedName());
			transformedTerm = desugarTransform(customDesugarTransformer, desugaredTerm);
		} else {
			transformedTerm = desugarTransform(desugarTransformer, desugaredTerm);
		}
		return attachResugarFunction(makeResult(transformedTerm), originalTerm);
	}

	private void ensureNoVariablesLeak(Environment old) {
		eval.setCurrentEnvt(new StayInScopeEnvironment(old));
	}
	
	public Result<IValue> desugar(Result<IValue> toDesugar) {
		Environment old = eval.getCurrentEnvt();
		try {
			ensureNoVariablesLeak(old);
			// We have already matched the surface pattern,
			// so we can now get the max ellipsis variables lengths.
			maxEllipsisVariablesLength = accumulateMaxEllipsisVariablesLength();
			/* In repeat mode, variables are not desugared,
			 * but DesugarTransformer is used to desugar top-down,
			 * similar to Confection's desugaring technique. */
			if (!repeatMode) {
				desugarPatternVariables();
				//tagVariables();
				return attachResugarFunction(desugarTerm(toDesugar.getValue()), toDesugar.getValue());
			}
			return desugarTermAndTransform(toDesugar.getValue());
		} finally {
			eval.unwind(old);
		}
	}

	public Desugar(DesugarTransformer<RuntimeException> desugarTransformer,
			IEvaluator<Result<IValue>> eval, boolean repeatMode, FunctionDeclaration functionDeclaration) {
		this.corePattern = functionDeclaration.getPatternCore();
		this.desugarTransformer = desugarTransformer;
		this.eval = eval;
		this.repeatMode = repeatMode;
		this.functionDeclaration = functionDeclaration;
	}
}
