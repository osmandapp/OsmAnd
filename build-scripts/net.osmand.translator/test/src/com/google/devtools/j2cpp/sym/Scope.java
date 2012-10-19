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

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A group of symbols, associated with an "owning" AST node, such as a type,
 * method, or block.  Scopes nest, from the global scope, types, methods, and
 * down to blocks.  Java packages are not included, since Objective-C doesn't
 * have namespaces.
 *
 * @author Tom Ball
 */
public class Scope {

  private final Map<IBinding, Symbol> symbols = Maps.newLinkedHashMap();
  private final Scope parent;
  private final ASTNode owner;
  private final IBinding owningType;

  public Scope(ASTNode owner, IBinding type, Scope parent) {
    Preconditions.checkNotNull(parent);
    this.owner = owner;
    this.owningType = type;
    this.parent = parent;
  }

  // A scope without a parent, which only is true for the global scope.
  Scope() {
    owner = null;
    owningType = null;
    parent = null;
  }

  public void define(Symbol symbol) {
    if (!symbols.containsKey(symbol.getBinding())) {
      symbols.put(symbol.getBinding(), symbol);
    }
  }

  public Symbol resolve(IBinding binding) {
    Symbol s = symbols.get(binding);
    if (s != null) {
      return s;
    }
    return parent != null ? parent.resolve(binding) : null;
  }

  public ASTNode getOwner() {
    return owner;
  }

  public Scope getParent() {
    return parent;
  }

  /**
   * Returns true if a symbol is defined in this specific scope, or any
   * parents of this scope.  This is the usual way to determine symbol
   * membership in a scope.
   */
  public boolean contains(Symbol symbol) {
    IBinding key = symbol.getBinding();
    if (symbols.containsKey(key)) {
      return true;
    }
    Scope scope = this;
    while ((scope = scope.parent) != null) {
      if (scope.symbols.containsKey(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if a symbol is defined in this specific scope.  Parent
   * scopes are not searched; to search for a symbol in a scope and its
   * parents, use contains().
   */
  public boolean owns(Symbol symbol) {
    return symbols.containsKey(symbol.getBinding());
  }

  public List<Symbol> getMembers() {
    return Lists.newArrayList(symbols.values());
  }

  @Override
  public String toString() {
    String values = symbols.values().toString();
    if (parent == null) {
      return "Global" + values;
    }
    return owningType != null ? owningType.getName() + values : values;
  }
}
