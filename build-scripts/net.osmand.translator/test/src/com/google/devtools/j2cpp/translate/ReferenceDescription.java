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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * A data class containing information about a variable reference.  These are
 * created when determining fix-ups that need to be made after converting an
 * anonymous class or extracting an inner class.
 *
 * @author Tom Ball
 */
class ReferenceDescription {
  final ASTNode node;
  final IVariableBinding binding;
  ITypeBinding declaringClass;
  IMethodBinding declaringMethod;
  IVariableBinding innerField;

  public ReferenceDescription(ASTNode node, IVariableBinding binding, IMethodBinding method) {
    this.node = node;
    this.binding = binding;
    declaringClass = binding.getDeclaringClass();
    declaringMethod = method;
  }

  @Override
  public String toString() {
    String bindingClass = declaringClass != null ? declaringClass.getName() + '.' : "";
    String innerStr = innerField != null ?
        innerField.getDeclaringClass().getName() + '.' + innerField.getName() : "null";
    return bindingClass + binding.getName() + " on " + innerStr;
  }

  static boolean typesEqual(ITypeBinding type1, ITypeBinding type2) {
    if (type1 == null && type2 == null) {
      return true;
    }
    if (type1 == null || type2 == null) {
      return false;
    }
    return type1.getTypeDeclaration().isEqualTo(type2.getTypeDeclaration());
  }
}
