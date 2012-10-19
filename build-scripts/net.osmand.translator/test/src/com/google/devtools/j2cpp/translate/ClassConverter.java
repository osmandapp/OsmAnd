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
import com.google.common.collect.Sets;

import com.google.devtools.j2cpp.sym.Symbols;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.GeneratedVariableBinding;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.TypeTrackingVisitor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.List;
import java.util.Set;

/**
 * Base class for the anonymous class converter and inner class extractor,
 * containing shared methods.
 *
 * @author Tom Ball
 */
public abstract class ClassConverter extends TypeTrackingVisitor {
  protected final CompilationUnit unit;
  private final Set<IMethodBinding> updatedConstructors = Sets.newLinkedHashSet();

  protected ClassConverter(CompilationUnit unit) {
    this.unit = unit;
  }

  /**
   * Returns a list of inner variables that need to be added to an inner type,
   * to resolve its references to outer classes.
   */
  protected List<IVariableBinding> getInnerVars(List<ReferenceDescription> references) {
    List<IVariableBinding> innerVars = Lists.newArrayList();
    outer: for (ReferenceDescription desc : references) {
      ITypeBinding declaringClass = desc.declaringClass;
      if (declaringClass == null) {
        declaringClass = desc.binding.getType();
      }
      declaringClass = declaringClass.getTypeDeclaration();
      if (desc.binding.isField()) {
        // Combine references to a type and its supertypes.
        for (int i = 0; i < innerVars.size(); i++) {
          IVariableBinding var = innerVars.get(i);
          ITypeBinding varType = var.getDeclaringClass();
          if (varType != null && varType.isAssignmentCompatible(declaringClass)) {
            desc.declaringClass = varType;
            continue outer;
          } else if (varType == null) {
            desc.declaringMethod = var.getDeclaringMethod();
          }
        }
      }
      if (!innerVars.contains(desc.binding)) {
        innerVars.add(desc.binding);
      }
    }
    return innerVars;
  }

  protected FieldDeclaration createField(String name, ITypeBinding varType,
       ITypeBinding declaringClass, AST ast) {
    VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
    SimpleName fieldName = ast.newSimpleName(name);
    GeneratedVariableBinding fieldBinding = new GeneratedVariableBinding(
        fieldName.getIdentifier(), Modifier.PRIVATE | Modifier.FINAL, varType,
        true, false, declaringClass, null);
    Types.addBinding(fieldName, fieldBinding);
    fragment.setName(fieldName);
    Types.addBinding(fragment, fieldBinding);

    FieldDeclaration field = ast.newFieldDeclaration(fragment);
    field.setType(Types.makeType(varType));
    @SuppressWarnings("unchecked")
    List<IExtendedModifier> mods = field.modifiers(); // safe by definition
    mods.add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
    mods.add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
    return field;
  }

  @SuppressWarnings("unchecked")
  protected List<SingleVariableDeclaration> createConstructorArguments(
      List<IVariableBinding> innerFields, IMethodBinding constructor, AST ast, String prefix) {
    int nameOffset = constructor.getParameterTypes().length;
    List<SingleVariableDeclaration> args = Lists.newArrayList();
    for (int i = 0; i < innerFields.size(); i++) {
      IVariableBinding field = innerFields.get(i);
      String argName = prefix + (i + nameOffset);
      SimpleName name = ast.newSimpleName(argName);
      GeneratedVariableBinding binding = new GeneratedVariableBinding(
          argName, Modifier.FINAL, field.getType(), false, true, constructor.getDeclaringClass(),
          constructor);
      Types.addBinding(name, binding);
      SingleVariableDeclaration newArg = ast.newSingleVariableDeclaration();
      newArg.setName(name);
      newArg.setType(Types.makeType(field.getType()));
      newArg.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
      Types.addBinding(newArg, binding);
      args.add(newArg);
    }
    return args;
  }

  protected void addInnerParameters(MethodDeclaration constructor,
      GeneratedMethodBinding binding, List<IVariableBinding> innerFields, AST ast,
      boolean methodVars) {
    // Add parameters and initializers for each field.
    @SuppressWarnings("unchecked") // safe by definition
    List<SingleVariableDeclaration> parameters = constructor.parameters();
    List<SingleVariableDeclaration> newParams = createConstructorArguments(innerFields,
        Types.getMethodBinding(constructor), ast, "outer$");
    Block body = constructor.getBody();
    @SuppressWarnings("unchecked") // safe by definition
    List<Statement> statements = body.statements();
    Statement first = statements.size() > 0 ? statements.get(0) : null;
    int offset =
        first != null &&
        (first instanceof SuperConstructorInvocation ||  first instanceof ConstructorInvocation)
        ? 1 : 0;
    boolean firstIsThisCall = first != null && first instanceof ConstructorInvocation;

    // If superclass constructor takes an outer$ parameter, create or edit
    // an invocation for it first
    if (!innerFields.isEmpty() && !methodVars) {
      if (firstIsThisCall) {
        ConstructorInvocation thisCall = (ConstructorInvocation) first;
        IMethodBinding cons = Types.getMethodBinding(thisCall);
        GeneratedMethodBinding newCons =
            new GeneratedMethodBinding(cons.getMethodDeclaration());
        // Create a new this invocation to the updated constructor.
        @SuppressWarnings("unchecked")
        List<Expression> args = ((ConstructorInvocation) first).arguments();
        int index = 0;
        for (SingleVariableDeclaration param : newParams) {
          IVariableBinding paramBinding = Types.getVariableBinding(param);
          newCons.addParameter(index, Types.getTypeBinding(param));
          args.add(index++, makeFieldRef(paramBinding, ast));
        }
        Types.addBinding(thisCall, newCons);
      } else {
        ITypeBinding superType = binding.getDeclaringClass().getSuperclass().getTypeDeclaration();
        if ((superType.getDeclaringClass() != null || superType.getDeclaringMethod() != null) &&
            (superType.getModifiers() & Modifier.STATIC) == 0) {

          // There may be more than one outer var supplied, find the right one.
          IVariableBinding outerVar = null;
          for (SingleVariableDeclaration param : newParams) {
            IVariableBinding paramBinding = Types.getVariableBinding(param);
            if (paramBinding.getType().isAssignmentCompatible(superType.getDeclaringClass())) {
              outerVar = paramBinding;
            }
          }
          assert outerVar != null;

          IMethodBinding cons = null;
          if (offset > 0) {
            cons = Types.getMethodBinding(statements.get(0));
          } else {
            for (IMethodBinding method : superType.getDeclaredMethods()) {
              // The super class's constructor may or may not have been already
              // modified.
              if (method.isConstructor()) {
                if (method.getParameterTypes().length == 0) {
                  cons = method;
                  break;
                } else if (method.getParameterTypes().length == 1 &&
                    outerVar.getType().isAssignmentCompatible(method.getParameterTypes()[0]) &&
                    method instanceof GeneratedMethodBinding) {
                  cons = method;
                  break;
                }
              }
            }
          }

          assert cons != null;

          if (!updatedConstructors.contains(cons)) {
            GeneratedMethodBinding newSuperCons =
                new GeneratedMethodBinding(cons.getMethodDeclaration());
            newSuperCons.addParameter(0, superType.getDeclaringClass());
            cons = newSuperCons;
          }

          SimpleName outerRef = makeFieldRef(outerVar, ast);
          SuperConstructorInvocation superInvocation = offset == 0 ?
              ast.newSuperConstructorInvocation() :
              (SuperConstructorInvocation) statements.get(0);
          @SuppressWarnings("unchecked")
          List<Expression> args = superInvocation.arguments();  // safe by definition
          args.add(0, outerRef);
          if (offset == 0) {
            statements.add(0, superInvocation);
            offset = 1;
          }
          Types.addBinding(superInvocation, cons);
        }
      }
    }

    for (int i = 0; i < newParams.size(); i++) {
      SingleVariableDeclaration parameter = newParams.get(i);

      // Only add an assignment statement for fields.
      if (innerFields.get(i).isField()) {
        statements.add(i + offset, createAssignment(innerFields.get(i),
            Types.getVariableBinding(parameter), ast));
      }

      // Add methodVars at the end of the method invocation.
      if (methodVars) {
        parameters.add(parameter);
        binding.addParameter(Types.getVariableBinding(parameter));
      } else {
        parameters.add(i, parameter);
        binding.addParameter(i, Types.getVariableBinding(parameter));
      }
    }

    Symbols.scanAST(constructor);
    updatedConstructors.add(binding);
    assert constructor.parameters().size() == binding.getParameterTypes().length;
  }

  protected Statement createAssignment(IVariableBinding field, IVariableBinding param, AST ast) {
    SimpleName fieldName = ast.newSimpleName(field.getName());
    Types.addBinding(fieldName, field);
    SimpleName paramName = ast.newSimpleName(param.getName());
    Types.addBinding(paramName, param);
    Assignment assign = ast.newAssignment();
    assign.setLeftHandSide(fieldName);
    assign.setRightHandSide(paramName);
    Types.addBinding(assign, field.getType());
    return ast.newExpressionStatement(assign);
  }

  protected SimpleName makeFieldRef(IVariableBinding newVar, AST ast) {
    SimpleName fieldRef = ast.newSimpleName(newVar.getName());
    Types.addBinding(fieldRef, newVar);
    return fieldRef;
  }

  @SuppressWarnings("unchecked")
  public static void setProperty(ASTNode node, Expression expr) {
    ASTNode parent = node.getParent();
    StructuralPropertyDescriptor locator = node.getLocationInParent();
    if (locator instanceof ChildPropertyDescriptor) {
      parent.setStructuralProperty(locator, expr);
    } else {
      // JDT doesn't directly support ChildListProperty replacement.
      List<Expression> args;
      if (parent instanceof MethodInvocation) {
        args = ((MethodInvocation) parent).arguments();
      } else if (parent instanceof ClassInstanceCreation) {
        args = ((ClassInstanceCreation) parent).arguments();
      } else if (parent instanceof InfixExpression) {
        args = ((InfixExpression) parent).extendedOperands();
      } else if (parent instanceof SynchronizedStatement) {
        SynchronizedStatement stmt = (SynchronizedStatement) parent;
        if (node.equals(stmt.getExpression())) {
          stmt.setExpression((Expression) node);
        }
        return;
      } else if (parent instanceof SuperConstructorInvocation) {
        args = ((SuperConstructorInvocation) parent).arguments();
      } else if (parent instanceof ArrayCreation) {
        args = ((ArrayCreation) parent).dimensions();
      } else {
        throw new AssertionError("unknown parent node type: " + parent.getClass().getSimpleName());
      }
      for (int i = 0; i < args.size(); i++) {
        if (node.equals(args.get(i))) {
          args.set(i, expr);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static void setProperty(ASTNode node, Statement stmt) {
    ASTNode parent = node.getParent();
    StructuralPropertyDescriptor locator = node.getLocationInParent();
    if (locator instanceof ChildPropertyDescriptor) {
      parent.setStructuralProperty(locator, stmt);
    } else {
      // JDT doesn't directly support ChildListProperty replacement.
      List<Statement> args;
      if (parent instanceof Block) {
        args = ((Block) parent).statements();
      } else {
        throw new AssertionError("unknown parent node type: " + parent.getClass().getSimpleName());
      }
      for (int i = 0; i < args.size(); i++) {
        if (node.equals(args.get(i))) {
          args.set(i, stmt);
        }
      }
    }
  }
}
