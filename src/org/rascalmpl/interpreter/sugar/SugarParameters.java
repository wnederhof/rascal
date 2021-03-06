package org.rascalmpl.interpreter.sugar;

import java.util.Random;
import java.util.UUID;

import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.ValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.rascalmpl.interpreter.env.Environment;

public class SugarParameters {
	private final static ValueFactory VF = ValueFactory.getInstance();
	private final static String RESUGAR_FUNCTION = "__resugarFunction",
								SUGAR_NODEID = "__nodeId",
								NOTIFY_TRACER_FN = "__tracerFn";

	public static void declareSugarParameters(Environment env, Type onType, TypeFactory TF) {
		declareSugarParameter(RESUGAR_FUNCTION, env, onType, TF.valueType());
		declareSugarParameter(SUGAR_NODEID, env, onType, TF.valueType());
		declareSugarParameter(NOTIFY_TRACER_FN, env, onType, TF.valueType());
	}

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

	private static IValue attachSugarKeywordLayer(String keyword, IValue attachTo, IValue value) {
		if (getParameter(keyword, attachTo) == null) {
			return setParameter(attachTo, keyword, value);
		}
		return setParameter(attachTo, keyword, VF.tuple(value, getParameter(keyword, attachTo)));
	}
	
	@SuppressWarnings("deprecation")
	private static IValue unsetParameter(IValue value, String name) {
		if (value.isAnnotatable()) {
			return value.asAnnotatable().removeAnnotation(name);
		} else if (value.mayHaveKeywordParameters()) {
			return value.asWithKeywordParameters().unsetParameter(name);
		}
		throw new RuntimeException("Cannot unset keyword parameter or annotation.");
	}

	private static IValue peelSugarKeywordLayer(String keyword, IValue stripFrom) {
		if (getParameter(keyword, stripFrom) instanceof ITuple) {
			return setParameter(stripFrom, keyword, ((ITuple) getParameter(keyword, stripFrom)).get(1));
		}
		return unsetParameter(stripFrom, keyword);
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

	public static IValue attachSugarKeywordsLayer(IValue attachTo, IValue resugarFunction) {
		return attachSugarKeywordLayer(RESUGAR_FUNCTION, attachTo, resugarFunction);
	}

	public static IValue peelSugarKeywordsLayer(IValue stripFrom) {
		return peelSugarKeywordLayer(RESUGAR_FUNCTION, stripFrom);
	}

	public static int sugarKeywordsThickness(IValue value) {
		IValue par = getParameter(RESUGAR_FUNCTION, value);
		if (par instanceof ITuple) {
			return sugarKeywordsThickness(((ITuple) par).get(1)) + 1;
		} else if (par == null) {
			return 0;
		}
		return 1;
	}

	public static boolean hasMultipleLayers(IValue termToDesugar) {
		return termToDesugar != null && getParameter(RESUGAR_FUNCTION, termToDesugar) instanceof ITuple;
	}
		
	public static IValue tag(IValue v) {
		String nodeIdStr;
		nodeIdStr = "" + new Random().nextInt();
		try {
			return attachSugarKeywordLayer(SUGAR_NODEID, v, VF.string(nodeIdStr));
		} catch(RuntimeException e) {
			// Just ignore the tagging.
			return v;
		}
	}

	public static IValue tagValue(IValue l) {
		IValue r = getParameter(SUGAR_NODEID, l);
		return r;
	}

	public static IValue getTopMostTracer(IValue value) {
		IValue parameter = getParameter(NOTIFY_TRACER_FN, value);
		if (parameter != null) {
			if (parameter instanceof ITuple) {
				return ((ITuple) parameter).get(0);
			}
			return parameter;
		}
		return null;
	}

}
