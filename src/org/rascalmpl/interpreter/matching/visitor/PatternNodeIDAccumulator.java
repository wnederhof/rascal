package org.rascalmpl.interpreter.matching.visitor;

import java.util.List;

import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.interpreter.matching.AbstractMatchingResult;
import org.rascalmpl.interpreter.sugar.SugarParameters;

public class PatternNodeIDAccumulator extends InnerOuterVisitor {
	private final int prime = 31; // Source: http://stackoverflow.com/questions/2730865/how-do-i-calculate-a-good-hash-code-for-a-list-of-strings
	private final int emptyStringHash = "".hashCode(); // note that tagValues are never empty.
	private int hashResult = 1;
	
	@Override
	public List<IValue> visitInner(AbstractMatchingResult result, List<IValue> list) {
		for (IValue l : list) {
			if (SugarParameters.tagValue(l) != null) {
				if (SugarParameters.tagValue(l) instanceof IString) {
					IString s = (IString) SugarParameters.tagValue(l);
					hashResult = hashResult * prime + s.getValue().hashCode();
					continue;
				}
			}
			hashResult = hashResult * prime + emptyStringHash;
		}
		return super.visitInner(result, list);
	}
	
	@Override
	public int hashCode() {
		return hashResult;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof PatternNodeIDAccumulator) ? obj.hashCode() == this.hashCode() : false;
	}
	
	public PatternNodeIDAccumulator(IValueMatchingResultVisitor innerNext, IValueMatchingResultVisitor outerNext) {
		super(innerNext, outerNext);
	}
}
