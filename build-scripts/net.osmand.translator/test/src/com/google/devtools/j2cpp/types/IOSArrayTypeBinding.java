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

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * IOSTypeBinding: synthetic binding for an iOS array type.
 *
 * @author Tom Ball
 */
public class IOSArrayTypeBinding extends IOSTypeBinding {
  private final String initMethod;
  private final String accessMethod;
  private final String copyMethod;
  private final ITypeBinding elementType;
  private final ITypeBinding primitiveElementType;

  /**
   * Create an array binding.
   *
   * @param name the iOS class name
   * @param initMethod the method used to create an array from a C array of
   *                   same primitive type
   * @param accessMethod the method used to access an array member, not including
   *                   parameter name
   * @param copyMethod the method used to copy the contents of this array, not including
   *                   parameter names
   * @param elementType the binding for the type of element this array contains
   * @param primitiveElementType the binding for the primitive type corresponding to
   *                   the element this array contains. null for Object[].
   */
  public IOSArrayTypeBinding(String name, String initMethod, String accessMethod,
      String copyMethod, ITypeBinding elementType, ITypeBinding primitiveElementType) {
    super(name, false);
    this.initMethod = initMethod;
    this.accessMethod = accessMethod;
    this.copyMethod = copyMethod;
    this.elementType = elementType;
    this.primitiveElementType = primitiveElementType;
  }

  @Override
  public ITypeBinding getComponentType() {
    return Types.getIOSArrayComponentType(this);
  }

  @Override
  public boolean isArray() {
    return true;
  }

  /**
   * Returns the name of the init method, such as "initWithInts".
   */
  public String getInitMethod() {
    return initMethod;
  }

  /**
   * Returns the name of the access method, such as "charAtIndex".
   */
  public String getAccessMethod() {
    return accessMethod;
  }

  /**
   * Returns the name of the copy method, such as "getChars".
   */
  public String getCopyMethod() {
    return copyMethod;
  }

  @Override
  public ITypeBinding getElementType() {
    return elementType;
  }

  @Override
  public boolean isAssignmentCompatible(ITypeBinding toType) {
    if (!toType.isArray()) {
      return false;
    }

    if (toType.getElementType().isPrimitive()) {
      return primitiveElementType != null &&
          primitiveElementType.isAssignmentCompatible(toType.getElementType());
    } else if (primitiveElementType == null) {
      // Object[] - we trust the compiler to already have checked types, since
      // we generalize all non-primitive (or boxed primitive) arrays to
      // Object[]. In Obj-C land, we can always assign arrays of objects.
      return true;
    } else {
      return elementType.isAssignmentCompatible(toType.getElementType());
    }
  }
}
