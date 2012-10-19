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

import com.google.devtools.j2cpp.types.IOSMethodBinding;
import com.google.devtools.j2cpp.types.NodeCopier;
import com.google.devtools.j2cpp.types.Types;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.List;

/**
 * Adds support for boxing and unboxing numeric primitive values.
 *
 * @author Tom Ball
 */
public class Autoboxer extends ErrorReportingASTVisitor {
  private final AST ast;
  private static final String VALUE_METHOD = "Value";
  private static final String VALUEOF_METHOD = "valueOf";

  public Autoboxer(AST ast) {
    this.ast = ast;
  }

  /**
   * Convert a primitive type expression into a wrapped instance.  Each
   * wrapper class has a static valueOf factory method, so "expr" gets
   * translated to "Wrapper.valueOf(expr)".
   */
  private Expression box(Expression expr) {
    ITypeBinding binding = getBoxType(expr);
    ITypeBinding wrapperBinding = Types.getWrapperType(binding);
    if (wrapperBinding != null) {
      MethodInvocation invocation = ast.newMethodInvocation();
      SimpleName wrapperClass = ast.newSimpleName(wrapperBinding.getName());
      Types.addBinding(wrapperClass, wrapperBinding);
      invocation.setExpression(wrapperClass);
      IMethodBinding wrapperMethod = getWrapperMethod(wrapperBinding, binding);
      SimpleName methodName = ast.newSimpleName(VALUEOF_METHOD);
      Types.addBinding(methodName, wrapperMethod);
      invocation.setName(methodName);
      Types.addBinding(invocation, wrapperMethod);

      @SuppressWarnings("unchecked")
      List<Expression> args = invocation.arguments(); // safe by definition
      args.add(NodeCopier.copySubtree(ast, expr));
      return invocation;
    } else {
      return NodeCopier.copySubtree(ast, expr);
    }
  }

  /**
   * Convert a wrapper class instance to its primitive equivalent.  Each
   * wrapper class has a "classValue()" method, such as intValue() or
   * booleanValue().  This method therefore converts "expr" to
   * "expr.classValue()".
   */
  private Expression unbox(Expression expr) {
    ITypeBinding binding = getBoxType(expr);
    if (Types.getPrimitiveType(binding) != null) {
      IMethodBinding valueMethod = getValueMethod(binding);
      MethodInvocation invocation = ast.newMethodInvocation();
      invocation.setExpression(NodeCopier.copySubtree(ast, expr));
      SimpleName methodName = ast.newSimpleName(valueMethod.getName());
      Types.addBinding(methodName, valueMethod);
      invocation.setName(methodName);
      Types.addBinding(invocation, valueMethod);
      return invocation;
    } else {
      return NodeCopier.copySubtree(ast, expr);
    }
  }

  @Override
  public void endVisit(Assignment node) {
    Expression lhs = node.getLeftHandSide();
    ITypeBinding lhType = getBoxType(lhs);
    Expression rhs = node.getRightHandSide();
    ITypeBinding rhType = getBoxType(rhs);
    Assignment.Operator op = node.getOperator();
    if (op != Assignment.Operator.ASSIGN && !lhType.isPrimitive() &&
        !lhType.equals(node.getAST().resolveWellKnownType("java.lang.String"))) {
      // Not a simple assignment; need to break the <operation>-WITH_ASSIGN
      // assignment apart.
      node.setOperator(Assignment.Operator.ASSIGN);
      node.setRightHandSide(box(newInfixExpression(lhs, rhs, op, lhType)));
    } else {
      if (lhType.isPrimitive() && !rhType.isPrimitive()) {
        node.setRightHandSide(unbox(rhs));
      } else if (!lhType.isPrimitive() && rhType.isPrimitive()) {
        node.setRightHandSide(box(rhs));
      }
    }
  }

  private InfixExpression newInfixExpression(
      Expression lhs, Expression rhs, Assignment.Operator op, ITypeBinding lhType) {
    InfixExpression newRhs = ast.newInfixExpression();
    newRhs.setLeftOperand(unbox(lhs));
    newRhs.setRightOperand(unbox(rhs));
    InfixExpression.Operator infixOp;
    // op isn't an enum, so this can't be a switch.
    if (op == Assignment.Operator.PLUS_ASSIGN) {
      infixOp = InfixExpression.Operator.PLUS;
    } else if (op == Assignment.Operator.MINUS_ASSIGN) {
      infixOp = InfixExpression.Operator.MINUS;
    } else if (op == Assignment.Operator.TIMES_ASSIGN) {
      infixOp = InfixExpression.Operator.TIMES;
    } else if (op == Assignment.Operator.DIVIDE_ASSIGN) {
      infixOp = InfixExpression.Operator.DIVIDE;
    } else if (op == Assignment.Operator.BIT_AND_ASSIGN) {
      infixOp = InfixExpression.Operator.AND;
    } else if (op == Assignment.Operator.BIT_OR_ASSIGN) {
      infixOp = InfixExpression.Operator.OR;
    } else if (op == Assignment.Operator.BIT_XOR_ASSIGN) {
      infixOp = InfixExpression.Operator.XOR;
    } else if (op == Assignment.Operator.REMAINDER_ASSIGN) {
      infixOp = InfixExpression.Operator.REMAINDER;
    } else if (op == Assignment.Operator.LEFT_SHIFT_ASSIGN) {
      infixOp = InfixExpression.Operator.LEFT_SHIFT;
    } else if (op == Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN) {
      infixOp = InfixExpression.Operator.RIGHT_SHIFT_SIGNED;
    } else if (op == Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN) {
      infixOp = InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;
    } else {
      throw new IllegalArgumentException();
    }
    newRhs.setOperator(infixOp);
    Types.addBinding(newRhs, Types.getPrimitiveType(lhType));
    return newRhs;
  }

  @Override
  public void endVisit(ArrayInitializer node) {
    ITypeBinding type = Types.getTypeBinding(node).getElementType();
    @SuppressWarnings("unchecked")
    List<Expression> expressions = node.expressions(); // safe by definition
    for (int i = 0; i < expressions.size(); i++) {
      Expression expr = expressions.get(i);
      Expression result = boxOrUnboxExpression(expr, type);
      if (expr != result) {
        expressions.set(i, result);
      }
    }
  }

  @Override
  public void endVisit(ClassInstanceCreation node) {
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by definition
    convertArguments(Types.getMethodBinding(node), args);
  }

  @Override
  public void endVisit(ConstructorInvocation node) {
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by definition
    convertArguments(Types.getMethodBinding(node), args);
  }

  @Override
  public void endVisit(EnumConstantDeclaration node) {
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by definition
    if (!args.isEmpty()) {
      IMethodBinding constructor = Types.getMethodBinding(node);
      int n = args.size();
      for (int i = 0; i < n; i++) {
        Expression arg = args.get(i);
        ITypeBinding parameterType = constructor.getParameterTypes()[i];
        boolean argumentIsPrimitive = getBoxType(arg).isPrimitive();
        boolean parameterIsPrimitive = parameterType.isPrimitive();
        if (argumentIsPrimitive && !parameterIsPrimitive) {
          args.set(i, box(arg));
        } else if (parameterIsPrimitive && !argumentIsPrimitive) {
          args.set(i, unbox(arg));
        }
      }
    }
  }

  @Override
  public void endVisit(InfixExpression node) {
    Expression lhs = node.getLeftOperand();
    ITypeBinding lhBinding = getBoxType(lhs);
    Expression rhs = node.getRightOperand();
    ITypeBinding rhBinding = getBoxType(rhs);

    // Unless the operator is == or !=, both operands must be primitive.
    InfixExpression.Operator op = node.getOperator();
    boolean needsPrimitive =
        op != InfixExpression.Operator.EQUALS && op != InfixExpression.Operator.NOT_EQUALS;

    if (!lhBinding.isPrimitive() && (rhBinding.isPrimitive() || needsPrimitive)) {
      node.setLeftOperand(unbox(lhs));
    }
    if ((lhBinding.isPrimitive() || needsPrimitive) && !rhBinding.isPrimitive()) {
      node.setRightOperand(unbox(rhs));
    }
  }

  @Override
  public void endVisit(MethodInvocation node) {
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by definition
    convertArguments(Types.getMethodBinding(node), args);
  }

  @Override
  public void endVisit(ReturnStatement node) {
    Expression expr = node.getExpression();
    if (expr != null) {
      ASTNode n = node.getParent();
      while (!(n instanceof MethodDeclaration)) {
        n = n.getParent();
      }
      ITypeBinding returnType = Types.getMethodBinding(n).getReturnType();
      ITypeBinding exprType = getBoxType(expr);
      if (returnType.isPrimitive() && !exprType.isPrimitive()) {
        node.setExpression(unbox(expr));
      }
      if (!returnType.isPrimitive() && exprType.isPrimitive()) {
        node.setExpression(box(expr));
      }
    }
  }

  @Override
  public void endVisit(SuperConstructorInvocation node) {
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by definition
    convertArguments(Types.getMethodBinding(node), args);
  }

  @Override
  public void endVisit(TypeLiteral node) {
    ITypeBinding binding = Types.getTypeBinding(node.getType());
    if (binding.isPrimitive() && !Types.isVoidType(binding)) {
      Type boxedType = Types.makeType(Types.getWrapperType(binding));
      node.setType(boxedType);
    }
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    Expression initializer = node.getInitializer();
    if (initializer != null) {
      ITypeBinding nodeType = getBoxType(node);
      ITypeBinding initType = getBoxType(initializer);
      if (nodeType.isPrimitive() && !initType.isPrimitive()) {
        node.setInitializer(unbox(initializer));
      } else if (!nodeType.isPrimitive() && initType.isPrimitive()) {
        node.setInitializer(box(initializer));
      }
    }
  }

  @Override
  public void endVisit(ConditionalExpression node) {
    ITypeBinding nodeType = getBoxType(node);

    Expression thenExpr = node.getThenExpression();
    ITypeBinding thenType = getBoxType(thenExpr);

    Expression elseExpr = node.getElseExpression();
    ITypeBinding elseType = getBoxType(elseExpr);

    if (thenType.isPrimitive() && !nodeType.isPrimitive()) {
      node.setThenExpression(box(thenExpr));
    } else if (!thenType.isPrimitive() && nodeType.isPrimitive()) {
      node.setThenExpression(unbox(thenExpr));
    }

    if (elseType.isPrimitive() && !nodeType.isPrimitive()) {
      node.setElseExpression(box(elseExpr));
    } else if (!elseType.isPrimitive() && nodeType.isPrimitive()) {
      node.setElseExpression(unbox(elseExpr));
    }
  }

  /**
   * Return the type to be checked for boxing or unboxing.
   */
  private ITypeBinding getBoxType(ASTNode node) {
    IBinding binding = Types.getBinding(node);
    if (binding instanceof ITypeBinding) {
      return (ITypeBinding) binding;
    }
    if (binding instanceof IVariableBinding) {
      return ((IVariableBinding) binding).getType();
    }
    if (binding instanceof IMethodBinding) {
      IMethodBinding method = (IMethodBinding) binding;
      return method.isConstructor() ? method.getDeclaringClass() : method.getReturnType();
    }
    throw new AssertionError("unknown box type");
  }

  private IMethodBinding getWrapperMethod(ITypeBinding wrapperClass, ITypeBinding primitiveType) {
    for (IMethodBinding method : wrapperClass.getDeclaredMethods()) {
      if (method.getName().equals(VALUEOF_METHOD)
          && method.getParameterTypes()[0].isEqualTo(primitiveType)) {
        return method;
      }
    }
    throw new AssertionError("could not find valueOf method for " + wrapperClass);
  }

  private IMethodBinding getValueMethod(ITypeBinding type) {
    ITypeBinding primitiveType = Types.getPrimitiveType(type);
    assert primitiveType != null;
    String methodName = primitiveType.getName() + VALUE_METHOD;
    for (IMethodBinding method : type.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    throw new AssertionError("could not find value method for " + type);
  }

  private void convertArguments(IMethodBinding methodBinding, List<Expression> args) {
    if (methodBinding instanceof IOSMethodBinding) {
      return; // already converted
    }

    ITypeBinding[] argTypes = methodBinding.getParameterTypes();
    if (methodBinding.isVarargs()) {
      ITypeBinding[] paramTypes = methodBinding.getParameterTypes();
      int explicitArgs = paramTypes.length - 1;  // the last arg is the varargs array

      for (int i = 0; i < args.size(); i++) {
        Expression arg = args.get(i);
        if (i < explicitArgs) {
          // Only box/unbox explicit args if necessary.
          Expression replacementArg = boxOrUnboxExpression(arg, argTypes[i]);
          if (replacementArg != arg) {
            args.set(i,  replacementArg);
          }
        } else {
          // Always box varargs, since they are passed as an object array.
          ITypeBinding argBinding = getBoxType(arg);
          if (argBinding.isPrimitive()) {
            args.set(i, box(arg));
          }
        }
      }
    } else {
      for (int i = 0; i < argTypes.length; i++) {
        Expression arg = args.get(i);
        Expression replacementArg = boxOrUnboxExpression(arg, argTypes[i]);
        if (replacementArg != arg) {
          args.set(i,  replacementArg);
        }
      }
    }
  }

  private Expression boxOrUnboxExpression(Expression arg, ITypeBinding argType) {
    ITypeBinding argBinding;
    IBinding binding = Types.getBinding(arg);
    if (binding instanceof IMethodBinding) {
      argBinding = ((IMethodBinding) binding).getReturnType();
    } else {
      argBinding = getBoxType(arg);
    }
    if (argType.isPrimitive() && !argBinding.isPrimitive()) {
      return unbox(arg);
    } else if (!argType.isPrimitive() && argBinding.isPrimitive()) {
      return box(arg);
    } else {
      return arg;
    }
  }
}
