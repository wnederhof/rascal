package org.rascalmpl.interpreter.env;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.rascalmpl.interpreter.result.Result;

/**
 * This class is used for functions that are not allowed to alter variables from their parents.
 * 
 * With normal functions:
 * 
 * someVariable = tuple[int (), int ()] (int x) {
 *    return <int () {
 *                     y = y + 1; return y;
 *                   },
 *            int () {
 *                     return y;
 *                   }>;
 *  };
 *
 *  c = someVariable(5);
 *  c[0]();
 *  c[0]();
 *  c[1]();
 *  
 *  returns 7, which is not what we want.
 * 
 * @author wouter
 *
 */
public class StayInScopeEnvironment extends Environment {

	public StayInScopeEnvironment(Environment parent, Environment callerScope, ISourceLocation callerLocation, ISourceLocation loc, String name) {
		super(parent, callerScope, callerLocation, loc, name);
		if (parent.variableEnvironment != null)
		this.variableEnvironment = new HashMap<>(parent.getVariables());
	}

	@Override
	protected Map<String, Result<IValue>> getVariableDefiningEnvironment(String name) {
		return null;
	}
	
}
