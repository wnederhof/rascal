package org.rascalmpl.interpreter.matching;

import java.util.List;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IValue;

public interface IMatchingResultVisitor {
	public List<IValue> visit(ListPattern listPattern,  List<IValue> list);
	public List<IValue> visit(ConcreteApplicationPattern a, List<IValue> list);
	public List<IValue> visit(ConcreteListPattern a, List<IValue> list);
	public List<IValue> visit(ConcreteListVariablePattern a, List<IValue> list);
	public List<IValue> visit(ConcreteOptPattern a, List<IValue> list);
	public List<IValue> visit(DescendantPattern a, List<IValue> list);
	public List<IValue> visit(GuardedPattern a, List<IValue> list);
	public List<IValue> visit(LiteralPattern a, List<IValue> list);
	public List<IValue> visit(NegativePattern a, List<IValue> list);
	public List<IValue> visit(NodePattern a, List<IValue> list);
	public List<IValue> visit(QualifiedNamePattern a, List<IValue> list);
	public List<IValue> visit(RegExpPatternValue a, List<IValue> list);
	public List<IValue> visit(ReifiedTypePattern a, List<IValue> list);
	public List<IValue> visit(SetPattern a, List<IValue> list);
	public List<IValue> visit(TuplePattern a, List<IValue> list);
	public List<IValue> visit(TypedVariablePattern a, List<IValue> list);
	public List<IValue> visit(ValuePattern a, List<IValue> list);
	public List<IValue> visit(VariableBecomesPattern a, List<IValue> list);
}
