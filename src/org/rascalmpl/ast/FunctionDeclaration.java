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

public abstract class FunctionDeclaration extends AbstractAST {
  public FunctionDeclaration(ISourceLocation src, IConstructor node) {
    super(src /* we forget node on purpose */);
  }

  
  public boolean hasConditions() {
    return false;
  }

  public java.util.List<org.rascalmpl.ast.Expression> getConditions() {
    throw new UnsupportedOperationException();
  }
  public boolean hasExpression() {
    return false;
  }

  public org.rascalmpl.ast.Expression getExpression() {
    throw new UnsupportedOperationException();
  }
  public boolean hasPatternCore() {
    return false;
  }

  public org.rascalmpl.ast.Expression getPatternCore() {
    throw new UnsupportedOperationException();
  }
  public boolean hasPatternSurface() {
    return false;
  }

  public org.rascalmpl.ast.Expression getPatternSurface() {
    throw new UnsupportedOperationException();
  }
  public boolean hasBody() {
    return false;
  }

  public org.rascalmpl.ast.FunctionBody getBody() {
    throw new UnsupportedOperationException();
  }
  public boolean hasName() {
    return false;
  }

  public org.rascalmpl.ast.Name getName() {
    throw new UnsupportedOperationException();
  }
  public boolean hasOptionalFallbackSugar() {
    return false;
  }

  public org.rascalmpl.ast.OptionalFallbackSugar getOptionalFallbackSugar() {
    throw new UnsupportedOperationException();
  }
  public boolean hasOptionalSugarType() {
    return false;
  }

  public org.rascalmpl.ast.OptionalSugarType getOptionalSugarType() {
    throw new UnsupportedOperationException();
  }
  public boolean hasOptionalUsing() {
    return false;
  }

  public org.rascalmpl.ast.OptionalUsingDesugaring getOptionalUsing() {
    throw new UnsupportedOperationException();
  }
  public boolean hasOptionalWhen() {
    return false;
  }

  public org.rascalmpl.ast.OptionalWhen getOptionalWhen() {
    throw new UnsupportedOperationException();
  }
  public boolean hasSignature() {
    return false;
  }

  public org.rascalmpl.ast.Signature getSignature() {
    throw new UnsupportedOperationException();
  }
  public boolean hasTags() {
    return false;
  }

  public org.rascalmpl.ast.Tags getTags() {
    throw new UnsupportedOperationException();
  }
  public boolean hasTypeCore() {
    return false;
  }

  public org.rascalmpl.ast.Type getTypeCore() {
    throw new UnsupportedOperationException();
  }
  public boolean hasVisibility() {
    return false;
  }

  public org.rascalmpl.ast.Visibility getVisibility() {
    throw new UnsupportedOperationException();
  }

  

  
  public boolean isAbstract() {
    return false;
  }

  static public class Abstract extends FunctionDeclaration {
    // Production: sig("Abstract",[arg("org.rascalmpl.ast.Tags","tags"),arg("org.rascalmpl.ast.Visibility","visibility"),arg("org.rascalmpl.ast.Signature","signature")],breakable=false)
  
    
    private final org.rascalmpl.ast.Tags tags;
    private final org.rascalmpl.ast.Visibility visibility;
    private final org.rascalmpl.ast.Signature signature;
  
    public Abstract(ISourceLocation src, IConstructor node , org.rascalmpl.ast.Tags tags,  org.rascalmpl.ast.Visibility visibility,  org.rascalmpl.ast.Signature signature) {
      super(src, node);
      
      this.tags = tags;
      this.visibility = visibility;
      this.signature = signature;
    }
  
    @Override
    public boolean isAbstract() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitFunctionDeclarationAbstract(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
      $l = tags.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        tags.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = visibility.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        visibility.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = signature.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        signature.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Abstract)) {
        return false;
      }        
      Abstract tmp = (Abstract) o;
      return true && tmp.tags.equals(this.tags) && tmp.visibility.equals(this.visibility) && tmp.signature.equals(this.signature) ; 
    }
   
    @Override
    public int hashCode() {
      return 71 + 503 * tags.hashCode() + 31 * visibility.hashCode() + 181 * signature.hashCode() ; 
    } 
  
    
    @Override
    public org.rascalmpl.ast.Tags getTags() {
      return this.tags;
    }
  
    @Override
    public boolean hasTags() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Visibility getVisibility() {
      return this.visibility;
    }
  
    @Override
    public boolean hasVisibility() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Signature getSignature() {
      return this.signature;
    }
  
    @Override
    public boolean hasSignature() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(tags), clone(visibility), clone(signature));
    }
            
  }
  public boolean isConditional() {
    return false;
  }

  static public class Conditional extends FunctionDeclaration {
    // Production: sig("Conditional",[arg("org.rascalmpl.ast.Tags","tags"),arg("org.rascalmpl.ast.Visibility","visibility"),arg("org.rascalmpl.ast.Signature","signature"),arg("org.rascalmpl.ast.Expression","expression"),arg("java.util.List\<org.rascalmpl.ast.Expression\>","conditions")],breakable=false)
  
    
    private final org.rascalmpl.ast.Tags tags;
    private final org.rascalmpl.ast.Visibility visibility;
    private final org.rascalmpl.ast.Signature signature;
    private final org.rascalmpl.ast.Expression expression;
    private final java.util.List<org.rascalmpl.ast.Expression> conditions;
  
    public Conditional(ISourceLocation src, IConstructor node , org.rascalmpl.ast.Tags tags,  org.rascalmpl.ast.Visibility visibility,  org.rascalmpl.ast.Signature signature,  org.rascalmpl.ast.Expression expression,  java.util.List<org.rascalmpl.ast.Expression> conditions) {
      super(src, node);
      
      this.tags = tags;
      this.visibility = visibility;
      this.signature = signature;
      this.expression = expression;
      this.conditions = conditions;
    }
  
    @Override
    public boolean isConditional() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitFunctionDeclarationConditional(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
      $l = tags.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        tags.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = visibility.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        visibility.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = signature.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        signature.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = expression.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        expression.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      for (AbstractAST $elem : conditions) {
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
      if (!(o instanceof Conditional)) {
        return false;
      }        
      Conditional tmp = (Conditional) o;
      return true && tmp.tags.equals(this.tags) && tmp.visibility.equals(this.visibility) && tmp.signature.equals(this.signature) && tmp.expression.equals(this.expression) && tmp.conditions.equals(this.conditions) ; 
    }
   
    @Override
    public int hashCode() {
      return 313 + 463 * tags.hashCode() + 257 * visibility.hashCode() + 211 * signature.hashCode() + 179 * expression.hashCode() + 457 * conditions.hashCode() ; 
    } 
  
    
    @Override
    public org.rascalmpl.ast.Tags getTags() {
      return this.tags;
    }
  
    @Override
    public boolean hasTags() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Visibility getVisibility() {
      return this.visibility;
    }
  
    @Override
    public boolean hasVisibility() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Signature getSignature() {
      return this.signature;
    }
  
    @Override
    public boolean hasSignature() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getExpression() {
      return this.expression;
    }
  
    @Override
    public boolean hasExpression() {
      return true;
    }
    @Override
    public java.util.List<org.rascalmpl.ast.Expression> getConditions() {
      return this.conditions;
    }
  
    @Override
    public boolean hasConditions() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(tags), clone(visibility), clone(signature), clone(expression), clone(conditions));
    }
            
  }
  public boolean isDefault() {
    return false;
  }

  static public class Default extends FunctionDeclaration {
    // Production: sig("Default",[arg("org.rascalmpl.ast.Tags","tags"),arg("org.rascalmpl.ast.Visibility","visibility"),arg("org.rascalmpl.ast.Signature","signature"),arg("org.rascalmpl.ast.FunctionBody","body")],breakable=false)
  
    
    private final org.rascalmpl.ast.Tags tags;
    private final org.rascalmpl.ast.Visibility visibility;
    private final org.rascalmpl.ast.Signature signature;
    private final org.rascalmpl.ast.FunctionBody body;
  
    public Default(ISourceLocation src, IConstructor node , org.rascalmpl.ast.Tags tags,  org.rascalmpl.ast.Visibility visibility,  org.rascalmpl.ast.Signature signature,  org.rascalmpl.ast.FunctionBody body) {
      super(src, node);
      
      this.tags = tags;
      this.visibility = visibility;
      this.signature = signature;
      this.body = body;
    }
  
    @Override
    public boolean isDefault() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitFunctionDeclarationDefault(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
      $l = tags.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        tags.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = visibility.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        visibility.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = signature.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        signature.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = body.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        body.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Default)) {
        return false;
      }        
      Default tmp = (Default) o;
      return true && tmp.tags.equals(this.tags) && tmp.visibility.equals(this.visibility) && tmp.signature.equals(this.signature) && tmp.body.equals(this.body) ; 
    }
   
    @Override
    public int hashCode() {
      return 839 + 2 * tags.hashCode() + 461 * visibility.hashCode() + 877 * signature.hashCode() + 599 * body.hashCode() ; 
    } 
  
    
    @Override
    public org.rascalmpl.ast.Tags getTags() {
      return this.tags;
    }
  
    @Override
    public boolean hasTags() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Visibility getVisibility() {
      return this.visibility;
    }
  
    @Override
    public boolean hasVisibility() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Signature getSignature() {
      return this.signature;
    }
  
    @Override
    public boolean hasSignature() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.FunctionBody getBody() {
      return this.body;
    }
  
    @Override
    public boolean hasBody() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(tags), clone(visibility), clone(signature), clone(body));
    }
            
  }
  public boolean isExpression() {
    return false;
  }

  static public class Expression extends FunctionDeclaration {
    // Production: sig("Expression",[arg("org.rascalmpl.ast.Tags","tags"),arg("org.rascalmpl.ast.Visibility","visibility"),arg("org.rascalmpl.ast.Signature","signature"),arg("org.rascalmpl.ast.Expression","expression")],breakable=false)
  
    
    private final org.rascalmpl.ast.Tags tags;
    private final org.rascalmpl.ast.Visibility visibility;
    private final org.rascalmpl.ast.Signature signature;
    private final org.rascalmpl.ast.Expression expression;
  
    public Expression(ISourceLocation src, IConstructor node , org.rascalmpl.ast.Tags tags,  org.rascalmpl.ast.Visibility visibility,  org.rascalmpl.ast.Signature signature,  org.rascalmpl.ast.Expression expression) {
      super(src, node);
      
      this.tags = tags;
      this.visibility = visibility;
      this.signature = signature;
      this.expression = expression;
    }
  
    @Override
    public boolean isExpression() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitFunctionDeclarationExpression(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
      $l = tags.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        tags.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = visibility.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        visibility.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = signature.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        signature.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = expression.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        expression.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Expression)) {
        return false;
      }        
      Expression tmp = (Expression) o;
      return true && tmp.tags.equals(this.tags) && tmp.visibility.equals(this.visibility) && tmp.signature.equals(this.signature) && tmp.expression.equals(this.expression) ; 
    }
   
    @Override
    public int hashCode() {
      return 281 + 43 * tags.hashCode() + 103 * visibility.hashCode() + 673 * signature.hashCode() + 509 * expression.hashCode() ; 
    } 
  
    
    @Override
    public org.rascalmpl.ast.Tags getTags() {
      return this.tags;
    }
  
    @Override
    public boolean hasTags() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Visibility getVisibility() {
      return this.visibility;
    }
  
    @Override
    public boolean hasVisibility() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Signature getSignature() {
      return this.signature;
    }
  
    @Override
    public boolean hasSignature() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getExpression() {
      return this.expression;
    }
  
    @Override
    public boolean hasExpression() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(tags), clone(visibility), clone(signature), clone(expression));
    }
            
  }
  public boolean isSugarCICDR() {
    return false;
  }

  static public class SugarCICDR extends FunctionDeclaration {
    // Production: sig("SugarCICDR",[arg("org.rascalmpl.ast.Tags","tags"),arg("org.rascalmpl.ast.Visibility","visibility"),arg("org.rascalmpl.ast.Type","typeCore"),arg("org.rascalmpl.ast.Name","name"),arg("org.rascalmpl.ast.Expression","patternSurface"),arg("org.rascalmpl.ast.OptionalUsingDesugaring","optionalUsing"),arg("org.rascalmpl.ast.OptionalFallbackSugar","optionalFallbackSugar"),arg("org.rascalmpl.ast.OptionalSugarType","optionalSugarType"),arg("org.rascalmpl.ast.Expression","patternCore"),arg("org.rascalmpl.ast.OptionalWhen","optionalWhen")],breakable=false)
  
    
    private final org.rascalmpl.ast.Tags tags;
    private final org.rascalmpl.ast.Visibility visibility;
    private final org.rascalmpl.ast.Type typeCore;
    private final org.rascalmpl.ast.Name name;
    private final org.rascalmpl.ast.Expression patternSurface;
    private final org.rascalmpl.ast.OptionalUsingDesugaring optionalUsing;
    private final org.rascalmpl.ast.OptionalFallbackSugar optionalFallbackSugar;
    private final org.rascalmpl.ast.OptionalSugarType optionalSugarType;
    private final org.rascalmpl.ast.Expression patternCore;
    private final org.rascalmpl.ast.OptionalWhen optionalWhen;
  
    public SugarCICDR(ISourceLocation src, IConstructor node , org.rascalmpl.ast.Tags tags,  org.rascalmpl.ast.Visibility visibility,  org.rascalmpl.ast.Type typeCore,  org.rascalmpl.ast.Name name,  org.rascalmpl.ast.Expression patternSurface,  org.rascalmpl.ast.OptionalUsingDesugaring optionalUsing,  org.rascalmpl.ast.OptionalFallbackSugar optionalFallbackSugar,  org.rascalmpl.ast.OptionalSugarType optionalSugarType,  org.rascalmpl.ast.Expression patternCore,  org.rascalmpl.ast.OptionalWhen optionalWhen) {
      super(src, node);
      
      this.tags = tags;
      this.visibility = visibility;
      this.typeCore = typeCore;
      this.name = name;
      this.patternSurface = patternSurface;
      this.optionalUsing = optionalUsing;
      this.optionalFallbackSugar = optionalFallbackSugar;
      this.optionalSugarType = optionalSugarType;
      this.patternCore = patternCore;
      this.optionalWhen = optionalWhen;
    }
  
    @Override
    public boolean isSugarCICDR() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitFunctionDeclarationSugarCICDR(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
      $l = tags.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        tags.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = visibility.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        visibility.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = typeCore.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        typeCore.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = name.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        name.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = patternSurface.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        patternSurface.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = optionalUsing.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        optionalUsing.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = optionalFallbackSugar.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        optionalFallbackSugar.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = optionalSugarType.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        optionalSugarType.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = patternCore.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        patternCore.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = optionalWhen.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        optionalWhen.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof SugarCICDR)) {
        return false;
      }        
      SugarCICDR tmp = (SugarCICDR) o;
      return true && tmp.tags.equals(this.tags) && tmp.visibility.equals(this.visibility) && tmp.typeCore.equals(this.typeCore) && tmp.name.equals(this.name) && tmp.patternSurface.equals(this.patternSurface) && tmp.optionalUsing.equals(this.optionalUsing) && tmp.optionalFallbackSugar.equals(this.optionalFallbackSugar) && tmp.optionalSugarType.equals(this.optionalSugarType) && tmp.patternCore.equals(this.patternCore) && tmp.optionalWhen.equals(this.optionalWhen) ; 
    }
   
    @Override
    public int hashCode() {
      return 61 + 173 * tags.hashCode() + 761 * visibility.hashCode() + 409 * typeCore.hashCode() + 997 * name.hashCode() + 367 * patternSurface.hashCode() + 401 * optionalUsing.hashCode() + 139 * optionalFallbackSugar.hashCode() + 937 * optionalSugarType.hashCode() + 521 * patternCore.hashCode() + 719 * optionalWhen.hashCode() ; 
    } 
  
    
    @Override
    public org.rascalmpl.ast.Tags getTags() {
      return this.tags;
    }
  
    @Override
    public boolean hasTags() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Visibility getVisibility() {
      return this.visibility;
    }
  
    @Override
    public boolean hasVisibility() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Type getTypeCore() {
      return this.typeCore;
    }
  
    @Override
    public boolean hasTypeCore() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Name getName() {
      return this.name;
    }
  
    @Override
    public boolean hasName() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getPatternSurface() {
      return this.patternSurface;
    }
  
    @Override
    public boolean hasPatternSurface() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.OptionalUsingDesugaring getOptionalUsing() {
      return this.optionalUsing;
    }
  
    @Override
    public boolean hasOptionalUsing() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.OptionalFallbackSugar getOptionalFallbackSugar() {
      return this.optionalFallbackSugar;
    }
  
    @Override
    public boolean hasOptionalFallbackSugar() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.OptionalSugarType getOptionalSugarType() {
      return this.optionalSugarType;
    }
  
    @Override
    public boolean hasOptionalSugarType() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getPatternCore() {
      return this.patternCore;
    }
  
    @Override
    public boolean hasPatternCore() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.OptionalWhen getOptionalWhen() {
      return this.optionalWhen;
    }
  
    @Override
    public boolean hasOptionalWhen() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(tags), clone(visibility), clone(typeCore), clone(name), clone(patternSurface), clone(optionalUsing), clone(optionalFallbackSugar), clone(optionalSugarType), clone(patternCore), clone(optionalWhen));
    }
            
  }
  public boolean isSugarConfection() {
    return false;
  }

  static public class SugarConfection extends FunctionDeclaration {
    // Production: sig("SugarConfection",[arg("org.rascalmpl.ast.Tags","tags"),arg("org.rascalmpl.ast.Visibility","visibility"),arg("org.rascalmpl.ast.Type","typeCore"),arg("org.rascalmpl.ast.Name","name"),arg("org.rascalmpl.ast.Expression","patternSurface"),arg("org.rascalmpl.ast.OptionalFallbackSugar","optionalFallbackSugar"),arg("org.rascalmpl.ast.OptionalSugarType","optionalSugarType"),arg("org.rascalmpl.ast.Expression","patternCore"),arg("org.rascalmpl.ast.OptionalWhen","optionalWhen")],breakable=false)
  
    
    private final org.rascalmpl.ast.Tags tags;
    private final org.rascalmpl.ast.Visibility visibility;
    private final org.rascalmpl.ast.Type typeCore;
    private final org.rascalmpl.ast.Name name;
    private final org.rascalmpl.ast.Expression patternSurface;
    private final org.rascalmpl.ast.OptionalFallbackSugar optionalFallbackSugar;
    private final org.rascalmpl.ast.OptionalSugarType optionalSugarType;
    private final org.rascalmpl.ast.Expression patternCore;
    private final org.rascalmpl.ast.OptionalWhen optionalWhen;
  
    public SugarConfection(ISourceLocation src, IConstructor node , org.rascalmpl.ast.Tags tags,  org.rascalmpl.ast.Visibility visibility,  org.rascalmpl.ast.Type typeCore,  org.rascalmpl.ast.Name name,  org.rascalmpl.ast.Expression patternSurface,  org.rascalmpl.ast.OptionalFallbackSugar optionalFallbackSugar,  org.rascalmpl.ast.OptionalSugarType optionalSugarType,  org.rascalmpl.ast.Expression patternCore,  org.rascalmpl.ast.OptionalWhen optionalWhen) {
      super(src, node);
      
      this.tags = tags;
      this.visibility = visibility;
      this.typeCore = typeCore;
      this.name = name;
      this.patternSurface = patternSurface;
      this.optionalFallbackSugar = optionalFallbackSugar;
      this.optionalSugarType = optionalSugarType;
      this.patternCore = patternCore;
      this.optionalWhen = optionalWhen;
    }
  
    @Override
    public boolean isSugarConfection() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitFunctionDeclarationSugarConfection(this);
    }
  
    @Override
    protected void addForLineNumber(int $line, java.util.List<AbstractAST> $result) {
      if (getLocation().getBeginLine() == $line) {
        $result.add(this);
      }
      ISourceLocation $l;
      
      $l = tags.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        tags.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = visibility.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        visibility.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = typeCore.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        typeCore.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = name.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        name.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = patternSurface.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        patternSurface.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = optionalFallbackSugar.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        optionalFallbackSugar.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = optionalSugarType.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        optionalSugarType.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = patternCore.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        patternCore.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = optionalWhen.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        optionalWhen.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof SugarConfection)) {
        return false;
      }        
      SugarConfection tmp = (SugarConfection) o;
      return true && tmp.tags.equals(this.tags) && tmp.visibility.equals(this.visibility) && tmp.typeCore.equals(this.typeCore) && tmp.name.equals(this.name) && tmp.patternSurface.equals(this.patternSurface) && tmp.optionalFallbackSugar.equals(this.optionalFallbackSugar) && tmp.optionalSugarType.equals(this.optionalSugarType) && tmp.patternCore.equals(this.patternCore) && tmp.optionalWhen.equals(this.optionalWhen) ; 
    }
   
    @Override
    public int hashCode() {
      return 773 + 277 * tags.hashCode() + 239 * visibility.hashCode() + 151 * typeCore.hashCode() + 397 * name.hashCode() + 137 * patternSurface.hashCode() + 811 * optionalFallbackSugar.hashCode() + 709 * optionalSugarType.hashCode() + 167 * patternCore.hashCode() + 53 * optionalWhen.hashCode() ; 
    } 
  
    
    @Override
    public org.rascalmpl.ast.Tags getTags() {
      return this.tags;
    }
  
    @Override
    public boolean hasTags() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Visibility getVisibility() {
      return this.visibility;
    }
  
    @Override
    public boolean hasVisibility() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Type getTypeCore() {
      return this.typeCore;
    }
  
    @Override
    public boolean hasTypeCore() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Name getName() {
      return this.name;
    }
  
    @Override
    public boolean hasName() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getPatternSurface() {
      return this.patternSurface;
    }
  
    @Override
    public boolean hasPatternSurface() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.OptionalFallbackSugar getOptionalFallbackSugar() {
      return this.optionalFallbackSugar;
    }
  
    @Override
    public boolean hasOptionalFallbackSugar() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.OptionalSugarType getOptionalSugarType() {
      return this.optionalSugarType;
    }
  
    @Override
    public boolean hasOptionalSugarType() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getPatternCore() {
      return this.patternCore;
    }
  
    @Override
    public boolean hasPatternCore() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.OptionalWhen getOptionalWhen() {
      return this.optionalWhen;
    }
  
    @Override
    public boolean hasOptionalWhen() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(tags), clone(visibility), clone(typeCore), clone(name), clone(patternSurface), clone(optionalFallbackSugar), clone(optionalSugarType), clone(patternCore), clone(optionalWhen));
    }
            
  }
}