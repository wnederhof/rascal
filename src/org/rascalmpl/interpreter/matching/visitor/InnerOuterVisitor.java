package org.rascalmpl.interpreter.matching.visitor;

import java.util.List;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.interpreter.matching.AbstractMatchingResult;
import org.rascalmpl.interpreter.matching.ConcreteApplicationPattern;
import org.rascalmpl.interpreter.matching.ConcreteListPattern;
import org.rascalmpl.interpreter.matching.ConcreteListVariablePattern;
import org.rascalmpl.interpreter.matching.ConcreteOptPattern;
import org.rascalmpl.interpreter.matching.DescendantPattern;
import org.rascalmpl.interpreter.matching.GuardedPattern;
import org.rascalmpl.interpreter.matching.IVarPattern;
import org.rascalmpl.interpreter.matching.ListPattern;
import org.rascalmpl.interpreter.matching.LiteralPattern;
import org.rascalmpl.interpreter.matching.MultiVariablePattern;
import org.rascalmpl.interpreter.matching.NegativePattern;
import org.rascalmpl.interpreter.matching.NodePattern;
import org.rascalmpl.interpreter.matching.QualifiedNamePattern;
import org.rascalmpl.interpreter.matching.RegExpPatternValue;
import org.rascalmpl.interpreter.matching.ReifiedTypePattern;
import org.rascalmpl.interpreter.matching.SetPattern;
import org.rascalmpl.interpreter.matching.TuplePattern;
import org.rascalmpl.interpreter.matching.TypedMultiVariablePattern;
import org.rascalmpl.interpreter.matching.TypedVariablePattern;
import org.rascalmpl.interpreter.matching.ValuePattern;
import org.rascalmpl.interpreter.matching.VariableBecomesPattern;

public class InnerOuterVisitor implements IValueMatchingResultVisitor {
	
	private IValueMatchingResultVisitor innerNext;
	private IValueMatchingResultVisitor outerNext;

	public List<IValue> visitInner(AbstractMatchingResult result, List<IValue> list) {
		return list;
	}
	
	public List<IValue> visitOuter(IVarPattern result, List<IValue> list) {
		return list;
	}

	@Override
	public List<IValue> visit(ListPattern listPattern, List<IValue> list) {
		return innerNext.visit(listPattern, visitInner(listPattern, list));
	}

	@Override
	public List<IValue> visit(ConcreteApplicationPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(ConcreteListPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(ConcreteListVariablePattern a, List<IValue> list) {
		return outerNext.visit(a, visitOuter(a, list));
	}

	@Override
	public List<IValue> visit(ConcreteOptPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(DescendantPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(GuardedPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(LiteralPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(NegativePattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(NodePattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(QualifiedNamePattern a, List<IValue> list) {
		return outerNext.visit(a, visitOuter(a, list));
	}

	@Override
	public List<IValue> visit(RegExpPatternValue a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(ReifiedTypePattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(SetPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(TuplePattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(TypedVariablePattern a, List<IValue> list) {
		return outerNext.visit(a, visitOuter(a, list));
	}

	@Override
	public List<IValue> visit(ValuePattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(VariableBecomesPattern a, List<IValue> list) {
		return innerNext.visit(a, visitInner(a, list));
	}

	@Override
	public List<IValue> visit(TypedMultiVariablePattern a, List<IValue> list) {
		return outerNext.visit(a, visitOuter(a, list));
	}

	@Override
	public List<IValue> visit(MultiVariablePattern a, List<IValue> list) {
		return outerNext.visit(a, visitOuter(a, list));
	}
	
	public InnerOuterVisitor(IValueMatchingResultVisitor innerNext, IValueMatchingResultVisitor outerNext) {
		this.innerNext = innerNext;
		this.outerNext = outerNext;
	}

}
