package org.rascalmpl.interpreter.sugar;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.BottomUpTransformer;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.rascalmpl.ast.QualifiedName;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;

public class ResugarTransformer<E extends Throwable> extends BottomUpTransformer<E> {
	protected IEvaluator<Result<IValue>> eval;
	private QualifiedName desugarName;

	public ResugarTransformer(IValueVisitor<IValue,E> visitor, IValueFactory factory,
			IEvaluator<Result<IValue>> eval) {
		super(visitor, factory);
		this.eval = eval;
	}
	
	private Result<IValue> makeResult(IValue v) {
		return ResultFactory.makeResult(v.getType(), v, eval);
	}

	private IValue resugar(IValue v) {
		//System.out.println("Resugar attempt.");
		Result<IValue> result = makeResult(SugarParameters.getTopMostResugarFunction(v)).call(
				new Type[] {v.getType()},
				new IValue[] { v },
				null);
		return result.getValue();
	}
	
	private static boolean isResugarCandidate(IConstructor o) {
		return SugarParameters.isResugarable(o);
	}
	
	private IValue resugarChildren(IConstructor o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o = o.set(i, o.get(i).accept(this));
		}
		return fVisitor.visitConstructor(o);
	}
	
	public IValue visitConstructor(IConstructor o) throws E {
		if (isResugarCandidate(o)) {
			try {
				return resugar(o);
			} catch(MatchFailed e) {
				e.printStackTrace();
				return resugarChildren(o);
			}
		}
		return resugarChildren(o);
	}
}
