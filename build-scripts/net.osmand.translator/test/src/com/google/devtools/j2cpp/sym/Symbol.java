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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;

/**
 * Base class for program symbols.  A symbol at a minimum consists of a name,
 * a binding (type) and a scope.  One difference between symbols and JDT
 * bindings is that only declaration bindings are used, not separate
 * references to declarations.  This makes it easier to do global changes,
 * such as changing names and adding method parameters.
 *
 * @author Tom Ball
 */
public abstract class Symbol {

  private String name;
  private final ASTNode declaration;
  private IBinding binding;
  private final Scope enclosingScope;

  protected Symbol(String name, ASTNode declaration, IBinding type, Scope enclosingScope) {
    this.name = name;
    this.declaration = declaration;
    this.binding = type;
    this.enclosingScope = enclosingScope;
    enclosingScope.define(this);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public IBinding getBinding() {
    return binding;
  }

  public void setBinding(IBinding binding) {
    this.binding = binding;
  }

  public Scope getEnclosingScope() {
    return enclosingScope;
  }

  public ASTNode getDeclaration() {
    return declaration;
  }
}
