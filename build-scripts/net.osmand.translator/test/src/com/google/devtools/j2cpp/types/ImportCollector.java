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

import com.google.common.collect.Sets;
import com.google.devtools.j2cpp.util.NameTable;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

import java.util.Set;

/**
 * Collects the set of imports and set of forward references needed to resolve
 * type references. Subclasses collect specific imports and super types needed
 * for header and implementation source files.
 *
 * @author Tom Ball
 */
public class ImportCollector extends ErrorReportingASTVisitor {

  private final Set<Import> imports = Sets.newLinkedHashSet();
  private final Set<Import> superTypes = Sets.newLinkedHashSet();

  public static final Import NULL_IMPORT = new Import("<not a type>", "", false);

  /**
   * Collects references and super types for a specified type declaration.
   *
   * @param typeDecl the type declaration to be scanned
   */
  public void collect(ASTNode node) {
    run(node);
    for (Import imp : superTypes) {
      if (imports.contains(imp)) {
        imports.remove(imp);
      }
    }
  }

  /**
   * Returns the collected set of imports.
   */
  public Set<Import> getImports() {
    return imports;
  }

  /**
   * Returns the collected set of super types.
   */
  public Set<Import> getSuperTypes() {
    return superTypes;
  }

  protected void addReference(Type type) {
    Import imp = getReference(type);
    if (imp != NULL_IMPORT) {
      addImport(imp);
    }
  }

  protected void addSuperType(Type type) {
    Import imp = getReference(type);
    if (imp != NULL_IMPORT) {
      superTypes.add(imp);
    }
  }

  protected Import getReference(Type type) {
    if (type == null || type instanceof PrimitiveType) {
      return NULL_IMPORT;
    }
    ITypeBinding binding = Types.getTypeBinding(type);
    if (binding == null) {
      binding = Types.resolveIOSType(type);
    }
    if (binding == null) {
      return NULL_IMPORT; // parser already reported missing class
    }
    if (Types.isIOSType(type)) {
      return NULL_IMPORT;
    }
    if (binding.isPrimitive()) {
      return NULL_IMPORT;
    }
    return getReference(binding);
  }

  protected void addReference(ITypeBinding binding) {
    addImport(getReference(binding));
  }

  protected Import getReference(ITypeBinding binding) {
    if (!binding.isTypeVariable() && !binding.isPrimitive() && !binding.isAnnotation()
        // Don't import IOS types, other than the IOS array types,
        // since they have header files.
        && (binding instanceof IOSArrayTypeBinding
            || !(binding instanceof IOSTypeBinding))) {
      binding = Types.mapType(binding);
      String typeName = NameTable.getFullName(binding);
      boolean isInterface = binding.isInterface();
      while (!binding.isTopLevel()) {
        binding = binding.getDeclaringClass();
      }
      return getReference(typeName,
          binding.getErasure().getQualifiedName(), isInterface);
    } else if (binding.isTypeVariable()) {
      ITypeBinding[] typeBounds = binding.getTypeBounds();
      if (typeBounds.length > 0 && !Types.isJavaObjectType(typeBounds[0])) {
        return getReference(typeBounds[0]);
      }
    }
    return NULL_IMPORT;
  }

  protected void addReference(String typeName, String javaFileName, boolean isInterface) {
    imports.add(getReference(typeName, javaFileName, isInterface));
  }

  protected void addSuperType(String typeName, String javaFileName, boolean isInterface) {
    superTypes.add(getReference(typeName, javaFileName, isInterface));
  }

  protected Import getReference(String typeName, String javaFileName, boolean isInterface) {
    return new Import(typeName, javaFileName, isInterface);
  }

  protected String getTypeName(Type type) throws ClassNotFoundException {
    assert type != null && !(type instanceof PrimitiveType);
    String fullName;
    ITypeBinding binding = type.resolveBinding();
    if (binding != null) {
      binding = Types.mapType(binding);
      fullName = NameTable.getFullName(binding);
    } else if (type instanceof SimpleType) {
      fullName = ((SimpleType) type).getName().getFullyQualifiedName();
    } else if (type instanceof QualifiedType) {
      fullName = ((QualifiedType) type).getName().getFullyQualifiedName();
    } else {
      throw new ClassNotFoundException(type.toString());
    }
    return fullName;
  }

  protected void addImport(Import imp) {
    if (imp != NULL_IMPORT && imp.needsImport()) {
     imports.add(imp);
    }
  }

  /**
   * Description of an imported type. Imports are equal if their fully qualified
   * type names are equal.
   */
  public static class Import implements Comparable<Import> {
    private final String typeName;
    private final String javaFileName;
    private final boolean isInterface;

    public Import(String typeName, String javaFileName, boolean isInterface) {
      this.typeName = typeName;
      this.javaFileName = javaFileName;
      this.isInterface = isInterface;
    }

    public String getTypeName() {
      return typeName;
    }

    public String getImportFileName() {
      return javaFileName.replace('.', '/');
    }

    public boolean isInterface() {
      return isInterface;
    }

    @Override
    public int compareTo(Import other) {
      return typeName.compareTo(other.typeName);
    }

    @Override
    public int hashCode() {
      return typeName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Import other = (Import) obj;
      return typeName.equals(other.typeName);
    }

    public boolean needsImport() {
      return !Types.isIOSType(typeName);
    }

    @Override
    public String toString() {
      return typeName;
    }
  }
}
