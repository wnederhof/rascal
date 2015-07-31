module SugarAnnotations

public anno list[value] node @ unusedVariables;
public anno value node @ unexpandFn;

bool isDesugarable(t) {
	return isNode(t) || isNonTerminalType(t);
}