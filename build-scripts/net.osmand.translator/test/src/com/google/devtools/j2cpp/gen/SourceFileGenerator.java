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

import com.google.common.io.Files;
import com.google.devtools.j2cpp.J2ObjC;
import com.google.devtools.j2cpp.Options;
import com.google.devtools.j2cpp.util.NameTable;

import com.google.devtools.j2objc.gen.SourceBuilder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This class handles common actions shared by the header, implementation, and
 * statement generators.
 *
 * @author Tom Ball
 */
public abstract class SourceFileGenerator {
  private final SourceBuilder builder;
  private final CompilationUnit unit;
  private final File outputDirectory;
  private final String sourceFileName;
  private final String source;

  public SourceFileGenerator(String sourceFileName, String source, CompilationUnit unit, boolean emitLineDirectives) {
    builder = new SourceBuilder(unit, emitLineDirectives);
    this.sourceFileName = sourceFileName;
    this.source = source;
    this.unit = unit;
    this.outputDirectory = Options.getOutputDirectory();
  }

  /**
   * Returns the name this source file should be saved as.  For example,
   * foo/bar/Mumble.java translates to $(OUTPUT_DIR)/foo/bar/Mumble.m for
   * Objective-C implementation files.
   * <p>
   * Note: class names are still camel-cased to avoid name collisions.
   */
  protected String getOutputFileName(CompilationUnit node) {
    String javaName = NameTable.getMainJavaName(node, sourceFileName);
    return javaName.replace('.', '/') + getSuffix();
  }

  /**
   * Returns the suffix for files created by this generator.
   */
  protected abstract String getSuffix();

  /**
   * Returns text from within a source code range, where that text is
   * surrounded by JSNI-like tokens ("/&#42;-[" and "]-&#42;/").
   *
   * @param offset the offset into the source to begin searching for
   *     a JSNI region
   * @param length the length of the text range to search
   * @return the extracted text between the JSNI delimiters, or null if
   *     a pair of JSNI delimiters aren't in the specified text range
   */
  protected String extractNativeCode(int offset, int length) {
    String text = source.substring(offset, offset + length);
    int start = text.indexOf("/*-[");  // start after the bracket
    int end = text.lastIndexOf("]-*/");

    //TODO(user): remove after everyone has updated to use the new delimiters.
    if (start == -1 || end <= start) {
      // Check for soon-to-be obsolete GWT JSNI tokens.
      start = text.indexOf("/*-{");
      end = text.lastIndexOf("}-*/");
    }

    if (start == -1 || end <= start) {
      return null;
    }
    return text.substring(start + 4, end);
  }

  protected void save(String path) {
    try {
      File outputFile = new File(outputDirectory, path);
      File dir = outputFile.getParentFile();
      if (dir != null && !dir.exists()) {
        if (!dir.mkdirs()) {
          J2ObjC.warning("cannot create output directory: " + outputDirectory);
        }
      }
      String source = builder.toString();

      // Make sure file ends with a new-line.
      if (!source.endsWith("\n")) {
        source += '\n';
      }

      Files.write(source, outputFile, Charset.defaultCharset());
    } catch (IOException e) {
      J2ObjC.error(e.getMessage());
    } finally {
      reset();
    }
  }

  protected void print(String s) {
    builder.print(s);
  }

  protected void print(char c) {
    builder.print(c);
  }

  protected void printf(String format, Object... args) {
    builder.printf(format, args);
  }

  protected void println(String s) {
    builder.println(s);
  }

  protected void newline() {
    builder.newline();
  }

  protected void indent() {
    builder.indent();
  }

  protected void unindent() {
    builder.unindent();
  }

  protected void printIndent() {
    builder.printIndent();
  }

  protected char[] pad(int n) {
    return builder.pad(n);
  }

  protected void reset() {
    builder.reset();
  }

  protected void syncLineNumbers(ASTNode node) {
    builder.syncLineNumbers(node);
  }

  protected void printStart(String path) {
    builder.printStart(path);
  }

  protected String reindent(String code) {
    return builder.reindent(code);
  }

  protected CompilationUnit getUnit() {
    return unit;
  }

  protected SourceBuilder getBuilder() {
    return builder;
  }

  protected String getSourceFileName() {
    return sourceFileName;
  }
}
