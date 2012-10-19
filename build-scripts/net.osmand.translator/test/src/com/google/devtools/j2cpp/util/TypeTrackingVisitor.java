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

package com.google.devtools.j2cpp.util;

import com.google.devtools.j2cpp.types.Types;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Stack;

/**
 * AST visitor that tracks what type encloses the current AST node.  This
 * is more efficient than backtracking through each parent node, testing
 * its type.
 *
 * @author Tom Ball
 */
public abstract class TypeTrackingVisitor extends ErrorReportingASTVisitor {
  private final Stack<ASTNode> typeIndex = new Stack<ASTNode>();

  @Override
  public boolean visit(TypeDeclaration node) {
    typeIndex.push(node);
    return true;
  }

  @Override
  public boolean visit(EnumDeclaration node) {
    typeIndex.push(node);
    return true;
  }

  @Override
  public boolean visit(AnonymousClassDeclaration node) {
    typeIndex.push(node);
    return true;
  }

  @Override
  public boolean visit(AnnotationTypeDeclaration node) {
    typeIndex.push(node);
    return true;
  }

  @Override
  public void endVisit(AnnotationTypeDeclaration node) {
    typeIndex.pop();
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {
    typeIndex.pop();
  }

  @Override
  public void endVisit(EnumDeclaration node) {
    typeIndex.pop();
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    typeIndex.pop();
  }

  public ASTNode getCurrentTypeNode() {
    return typeIndex.peek();
  }

  public ITypeBinding getCurrentType() {
    return Types.getTypeBinding(getCurrentTypeNode());
  }
}
