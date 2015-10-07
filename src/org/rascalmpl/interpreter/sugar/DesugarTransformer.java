package org.rascalmpl.interpreter.sugar;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AnnotatedConstructorFacade;
import org.eclipse.imp.pdb.facts.visitors.BottomUpTransformer;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.rascalmpl.ast.QualifiedName;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.control_exceptions.MatchFailed;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.types.NonTerminalType;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.values.uptr.SymbolAdapter;

public class DesugarTransformer<E extends Throwable> extends BottomUpTransformer<E> {
	protected IEvaluator<Result<IValue>> eval;
	private QualifiedName desugarName;

	public DesugarTransformer(IValueVisitor<IValue,E> visitor, IValueFactory factory,
			IEvaluator<Result<IValue>> eval, QualifiedName desugarName) {
		super(visitor, factory);
		this.eval = eval;
		this.desugarName = desugarName;
	}

	private IValue desugar(IValue v) {
		return eval.call(Names.fullName(desugarName), v);
	}
	
	private boolean isDesugarCandidate(IConstructor o) {
		if (o instanceof AnnotatedConstructorFacade) {
			AnnotatedConstructorFacade acf = (AnnotatedConstructorFacade) o;
			if (acf.getType() instanceof NonTerminalType) {
				NonTerminalType ntt = (NonTerminalType) acf.getType();
				String name = SymbolAdapter.toString(ntt.getSymbol(), false);
				if (eval.getCurrentEnvt().lookupAbstractDataType(name) != null
						|| eval.getCurrentEnvt().lookupConcreteSyntaxType(name) != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	private IValue desugarChildren(IConstructor o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o = o.set(i, o.get(i).accept(this));
		}
		return fVisitor.visitConstructor(o);
	}
	
	public IValue visitConstructor(IConstructor o) throws E {
		if (isDesugarCandidate(o)) {
			try {
				return desugar(o);
			} catch(MatchFailed e) {
				return desugarChildren(o);
			}
		}
		return desugarChildren(o);
	}
}