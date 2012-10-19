/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2cpp.types;

import com.google.common.base.Preconditions;

import com.google.devtools.j2cpp.sym.Symbols;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;

import javax.annotation.Nullable;

/**
 * Binding class for variables and parameters created during translation.
 *
 * @author Tom Ball
 */
public class GeneratedVariableBinding implements IVariableBinding {
  private final String name;
  private final int modifiers;
  private final ITypeBinding type;
  private ITypeBinding declaringClass;
  private IMethodBinding declaringMethod;  // optional
  private final boolean isParameter;
  private final boolean isField;

  public static final String UNNAMED_VARIABLE = "<unnamed-variable>";

  public GeneratedVariableBinding(String name, int modifiers, ITypeBinding type,
      boolean isField, boolean isParameter, @Nullable ITypeBinding declaringClass,
      @Nullable IMethodBinding declaringMethod) {
    Preconditions.checkNotNull(name);
    this.name = name;
    this.modifiers = modifiers;
    this.type = type;
    this.isParameter = isParameter;
    this.declaringClass = declaringClass;
    this.declaringMethod = declaringMethod;
    this.isField = isField;
    Symbols.resolve(this);
  }

  /**
   * For renaming variables: creates a new binding from a new declaration and
   * the original binding.
   */
  public GeneratedVariableBinding(VariableDeclaration var, int modifiers, boolean isParameter,
        IVariableBinding oldBinding) {
    this(var.getName().getIdentifier(), modifiers, oldBinding.getType(), oldBinding.isField(),
        isParameter, oldBinding.getDeclaringClass(), oldBinding.getDeclaringMethod());
  }

  /**
   * Create a place-holder variable binding from a type binding.
   */
  public GeneratedVariableBinding(ITypeBinding binding, boolean isField, boolean isParameter,
      ITypeBinding declaringClass, IMethodBinding declaringMethod) {
    this(UNNAMED_VARIABLE, 0, binding, isField, isParameter, declaringClass, declaringMethod);
  }

  @Override
  public IAnnotationBinding[] getAnnotations() {
    return new IAnnotationBinding[0];
  }

  @Override
  public int getKind() {
    return IBinding.VARIABLE;
  }

  @Override
  public int getModifiers() {
    return modifiers;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isRecovered() {
    return false;
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }

  @Override
  public IJavaElement getJavaElement() {
    throw new AssertionError("not implemented");
  }

  @Override
  public String getKey() {
    throw new AssertionError("not implemented");
  }

  @Override
  public boolean isEqualTo(IBinding binding) {
    return equals(binding);
  }

  @Override
  public boolean isField() {
    return isField;
  }

  @Override
  public boolean isEnumConstant() {
    return false;
  }

  @Override
  public boolean isParameter() {
    return isParameter;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ITypeBinding getDeclaringClass() {
    return declaringClass;
  }

  public void setDeclaringClass(ITypeBinding newBinding) {
    declaringClass = newBinding;
  }

  @Override
  public ITypeBinding getType() {
    return type;
  }

  @Override
  public int getVariableId() {
    throw new AssertionError("not implemented");
  }

  @Override
  public Object getConstantValue() {
    return null;
  }

  @Override
  public IMethodBinding getDeclaringMethod() {
    return declaringMethod;
  }

  @Override
  public IVariableBinding getVariableDeclaration() {
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
    result = prime * result + ((declaringMethod == null)
        ? 0 : declaringMethod.getName().hashCode());
    result = prime * result + (isParameter ? 1231 : 1237);
    result = prime * result + modifiers;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return prime * result + ((type == null) ? 0 : type.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GeneratedVariableBinding)) {
      return false;
    }
    GeneratedVariableBinding other = (GeneratedVariableBinding) obj;
    if (!name.equals(other.name)
        || modifiers != other.modifiers
        || isParameter != other.isParameter
        || !type.equals(other.type)) {
      return false;
    }
    if (declaringClass == null) {
      if (other.declaringClass != null) {
        return false;
      }
    } else if (!declaringClass.equals(other.declaringClass)) {
      return false;
    }
    if (declaringMethod == null) {
      if (other.declaringMethod != null) {
        return false;
      }
    } else if (!declaringMethod.toString().equals(other.declaringMethod.toString())) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    ASTNode.printModifiers(modifiers, sb);
    sb.append(type != null ? type.getName() : "<no type>");
    sb.append(" ");
    sb.append((name != null) ? name : "<no name>");
    return sb.toString();
  }
}
