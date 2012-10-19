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

import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
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

import com.google.common.collect.Maps;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

/**
 * Walks an AST and copies resolved bindings to maps in {@link Types}.
 * The reason this step is necessary is because the JDT DOM has no
 * way of adding types or resolving added or modified code.  With a
 * separate map, both ASTNodes and IOS synthetic nodes can be
 * resolved.
 *
 * @author Tom Ball
 */
class BindingMapBuilder extends ErrorReportingASTVisitor {

  // keys may be ASTNodes or IOS nodes
  private final Map<Object, IBinding> bindingMap = Maps.newHashMap();

  public static Map<Object, IBinding> buildBindingMap(CompilationUnit unit) {
    BindingMapBuilder builder = new BindingMapBuilder();
    builder.run(unit);
    BindingMapVerifier.verify(unit, builder.bindingMap);
    return builder.bindingMap;
  }

  private void put(ASTNode node, IBinding binding) {
    assert binding != null;
    bindingMap.put(node, binding);
  }

  // There is a lot of seemingly duplicate code here, but each
  // resolve*Binding() method is specific to the node type.

  @Override
  public boolean visit(AnnotationTypeDeclaration node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(AnonymousClassDeclaration node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(ArrayAccess node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(ArrayCreation node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(ArrayInitializer node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(ArrayType node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(Assignment node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(BooleanLiteral node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(BreakStatement node) {
    SimpleName label = node.getLabel();
    if (label != null) {
      put(label, createLabelBinding(label));
    }
    return false;
  }

  @Override
  public boolean visit(CastExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(CharacterLiteral node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    put(node, node.resolveConstructorBinding());
    return true;
  }

  @Override
  public boolean visit(ConditionalExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(ConstructorInvocation node) {
    put(node, node.resolveConstructorBinding());
    return true;
  }

  @Override
  public boolean visit(ContinueStatement node) {
    SimpleName label = node.getLabel();
    if (label != null) {
      put(label, createLabelBinding(label));
    }
    return false;
  }

  @Override
  public boolean visit(EnumConstantDeclaration node) {
    put(node, node.resolveConstructorBinding());
    return true;
  }

  @Override
  public boolean visit(EnumDeclaration node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(FieldAccess node) {
    put(node, node.resolveFieldBinding());
    return true;
  }

  @Override
  public boolean visit(InfixExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(InstanceofExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(LabeledStatement node) {
    SimpleName label = node.getLabel();
    if (label != null) {
      put(label, createLabelBinding(label));
    }
    node.getBody().accept(this);
    return false;
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(MethodInvocation node) {
    put(node, node.resolveMethodBinding());
    return true;
  }

  @Override
  public boolean visit(MethodRef node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(NullLiteral node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(NumberLiteral node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(ParameterizedType node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(ParenthesizedExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(PostfixExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(PrefixExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(PrimitiveType node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(QualifiedName node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(QualifiedType node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(SimpleName node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(SimpleType node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(SingleVariableDeclaration node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(StringLiteral node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(SuperConstructorInvocation node) {
    put(node, node.resolveConstructorBinding());
    return true;
  }

  @Override
  public boolean visit(SuperFieldAccess node) {
    put(node, node.resolveFieldBinding());
    return true;
  }

  @Override
  public boolean visit(SuperMethodInvocation node) {
    put(node, node.resolveMethodBinding());
    return true;
  }

  @Override
  public boolean visit(ThisExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(TypeLiteral node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(TypeParameter node) {
    put(node, node.resolveBinding());
    return true;
  }

  @Override
  public boolean visit(VariableDeclarationExpression node) {
    put(node, node.resolveTypeBinding());
    return true;
  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
    put(node, node.resolveBinding());
    return true;
  }

  private ITypeBinding createLabelBinding(SimpleName label) {
    return new IOSTypeBinding(label.getIdentifier(), false);
  }
}
