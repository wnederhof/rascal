package org.rascalmpl.interpreter.env;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.QualifiedName;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.utils.Names;

/**
 * This environment emulates an environment with no variables, so the pattern matcher will always bind to a variable.
 * 
 * @author wouter
 * // (NOTE TO SELF: SimpleVariable doesnt check for functions...)
 */
public class EmptyVariablesEnvironment extends Environment {

	public EmptyVariablesEnvironment(Environment old) {
		super(old);
	}
	
	@Override
	public Result<IValue> getVariable(String name) {
		// TODO: Not very efficient...
		if (super.getSimpleVariable(name) == null) {
			return super.getVariable(name);
		}
		System.out.println("Debug (getSimpleVariable): Trying to fetch: " + name);
		return null;
	}
	
	@Override
	public Map<String, Result<IValue>> getVariables() {
		return new HashMap<String, Result<IValue>>();
	}
	
	@Override
	public Result<IValue> getSimpleVariable(String name) {
		System.out.println("Debug (getSimpleVariable): Trying to fetch: " + name);
		return null;
	}

}
