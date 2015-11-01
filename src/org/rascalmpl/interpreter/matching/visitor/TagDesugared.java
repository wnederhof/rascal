package org.rascalmpl.interpreter.matching.visitor;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.interpreter.matching.AbstractMatchingResult;
import org.rascalmpl.interpreter.sugar.SugarParameters;

public class TagDesugared extends InnerOuterVisitor {
	
	public TagDesugared(IValueMatchingResultVisitor innerNext, IValueMatchingResultVisitor outerNext) {
		super(innerNext, outerNext);
	}

	@Override
	public List<IValue> visitInner(AbstractMatchingResult result, List<IValue> list) {
		List<IValue> values = new LinkedList<IValue>();
		for (IValue l : list) {
			values.add(tag(l));
		}
		return values;
	}

	private IValue tag(IValue l) {
		return SugarParameters.tag(l);
	}

}
