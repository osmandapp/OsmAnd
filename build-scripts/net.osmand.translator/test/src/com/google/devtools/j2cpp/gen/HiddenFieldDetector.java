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

package com.google.devtools.j2cpp.gen;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;

import com.google.devtools.j2objc.util.ASTNodeException;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans for parameters and local variable declarations that hide
 * fields.  Java allows this, but gcc warns loudly.
 *
 * @author Tom Ball
 */
public class HiddenFieldDetector extends ErrorReportingASTVisitor {
  private final Set<IVariableBinding> fieldNameConflicts = Sets.newLinkedHashSet();

  /**
   * A map of each type to a set of their field names.
   */
  Map<String, Set<String>> fieldNameMap = Maps.newHashMap();
  private static final Set<String> NO_FIELDS = Sets.newLinkedHashSet();

  public static Set<IVariableBinding> getFieldNameConflicts(ASTNode node)
      throws ASTNodeException {
    HiddenFieldDetector detector = new HiddenFieldDetector();
    detector.run(node);
    return detector.fieldNameConflicts;
  }


  @Override
  public boolean visit(TypeDeclaration node) {
    if (!node.isInterface()) {
      Set<String> names = Sets.newLinkedHashSet();
      ITypeBinding binding = Types.getTypeBinding(node);
      addFields(binding, true, names);
      fieldNameMap.put(binding.getBinaryName(), names);
    }
    return super.visit(node);
  }

  private void addFields(ITypeBinding binding, boolean includePrivate, Set<String> names) {
    for (IVariableBinding field : binding.getDeclaredFields()) {
      if (includePrivate || (field.getModifiers() & Modifier.PRIVATE) == 0) {
        names.add(NameTable.javaFieldToCpp(field.getName()));
      }
    }
    ITypeBinding superType = binding.getSuperclass();
    if (superType != null) {
      addFields(superType, false, names);
    }
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    IMethodBinding binding = node.resolveBinding();
    if (binding != null) {
      Set<String> fieldNames = fieldNameMap.get(binding.getDeclaringClass().getBinaryName());
      if (fieldNames == null) {
        fieldNames = NO_FIELDS;
      }

      @SuppressWarnings("unchecked")
      List<SingleVariableDeclaration> parameters = node.parameters();
      for (SingleVariableDeclaration param : parameters) {
        IVariableBinding varBinding = param.resolveBinding();
        if (varBinding != null && fieldNames.contains(varBinding.getName())) {
          fieldNameConflicts.add(varBinding);
        }
      }
    }
    return super.visit(node);
  }
}
