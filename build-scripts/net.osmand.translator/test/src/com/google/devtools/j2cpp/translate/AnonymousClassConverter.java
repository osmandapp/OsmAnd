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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.devtools.j2cpp.sym.Symbols;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.GeneratedVariableBinding;
import com.google.devtools.j2cpp.types.NodeCopier;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;
import com.google.devtools.j2cpp.types.RenamedTypeBinding;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Converts anonymous classes into inner classes.  This includes creating
 * constructors that take the referenced final variables and parameters,
 * but references to other classes remain aren't modified.  By separating
 * anonymous class conversion from inner class extraction, each step can
 * be separately and more thoroughly verified.
 *
 * @author Tom Ball
 */
public class AnonymousClassConverter extends ClassConverter {

  public AnonymousClassConverter(CompilationUnit unit) {
    super(unit);
  }

  @Override
  public boolean visit(CompilationUnit node) {
    preProcessUnit(node);
    return true;
  }

  @VisibleForTesting
  void preProcessUnit(CompilationUnit node) {
    node.accept(new AnonymousClassRenamer());
  }

  // Uncomment to verify this translator's changes.
  //  @Override
  //  public void endVisit(CompilationUnit node) {
  //    Types.verifyNode(node);
  //  }

  private boolean isConstant(IVariableBinding var) {
    return var.getConstantValue() != null;
  }

  /**
   * Returns a list of nodes that reference any of the method's read-only
   * parameters and local variables.
   */
  private List<ReferenceDescription> findReferences(AnonymousClassDeclaration node,
      final Set<IVariableBinding> methodVars) {
    final List<ReferenceDescription> refs = Lists.newArrayList();
    MethodDeclaration decl = getEnclosingMethod(node);
    if (decl != null) {
      final IMethodBinding enclosingMethod = Types.getMethodBinding(decl);
      @SuppressWarnings("unchecked")
      List<BodyDeclaration> classMembers = node.bodyDeclarations(); // safe by definition
      for (BodyDeclaration member : classMembers) {
        member.accept(new ASTVisitor() {
          @Override
          public void endVisit(SimpleName node) {
            IVariableBinding varType = Types.getVariableBinding(node);
            if (varType != null) {
              if (methodVars.contains(varType.getVariableDeclaration())) {
                refs.add(new ReferenceDescription(node, varType, enclosingMethod));
              }
            }
          }

          @Override
          public boolean visit(AnonymousClassDeclaration node) {
            return false;
          }
        });
      }
    }
    return refs;
  }

  /**
   * Returns the set of read-only variables (parameters and local variables)
   * defined in the scope of the method enclosing a specified anonymous
   * class declaration.
   */
  private Set<IVariableBinding> getMethodVars(AnonymousClassDeclaration node) {
    final Set<IVariableBinding> methodVars = Sets.newHashSet();
    MethodDeclaration method = getEnclosingMethod(node);
    if (method != null) {
      @SuppressWarnings("unchecked")
      List<SingleVariableDeclaration> params = method.parameters(); // safe by definition
      for (SingleVariableDeclaration param : params) {
        IVariableBinding var = Types.getVariableBinding(param);
        assert var != null;
        if (Modifier.isFinal(var.getModifiers())) {
          methodVars.add(var);
        }
      }

      ASTNode lastNode = node;
      do {
        getMethodVars(method, lastNode, methodVars);
        lastNode = method;
        method = getEnclosingMethod(lastNode);
      } while (method != null);
    }
    return methodVars;
  }

  private void getMethodVars(MethodDeclaration method, ASTNode node,
      final Set<IVariableBinding> methodVars) {
    @SuppressWarnings("unchecked")
    List<Statement> statements = method.getBody().statements(); // safe by definition
    Statement enclosingStatement = getEnclosingStatement(node);
    for (Statement stmt : statements) {
      if (stmt == enclosingStatement) {
        // Local variables declared after this statement cannot be
        // referenced by the anonymous class.
        break;
      }
      stmt.accept(new ASTVisitor() {
        @Override
        public boolean visit(VariableDeclarationStatement node) {
          if (Modifier.isFinal(node.getModifiers())) {
            @SuppressWarnings("unchecked")  // safe by definition
            List<VariableDeclarationFragment> localVars = node.fragments();
            for (VariableDeclarationFragment localVar : localVars) {
              IVariableBinding var = Types.getVariableBinding(localVar);
              assert var != null;
              methodVars.add(var);
            }
          }
          return true;
        }

        @Override
        public boolean visit(SingleVariableDeclaration node) {
          IVariableBinding var = Types.getVariableBinding(node);
          assert var != null;
          if (Modifier.isFinal(var.getModifiers())) {
            methodVars.add(var);
          }
          return true;
        }

        @Override
        public boolean visit(AnonymousClassDeclaration node) {
          return false;
        }
      });
    }
  }

  /**
   * Returns the method surrounding an anonymous class declaration, or null if
   * the class is defined outside of a method.
   */
  private MethodDeclaration getEnclosingMethod(ASTNode node) {
    ASTNode parent = node.getParent();
    while (parent != null && !(parent instanceof MethodDeclaration)) {
      parent = parent.getParent();
    }
    return (MethodDeclaration) parent;
  }

  /**
   * Returns the method-level statement surrounding an anonymous class
   * declaration.
   */
  private Statement getEnclosingStatement(ASTNode node) {
    ASTNode lastChild = node;
    ASTNode parent = node.getParent();
    while (parent != null && !(parent instanceof MethodDeclaration)) {
      lastChild = parent;
      parent = parent.getParent();
    }
    return (Statement) lastChild;
  }

  /**
   * Convert the anonymous class into an inner class.  Fields are added for
   * final variables that are referenced, and a constructor is added.
   *
   * Note: endVisit is used for a depth-first traversal, to make it easier
   * to scan their containing nodes for references.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void endVisit(AnonymousClassDeclaration node) {
    ASTNode parent = node.getParent();
    ClassInstanceCreation newInvocation = null;
    EnumConstantDeclaration enumConstant = null;
    List<Expression> parentArguments;
    String newClassName;
    ITypeBinding innerType;
    boolean isStatic = staticParent(node);
    int modifiers = isStatic ? Modifier.STATIC : 0;
    if (parent instanceof ClassInstanceCreation) {
      newInvocation = (ClassInstanceCreation) parent;
      parentArguments = newInvocation.arguments();
      innerType = Types.getTypeBinding(newInvocation);
      newClassName = innerType.getName();
      innerType = RenamedTypeBinding.rename(newClassName, innerType.getDeclaringClass(), innerType,
          modifiers);
    } else if (parent instanceof EnumConstantDeclaration) {
      enumConstant = (EnumConstantDeclaration) parent;
      parentArguments = enumConstant.arguments();
      innerType = Types.getTypeBinding(node);
      newClassName = Types.getTypeBinding(node).getName();
      innerType = RenamedTypeBinding.rename(newClassName, innerType.getDeclaringClass(), innerType,
          modifiers);
    } else {
      throw new AssertionError(
          "unknown anonymous class declaration parent: " + parent.getClass().getName());
    }

    // Create a type declaration for this anonymous class.
    AST ast = node.getAST();
    TypeDeclaration typeDecl = ast.newTypeDeclaration();
    if (isStatic) {
      typeDecl.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
    }
    Types.addBinding(typeDecl, innerType);
    typeDecl.setName(ast.newSimpleName(newClassName));
    Types.addBinding(typeDecl.getName(), innerType);
    typeDecl.setSourceRange(node.getStartPosition(), node.getLength());

    Type superType = Types.makeType(Types.mapType(innerType.getSuperclass()));
    typeDecl.setSuperclassType(superType);
    for (ITypeBinding interfaceType : innerType.getInterfaces()) {
      typeDecl.superInterfaceTypes().add(Types.makeType(Types.mapType(interfaceType)));
    }

    for (Object bodyDecl : node.bodyDeclarations()) {
      BodyDeclaration decl = (BodyDeclaration) bodyDecl;
      typeDecl.bodyDeclarations().add(NodeCopier.copySubtree(ast, decl));
    }
    typeDecl.accept(new InitializationNormalizer());

    // Fix up references to external types, if necessary.
    Set<IVariableBinding> methodVars = getMethodVars(node);
    final List<ReferenceDescription> references = findReferences(node, methodVars);
    final List<Expression> invocationArgs = parentArguments;
    if (!references.isEmpty() || !invocationArgs.isEmpty()) {  // is there anything to fix-up?
      List<IVariableBinding> innerVars = getInnerVars(references);
      if (!innerVars.isEmpty() || !invocationArgs.isEmpty()) {
        GeneratedMethodBinding defaultConstructor = addInnerVars(typeDecl, innerVars, references,
            parentArguments);
        Types.addBinding(parent, defaultConstructor);
        for (IVariableBinding var : innerVars) {
          if (!isConstant(var)) {
            parentArguments.add(makeFieldRef(var, ast));
          }
        }
        assert defaultConstructor.getParameterTypes().length == parentArguments.size();
        typeDecl.accept(new ASTVisitor() {
          @Override
          public void endVisit(SimpleName node) {
            IVariableBinding var = Types.getVariableBinding(node);
            if (var != null) {
              for (ReferenceDescription ref : references) {
                if (var.isEqualTo(ref.binding)) {
                  if (ref.innerField != null) {
                    setProperty(node, makeFieldRef(ref.innerField, node.getAST()));
                  } else {
                    // In-line constant.
                    Object o = var.getConstantValue();
                    assert o != null;
                    Expression literal =
                        makeLiteral(o, var.getType().getQualifiedName(), node.getAST());
                    setProperty(node, literal);
                  }
                  return;
                }
              }
            }
          }
        });
      }
    }

    // If invocation, replace anonymous class invocation with the new constructor.
    if (newInvocation != null) {
      newInvocation.setAnonymousClassDeclaration(null);
      newInvocation.setType(Types.makeType(innerType));
      IMethodBinding oldBinding = Types.getMethodBinding(newInvocation);
      if (oldBinding == null) {
        oldBinding = newInvocation.resolveConstructorBinding();
      }
      if (oldBinding != null) {
        GeneratedMethodBinding invocationBinding = new GeneratedMethodBinding(oldBinding);
        invocationBinding.setDeclaringClass(innerType);
        Types.addBinding(newInvocation, invocationBinding);
      }
    } else {
      enumConstant.setAnonymousClassDeclaration(null);
    }

    // Add type declaration to enclosing type.
    ITypeBinding outerType = innerType.getDeclaringClass();
    if (outerType.isAnonymous()) {
      // Get outerType node.
      ASTNode n = parent.getParent();
      while (!(n instanceof AnonymousClassDeclaration) && !(n instanceof TypeDeclaration)) {
        n = n.getParent();
      }
      if (n instanceof AnonymousClassDeclaration) {
        AnonymousClassDeclaration outerDecl = (AnonymousClassDeclaration) n;
        outerDecl.bodyDeclarations().add(typeDecl);
      }
    } else {
      AbstractTypeDeclaration outerDecl =
          (AbstractTypeDeclaration) unit.findDeclaringNode(outerType);
      outerDecl.bodyDeclarations().add(typeDecl);
    }
    Symbols.scanAST(typeDecl);
    super.endVisit(node);
  }

  /**
   * Returns true if this anonymous class is defined in a static method or
   * used to initialize a static variable.
   */
  private boolean staticParent(AnonymousClassDeclaration node) {
    ITypeBinding type = Types.getTypeBinding(node);
    IMethodBinding declaringMethod = type.getDeclaringMethod();
    if (declaringMethod != null) {
      return Modifier.isStatic(declaringMethod.getModifiers());
    }
    ASTNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof Assignment) {
        Assignment assign = (Assignment) parent;
        IVariableBinding field = Types.getVariableBinding(assign.getLeftHandSide());
        if (field != null) {
          return Modifier.isStatic(field.getModifiers());
        }
      } else if (parent instanceof MethodDeclaration) {
        // TODO(user): Should set the declaringMethod on the expression
        // instead. See InnerClassExtractorTest.testNoOuterInStaticInitializer.
        assert ((MethodDeclaration) parent).getName().getIdentifier().equals("initialize");
        assert Modifier.isStatic(((MethodDeclaration) parent).getModifiers());
        return true;
      }
      parent = parent.getParent();
    }
    return false;
  }

  /**
   * Adds val$N instance fields to type so references to external
   * variables can be resolved.  Existing constructors are updated to
   * initialize these fields; if no constructor exists, one is added.
   */
  private GeneratedMethodBinding addInnerVars(TypeDeclaration node,
      List<IVariableBinding> innerVars, List<ReferenceDescription> references,
      List<Expression> invocationArguments) {
    @SuppressWarnings("unchecked")
    List<BodyDeclaration> members = node.bodyDeclarations(); // safe by definition
    List<IVariableBinding> innerFields = Lists.newArrayList();
    AST ast = node.getAST();
    ITypeBinding clazz = Types.getTypeBinding(node);

    for (IVariableBinding var : innerVars) {
      if (!isConstant(var)) {  // Constants are in-lined instead.
        ITypeBinding varType = var.getDeclaringClass();
        if (varType == null) {
          varType = var.getType();
        }
        if (Types.hasIOSEquivalent(varType)) {
          varType = Types.mapType(varType);
        }
        String fieldName = "val$" + var.getName();
        FieldDeclaration field = createField(fieldName, varType, clazz, ast);
        members.add(field);
        IVariableBinding fieldVar = Types.getVariableBinding(field.fragments().get(0));
        innerFields.add(fieldVar);
        for (ReferenceDescription ref : references) {
          if (ref.binding == var && ref.innerField == null) {
            ref.innerField = fieldVar;
          }
        }
      }
    }

    // Insert new parameters into constructor, if one was added by the
    // InitializationNormalizer from initializer blocks.
    boolean needsConstructor = true;
    GeneratedMethodBinding defaultConstructor = null;
    List<MethodDeclaration> enumConstructors = Lists.newArrayList();
    ITypeBinding enclosingClass = clazz.getDeclaringClass();
    for (BodyDeclaration member : members) {
      if (member instanceof MethodDeclaration && ((MethodDeclaration) member).isConstructor()) {
        if (argsMatch(invocationArguments, Types.getMethodBinding(member).getParameterTypes())) {
          MethodDeclaration constructor = (MethodDeclaration) member;
          needsConstructor = false;
          IMethodBinding oldBinding = Types.getMethodBinding(constructor);
          GeneratedMethodBinding newBinding = new GeneratedMethodBinding(oldBinding);
          Types.addBinding(constructor, newBinding);
          addInnerParameters(constructor, newBinding, innerFields, ast, true);
          defaultConstructor = newBinding;
          Symbols.scanAST(constructor);
          assert constructor.parameters().size() == defaultConstructor.getParameterTypes().length;
        }
        if (enclosingClass.isEnum()) {
          enumConstructors.add((MethodDeclaration) member);
        }
      }
    }

    if (!enumConstructors.isEmpty()) {
      for (MethodDeclaration constructor : enumConstructors) {
        GeneratedMethodBinding binding =
            new GeneratedMethodBinding(Types.getMethodBinding(constructor));
        if (!invocationArguments.isEmpty()) {
          // Remove super invocation added by InitializationNormalizer.
          @SuppressWarnings("unchecked")
          List<Statement> stmts = constructor.getBody().statements(); // safe by definition
          for (int i = 0; i < stmts.size(); i++) {
            if (stmts.get(i) instanceof SuperConstructorInvocation) {
              stmts.remove(i);
              break;
            }
          }

          // Update the binding to include the arguments,
          addArguments(invocationArguments, ast, clazz, constructor, binding);
          addInnerParameters(constructor, binding, innerFields, ast, true);
          Symbols.scanAST(constructor);
          Types.addBinding(constructor, binding);
        } else {
          defaultConstructor = binding;
        }
      }
      if (defaultConstructor == null) {
        defaultConstructor =
            new GeneratedMethodBinding(Types.getMethodBinding(enumConstructors.get(0)));
      }
    } else if (needsConstructor) {
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
      if (!invocationArguments.isEmpty()) {
        addArguments(invocationArguments, ast, clazz, constructor, binding);
      }
      addInnerParameters(constructor, binding, innerFields, ast, true);
      members.add(constructor);
      Symbols.scanAST(constructor);
      defaultConstructor = binding;
      assert constructor.parameters().size() == defaultConstructor.getParameterTypes().length;
    }

    assert defaultConstructor != null;
    return defaultConstructor;
  }

  private void addArguments(List<Expression> invocationArguments, AST ast, ITypeBinding clazz,
      MethodDeclaration constructor, GeneratedMethodBinding binding) {
    // Create a parameter list, based on the invocation arguments.
    @SuppressWarnings("unchecked") // safe by definition
    List<SingleVariableDeclaration> parameters = constructor.parameters();
    int parameterOffset = binding.getParameterTypes().length;
    for (int i = 0; i < invocationArguments.size(); i++) {
      Expression arg = invocationArguments.get(i);
      ITypeBinding argType = Types.getTypeBinding(arg);
      SimpleName paramName = ast.newSimpleName("arg$" + i);
      GeneratedVariableBinding paramBinding = new GeneratedVariableBinding(
          paramName.getIdentifier(), 0, argType, false, true, clazz, null);
      Types.addBinding(paramName, paramBinding);
      SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
      param.setName(paramName);
      param.setType(Types.makeType(argType));
      Types.addBinding(param, paramBinding);
      parameters.add(param);
      binding.addParameter(i + parameterOffset, argType);
    }

    // Add super constructor call, forwarding the invocation arguments.
    SuperConstructorInvocation superInvocation = ast.newSuperConstructorInvocation();
    @SuppressWarnings("unchecked")
    List<Expression> superArgs = superInvocation.arguments(); // safe by definition
    for (SingleVariableDeclaration param : parameters) {
      superArgs.add(NodeCopier.copySubtree(ast, param.getName()));
    }
    Types.addBinding(superInvocation,
        findSuperConstructorBinding(clazz.getSuperclass(), invocationArguments));
    @SuppressWarnings("unchecked")
    List<Statement> statements = constructor.getBody().statements(); // safe by definition
    statements.add(superInvocation);
  }

  private boolean argsMatch(List<Expression> invocationArguments, ITypeBinding[] parameterTypes) {
    if (invocationArguments.size() != parameterTypes.length) {
      return false;
    }
    for (int i = 0; i < parameterTypes.length; i++) {
      ITypeBinding argType = Types.getTypeBinding(invocationArguments.get(i));
      if (!parameterTypes[i].isEqualTo(argType)) {
        return false;
      }
    }
    return true;
  }

  private IMethodBinding findSuperConstructorBinding(ITypeBinding clazz,
      List<Expression> superArgs) {
    if (clazz == null) {
      throw new AssertionError("could not find constructor");
    }
    outer: for (IMethodBinding m : clazz.getDeclaredMethods()) {
      if (m.isConstructor()) {
        ITypeBinding[] paramTypes = m.getParameterTypes();
        if (superArgs.size() == paramTypes.length) {
          for (int i = 0; i < paramTypes.length; i++) {
            ITypeBinding argType = Types.getTypeBinding(superArgs.get(i)).getErasure();
            if (!argType.isAssignmentCompatible(paramTypes[i].getErasure())) {
              continue outer;
            }
          }
          return m;
        }
      }
    }
    return findSuperConstructorBinding(clazz.getSuperclass(), superArgs);
  }

  /**
   * Returns a literal node for a specified constant value.
   */
  private Expression makeLiteral(Object value, String typeName, AST ast) {
    Expression literal;
    if (value instanceof Boolean) {
      literal = ast.newBooleanLiteral((Boolean) value);
    } else if (value instanceof Character) {
      CharacterLiteral c = ast.newCharacterLiteral();
      c.setCharValue((Character) value);
      literal = c;
    } else if (value instanceof Number) {
      literal = ast.newNumberLiteral(value.toString());
    } else if (value instanceof String) {
        StringLiteral s = ast.newStringLiteral();
        s.setLiteralValue((String) value);
        literal = s;
    } else {
      throw new AssertionError("unknown constant type");
    }
    ITypeBinding type = ast.resolveWellKnownType(typeName);
    assert type != null : "unknown constant type";
    Types.addBinding(literal, type);
    return literal;
  }

  /**
   * Rename anonymous classes to class file-like $n names, where n is the
   * index of the number of anonymous classes for the parent type.  A stack
   * is used to ensure that anonymous classes defined inside of other
   * anonymous classes are numbered correctly.
   */
  static class AnonymousClassRenamer extends ASTVisitor {

    private static class Frame {
      int classCount = 0;
    }
    final Stack<Frame> classIndex = new Stack<Frame>();

    @Override
    public boolean visit(TypeDeclaration node) {
      return processType(node);
    }

    @Override
    public boolean visit(EnumDeclaration node) {
      return processType(node);
    }

    private boolean processType(AbstractTypeDeclaration node) {
      Frame parentFrame = classIndex.isEmpty() ? null : classIndex.peek();
      if (parentFrame != null) {
        String newName = node.getName().getIdentifier();
        ITypeBinding newBinding = renameClass(newName, Types.getTypeBinding(node));
        SimpleName nameNode = node.getAST().newSimpleName(newName);
        node.setName(nameNode);
        Types.addBinding(nameNode, newBinding);
        Types.addBinding(node, newBinding);
      }
      classIndex.push(new Frame());
      return true;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
      if (node.getAnonymousClassDeclaration() != null) {
        AnonymousClassDeclaration anonDecl = node.getAnonymousClassDeclaration();
        Frame parentFrame = classIndex.peek();

        String className = "$" + ++parentFrame.classCount;
        ITypeBinding innerType = renameClass(className, Types.getTypeBinding(anonDecl));
        Types.addBinding(anonDecl, innerType);

        node.setType(Types.makeType(innerType));
        IMethodBinding oldBinding = Types.getMethodBinding(node);
        GeneratedMethodBinding newConstructorBinding = new GeneratedMethodBinding("init", 0,
            node.getAST().resolveWellKnownType("void"), innerType, true, false, true);
        for (ITypeBinding paramType : oldBinding.getParameterTypes()) {
          newConstructorBinding.addParameter(paramType);
        }
        Types.addBinding(node, newConstructorBinding);
        classIndex.push(new Frame());
      }
      return true;
    }

    @Override
    public boolean visit(EnumConstantDeclaration node) {
      if (node.getAnonymousClassDeclaration() != null) {
        AnonymousClassDeclaration anonDecl = node.getAnonymousClassDeclaration();
        Frame parentFrame = classIndex.peek();

        String className = "$" + ++parentFrame.classCount;
        ITypeBinding innerType = renameClass(className, Types.getTypeBinding(anonDecl));
        Types.addBinding(anonDecl, innerType);

        classIndex.push(new Frame());
      }
      return true;
    }

    private ITypeBinding renameClass(String name, ITypeBinding oldBinding) {
      ITypeBinding outerType = Types.getRenamedBinding(oldBinding.getDeclaringClass());
      NameTable.rename(oldBinding, name);
      ITypeBinding newBinding = Types.renameTypeBinding(name, outerType, oldBinding);
      assert newBinding.getName().equals(name);
      return newBinding;
    }

    @Override
    public void endVisit(ClassInstanceCreation node) {
      if (node.getAnonymousClassDeclaration() != null) {
        classIndex.pop();
      }
    }

    @Override
    public void endVisit(TypeDeclaration node) {
      classIndex.pop();
    }

    @Override
    public void endVisit(EnumConstantDeclaration node) {
      if (node.getAnonymousClassDeclaration() != null) {
        classIndex.pop();
      }
    }
  }
}
