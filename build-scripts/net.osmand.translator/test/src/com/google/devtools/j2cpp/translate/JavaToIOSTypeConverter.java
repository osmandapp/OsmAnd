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

import com.google.devtools.j2cpp.types.GeneratedVariableBinding;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.Iterator;
import java.util.List;

/**
 * ObjectiveCTypeConverter: converts code that references core Java types to
 * similar iOS Foundation types. For example, Object maps to NSObject, and
 * String to NSString. Arrays are also converted, but because their contents are
 * fixed-size and contain nulls, custom classes are used.
 *
 * @author Tom Ball
 */
public class JavaToIOSTypeConverter extends ErrorReportingASTVisitor {

  @Override
  public boolean visit(TypeDeclaration node) {
    ITypeBinding binding = Types.getTypeBinding(node);
    assert binding == Types.mapType(binding); // don't try to translate the
                                                 // types being mapped
    Type superClass = node.getSuperclassType();
    if (!node.isInterface()) {
      if (superClass == null) {
        node.setSuperclassType(Types.makeType(Types.getNSObject()));
      } else {
        binding = Types.getTypeBinding(superClass);
        if (Types.hasIOSEquivalent(binding)) {
          ITypeBinding newBinding = Types.mapType(binding);
          node.setSuperclassType(Types.makeType(newBinding));
        }
      }
    }
    @SuppressWarnings("unchecked")
    List<Type> interfaces = node.superInterfaceTypes(); // safe by definition
    for (int i = 0; i < interfaces.size(); i++) {
      Type intrface = interfaces.get(i);
      binding = Types.getTypeBinding(intrface);
      if (Types.hasIOSEquivalent(binding)) {
        ITypeBinding newBinding = Types.mapType(binding);
        interfaces.set(i, Types.makeType(newBinding));
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    IMethodBinding binding = Types.getMethodBinding(node);
    ITypeBinding returnBinding = binding.getReturnType();
    ITypeBinding newBinding = Types.mapType(returnBinding);
    if (returnBinding != newBinding) {
      node.setReturnType2(Types.makeType(newBinding));
    }

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> parameters = node.parameters();
    for (Iterator<SingleVariableDeclaration> iterator = parameters.iterator();
        iterator.hasNext();) {
      SingleVariableDeclaration parameter = iterator.next();
      Type type = parameter.getType();
      ITypeBinding varBinding = type.resolveBinding();
      if (varBinding != null) { // true for primitive types
        newBinding = Types.mapType(varBinding);
        if (varBinding != newBinding) {
          parameter.setType(Types.makeType(newBinding));
        }
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldDeclaration node) {
    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> vars = node.fragments(); // safe by definition
    for (VariableDeclarationFragment var : vars) {
      IVariableBinding binding = Types.getVariableBinding(var);
      Type newType = Types.makeIOSType(binding.getType());
      if (newType != null) {
        ITypeBinding newTypeBinding = Types.getTypeBinding(newType);
        GeneratedVariableBinding varBinding = new GeneratedVariableBinding(
            NameTable.getName(binding), binding.getModifiers(), newTypeBinding, true, false,
            binding.getDeclaringClass(), binding.getDeclaringMethod());
        Types.addMappedVariable(var, varBinding);
        node.setType(newType);
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    Expression receiver = node.getExpression();
    if (receiver instanceof SimpleName) {
      String name = ((SimpleName) receiver).getIdentifier();
      String newName = Types.mapSimpleTypeName(name);
      if (name != newName) { // identity test
        SimpleName nameNode = node.getAST().newSimpleName(newName);
        Types.addBinding(nameNode, Types.getBinding(node));
        node.setExpression(nameNode);
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(SingleVariableDeclaration node) {
    ITypeBinding binding = Types.getTypeBinding(node);
    Type newType = Types.makeIOSType(binding);
    if (newType != null) {
      node.setType(newType);
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(VariableDeclarationStatement node) {
    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> vars = node.fragments(); // safe by definition
    for (VariableDeclarationFragment var : vars) {
      IVariableBinding binding = Types.getVariableBinding(var);
      Type newType = Types.makeIOSType(binding.getType());
      if (newType != null) {
        ITypeBinding newTypeBinding = Types.getTypeBinding(newType);
        GeneratedVariableBinding varBinding = new GeneratedVariableBinding(
            NameTable.getName(binding), binding.getModifiers(), newTypeBinding, false, false,
            binding.getDeclaringClass(), binding.getDeclaringMethod());
        Types.addMappedVariable(var, varBinding);
        node.setType(newType);
      }
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(ArrayCreation node) {
    ArrayType type = node.getType();
    Type newType = Types.makeIOSType(type);
    if (newType != null) {
      type.setComponentType(newType);
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(CastExpression node) {
    Type newType = Types.makeIOSType(node.getType());
    if (newType != null) {
      node.setType(newType);
    }
    return true;
  }
}
