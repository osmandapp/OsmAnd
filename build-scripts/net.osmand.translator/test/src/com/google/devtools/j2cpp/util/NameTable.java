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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.devtools.j2cpp.Options;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.types.IOSTypeBinding;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimplePropertyDescriptor;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Singleton service for type/method/variable name support.
 *
 * @author Tom Ball
 */
public class NameTable {

  private static NameTable instance;
  private final Map<IBinding, String> renamings = Maps.newHashMap();

  public static final String CLINIT_NAME = "initialize";

  public static final String ID_TYPE = "id";
  
  private static final Logger logger = Logger.getLogger(NameTable.class.getName());

  /**
   * The list of predefined types, common primitive typedefs, constants and
   * variables.
   */
  public static final List<String> reservedNames = Lists.newArrayList(
      // types
      "id", "bool", "BOOL", "SEL", "IMP", "unichar",

      // constants
      "nil", "Nil", "YES", "NO", "TRUE", "FALSE",

      // C99 keywords
      "auto", "const", "extern", "inline", "register", "restrict", "signed", "goto", "sizeof",
      "struct", "typedef", "union", "volatile",

      // C++ keywords
      "template", "mutable", "not", "delete",

      // variables
      "self", "isa",

      // Definitions from standard C and Objective-C headers, not including
      // typedefs and #defines that start with "_", nor #defines for
      // functions.  Some of these may seem very unlikely to be used in
      // Java source, but if a name is legal some Java developer might very
      // well use it.

      // Definitions from stddef.h
      "ptrdiff_t", "size_t", "wchar_t", "wint_t",

      // Definitions from stdint.h
      "int8_t", "int16_t", "int32_t", "int64_t", "uint8_t", "uint16_t", "uint32_t", "uint64_t",
      "int_least8_t", "int_least16_t", "int_least32_t", "int_least64_t",
      "uint_least8_t", "uint_least16_t", "uint_least32_t", "uint_least64_t",
      "int_fast8_t", "int_fast16_t", "int_fast32_t", "int_fast64_t",
      "uint_fast8_t", "uint_fast16_t", "uint_fast32_t", "uint_fast64_t",
      "intptr_t", "uintptr_t", "intmax_t", "uintmax_t",
      "INT8_MAX", "INT16_MAX", "INT32_MAX", "INT64_MAX", "INT8_MIN", "INT16_MIN", "INT32_MIN",
      "INT64_MIN", "UINT8_MAX", "UINT16_MAX", "UINT32_MAX", "UINT64_MAX", "INT_LEAST8_MIN",
      "INT_LEAST16_MIN", "INT_LEAST32_MIN", "INT_LEAST64_MIN", "INT_LEAST8_MAX", "INT_LEAST16_MAX",
      "INT_LEAST32_MAX", "INT_LEAST64_MAX", "INT_FAST8_MIN", "INT_FAST16_MIN", "INT_FAST32_MIN",
      "INT_FAST64_MIN", "INT_FAST8_MAX", "INT_FAST16_MAX", "INT_FAST32_MAX", "INT_FAST64_MAX",
      "UINT_FAST8_MAX", "UINT_FAST16_MAX", "UINT_FAST32_MAX", "UINT_FAST64_MAX", "INTPTR_MIN",
      "INTPTR_MAX", "UINTPTR_MAX", "INTMAX_MIN", "INTMAX_MAX", "UINTMAX_MAX", "PTRDIFF_MIN",
      "PTRDIFF_MAX", "SIZE_MAX", "WCHAR_MAX", "WCHAR_MIN", "WINT_MIN", "WINT_MAX",
      "SIG_ATOMIC_MIN", "SIG_ATOMIC_MAX", "INT8_MAX", "INT16_MAX", "INT32_MAX", "INT64_MAX",
      "UINT8_C", "UINT16_C", "UINT32_C", "UINT64_C", "INTMAX_C", "UINTMAX_C",

      // Definitions from stdio.h
      "va_list", "fpos_t", "FILE", "off_t", "ssize_t", "BUFSIZ", "EOF", "FOPEN_MAX",
      "FILENAME_MAX", "TMP_MAX", "SEEK_SET", "SEEK_CUR", "SEEK_END", "stdin", "stdout", "stderr",

      // Definitions from stdlib.h
      "ct_rune_t", "rune_t", "div_t", "ldiv_t", "lldiv_t", "dev_t", "mode_t",
      "NULL", "EXIT_FAILURE", "EXIT_SUCCESS", "RAND_MAX", "MB_CUR_MAX", "MB_CUR_MAX_L",

      // Cocoa definitions from ConditionalMacros.h
      "CFMSYSTEMCALLS", "CGLUESUPPORTED", "FUNCTION_PASCAL", "FUNCTION_DECLSPEC",
      "FUNCTION_WIN32CC", "GENERATING68881", "GENERATING68K", "GENERATINGCFM", "GENERATINGPOWERPC",
      "OLDROUTINELOCATIONS", "PRAGMA_ALIGN_SUPPORTED", "PRAGMA_ENUM_PACK", "PRAGMA_ENUM_ALWAYSINT",
      "PRAGMA_ENUM_OPTIONS", "PRAGMA_IMPORT", "PRAGMA_IMPORT_SUPPORTED", "PRAGMA_ONCE",
      "PRAGMA_STRUCT_ALIGN", "PRAGMA_STRUCT_PACK", "PRAGMA_STRUCT_PACKPUSH",
      "TARGET_API_MAC_CARBON", "TARGET_API_MAC_OS8", "TARGET_API_MAC_OSX", "TARGET_CARBON",
      "TYPE_BOOL", "TYPE_EXTENDED", "TYPE_LONGDOUBLE_IS_DOUBLE", "TYPE_LONGLONG",
      "UNIVERSAL_INTERFACES_VERSION",

      // Core Foundation definitions
      "BIG_ENDIAN", "BYTE_ORDER", "LITTLE_ENDIAN", "PDP_ENDIAN",

      // Foundation methods with conflicting return types
      "scale");

  /**
   * List of NSObject message names.  Java methods with one of these names are
   * renamed to avoid unintentional overriding.  Message names with trailing
   * colons are not included since they can't be overridden.  For example,
   * "public boolean isEqual(Object o)" would be translated as
   * "- (BOOL)isEqualWithObject:(NSObject *)o", not NSObject's "isEqual:".
   */
  public static final List<String> nsObjectMessages = Lists.newArrayList(
      "alloc", "attributeKeys", "autoContentAccessingProxy", "autorelease",
      "classCode", "classDescription", "classForArchiver",
      "classForKeyedArchiver", "classFallbacksForKeyedArchiver",
      "classForPortCoder", "className", "copy", "dealloc", "description",
      "hash", "init", "initialize", "isProxy", "load", "mutableCopy", "new",
      "release", "retain", "retainCount", "scriptingProperties", "self",
      "superclass", "toManyRelationshipKeys", "toOneRelationshipKeys",
      "version");

  /**
   * Map of package names to their specified prefixes.  Multiple packages
   * can share a prefix; for example, the com.google.common packages in
   * Guava could share a "GG" (Google Guava) or simply "Guava" prefix.
   */
  private final Map<String, String> prefixMap;

  private NameTable(Map<String, String> prefixMap) {
    this.prefixMap = prefixMap;
  }

  /**
   * Initialize this service using the AST returned by the parser.
   */
  public static void initialize(CompilationUnit unit) {
    instance = new NameTable(Options.getPackagePrefixes());
  }

  public static void cleanup() {
    instance = null;
  }

  /**
   * Returns a bound name that may have been renamed by a translation phase.
   *
   * @return the new name, or the old name if no renaming exists
   */
  public static String getName(IBinding binding) {
    assert binding != null;
    binding = getBindingDeclaration(binding);
    String newName = instance.renamings.get(binding);
    if (newName != null) {
      return newName;
    }
    String name = binding.getName();
    if (binding instanceof IVariableBinding) {
      IVariableBinding var = (IVariableBinding) binding;
      if (isReservedName(name)) {
        name += "_";
      }
      if (var.isField()) {
        // Check if field has the same name as a method.
        ITypeBinding superclass = ((IVariableBinding) binding).getDeclaringClass();
        for (IMethodBinding method : superclass.getDeclaredMethods()) {
          if (method.getName().equals(name)) {
            name = name + '_';
            break;
          }
        }
      }
    }
    return name;
  }

  private static IBinding getBindingDeclaration(IBinding binding) {
    if (binding instanceof IVariableBinding) {
      return ((IVariableBinding) binding).getVariableDeclaration();
    }
    if (binding instanceof IMethodBinding) {
      return ((IMethodBinding) binding).getMethodDeclaration();
    }
    if (binding instanceof ITypeBinding) {
      return ((ITypeBinding) binding).getTypeDeclaration();
    }
    return binding;
  }

  /**
   * Returns a name for a SimpleName that may have been renamed by a
   * translation phase.
   *
   * @return the new name, or the old name if no renaming exists
   */
  public static String getName(SimpleName node) {
    return getName(Types.getBinding(node));
  }

  public static boolean isRenamed(IBinding binding) {
    return instance.renamings.containsKey(binding);
  }

  public static boolean isRenamed(SimpleName node) {
    return isRenamed(Types.getBinding(node));
  }

  /**
   * Adds a name to the renamings map, used by getName().
   */
  public static void rename(IBinding oldName, String newName) {
    oldName = getBindingDeclaration(oldName);
    String previousName = instance.renamings.get(oldName);
    if (previousName != null && !previousName.equals(newName)) {
      logger.fine(String.format("Changing previous rename: %s => %s, now: %s => %s",
          oldName.toString(), previousName, oldName, newName));
    }
    rename(oldName, newName, false);
  }

  public static void rename(IBinding oldName, String newName, boolean allowPreviousRenames) {
    instance.renamings.put(getBindingDeclaration(oldName), newName);
  }

  /**
   * Adds a SimpleName to the renamings map.
   */
  public static void rename(SimpleName node, String newName) {
    rename(Types.getBinding(node), newName);
  }

  /**
   * Capitalize the first letter of a string.
   */
  public static String capitalize(String s) {
    return s.length() > 0 ? Character.toUpperCase(s.charAt(0)) + s.substring(1) : s;
  }

  /**
   * Given a period-separated name, return as a camel-cased type name.  For
   * example, java.util.logging.Level is returned as JavaUtilLoggingLevel.
   */
  public static String camelCaseQualifiedName(String fqn) {
    StringBuilder sb = new StringBuilder();
    for (String part : fqn.split("\\.")) {
      sb.append(capitalize(part));
    }
    return sb.toString();
  }

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
    return getFullName(binding);
  }

  /**
   * Convert a Java type reference into an equivalent C++ type.
   */
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
        return String.format("%s<%s>", ID_TYPE, getFullName(type));
      }
      return NameTable.ID_TYPE;
    }
    return typeName + " *";
  }

  /**
   * Return a comma-separated list of field names from a fragments list.
   * Skip any discarded variables; currently that's just serialVersionUID.
   *
   * @param fragments a list of VariableDeclarationFragment instances
   */
  public static String fieldNames(List<?> fragments) {
    if (fragments.isEmpty()) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (Iterator<?> iterator = fragments.iterator(); iterator.hasNext();) {
      Object o = iterator.next();
      if (o instanceof VariableDeclarationFragment) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
        String name = fragment.getName().getIdentifier();
        if ("serialVersionUID".equals(name)) {
          continue;
        }
        sb.append(name);
        if (iterator.hasNext()) {
          sb.append(", ");
        }
      } else {
        throw new AssertionError("unknown fragment type: " + o.getClass());
      }
    }
    return sb.toString();
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
      String baseName = getFullName(outerBinding) + prefix + '_' + getName(binding);
      return outerBinding.isEnum() ? baseName : baseName + suffix;
    }
    IPackageBinding pkg = binding.getPackage();
    String pkgName = pkg != null ? getPrefix(pkg.getName()) : "";
    return pkgName + binding.getName() + suffix;
  }

  /**
   * Returns the full name of a type declaration's superclass.
   */
  public static String getSuperClassName(TypeDeclaration typeDecl) {
    Type superclass = typeDecl.getSuperclassType();
    if (superclass instanceof ParameterizedType) {
      superclass = ((ParameterizedType) superclass).getType();
    }
    if (superclass instanceof SimpleType || superclass instanceof QualifiedType) {
      ITypeBinding binding = Types.getTypeBinding(superclass).getErasure();
      String typeName = binding instanceof IOSTypeBinding ? binding.getQualifiedName()
          : getFullName(binding);
      return Types.mapSimpleTypeName(typeName);
    }
    return "NSObject";
  }

  /**
   * Returns a "Type_method" function name for static methods, such as from
   * enum types.
   */
  public static String makeFunctionName(AbstractTypeDeclaration cls, MethodDeclaration method) {
    return getFullName(cls) + '_' + method.getName().getIdentifier();
  }

  /**
   * Returns a SimpleName for an identifier that may not be a legal
   * Java identifier but is for iOS.  For example, JDT doesn't allow
   * Java keywords such as "class" to be used as names.
   */
  public static SimpleName unsafeSimpleName(String identifier, AST ast) {
    SimpleName name = ast.newSimpleName("foo");
    try {
      Field field = SimpleName.class.getDeclaredField("identifier");
      field.setAccessible(true);

      Class<?>[] argTypes = new Class[] { SimplePropertyDescriptor.class };
      Object[] args = new Object[] { SimpleName.IDENTIFIER_PROPERTY };
      Method preValueChange = ASTNode.class.getDeclaredMethod("preValueChange", argTypes);
      Method postValueChange = ASTNode.class.getDeclaredMethod("postValueChange", argTypes);
      preValueChange.setAccessible(true);
      postValueChange.setAccessible(true);

      preValueChange.invoke(name, args);
      field.set(name, identifier);
      postValueChange.invoke(name, args);
    } catch (Exception e) {
      // should never happen, since only the one known class is manipulated
      e.printStackTrace();
      System.exit(1);
    }
    return name;
  }

  public static boolean isReservedName(String name) {
    return reservedNames.contains(name) || nsObjectMessages.contains(name);
  }

  /**
   * Returns the fully-qualified main class for a given compilation unit.
   */
  public static String getMainJavaName(CompilationUnit node, String sourceFileName) {
    String className = getClassNameFromSourceFileName(sourceFileName);
    PackageDeclaration pkgDecl = node.getPackage();
    if (pkgDecl != null) {
      className = pkgDecl.getName().getFullyQualifiedName()  + '.' + className;
    }
    return className;
  }

  private static String getClassNameFromSourceFileName(String sourceFileName) {
    int begin = sourceFileName.lastIndexOf(File.separatorChar) + 1;
    int end = sourceFileName.lastIndexOf(".java");
    String className = sourceFileName.substring(begin, end);
    return className;
  }

  public static String getMainTypeName(CompilationUnit node, String sourceFileName) {
    String className = getClassNameFromSourceFileName(sourceFileName);
    PackageDeclaration pkgDecl = node.getPackage();
    if (pkgDecl != null) {
      String pkgName = getPrefix(pkgDecl.getName().getFullyQualifiedName());
      return pkgName + className;
    } else {
      return className;
    }
  }

  public static String getStaticAccessorName(String varName) {
    // follow the Obj-C style guide for reader names, unless it's an illegal name
    return isReservedName(varName) ? "get" + capitalize(varName) : varName;
  }

  public static String getStaticVarQualifiedName(ITypeBinding declaringType, String varName) {
    return getFullName(declaringType) + "_" + varName + "_";
  }

  public static String getPrimitiveConstantName(IVariableBinding constant) {
    return String.format("%s_%s", getFullName(constant.getDeclaringClass()), constant.getName());
  }

  public static String getParameterTypeName(String typeName, ITypeBinding typeBinding) {
    if (typeName.equals("long long int") || typeName.equals("long")) {
      typeName = "LongInt";   // avoid name conflict with java.lang.Long
    } else if (typeName.equals("short int") || typeName.equals("short")) {
      typeName = "ShortInt";  // or java.lang.Short
    } else if (typeBinding.isArray()) {
      ITypeBinding elementType = typeBinding.getElementType();
      if (elementType.isPrimitive()) {
        elementType = Types.getWrapperType(elementType);
      }
      if (elementType.isParameterizedType()) {
        elementType = elementType.getErasure();
      }
      if (elementType.isCapture()) {
        elementType = elementType.getWildcard();
      }
      if (elementType.isWildcardType()) {
        ITypeBinding bound = elementType.getBound();
        if (bound != null) {
          elementType = bound;
        }
      }
      typeName = getFullName(elementType) + "Array";
    }
    return typeName;
  }

  public static String javaFieldToCpp(String fieldName) {
    return fieldName + "_";
  }

  public static void mapPackageToPrefix(String packageName, String prefix) {
    instance.prefixMap.put(packageName, prefix);
  }

  /**
   * Return the prefix for a specified package.  If a prefix was specified
   * for the package on the command-line, then that prefix is returned.
   * Otherwise, a camel-cased prefix is created from the package name.
   */
  public static String getPrefix(String packageName) {
    if (hasPrefix(packageName)) {
      return instance.prefixMap.get(packageName);
    }
    StringBuilder sb = new StringBuilder();
    for (String part : packageName.split("\\.")) {
      sb.append(capitalize(part));
    }
    return sb.toString();
  }

  public static boolean hasPrefix(String packageName) {
    return instance.prefixMap.containsKey(packageName);
  }
}
