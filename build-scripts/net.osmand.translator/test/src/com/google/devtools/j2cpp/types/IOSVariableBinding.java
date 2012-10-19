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

import com.google.devtools.j2cpp.sym.Symbols;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.lang.reflect.Modifier;

/**
 * IOSVariableBinding: synthetic binding for an iOS variable or parameter.
 *
 * @author Tom Ball
 */
public class IOSVariableBinding implements IVariableBinding {
  private final String name;
  private final int modifiers;
  private final Kind kind;
  private final ITypeBinding type;
  private final ITypeBinding declaringClass;
  private final IMethodBinding declaringMethod;
  private final int index;

  private enum Kind { FIELD, PARAMETER, ENUM_CONSTANT };

  public static IOSVariableBinding newParameter(String name, int index, ITypeBinding type,
      IMethodBinding declaringMethod, ITypeBinding declaringClass, boolean isFinal) {
    int modifiers = isFinal ? Modifier.FINAL : 0;
    assert declaringMethod != null || declaringClass != null;
    return new IOSVariableBinding(name, modifiers, Kind.PARAMETER, type, declaringClass,
        declaringMethod, index);
  }

  public static IOSVariableBinding newField(String name, int modifiers, ITypeBinding type,
      ITypeBinding declaringClass) {
    return new IOSVariableBinding(name, modifiers, Kind.FIELD, type, declaringClass, null, 0);
  }

  private IOSVariableBinding(String name, int modifiers, Kind kind, ITypeBinding type,
      ITypeBinding declaringClass, IMethodBinding declaringMethod, int index) {
    this.name = name;
    this.modifiers = modifiers;
    this.kind = kind;
    this.type = type;
    this.declaringClass = declaringClass;
    this.declaringMethod = declaringMethod;
    this.index = index;
    Symbols.queueForResolution(this);
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
    return kind == Kind.FIELD;
  }

  @Override
  public boolean isEnumConstant() {
    return kind == Kind.ENUM_CONSTANT;
  }

  @Override
  public boolean isParameter() {
    return kind == Kind.PARAMETER;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ITypeBinding getDeclaringClass() {
    return declaringClass;
  }

  @Override
  public ITypeBinding getType() {
    return type;
  }

  @Override
  public int getVariableId() {
    return index;
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

}
