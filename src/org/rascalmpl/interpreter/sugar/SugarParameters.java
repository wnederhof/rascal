package org.rascalmpl.interpreter.sugar;

import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.ValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.interpreter.env.Environment;

public class SugarParameters {
	private final static String RESUGAR_FUNCTION = "__resugarFunction";

	@SuppressWarnings("deprecation")
	private static IValue getParameter(String name, IValue value) {
		if (value.isAnnotatable()) {
			return value.asAnnotatable().getAnnotation(name);
		} else if (value.mayHaveKeywordParameters()) {
			return value.asWithKeywordParameters().setParameter(name, value);
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private static IValue setParameter(IValue toSet, String name, IValue value) {
		if (toSet.isAnnotatable()) {
			return toSet.asAnnotatable().setAnnotation(name, value);
		} else if (toSet.mayHaveKeywordParameters()) {
			return value.asWithKeywordParameters().setParameter(name, value);
		}
		throw new RuntimeException("Cannot set keyword parameter or annotation.");
	}

	private static void declareSugarParameter(String name, Environment env, Type onType, Type valueType) {
		if (env.getStore().hasKeywordParameters(onType)) {
			declareKeywordParameter(name, env, onType, valueType);
			return;
		}
		declareAnnotation(name, env, onType, valueType);
	}

	private static void declareAnnotation(String name, Environment env, Type onType, Type valueType) {
		env.getStore().declareAnnotation(onType, name, valueType);
	}

	private static void declareKeywordParameter(String name, Environment env, Type onType, Type valueType) {
		env.getStore().declareKeywordParameter(onType, name, valueType);
	}

	private static IValue attachSugarKeywordLayer(String keyword, IValue attachTo, IValue value, ValueFactory VF) {
		if (getParameter(keyword, attachTo) == null) {
			return setParameter(attachTo, keyword, value);
		}
		return setParameter(attachTo, keyword, VF.tuple(value, getParameter(keyword, attachTo)));
	}

	private static IValue stripSugarKeywordLayer(String keyword, IValue stripFrom) {
		if (getParameter(keyword, stripFrom) instanceof ITuple) {
			return setParameter(stripFrom, keyword, ((ITuple) getParameter(keyword, stripFrom)).get(1));
		}
		return stripFrom;
	}

	public static void declareSugarParameters(Environment env, Type onType, TypeFactory TF) {
		declareSugarParameter(RESUGAR_FUNCTION, env, onType, TF.valueType());
	}

	@SuppressWarnings("deprecation")
	public static boolean isResugarable(IValue v) {
		if (v.isAnnotatable() || v.mayHaveKeywordParameters()) {
			return getTopMostResugarFunction(v) != null;
		}
		return false;
	}

	public static IValue getTopMostResugarFunction(IValue value) {
		IValue parameter = getParameter(RESUGAR_FUNCTION, value);
		if (parameter != null) {
			if (parameter instanceof ITuple) {
				return ((ITuple) parameter).get(0);
			}
			return parameter;
		}
		return null;
	}

	public static IValue attachSugarKeywordsLayer(IValue attachTo, IValue resugarFunction, ValueFactory VF) {
		return attachSugarKeywordLayer(RESUGAR_FUNCTION, attachTo, resugarFunction, VF);
	}

	public static IValue stripSugarKeywordsLayer(IValue stripFrom) {
		return stripSugarKeywordLayer(RESUGAR_FUNCTION, stripFrom);
	}

}
