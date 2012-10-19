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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An ASTNode deep-copier, which also copies the associated
 * bindings for each node.  It extends ASTMatcher, but for its
 * one-for-one node serving rather than any matching functionality.
 *
 * @author Tom Ball
 */
public class NodeCopier extends ASTMatcher {
  private static NodeCopier instance = new NodeCopier();

  /**
   * Returns a deep copy of the subtree of AST nodes rooted at the
   * given node.
   */
  @SuppressWarnings("unchecked")
  public static <E extends ASTNode> E copySubtree(AST ast, E node) {
    E newNode = (E) ASTNode.copySubtree(ast, node);
    instance.safeSubtreeMatch(node, newNode);
    return newNode;
  }

  /**
   * Returns a deep copy of the subtrees of AST nodes rooted at the
   * given list of nodes.
   */
  public static <E extends ASTNode> List<E> copySubtrees(AST ast, List<E> nodes) {
      List<E> result = new ArrayList<E>(nodes.size());
      for (Iterator<E> it = nodes.iterator(); it.hasNext(); ) {
          E oldNode = it.next();
          E newNode = copySubtree(ast, oldNode);
          result.add(newNode);
      }
      return result;
  }

  private void copy(Object from, Object to) {
    IBinding sharedBinding = Types.getBinding(from);
    assert sharedBinding != null;
    Types.addBinding(to, sharedBinding);

    if (from instanceof ASTNode) {
      ASTNode fromNode = (ASTNode) from;
      ASTNode replacement = Types.getNode(fromNode);
      if (replacement != null) {
        Types.substitute((ASTNode) to, replacement);
      }
      Symbols.substitute(fromNode, (ASTNode) to);
    }
  }

  @Override
  public boolean match(AnonymousClassDeclaration node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ArrayAccess node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ArrayCreation node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ArrayInitializer node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ArrayType node, Object other) {
    copy(node, other);
    copy(node.getComponentType(), ((ArrayType) other).getComponentType());// fix for ASTMatcher bug
    return super.match(node, other);
  }

  @Override
  public boolean match(Assignment node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(BooleanLiteral node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(CastExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(CharacterLiteral node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ClassInstanceCreation node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ConditionalExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ConstructorInvocation node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(EnumConstantDeclaration node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(EnumDeclaration node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(FieldAccess node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(InfixExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(InstanceofExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(MethodDeclaration node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(MethodInvocation node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(MethodRef node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(NullLiteral node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(NumberLiteral node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ParameterizedType node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ParenthesizedExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(PostfixExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(PrefixExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(PrimitiveType node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(QualifiedName node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(QualifiedType node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(SimpleName node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(SimpleType node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(SingleVariableDeclaration node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(StringLiteral node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(SuperConstructorInvocation node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(SuperFieldAccess node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(SuperMethodInvocation node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(ThisExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(TypeDeclaration node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(TypeLiteral node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(TypeParameter node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(VariableDeclarationExpression node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }

  @Override
  public boolean match(VariableDeclarationFragment node, Object other) {
    copy(node, other);
    return super.match(node, other);
  }
}
