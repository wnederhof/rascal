package org.rascalmpl.interpreter.env;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.interpreter.result.Result;

/**
 * This environment emulates an environment with no variables, so the pattern matcher will always bind to a variable.
 * 
 * @author wouter
 * // (NOTE TO SELF: SimpleVariable doesnt check for functions...)
 */
public class EmptyVariablesEnvironment extends Environment {

	public EmptyVariablesEnvironment(Environment old) {
		super(old);
		variableEnvironment = new HashMap<>();
	}

	@Override
	public Result<IValue> getSimpleVariable(String name) {
		Result<IValue> t = null;
		
		if (variableEnvironment != null) { // TODO: huh?!?
			t = variableEnvironment.get(name);
		}
		
		return t;
	}
	
	@Override
	public Map<String, Result<IValue>> getVariables() {
		return variableEnvironment;
	}
	
	@Override
	public void storeLocalVariable(String name, Result<IValue> value) {
		System.out.println("Stored variable: " + name + ", value: " + value);
		variableEnvironment.put(name, value);
	}
	
	@Override
	public void storeVariable(String name, Result<IValue> value) {
		System.out.println("Stored variable: " + name + ", value: " + value);
		variableEnvironment.put(name, value);
	}

}
