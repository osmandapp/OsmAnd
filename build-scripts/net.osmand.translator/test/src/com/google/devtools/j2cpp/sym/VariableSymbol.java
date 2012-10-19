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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * A symbol for a variable and its binding.
 *
 * @author Tom Ball
 */
public class VariableSymbol extends Symbol {

  public VariableSymbol(IVariableBinding binding, Scope enclosingScope) {
    this(null, binding, enclosingScope);
  }

  public VariableSymbol(ASTNode declaration, IVariableBinding binding, Scope enclosingScope) {
    super(binding.getName(), declaration, binding, enclosingScope);
  }

  public TypeSymbol getTypeSymbol() {
    ITypeBinding type = ((IVariableBinding) getBinding()).getType();
    return Symbols.resolve(type);
  }

  @Override
  public String toString() {
    return getTypeSymbol().toReferenceString() + ' ' + getName();
  }
}
