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
  public boolean hasPatternLhs() {
    return false;
  }

  public org.rascalmpl.ast.Expression getPatternLhs() {
    throw new UnsupportedOperationException();
  }
  public boolean hasPatternRhs() {
    return false;
  }

  public org.rascalmpl.ast.Expression getPatternRhs() {
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
  public boolean hasTypeLhs() {
    return false;
  }

  public org.rascalmpl.ast.Type getTypeLhs() {
    throw new UnsupportedOperationException();
  }
  public boolean hasTypeRhs() {
    return false;
  }

  public org.rascalmpl.ast.Type getTypeRhs() {
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
      return 571 + 211 * tags.hashCode() + 727 * visibility.hashCode() + 251 * signature.hashCode() ; 
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
      return 131 + 811 * tags.hashCode() + 199 * visibility.hashCode() + 5 * signature.hashCode() + 157 * expression.hashCode() + 389 * conditions.hashCode() ; 
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
      return 163 + 163 * tags.hashCode() + 71 * visibility.hashCode() + 503 * signature.hashCode() + 31 * body.hashCode() ; 
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
      return 181 + 313 * tags.hashCode() + 463 * visibility.hashCode() + 257 * signature.hashCode() + 211 * expression.hashCode() ; 
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
  public boolean isSugar() {
    return false;
  }

  static public class Sugar extends FunctionDeclaration {
    // Production: sig("Sugar",[arg("org.rascalmpl.ast.Tags","tags"),arg("org.rascalmpl.ast.Visibility","visibility"),arg("org.rascalmpl.ast.Name","name"),arg("org.rascalmpl.ast.Type","typeLhs"),arg("org.rascalmpl.ast.Expression","patternLhs"),arg("org.rascalmpl.ast.Type","typeRhs"),arg("org.rascalmpl.ast.Expression","patternRhs")],breakable=false)
  
    
    private final org.rascalmpl.ast.Tags tags;
    private final org.rascalmpl.ast.Visibility visibility;
    private final org.rascalmpl.ast.Name name;
    private final org.rascalmpl.ast.Type typeLhs;
    private final org.rascalmpl.ast.Expression patternLhs;
    private final org.rascalmpl.ast.Type typeRhs;
    private final org.rascalmpl.ast.Expression patternRhs;
  
    public Sugar(ISourceLocation src, IConstructor node , org.rascalmpl.ast.Tags tags,  org.rascalmpl.ast.Visibility visibility,  org.rascalmpl.ast.Name name,  org.rascalmpl.ast.Type typeLhs,  org.rascalmpl.ast.Expression patternLhs,  org.rascalmpl.ast.Type typeRhs,  org.rascalmpl.ast.Expression patternRhs) {
      super(src, node);
      
      this.tags = tags;
      this.visibility = visibility;
      this.name = name;
      this.typeLhs = typeLhs;
      this.patternLhs = patternLhs;
      this.typeRhs = typeRhs;
      this.patternRhs = patternRhs;
    }
  
    @Override
    public boolean isSugar() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitFunctionDeclarationSugar(this);
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
      
      $l = name.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        name.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = typeLhs.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        typeLhs.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = patternLhs.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        patternLhs.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = typeRhs.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        typeRhs.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
      $l = patternRhs.getLocation();
      if ($l.hasLineColumn() && $l.getBeginLine() <= $line && $l.getEndLine() >= $line) {
        patternRhs.addForLineNumber($line, $result);
      }
      if ($l.getBeginLine() > $line) {
        return;
      }
      
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Sugar)) {
        return false;
      }        
      Sugar tmp = (Sugar) o;
      return true && tmp.tags.equals(this.tags) && tmp.visibility.equals(this.visibility) && tmp.name.equals(this.name) && tmp.typeLhs.equals(this.typeLhs) && tmp.patternLhs.equals(this.patternLhs) && tmp.typeRhs.equals(this.typeRhs) && tmp.patternRhs.equals(this.patternRhs) ; 
    }
   
    @Override
    public int hashCode() {
      return 179 + 457 * tags.hashCode() + 839 * visibility.hashCode() + 2 * name.hashCode() + 461 * typeLhs.hashCode() + 877 * patternLhs.hashCode() + 599 * typeRhs.hashCode() + 281 * patternRhs.hashCode() ; 
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
    public org.rascalmpl.ast.Name getName() {
      return this.name;
    }
  
    @Override
    public boolean hasName() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Type getTypeLhs() {
      return this.typeLhs;
    }
  
    @Override
    public boolean hasTypeLhs() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getPatternLhs() {
      return this.patternLhs;
    }
  
    @Override
    public boolean hasPatternLhs() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Type getTypeRhs() {
      return this.typeRhs;
    }
  
    @Override
    public boolean hasTypeRhs() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.Expression getPatternRhs() {
      return this.patternRhs;
    }
  
    @Override
    public boolean hasPatternRhs() {
      return true;
    }	
  
    @Override
    public Object clone()  {
      return newInstance(getClass(), src, (IConstructor) null , clone(tags), clone(visibility), clone(name), clone(typeLhs), clone(patternLhs), clone(typeRhs), clone(patternRhs));
    }
            
  }
}