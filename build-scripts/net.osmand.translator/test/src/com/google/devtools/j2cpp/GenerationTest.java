package com.google.devtools.j2cpp;
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



import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.devtools.j2objc.J2ObjC;
import com.google.devtools.j2objc.Options;
import com.google.devtools.j2objc.J2ObjC.Language;
import com.google.devtools.j2objc.gen.ObjectiveCHeaderGenerator;
import com.google.devtools.j2objc.gen.ObjectiveCImplementationGenerator;
import com.google.devtools.j2objc.gen.SourceBuilder;
import com.google.devtools.j2objc.gen.StatementGenerator;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * Tests code generation. A string containing the source code for a list of Java
 * statements is parsed and translated, then iOS code is generated for one or
 * more of those statements for comparison in a specific generation test.
 *
 * @author Tom Ball
 */
public abstract class GenerationTest extends TestCase {
  protected File tempDir;

  @Override
  protected void setUp() throws IOException {
    tempDir = createTempDir();
    Options.load(new String[] { "-d", tempDir.getAbsolutePath() });
  }

  @Override
  protected void tearDown() throws Exception {
	  System.out.println(tempDir.getAbsolutePath());
//    deleteTempDir(tempDir);
//    J2ObjC.reset();
  }

  /**
   * Translate a string of Java statement(s) into a list of
   * JDT DOM Statements.  Although JDT has support for statement
   * parsing, it doesn't resolve them.  The statements are therefore
   * wrapped in a type declaration so they having bindings.
   */
  protected List<Statement> translateStatements(String stmts) {
    // Wrap statements in test class, so type resolution works.
    String source = "public class Test { void test() { " + stmts + "}}";
    CompilationUnit unit = translateType("Test", source);
    assertNoCompilationErrors(unit);
    final List<Statement> statements = Lists.newArrayList();
    unit.accept(new ASTVisitor() {
      @SuppressWarnings("unchecked")
      @Override
      public boolean visit(MethodDeclaration node) {
        if (node.getName().getIdentifier().equals("test")) {
          statements.addAll(node.getBody().statements());
        }
        return false;
      }
    });
    return statements;
  }

  /**
   * Translates Java source, as contained in a source file.
   *
   * @param name the name of the public type being declared
   * @param source the source code
   * @return the translated compilation unit
   */
  protected CompilationUnit translateType(String name, String source) {
    return translateType(name, source, true);
  }

  /**
   * Translates Java source, as contained in a source file.
   *
   * @param name the name of the public type being declared
   * @param source the source code
   * @param assertErrors assert that no compilation errors were reported
   * @return the translated compilation unit
   */
  protected CompilationUnit translateType(String name, String source, boolean assertErrors) {
    CompilationUnit unit = compileType(name, source, assertErrors);
    J2ObjC.initializeTranslation(unit);
    J2ObjC.removeDeadCode(unit, source);
    J2ObjC.translate(unit, source);
    return unit;
  }

  /**
   * Compiles Java source, as contained in a source file.
   *
   * @param name the name of the public type being declared
   * @param source the source code
   * @return the parsed compilation unit
   */
  protected CompilationUnit compileType(String name, String source) {
    return compileType(name, source, true);
  }

  /**
   * Compiles Java source, as contained in a source file.
   *
   * @param name the name of the public type being declared
   * @param source the source code
   * @param assertErrors assert that no compilation errors were reported
   * @return the parsed compilation unit
   */
  protected CompilationUnit compileType(String name, String source, boolean assertErrors) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setCompilerOptions(Options.getCompilerOptions());
    parser.setSource(source.toCharArray());
    parser.setResolveBindings(true);
    parser.setUnitName(name + ".java");
    parser.setEnvironment(new String[] { getComGoogleDevtoolsJ2objcPath() },
        new String[] { tempDir.getAbsolutePath() }, null, true);
    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
    assertNoCompilationErrors(unit);
    return unit;
  }

  protected String getComGoogleDevtoolsJ2objcPath() {
    ClassLoader loader = GenerationTest.class.getClassLoader();

    // Used when running tests with Maven.
    String classpath = "";
    File classesDir = new File("target/classes/");
    if (classesDir.exists()) {
      classpath = classesDir.getAbsolutePath();
    }

    if (loader instanceof URLClassLoader) {
      URL[] urls = ((URLClassLoader) GenerationTest.class.getClassLoader()).getURLs();
      String encoding = System.getProperty("file.encoding");
      for (int i = 0; i < urls.length; i++) {
        try {
          if (classpath.length() > 0) {
            classpath += File.pathSeparatorChar;
          }
          classpath += URLDecoder.decode(urls[i].getFile(), encoding);
        } catch (UnsupportedEncodingException e) {
          throw new AssertionError("System doesn't have the default encoding");
        }
      }
    }
    return classpath;
  }

  protected String generateStatement(Statement statement) {
    return StatementGenerator.generate(statement,
        Collections.<IVariableBinding>emptySet(), false, SourceBuilder.BEGINNING_OF_FILE).trim();
  }

  /**
   * Returns a newly-created temporary directory.
   */
  protected File createTempDir() throws IOException {
    File tempDir = File.createTempFile("testout", ".tmp");
    tempDir.delete();
    tempDir.mkdir();
    return tempDir;
  }

  /**
   * Recursively delete specified directory. Files.deleteRecursively() won't
   * work on Macs, because OS X returns a tempfile spec which has a symlink
   * that deleteRecursively() explicitly won't follow.
   */
  protected void deleteTempDir(File dir) {
    if (dir.exists()) {
      for (File f : dir.listFiles()) {
        if (f.isDirectory()) {
          deleteTempDir(f);
        } else {
          f.delete();
        }
      }
      dir.delete();
    }
  }

  /**
   * Asserts that a compilation unit is error-free.
   */
  protected void assertNoCompilationErrors(CompilationUnit unit) {
    for (IProblem problem : unit.getProblems()) {
      assertFalse(problem.getMessage(), problem.isError());
    }
  }

  /**
   * Asserts that translated source contains a specified string.
   */
  protected void assertTranslation(String translation, String expected) {
    if (!translation.contains(expected)) {
      fail("expected:\"" + expected + "\" in:\n" + translation);
    }
  }


  /**
   * Translate a Java method into a JDT DOM MethodDeclaration.  Although JDT
   * has support for parsing methods, it doesn't resolve them.  The statements
   * are therefore wrapped in a type declaration so they having bindings.
   */
  protected MethodDeclaration translateMethod(String method) {
    // Wrap statements in test class, so type resolution works.
    String source = "public class Test { " + method + "}";
    CompilationUnit unit = translateType("Test", source);
    assertNoCompilationErrors(unit);
    final MethodDeclaration[] result = new MethodDeclaration[1];
    unit.accept(new ASTVisitor() {
      @Override
      public boolean visit(MethodDeclaration node) {
        assert result[0] == null;
        result[0] = node;
        return false;
      }
    });
    return result[0];
  }

  /**
   * Translate a Java source file contents, returning the contents of either
   * the generated header or implementation file.
   *
   * @param typeName the name of the main type defined by this source file
   * @param fileName the name of the file whose contents should be returned,
   *                 which is either the Obj-C header or implementation file
   */
  protected String translateSourceFile(String typeName, String fileName) throws IOException {
    String source = getTranslatedFile(typeName + ".java");
    return translateSourceFile(source, typeName, fileName);
  }

  /**
   * Translate a Java source file contents, returning the contents of either
   * the generated header or implementation file.
   *
   * @param source the source file contents
   * @param typeName the name of the main type defined by this source file
   * @param fileName the name of the file whose contents should be returned,
   *                 which is either the Obj-C header or implementation file
   */
  protected String translateSourceFile(String source, String typeName, String fileName)
      throws IOException {
    CompilationUnit unit = translateType(typeName, source);
    assertNoCompilationErrors(unit);
    String sourceName = typeName + ".java";
    ObjectiveCHeaderGenerator.generate(sourceName, source, unit);
    ObjectiveCImplementationGenerator.generate(sourceName, Language.OBJECTIVE_C, unit, source);
    return getTranslatedFile(fileName);
  }

  protected void addSourceFile(String source, String fileName) throws IOException {
    File file = new File(tempDir, fileName);
    Files.write(source, file, Charset.defaultCharset());
  }

  /**
   * Return the contents of a previously translated file, made by a call to
   * {@link #translateMethod} above.
   */
  protected String getTranslatedFile(String fileName) throws IOException {
    File f = new File(tempDir, fileName);
    assertTrue(f.exists());
    return Files.toString(f, Charset.defaultCharset());
  }
}
