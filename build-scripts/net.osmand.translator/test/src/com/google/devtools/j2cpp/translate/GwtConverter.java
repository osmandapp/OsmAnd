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

package com.google.devtools.j2cpp.translate;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Sets;

import com.google.devtools.j2cpp.J2ObjC;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.NodeCopier;
import com.google.devtools.j2cpp.types.Types;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;

import java.util.List;
import java.util.Set;

/**
 * Updates the Java AST to remove methods annotated with GwtIncompatible,
 * and code bound by GWT.isClient and GWT.isScript tests.
 *
 * @author Tom Ball
 */
public class GwtConverter extends ErrorReportingASTVisitor {

  private static final String GWT_CLASS = "com.google.gwt.core.client.GWT";

  /**
   * The list of APIs that can be translated by J2ObjC, but not by GWT. These
   * strings were found by scanning common sources like the Google Guava
   * library.
   *
   * Note: this list is neither exhaustive, nor guaranteed to match a
   * specified GwtIncompatible value, since it takes unchecked strings.
   */
  private static final Set<String> compatibleAPIs = Sets.newHashSet(
    "proto", "protos", "Class.isInstance", "Class.isAssignableFrom", "java.util.BitSet");

  @Override
  public boolean visit(ConditionalExpression node) {
    if (isGwtTest(node.getExpression())) {
      // Replace this node with the else expression, removing this conditional.
      ClassConverter.setProperty(node,
          NodeCopier.copySubtree(node.getAST(), node.getElseExpression()));
    }
    node.getElseExpression().accept(this);
    return false;
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    @SuppressWarnings("unchecked")
    List<IExtendedModifier> modifiers = node.modifiers();
    if (hasAnnotation(GwtIncompatible.class, modifiers)) {
      // Remove method from its declaring class.
      ASTNode parent = node.getParent();
      if (parent instanceof TypeDeclarationStatement) {
        parent = ((TypeDeclarationStatement) parent).getDeclaration();
      }
      if (parent instanceof AbstractTypeDeclaration) {
        ((AbstractTypeDeclaration) parent).bodyDeclarations().remove(node);
      } else if (parent instanceof AnonymousClassDeclaration) {
        ((AnonymousClassDeclaration) parent).bodyDeclarations().remove(node);
      } else {
        throw new AssertionError("unknown parent type: " + parent.getClass().getSimpleName());
      }
    }
    return true;
  }

  @Override
  public boolean visit(IfStatement node) {
    if (isGwtTest(node.getExpression())) {
      if (node.getElseStatement() != null) {
        // Replace this node with the else statement.
        ClassConverter.setProperty(node,
            NodeCopier.copySubtree(node.getAST(), node.getElseStatement()));
        node.getElseStatement().accept(this);
      } else {
        // No else statement, so remove this if statement or replace it
        // with an empty statement.
        ASTNode parent = node.getParent();
        if (parent instanceof Block) {
          @SuppressWarnings("unchecked")
          List<Statement> stmts = ((Block) parent).statements();
          stmts.remove(node);
        } else {
          ClassConverter.setProperty(node, node.getAST().newEmptyStatement());
        }
      }
    }
    return false;
  }

  @Override
  public boolean visit(MethodInvocation node) {
    IMethodBinding method = Types.getMethodBinding(node);
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments();
    if (method.getName().equals("create") &&
        method.getDeclaringClass().getQualifiedName().equals(GWT_CLASS) &&
        args.size() == 1) {
      // Convert GWT.create(Foo.class) to Foo.class.newInstance().
      AST ast = node.getAST();
      SimpleName name = ast.newSimpleName("newInstance");
      node.setName(name);
      Expression clazz = NodeCopier.copySubtree(ast, args.get(0));
      args.remove(0);
      node.setExpression(clazz);
      GeneratedMethodBinding newBinding = new GeneratedMethodBinding("newInstance", 0,
        ast.resolveWellKnownType("java.lang.Object"), ast.resolveWellKnownType("java.lang.Class"),
        false, false, false);
      Types.addBinding(name, newBinding);
      Types.addBinding(node, newBinding);
    } else if (isGwtTest(node)) {
      J2ObjC.error(node, "GWT.isScript() detected in boolean expression, which is not supported");
    }
    return true;
  }

  /**
   * Returns true if expression is a method invocation, and that
   * method refers to GWT.isClient() or GWT.isScript().
   *
   * NOTE: this method only checks for GWT.isClient() method invocations,
   * not any other boolean expression that might contain them.  For
   * example, !GWT.isClient() won't detect the method, since the binding
   * for that expression will be a boolean type rather than a method
   * binding.  The isGwtTest() call in visit(MethodInvocation) warns when
   * code like this is translated.
   */
  private boolean isGwtTest(Expression node) {
    IBinding binding = Types.getBinding(node);
    if (binding instanceof IMethodBinding) {
      IMethodBinding method = (IMethodBinding) binding;
      if (method.getDeclaringClass().getQualifiedName().equals(GWT_CLASS)) {
        String name = method.getName();
        return name.equals("isClient") || name.equals("isScript");
      }
    }
    return false;
  }

  private boolean hasAnnotation(Class<?> annotation, List<IExtendedModifier> modifiers) {
    // Annotation bindings don't have the annotation's package.
    String annotationName = annotation.getSimpleName();
    for (IExtendedModifier mod : modifiers) {
      if (mod.isAnnotation()) {
        Annotation annotationNode = (Annotation) mod;
        String modName = annotationNode.getTypeName().getFullyQualifiedName();
        if (modName.equals(annotationName)) {
          if (annotationName.equals("GwtIncompatible") &&
              annotationNode.isSingleMemberAnnotation()) {
            Expression value = ((SingleMemberAnnotation) annotationNode).getValue();
            if (value instanceof StringLiteral) {
              if (compatibleAPIs.contains(((StringLiteral) value).getLiteralValue())) {
                // Pretend incompatible annotation isn't present, since what it's
                // flagging is J2ObjC-compatible.
                return false;
              }
            }
          }
          return true;
        }
      }
    }
    return false;
  }
}
