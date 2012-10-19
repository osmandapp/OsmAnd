/*
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
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
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

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

/**
 * Walks an AST and asserts there is a resolved binding for every ASTNode type
 * that is supposed to have one.
 *
 * @author Tom Ball
 */
class BindingMapVerifier extends ErrorReportingASTVisitor {
  private final Map<Object, IBinding> bindingMap;

  public static void verify(ASTNode node, Map<Object, IBinding> bindingMap) {
    BindingMapVerifier builder = new BindingMapVerifier(bindingMap);
    builder.run(node);
  }

  private BindingMapVerifier(Map<Object, IBinding> bindingMap) {
    this.bindingMap = bindingMap;
  }

  private boolean verify(ASTNode node) {
    assert bindingMap.get(node) != null;
    return true;
  }

  // There is a lot of seemingly duplicate code here, but each
  // resolve*Binding() method is specific to the node type.

  @Override
  public boolean visit(AnnotationTypeDeclaration node) {
    return verify(node);
  }

  @Override
  public boolean visit(AnonymousClassDeclaration node) {
    return verify(node);
  }

  @Override
  public boolean visit(ArrayAccess node) {
    return verify(node);

  }

  @Override
  public boolean visit(ArrayCreation node) {
    return verify(node);

  }

  @Override
  public boolean visit(ArrayInitializer node) {
    return verify(node);

  }

  @Override
  public boolean visit(ArrayType node) {
    return verify(node);

  }

  @Override
  public boolean visit(Assignment node) {
    return verify(node);

  }

  @Override
  public boolean visit(BooleanLiteral node) {
    return verify(node);

  }

  @Override
  public boolean visit(CastExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(CharacterLiteral node) {
    return verify(node);

  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    IBinding binding = bindingMap.get(node);
    assert binding instanceof IMethodBinding;
    assert ((IMethodBinding) binding).isVarargs() ||
        node.arguments().size() == ((IMethodBinding) binding).getParameterTypes().length;
    return verify(node);
  }

  @Override
  public boolean visit(ConditionalExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(ConstructorInvocation node) {
    IBinding binding = bindingMap.get(node);
    assert binding instanceof IMethodBinding;
    return true;
  }

  @Override
  public boolean visit(EnumConstantDeclaration node) {
    return verify(node);

  }

  @Override
  public boolean visit(EnumDeclaration node) {
    return verify(node);

  }

  @Override
  public boolean visit(FieldAccess node) {
    return verify(node);

  }

  @Override
  public boolean visit(InfixExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(InstanceofExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(MethodDeclaration node) {
    IMethodBinding binding = (IMethodBinding) bindingMap.get(node);
    assert node.parameters().size() == binding.getParameterTypes().length;
    return verify(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    IBinding binding = bindingMap.get(node);
    assert binding instanceof IMethodBinding;
    IMethodBinding method = (IMethodBinding) binding;
    assert method.isVarargs() || node.arguments().size() == method.getParameterTypes().length
        || binding instanceof IOSMethodBinding;
    return true;
  }

  @Override
  public boolean visit(NumberLiteral node) {
    return verify(node);

  }

  @Override
  public boolean visit(ParameterizedType node) {
    return verify(node);

  }

  @Override
  public boolean visit(ParenthesizedExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(PostfixExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(PrefixExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(PrimitiveType node) {
    return verify(node);

  }

  @Override
  public boolean visit(QualifiedName node) {
    return verify(node);

  }

  @Override
  public boolean visit(QualifiedType node) {
    return verify(node);

  }

  @Override
  public boolean visit(SimpleName node) {
    return verify(node);

  }

  @Override
  public boolean visit(SimpleType node) {
    return verify(node);

  }

  @Override
  public boolean visit(SingleVariableDeclaration node) {
    return verify(node);

  }

  @Override
  public boolean visit(StringLiteral node) {
    return verify(node);

  }

  @Override
  public boolean visit(SuperConstructorInvocation node) {
    return verify(node);

  }

  @Override
  public boolean visit(SuperFieldAccess node) {
    return verify(node);

  }

  @Override
  public boolean visit(SuperMethodInvocation node) {
    return verify(node);

  }

  @Override
  public boolean visit(ThisExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(TypeDeclaration node) {
    return verify(node);

  }

  @Override
  public boolean visit(TypeParameter node) {
    return verify(node);

  }

  @Override
  public boolean visit(TypeLiteral node) {
    return verify(node);

  }

  @Override
  public boolean visit(VariableDeclarationExpression node) {
    return verify(node);

  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
    return verify(node);

  }
}
