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

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.devtools.j2cpp.types.Types;

/**
 * Symbol related utility methods, using a symbol table created from the
 * JDT bindings of the compiled sources.
 *
 * @author Tom Ball
 */
public class Symbols {

  private final Map<IBinding, Symbol> symbolTable = Maps.newHashMap();
  private final CompilationUnit currentUnit;
  private final Scope globalScope = new Scope();
  private static Symbols instance;
  private Map<ASTNode, Scope> scopeTable;
  private static Set<IBinding> resolutionSet = Sets.newLinkedHashSet();

  /**
   * Initialize this service using the AST returned by the parser.
   */
  public static void initialize(CompilationUnit unit) {
    instance = new Symbols(unit);
    for (IBinding binding : resolutionSet) {
      resolveQueuedBinding(binding);
    }
    resolutionSet.clear();
    instance.scopeTable = SymbolTableBuilder.build(unit, instance.symbolTable);
  }

  public static void cleanup() {
    instance = null;
    resolutionSet.clear();
  }

  public static void scanAST(ASTNode node) {
    Map<ASTNode, Scope> newScopes = SymbolTableBuilder.build(node, instance.symbolTable);
    instance.scopeTable.putAll(newScopes);
  }

  private Symbols(CompilationUnit unit) {
    this.currentUnit = unit;
  }

  public static Scope getGlobalScope() {
    return instance.globalScope;
  }

  public static Scope getScope(ASTNode node) {
    ASTNode n = node;
    while (n != null) {
      Scope scope = instance.scopeTable.get(n);
      if (scope != null) {
        return scope;
      }
      n = n.getParent();
    }
    throw new AssertionError("could not find scope for " + node);
  }

  public static void queueForResolution(IBinding binding) {
    if (resolutionSet != null) {
      resolutionSet.add(binding);
    } else {
      resolveQueuedBinding(binding);
    }
  }

  private static void resolveQueuedBinding(IBinding binding) {
    if (binding instanceof ITypeBinding) {
      resolve((ITypeBinding) binding);
    } else if (binding instanceof IMethodBinding) {
      resolve((IMethodBinding) binding);
    } else {
      resolve((IVariableBinding) binding);
    }
  }

  public static TypeSymbol resolve(ITypeBinding binding) {
    if (binding == null || binding.isNullType() || Types.isVoidType(binding)) {
      return null;
    }
    binding = binding.getTypeDeclaration();
    TypeSymbol symbol = (TypeSymbol) instance.symbolTable.get(binding);
    if (symbol == null) {
      // Update table with symbol references.
      if (binding.getSuperclass() != null) {
        resolve(binding.getSuperclass());
      }
      for (ITypeBinding tb : binding.getInterfaces()) {
        resolve(tb);
      }
      if (binding.isArray()) {
        resolve(binding.getComponentType());
      }
      Scope enclosingScope;
      if (binding.getSuperclass() == null || binding.isPrimitive()) {
        enclosingScope = getGlobalScope();
      } else if (binding.isArray()) {
        enclosingScope = resolve(Types.getNSObject()).getScope();
      } else if (binding.getSuperclass() != null) {
        enclosingScope = resolve(binding.getSuperclass()).getScope();
      } else {
        assert binding.getDeclaringMethod() != null;
        MethodSymbol declaringMethod = resolve(binding.getDeclaringMethod());
        enclosingScope = declaringMethod.getScope();
      }
      ASTNode declaration = instance.currentUnit.findDeclaringNode(binding);
      symbol = new TypeSymbol(declaration, binding, enclosingScope);
      instance.symbolTable.put(binding, symbol);
      for (IVariableBinding field : binding.getDeclaredFields()) {
        resolve(field);
      }
      for (IMethodBinding method : binding.getDeclaredMethods()) {
        resolve(method);
      }
    }
    return symbol;
  }

  public static MethodSymbol resolve(IMethodBinding binding) {
    return resolve(null, binding);
  }

  public static MethodSymbol resolve(MethodDeclaration method, IMethodBinding binding) {
    if (binding == null) {
      return null;
    }
    binding = binding.getMethodDeclaration();
    MethodSymbol symbol = (MethodSymbol) instance.symbolTable.get(binding);
    if (symbol == null) {
      TypeSymbol declaringType = resolve(binding.getDeclaringClass());
      if (declaringType != null) {  // Declaring type is null for functions.
        symbol = new MethodSymbol(method, binding, declaringType.getScope());
      }
      instance.symbolTable.put(binding, symbol);
    }
    if (method != null) {
      scanAST(method);
    }
    return symbol;
  }

  public static VariableSymbol resolve(IVariableBinding binding) {
    if (binding == null) {
      return null;
    }
    binding = binding.getVariableDeclaration();
    VariableSymbol symbol = (VariableSymbol) instance.symbolTable.get(binding);
    if (symbol == null) {
      resolve(binding.getType());
      IMethodBinding declaringMethod = binding.getDeclaringMethod();
      if (declaringMethod != null) {
        MethodSymbol method = resolve(declaringMethod);
        symbol = new VariableSymbol(binding, method.getScope());
      } else {
        ITypeBinding declaringClass = binding.getDeclaringClass();
        if (declaringClass != null) {
          TypeSymbol type = resolve(declaringClass);
          symbol = new VariableSymbol(binding, type.getScope());
        }
      }
      instance.symbolTable.put(binding, symbol);
    }
    return symbol;
  }

  public static void substitute(ASTNode oldNode, ASTNode newNode) {
    Scope scope = instance.scopeTable.get(oldNode);
    if (scope != null) {
      instance.scopeTable.remove(oldNode);
      Scope previous = instance.scopeTable.put(newNode, scope);
      assert previous == null;
    }
  }
}
