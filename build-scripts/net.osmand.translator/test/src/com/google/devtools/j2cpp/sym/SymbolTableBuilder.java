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
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Maps;

import com.google.devtools.j2cpp.types.Types;

/**
 * Builds a symbol table by walking a source file's AST.
 *
 * @author Tom Ball
 */
class SymbolTableBuilder extends ASTVisitor {
  private Stack<Scope> scopeStack = new Stack<Scope>();

  private final CompilationUnit unit;
  private final Map<IBinding, Symbol> symbolTable;
  private final Map<ASTNode, Scope> scopeTable = Maps.newHashMap();

  static Map<ASTNode, Scope> build(ASTNode node, Map<IBinding, Symbol> symbolTable) {
    CompilationUnit unit = node instanceof CompilationUnit ? (CompilationUnit) node : null;
    SymbolTableBuilder builder = new SymbolTableBuilder(unit, symbolTable);
    node.accept(builder);
    return builder.scopeTable;
  }

  public SymbolTableBuilder(CompilationUnit unit, Map<IBinding, Symbol> symbolTable) {
    this.unit = unit;
    this.symbolTable = symbolTable;
    scopeStack.push(Symbols.getGlobalScope());
  }

  private Scope currentScope() {
    return scopeStack.peek();
  }

  /**
   * Push a block scope onto the stack.
   */
  private void pushScope(ASTNode owner) {
    Scope currentScope = currentScope();
    Scope newScope = new Scope(owner, null, currentScope);
    scopeStack.push(newScope);
    scopeTable.put(owner, newScope);
  }

  /**
   * Push a new type declaration scope.
   */
  private TypeSymbol pushType(ASTNode node) {
    assert node instanceof AbstractTypeDeclaration || node instanceof AnonymousClassDeclaration;
    ITypeBinding type = Types.getTypeBinding(node);
    type = type.getTypeDeclaration();
    TypeSymbol symbol =
        symbolTable.containsKey(type) ? (TypeSymbol) symbolTable.get(type) : addType(node, type);
    currentScope().define(symbol);
    Scope newScope = symbol.getScope();
    scopeStack.push(newScope);
    scopeTable.put(node, newScope);
    return symbol;
  }

  /**
   * Push a new method declaration scope.
   */
  private MethodSymbol pushMethod(MethodDeclaration declaration) {
    IMethodBinding method = Types.getMethodBinding(declaration);
    method = method.getMethodDeclaration();
    MethodSymbol symbol = symbolTable.containsKey(method) ?
        (MethodSymbol) symbolTable.get(method) : addMethod(declaration);
    currentScope().define(symbol);
    Scope newScope = symbol.getScope();
    scopeStack.push(newScope);
    scopeTable.put(declaration, newScope);
    return symbol;
  }

  private TypeSymbol addType(ITypeBinding type) {
    ASTNode declaration = unit != null ? unit.findDeclaringNode(type.getTypeDeclaration()) : null;
    return declaration != null ? addType(declaration, type) : null;
  }

  /**
   * Adds a type symbol.
   */
  private TypeSymbol addType(ASTNode declaration, ITypeBinding type) {
    if (type == null || type.isNullType() || Types.isVoidType(type)) {
      return null;
    }
    type = type.getTypeDeclaration();
    TypeSymbol symbol = (TypeSymbol) symbolTable.get(type);
    if (symbol == null) {
      assert declaration instanceof AbstractTypeDeclaration ||
          declaration instanceof AnonymousClassDeclaration;
      Scope enclosingScope = Symbols.getGlobalScope();
      ITypeBinding superType = Types.mapType(type.getSuperclass());
      if (superType != null) {
        TypeSymbol superclass = Symbols.resolve(superType);
        enclosingScope = superclass.getScope();
      }
      symbol = new TypeSymbol(declaration, type, enclosingScope);
      symbolTable.put(type, symbol);
    }
    return symbol;
  }

  /**
   * Adds a method symbol.
   */
  private MethodSymbol addMethod(MethodDeclaration declaration) {
    IMethodBinding method = Types.getMethodBinding(declaration).getMethodDeclaration();
    MethodSymbol symbol = (MethodSymbol) symbolTable.get(method);
    if (symbol == null) {
      symbol = new MethodSymbol(declaration, method);
      symbolTable.put(method, symbol);
    }
    return symbol;
  }

  /**
   * Adds a variable symbol.
   */
  private VariableSymbol addVariable(ASTNode declaration) {
    return addVariable(declaration,
        Types.getVariableBinding(declaration).getVariableDeclaration());
  }

  private VariableSymbol addVariable(ASTNode declaration, IVariableBinding var) {
    VariableSymbol symbol = (VariableSymbol) symbolTable.get(var);
    if (symbol == null) {
      symbol = new VariableSymbol(declaration, var, currentScope());
      symbolTable.put(var, symbol);
    } else {
      currentScope().define(symbol);
    }
    return symbol;
  }

  private void popScope() {
    scopeStack.pop();
  }

  @Override
  public boolean visit(AnnotationTypeDeclaration node) {
    pushType(node);
    return true;
  }

  @Override
  public void endVisit(AnnotationTypeDeclaration node) {
    popScope();
  }

  @Override
  public boolean visit(AnonymousClassDeclaration node) {
    pushType(node);
    return true;
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {
    popScope();
  }

  @Override
  public boolean visit(Block node) {
    pushScope(node);
    return true;
  }

  @Override
  public void endVisit(Block node) {
    popScope();
  }

  @Override
  public boolean visit(CatchClause node) {
    pushScope(node);
    return true;
  }

  @Override
  public void endVisit(CatchClause node) {
    popScope();
  }

  @Override
  public boolean visit(EnhancedForStatement node) {
    pushScope(node);
    return true;
  }

  @Override
  public void endVisit(EnhancedForStatement node) {
    popScope();
  }

  @Override
  public boolean visit(EnumConstantDeclaration node) {
    addVariable(node, node.resolveVariable());
    return true;
  }

  @Override
  public boolean visit(EnumDeclaration node) {
    pushType(node);
    return true;
  }

  @Override
  public void endVisit(EnumDeclaration node) {
    popScope();
  }

  @Override
  public boolean visit(ForStatement node) {
    pushScope(node);
    return true;
  }

  @Override
  public void endVisit(ForStatement node) {
    popScope();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(MethodDeclaration node) {
    MethodSymbol symbol = pushMethod(node);
    symbol.setParameters(node.parameters());  // safe by definition
    return true;
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    popScope();
  }

  @Override
  public boolean visit(SingleVariableDeclaration node) {
    addVariable(node);
    return true;
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    pushType(node);
    return true;
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    popScope();
  }

  @Override
  public boolean visit(VariableDeclarationExpression node) {
    addType(Types.getTypeBinding(node));
    return true;
  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
    addVariable(node);
    return true;
  }
}
