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

package com.google.devtools.j2cpp.gen;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.devtools.j2cpp.J2ObjC;
import com.google.devtools.j2cpp.Options;
import com.google.devtools.j2cpp.gen.CppStatementGenerator;
import com.google.devtools.j2cpp.gen.HiddenFieldDetector;
import com.google.devtools.j2cpp.types.Types;
import com.google.devtools.j2cpp.types.ImplementationImportCollector;
import com.google.devtools.j2cpp.types.ImportCollector;
import com.google.devtools.j2cpp.types.IOSMethod;
import com.google.devtools.j2cpp.util.NameTable;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

import com.google.devtools.j2cpp.gen.CppSourceFileGenerator;
/**
 * Generates C++ implementation (.m) files from compilation units.
 *
 * @author Tom Ball
 */
public class CppImplementationGenerator extends CppSourceFileGenerator {
  private Set<IVariableBinding> fieldHiders;
  private final String suffix;

  /**
   * Generate an C++ implementation file for each type declared in a
   * specified compilation unit.
   */
  public static void generate(String fileName, CompilationUnit unit, String source) {
    CppImplementationGenerator implementationGenerator = new CppImplementationGenerator(fileName, unit, source);
    implementationGenerator.generate(unit);
  }

  private CppImplementationGenerator(String fileName, CompilationUnit unit, String source) {
    super(fileName, source, unit, Options.emitLineDirectives());
    fieldHiders = HiddenFieldDetector.getFieldNameConflicts(unit);
    suffix = ".cpp";
  }

  @Override
  protected String getSuffix() {
    return suffix;
  }

  public void generate(CompilationUnit unit) {
    println(J2ObjC.getFileHeader(getSourceFileName()));
    if (needsPrinting(unit)) {
      printStart(getSourceFileName());
      printImports(unit);
      unit.accept(new ErrorReportingASTVisitor() {
        @Override
        public boolean visit(TypeDeclaration node) {
          generate(node);
          return true;
        }

        @Override
        public boolean visit(EnumDeclaration node) {
          generate(node);
          return true;
        }
      });
    } else {
      // Print a dummy C function so compiled object file is valid.
      @SuppressWarnings("unchecked")
      List<AbstractTypeDeclaration> types = unit.types(); // safe by definition
      if (!types.isEmpty()) {
        printf("void %s_unused() {}\n", NameTable.getFullName(types.get(0)));
      }
    }
    save(unit);
  }

  private boolean needsPrinting(CompilationUnit unit) {
    final boolean[] result = { false };
    unit.accept(new ErrorReportingASTVisitor() {
      @Override
      public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) {
          result[0] = true;  // always print concrete types
          return false;
        }
        return true;
      }

      @Override
      public boolean visit(EnumDeclaration node) {
        result[0] = true; // always print enums
        return false;
      }

      @Override
      public boolean visit(AnnotationTypeDeclaration node) {
        return false;  // never print annotations
      }

      @Override
      public void endVisit(MethodDeclaration node) {
        // Only print protocols if they have static constants.
        if (isInterfaceConstantAccessor(Types.getMethodBinding(node))) {
          result[0] = true;
        }
      }
    });
    return result[0];
  }

  @Override
  public void generate(TypeDeclaration node) {
    List<IMethodBinding> testMethods = null;
    if (Types.isJUnitTest(Types.getTypeBinding(node))) {
       testMethods = findTestMethods(node);
    }
    syncLineNumbers(node.getName()); // avoid doc-comment

    fieldHiders = HiddenFieldDetector.getFieldNameConflicts(node);
    if (node.isInterface()) {
      printStaticInterface(node);
    } else {
      String typeName = NameTable.getFullName(node);
      printf("class %s {\n\n", typeName);
      printStaticVars(Lists.newArrayList(node.getFields()));
      printProperties(node.getFields());
      printMethods(node);
      printObjCTypeMethod(node);

      println("}\n");

      // Generate main method, if declared.
      MethodDeclaration main = null;
      for (MethodDeclaration m : node.getMethods()) {
        if (isMainMethod(m)) {
          main = m;
          break;
        }
      }
      newline();
      if (main != null || (testMethods != null && Options.generateTestMain())) {
        printMainMethod(main, typeName, testMethods);
      }
    }
  }

  private List<IMethodBinding> findTestMethods(TypeDeclaration node) {
    ITypeBinding type = Types.getTypeBinding(node);
    List<IMethodBinding> tests = Lists.newArrayList();
    while (type != null) {
      for (IMethodBinding md : type.getDeclaredMethods()) {
        int modifiers = md.getModifiers();
        if (Modifier.isPublic(modifiers)) {
          if (md.getName().startsWith("test") && md.getParameterTypes().length == 0) {
            tests.add(md);
          }
        }
      }
      type = type.getSuperclass();
    }
    return tests.isEmpty() ? null : tests;
  }

  @Override
  protected void generate(AnnotationTypeDeclaration node) {
    // No implementation for annotations.
  }

  private void printMethods(TypeDeclaration node) {
    printMethods(Lists.newArrayList(node.getMethods()));

    // If node implements CharSequence, add forwarding method from the
    // sequenceDescription method to description (toString()).  See
    // JavaToIOSMethodTranslator.loadCharSequenceMethods() for details.
    ITypeBinding binding = Types.getTypeBinding(node);
    for (ITypeBinding interfaze : binding.getInterfaces()) {
      if (interfaze.getQualifiedName().equals("java.lang.CharSequence")) {
        println("- (NSString *)description {\n  return [self sequenceDescription];\n}\n");
      }
    }

    // If node defines a primitive number wrapper, add a getValue() method.
    // This is required by iOS 5.0 to support cloning these types.
    if (Types.isJavaNumberType(binding)) {
      ITypeBinding primitiveType = Types.getPrimitiveType(binding);
      if (primitiveType != null) {
        // All java.lang primitive type wrappers have a "value" field.
        printf("- (void)getValue:(void *)buffer {\n  *((%s *) buffer) = value_;\n}\n\n",
            NameTable.getFullName(primitiveType));
      }
    }
  }

  private void printStaticInterface(TypeDeclaration node) {
    // Print implementation for static constants, if any.
    boolean needsPrinting = false;
    List<MethodDeclaration> methods = Lists.newArrayList(node.getMethods());
    for (MethodDeclaration m : methods) {
      if (isInterfaceConstantAccessor(Types.getMethodBinding(m))) {
        needsPrinting = true;
        break;
      }
    }

    if (needsPrinting) {
      printf("\n@implementation %s\n\n", NameTable.getFullName(node));
      printStaticVars(Lists.newArrayList(node.getFields()));
      for (MethodDeclaration m : methods) {
        IMethodBinding binding = Types.getMethodBinding(m);
        if (binding.isSynthetic() || isInterfaceConstantAccessor(binding)) {
          printMethod(m);
        }
      }
      println("@end");
    }
  }

  @Override
  protected void generate(EnumDeclaration node) {
    @SuppressWarnings("unchecked")
    List<EnumConstantDeclaration> constants = node.enumConstants(); // safe by definition
    List<MethodDeclaration> methods = Lists.newArrayList();
    List<FieldDeclaration> fields = Lists.newArrayList();
    MethodDeclaration initializeMethod = null;
    @SuppressWarnings("unchecked")
    List<BodyDeclaration> declarations = node.bodyDeclarations(); // safe by definition
    for (BodyDeclaration decl : declarations) {
      if (decl instanceof FieldDeclaration) {
        fields.add((FieldDeclaration) decl);
      } else if (decl instanceof MethodDeclaration) {
        MethodDeclaration md = (MethodDeclaration) decl;
        if (md.getName().getIdentifier().equals("initialize")) {
          initializeMethod = md;
        } else {
          methods.add(md);
        }
      }
    }
    syncLineNumbers(node.getName()); // avoid doc-comment

    String typeName = NameTable.getFullName(node);
    for (EnumConstantDeclaration constant : constants) {
      printf("static %s *%s_%s;\n", typeName, typeName, NameTable.getName(constant.getName()));
    }
    printf("IOSObjectArray *%s_values;\n", typeName);
    newline();

    printf("@implementation %s\n\n", typeName);
    printStaticVars(fields);

    for (EnumConstantDeclaration constant : constants) {
      String name = NameTable.getName(constant.getName());
      printf("+ (%s *)%s {\n", typeName, name);
      printf("  return %s_%s;\n", typeName, name);
      println("}");
    }
    newline();

    // Enum constants needs to implement NSCopying.  Being singletons, they
    // can just return self, as long the retain count is incremented.
    String selfString = Options.useReferenceCounting() ? "[self retain]" : "self";
    printf("- (id)copyWithZone:(NSZone *)zone {\n  return %s;\n}\n\n", selfString);

    printProperties(fields.toArray(new FieldDeclaration[0]));
    printMethods(methods);

    printf("+ (void)initialize {\n  if (self == [%s class]) {\n", typeName);
    for (int i = 0; i < constants.size(); i++) {
      EnumConstantDeclaration constant = constants.get(i);
      @SuppressWarnings("unchecked")
      List<Expression> args = constant.arguments(); // safe by definition
      String name = NameTable.getName(constant.getName());
      String constantTypeName = NameTable.getFullName(Types.getMethodBinding(constant).getDeclaringClass());
      printf("    %s_%s = [[%s alloc] init", typeName, name, constantTypeName);
      boolean isSimpleEnum = constantTypeName.equals(typeName);

      // Common-case: no extra fields and no constant anonymous classes.
      if (args.isEmpty() && isSimpleEnum) {
        printf("WithNSString:@\"%s_%s\" withInt:%d];\n", typeName.replace("Enum", ""), name, i);
      } else {
        String argString = CppStatementGenerator.generateArguments(Types.getMethodBinding(constant),
            args, fieldHiders, getBuilder().getCurrentLine());
        print(argString);
        if (args.isEmpty()) {
          print("With");
        } else {
          print(" with");
        }
        printf("NSString:@\"%s_%s\" withInt:%d];\n", typeName.replace("Enum", ""), name, i);
      }
    }
    printf("    %s_values = [[IOSObjectArray alloc] initWithObjects:(id[]){ ", typeName);
    for (EnumConstantDeclaration constant : constants) {
      printf("%s_%s, ", typeName, NameTable.getName(constant.getName()));
    }
    printf("nil } count:%d type:[IOSClass classWithClass:[%s class]]];\n",
        constants.size(), typeName);
    if (initializeMethod != null) {
      @SuppressWarnings("unchecked")
      List<Statement> stmts = initializeMethod.getBody().statements(); // safe by definition
      for (Statement s : stmts) {
        printf("    %s", CppStatementGenerator.generate(s, fieldHiders, false,
            getBuilder().getCurrentLine()));
      }
    }
    println("  }\n}\n");

    // Print generated values and valueOf methods.
    println("+ (IOSObjectArray *)values {");
    printf("  return [IOSObjectArray arrayWithArray:%s_values];\n", typeName);
    println("}\n");
    printf("+ (%s *)valueOfWithNSString:(NSString *)name {\n", typeName);
    printf("  for (int i = 0; i < [%s_values count]; i++) {\n", typeName);
    printf("    %s *e = [%s_values objectAtIndex:i];\n", typeName, typeName);
    printf("    if ([name isEqual:[e name]]) {\n");
    printf("      return e;\n");
    printf("    }\n");
    printf("  }\n");
    printf("  @throw [[JavaLangIllegalArgumentException alloc] initWithNSString:name];\n");
    printf("  return nil;\n");
    println("}\n");

    println("@end");
  }

  @Override
  protected String methodDeclaration(MethodDeclaration m) {
    int modifiers = m.getModifiers();
    if ((modifiers & Modifier.NATIVE) > 0) {
      return super.methodDeclaration(m) + " " + extractNativeMethodBody(m) + "\n\n";
    }
    String methodBody = generateMethodBody(m);
    return super.methodDeclaration(m) + " " + reindent(methodBody) + "\n\n";
  }

  @Override
  protected String mappedMethodDeclaration(MethodDeclaration method, IOSMethod mappedMethod) {
    String methodBody;
    if ((method.getModifiers() & Modifier.NATIVE) > 0) {
      methodBody = extractNativeMethodBody(method);
    } else {
      methodBody = generateMethodBody(method);
    }
    return super.mappedMethodDeclaration(method, mappedMethod)
        + " " + reindent(methodBody) + "\n\n";
  }

  private String generateMethodBody(MethodDeclaration m) {
    if (Modifier.isAbstract(m.getModifiers())) {
      // Generate a body which throws a NSInvalidArgumentException.
      String body =
          "{\n // can't call an abstract method\n " +
              "[self doesNotRecognizeSelector:_cmd];\n ";
      if (!Types.isVoidType(m.getReturnType2())) {
        body += "return 0;\n"; // Never executes, but avoids a gcc warning.
      }
      return body + "}";
    }
    // generate a normal method body
    String methodBody = generateStatement(m.getBody(), false);
    if (Types.hasAutoreleasePoolAnnotation(Types.getBinding(m))) {
      return reindent(
          "{\nNSAutoreleasePool *pool__ = [[NSAutoreleasePool alloc] init];\n" +
          methodBody +
          "[pool__ release];\n}");
    } else {
      return methodBody;
    }
  }

  @Override
  protected String getParameterName(SingleVariableDeclaration param) {
    String name = super.getParameterName(param);
    IVariableBinding binding = param.resolveBinding();
    return binding != null && fieldHiders.contains(binding) ? name + "Arg" : name;
  }

  @Override
  protected String constructorDeclaration(MethodDeclaration m) {
    String methodBody;
    IMethodBinding binding = Types.getMethodBinding(m);
    @SuppressWarnings("unchecked")
    List<Statement> statements = m.getBody().statements();
    if (binding.getDeclaringClass().isEnum()) {
      return enumConstructorDeclaration(m, statements, binding);
    } else if (statements.isEmpty()) {
      methodBody = "{\nreturn (self = [super init]);\n}";
    } else if (statements.size() == 1 &&
        (statements.get(0) instanceof ConstructorInvocation ||
         statements.get(0) instanceof SuperConstructorInvocation)) {
      methodBody = "{\nreturn " + generateStatement(statements.get(0), false, true) + ";\n}";
    } else {
      StringBuffer sb = new StringBuffer();
      Statement first = statements.get(0);
      boolean firstPrinted = false;
      sb.append("{\nif ((self = ");
      if (first instanceof ConstructorInvocation ||
          first instanceof SuperConstructorInvocation) {
        sb.append(generateStatement(first, false, true));
        firstPrinted = true;
      } else {
        sb.append("[super init]");
      }
      sb.append(")) {\n");
      for (int i = firstPrinted ? 1 : 0; i < statements.size(); i++) {
        sb.append(generateStatement(statements.get(i), false, true));
      }
      sb.append("}\nreturn self;\n}");
      methodBody = sb.toString();
    }
    return super.constructorDeclaration(m) + " " + reindent(methodBody) + "\n\n";
  }

  private String enumConstructorDeclaration(MethodDeclaration m, List<Statement> statements,
      IMethodBinding binding) {
    assert !statements.isEmpty();

    // Append enum generated parameters to invocation.  The
    // InitializationNormalizer should have fixed this constructor so the
    // first statement is a constructor or super invocation.
    Statement s = statements.get(0);
    assert s instanceof ConstructorInvocation || s instanceof SuperConstructorInvocation;
    String invocation = generateStatement(statements.get(0), false, true) + ";\n";
    List<?> args = s instanceof ConstructorInvocation
        ? ((ConstructorInvocation) s).arguments() : ((SuperConstructorInvocation) s).arguments();
    String impliedArgs = (args.isEmpty() ? "W" : " w") + "ithNSString:name withInt:ordinal";
    int index = invocation.lastIndexOf(']');
    invocation = invocation.substring(0, index) + impliedArgs + ']';

    StringBuffer sb = new StringBuffer();
    if (statements.size() == 1) {
      sb.append("{\nreturn ");
      sb.append(invocation);
      sb.append(";\n}");
    } else {
      sb.append("{\nif ((self = ");
      sb.append(invocation);
      sb.append(")) {\n");
      for (int i = 1; i < statements.size(); i++) {
        sb.append(generateStatement(statements.get(i), false, true));
      }
      sb.append("}\nreturn self;\n}");
    }
    String result = super.constructorDeclaration(m) + " " + reindent(sb.toString()) + "\n\n";
    return result;
  }

  @Override
  protected void printStaticConstructorDeclaration(MethodDeclaration m) {
    String className =
        NameTable.javaTypeToCpp(Types.getMethodBinding(m).getDeclaringClass(), false);
    StringBuffer sb = new StringBuffer();
    sb.append("{\nif (self == [" + className + " class]) {\n");
    @SuppressWarnings("unchecked")
    List<Statement> statements = m.getBody().statements();
    for (Statement statement : statements) {
      sb.append(generateStatement(statement, false, true));
    }
    sb.append("}\n}");
    print("+ (void)initialize " + reindent(sb.toString()) + "\n\n");
  }

  private String generateStatement(Statement stmt, boolean asFunction, boolean inConstructor) {
    return CppStatementGenerator.generate(stmt, fieldHiders, asFunction,
        getBuilder().getCurrentLine());
  }

  private String generateStatement(Statement stmt, boolean asFunction) {
    return CppStatementGenerator.generate(stmt, fieldHiders, asFunction,
        getBuilder().getCurrentLine());
  }

  private String generateExpression(Expression expr) {
    return CppStatementGenerator.generate(expr, fieldHiders, false, getBuilder().getCurrentLine());
  }

  private void printMainMethod(MethodDeclaration m, String typeName,
      List<IMethodBinding> testMethods) {
    if (m != null) {  // True for unit tests.
      Types.addFunction(Types.getMethodBinding(m));
    }
    println("int main( int argc, const char *argv[] ) {");
    if (m != null && (m.getModifiers() & Modifier.NATIVE) > 0) {
      println(extractNativeMethodBody(m));
      return;
    }
    indent();
    if (Options.useReferenceCounting()) {
      printIndent();
      println("NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];");
    }

    if (m != null) {
      @SuppressWarnings("unchecked")
      List<SingleVariableDeclaration> params = m.parameters();
      assert params.size() == 1;  // Previously checked in isMainMethod().
      printIndent();
      printf("IOSObjectArray *%s = JreEmulationMainArguments(argc, argv);\n\n",
          params.get(0).getName().getIdentifier());
      printMethodBody(m, true);
    }
    if (testMethods != null) {
      printIndent();
      printf("int exitCode = [JUnitRunner runTests:[%s class]", typeName);
      for (IMethodBinding test : testMethods) {
        printf(", @\"%s\"", test.getName());
      }
      println(", nil];");
    } else {
      printIndent();
      println("int exitCode = 0;");
    }
    if (Options.useReferenceCounting()) {
      print('\n');
      printIndent();
      println("[pool release];");
    }
    printIndent();
    println("return exitCode;");
    unindent();
    println("}");
  }

  private void printMethodBody(MethodDeclaration m, boolean isFunction) throws AssertionError {
    for (Object stmt : m.getBody().statements()) {
      if (stmt instanceof Statement) {
        String objcStmt = reindent(generateStatement((Statement) stmt, isFunction));
        println(objcStmt);
      } else {
        throw new AssertionError("unexpected AST type: " + stmt.getClass());
      }
    }
  }

  private String extractNativeMethodBody(MethodDeclaration m) {
    assert (m.getModifiers() & Modifier.NATIVE) > 0;
    String nativeCode = extractNativeCode(m.getStartPosition(), m.getLength());
    if (nativeCode == null) {
      J2ObjC.error(m, "no native code found");
      return "ERROR";
    }
    indent();
    String code = reindent('{' + nativeCode + '}');
    unindent();
    return code;
  }

  private void printImports(CompilationUnit node) {
    ImplementationImportCollector collector = new ImplementationImportCollector();
    collector.collect(node, getSourceFileName());
    Set<ImportCollector.Import> imports = collector.getImports();

    if (!imports.isEmpty()) {
      Set<String> importStmts = Sets.newTreeSet();
      for (ImportCollector.Import imp : imports) {
        importStmts.add(String.format("#include <%s.h>", imp.getImportFileName()));
      }
      for (String stmt : importStmts) {
        println(stmt);
      }
      newline();
    }
  }

  private void printStaticVars(List<FieldDeclaration> fields) {
    boolean hadStaticVar = false;
    for (FieldDeclaration f : fields) {
      if (Modifier.isStatic(f.getModifiers())) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = f.fragments(); // safe by specification
        for (VariableDeclarationFragment var : fragments) {
          IVariableBinding binding = Types.getVariableBinding(var);
          if (!Types.isPrimitiveConstant(binding)) {
            String name = NameTable.getName(binding);
            Expression initializer = var.getInitializer();
            if (initializer != null) {
              printConstant(name, initializer);
            } else {
              printf("static %s %s;\n", NameTable.javaRefToCpp(f.getType()), name);
            }
            hadStaticVar = true;
          }
        }
      }
    }
    if (hadStaticVar) {
      newline();
    }
  }

  private void printProperties(FieldDeclaration[] fields) {
    int nPrinted = 0;
    for (FieldDeclaration field : fields) {
      if ((field.getModifiers() & Modifier.STATIC) == 0) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> vars = field.fragments(); // safe by definition
        for (VariableDeclarationFragment var : vars) {
          if (var.getName().getIdentifier().startsWith("this$") && superDefinesVariable(var)) {
            // Don't print, as it shadows an inner field in a super class.
            continue;
          }

          String name = NameTable.getName(var.getName());
          ITypeBinding type = Types.getTypeBinding(field.getType());
          String typeString = NameTable.javaRefToCpp(type);
          if (!typeString.endsWith("*")) {
            typeString += " ";
          }

          // Don't emit the getter when there is already a method with the
          // same name.
          // TODO(user,user): Update when getters are merged with property
          // accessors (see issues).
          boolean noGetter = false;
          ITypeBinding declaringClass = Types.getTypeBinding(field.getParent());
          if (declaringClass != null) {
            IMethodBinding[] methods = declaringClass.getDeclaredMethods();
            for (IMethodBinding method : methods) {
              if (method.getName().equals(name) && method.getParameterTypes().length == 0) {
                noGetter = true;
                break;
              }
            }
          }

          String objCFieldName = NameTable.javaFieldToCpp(name);

          // Getter
          if (!noGetter) {
            printf(String.format("- (%s)%s {\n  return %s;\n}\n\n",
                typeString.trim(), name, objCFieldName));
          }

          // Setter
          printf(String.format("- (void)set%s:(%s)new%s {\n",
              NameTable.capitalize(name), typeString.trim(), NameTable.capitalize(name)));
          if (type.isPrimitive()) {
            printf(String.format("  %s = new%s;\n}\n\n",
                objCFieldName, NameTable.capitalize(name)));
          } else if (Options.useReferenceCounting() &&
              !Types.isWeakReference(Types.getVariableBinding(var))) {
            String retentionMethod = type.isEqualTo(Types.getNSString()) ? "copy" : "retain";
            printf(String.format("  [%s autorelease];\n  %s = [new%s %s];\n}\n\n",
                objCFieldName, objCFieldName, NameTable.capitalize(name), retentionMethod));
          } else {
            printf(String.format("  %s = new%s;\n}\n\n",
              objCFieldName, NameTable.capitalize(name)));
          }
          nPrinted++;
        }
      }
    }
    if (nPrinted > 0) {
      newline();
    }
  }

  private void printConstant(String name, Expression initializer) {
    Object constant = initializer.resolveConstantExpressionValue();
    String text = generateExpression(initializer);
    // non-constant initializers were already moved to static blocks
    assert constant != null;
    print("static ");
    if (constant instanceof String) {
      printf("NSString * %s = %s;\n", name, text);
    } else if (constant instanceof Boolean) {
      printf("BOOL %s = %s;\n;", name, ((Boolean) constant).booleanValue() ? "YES" : "NO");
    } else if (constant instanceof Character) {
      printf("unichar %s = %s;\n", name, text);
    } else {
      assert constant instanceof Number;
      Number number = (Number) constant;
      if (constant instanceof Byte) {
        printf("char %s = %d;\n", name, number.byteValue());
      } else if (constant instanceof Double) {
        printf("double %s = %s;\n", name, text);
      } else if (constant instanceof Float) {
        printf("float %s = %s;\n", name, text);
      } else if (constant instanceof Integer) {
        printf("int %s = %s;\n", name, text);
      } else if (constant instanceof Long) {
        printf("long long %s = %s;\n", name, text);
      } else {
        printf("short %s = %d;\n", name, number.shortValue());
      }
    }
  }

   /**
    * If type extends java.lang.Number, add a required implementation of
    * NSValue.objCType().  This can't be implemented as a native method
    * because its return type is const char *.  Since this method overrides
    * the default implementation, the signatures need to match exactly.
    */
  private void printObjCTypeMethod(TypeDeclaration node) {
    ITypeBinding type = Types.getTypeBinding(node);
    if (Types.isJavaNumberType(type)) {
      char objCType;
      String s = type.getName();
      // Strings as case values would be nice here.
      if (s.equals("Byte")) {
        objCType = 'c';
      } else if (s.equals("Double")) {
        objCType = 'd';
      } else if (s.equals("Float")) {
        objCType = 'f';
      } else if (s.equals("Integer")) {
        objCType = 'i';
      } else if (s.equals("Long")) {
        objCType = 'q';
      } else if (s.equals("Short")) {
        objCType = 's';
      } else {
        return;  // Other numeric types will be returned as objects.
      }
      println("- (const char *)objCType {");
      printf("  return \"%c\";\n", objCType);
      println("}\n");
    }
  }
}
