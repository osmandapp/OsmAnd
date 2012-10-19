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

import com.google.common.base.Preconditions;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * An immutable description of a Java method, containing its name, declaring
 * class, and signature.  These descriptions are used when mapping Java method
 * use to equivalent iOS methods.
 *
 * @see IOSMethod
 * @author Tom Ball
 */
public class JavaMethod {
  private final String clazz;
  private final String name;
  private final String signature;
  private final boolean varargs;

  /**
   * Factory method that returns a JavaMethod for a specified method binding.
   */
  public static JavaMethod getJavaMethod(IMethodBinding binding) {
    Preconditions.checkNotNull(binding);
    ITypeBinding classBinding = binding.getDeclaringClass();
    if (classBinding == null) {
      return null;
    }
    String clazz = classBinding.getBinaryName();
    if (clazz == null) {
      return null; // true for local variables in unreachable code
    }
    return new JavaMethod(binding, classBinding);
  }

  private JavaMethod(IMethodBinding binding, ITypeBinding classBinding) {
    name = binding.getName();
    signature = Types.getSignature(binding);
    clazz = classBinding.getBinaryName();
    varargs = binding.isVarargs();
  }

  public String getClazz() {
    return clazz;
  }

  public String getName() {
    return name;
  }

  public String getSignature() {
    return signature;
  }

  public String getKey() {
    return clazz + '.' + name + signature;
  }

  public boolean isVarArgs() {
    return varargs;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result += clazz.hashCode() * 31;
    result += name.hashCode() * 31;
    result += signature.hashCode() * 31;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JavaMethod)) {
      return false;
    }
    JavaMethod other = (JavaMethod) obj;
    return clazz.equals(other.clazz) && name.equals(other.name)
        && signature.equals(other.signature);
  }

  @Override
  public String toString() {
    return "[clazz=" + clazz + ", name=" + name + ", signature=" + signature + "]";
  }
}
