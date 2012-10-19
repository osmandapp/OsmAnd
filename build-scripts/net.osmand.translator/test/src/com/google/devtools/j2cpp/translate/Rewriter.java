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
import com.google.common.collect.Maps;

import com.google.devtools.j2cpp.sym.Symbols;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.GeneratedVariableBinding;
import com.google.devtools.j2cpp.types.NodeCopier;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;
import com.google.devtools.j2cpp.types.IOSArrayTypeBinding;
import com.google.devtools.j2cpp.types.IOSMethodBinding;
import com.google.devtools.j2cpp.types.IOSVariableBinding;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor; 

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Rewrites the Java AST to replace difficult to translate code with methods
 * that are more Objective C/iOS specific. For example, Objective C doesn't have
 * the concept of class variables, so they need to be replaced with static
 * accessor methods referencing private static data.
 *
 * @author Tom Ball
 */
public class Rewriter extends ErrorReportingASTVisitor {

  /**
   * The list of Objective-C type qualifier keywords.
   */
  private static final List<String> typeQualifierKeywords = Lists.newArrayList("in", "out",
      "inout", "oneway", "bycopy", "byref");

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(TypeDeclaration node) {
    return visitType(
        node.getAST(), Types.getTypeBinding(node), node.bodyDeclarations(), node.getModifiers());
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(AnonymousClassDeclaration node) {
    return visitType(
        node.getAST(), Types.getTypeBinding(node), node.bodyDeclarations(), Modifier.NONE);
  }

  private boolean visitType(
      AST ast, ITypeBinding typeBinding, List<BodyDeclaration> members, int modifiers) {
    ITypeBinding[] interfaces = typeBinding.getInterfaces();
    if (interfaces.length > 0) {
      if (Modifier.isAbstract(modifiers)) {

        // Add any interface methods that aren't defined by this abstract type.
        // Obj-C needs these to verify that the generated class implements the
        // interface/protocol.
        for (ITypeBinding intrface : interfaces) {
          // Collect needed methods from this interface and all super-interfaces.
          Queue<ITypeBinding> interfaceQueue = new LinkedList<ITypeBinding>();
          Set<IMethodBinding> interfaceMethods = new LinkedHashSet<IMethodBinding>();
          interfaceQueue.add(intrface);
          while ((intrface = interfaceQueue.poll()) != null) {
            interfaceMethods.addAll(Arrays.asList(intrface.getDeclaredMethods()));
            interfaceQueue.addAll(Arrays.asList(intrface.getInterfaces()));
          }
          addMissingMethods(ast, typeBinding, interfaceMethods, members);
        }
      } else if (!typeBinding.isInterface()) {
        // Check for methods that the type *explicitly implements* for cases
        // where a superclass provides the implementation.  For example, many
        // Java interfaces define equals(Object) to provide documentation, which
        // a class doesn't need to implement in Java, but does in Obj-C.  These
        // classes need a forwarding method to pass the Obj-C compiler.
        Set<IMethodBinding> interfaceMethods = new LinkedHashSet<IMethodBinding>();
        for (ITypeBinding intrface : interfaces) {
          interfaceMethods.addAll(Arrays.asList(intrface.getDeclaredMethods()));
        }
        addForwardingMethods(ast, typeBinding, interfaceMethods, members);
      }
    }

    removeSerialization(members);

    renameDuplicateMembers(typeBinding);
    return true;
  }

  private void addMissingMethods(
      AST ast, ITypeBinding typeBinding, Set<IMethodBinding> interfaceMethods,
      List<BodyDeclaration> decls) {
    for (IMethodBinding interfaceMethod : interfaceMethods) {
      if (!isMethodImplemented(typeBinding, interfaceMethod, decls)) {
        addAbstractMethod(ast, typeBinding, interfaceMethod, decls);
      }
    }
  }

  private void addForwardingMethods(
      AST ast, ITypeBinding typeBinding, Set<IMethodBinding> interfaceMethods,
      List<BodyDeclaration> decls) {
    for (IMethodBinding interfaceMethod : interfaceMethods) {
      String methodName = interfaceMethod.getName();
      // These are the only java.lang.Object methods that are both overridable
      // and translated to Obj-C.
      if (methodName.matches("equals|hashCode|toString")) {
        if (!isMethodImplemented(typeBinding, interfaceMethod, decls)) {
          addForwardingMethod(ast, typeBinding, interfaceMethod, decls);
        }
      }
    }
  }

  private boolean isMethodImplemented(
      ITypeBinding type, IMethodBinding interfaceMethod, List<BodyDeclaration> decls) {
    for (BodyDeclaration decl : decls) {
      if (!(decl instanceof MethodDeclaration)) {
        continue;
      }

      if (Types.getMethodBinding(decl).isSubsignature(interfaceMethod)) {
        return true;
      }
    }
    return isMethodImplemented(type.getSuperclass(), interfaceMethod);
  }

  private boolean isMethodImplemented(ITypeBinding type, IMethodBinding method) {
    if (type == null || type.getQualifiedName().equals("java.lang.Object")) {
      return false;
    }

    for (IMethodBinding m : type.getDeclaredMethods()) {
      if (method.isSubsignature(m) ||
          (method.getName().equals(m.getName()) &&
          method.getReturnType().getErasure().isEqualTo(m.getReturnType().getErasure()) &&
          Arrays.equals(method.getParameterTypes(), m.getParameterTypes()))) {
        return true;
      }
    }

    return isMethodImplemented(type.getSuperclass(), method);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    // change the names of any methods that conflict with NSObject messages
    IMethodBinding binding = Types.getMethodBinding(node);
    String name = binding.getName();
    renameReservedNames(name, binding);

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> params = node.parameters();
    for (int i = 0; i < params.size(); i++) {
      // Change the names of any parameters that are type qualifier keywords.
      SingleVariableDeclaration param = params.get(i);
      name = param.getName().getIdentifier();
      if (typeQualifierKeywords.contains(name)) {
        IVariableBinding varBinding = param.resolveBinding();
        NameTable.rename(varBinding, name + "Arg");
      }
    }
    return true;
  }

  @Override
  public boolean visit(MethodInvocation node) {
    boolean visitChildren = true;
    if (rewriteSystemOut(node)) {
      visitChildren =  false;
    }
    if (rewriteStringFormat(node)) {
      visitChildren =  false;
    }
    IMethodBinding binding = Types.getMethodBinding(node);
    String name = binding.getName();
    renameReservedNames(name, binding);
    return visitChildren;
  }

  @Override
  public boolean visit(SuperMethodInvocation node) {
    renameReservedNames(node.getName().getIdentifier(), Types.getMethodBinding(node));
    return true;
  }

  private void renameReservedNames(String name, IMethodBinding binding) {
    if (NameTable.isReservedName(name)) {
      NameTable.rename(binding, name + "__");
    }
  }

  @Override
  public boolean visit(FieldDeclaration node) {
    int mods = node.getModifiers();
    if (Modifier.isStatic(mods)) {
      ASTNode parent = node.getParent();
      @SuppressWarnings("unchecked")
      List<BodyDeclaration> classMembers =
          parent instanceof AbstractTypeDeclaration ?
              ((AbstractTypeDeclaration) parent).bodyDeclarations() :
              ((AnonymousClassDeclaration) parent).bodyDeclarations();  // safe by specification
      int indexOfNewMember = classMembers.indexOf(node) + 1;

      @SuppressWarnings("unchecked")
      List<VariableDeclarationFragment> fragments = node.fragments(); // safe by specification
      for (VariableDeclarationFragment var : fragments) {
        IVariableBinding binding = Types.getVariableBinding(var);
        if (Types.isPrimitiveConstant(binding) && Modifier.isPrivate(binding.getModifiers())) {
          // Don't define accessors for private constants, since they can be
          // directly referenced.
          continue;
        }

        // rename varName to varName_, per Obj-C style guide
        SimpleName oldName = var.getName();
        ITypeBinding type = ((AbstractTypeDeclaration) node.getParent()).resolveBinding();
        String varName = NameTable.getStaticVarQualifiedName(type, oldName.getIdentifier());
        NameTable.rename(binding, varName);
        ITypeBinding typeBinding = binding.getType();
        var.setExtraDimensions(0);  // if array, type was corrected above

        // add accessor(s)
        if (needsReader(var, classMembers)) {
          classMembers.add(indexOfNewMember++, makeStaticReader(var, mods));
        }
        if (!Modifier.isFinal(node.getModifiers()) && needsWriter(var, classMembers)) {
          classMembers.add(
              indexOfNewMember++,
              makeStaticWriter(var, oldName.getIdentifier(), node.getType(), mods));
        }

        // move non-constant initialization to init block
        Expression initializer = var.getInitializer();
        if (initializer != null && initializer.resolveConstantExpressionValue() == null) {
          var.setInitializer(null);

          AST ast = var.getAST();
          SimpleName newName = ast.newSimpleName(varName);
          Types.addBinding(newName, binding);
          Assignment assign = ast.newAssignment();
          assign.setLeftHandSide(newName);
          Expression newInit = NodeCopier.copySubtree(ast, initializer);
          assign.setRightHandSide(newInit);
          Types.addBinding(assign, typeBinding);

          Block initBlock = ast.newBlock();
          @SuppressWarnings("unchecked")
          List<Statement> stmts = initBlock.statements(); // safe by definition
          stmts.add(ast.newExpressionStatement(assign));
          Initializer staticInitializer = ast.newInitializer();
          staticInitializer.setBody(initBlock);
          @SuppressWarnings("unchecked")
          List<IExtendedModifier> initMods = staticInitializer.modifiers(); // safe by definition
          initMods.add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
          classMembers.add(indexOfNewMember++, staticInitializer);
        }
      }
    }
    return true;
  }

  @Override
  public boolean visit(Block node) {
    // split array declarations so that initializers are in separate statements.
    @SuppressWarnings("unchecked")
    List<Statement> stmts = node.statements(); // safe by definition
    int n = stmts.size();
    for (int i = 0; i < n; i++) {
      Statement s = stmts.get(i);
      if (s instanceof VariableDeclarationStatement) {
        VariableDeclarationStatement var = (VariableDeclarationStatement) s;
        Map<VariableDeclarationFragment, Expression> initializers = Maps.newLinkedHashMap();
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = var.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
          ITypeBinding varType = Types.getTypeBinding(fragment);
          if (varType.isArray()) {
            fragment.setExtraDimensions(0);
            Expression initializer = fragment.getInitializer();
            if (initializer != null) {
              initializers.put(fragment, initializer);
              if (initializer instanceof ArrayCreation) {
                ArrayCreation creator = (ArrayCreation) initializer;
                if (creator.getInitializer() != null) {
                  // replace this redundant array creation node with its
                  // rewritten initializer
                  initializer = creator.getInitializer();
                } else {
                  continue;
                }
              }
              if (initializer instanceof ArrayInitializer) {
                fragment.setInitializer(createIOSArrayInitializer(
                    Types.getTypeBinding(fragment), (ArrayInitializer) initializer));
              }
            }
          }
        }
      } else if (s instanceof ExpressionStatement &&
          ((ExpressionStatement) s).getExpression() instanceof Assignment) {
        Assignment assign = (Assignment) ((ExpressionStatement) s).getExpression();
        ITypeBinding assignType = Types.getTypeBinding(assign);
        if (assign.getRightHandSide() instanceof ArrayInitializer) {
          ArrayInitializer arrayInit = (ArrayInitializer) assign.getRightHandSide();
          assert assignType.isArray() : "array initializer assigned to non-array";
          assign.setRightHandSide(
              createIOSArrayInitializer(assignType, arrayInit));
        } else if (assign.getRightHandSide() instanceof ArrayCreation) {
          ArrayCreation arrayCreate = (ArrayCreation) assign.getRightHandSide();
          ArrayInitializer arrayInit = arrayCreate.getInitializer();
          if (arrayInit != null) {
            // Replace ArrayCreation node with its initializer.
            AST ast = node.getAST();
            Assignment newAssign = ast.newAssignment();
            Types.addBinding(newAssign, assignType);
            newAssign.setLeftHandSide(NodeCopier.copySubtree(ast, assign.getLeftHandSide()));
            newAssign.setRightHandSide(createIOSArrayInitializer(assignType, arrayInit));
            ((ExpressionStatement) s).setExpression(newAssign);
          }
        }
      }
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(LabeledStatement node) {
    Statement s = node.getBody();
    Statement statementBody = null;
    if (s instanceof DoStatement) {
      statementBody = ((DoStatement) s).getBody();
    } else if (s instanceof EnhancedForStatement) {
      statementBody = ((EnhancedForStatement) s).getBody();
    } else if (s instanceof ForStatement) {
      statementBody = ((ForStatement) s).getBody();
    } else if (s instanceof WhileStatement) {
      statementBody = ((WhileStatement) s).getBody();
    }
    if (statementBody != null) {
      AST ast = node.getAST();

      final boolean[] hasContinue = new boolean[1];
      final boolean[] hasBreak = new boolean[1];
      node.accept(new ASTVisitor() {
        @Override
        public void endVisit(ContinueStatement node) {
          if (node.getLabel() != null) {
            hasContinue[0] = true;
          }
        }
        @Override
        public void endVisit(BreakStatement node) {
          if (node.getLabel() != null) {
            hasBreak[0] = true;
          }
        }
      });

      List<Statement> stmts = null;
      if (hasContinue[0]) {
        if (statementBody instanceof Block) {
          // Add empty labeled statement as last block statement.
          stmts = ((Block) statementBody).statements();
          LabeledStatement newLabel = ast.newLabeledStatement();
          newLabel.setLabel(NodeCopier.copySubtree(ast, node.getLabel()));
          newLabel.setBody(ast.newEmptyStatement());
          stmts.add(newLabel);
        }
      }
      if (hasBreak[0]) {
        ASTNode parent = node.getParent();
        if (parent instanceof Block) {
          stmts = ((Block) parent).statements();
        } else {
          // Surround parent with block.
          Block block = ast.newBlock();
          stmts = block.statements();
          stmts.add((Statement) parent);

          // Replace parent in its statement list with new block.
          List<Statement> superStmts = ((Block) parent.getParent()).statements();
          for (int i = 0; i < superStmts.size(); i++) {
            if (superStmts.get(i) == parent) {
              superStmts.set(i, block);
              break;
            }
          }
          stmts = block.statements();
        }
        // Find node in statement list, and add empty labeled statement after it.
        for (int i = 0; i < stmts.size(); i++) {
          if (stmts.get(i) == node) {
            LabeledStatement newLabel = ast.newLabeledStatement();
            newLabel.setLabel(NodeCopier.copySubtree(ast, node.getLabel()));
            newLabel.setBody(ast.newEmptyStatement());
            stmts.add(i + 1, newLabel);
            break;
          }
        }
      }

      if (hasContinue[0] || hasBreak[0]) {
        // Replace this node with its statement, thus deleting the label.
        ASTNode parent = node.getParent();
        if (parent instanceof Block) {
          stmts = ((Block) parent).statements();
          for (int i = 0; i < stmts.size(); i++) {
            if (stmts.get(i) == node) {
              stmts.set(i, NodeCopier.copySubtree(ast, node.getBody()));
              break;
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * Returns true if a reader method is needed for a specified field.  The
   * heuristic used is to find a method that has the same name, returns the
   * same type, and has no parameters.  Obviously, lousy code can fail this
   * test, but it should work in practice with existing Java code standards.
   */
  private boolean needsReader(VariableDeclarationFragment var, List<BodyDeclaration> classMembers) {
    String methodName = var.getName().getIdentifier();
    ITypeBinding varType = Types.getTypeBinding(var);
    for (BodyDeclaration member : classMembers) {
      if (member instanceof MethodDeclaration) {
        IMethodBinding method = Types.getMethodBinding(member);
        if (method.getName().equals(methodName) && method.getReturnType().isEqualTo(varType) &&
            method.getParameterTypes().length == 0) {
          return false;
        }
      }
    }
    return true;
  }


  /**
   * Returns true if a writer method is needed for a specified field.  The
   * heuristic used is to find a method that has "set" plus the capitalized
   * field name, returns null, and takes a single parameter of the same type.
   * Obviously, lousy code can fail this test, but it should work in practice
   * with Google code standards.
   */
  private boolean needsWriter(VariableDeclarationFragment var, List<BodyDeclaration> classMembers) {
    String methodName = "set" + NameTable.capitalize(var.getName().getIdentifier());
    ITypeBinding varType = Types.getTypeBinding(var);
    ITypeBinding voidType = var.getAST().resolveWellKnownType("void");
    for (BodyDeclaration member : classMembers) {
      if (member instanceof MethodDeclaration) {
        IMethodBinding method = Types.getMethodBinding(member);
        ITypeBinding[] params = method.getParameterTypes();
        if (method.getName().equals(methodName) && method.getReturnType().isEqualTo(voidType) &&
            params.length == 1 && params[0].isEqualTo(varType)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Convert an array initializer into a init method on the equivalent
   * IOSArray. This init method takes a C array and count, like
   * NSArray.arrayWithObjects:count:. For example, "int[] a = { 1, 2, 3 };"
   * translates to "[IOSIntArray initWithInts:(int[]){ 1, 2, 3 } count:3];".
   */
  private MethodInvocation createIOSArrayInitializer(ITypeBinding arrayType,
      ArrayInitializer arrayInit) {
    AST ast = arrayInit.getAST();

    int dimensions = arrayType.getDimensions();
    ITypeBinding componentType;
    IOSArrayTypeBinding iosArrayBinding;
    if (dimensions > 2) {
      // This gets resolved into IOSObjectArray, for an array of arrays.
      componentType = iosArrayBinding = Types.resolveArrayType(arrayType);
    } else if (dimensions == 2) {
      // Creates a single-dimension array type.
      componentType = Types.resolveArrayType(arrayType.getElementType());
      iosArrayBinding = Types.resolveArrayType(componentType);
    } else {
      componentType = Types.getTypeBinding(arrayInit).getComponentType();
      iosArrayBinding = Types.resolveArrayType(componentType);
    }

    // Create IOS message.
    MethodInvocation message = ast.newMethodInvocation();
    SimpleName receiver = ast.newSimpleName(iosArrayBinding.getName());
    Types.addBinding(receiver, iosArrayBinding);
    message.setExpression(receiver);
    String methodName = iosArrayBinding.getInitMethod();
    SimpleName messageName = ast.newSimpleName(methodName);
    GeneratedMethodBinding methodBinding = new GeneratedMethodBinding(methodName,
        Modifier.PUBLIC | Modifier.STATIC, iosArrayBinding, iosArrayBinding, false, false, true);
    Types.addBinding(messageName, methodBinding);
    message.setName(messageName);
    Types.addBinding(message, methodBinding);

    // Pass array initializer as C-style array to message.
    @SuppressWarnings("unchecked")
    List<Expression> args = message.arguments(); // safe by definition
    ArrayInitializer newArrayInit = NodeCopier.copySubtree(ast, arrayInit);
    @SuppressWarnings("unchecked")
    List<Expression> exprs = newArrayInit.expressions();
    for (int i = 0; i < exprs.size(); i++) {
      // Convert any elements that are also array initializers.
      Expression expr = exprs.get(i);
      if (expr instanceof ArrayInitializer) {
        exprs.set(i, createIOSArrayInitializer(componentType, (ArrayInitializer) expr));
      }
    }
    args.add(newArrayInit);
    GeneratedVariableBinding argBinding = new GeneratedVariableBinding(arrayType,
        false, true, null, methodBinding);
    methodBinding.addParameter(argBinding);
    NumberLiteral arraySize =
          ast.newNumberLiteral(Integer.toString(arrayInit.expressions().size()));
    Types.addBinding(arraySize, ast.resolveWellKnownType("int"));
    args.add(arraySize);
    argBinding = new GeneratedVariableBinding(ast.resolveWellKnownType("int"),
        false, true, null, methodBinding);
    methodBinding.addParameter(argBinding);

    // Specify type for object arrays.
    if (iosArrayBinding.getName().equals("IOSObjectArray")) {
      TypeLiteral typeLiteral = ast.newTypeLiteral();
      typeLiteral.setType(Types.makeType(componentType));
      Types.addBinding(typeLiteral, Types.getIOSClass());
      args.add(typeLiteral);
      argBinding = new GeneratedVariableBinding("type", 0, Types.getIOSClass(),
          false, true, null, methodBinding);
      methodBinding.addParameter(argBinding);
    }

    return message;
  }

  /**
   * Add a static read accessor method for a specified variable. The generator
   * phase will rename the variable from "name" to "name_", following the Obj-C
   * style guide.
   */
  private MethodDeclaration makeStaticReader(VariableDeclarationFragment var,
      int modifiers) {
    AST ast = var.getAST();
    String varName = var.getName().getIdentifier();
    IVariableBinding varBinding = var.resolveBinding();
    String methodName;
    methodName = NameTable.getStaticAccessorName(varName);

    Type returnType = Types.makeType(varBinding.getType());
    MethodDeclaration accessor = createBlankAccessor(var, methodName, modifiers, returnType);

    ReturnStatement returnStmt = ast.newReturnStatement();
    SimpleName returnName = ast.newSimpleName(var.getName().getIdentifier() + "_");
    Types.addBinding(returnName, varBinding);
    returnStmt.setExpression(returnName);

    @SuppressWarnings("unchecked")
    List<Statement> stmts = accessor.getBody().statements(); // safe by definition
    stmts.add(returnStmt);

    GeneratedMethodBinding binding =
        new GeneratedMethodBinding(accessor, varBinding.getDeclaringClass(), false);
    Types.addBinding(accessor, binding);
    Types.addBinding(accessor.getName(), binding);
    Symbols.scanAST(accessor);
    return accessor;
  }

  /**
   * Add a static write accessor method for a specified variable.
   */
  private MethodDeclaration makeStaticWriter(VariableDeclarationFragment var,
      String paramName, Type type, int modifiers) {
    AST ast = var.getAST();
    String varName = var.getName().getIdentifier();
    IVariableBinding varBinding = Types.getVariableBinding(var);

    Type returnType = ast.newPrimitiveType(PrimitiveType.VOID);
    Types.addBinding(returnType, ast.resolveWellKnownType("void"));
    String methodName = "set" + NameTable.capitalize(varName);
    MethodDeclaration accessor = createBlankAccessor(var, methodName, modifiers, returnType);
    GeneratedMethodBinding binding =
        new GeneratedMethodBinding(accessor, varBinding.getDeclaringClass(), false);
    Types.addBinding(accessor, binding);
    Types.addBinding(accessor.getName(), binding);

    SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
    param.setName(ast.newSimpleName(paramName));
    Type paramType = NodeCopier.copySubtree(ast, type);
    param.setType(paramType);
    Types.addBinding(paramType, type.resolveBinding());
    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> parameters = accessor.parameters(); // safe by definition
    GeneratedVariableBinding paramBinding = new GeneratedVariableBinding(paramName, 0,
        type.resolveBinding(), false, true, varBinding.getDeclaringClass(), binding);
    Types.addBinding(param, paramBinding);
    Types.addBinding(param.getName(), paramBinding);
    parameters.add(param);
    binding.addParameter(paramBinding);

    Assignment assign = ast.newAssignment();
    SimpleName sn = ast.newSimpleName(NameTable.getName(varBinding));
    assign.setLeftHandSide(sn);
    Types.addBinding(sn, varBinding);
    assign.setRightHandSide(NodeCopier.copySubtree(ast, param.getName()));
    Types.addBinding(assign, varBinding.getType());
    ExpressionStatement assignStmt = ast.newExpressionStatement(assign);

    @SuppressWarnings("unchecked")
    List<Statement> stmts = accessor.getBody().statements(); // safe by definition
    stmts.add(assignStmt);
    Symbols.scanAST(accessor);
    return accessor;
  }

  /**
   * Create an unbound accessor method, minus its code.
   */
  @SuppressWarnings("unchecked") // safe by specification
  private MethodDeclaration createBlankAccessor(VariableDeclarationFragment var,
      String name, int modifiers, Type returnType) {
    AST ast = var.getAST();
    MethodDeclaration accessor = ast.newMethodDeclaration();
    accessor.setName(ast.newSimpleName(name));
    accessor.modifiers().addAll(ast.newModifiers(modifiers));
    accessor.setBody(ast.newBlock());
    accessor.setReturnType2(NodeCopier.copySubtree(ast, returnType));
    return accessor;
  }

  /**
   * Rewrites System.out and System.err println calls as NSLog calls.
   *
   * @return true if the node was rewritten
   */
  // TODO(user): remove when there is iOS console support.
  @SuppressWarnings("unchecked")
  private boolean rewriteSystemOut(MethodInvocation node) {
    Expression expression = node.getExpression();
    if (expression instanceof Name) {
      Name expr = (Name) node.getExpression();
      IBinding binding = expr.resolveBinding();
      if (binding instanceof IVariableBinding) {
        IVariableBinding varBinding = (IVariableBinding) binding;
        ITypeBinding type = varBinding.getDeclaringClass();
        if (type == null) {
          return false;
        }
        String clsName = type.getQualifiedName();
        String varName = varBinding.getName();
        if (clsName.equals("java.lang.System")
            && (varName.equals("out") || varName.equals("err"))) {
          // Change System.out.* or System.err.* to NSLog
          AST ast = node.getAST();
          MethodInvocation newInvocation = ast.newMethodInvocation();
          IMethodBinding methodBinding = new IOSMethodBinding("NSLog",
              Types.getMethodBinding(node), null);
          Types.addBinding(newInvocation, methodBinding);
          Types.addFunction(methodBinding);
          newInvocation.setName(ast.newSimpleName("NSLog"));
          Types.addBinding(newInvocation.getName(), methodBinding);
          newInvocation.setExpression(null);

          // Insert NSLog format argument
          List<Expression> args = node.arguments();
          if (args.size() == 1) {
            Expression arg = args.get(0);
            arg.accept(this);
            String format = getFormatArgument(arg);
            StringLiteral literal = ast.newStringLiteral();
            literal.setLiteralValue(format);
            Types.addBinding(literal, ast.resolveWellKnownType("java.lang.String"));
            newInvocation.arguments().add(literal);

            // JDT won't let nodes be re-parented, so copy and map.
            ASTNode newArg = NodeCopier.copySubtree(ast, arg);
            if (arg instanceof MethodInvocation) {
              IMethodBinding argBinding = ((MethodInvocation) arg).resolveMethodBinding();
              if (!argBinding.getReturnType().isPrimitive()) {
                IOSMethodBinding newBinding =
                    new IOSMethodBinding("format", argBinding, Types.getNSString());
                Types.addMappedInvocation((MethodInvocation) newArg, newBinding);
              }
            }
            newInvocation.arguments().add(newArg);
          } else if (args.size() > 1 && node.getName().getIdentifier().equals("printf")) {
            newInvocation.arguments().addAll(NodeCopier.copySubtrees(ast, args));
          } else if (args.size() == 0) {
            // NSLog requires a format string.
            StringLiteral literal = ast.newStringLiteral();
            literal.setLiteralValue("");
            Types.addBinding(literal,  ast.resolveWellKnownType("java.lang.String"));
            newInvocation.arguments().add(literal);
          }

          // Replace old invocation with new.
          ASTNode parent = node.getParent();
          if (parent instanceof ExpressionStatement) {
            ExpressionStatement stmt = (ExpressionStatement) parent;
            stmt.setExpression(newInvocation);
          } else {
            throw new AssertionError("unknown parent type: " + parent.getClass().getSimpleName());
          }
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Rewrites String.format()'s format string to be iOS-compatible.
   *
   * @return true if the node was rewritten
   */
  private boolean rewriteStringFormat(MethodInvocation node) {
    IMethodBinding binding = node.resolveMethodBinding();
    if (binding == null) {
      // No binding due to error already reported.
      return false;
    }
    ITypeBinding typeBinding = binding.getDeclaringClass();
    AST ast = node.getAST();
    if (typeBinding.equals(ast.resolveWellKnownType("java.lang.String"))
        && binding.getName().equals("format")) {

      @SuppressWarnings("unchecked")
      List<Expression> args = node.arguments();
      if (args.isEmpty()) {
        return false;
      }
      Expression first = args.get(0);
      typeBinding = first.resolveTypeBinding();
      if (typeBinding.getQualifiedName().equals("java.util.Locale")) {
        args.remove(0); // discard locale parameter
        first = args.get(0);
        typeBinding = first.resolveTypeBinding();
      }
      if (first instanceof StringLiteral) {
        String format = ((StringLiteral) first).getLiteralValue();
        String convertedFormat = convertStringFormatString(format);
        if (!format.equals(convertedFormat)) {
          StringLiteral newLiteral = ast.newStringLiteral();
          newLiteral.setLiteralValue(convertedFormat);
          Types.addBinding(newLiteral, ast.resolveWellKnownType("java.lang.String"));
          args.set(0, newLiteral);
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Given a AST node, return the appropriate printf() format specifier.
   */
  private String getFormatArgument(ASTNode node) {
    ITypeBinding type = Types.getTypeBinding(node);
    AST ast = node.getAST();
    if (node instanceof CharacterLiteral || type.isEqualTo(ast.resolveWellKnownType("char"))) {
      return "%C";
    }
    if (node instanceof BooleanLiteral || type.isEqualTo(ast.resolveWellKnownType("boolean"))) {
      return "%d";
    }
    if (type.isEqualTo(ast.resolveWellKnownType("byte")) ||
        type.isEqualTo(ast.resolveWellKnownType("int")) ||
        type.isEqualTo(ast.resolveWellKnownType("short"))) {
      return "%d";
    }
    if (type.isEqualTo(ast.resolveWellKnownType("long"))) {
      return "%lld";
    }
    if (type.isEqualTo(ast.resolveWellKnownType("float")) ||
        type.isEqualTo(ast.resolveWellKnownType("double"))) {
      return "%f";
    }
    if (node instanceof NumberLiteral) {
      String token = ((NumberLiteral) node).getToken();
      try {
        Integer.parseInt(token);
        return "%d";
      } catch (NumberFormatException e) {
        try {
          Long.parseLong(token);
          return "%lld";
        } catch (NumberFormatException e2) {
          try {
            Double.parseDouble(token);
            return "%f";
          } catch (NumberFormatException e3) {
            throw new AssertionError("unknown number literal format: \"" + token + "\"");
          }
        }
      }
    }
    return "%@"; // object, including string
  }

  /**
   * Convert a Java string format string into a NSString equivalent.
   */
  @SuppressWarnings("fallthrough")
  private String convertStringFormatString(String s) {
    if (s.isEmpty()) {
      return s;
    }
    String[] parts = s.split("%");
    StringBuffer result = new StringBuffer();
    int i = 0;
    if (!s.startsWith("%")) {
      result.append(parts[0]);
      i++;
    }
    while (i < parts.length) {
      String part = parts[i];
      if (part.length() > 0) {
        result.append('%');
        switch (part.charAt(0)) {
          case 's':
          case 'S':
            result.append('@');
            break;
          case 'c':
          case 'C':
            result.append('C');
            break;
          case 'h':
          case 'H':
            result.append('x');
            break;

          // These aren't mapped, so escape them so it's obvious when output
          case 'b':
          case 'B':
          case 't':
          case 'T':
          case 'n':
            result.append('%'); // and fall-through
          default:
            result.append(part.charAt(0));
        }
        result.append(part.substring(1));
      }
      i++;
    }
    return result.toString();
  }

  /**
   * Add an abstract method to the given type that implements the given
   * interface method binding.
   */
  private void addAbstractMethod(
      AST ast, ITypeBinding typeBinding, IMethodBinding interfaceMethod,
      List<BodyDeclaration> decls) {
    MethodDeclaration method = createInterfaceMethodBody(ast, typeBinding, interfaceMethod);

    @SuppressWarnings("unchecked")
    List<Modifier> modifiers = method.modifiers();
    modifiers.add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));

    decls.add(method);
  }

  /**
   * Java interfaces that redeclare java.lang.Object's equals, hashCode, or
   * toString methods need a forwarding method if the implementing class
   * relies on java.lang.Object's implementation.  This is because NSObject
   * is declared as adhering to the NSObject protocol, but doesn't explicitly
   * declare these method in its interface.  This prevents gcc from finding
   * an implementation, so it issues a warning.
   */
  private void addForwardingMethod(
      AST ast, ITypeBinding typeBinding, IMethodBinding interfaceMethod,
      List<BodyDeclaration> decls) {
    Logger.getAnonymousLogger().fine(String.format("adding %s to %s",
        interfaceMethod.getName(), typeBinding.getQualifiedName()));
    MethodDeclaration method = createInterfaceMethodBody(ast, typeBinding, interfaceMethod);

    // Add method body with single "super.method(parameters);" statement.
    Block body = ast.newBlock();
    method.setBody(body);
    SuperMethodInvocation superInvocation = ast.newSuperMethodInvocation();
    superInvocation.setName(NodeCopier.copySubtree(ast, method.getName()));

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> parameters = method.parameters(); // safe by design
    @SuppressWarnings("unchecked")
    List<Expression> args = superInvocation.arguments();  // safe by definition
    for (SingleVariableDeclaration param : parameters) {
      Expression arg = NodeCopier.copySubtree(ast, param.getName());
      args.add(arg);
    }
    Types.addBinding(superInvocation, Types.getMethodBinding(method));
    @SuppressWarnings("unchecked")
    List<Statement> stmts = body.statements(); // safe by definition
    ReturnStatement returnStmt = ast.newReturnStatement();
    returnStmt.setExpression(superInvocation);
    stmts.add(returnStmt);

    decls.add(method);
  }

  private MethodDeclaration createInterfaceMethodBody(
      AST ast, ITypeBinding typeBinding, IMethodBinding interfaceMethod) {
    IMethodBinding methodBinding = new IOSMethodBinding(interfaceMethod.getName(), interfaceMethod,
        typeBinding);

    MethodDeclaration method = ast.newMethodDeclaration();
    Types.addBinding(method, methodBinding);
    method.setReturnType2(Types.makeType(interfaceMethod.getReturnType()));

    SimpleName methodName = ast.newSimpleName(interfaceMethod.getName());
    Types.addBinding(methodName, methodBinding);
    method.setName(methodName);

    @SuppressWarnings("unchecked")
    List<Modifier> modifiers = method.modifiers();
    modifiers.add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> parameters = method.parameters(); // safe by design
    ITypeBinding[] parameterTypes = interfaceMethod.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      ITypeBinding paramType = parameterTypes[i];
      String paramName = "param" + i;
      SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
      IVariableBinding paramBinding = IOSVariableBinding.newParameter(paramName, i, paramType,
          methodBinding, paramType.getDeclaringClass(),
          Modifier.isFinal(paramType.getModifiers()));
      Types.addBinding(param, paramBinding);
      param.setName(ast.newSimpleName(paramName));
      Types.addBinding(param.getName(), paramBinding);
      param.setType(Types.makeType(paramType));
      parameters.add(param);
    }
    Symbols.scanAST(method);
    return method;
  }

  /**
   * Remove private serialization methods and fields; since Java serialization
   * isn't supported, they only take up space.  The list of methods is taken
   * from the java.io.Serialization javadoc comments.
   */
  private void removeSerialization(List<BodyDeclaration> members) {
    for (Iterator<BodyDeclaration> iterator = members.iterator(); iterator.hasNext(); ) {
      BodyDeclaration member = iterator.next();
      int mods = member.getModifiers();
      if (member instanceof MethodDeclaration) {
        IMethodBinding binding = Types.getMethodBinding(member);
        String name = binding.getName();
        ITypeBinding[] parameterTypes = binding.getParameterTypes();
        ITypeBinding returnType = binding.getReturnType();
        if (name.equals("readObject")
            && Modifier.isPrivate(mods)
            && parameterTypes.length == 1
            && parameterTypes[0].getQualifiedName().equals("java.io.ObjectInputStream")
            && returnType.getBinaryName().equals("V")) {
          iterator.remove();
          continue;
        }
        if (name.equals("writeObject")
            && Modifier.isPrivate(mods)
            && parameterTypes.length == 1
            && parameterTypes[0].getQualifiedName().equals("java.io.ObjectOutputStream")
            && returnType.getBinaryName().equals("V")) {
          iterator.remove();
          continue;
        }
        if (name.equals("readObjectNoData")
            && Modifier.isPrivate(mods)
            && parameterTypes.length == 0
            && returnType.getBinaryName().equals("V")) {
          iterator.remove();
          continue;
        }
        if ((name.equals("readResolve") || name.equals("writeResolve"))
            && Modifier.isPrivate(mods)
            && parameterTypes.length == 0
            && returnType.getQualifiedName().equals("java.lang.Object")) {
          iterator.remove();
          continue;
        }
      } else if (member instanceof FieldDeclaration) {
        FieldDeclaration field = (FieldDeclaration) member;
        Type type = field.getType();
        VariableDeclarationFragment var = (VariableDeclarationFragment) field.fragments().get(0);
        if (var.getName().getIdentifier().equals("serialVersionUID")
            && type.isPrimitiveType()
            && ((PrimitiveType) type).getPrimitiveTypeCode() == PrimitiveType.LONG
            && Modifier.isPrivate(mods) && Modifier.isStatic(mods)) {
          iterator.remove();
          continue;
        }
      }
    }
  }

  /**
   * If a field and method have the same name, or if a field hides a visible
   * superclass field, rename the field.  This is necessary to avoid a name
   * clash when the fields are declared as properties.
   */
  private void renameDuplicateMembers(ITypeBinding typeBinding) {
    Map<String, IVariableBinding> fields = Maps.newHashMap();

    // Check all superclass(es) fields with declared fields.
    ITypeBinding superclass = typeBinding.getSuperclass();
    if (superclass != null) {
      addFields(superclass, true, true, fields);
      for (IVariableBinding var : typeBinding.getDeclaredFields()) {
        String name = var.getName();
        IVariableBinding field = fields.get(name);
        if (field != null) {
          name += '_';
          NameTable.rename(var, name);
          fields.put(name, var);
        }
      }
    }

    // Check all declared fields with method names.
    addFields(typeBinding, true, false, fields);
    for (IMethodBinding method : typeBinding.getDeclaredMethods()) {
      String name = method.getName();
      IVariableBinding field = fields.get(name);
      if (field != null) {
        IVariableBinding newField;
        while ((newField = fields.get(name)) != null) {
          name += '_';
          field = newField;
        }
        NameTable.rename(field, name, true);
      }
    }
  }

  private void addFields(ITypeBinding type, boolean includePrivate, boolean includeSuperclasses,
      Map<String, IVariableBinding> fields) {
    for (IVariableBinding field : type.getDeclaredFields()) {
      if (!fields.containsValue(field)) { // if not already renamed
        int mods = field.getModifiers();
        if (!Modifier.isStatic(mods)) {
          if (includePrivate) {
            fields.put(field.getName(), field);
          } else if (Modifier.isPublic(mods) || Modifier.isProtected(mods)) {
            fields.put(field.getName(), field);
          } else {
            IPackageBinding typePackage = type.getPackage();
            IPackageBinding fieldPackage = field.getDeclaringClass().getPackage();
            if (typePackage.isEqualTo(fieldPackage)) {
              fields.put(field.getName(), field);
            }
          }
        }
      }
    }
    ITypeBinding superclass = type.getSuperclass();
    if (includeSuperclasses && superclass != null) {
      addFields(superclass, false, true, fields);
    }
  }
}
