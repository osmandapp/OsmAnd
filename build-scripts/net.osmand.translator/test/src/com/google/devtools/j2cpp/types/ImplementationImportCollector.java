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

import com.google.common.collect.Lists;
import com.google.devtools.j2cpp.util.NameTable;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.Iterator;
import java.util.List;

/**
 * Collects the set of imports needed to resolve type references in an
 * implementation (.m) file.
 *
 * @author Tom Ball
 */
public class ImplementationImportCollector extends HeaderImportCollector {
  private final List<Import> declaredTypes = Lists.newArrayList();

  public ImplementationImportCollector() {
    super(true);
  }

  @Override
  public void collect(CompilationUnit unit, String sourceFileName) {
    super.collect(unit, sourceFileName);
    getDeclaredTypes(unit);
    Iterator<Import> imports = getImports().iterator();
    while (imports.hasNext()) {
      Import imp = imports.next();
      if (declaredTypes.contains(imp)) {
        imports.remove();
      }
    }
  }

  // Keep track of any declared types to avoid invalid imports.  The
  // exception is the main type, as it's needed to import the matching
  // header file.
  private void getDeclaredTypes(ASTNode node) {
    node.accept(new ASTVisitor() {
      @Override
      public void endVisit(TypeDeclaration node) {
        declareType(node, mainTypeName);
      }

      @Override
      public void endVisit(EnumDeclaration node) {
        declareType(node, mainTypeName + "Enum");
      }

      private void declareType(AbstractTypeDeclaration type, String mainType) {
        ITypeBinding binding = Types.getTypeBinding(type);
        boolean isMain = NameTable.getFullName(binding).equals(mainType);
        if (!isMain) {
          declaredTypes.add(getReference(binding));
        }
      }
    });
  }

  @Override
  public boolean visit(ArrayAccess node) {
    addReference(Types.getTypeBinding(node));
    return super.visit(node);
  }

  @Override
  public boolean visit(Assignment node) {
    if (node.getOperator() == Operator.PLUS_ASSIGN &&
        Types.isJavaStringType(Types.getTypeBinding(node.getLeftHandSide())) &&
        Types.isBooleanType(Types.getTypeBinding(node.getRightHandSide()))) {
      // Implicit conversion from boolean -> String translates into a
      // Boolean.toString(...) call, so add a reference to java.lang.Boolean.
      addReference(node.getAST().resolveWellKnownType("java.lang.Boolean"));
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(ArrayType node) {
    addReference(Types.getTypeBinding(node.getComponentType()));
    return super.visit(node);
  }

  @Override
  public boolean visit(CastExpression node) {
    addReference(node.getType());
    return super.visit(node);
  }

  @Override
  public boolean visit(CatchClause node) {
    addReference(node.getException().getType());
    return super.visit(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    addReference(node.getType());
    IMethodBinding binding = Types.getMethodBinding(node);
    if (binding != null) {
      ITypeBinding[] parameterTypes = binding.getParameterTypes();
      for (int i = 0; i < node.arguments().size(); i++) {

        ITypeBinding parameterType;
        if (i < parameterTypes.length) {
          parameterType = parameterTypes[i];
        } else {
          parameterType = parameterTypes[parameterTypes.length - 1];
        }
        ITypeBinding actualType = Types.getTypeBinding(node.arguments().get(i));
        if (!parameterType.equals(actualType) &&
            actualType.isAssignmentCompatible(parameterType)) {
          addReference(actualType);
        }
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(TypeLiteral node) {
    addReference(node.getType());
    return super.visit(node);
  }

  @Override
  public boolean visit(EnhancedForStatement node) {
    addReference(node.getParameter().getType());
    addReference("JavaLangNullPointerException", "java.lang.NullPointerException", true);
    addReference("JavaUtilIterator", "java.util.Iterator", true);
    return super.visit(node);
  }

  @Override
  public boolean visit(EnumDeclaration node) {
    addReference(Types.getTypeBinding(node));
    addReference("JavaLangIllegalArgumentException", "java.lang.IllegalArgumentException", false);
    return true;
  }

  @Override
  public boolean visit(FieldAccess node) {
    addReference(Types.getTypeBinding(node.getName()));
    return true;
  }

  @Override
  public boolean visit(FieldDeclaration node) {
    // non-private references are gathered by the header file collector
    if (Modifier.isPrivate(node.getModifiers())) {
      addReference(node.getType());
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(InstanceofExpression node) {
    addReference(node.getRightOperand());
    return super.visit(node);
  }

  @Override
  public boolean visit(InfixExpression node) {
    if (Types.isJavaStringType(Types.getTypeBinding(node))) {
      boolean needsImport = false;
      if (Types.isBooleanType(Types.getTypeBinding(node.getLeftOperand())) ||
          Types.isBooleanType(Types.getTypeBinding(node.getRightOperand()))) {
        needsImport = true;
      } else {
        @SuppressWarnings("unchecked")
        List<Expression> extendedExpressions = node.extendedOperands();  // Safe by definition
        for (Expression extendedExpression : extendedExpressions) {
          if (Types.isBooleanType(Types.getTypeBinding(extendedExpression))) {
            needsImport = true;
            break;
          }
        }
      }

      if (needsImport) {
        // Implicit conversion from boolean -> String translates into a
        // Boolean.toString(...) call, so add a reference to java.lang.Boolean.
        addReference(node.getAST().resolveWellKnownType("java.lang.Boolean"));
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    addReference(node.getReturnType2());
    if (node.resolveBinding() != null && node.resolveBinding().isVarargs()) {
      addReference(Types.resolveIOSType("IOSObjectArray"));
    }
    for (Iterator<?> iterator = node.parameters().iterator(); iterator.hasNext(); ) {
      Object o = iterator.next();
      if (o instanceof SingleVariableDeclaration) {
        addReference(((SingleVariableDeclaration) o).getType());
      } else {
        throw new AssertionError("unknown AST type: " + o.getClass());
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    // Check for vararg method
    IMethodBinding binding = Types.getMethodBinding(node);
    if (binding != null && binding.isVarargs()) {
      addReference(Types.resolveIOSType("IOSObjectArray"));
    } else if (binding != null) {
      ITypeBinding[] parameterTypes = binding.getParameterTypes();
      for (int i = 0; i < parameterTypes.length; i++) {
        ITypeBinding parameterType = parameterTypes[i];
        ITypeBinding actualType = Types.getTypeBinding(node.arguments().get(i));
        if (!parameterType.equals(actualType) &&
            actualType.isAssignmentCompatible(parameterType)) {
          addReference(actualType);
        }
      }
    }
    // Check for static method references.
    Expression expr = node.getExpression();
    if (expr == null) {
      // check for method that's been statically imported
      if (binding != null) {
        ITypeBinding typeBinding = binding.getDeclaringClass();
        if (typeBinding != null) {
          addReference(typeBinding);
        }
      }
    } else {
      IMethodBinding receiver = Types.getMethodBinding(expr);
      if (receiver != null && !receiver.isConstructor()) {
        addReference(receiver.getReturnType());
      }
      if (receiver == null) {
        // Check for class variable or enum constant.
        IVariableBinding var = Types.getVariableBinding(expr);
        if (var == null || var.isEnumConstant()) {
          addReference(Types.getTypeBinding(expr));
        }
      }
    }
    while (expr != null && expr instanceof Name) {
      IMethodBinding methodBinding = Types.getMethodBinding(node);
      if (methodBinding instanceof IOSMethodBinding) {
        // true for mapped methods
        IMethodBinding resolvedBinding = Types.resolveInvocationBinding(node);
        if (resolvedBinding != null) {
          addReference(resolvedBinding.getDeclaringClass());
          break;
        }
      }
      addReference(methodBinding.getReturnType());
      ITypeBinding typeBinding = Types.getTypeBinding(expr);
      if (typeBinding != null && typeBinding.isClass()) { // if class literal
        addReference(typeBinding);
        break;
      }
      if (expr instanceof QualifiedName) {
        expr = ((QualifiedName)expr).getQualifier();
        if (expr.resolveTypeBinding() == null) {
          break;
        }
      } else {
        break;
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(QualifiedName node) {
    IBinding type = Types.getTypeBinding(node);
    if (type != null) {
      addReference((ITypeBinding) type);
    }
    return true;
  }

  @Override
  public boolean visit(SimpleName node) {
    IVariableBinding var = Types.getVariableBinding(node);
    if (var != null && Modifier.isStatic(var.getModifiers())) {
      ITypeBinding declaringClass = var.getDeclaringClass();
      addReference(declaringClass);
    }
    return true;
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    ITypeBinding type = Types.getTypeBinding(node);
    addReference(type);
    if (Types.isJUnitTest(type)) {
      addReference("JUnitRunner", "JUnitRunner", true);
    }
    return true;
  }

  @Override
  public boolean visit(VariableDeclarationExpression node) {
    Type type = node.getType();
    addReference(type);
    return super.visit(node);
  }

  @Override
  public boolean visit(VariableDeclarationStatement node) {
    addReference(node.getType());
    return super.visit(node);
  }

  @Override
  protected void addReference(ITypeBinding binding) {
    // There is similar code in gen/StatementGenerator, but here
    // recursion is used to tease out the references in nested
    // generic type declarations, which isn't needed when
    // generating statements.
    if (binding != null && !Types.isVoidType(binding) && !binding.isAnnotation()) {
      if (binding.isWildcardType()) {
        addReference(binding.getBound());
      } else if (binding.isCapture()) {
        addReference(binding.getWildcard());
      } else {
        assert (!binding.isCapture() && !binding.isWildcardType());
        super.addReference(binding);
      }
      for (ITypeBinding typeArg : binding.getTypeArguments()) {
        addReference(typeArg);
      }
    }
  }
}
