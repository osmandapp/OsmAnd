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

package com.google.devtools.j2cpp.sym;

import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * A symbol for a class, interface, enum or annotation type.
 *
 * @author Tom Ball
 */
public class TypeSymbol extends Symbol {
  private final Scope scope;

  public TypeSymbol(ASTNode declaration, Scope enclosingScope) {
    this(declaration, Types.getTypeBinding(declaration), enclosingScope);
  }

  public TypeSymbol(ASTNode declaration, ITypeBinding type, Scope enclosingScope) {
    super(type.getName(), declaration, type, enclosingScope);
    scope = new Scope(declaration, type, enclosingScope);
  }

  public ITypeBinding getType() {
    return (ITypeBinding) getBinding();
  }

  public String toReferenceString() {
    return NameTable.javaRefToCpp(getType());
  }

  public Scope getScope() {
    return scope;
  }

  public TypeSymbol getDeclaringClass() {
    return Symbols.resolve(getType().getDeclaringClass());
  }

  public boolean isAnnotation() {
    return getType().isAnnotation();
  }

  public boolean isClass() {
    return getType().isClass();
  }

  public boolean isEnum() {
    return getType().isEnum();
  }

  public boolean isInterface() {
    return getType().isInterface();
  }

  @Override
  public String toString() {
    ITypeBinding type = getType();
    return type.isPrimitive() ? type.getName() : NameTable.getFullName(type);
  }
}
