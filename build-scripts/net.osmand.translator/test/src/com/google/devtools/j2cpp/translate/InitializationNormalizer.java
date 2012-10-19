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

import com.google.common.collect.Lists;

import com.google.devtools.j2cpp.sym.Symbols;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.NodeCopier;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.Iterator;
import java.util.List;

/**
 * Modifies initializers to be more iOS like.  Static initializers are
 * combined into a static initialize method, instance initializer
 * statements are injected into constructors.  If a class doesn't have
 * any constructors but does have instance initialization statements,
 * a default constructor is added to run them.
 *
 * @author Tom Ball
 */
public class InitializationNormalizer extends ErrorReportingASTVisitor {

  public static final String INIT_NAME = "init";

  @Override
  public void endVisit(TypeDeclaration node) {
    normalizeMembers(node);
    super.endVisit(node);
  }

  @Override
  public void endVisit(EnumDeclaration node) {
    normalizeMembers(node);
    super.endVisit(node);
  }

  void normalizeMembers(AbstractTypeDeclaration node) {
    List<Statement> initStatements = Lists.newArrayList();
    List<Statement> classInitStatements = Lists.newArrayList();
    List<MethodDeclaration> methods = Lists.newArrayList();
    ITypeBinding binding = Types.getTypeBinding(node);

    // Scan class, gathering initialization statements in declaration order.
    @SuppressWarnings("unchecked")
    List<BodyDeclaration> members = node.bodyDeclarations(); // safe by specification
    Iterator<BodyDeclaration> iterator = members.iterator();
    while (iterator.hasNext()) {
      BodyDeclaration member = iterator.next();
      switch (member.getNodeType()) {
        case ASTNode.ENUM_DECLARATION:
        case ASTNode.TYPE_DECLARATION:
          normalizeMembers((AbstractTypeDeclaration) member);
          break;
        case ASTNode.METHOD_DECLARATION:
          methods.add((MethodDeclaration) member);
          break;
        case ASTNode.INITIALIZER:
          addInitializer(member, initStatements, classInitStatements);
          iterator.remove();
          break;
        case ASTNode.FIELD_DECLARATION:
          if (!binding.isInterface()) { // All interface fields are constants.
            addFieldInitializer(member, initStatements, classInitStatements);
          }
          break;
      }
    }

    // Update any primary constructors with init statements.
    if (!initStatements.isEmpty() || binding.isEnum()) {
      boolean needsConstructor = true;
      for (MethodDeclaration md : methods) {
        if (md.isConstructor()) {
          needsConstructor = false;
        }
        normalizeMethod(md, initStatements);
      }
      if (needsConstructor) {
        addDefaultConstructor(binding, members, initStatements, node.getAST());
      }
    }

    // Create an initialize method, if necessary.
    if (!classInitStatements.isEmpty()) {
      addClassInitializer(binding, members, classInitStatements, node.getAST());
    }
  }

  /**
   * Add a static or instance init block's statements to the appropriate list
   * of initialization statements.
   */
  private void addInitializer(BodyDeclaration member, List<Statement> initStatements,
      List<Statement> classInitStatements) {
    Initializer initializer = (Initializer) member;
    List<Statement> l =
        Modifier.isStatic(initializer.getModifiers()) ? classInitStatements : initStatements;
    @SuppressWarnings("unchecked")
    List<Statement> stmts = initializer.getBody().statements(); // safe by specification
    l.addAll(stmts);
  }

  /**
   * Strip field initializers, convert them to assignment statements, and
   * add them to the appropriate list of initialization statements.
   */
  private void addFieldInitializer(BodyDeclaration member, List<Statement> initStatements,
      List<Statement> classInitStatements) {
    FieldDeclaration field = (FieldDeclaration) member;
    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> fragments = field.fragments(); // safe by specification
    for (VariableDeclarationFragment frag : fragments) {
      if (frag.getInitializer() != null) {
        Statement assignStmt = makeAssignmentStatement(frag);
        if (Modifier.isStatic(field.getModifiers())) {
          IVariableBinding binding = Types.getVariableBinding(frag);
          if (binding.getConstantValue() == null) { // constants don't need initialization
            classInitStatements.add(assignStmt);
            frag.setInitializer(null);
          }
        } else {
          // always initialize instance variables, since they can't be constants
          initStatements.add(assignStmt);
          frag.setInitializer(null);
        }
      }
    }
  }

  private ExpressionStatement makeAssignmentStatement(VariableDeclarationFragment fragment) {
    AST ast = fragment.getAST();
    IVariableBinding varBinding = Types.getVariableBinding(fragment);
    Assignment assignment = ast.newAssignment();
    Types.addBinding(assignment, varBinding.getType());
    Expression lhs = ast.newSimpleName(fragment.getName().getIdentifier());
    Types.addBinding(lhs, varBinding);
    assignment.setLeftHandSide(lhs);

    Expression initializer = fragment.getInitializer();
    if (initializer instanceof ArrayInitializer) {
      // An array initializer cannot be directly assigned, since by itself
      // it's just shorthand for an array creation node.  This therefore
      // builds an array creation node with the existing initializer.
      ArrayCreation arrayCreation = ast.newArrayCreation();
      ITypeBinding arrayType = varBinding.getType();
      Types.addBinding(arrayCreation, arrayType);
      Type newType = Types.makeIOSType(arrayType);
      assert newType != null;
      ArrayType newArrayType = ast.newArrayType(newType);
      Types.addBinding(newArrayType, arrayType);
      arrayCreation.setType(newArrayType);
      arrayCreation.setInitializer((ArrayInitializer) NodeCopier.copySubtree(ast, initializer));
      assignment.setRightHandSide(arrayCreation);
    } else {
      assignment.setRightHandSide(NodeCopier.copySubtree(ast, initializer));
    }
    return ast.newExpressionStatement(assignment);
  }

  /**
   * Insert initialization statements into "primary" constructors.  A
   * "primary" construction is defined here as a constructor that doesn't
   * call other constructors in this class, and is similar in concept to
   * Objective-C's "designated initializers."
   *
   * @return true if constructor was normalized
   */
  void normalizeMethod(MethodDeclaration node, List<Statement> initStatements) {
    if (isDesignatedConstructor(node)) {
      @SuppressWarnings("unchecked")
      List<Statement> stmts = node.getBody().statements(); // safe by specification

      // Insert initializer statements after the super invocation. If there
      // isn't a super invocation, add one (like all Java compilers do).
      if (stmts.isEmpty() || !(stmts.get(0) instanceof SuperConstructorInvocation)) {
        SuperConstructorInvocation superCall = node.getAST().newSuperConstructorInvocation();
        ITypeBinding superType = Types.getTypeBinding(node).getSuperclass();
        GeneratedMethodBinding newBinding = new GeneratedMethodBinding(INIT_NAME, Modifier.PUBLIC,
            node.getAST().resolveWellKnownType("void"), superType, true, false, true);
        Types.addBinding(superCall, newBinding);
        stmts.add(0, superCall);
      }
      List<Statement> unparentedStmts =
          NodeCopier.copySubtrees(node.getAST(), initStatements); // safe by specification
      stmts.addAll(1, unparentedStmts);
    }
  }

  /**
   * Returns true if this is a constructor that doesn't doesn't call
   * "this(...)".  This constructors are skipped so initializers
   * aren't run more than once per instance creation.
   */
  boolean isDesignatedConstructor(MethodDeclaration node) {
    if (!node.isConstructor()) {
      return false;
    }
    Block body = node.getBody();
    if (body == null) {
      return false;
    }
    @SuppressWarnings("unchecked")
    List<Statement> stmts = body.statements(); // safe by specification
    if (stmts.isEmpty()) {
      return true;
    }
    Statement firstStmt = stmts.get(0);
    return !(firstStmt instanceof ConstructorInvocation);
  }

  void addDefaultConstructor(ITypeBinding type, List<BodyDeclaration> members,
      List<Statement> initStatements, AST ast) {
    SuperConstructorInvocation superCall = ast.newSuperConstructorInvocation();
    GeneratedMethodBinding binding = new GeneratedMethodBinding(
        INIT_NAME, Modifier.PUBLIC, null, type.getSuperclass(), true, false, true);
    Types.addBinding(superCall, binding);
    initStatements.add(0, superCall);
    addMethod(INIT_NAME, Modifier.PUBLIC, type, initStatements, members, ast, true);
  }

  void addClassInitializer(ITypeBinding type, List<BodyDeclaration> members,
      List<Statement> classInitStatements, AST ast) {
    int modifiers = Modifier.PUBLIC | Modifier.STATIC;
    addMethod(NameTable.CLINIT_NAME, modifiers, type, classInitStatements, members, ast, false);
  }

  @SuppressWarnings("unchecked")
  private void addMethod(String name, int modifiers, ITypeBinding type,
      List<Statement> initStatements, List<BodyDeclaration> members, AST ast,
      boolean isConstructor) {
    Block body = ast.newBlock();
    List<Statement> stmts = body.statements();  // safe by definition
    for (Statement stmt : initStatements) {
      Statement newStmt = NodeCopier.copySubtree(ast, stmt);
      stmts.add(newStmt);
    }
    MethodDeclaration method = ast.newMethodDeclaration();
    GeneratedMethodBinding binding =
        new GeneratedMethodBinding(name, modifiers, null, type, isConstructor, false, true);
    Types.addBinding(method, binding);
    Type returnType = ast.newPrimitiveType(PrimitiveType.VOID);
    Types.addBinding(returnType, ast.resolveWellKnownType("void"));
    method.setReturnType2(returnType);
    method.setBody(body);
    method.setConstructor(isConstructor);
    method.modifiers().addAll(ast.newModifiers(modifiers));
    SimpleName nameNode = NameTable.unsafeSimpleName(name, ast);
    Types.addBinding(nameNode, binding);
    method.setName(nameNode);
    members.add(method);
    Symbols.resolve(method, binding);
  }
}
