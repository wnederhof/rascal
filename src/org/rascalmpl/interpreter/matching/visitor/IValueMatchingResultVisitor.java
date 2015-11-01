package org.rascalmpl.interpreter.matching.visitor;

import java.util.List;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.interpreter.matching.ConcreteApplicationPattern;
import org.rascalmpl.interpreter.matching.ConcreteListPattern;
import org.rascalmpl.interpreter.matching.ConcreteListVariablePattern;
import org.rascalmpl.interpreter.matching.ConcreteOptPattern;
import org.rascalmpl.interpreter.matching.DescendantPattern;
import org.rascalmpl.interpreter.matching.GuardedPattern;
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

/**
 * The IValueMatchingResultVisitor is a visitor that accepts a matching result
 * and that result's corresponding match value. Note that we implemented the
 * visitor on a limited number of patterns (i.e. all ExpressionAndPatterns).
 * 
 * @author wouter
 *
 */
public interface IValueMatchingResultVisitor {

	List<IValue> visit(ListPattern a, List<IValue> list);

	List<IValue> visit(ConcreteApplicationPattern a, List<IValue> list);

	List<IValue> visit(ConcreteListPattern a, List<IValue> list);

	List<IValue> visit(ConcreteListVariablePattern a, List<IValue> list);

	List<IValue> visit(ConcreteOptPattern a, List<IValue> list);

	List<IValue> visit(DescendantPattern a, List<IValue> list);

	List<IValue> visit(GuardedPattern a, List<IValue> list);

	List<IValue> visit(LiteralPattern a, List<IValue> list);

	List<IValue> visit(NegativePattern a, List<IValue> list);

	List<IValue> visit(NodePattern a, List<IValue> list);

	List<IValue> visit(QualifiedNamePattern a, List<IValue> list);

	List<IValue> visit(RegExpPatternValue a, List<IValue> list);

	List<IValue> visit(ReifiedTypePattern a, List<IValue> list);

	List<IValue> visit(SetPattern a, List<IValue> list);

	List<IValue> visit(TuplePattern a, List<IValue> list);

	List<IValue> visit(TypedVariablePattern a, List<IValue> list);

	List<IValue> visit(ValuePattern a, List<IValue> list);

	List<IValue> visit(VariableBecomesPattern a, List<IValue> list);
	
	List<IValue> visit(MultiVariablePattern a, List<IValue> list);

	List<IValue> visit(TypedMultiVariablePattern a, List<IValue> list);
}
