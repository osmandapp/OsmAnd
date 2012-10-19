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

import com.google.devtools.j2cpp.J2ObjC;
import com.google.devtools.j2cpp.types.GeneratedMethodBinding;
import com.google.devtools.j2cpp.types.GeneratedVariableBinding;
import com.google.devtools.j2cpp.types.IOSMethod;
import com.google.devtools.j2cpp.types.IOSParameter;
import com.google.devtools.j2cpp.types.IOSTypeBinding;
import com.google.devtools.j2cpp.types.NodeCopier;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.util.NameTable;
import com.google.devtools.j2cpp.types.JavaMethod;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;
import java.util.Map;

/**
 * Translates method invocations and overridden methods from Java core types to
 * iOS equivalents. For example, <code>object.toString()</code> becomes
 * <code>[object description]</code>. Since many methods don't have direct
 * equivalents, other code replaces the method invocation. If the replacement
 * code is too lengthy, though, a call to an emulation library is substituted to
 * prevent code bloat.
 *
 * @author Tom Ball
 */
public class JavaToIOSMethodTranslator extends ErrorReportingASTVisitor {
  private AST ast;
  private Map<IMethodBinding, JavaMethod> descriptions = Maps.newLinkedHashMap();
  private List<IMethodBinding> overridableMethods = Lists.newArrayList();
  private List<IMethodBinding> mappedMethods = Lists.newArrayList();
  private final ITypeBinding javaLangCloneable;

  private final Map<String, String> methodMappings;

  public JavaToIOSMethodTranslator(AST ast, Map<String, String> methodMappings) {
    this.ast = ast;
    this.methodMappings = methodMappings;
    loadTargetMethods(ast.resolveWellKnownType("java.lang.Object"));
    loadTargetMethods(ast.resolveWellKnownType("java.lang.Class"));
    ITypeBinding javaLangString = ast.resolveWellKnownType("java.lang.String");
    loadTargetMethods(javaLangString);
    loadCharSequenceMethods(javaLangString);
    javaLangCloneable = ast.resolveWellKnownType("java.lang.Cloneable");

  }

  private void loadTargetMethods(ITypeBinding clazz) {
    for (IMethodBinding method : clazz.getDeclaredMethods()) {
      if (method.isConstructor() && Types.isJavaObjectType(method.getDeclaringClass())) {
        continue;  // No mapping needed for new Object();
      }
      if (method.getName().equals("clone")) {
        continue;
      }
      // track all non-final public, protected and package-private methods
      int mods = method.getModifiers();
      if (!Modifier.isPrivate(mods)) {
        if (!Modifier.isFinal(mods)) {
          overridableMethods.add(method);
        }
        mappedMethods.add(method);
        addDescription(method);
      }
    }
  }

  private void loadCharSequenceMethods(ITypeBinding stringClass) {
    for (ITypeBinding binding : stringClass.getInterfaces()) {
      if (binding.getQualifiedName().equals("java.lang.CharSequence")) {
        for (IMethodBinding method : binding.getDeclaredMethods()) {
          if (method.getName().equals("length")) {
            overridableMethods.add(0, method);
            NameTable.rename(method, "sequenceLength");
            mappedMethods.add(method);
            addDescription(method);
          } else if (method.getName().equals("toString")) {
            overridableMethods.add(0, method);
            NameTable.rename(method, "sequenceDescription");
            mappedMethods.add(method);
            addDescription(method);
          } else if (method.getName().equals("subSequence")) {
            overridableMethods.add(0, method);
            NameTable.rename(method, "subSequenceFrom");
            mappedMethods.add(method);
            addDescription(method);
          }
        }
      }
    }
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    if (node.getBody() != null) {
      visit(node.getBody());
    }
    IMethodBinding binding = Types.getMethodBinding(node);
    for (IMethodBinding overridable : overridableMethods) {
      if (!binding.isConstructor() &&
          (binding.isEqualTo(overridable) || binding.overrides(overridable))) {
        JavaMethod md = getDescription(overridable);
        String key = md.getKey();
        String value = methodMappings.get(key);
        if (value != null) {
          IOSMethod iosMethod = new IOSMethod(value, binding, ast);
          node.setName(ast.newSimpleName(iosMethod.getName()));
          Types.addBinding(node.getName(), iosMethod.resolveBinding());
          Types.addMappedIOSMethod(binding, iosMethod);

          // Map parameters, if any.
          @SuppressWarnings("unchecked")
          List<SingleVariableDeclaration> parameters = node.parameters();
          int n = parameters.size();
          if (n > 0) {
            List<IOSParameter> iosArgs = iosMethod.getParameters();
            assert n == iosArgs.size() || iosMethod.isVarArgs();

            // Pull parameters out of list, so they can be reordered.
            SingleVariableDeclaration[] params =
                parameters.toArray(new SingleVariableDeclaration[n]);

            for (int i = 0; i < n; i++) {
              SingleVariableDeclaration var = params[i];
              IVariableBinding varBinding = Types.getVariableBinding(var);
              IOSParameter iosArg = iosArgs.get(i);
              SimpleType paramType =
                  ast.newSimpleType(NameTable.unsafeSimpleName(iosArg.getType(), ast));
              Types.addBinding(paramType, varBinding);
              Types.addBinding(paramType.getName(), varBinding);
              var.setType(paramType);
              Types.addBinding(var.getName(), varBinding);
              parameters.set(iosArg.getIndex(), var);
            }
          }

          Types.addMappedIOSMethod(binding, iosMethod);
        }
        return false;
      }
    }
    return false;
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    // translate any embedded method invocations
    if (node.getExpression() != null) {
      node.getExpression().accept(this);
    }
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by design
    for (Expression e : args) {
      e.accept(this);
    }
    if (node.getAnonymousClassDeclaration() != null) {
      node.getAnonymousClassDeclaration().accept(this);
    }

    IMethodBinding binding = Types.getMethodBinding(node);
    JavaMethod md = descriptions.get(binding);
    if (md != null) {
      String key = md.getKey();
      String value = methodMappings.get(key);
      if (value != null) {
        IOSMethod iosMethod = new IOSMethod(value, binding, binding.getDeclaringClass(), ast);
        IMethodBinding methodBinding = iosMethod.resolveBinding();
        MethodInvocation newInvocation = createMappedInvocation(iosMethod, binding, methodBinding);

        // Set parameters.
        @SuppressWarnings("unchecked")
        List<Expression> oldArgs = node.arguments(); // safe by definition
        @SuppressWarnings("unchecked")
        List<Expression> newArgs = newInvocation.arguments(); // safe by definition
        copyInvocationArguments(null, oldArgs, newArgs);

        Types.substitute(node, newInvocation);
        Types.addMappedIOSMethod(binding, iosMethod);
        Types.addMappedInvocation(node, iosMethod.resolveBinding());
      } else {
        J2ObjC.error(node, createMissingMethodMessage(binding));
      }
    }
    return false;
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    // If this type implements Cloneable but its parent doesn't, add a
    // copyWithZone: method that calls clone().
    ITypeBinding type = Types.getTypeBinding(node);
    if (type.isAssignmentCompatible(javaLangCloneable)) {
      ITypeBinding superclass = type.getSuperclass();
      if (superclass == null || !superclass.isAssignmentCompatible(javaLangCloneable)) {
        addCopyWithZoneMethod(node);
      }
    }
  }

  @Override
  public boolean visit(MethodInvocation node) {
    // translate any embedded method invocations
    if (node.getExpression() != null) {
      node.getExpression().accept(this);
    }
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by definition
    for (Expression e : args) {
      e.accept(this);
    }

    IMethodBinding binding = Types.getMethodBinding(node);
    JavaMethod md = descriptions.get(binding);
    if (md == null && !binding.getName().equals("clone")) { // never map clone()
      IVariableBinding receiver =
          node.getExpression() != null ? Types.getVariableBinding(node.getExpression()) : null;
      ITypeBinding clazz =
          receiver != null ? receiver.getType() : binding.getDeclaringClass();
      if (clazz != null && !clazz.isArray()) {
        for (IMethodBinding method : descriptions.keySet()) {
          if (binding.isSubsignature(method)
              && clazz.isAssignmentCompatible(method.getDeclaringClass())) {
            md = descriptions.get(method);
            break;
          }
        }
      }
    }
    if (md != null) {
      String key = md.getKey();
      String value = methodMappings.get(key);
      if (value == null) {
        J2ObjC.error(node, createMissingMethodMessage(binding));
        return true;
      }
      IOSMethod iosMethod = new IOSMethod(value, binding, ast);
      NameTable.rename(binding, iosMethod.getName());
      if (node.getExpression() instanceof SimpleName) {
        SimpleName expr = (SimpleName) node.getExpression();
        if (expr.getIdentifier().equals(binding.getDeclaringClass().getName())
            || expr.getIdentifier().equals(binding.getDeclaringClass().getQualifiedName())) {
          NameTable.rename(binding.getDeclaringClass(), iosMethod.getDeclaringClass());
        }
      }
      Types.addMappedIOSMethod(binding, iosMethod);
      Types.addMappedInvocation(node, iosMethod.resolveBinding());
    } else {
      // Not mapped, check if it overrides a mapped method.
      for (IMethodBinding methodBinding : mappedMethods) {
        if (binding.overrides(methodBinding)) {
          JavaMethod desc = getDescription(methodBinding);
          String value = methodMappings.get(desc.getKey());
          if (value != null) {
            IOSMethod iosMethod = new IOSMethod(value, binding, ast);
            NameTable.rename(methodBinding, iosMethod.getName());
            Types.addMappedIOSMethod(binding, iosMethod);
            Types.addMappedInvocation(node, iosMethod.resolveBinding());
            break;
          }
        }
      }
    }
    return false;
  }

  public MethodInvocation createMappedInvocation(IOSMethod iosMethod,
      IMethodBinding oldMethodBinding, IMethodBinding newMethodBinding) {
    // create invocation of mapped method
    MethodInvocation newInvocation = ast.newMethodInvocation();
    Types.addBinding(newInvocation, newMethodBinding);
    newInvocation.setName(NameTable.unsafeSimpleName(iosMethod.getName(), ast));
    Types.addBinding(newInvocation.getName(), newMethodBinding);
    newInvocation.setExpression(ast.newName(iosMethod.getDeclaringClass()));
    Types.addBinding(newInvocation.getExpression(),
        Types.resolveIOSType(iosMethod.getDeclaringClass()));
    Types.addMappedIOSMethod(oldMethodBinding, iosMethod);
    Types.addMappedInvocation(newInvocation, newMethodBinding);
    return newInvocation;
  }

  private void copyInvocationArguments(Expression receiver, List<Expression> oldArgs,
      List<Expression> newArgs) {
    // set the receiver as the first argument
    if (receiver != null) {
      Expression delegate = NodeCopier.copySubtree(ast, receiver);
      delegate.accept(this);
      newArgs.add(delegate);
    }

    // copy remaining arguments
    for (Expression oldArg : oldArgs) {
      newArgs.add(NodeCopier.copySubtree(ast, oldArg));
    }
  }

  @Override
  public boolean visit(SuperMethodInvocation node) {
    // translate any embedded method invocations
    @SuppressWarnings("unchecked")
    List<Expression> args = node.arguments(); // safe by definition
    for (Expression e : args) {
      e.accept(this);
    }

    IMethodBinding binding = Types.getMethodBinding(node);
    JavaMethod md = descriptions.get(binding);
    if (md != null) {
      String key = md.getKey();
      String value = methodMappings.get(key);
      if (value == null) {
        // Method has same name as a mapped method's, but it's ignored since
        // it doesn't override it.
        return super.visit(node);
      }
      IOSMethod iosMethod = new IOSMethod(value, binding, ast);
      node.setName(NameTable.unsafeSimpleName(iosMethod.getName(), ast));
      SimpleName name = node.getName();
      if (name.getIdentifier().equals(binding.getDeclaringClass().getName())
          || name.getIdentifier().equals(binding.getDeclaringClass().getQualifiedName())) {
        node.setName(NameTable.unsafeSimpleName(iosMethod.getDeclaringClass(), ast));
      }
      Types.addMappedIOSMethod(binding, iosMethod);
      IMethodBinding newBinding = iosMethod.resolveBinding();
      Types.addMappedInvocation(node, newBinding);
      Types.addBinding(node, newBinding);
      Types.addBinding(name, newBinding);
    } else {
      // Not mapped, check if it overrides a mapped method.
      for (IMethodBinding methodBinding : mappedMethods) {
        if (binding.overrides(methodBinding)) {
          JavaMethod desc = getDescription(methodBinding);
          String value = methodMappings.get(desc.getKey());
          if (value != null) {
            IOSMethod iosMethod = new IOSMethod(value, binding, ast);
            node.setName(NameTable.unsafeSimpleName(iosMethod.getName(), ast));
            Types.addMappedIOSMethod(binding, iosMethod);
            IMethodBinding newBinding = iosMethod.resolveBinding();
            Types.addBinding(node, newBinding);
            Types.addBinding(node.getName(), newBinding);
          }
        }
      }
    }
    return false;
  }

  private JavaMethod getDescription(IMethodBinding binding) {
    if (descriptions.containsKey(binding)) {
      return descriptions.get(binding);
    }
    return addDescription(binding);
  }

  private JavaMethod addDescription(IMethodBinding binding) {
    JavaMethod desc = JavaMethod.getJavaMethod(binding);
    descriptions.put(binding, desc);
    return desc;
  }

  /**
   * Explicitly walk block statement lists, to work around a bug in
   * ASTNode.visitChildren that skips list members.
   */
  @Override
  public boolean visit(Block node) {
    @SuppressWarnings("unchecked")
    List<Statement> stmts = node.statements(); // safe by design
    for (Statement s : stmts) {
      s.accept(this);
    }
    return false;
  }

  private String createMissingMethodMessage(IMethodBinding binding) {
    StringBuilder sb = new StringBuilder("Internal error: ");
    sb.append(binding.getDeclaringClass().getName());
    if (!binding.isConstructor()) {
      sb.append('.');
      sb.append(binding.getName());
    }
    sb.append('(');
    ITypeBinding[] args = binding.getParameterTypes();
    int nargs = args.length;
    for (int i = 0; i < nargs; i++) {
      sb.append(args[i].getName());
      if (i + 1 < nargs) {
        sb.append(',');
      }
    }
    sb.append(") not mapped");
    return sb.toString();
  }

  private SingleVariableDeclaration makeZoneParameter(GeneratedVariableBinding zoneBinding) {
    SimpleName zoneName = ast.newSimpleName("zone");
    Types.addBinding(zoneName, zoneBinding);
    SingleVariableDeclaration zoneParam = ast.newSingleVariableDeclaration();
    zoneParam.setName(zoneName);
    zoneParam.setType(Types.makeType(zoneBinding.getType()));
    Types.addBinding(zoneParam, zoneBinding);
    return zoneParam;
  }

  private MethodInvocation makeCloneInvocation(ITypeBinding declaringClass,
      GeneratedVariableBinding zoneBinding) {
    GeneratedMethodBinding cloneBinding =
        makeCloneBinding(declaringClass, zoneBinding);

    SimpleName cloneInvocationName = ast.newSimpleName("clone");
    Types.addBinding(cloneInvocationName, cloneBinding);
    MethodInvocation cloneInvocation = ast.newMethodInvocation();
    cloneInvocation.setName(cloneInvocationName);
    Types.addBinding(cloneInvocation, cloneBinding);
    return cloneInvocation;
  }

  /**
   * Returns a bound method name for "copyWithZone", given the method's
   * declaring class type.
   */
  private SimpleName newCloneMethodName(ITypeBinding declaringClass, boolean isSynthetic) {
    GeneratedMethodBinding newBinding = new GeneratedMethodBinding("copyWithZone", 0,
        Types.resolveIOSType("id"), declaringClass, false, false, isSynthetic);
    IOSMethod iosMethod = new IOSMethod("id copyWithZone:(NSZone *)zone", newBinding, ast);
    Types.addMappedIOSMethod(newBinding, iosMethod);
    SimpleName copyMethodName = ast.newSimpleName(iosMethod.getName());
    Types.addBinding(copyMethodName, newBinding);
    return copyMethodName;
  }

  private void addCopyWithZoneMethod(TypeDeclaration node) {
    // Create copyWithZone: method.
    ITypeBinding type = Types.getTypeBinding(node).getTypeDeclaration();
    SimpleName methodName = newCloneMethodName(type, true);
    GeneratedMethodBinding binding = (GeneratedMethodBinding) Types.getMethodBinding(methodName);
    IOSMethod iosMethod = Types.getMappedMethod(binding);
    MethodDeclaration cloneMethod = ast.newMethodDeclaration();
    Types.addBinding(cloneMethod, binding);
    cloneMethod.setName(methodName);
    cloneMethod.setReturnType2(Types.makeType(binding.getReturnType()));

    // Add NSZone *zone parameter.
    IOSTypeBinding nsZoneType = new IOSTypeBinding("NSZone", false);
    GeneratedVariableBinding zoneBinding = new GeneratedVariableBinding("zone", 0, nsZoneType,
        false, true, binding.getDeclaringClass(), binding);
    binding.addParameter(zoneBinding);
    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> parameters = cloneMethod.parameters(); // safe by definition
    parameters.add(makeZoneParameter(zoneBinding));
    Types.addMappedIOSMethod(binding, iosMethod);

    Block block = ast.newBlock();
    cloneMethod.setBody(block);

    MethodInvocation cloneInvocation = makeCloneInvocation(type, zoneBinding);
    ReturnStatement returnStmt = ast.newReturnStatement();
    returnStmt.setExpression(cloneInvocation);
    @SuppressWarnings("unchecked")
    List<Statement> stmts = block.statements(); // safe by definition
    stmts.add(returnStmt);

    @SuppressWarnings("unchecked")
    List<BodyDeclaration> members = node.bodyDeclarations(); // safe by definition
    members.add(cloneMethod);
  }

  private GeneratedMethodBinding makeCloneBinding(ITypeBinding declaringClass,
      GeneratedVariableBinding zoneBinding) {
    GeneratedMethodBinding copyObjectBinding = new GeneratedMethodBinding("clone", 0,
        Types.resolveIOSType("NSObject"), declaringClass, false, false, true);
    return copyObjectBinding;
  }
}
