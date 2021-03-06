/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
 *   * Wietse Venema - wietsevenema@gmail.com - CWI
 *******************************************************************************/
package org.rascalmpl.library.experiments.Compiler.RVM.Interpreter;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.ITestResultListener;

public class TestExecutor {

	private final Evaluator eval;
	private final ITestResultListener testResultListener;

	public TestExecutor(Evaluator eval, ITestResultListener testResultListener){
		super();

		this.eval = eval;
		this.testResultListener = testResultListener;
		// Make listener known to compiler's run-time system
		Execute.setTestResultListener(testResultListener);
	}

	public void test(String moduleName, int nTests) {
		testResultListener.start(nTests);
		try {
			ISourceLocation src = eval.getRascalResolver().resolveModule(moduleName);
			System.err.println("TestExecutor.test: testing " + moduleName + ", " + nTests + " tests");
			eval.call("executeTests", src);
			//System.err.println("TestExecutor.test: testing " + moduleName + " ... done");
		} 
		catch (Exception e) {
			System.err.println("TestExecutor.test: " + moduleName + " unexpected exception: " + e.getMessage());
			throw e;
		}
		finally {
			testResultListener.done();
		}
	}

}
