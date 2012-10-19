/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.devtools.j2cpp.types.Types;

import com.google.devtools.j2objc.util.DeadCodeMap;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Updates the Java AST to remove methods and classes reported as dead
 * by a ProGuard usage report.
 *
 * @author Daniel Connelly
 */
public class DeadCodeEliminator extends ErrorReportingASTVisitor {

  private static final Joiner innerClassJoiner = Joiner.on('$');

  private static final String WEAK = Weak.class.getName();
  private static final String WEAK_OUTER = WeakOuter.class.getName();

  private static int classCount = 0;
  private static String generateClassName() {
    return "J2OBJC_DUMMY_CLASS_" + (classCount++);
  }

  // Keep track of generated method declarations so we don't delete them.
  private final Set<MethodDeclaration> generatedMethods = Sets.newHashSet();

  // Keep track of generated Type instances so we can resolve their bindings.
  private final Map<Type, ITypeBinding> generatedTypes = Maps.newHashMap();

  private final DeadCodeMap deadCodeMap;

  public DeadCodeEliminator(DeadCodeMap deadCodeMap) {
    this.deadCodeMap = deadCodeMap;
  }

  // =========================================================================
  // Top-level elimination

  @SuppressWarnings("unchecked")
  @Override
  public void endVisit(TypeDeclaration node) {
    ITypeBinding binding = node.resolveBinding();
    List<BodyDeclaration> body = node.bodyDeclarations();
    eliminateDeadCode(node.resolveBinding(), node.bodyDeclarations());

    if (!node.isInterface() && !Modifier.isAbstract(node.getModifiers())) {
      generateMissingMethods(node.getAST(), binding, body);
    }

    ITypeBinding clazz = node.resolveBinding();
    ITypeBinding superClass = clazz.getSuperclass();
    if (!clazz.isInterface() && !clazz.isAnonymous() && superClass != null
        && !getConstructors(clazz).hasNext()) {
      Iterator<IMethodBinding> superConstructors = getVisible(getConstructors(superClass));
      if (superConstructors.hasNext() && !getWithArity(0, superConstructors).hasNext()) {
        generateConstructor(node);
      }
    }

    finishElimination();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void endVisit(EnumDeclaration node) {
    ITypeBinding binding = node.resolveBinding();
    List<BodyDeclaration> body = node.bodyDeclarations();
    eliminateDeadCode(binding, body);
    generateMissingMethods(node.getAST(), binding, body);
    if (deadCodeMap.isDeadClass(Types.getSignature(node.resolveBinding()))) {
      // Dead enum means none of the constants are ever used, so they can all be deleted.
      node.enumConstants().clear();
    }
    finishElimination();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void endVisit(AnnotationTypeDeclaration node) {
    eliminateDeadCode(node.resolveBinding(), node.bodyDeclarations());
    // All annotations are stripped, so we can remove all annotation members.
    removeAnnotationMembers(node.bodyDeclarations());
    finishElimination();
  }

  @Override
  public void endVisit(ImportDeclaration node) {
    IBinding binding = node.resolveBinding();
    if (binding instanceof IMethodBinding) {
      // Remove static imports for dead methods
      IMethodBinding method = (IMethodBinding) binding;
      if (allMethodsDeadWithName(method.getDeclaringClass(), getProGuardName(method))) {
        node.delete();
      }
    } else if (binding instanceof IVariableBinding) {
      // Remove static imports for dead non-constant fields
      IVariableBinding var = (IVariableBinding) binding;
      String clazz = Types.getSignature(var.getDeclaringClass());
      String name = var.getName();
      if (deadCodeMap.isDeadField(clazz, name) && var.getConstantValue() == null) {
        node.delete();
      }
    }
    // Skip on-demand imports
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {
    ITypeBinding binding = node.resolveBinding();
    @SuppressWarnings("unchecked")
    List<BodyDeclaration> body = node.bodyDeclarations();
    eliminateDeadCode(binding, body);
    generateMissingMethods(node.getAST(), binding, body);
    finishElimination();
  }

  @Override
  public void endVisit(MarkerAnnotation annotation) {
    removeAnnotation(annotation);
  }

  @Override
  public void endVisit(NormalAnnotation annotation) {
    removeAnnotation(annotation);
  }

  @Override
  public void endVisit(SingleMemberAnnotation annotation) {
    removeAnnotation(annotation);
  }

  /**
   * Remove dead members from a type.
   */
  private void eliminateDeadCode(ITypeBinding type, List<BodyDeclaration> body) {
    String clazz = Types.getSignature(type);
    removeDeadMethods(clazz, body);
    removeDeadFields(clazz, body);
    if (deadCodeMap.isDeadClass(clazz)) {
      removeInitializerBlocks(body);
    }
    initializeFinalFields(body);
  }

  /**
   * Per-type cleanup.
   */
  private void finishElimination() {
    generatedTypes.clear();
    generatedMethods.clear();
  }

  // =========================================================================
  // Methods

  /**
   * Remove dead methods from a type's body declarations.
   */
  private void removeDeadMethods(String clazz, List<BodyDeclaration> declarations) {
    Iterator<BodyDeclaration> declarationsIter = declarations.iterator();
    while (declarationsIter.hasNext()) {
      BodyDeclaration declaration = declarationsIter.next();
      if (declaration instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) declaration;
        // Generated methods aren't dead--they satisfy the inheritance hierarchy.
        if (!generatedMethods.contains(method)) {
          IMethodBinding binding = method.resolveBinding();
          String name = getProGuardName(binding);
          String signature = Types.getSignature(binding);
          if (deadCodeMap.isDeadMethod(clazz, name, signature)) {
            declarationsIter.remove();
          }
        }
      }
    }
  }

  /**
   * Generate stubs for required methods not yet implemented by a type.
   * Also adds an import statement if necessary.
   */
  private void generateMissingMethods(AST ast, ITypeBinding type, List<BodyDeclaration> body) {
    List<List<IMethodBinding>> groups =
        groupOverrideEquivalentMethods(getVisibleMethods(type));
    for (List<IMethodBinding> group : groups) {
      assert group.size() > 0;
      // Constructors handled elsewhere
      if (!group.get(0).isConstructor() && getConcreteMethod(group) == null) {
        // Must generate a concrete implementation for this signature.
        IMethodBinding method = group.get(0);
        generateMethodStub(ast, type, body, method, createReturnType(ast, type, body, group));
      }
    }
  }

  /**
   * Returns a non-abstract method from a list of override-equivalent methods
   * that is return-type substitutable for all the others and has a throws
   * clause that is compatible with all the others, or null if none exists.
   */
  private IMethodBinding getConcreteMethod(List<IMethodBinding> overrideEquivalentMethods) {
    // One of the following must be true:
    // 1. The group contains exactly one method, which belongs to the base class.
    if (overrideEquivalentMethods.size() == 1
        && !Modifier.isAbstract(overrideEquivalentMethods.get(0).getModifiers())) {
      return overrideEquivalentMethods.get(0);
    }
    // 2. The group contains some methods, none of which belong to the base class,
    //    and at most one of which is not abstract.
    IMethodBinding concrete = null;
    for (IMethodBinding method : overrideEquivalentMethods) {
      int modifiers = method.getModifiers();
      if (!Modifier.isAbstract(modifiers) && !Modifier.isNative(modifiers)) {
        if (concrete == null) {
          concrete = method;
        } else {
          throw new AssertionError("Can't inherit multiple concrete methods with same signature");
        }
      }
    }
    if (concrete == null) {
      return null;
    }
    // If the group contains a non-abstract method, for it to satisfy all of
    // the override-equivalent method signatures, it must be return-type
    // substitutable for all the others and have a compatible throws clause.
    // http://docs.oracle.com/javase/specs/jls/se5.0/html/classes.html#8.4.8.4
    for (IMethodBinding inherited : overrideEquivalentMethods) {
      if (!isSubstitutableBy(concrete.getReturnType()).apply(inherited.getReturnType())
          || !compatibleThrowsClauses(concrete, inherited)) {
        return null;
      }
    }
    return concrete;
  }

  /**
   * Determines whether one type is return-type-substitutable by another, as
   * determined by the Java Language Specification 3.0, section 8.4.5.
   */
  private static final Predicate<ITypeBinding> isSubstitutableBy(final ITypeBinding subtype) {
    return new Predicate<ITypeBinding>() {
      @Override
      public boolean apply(ITypeBinding supertype) {
        // http://docs.oracle.com/javase/specs/jls/se5.0/html/classes.html#296201
        return subtype.isPrimitive() && subtype.isEqualTo(supertype)
            || subtype.isSubTypeCompatible(supertype)
            || subtype.isEqualTo(supertype.getErasure())
            || subtype.getName().equals("void") && supertype.getName().equals("void");
      }
    };
  }

  /**
   * Determines whether an overriding method has a throws clause that does not
   * conflict with the throws clause of another method, as determined by JLS3,
   * section 8.4.6.
   */
  private boolean compatibleThrowsClauses(IMethodBinding overrider, IMethodBinding overridee) {
    for (ITypeBinding type1 : overrider.getExceptionTypes()) {
      // Overriding methods can only declare all or a subset of the checked
      // exceptions declared by the overridden method.  Therefore, each
      // exception declared by the overriding method must be equal to or a
      // subtype of an exception declared by the overridden method.
      // http://docs.oracle.com/javase/specs/jls/se5.0/html/classes.html#308526
      boolean present = false;
      for (ITypeBinding type2 : overridee.getExceptionTypes()) {
        if (type1.isSubTypeCompatible(type2)) {
          present = true;
          break;
        }
      }
      if (!present) {
        return false;
      }
    }
    return true;
  }

  /**
   * Create a Type instance that is return-type-substitutable for all the
   * specified methods.
   */
  private Type createReturnType(
      AST ast, ITypeBinding scope, List<BodyDeclaration> scopeBody, List<IMethodBinding> methods) {
    // Compilation will have checked that these methods can all be satisfied
    // by one return type.
    List<ITypeBinding> returnTypes = Lists.newArrayList();
    for (IMethodBinding method : methods) {
      returnTypes.add(method.getReturnType());
    }
    return createType(ast, scope, scopeBody, returnTypes);
  }

  /**
   * Group a list of methods by override-equivalency, as defined by JLS3, section 8.4.2.
   */
  private List<List<IMethodBinding>> groupOverrideEquivalentMethods(List<IMethodBinding> methods) {
    List<List<IMethodBinding>> groups = Lists.newArrayList();
    for (IMethodBinding method : methods) {
      List<IMethodBinding> group = null;
      for (List<IMethodBinding> candidate : groups) {
        IMethodBinding first = candidate.get(0);
        // Two methods are override-equivalent if either is a subsignature of the other.
        // http://docs.oracle.com/javase/specs/jls/se5.0/html/classes.html#38649
        if (isSubsignature(method, first) || isSubsignature(first, method)) {
          group = candidate;
          break;
        }
      }
      if (group != null) {
        group.add(method);
      } else {
        groups.add(Lists.newArrayList(method));
      }
    }
    return groups;
  }

  /**
   * Determines whether one method is a subsignature of another, as defined by JLS3,
   * section 8.4.2.
   */
  private boolean isSubsignature(IMethodBinding submethod, IMethodBinding supermethod) {
    if (submethod.isSubsignature(supermethod)) {
      return true;
    }
    // IMethodBinding#isSubsignature doesn't seem to be checking subsignatures
    // correctly when comparing two non-generic methods belonging to different
    // generic classes with the same type arguments, so I'm checking this case
    // manually.
    // http://docs.oracle.com/javase/specs/jls/se5.0/html/classes.html#38649
    if (!submethod.getName().equals(supermethod.getName())
        || submethod.getParameterTypes().length != supermethod.getParameterTypes().length
        || submethod.getTypeParameters().length != supermethod.getTypeParameters().length) {
      return false;
    }
    for (int i = 0; i < submethod.getParameterTypes().length; i++) {
      if (!submethod.getParameterTypes()[i].isEqualTo(supermethod.getParameterTypes()[i])) {
        return false;
      }
    }
    for (int i = 0; i < submethod.getTypeParameters().length; i++) {
      if (!submethod.getTypeParameters()[i].isEqualTo(supermethod.getParameterTypes()[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a list of all the non-dead non-private instance methods of a type
   * (including inherited methods).
   */
  private List<IMethodBinding> getVisibleMethods(ITypeBinding type) {
    List<IMethodBinding> methods = Lists.newArrayList();
    String clazz = Types.getSignature(type);
    for (IMethodBinding method : type.getDeclaredMethods()) {
      int modifiers = method.getModifiers();
      if (!deadCodeMap.isDeadMethod(clazz, getProGuardName(method), Types.getSignature(method))
          && !Modifier.isPrivate(modifiers)
          && !Modifier.isStatic(modifiers)) {
        methods.add(method);
      }
    }
    List<IMethodBinding> inherited = Lists.newArrayList();
    if (type.getSuperclass() != null) {
      collectVisibleMethods(type.getSuperclass(), inherited, methods);
    }
    for (ITypeBinding intrface : type.getInterfaces()) {
      collectVisibleMethods(intrface, inherited, methods);
    }
    methods.addAll(inherited);
    return methods;
  }

  /**
   * {@link #getVisibleMethods(ITypeBinding)}
   */
  private void collectVisibleMethods(
      ITypeBinding type, List<IMethodBinding> collected, List<IMethodBinding> overrides) {
    for (IMethodBinding inheritedMethod : getVisibleMethods(type)) {
      if (!hasOverride(overrides, inheritedMethod)) {
        collected.add(inheritedMethod);
      }
    }
  }

  /**
   * Determines whether a method is overridden by any of a list of other methods.
   */
  private boolean hasOverride(List<IMethodBinding> methods, IMethodBinding otherMethod) {
    for (IMethodBinding method : methods) {
      if (method.overrides(otherMethod)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add a method stub, the body of which throws an assertion error, to a type.
   */
  @SuppressWarnings("unchecked")
  private void generateMethodStub(
      AST ast,
      ITypeBinding scope,
      List<BodyDeclaration> scopeBody,
      IMethodBinding method,
      Type returnType) {
    MethodDeclaration decl = ast.newMethodDeclaration();
    decl.setName(ast.newSimpleName(method.getName()));

    // Return type
    decl.setReturnType2(returnType);

    // Generic type
    for (ITypeBinding typeParamBinding : method.getTypeParameters()) {
      TypeParameter typeParam = ast.newTypeParameter();
      typeParam.setName(ast.newSimpleName(typeParamBinding.getName()));
      for (ITypeBinding typeBound : typeParamBinding.getTypeBounds()) {
        typeParam.typeBounds().add(createType(ast, scope, typeBound));
      }
      decl.typeParameters().add(typeParam);
    }

    // Parameters
    int paramCount = 0;
    for (ITypeBinding paramBinding : method.getParameterTypes()) {
      SingleVariableDeclaration var = ast.newSingleVariableDeclaration();

      // Binding doesn't track original parameter name; generate new parameter names.
      String paramName = "arg" + (paramCount++);

      var.setName(ast.newSimpleName(paramName));
      var.setType(createType(ast, scope, paramBinding));
      decl.parameters().add(var);
    }

    // Modifiers
    int modifiers = method.getModifiers();
    // Always make the new method public.  Even if this method overrides a
    // protected method, it might also need to implement an interface.
    decl.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
    if (Modifier.isStrictfp(modifiers)){
      decl.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STRICTFP_KEYWORD));
    }
    if (Modifier.isSynchronized(modifiers)) {
      decl.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD));
    }

    // Body
    Block block = ast.newBlock();
    decl.setBody(block);
    addAssertionError(block);

    // Add to type
    scopeBody.add(decl);
    generatedMethods.add(decl);
  }

  /**
   * Add a thrown AssertionError statement to a block.
   */
  @SuppressWarnings("unchecked")
  private void addAssertionError(Block block) {
    AST ast = block.getAST();

    ThrowStatement throwStatement = ast.newThrowStatement();
    block.statements().add(throwStatement);

    ClassInstanceCreation newException = ast.newClassInstanceCreation();
    throwStatement.setExpression(newException);

    Type assertionError = ast.newSimpleType(ast.newSimpleName("AssertionError"));
    newException.setType(assertionError);

    StringLiteral assertionDescription = ast.newStringLiteral();
    assertionDescription.setLiteralValue("Cannot invoke dead method");
    newException.arguments().add(assertionDescription);
  }

  // =========================================================================
  // Fields

  /**
   * Deletes non-constant dead fields from a type's body declarations list.
   */
  private void removeDeadFields(String clazz, List<BodyDeclaration> declarations) {
    Iterator<BodyDeclaration> declarationsIter = declarations.iterator();
    while (declarationsIter.hasNext()) {
      BodyDeclaration declaration = declarationsIter.next();
      if (declaration instanceof FieldDeclaration) {
        FieldDeclaration field = (FieldDeclaration) declaration;
        @SuppressWarnings("unchecked")
        Iterator<VariableDeclarationFragment> fragmentsIter = field.fragments().iterator();
        while (fragmentsIter.hasNext()) {
          VariableDeclarationFragment fragment = fragmentsIter.next();
          // Don't delete any constants because we can't detect their use.
          if (fragment.resolveBinding().getConstantValue() == null &&
              deadCodeMap.isDeadField(clazz, fragment.getName().getIdentifier())) {
            fragmentsIter.remove();
          }
        }
        if (field.fragments().isEmpty()) {
          declarationsIter.remove();
        }
      }
    }
  }

  /**
   * Initializes final instance fields in classes whose constructors have all
   * been deleted.  This must take place after dead methods are removed.
   */
  private void initializeFinalFields(List<BodyDeclaration> body) {
    Set<VariableDeclarationFragment> finalVars = Sets.newHashSet();
    for (BodyDeclaration declaration : body) {
      // If there's any non-generated constructor remaining, it must initialize
      // all final fields.
      if (declaration instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) declaration;
        if (!generatedMethods.contains(method) && method.isConstructor()) {
          return;
        }
      }
      // Collect final fields that lack initializers.
      if (declaration instanceof FieldDeclaration) {
        FieldDeclaration field = (FieldDeclaration) declaration;
        int modifiers = field.getModifiers();
        if (!Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
          @SuppressWarnings("unchecked")
          List<VariableDeclarationFragment> fragments = field.fragments();
          for (VariableDeclarationFragment fragment : fragments) {
            if (fragment.getInitializer() == null) {
              finalVars.add(fragment);
            }
          }
        }
      }
    }
    // If we get this far, there are no explicit constructors left.
    for (VariableDeclarationFragment var : finalVars) {
      if (var.getInitializer() == null) {
        addFieldInitializer(var);
      }
    }
  }

  /**
   * Add an initializer expression to a field declaration fragment.
   */
  private void addFieldInitializer(VariableDeclarationFragment var) {
    AST ast = var.getAST();
    ITypeBinding type = ((FieldDeclaration) var.getParent()).getType().resolveBinding();
    var.setInitializer(getDefaultValue(ast, type));
  }

  /**
   * Determines whether every method in a class with a given name is dead.
   */
  private boolean allMethodsDeadWithName(ITypeBinding type, String name) {
    String clazz = Types.getSignature(type);
    for (IMethodBinding method : type.getDeclaredMethods()) {
      if (method.getName().equals(name)
          && !deadCodeMap.isDeadMethod(clazz, name, Types.getSignature(method))) {
        return false;
      }
    }
    return true;
  }

  // =========================================================================
  // Annotations

  /**
   * Remove all annotations except @Weak and @WeakOuter.
   */
  private void removeAnnotation(Annotation annotation) {
    String signature = Types.getSignature(annotation.resolveTypeBinding());
    if (!signature.equals(WEAK) && !signature.equals(WEAK_OUTER)) {
      annotation.delete();
    }
  }

  /**
   * Remove all AnnotationTypeMemberDeclarations from a list of declarations.
   */
  private void removeAnnotationMembers(List<BodyDeclaration> declarations) {
    Iterator<BodyDeclaration> declarationsIter = declarations.iterator();
    while (declarationsIter.hasNext()) {
      BodyDeclaration declaration = declarationsIter.next();
      if (declaration instanceof AnnotationTypeMemberDeclaration) {
        declarationsIter.remove();
      }
    }
  }

  // =========================================================================
  // Initializers

  /**
   * Remove initializer blocks from a list of declarations.
   */
  private void removeInitializerBlocks(List<BodyDeclaration> declarations) {
    Iterator<BodyDeclaration> declarationsIter = declarations.iterator();
    while (declarationsIter.hasNext()) {
      BodyDeclaration decl = declarationsIter.next();
      if (decl instanceof Initializer) {
        declarationsIter.remove();
      }
    }
  }

  // =========================================================================
  // Constructors

  /**
   * Adds a nullary constructor that invokes a superclass constructor with
   * default arguments.
   */
  @SuppressWarnings("unchecked")
  private void generateConstructor(TypeDeclaration node) {
    ITypeBinding clazz = node.resolveBinding();
    IMethodBinding superConstructor = getVisible(getConstructors(clazz.getSuperclass())).next();

    // Add an explicit constructor that calls super with suitable default arguments.
    AST ast = node.getAST();
    MethodDeclaration constructor = ast.newMethodDeclaration();
    constructor.setConstructor(true);
    constructor.setName(ast.newSimpleName(node.getName().getIdentifier()));
    constructor.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
    node.bodyDeclarations().add(constructor);

    Block block = ast.newBlock();
    constructor.setBody(block);
    SuperConstructorInvocation invocation = ast.newSuperConstructorInvocation();
    block.statements().add(invocation);
    addAssertionError(block);

    for (ITypeBinding type : superConstructor.getParameterTypes()) {
      Expression value = getDefaultValue(ast, type);
      CastExpression cast = ast.newCastExpression();
      cast.setExpression(value);
      cast.setType(createType(ast, clazz, type));
      invocation.arguments().add(cast);
    }
  }

  /**
   * Retrieve all non-dead constructors of a class.
   */
  private Iterator<IMethodBinding> getConstructors(final ITypeBinding clazz) {
    final String classSignature = Types.getSignature(clazz);
    return Iterators.filter(Iterators.forArray(clazz.getDeclaredMethods()),
        new Predicate<IMethodBinding>() {
      @Override public boolean apply(IMethodBinding method) {
        return method.isConstructor() && !deadCodeMap.isDeadMethod(
            classSignature, getProGuardName(method), Types.getSignature(method));
      }
    });
  }

  /**
   * Retrieve all non-private bindings.
   */
  private <T extends IBinding> Iterator<T> getVisible(Iterator<T> bindings) {
    return Iterators.filter(bindings, new Predicate<T>() {
      @Override public boolean apply(T binding) {
        return !Modifier.isPrivate(binding.getModifiers());
      }
    });
  }

  /**
   * Retrieve methods with the specified arity.
   */
  private Iterator<IMethodBinding> getWithArity(final int arity, Iterator<IMethodBinding> methods) {
    return Iterators.filter(methods, new Predicate<IMethodBinding>() {
      @Override public boolean apply(IMethodBinding method) {
        return method.getParameterTypes().length == arity;
      }
    });
  }

  /**
   * Creates an Expression with a suitable zero-value for the specified type.
   * TODO(user): this doesn't take into account @NonNull annotations.
   */
  private Expression getDefaultValue(AST ast, ITypeBinding type) {
    if (type.isPrimitive()) {
      if (type.getName().equals("boolean")) {
        return ast.newBooleanLiteral(false);
      }
      // All primitives types except boolean can be initialized as "0".
      return ast.newNumberLiteral("0");
    } else {
      return ast.newNullLiteral();
    }
  }

  // =========================================================================
  // Utilities

  /**
   * Get the ProGuard name of a method.
   * For non-constructors this is the method's name.
   * For constructors of top-level classes, this is the name of the class.
   * For constructors of inner classes, this is the $-delimited name path
   * from the outermost class declaration to the inner class declaration.
   */
  private String getProGuardName(IMethodBinding method) {
    if (!method.isConstructor() || !method.getDeclaringClass().isMember()) {
      return method.getName();
    }
    ITypeBinding parent = method.getDeclaringClass();
    assert parent != null;
    List<String> components = Lists.newLinkedList(); // LinkedList is faster for front-appending
    do {
      components.add(0, parent.getName());
      parent = parent.getDeclaringClass();
    } while (parent != null);
    return innerClassJoiner.join(components);
  }

  /**
   * Determines whether a type is visible in the scope of the specified context.
   */
  private boolean inScope(ITypeBinding type, ITypeBinding context) {
    return context.equals(type)
        || context.getSuperclass() != null && inScope(type, context.getSuperclass())
        || Iterables.any(Arrays.asList(context.getDeclaredTypes()), Predicates.equalTo(type));
  }

  /**
   * Create a new Type for the given type binding.  If the binding
   * represents a parameterized type reference, the returned Type
   * is a ParameterizedType with the same type parameters.  Otherwise
   * the returned Type is a SimpleType.
   */
  @SuppressWarnings("unchecked")
  private Type createType(AST ast, ITypeBinding scope, ITypeBinding type) {
    Type newType;
    if (type.isArray()) {
      newType = ast.newArrayType(
          createType(ast, scope, type.getElementType()), type.getDimensions());
    } else if (type.isPrimitive()) {
      newType = ast.newPrimitiveType(PrimitiveType.toCode(type.getName()));
    } else if (type.isWildcardType()) {
      WildcardType wildType = ast.newWildcardType();
      ITypeBinding bound = type.getBound();
      if (bound != null) {
        wildType.setBound(createType(ast, scope, bound), type.isUpperbound());
      }
      newType = wildType;
    } else if (!type.isParameterizedType()) {
      String name = inScope(type, scope) ? type.getName() : type.getQualifiedName();
      newType = ast.newSimpleType(ast.newName(name));
    } else {
      ITypeBinding erasure = type.getErasure();
      String name = inScope(type, scope) ? erasure.getName() : erasure.getQualifiedName();
      Type rawType = ast.newSimpleType(ast.newName(name));
      ParameterizedType paramType = ast.newParameterizedType(rawType);
      ITypeBinding[] typeArgs = type.getTypeArguments();
      for (ITypeBinding param : typeArgs) {
        paramType.typeArguments().add(createType(ast, scope, param));
      }
      newType = paramType;
    }
    generatedTypes.put(newType, type);
    return newType;
  }

  /**
   * Create a new Type instance that is return-type-substitutable for all
   * of a list of types.  If necessary, a new class is created and added
   * to the current scope.
   */
  private Type createType(
      AST ast, ITypeBinding scope, List<BodyDeclaration> scopeBody, List<ITypeBinding> types) {
    assert types.size() > 0;
    // Check for a type to return directly.
    ITypeBinding type = null;
    for (ITypeBinding otherType : types) {
      if (otherType.isPrimitive() || otherType.isArray() || otherType.isEnum()) {
        type = otherType;
        break;
      }
    }
    if (type != null) {
      // Assume that the other types are all compatible.  This should have
      // been verified by the Java compiler before dead code elimination.
      return createType(ast, scope, type);
    }
    // All classes or interfaces--try to find one that satisfies the others.
    for (ITypeBinding baseType : types) {
      if (Iterables.all(types, isSubstitutableBy(baseType))) {
        return createType(ast, scope, baseType);
      }
    }
    // No suitable class--make a new one.
    ITypeBinding parent = null;
    List<ITypeBinding> interfaces = Lists.newArrayList();
    for (ITypeBinding otherType : types) {
      if (otherType.isClass()) {
        if (parent == null) {
          parent = otherType;
        } else if (isSubstitutableBy(otherType).apply(parent)) {
          parent = otherType;
        } else if (isSubstitutableBy(parent).apply(otherType)) {
          // do nothing
        } else {
          throw new AssertionError("Impossible common type: unrelated classes");
        }
      } else {
        assert otherType.isInterface();
        interfaces.add(otherType);
      }
    }
    // Create the new abstract class and add it to the current scope.
    TypeDeclaration newClass = createClass(ast, scope, parent, interfaces);
    scopeBody.add(newClass);
    return ast.newSimpleType(ast.newName(newClass.getName().getFullyQualifiedName()));
  }

  /**
   * Creates a type declaration for a new class with the specified parent
   * and interfaces types.
   */
  @SuppressWarnings("unchecked")
  private TypeDeclaration createClass(
      AST ast, ITypeBinding scope, ITypeBinding superClass, List<ITypeBinding> interfaces) {
    TypeDeclaration decl = ast.newTypeDeclaration();
    if (superClass != null) {
      decl.setSuperclassType(createType(ast, scope, superClass));
    }
    for (ITypeBinding intrface : interfaces) {
      decl.superInterfaceTypes().add(createType(ast, scope, intrface));
    }
    decl.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
    decl.setName(ast.newSimpleName(generateClassName()));
    return decl;
  }

}
