/*******************************************************************************
 * Copyright (c) 2009-2015 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.rascalmpl.ast;


import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISourceLocation;

public abstract class OptionalUsingDesugaring extends AbstractAST {
  public OptionalUsingDesugaring(ISourceLocation src, IConstructor node) {
    super(src /* we forget node on purpose */);
  }

  
  public boolean hasSugarFunctionMapping() {
    return false;
  }

  public java.util.List<org.rascalmpl.ast.SugarFunctionMapping> getSugarFunctionMapping() {
    throw new UnsupportedOperationException();
  }

  

  
  public boolean isDefault() {
    return false;
  }

  static public class Default extends OptionalUsingDesugaring {
    // Production: sig("Default",[arg("java.util.List\<org.rascalmpl.ast.SugarFunctionMapping\>","sugarFunctionMapping")],breakable=false)
  
    
    private final java.util.List<org.rascalmpl.ast.SugarFunctionMapping> sugarFunctionMapping;
  
    public Default(ISourceLocation src, IConstructor node , java.util.List<org.rascalmpl.ast.SugarFunctionMapping> sugarFunctionMapping) {
      super(src, node);
      
      this.sugarFunctionMapping = sugarFunctionMapping;
    }
  
    @Override
    public boolean isDefault() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitOptionalUsingDesugaringDefault(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
      for (AbstractAST $elem : sugarFunctionMapping) {
        $l = $elem.getLocation();
        if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
          $elem.addForLineNumber($line, $result);
        }
        if ($l.getBeginLine() > $line) {
          return;
        }
  
      }
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Default)) {
        return false;
      }        
      Default tmp = (Default) o;
      return true && tmp.sugarFunctionMapping.equals(this.sugarFunctionMapping) ; 
    }
   
    @Override
    public int hashCode() {
      return 5 + 19 * sugarFunctionMapping.hashCode() ; 
    } 
  
    
    @Override
    public java.util.List<org.rascalmpl.ast.SugarFunctionMapping> getSugarFunctionMapping() {
      return this.sugarFunctionMapping;
    }
  
    @Override
    public boolean hasSugarFunctionMapping() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(sugarFunctionMapping));
    }
            
  }
  public boolean isNone() {
    return false;
  }

  static public class None extends OptionalUsingDesugaring {
    // Production: sig("None",[],breakable=false)
  
    
  
    public None(ISourceLocation src, IConstructor node ) {
      super(src, node);
      
    }
  
    @Override
    public boolean isNone() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitOptionalUsingDesugaringNone(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof None)) {
        return false;
      }        
      None tmp = (None) o;
      return true ; 
    }
   
    @Override
    public int hashCode() {
      return 19 ; 
    } 
  
    	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null );
    }
            
  }
}