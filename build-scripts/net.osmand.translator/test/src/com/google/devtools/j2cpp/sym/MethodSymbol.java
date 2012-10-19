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

import com.google.common.collect.Lists;

import com.google.devtools.j2cpp.types.Types;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.Iterator;
import java.util.List;

/**
 * A symbol for a method and its binding.
 *
 * @author Tom Ball
 */
public class MethodSymbol extends Symbol {
  private final List<VariableSymbol> parameters = Lists.newArrayList();
  private final Scope scope;

  @SuppressWarnings("unchecked")
  public MethodSymbol(MethodDeclaration decl) {
    this(decl, Types.getMethodBinding(decl));
    setParameters(decl.parameters());  // safe by definition
  }

  public MethodSymbol(MethodDeclaration decl, IMethodBinding binding) {
    this(decl, binding, Symbols.resolve(binding.getDeclaringClass()).getScope());
  }

  public MethodSymbol(MethodDeclaration decl, IMethodBinding binding, Scope enclosingScope) {
    super(binding.getName(), decl, binding, enclosingScope);
    scope = new Scope(decl, binding, enclosingScope);
  }

  public List<VariableSymbol> getParameters() {
    return parameters;
  }

  public void setParameters(List<SingleVariableDeclaration> params) {
    List<VariableSymbol> parameters = Lists.newArrayList();
    for (SingleVariableDeclaration param : params) {
      VariableSymbol var = Symbols.resolve(Types.getVariableBinding(param));
      scope.define(var);
      parameters.add(var);
    }
  }

  public void addParameter(VariableSymbol parameter) {
    parameters.add(parameter);
    scope.define(parameter);
  }

  public void addParameter(int index, VariableSymbol parameter) {
    parameters.add(index, parameter);
    scope.define(parameter);
  }

  public Scope getScope() {
    return scope;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getName());
    sb.append('(');
    if (parameters != null) {
      Iterator<VariableSymbol> iter = parameters.iterator();
      while (iter.hasNext()) {
        sb.append(iter.next());
        if (iter.hasNext()) {
          sb.append(", ");
        }
      }
    }
    sb.append(')');
    return sb.toString();
  }
}
