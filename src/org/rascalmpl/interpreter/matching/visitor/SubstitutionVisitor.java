package org.rascalmpl.interpreter.matching.visitor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.matching.ConcreteListVariablePattern;
import org.rascalmpl.interpreter.matching.QualifiedNamePattern;
import org.rascalmpl.interpreter.matching.TypedMultiVariablePattern;
import org.rascalmpl.interpreter.matching.TypedVariablePattern;
import org.rascalmpl.interpreter.result.Result;

public class SubstitutionVisitor extends IdentityValueMatchingResultVisitor implements IValueMatchingResultVisitor {
	private Environment env;
	private Map<String, Result<IValue>> substitutionVariables;

	@Override
	public List<IValue> visit(ConcreteListVariablePattern a, List<IValue> list) {
		if (substitutionVariables.containsKey(a.name())) {
			return Arrays.asList(substitutionVariables.get(a.name()).getValue());
		} else if (env.getVariable(a.name()) != null) {
			return Arrays.asList(env.getVariable(a.name()).getValue());
		}
		return list;
	}
	
	@Override
	public List<IValue> visit(QualifiedNamePattern a, List<IValue> list) {
		if (substitutionVariables.containsKey(a.name())) {
			return Arrays.asList(substitutionVariables.get(a.name()).getValue());
		} else if (env.getVariable(a.name()) != null) {
			return Arrays.asList(env.getVariable(a.name()).getValue());
		}
		return list;
	}
	
	@Override
	public List<IValue> visit(TypedVariablePattern a, List<IValue> list) {
		// TODO: Typing.
		if (substitutionVariables.containsKey(a.name())) {
//			System.out.println(substitutionVariables.get(a.name()).getValue());
			return Arrays.asList(substitutionVariables.get(a.name()).getValue());
		} else if (env.getVariable(a.name()) != null) {
			return Arrays.asList(env.getVariable(a.name()).getValue());
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<IValue> visit(TypedMultiVariablePattern a, List<IValue> list) {
		// TODO: Typing.
		List<IValue> resultList = new LinkedList<>();
		IValue resultElem;
		if (substitutionVariables.containsKey(a.name())) {
			resultElem = substitutionVariables.get(a.name()).getValue();
			if (resultElem.getType().isList() || resultElem.getType().isSet()) {
				for (IValue val : (Iterable<IValue>) resultElem) {
					resultList.add(val);
				}
			}
			return resultList;
		} else if (env.getVariable(a.name()) != null) {
			resultElem = env.getVariable(a.name()).getValue();
			if (resultElem.getType().isList() || resultElem.getType().isSet()) {
				for (IValue val : (Iterable<IValue>) resultElem) {
					resultList.add(val);
				}
			}
			return resultList;
		}
		return list;
	}
	
	public SubstitutionVisitor(Environment env, Map<String, Result<IValue>> substitutionVariables) {
		this.env = env;
		this.substitutionVariables = substitutionVariables;
	}
}
