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

import com.google.devtools.j2cpp.sym.MethodSymbol;
import com.google.devtools.j2cpp.sym.Scope;
import com.google.devtools.j2cpp.sym.Symbol;
import com.google.devtools.j2cpp.sym.Symbols;
import com.google.devtools.j2cpp.sym.TypeSymbol;
import com.google.devtools.j2cpp.sym.VariableSymbol;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.GeneratedVariableBinding;
import com.google.devtools.j2cpp.types.NodeCopier;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;
import com.google.devtools.j2cpp.util.TypeTrackingVisitor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.List;

/**
 * Adds support for inner and anonymous classes, and extracts them to be
 * top-level classes (also like class files). This is similar to how Java
 * compilers convert inner classes into class files, which are all top-level.
 *
 * @author Tom Ball
 */
public class InnerClassExtractor extends ClassConverter {
  private final List<AbstractTypeDeclaration> unitTypes;

  static final char INNERCLASS_DELIMITER = '_';

  @SuppressWarnings("unchecked")
  public InnerClassExtractor(CompilationUnit unit) {
    super(unit);
    unitTypes = unit.types(); // safe by definition
  }

  @Override
  public boolean visit(CompilationUnit node) {
    return true;
  }

  @Override
  public void endVisit(CompilationUnit node) {
    // Fixup references to the inner fields.
    node.accept(new OuterReferenceFixer());
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    super.visit(node);
    visitType(node);
    return true;
  }

  @Override
  public boolean visit(EnumDeclaration node) {
    super.visit(node);
    visitType(node);
    return true;
  }

  @Override
  public boolean visit(AnnotationTypeDeclaration node) {
    super.visit(node);
    return false; // ignore annotations
  }

  public boolean visitType(AbstractTypeDeclaration node) {
    ASTNode parentNode = node.getParent();
    if (!(parentNode instanceof CompilationUnit)) {
      ITypeBinding type = Types.getTypeBinding(node);
      if (!type.isInterface() && !type.isAnnotation() && !Modifier.isStatic(type.getModifiers())) {
        addOuterFields(node);
      }

      if (parentNode instanceof AbstractTypeDeclaration) {
        // Remove declaration from declaring type.
        AbstractTypeDeclaration parent = (AbstractTypeDeclaration) node.getParent();
        @SuppressWarnings("unchecked")
        List<AbstractTypeDeclaration> parentTypes =
            parent.bodyDeclarations(); // safe by definition
        boolean success = parentTypes.remove(node);
        assert success;
      } else {
        TypeDeclarationStatement typeStatement = (TypeDeclarationStatement) parentNode;
        node = NodeCopier.copySubtree(node.getAST(), typeStatement.getDeclaration());

        // Remove stmt from method body (or an if/else/try/catch/finally clause).
        Block body = (Block) typeStatement.getParent();
        @SuppressWarnings("unchecked")
        List<Statement> stmts = body.statements(); // safe by definition
        boolean success = stmts.remove(typeStatement);
        assert success;
      }

      // Make this node non-private, if necessary, and add it to the unit's type
      // list.
      @SuppressWarnings("unchecked") // safe by definition
      List<IExtendedModifier> modifiers = node.modifiers();
      for (IExtendedModifier iem : modifiers) {
        if (iem instanceof Modifier) {
          Modifier mod = (Modifier) iem;
          if (mod.getKeyword().equals(ModifierKeyword.PRIVATE_KEYWORD)) {
            modifiers.remove(mod);
            break;
          }
        }
      }
      unitTypes.add(node);
    }
    return true;
  }

  private void addOuterFields(AbstractTypeDeclaration node) {
    @SuppressWarnings("unchecked")
    List<BodyDeclaration> members = node.bodyDeclarations(); // safe by definition
    AST ast = node.getAST();
    ITypeBinding clazz = Types.getTypeBinding(node);
    ITypeBinding outerClazz = clazz.getDeclaringClass();
    assert outerClazz != null;

    List<IVariableBinding> innerFields = Lists.newArrayList();
    // Ensure that the new outer field does not conflict with a field in a superclass.
    ITypeBinding superClazz = clazz.getSuperclass();
    ITypeBinding superDeclaringClazz = superClazz.getDeclaringClass();
    int suffix = 0;
    while (superClazz.getDeclaringClass() != null) {
      if (!Modifier.isStatic(superClazz.getModifiers())) {
        suffix++;
      }
      superClazz = superClazz.getSuperclass();
    }
    String outerFieldName = "this$" + suffix;

    FieldDeclaration outerField = createField(outerFieldName, outerClazz, clazz, ast);
    members.add(0, outerField);
    IVariableBinding outerVar = Types.getVariableBinding(outerField.fragments().get(0));
    innerFields.add(outerVar);

    if (superDeclaringClazz != null &&
        !Modifier.isStatic(clazz.getSuperclass().getModifiers()) &&
        !outerClazz.isAssignmentCompatible(superDeclaringClazz.getTypeDeclaration())) {
      // The super class is an inner class, and it's declaring class and the
      // current node's declaring class don't match, so we need another outer
      // var. This var is only a parameter, not a field, an it's name doesn't
      // matter because addInnerParameters will assign a different name to it
      // anyway.
      IVariableBinding secondOuterVar = new GeneratedVariableBinding(
          "", Modifier.FINAL, superDeclaringClazz, false, true, superClazz, null);
      innerFields.add(secondOuterVar);
    }

    // Insert new parameters for each constructor in class.
    boolean needsConstructor = true;
    for (BodyDeclaration member : members) {
      if (member instanceof MethodDeclaration && ((MethodDeclaration) member).isConstructor()) {
        needsConstructor = false;
        MethodDeclaration constructor = (MethodDeclaration) member;
        IMethodBinding oldBinding = Types.getMethodBinding(constructor);
        GeneratedMethodBinding newBinding = new GeneratedMethodBinding(oldBinding);
        Types.addBinding(constructor, newBinding);
        addInnerParameters(constructor, newBinding, innerFields, ast, false);
        assert constructor.parameters().size() == newBinding.getParameterTypes().length;
      }
    }

    if (needsConstructor) {
      MethodDeclaration constructor = ast.newMethodDeclaration();
      constructor.setConstructor(true);
      ITypeBinding voidType = ast.resolveWellKnownType("void");
      GeneratedMethodBinding binding = new GeneratedMethodBinding("init", 0,
          voidType, clazz, true, false, true);
      Types.addBinding(constructor, binding);
      Types.addBinding(constructor.getReturnType2(), voidType);
      SimpleName name = ast.newSimpleName("init");
      Types.addBinding(name, binding);
      constructor.setName(name);
      constructor.setBody(ast.newBlock());
      addInnerParameters(constructor, binding, innerFields, ast, false);
      members.add(constructor);
      assert constructor.parameters().size() == binding.getParameterTypes().length;
    }
  }

  /**
   * Updates variable references outside an inner class to the new fields
   * injected into it.
   */
  private class OuterReferenceFixer extends TypeTrackingVisitor {

    @Override
    public boolean visit(ClassInstanceCreation node) {
      IMethodBinding binding = Types.getMethodBinding(node);
      ITypeBinding newType = Types.getTypeBinding(node);
      Expression outer = node.getExpression();
      ITypeBinding outerType = outer == null ? null : Types.getTypeBinding(outer);
      if (outer != null) {
        // Outer expression.new Inner(): convert to new Inner(Outer expression).
        IBinding outerBinding = Types.getBinding(outer);
        node.setExpression(null);

        // Add copy of outer expression as constructor argument.
        outer = NodeCopier.copySubtree(node.getAST(), outer);
        Types.addBinding(outer, outerBinding);
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments(); // safe by definition
        args.add(0, outer);

        // Update constructor binding with added parameter.
        GeneratedMethodBinding newBinding = new GeneratedMethodBinding(binding);
        binding = newBinding;
        GeneratedVariableBinding param =
            new GeneratedVariableBinding(outerType, false, true, outerType, null);
        newBinding.addParameter(0, param);
        Types.addBinding(node, newBinding);
      }

      if (!Modifier.isStatic(newType.getModifiers()) &&
          (newType.isMember() || newType.isAnonymous()) &&
          (outer == null || !outerType.isAssignmentCompatible(newType.getDeclaringClass()))) {
        Expression expr = null;
        ITypeBinding declaringClass = newType.getDeclaringClass();
        ITypeBinding currentType = getCurrentType();
        if (NameTable.getFullName(declaringClass).equals(NameTable.getFullName(currentType))
            || currentType.isAssignmentCompatible(declaringClass)) {
          expr = node.getAST().newThisExpression();
          Types.addBinding(expr, declaringClass);
        } else {
          // Use this$ reference as first argument.
          TypeSymbol typeSymbol = Symbols.resolve(currentType);
          for (Symbol sym : typeSymbol.getScope().getMembers()) {
            if (sym.getName().startsWith("this$")) {
              expr = makeFieldRef((IVariableBinding) sym.getBinding(), node.getAST());
              break;
            }
          }
          assert expr != null;
        }
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments(); // safe by definition
        GeneratedMethodBinding newBinding = new GeneratedMethodBinding(binding);
        Types.addBinding(node, newBinding);
        binding = newBinding;
        args.add(0, expr);
        GeneratedVariableBinding param = new GeneratedVariableBinding(declaringClass,
            false, true, declaringClass, binding);
        newBinding.addParameter(0, param);
        assert node.arguments().size() == binding.getParameterTypes().length;
      }
      return true;
    }

    @Override
    public boolean visit(FieldAccess node) {
      // Update Outer.this expressions.
      if (node.getExpression() instanceof ThisExpression) {
        ThisExpression thisExpr = (ThisExpression) node.getExpression();
        Name qualifier = thisExpr.getQualifier();
        if (qualifier != null) {
          TypeSymbol outer = Symbols.resolve(Types.getTypeBinding(qualifier));
          TypeSymbol current = Symbols.resolve(getCurrentType());
          if (getCurrentType().isTopLevel() || outer.equals(current)) {
            thisExpr.setQualifier(null);
          } else {
            AST ast = node.getAST();
            Name outerQualifier = makeOuterQualifier(outer, current, ast);
            ITypeBinding tb = (ITypeBinding) outer.getBinding();
            if (outerQualifier.isQualifiedName()) {
              // Replace this field access.
              String name = ((QualifiedName) outerQualifier).getName().getIdentifier();
              IVariableBinding binding =
                  new GeneratedVariableBinding(name, 0, tb, true, false, tb, null);
              Types.addBinding(outerQualifier, binding);
              node.setExpression(outerQualifier);
            } else {
              // Replace this field access with a qualified one.
              SimpleName name = (SimpleName) outerQualifier;
              IVariableBinding binding =
                  new GeneratedVariableBinding(name.getIdentifier(), 0, tb, true, false, tb, null);
              Types.addBinding(outerQualifier, binding);
              ThisExpression newThis = ast.newThisExpression();
              Types.addBinding(newThis, tb);
              FieldAccess innerFieldAccess = ast.newFieldAccess();
              innerFieldAccess.setName(name);
              innerFieldAccess.setExpression(newThis);
              Types.addBinding(innerFieldAccess, binding);
              node.setExpression(innerFieldAccess);
            }
          }
        }
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(MethodInvocation node) {
      if (node.getExpression() == null) {
        IMethodBinding methodBinding = Types.getMethodBinding(node);
        MethodSymbol method = Symbols.resolve(methodBinding);
        ITypeBinding currentType = getCurrentType();
        TypeSymbol current = Symbols.resolve(currentType);
        if (needsOuterReference(methodBinding, currentType)) {
          Name outerQualifier = makeOuterQualifier(method, current, node.getAST());
          node.setExpression(outerQualifier);
        }
      }
      return true;
    }

    private boolean needsOuterReference(IMethodBinding methodBinding, ITypeBinding currentType) {
      MethodSymbol method = Symbols.resolve(methodBinding);
      TypeSymbol current = Symbols.resolve(currentType);

      if (method == null || current.getScope().contains(method) ||
          Modifier.isStatic(methodBinding.getModifiers()) ||
          currentType.isTopLevel() || Modifier.isStatic(currentType.getModifiers())) {
        return false;
      }

      ITypeBinding declaringType = methodBinding.getMethodDeclaration().getDeclaringClass();
      while (currentType != null) {
        if (currentType.isAssignmentCompatible(declaringType)) {
          return true;
        }

        currentType = currentType.getDeclaringClass();
      }

      return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
      // Check for outer array.length, to make sure array is visited.
      IBinding binding = Types.getBinding(node);
      if (binding instanceof IVariableBinding) {
        IVariableBinding var = (IVariableBinding) binding;
        Name qualifier = node.getQualifier();
        if (var.getName().equals("length") && Types.getTypeBinding(qualifier).isArray()) {
          qualifier.accept(this);
        }
      }
      return false;
    }

    @Override
    public boolean visit(SimpleName node) {
      if (node.getParent() instanceof FieldAccess) {
        // Already a qualified node - no need to fix this reference.
        return false;
      }

      TypeSymbol currentType = Symbols.resolve(getCurrentType());
      IBinding binding = Types.getBinding(node);
      if (binding instanceof IVariableBinding) {
        IVariableBinding varBinding = (IVariableBinding) binding;
        if (varBinding.isEnumConstant() || currentType.isEnum()) {
          return true;
        }
        if (Modifier.isStatic(varBinding.getModifiers()) || !varBinding.isField()) {
          return true;
        }
        if (varBinding.getName().startsWith("this$")) {
          return true;  // Already resolved.
        }
        VariableSymbol sym = Symbols.resolve(varBinding);
        Scope scope = Symbols.getScope(node);
        if (scope.contains(sym)) {
          return true;
        }
        AST ast = node.getAST();
        Name newName = makeQualifiedName(node, sym, currentType, ast);
        Types.addBinding(newName, binding);
        setProperty(node, newName);
      }

      return true;
    }

    @Override
    public boolean visit(ThisExpression node) {
      Name qualifier = node.getQualifier();
      if (qualifier != null) {
        ITypeBinding outerType = Types.getTypeBinding(qualifier);
        TypeSymbol outer = Symbols.resolve(outerType);
        TypeSymbol current = Symbols.resolve(getCurrentType());
        if (outer.equals(current)) {
          node.setQualifier(null);
        } else {
          Name outerQualifier = makeOuterQualifier(outer, current, node.getAST());
          setProperty(node, outerQualifier);
        }
      }
      return true;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
      Expression initializer = node.getInitializer();
      if (initializer != null) {
        initializer.accept(this);
      }
      return false;
    }

    /**
     * Returns the full name for a given name reference and containing type.
     */
    private Name makeQualifiedName(SimpleName name, VariableSymbol sym,
        TypeSymbol currentType, AST ast) {
      Name qualifier = makeOuterQualifier(sym, currentType, ast);
      QualifiedName fullName = ast.newQualifiedName(qualifier, NodeCopier.copySubtree(ast, name));
      Types.addBinding(fullName, sym.getBinding());
      return fullName;
    }

    private Name makeOuterQualifier(Symbol sym, TypeSymbol currentType, AST ast) {
      Name outerName = makeOuterName(currentType, ast);
      IVariableBinding outerVar = Types.getVariableBinding(outerName);
      while ((currentType = currentType.getDeclaringClass()) != null &&
          !currentType.getScope().owns(sym) &&
          !currentType.equals(sym) && !currentType.getType().isTopLevel() &&
          !Modifier.isStatic(currentType.getBinding().getModifiers())) {
        outerName = ast.newQualifiedName(outerName, makeOuterName(currentType, ast));
        Types.addBinding(outerName, outerVar);
      }
      return outerName;
    }

    private SimpleName makeOuterName(TypeSymbol currentType, AST ast) {
      for (Symbol sym : currentType.getScope().getMembers()) {
        if (sym instanceof VariableSymbol && sym.getName().startsWith("this$")) {
          IVariableBinding outerVar = (IVariableBinding) sym.getBinding();
          assert outerVar.getType().isEqualTo(currentType.getDeclaringClass().getType());
          SimpleName name = ast.newSimpleName(sym.getName());
          Types.addBinding(name, outerVar);
          return name;
        }
      }
      throw new AssertionError("no outer field in scope");
    }
  }
}
