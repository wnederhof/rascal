package org.rascalmpl.library.experiments.Compiler.RVM.Interpreter.traverse;

import java.util.HashSet;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.interpreter.TypeReifier;
import org.rascalmpl.library.experiments.Compiler.RVM.Interpreter.RascalPrimitive;
import org.rascalmpl.values.uptr.ITree;

/**
 * Create a descendant descriptor given
 * 1: symbolset (converted from ISet of values to HashSet of Types, symbols and Productions)
 * 2: concreteMatch, indicates a concrete or abstract match
 * 
 * [ IString id, ISet symbolset, IBool concreteMatch] => descendant_descriptor
 */

public class DescendantDescriptor {
	private final HashSet<Object> mSymbolSet;
	private final boolean concreteMatch;
	private final boolean containsNodeOrValueType;
	//private int counter = 0;
	
	public DescendantDescriptor(IValueFactory vf, ISet symbolset, IMap definitions, IBool concreteMatch ){
		mSymbolSet = new HashSet<Object>(symbolset.size());
		this.concreteMatch = concreteMatch.getValue();
		TypeReifier reifier = new TypeReifier(vf);
		for(IValue v : symbolset){
			try {
				IConstructor cons = (IConstructor) v;
				if(cons.getName().equals("prod")){
					mSymbolSet.add(cons);							// Add the production itself as SYMBOL to the set
				} else if(cons.getName().equals("regular")){
					mSymbolSet.add(cons);							// Add as SYMBOL to the set
				} else {
					Type tp = reifier.symbolToType(cons, definitions);
					mSymbolSet.add(tp);							// Otherwise add as TYPE to the set
				}
			} catch (Throwable e) {
				System.err.println("Problem with " + v + ", " + e);
			}
		}
		containsNodeOrValueType = mSymbolSet.contains(RascalPrimitive.nodeType) || mSymbolSet.contains(RascalPrimitive.valueType);
	}
	
	public boolean isConcreteMatch(){
		return concreteMatch;
	}
	
	public boolean isAllwaysTrue(){
		return containsNodeOrValueType;
	}
	
	public IBool shouldDescentInAbstractValue(IValue subject) {
		assert !concreteMatch : "shouldDescentInAbstractValue: abstract traversal required";
		//System.out.println("shouldDescentInAbstractValue: " + ++counter + ", " + subject.toString());
		if (containsNodeOrValueType) {
			return RascalPrimitive.Rascal_TRUE;
		}
		Type type = subject instanceof IConstructor 
				    ? ((IConstructor) subject).getConstructorType() 
				    : subject.getType();
		return mSymbolSet.contains(type) ? RascalPrimitive.Rascal_TRUE : RascalPrimitive.Rascal_FALSE;
	}
	
	public IBool shouldDescentInConcreteValue(ITree subject) {
		assert concreteMatch : "shouldDescentInConcreteValue: concrete traversal required";
		if (subject.isAppl()) {
			IConstructor prod = (IConstructor) subject.getProduction();
			return mSymbolSet.contains(prod) ? RascalPrimitive.Rascal_TRUE : RascalPrimitive.Rascal_FALSE;
		}
		if (subject.isAmb()) {
			return RascalPrimitive.Rascal_TRUE;
		}
		return RascalPrimitive.Rascal_FALSE;
	}
	
//	public IBool shouldDescentInType(Type type) {
//		assert !concreteMatch : "shouldDescentInType: abstract traversal required";
//		if (containsNodeOrValueType) {
//			return RascalPrimitive.Rascal_TRUE;
//		}
//		return mSymbolSet.contains(type) ? RascalPrimitive.Rascal_TRUE : RascalPrimitive.Rascal_FALSE;
//	}
	
}
