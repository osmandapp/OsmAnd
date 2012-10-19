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

/**
 * IOSMethodBinding: synthetic binding for an iOS method.
 *
 * @author Tom Ball
 */
public class IOSMethodBinding implements IMethodBinding {
  private final String name;
  private final IMethodBinding delegate;
  private final ITypeBinding clazz;
  private final ITypeBinding returnType;
  private final boolean hasVarArgsTarget;

  public IOSMethodBinding(String name, IMethodBinding originalBinding, ITypeBinding clazz) {
    this(name, originalBinding, clazz, originalBinding.getReturnType(),
        originalBinding.isVarargs());
  }

  public IOSMethodBinding(String name, IMethodBinding originalBinding, ITypeBinding clazz,
      ITypeBinding returnType, boolean hasVarArgs) {
    this.name = name;
    delegate = originalBinding;
    this.clazz = clazz;
    this.returnType = returnType;
    Symbols.queueForResolution(this);
    if (clazz instanceof IOSTypeBinding) {
      ((IOSTypeBinding) clazz).addMethod(this);
    }
    this.hasVarArgsTarget = hasVarArgs;
  }

  @Override
  public IAnnotationBinding[] getAnnotations() {
    return new IAnnotationBinding[0];
  }

  @Override
  public int getKind() {
    return IBinding.METHOD;
  }

  @Override
  public int getModifiers() {
    return delegate.getModifiers();
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
    return delegate.getJavaElement();
  }

  @Override
  public String getKey() {
    return delegate.getKey();
  }

  @Override
  public boolean isEqualTo(IBinding binding) {
    return binding instanceof IOSMethodBinding
      && delegate.isEqualTo(binding);
  }

  @Override
  public boolean isConstructor() {
    return delegate.isConstructor();
  }

  @Override
  public boolean isDefaultConstructor() {
    return delegate.isDefaultConstructor();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ITypeBinding getDeclaringClass() {
    return clazz;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

  @Override
  public IAnnotationBinding[] getParameterAnnotations(int paramIndex) {
    return new IAnnotationBinding[0];
  }

  @Override
  public ITypeBinding[] getParameterTypes() {
    return delegate.getParameterTypes();
  }

  @Override
  public ITypeBinding getReturnType() {
    return returnType;
  }

  @Override
  public ITypeBinding[] getExceptionTypes() {
    return delegate.getExceptionTypes();
  }

  @Override
  public ITypeBinding[] getTypeParameters() {
    return new ITypeBinding[0];
  }

  @Override
  public boolean isAnnotationMember() {
    return false;
  }

  @Override
  public boolean isGenericMethod() {
    return false;
  }

  @Override
  public boolean isParameterizedMethod() {
    return false;
  }

  @Override
  public ITypeBinding[] getTypeArguments() {
    return new ITypeBinding[0];
  }

  @Override
  public IMethodBinding getMethodDeclaration() {
    return this;
  }

  @Override
  public boolean isRawMethod() {
    return true;
  }

  @Override
  public boolean isSubsignature(IMethodBinding otherMethod) {
    return delegate.isSubsignature(otherMethod);
  }

  @Override
  public boolean isVarargs() {
    return delegate.isVarargs();
  }

  @Override
  public boolean overrides(IMethodBinding method) {
    return delegate.overrides(method);
  }

  public IMethodBinding getDelegate() {
    return delegate;
  }

  public boolean hasVarArgsTarget() {
    return hasVarArgsTarget;
  }
}
