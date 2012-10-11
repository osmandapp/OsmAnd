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

package com.google.devtools.j2cpp.util;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.NameTable;

/**
 * Singleton service for type/method/variable name support.
 *
 * @author Tom Ball
 */
public class NameTableCpp {

  /**
   * Return the Objective-C equivalent name for a Java primitive type.
   */
  public static String primitiveTypeToCpp(PrimitiveType type) {
    PrimitiveType.Code code = type.getPrimitiveTypeCode();
    return primitiveTypeToCpp(code.toString());
  }

  private static String primitiveTypeToCpp(String javaName) {
	  if (javaName.equals("boolean")) {
	      return "bool"; // defined in NSObject.h
	    }
	    if (javaName.equals("byte")) {
//	    TODO change to appropriate type 
	      return "wchar_t";
	    }
	    if (javaName.equals("char")) {
	      return "wchar_t";
	    }
	    if (javaName.equals("short")) {
	      return "signed short";
	    }
	    if (javaName.equals("long")) {
	      return "long long";
	    }
	    if (javaName.equals("float")) {
	        return "float";
	     }
	    if (javaName.equals("double")) {
	        return "double";
	     }
	    // type name unchanged for int, float, double, and void
	    return javaName;
  }

  public static String basicTypeToCpp(String javaName) {
//	  TODO populate
		if (javaName.equals("String")) {
			return "string";
		}
		if (javaName.equals("NSString *")) {
			return "string";
		}
		if (javaName.equals("JavaUtilList")) {
			return "vector";
		}
		if (javaName.equals("LongInt")) {
			return "long long";
		}
		if (javaName.equals("IOSCharArray *")) {
			return "wchar_t []";
		}
		return javaName;
  }
  
  /**
   * Convert a Java type into an equivalent C++ type.
   */
  public static String javaTypeToCpp(Type type, boolean includeInterfaces) {
    if (type instanceof PrimitiveType) {
      return primitiveTypeToCpp((PrimitiveType) type);
    }
    if (type instanceof ParameterizedType) {
      type = ((ParameterizedType) type).getType();  // erase parameterized type
    }
    if (type instanceof ArrayType) {
      ITypeBinding arrayBinding = Types.getTypeBinding(type);
      if (arrayBinding != null) {
        ITypeBinding elementType = arrayBinding.getElementType();
        return Types.resolveArrayType(elementType).getName();
      }
    }
    ITypeBinding binding = Types.getTypeBinding(type);
    return javaTypeToCpp(binding, includeInterfaces);
  }

  public static String javaTypeToCpp(ITypeBinding binding, boolean includeInterfaces) {
    if (binding.isInterface() && !includeInterfaces || binding == Types.resolveIOSType("id") ||
        binding == Types.resolveIOSType("NSObject")) {
      return NameTable.ID_TYPE;
    }
    if (binding.isTypeVariable()) {
      binding = binding.getErasure();
      if (Types.isJavaObjectType(binding) || binding.isInterface()) {
        return NameTable.ID_TYPE;
      }
      // otherwise fall-through
    }
    return NameTable.getFullName(binding);
  }

  public static String javaRefToCpp(Type type) {
	    return javaRefToCpp(Types.getTypeBinding(type));
	  }

	  public static String javaRefToCpp(ITypeBinding type) {
	    if (type.isPrimitive()) {
	      return primitiveTypeToCpp(type.getName());
	    }
	    String typeName = javaTypeToCpp(type, false);
	    if (typeName.equals(NameTable.ID_TYPE) || Types.isJavaVoidType(type)) {
	      if (type.isInterface()) {
	        return String.format("%s<%s>", NameTable.ID_TYPE, NameTable.getFullName(type));
	      }
	      return NameTable.ID_TYPE;
	    }
	    return typeName + " *";
	  }
	  /**
	   * Return the full name of a type, including its package.  For outer types,
	   * is the type's full name; for example, java.lang.Object's full name is
	   * "JavaLangObject".  For inner classes, the full name is their outer class'
	   * name plus the inner class name; for example, java.util.ArrayList.ListItr's
	   * name is "JavaUtilArrayList_ListItr".
	   */
	  public static String getFullName(AbstractTypeDeclaration typeDecl) {
	    return getFullName(Types.getTypeBinding(typeDecl));
	  }

	  public static String getFullName(ITypeBinding binding) {
	    if (binding.isPrimitive()) {
	      return primitiveTypeToCpp(binding.getName());
	    }
	    binding = Types.mapType(binding.getErasure());  // Make sure type variables aren't included.
	    String suffix = binding.isEnum() ? "Enum" : "";
	    String prefix = "";
	    IMethodBinding outerMethod = binding.getDeclaringMethod();
	    if (outerMethod != null && !binding.isAnonymous()) {
	      prefix += "_" + outerMethod.getName();
	    }
	    ITypeBinding outerBinding = binding.getDeclaringClass();
	    if (outerBinding != null) {
	      while (outerBinding.isAnonymous()) {
	        prefix += "_" + outerBinding.getName();
	        outerBinding = outerBinding.getDeclaringClass();
	      }
	      String baseName = getFullName(outerBinding) + prefix + '_' + NameTable.getName(binding);
	      return outerBinding.isEnum() ? baseName : baseName + suffix;
	    }
	    IPackageBinding pkg = binding.getPackage();
	    String pkgName = pkg != null ? NameTable.getPrefix(pkg.getName()) : "";
	    return pkgName + binding.getName() + suffix;
	  }
}
