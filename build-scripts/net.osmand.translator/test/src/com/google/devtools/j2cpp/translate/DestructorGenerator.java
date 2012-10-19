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

import com.google.devtools.j2cpp.Options;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.List;

/**
 * Adds release methods to Java classes, in preparation for translation
 * to iOS.  Because Objective-C allows messages to be sent to nil, all
 * fields can be released regardless of whether they currently reference
 * data.
 *
 * @author Tom Ball
 */
public class DestructorGenerator extends ErrorReportingASTVisitor {
  private final String destructorName;

  public static final String FINALIZE_METHOD = "finalize";
  public static final String DEALLOC_METHOD = "dealloc";

  public DestructorGenerator() {
    destructorName = Options.useGC() ? FINALIZE_METHOD : DEALLOC_METHOD;
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    final List<IVariableBinding> releaseableFields = Lists.newArrayList();
    for (final FieldDeclaration field : node.getFields()) {
      if (!field.getType().isPrimitiveType() && !isStatic(field)) {
        ErrorReportingASTVisitor varFinder = new ErrorReportingASTVisitor() {
          @Override
          public boolean visit(VariableDeclarationFragment node) {
            IVariableBinding binding = Types.getVariableBinding(node);
            if (!Modifier.isStatic(field.getModifiers()) && !Types.isConstantVariable(binding)) {
              releaseableFields.add(binding);
            }
            return true;
          }
        };
        varFinder.run(field);
      }
    }
    if (!releaseableFields.isEmpty()) {
      Types.addReleaseableFields(releaseableFields);

      boolean foundDestructor = false;

      // If a destructor method already exists, append release statements.
      for (MethodDeclaration method : node.getMethods()) {
        if (FINALIZE_METHOD.equals(method.getName().getIdentifier())) {
          addReleaseStatements(method, releaseableFields);
          foundDestructor = true;
        }
      }

      // No destructor, so create a new one.
      if (!foundDestructor && !Options.useARC()) {
        MethodDeclaration finalizeMethod =
            buildFinalizeMethod(node.getAST(), Types.getTypeBinding(node), releaseableFields);
        @SuppressWarnings("unchecked")
        List<BodyDeclaration> declarations = node.bodyDeclarations();
        declarations.add(finalizeMethod);
      }
    }

    // Rename method to correct destructor name.  This is down outside of
    // the loop above, because a class may have a finalize() method but no
    // releasable fields.
    for (MethodDeclaration method : node.getMethods()) {
      if (needsRenaming(method.getName())) {
        NameTable.rename(Types.getBinding(method), destructorName);
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    if (needsRenaming(node.getName())) {
      NameTable.rename(Types.getBinding(node), destructorName);
    }
    return true;
  }

  @Override
  public boolean visit(SuperMethodInvocation node) {
    if (needsRenaming(node.getName())) {
      NameTable.rename(Types.getBinding(node), destructorName);
    }
    return true;
  }

  private boolean isStatic(FieldDeclaration f) {
    return (f.getModifiers() & Modifier.STATIC) != 0;
  }

  private boolean needsRenaming(SimpleName methodName) {
    return destructorName.equals(DEALLOC_METHOD) &&
        FINALIZE_METHOD.equals(methodName.getIdentifier());
  }

  @SuppressWarnings("unchecked")
  private void addReleaseStatements(MethodDeclaration method, List<IVariableBinding> fields) {
    // Find existing super.finalize(), if any.
    final boolean[] hasSuperFinalize = new boolean[1];
    method.accept(new ASTVisitor() {
      @Override
      public void endVisit(SuperMethodInvocation node) {
        if (FINALIZE_METHOD.equals(node.getName().getIdentifier())) {
          hasSuperFinalize[0] = true;
        }
      }
    });

    List<Statement> statements = method.getBody().statements(); // safe by definition
    if (!statements.isEmpty() && statements.get(0) instanceof TryStatement) {
      TryStatement tryStatement = ((TryStatement) statements.get(0));
      if (tryStatement.getBody() != null) {
        statements = tryStatement.getBody().statements(); // safe by definition
      }
    }
    AST ast = method.getAST();
    int index = statements.size();
    for (IVariableBinding field : fields) {
      if (!field.getType().isPrimitive() && !Types.isWeakReference(field)) {
        Assignment assign = ast.newAssignment();
        SimpleName receiver = ast.newSimpleName(field.getName());
        Types.addBinding(receiver, field);
        assign.setLeftHandSide(receiver);
        assign.setRightHandSide(Types.newNullLiteral());
        Types.addBinding(assign, field.getDeclaringClass());
        ExpressionStatement stmt = ast.newExpressionStatement(assign);
        statements.add(index, stmt);
      }
    }
    if (Options.useReferenceCounting() && !hasSuperFinalize[0]) {
      SuperMethodInvocation call = ast.newSuperMethodInvocation();
      IMethodBinding methodBinding = Types.getMethodBinding(method);
      GeneratedMethodBinding binding = new GeneratedMethodBinding(destructorName, Modifier.PUBLIC,
        Types.mapTypeName("void"), methodBinding.getDeclaringClass(), false, false, true);
      Types.addBinding(call, binding);
      call.setName(ast.newSimpleName(destructorName));
      Types.addBinding(call.getName(), binding);
      ExpressionStatement stmt = ast.newExpressionStatement(call);
      statements.add(stmt);
    }
  }

  private MethodDeclaration buildFinalizeMethod(AST ast, ITypeBinding declaringClass,
        List<IVariableBinding> fields) {
    ITypeBinding voidType = Types.mapTypeName("void");
    GeneratedMethodBinding binding = new GeneratedMethodBinding(destructorName, Modifier.PUBLIC,
      voidType, declaringClass, false, false, true);
    MethodDeclaration method = ast.newMethodDeclaration();
    Types.addBinding(method, binding);
    method.setName(ast.newSimpleName(destructorName));
    Types.addBinding(method.getName(), binding);
    @SuppressWarnings("unchecked")
    List<Modifier> modifiers = method.modifiers();
    modifiers.add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
    method.setBody(ast.newBlock());
    addReleaseStatements(method, fields);
    Type returnType = ast.newPrimitiveType(PrimitiveType.VOID);
    Types.addBinding(returnType, ast.resolveWellKnownType("void"));
    method.setReturnType2(returnType);
    return method;
  }
}
