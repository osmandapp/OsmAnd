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

package com.google.devtools.j2cpp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import com.google.devtools.j2cpp.J2ObjC.Language;

import com.google.devtools.j2objc.util.DeadCodeMap;

/**
 * The set of tool properties, initialized by the command-line arguments.
 * This class was extracted from the main class, to make it easier for
 * other classes to access options.
 *
 * @author Tom Ball
 */
public class Options {

  private static Map<String, String> compilerOptions;
  private static List<String> sourcePathEntries = Lists.newArrayList( "." );
  private static List<String> classPathEntries = Lists.newArrayList( "." );
  private static List<String> pluginPathEntries = Lists.newArrayList();
  private static String pluginOptionString = "";
//  private static List<Plugin> plugins = new ArrayList<Plugin>();
  private static File outputDirectory = new File(".");
  private static Language language = Language.OBJECTIVE_C;
  private static boolean printConvertedSources = false;
  private static boolean ignoreMissingImports = false;
  private static MemoryManagementOption memoryManagementOption = null;
  private static boolean emitLineDirectives = false;
  private static boolean warningsAsErrors = false;
  private static boolean inlineFieldAccess = true;
  private static Map<String, String> methodMappings = Maps.newLinkedHashMap();
  private static boolean generateTestMain = true;

  private static DeadCodeMap deadCodeMap = null;
  private static File proGuardUsageFile = null;

  private static final String JRE_MAPPINGS_FILE = "JRE.mappings";
  private static final List<String> mappingFiles = Lists.newArrayList(JRE_MAPPINGS_FILE);

  private static String fileHeader;
  private static final String FILE_HEADER_KEY = "file-header";
  private static String usageMessage;
  private static String helpMessage;
  private static final String USAGE_MSG_KEY = "usage-message";
  private static final String HELP_MSG_KEY = "help-message";
  private static String temporaryDirectory;
  private static final String XBOOTCLASSPATH = "-Xbootclasspath:";
  private static String bootclasspath = null;
  private static Map<String, String> packagePrefixes = Maps.newHashMap();

  static {
    // Load string resources.
    URL propertiesUrl = Resources.getResource(J2ObjC.class, "J2ObjC.properties");
	Properties properties = new Properties();
    try {
        properties.load(propertiesUrl.openStream());
    } catch (IOException e) {
      System.err.println("unable to access tool properties: " + e);
      System.exit(1);
    }
    fileHeader = properties.getProperty(FILE_HEADER_KEY);
    Preconditions.checkNotNull(fileHeader);
    usageMessage = properties.getProperty(USAGE_MSG_KEY);
    Preconditions.checkNotNull(usageMessage);
    helpMessage = properties.getProperty(HELP_MSG_KEY);
    Preconditions.checkNotNull(helpMessage);
  }

  public static enum MemoryManagementOption { REFERENCE_COUNTING, GC, ARC }
  private static final MemoryManagementOption DEFAULT_MEMORY_MANAGEMENT_OPTION =
      MemoryManagementOption.REFERENCE_COUNTING;

  // Share a single logger so it's level is easily configurable.
  private static final Logger logger = Logger.getLogger(J2ObjC.class.getName());

  /**
   * Load the options from a command-line, returning the arguments that were
   * not option-related (usually files).  If help is requested or an error is
   * detected, the appropriate status method is invoked and the app terminates.
   * @throws IOException
   */
  public static String[] load(String[] args) throws IOException {
    compilerOptions = Maps.newHashMap();
    compilerOptions.put(org.eclipse.jdt.core.JavaCore.COMPILER_SOURCE, "1.6");
    compilerOptions.put(org.eclipse.jdt.core.JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.6");
    compilerOptions.put(org.eclipse.jdt.core.JavaCore.COMPILER_COMPLIANCE, "1.6");
    logger.setLevel(Level.INFO);

    // Create a temporary directory as the sourcepath's first entry, so that
    // modified sources will take precedence over regular files.
    sourcePathEntries = Lists.newArrayList();

    int nArg = 0;
    String[] noFiles = new String[0];
    while (nArg < args.length) {
      String arg = args[nArg];
      if (arg.equals("-classpath")) {
        if (++nArg == args.length) {
          return noFiles;
        }
        classPathEntries = getPathArgument(args[nArg]);
      } else if (arg.equals("-sourcepath")) {
        if (++nArg == args.length) {
          usage();
        }
        sourcePathEntries.addAll(getPathArgument(args[nArg]));
      } else if (arg.equals("-pluginpath")) {
        if (++nArg == args.length) {
          usage();
        }
        pluginPathEntries = getPathArgument(args[nArg]);
      } else if (arg.equals("-pluginoptions")) {
        if (++nArg == args.length){
          usage();
        }
        pluginOptionString = args[nArg];
      } else if (arg.equals("-d")) {
        if (++nArg == args.length) {
          usage();
        }
        outputDirectory = new File(args[nArg]);
      } else if (arg.equals("--mapping")) {
        if (++nArg == args.length) {
          usage();
        }
        mappingFiles.add(args[nArg]);
      } else if (arg.equals("--dead-code-report")) {
        if (++nArg == args.length) {
          usage();
        }
        proGuardUsageFile = new File(args[nArg]);
      } else if (arg.equals("--prefix")) {
        if (++nArg == args.length) {
          usage();
        }
        addPrefixOption(args[nArg]);
      } else if (arg.equals("--prefixes")) {
        if (++nArg == args.length) {
          usage();
        }
        addPrefixesFile(args[nArg]);
      } else if (arg.equals("-x")) {
        if (++nArg == args.length) {
          usage();
        }
        String s = args[nArg];
        if (s.equals("objective-c")) {
          language = Language.OBJECTIVE_C;
        } else if (s.equals("objective-c++")) {
          language = Language.OBJECTIVE_CPP;
        } else {
          usage();
        }
      } else if (arg.equals("--print-converted-sources")) {
        printConvertedSources = true;
      } else if (arg.equals("--ignore-missing-imports")) {
        ignoreMissingImports = true;
      } else if (arg.equals("-use-reference-counting")) {
        checkMemoryManagementOption(MemoryManagementOption.REFERENCE_COUNTING);
      } else if (arg.equals("--inline-field-access")) {
        inlineFieldAccess = true;
      } else if (arg.equals("--no-inline-field-access")) {
        inlineFieldAccess = false;
      } else if (arg.equals("--generate-test-main")) {
        generateTestMain = true;
      } else if (arg.equals("--no-generate-test-main")) {
        generateTestMain = false;
      } else if (arg.equals("-use-gc")) {
        checkMemoryManagementOption(MemoryManagementOption.GC);
      } else if (arg.equals("-use-arc")) {
        checkMemoryManagementOption(MemoryManagementOption.ARC);
      } else if (arg.equals("-g")) {
        emitLineDirectives = true;
      } else if (arg.equals("-Werror")) {
        warningsAsErrors = true;
      } else if (arg.equals("-q") || arg.equals("--quiet")) {
        logger.setLevel(Level.WARNING);
      } else if (arg.equals("-t") || arg.equals("--timing-info")) {
        logger.setLevel(Level.FINE);
      } else if (arg.equals("-v") || arg.equals("--verbose")) {
        logger.setLevel(Level.FINEST);
      } else if (arg.startsWith(XBOOTCLASSPATH)) {
        bootclasspath = arg.substring(XBOOTCLASSPATH.length());
      } else if (arg.startsWith("-h") || arg.equals("--help")) {
        help();
      } else if (arg.startsWith("-")) {
        usage();
      } else {
        break;
      }
      ++nArg;
    }

    if (memoryManagementOption == null) {
      memoryManagementOption = MemoryManagementOption.REFERENCE_COUNTING;
    }

    int nFiles = args.length - nArg;
    String[] files = new String[nFiles];
    for (int i = 0; i < nFiles; i++) {
      String path = args[i + nArg];
      if (path.endsWith(".jar")) {
        appendSourcePath(path);
      }
      files[i] = path;
    }
    return files;
  }

  /**
   * Add prefix option, which has a format of "<package>=<prefix>".
   */
  private static void addPrefixOption(String arg) {
    int i = arg.indexOf('=');

    // Make sure key and value are at least 1 character.
    if (i < 1 || i >= arg.length() - 1) {
      usage();
    }
    String pkg = arg.substring(0, i);
    String prefix = arg.substring(i + 1);
    addPackagePrefix(pkg, prefix);
  }

  /**
   * Add a file map of packages to their respective prefixes, using the
   * Properties file format.
   */
  private static void addPrefixesFile(String filename) throws IOException {
    Properties props = new Properties();
    FileInputStream fis = new FileInputStream(filename);
    props.load(fis);
    fis.close();
    for (String pkg : props.stringPropertyNames()) {
      addPackagePrefix(pkg, props.getProperty(pkg));
    }
  }

  /**
   * Check that the memory management option wasn't previously set to a
   * different value.  If okay, then set the option.
   */
  private static void checkMemoryManagementOption(MemoryManagementOption option) {
    if (memoryManagementOption != null &&
        memoryManagementOption != option) {
      System.err.println("Multiple memory management options cannot be set.");
      usage();
    }
    setMemoryManagementOption(option);
  }

  private static void usage() {
    System.err.println(usageMessage);
    System.exit(1);
  }

  private static void help() {
    System.err.println(helpMessage);
    System.exit(0);
  }

  private static List<String> getPathArgument(String argument) {
    List<String> entries = Lists.newArrayList();
    for (String entry : Splitter.on(':').split(argument)) {
      if (new File(entry).exists()) {  // JDT fails with bad path entries.
        entries.add(entry);
      } else if (entry.startsWith("~/")) {
        // Expand bash/csh tildes, which don't get expanded by the shell
        // first if in the middle of a path string.
        String expanded = System.getProperty("user.home") + entry.substring(1);
        if (new File(expanded).exists()) {
          entries.add(expanded);
        }
      }
    }
    return entries;
  }

  public static Map<String, String> getCompilerOptions() {
    return compilerOptions;
  }

  public static String[] getSourcePathEntries() {
    return sourcePathEntries.toArray(new String[0]);
  }

  public static void appendSourcePath(String entry) {
    sourcePathEntries.add(entry);
  }

  public static void insertSourcePath(int index, String entry) {
    sourcePathEntries.add(index, entry);
  }

  public static String[] getClassPathEntries() {
    return classPathEntries.toArray(new String[classPathEntries.size()]);
  }

  public static String[] getPluginPathEntries() {
    return pluginPathEntries.toArray(new String[pluginPathEntries.size()]);
  }

  public static String getPluginOptionString() {
    return pluginOptionString;
  }

//  public static List<Plugin> getPlugins() {
//    return plugins;
//  }

  public static File getOutputDirectory() {
    return outputDirectory;
  }

  public static Language getLanguage() {
    return language;
  }

  public static boolean printConvertedSources() {
    return printConvertedSources;
  }

  public static boolean ignoreMissingImports() {
    return ignoreMissingImports;
  }

  public static boolean inlineFieldAccess() {
    return inlineFieldAccess;
  }

  public static boolean useReferenceCounting() {
    return memoryManagementOption == MemoryManagementOption.REFERENCE_COUNTING;
  }

  public static boolean useGC() {
    return memoryManagementOption == MemoryManagementOption.GC;
  }

  public static boolean useARC() {
    return memoryManagementOption == MemoryManagementOption.ARC;
  }

  public static MemoryManagementOption getMemoryManagementOption() {
    return memoryManagementOption;
  }

  // Used by tests.
  public static void setMemoryManagementOption(MemoryManagementOption option) {
    memoryManagementOption = option;
  }

  public static void resetMemoryManagementOption() {
    memoryManagementOption = DEFAULT_MEMORY_MANAGEMENT_OPTION;
  }

  public static boolean emitLineDirectives() {
    return emitLineDirectives;
  }

  public static void setEmitLineDirectives(boolean b) {
    emitLineDirectives = b;
  }

  public static boolean treatWarningsAsErrors() {
    return warningsAsErrors;
  }

  public static Map<String, String> getMethodMappings() {
    return methodMappings;
  }

  public static List<String> getMappingFiles() {
    return mappingFiles;
  }

  public static String getUsageMessage() {
    return usageMessage;
  }

  public static String getHelpMessage() {
    return helpMessage;
  }

  public static String getFileHeader() {
    return fileHeader;
  }

  public static boolean generateTestMain() {
    return generateTestMain;
  }

  public static File getProGuardUsageFile() {
    return proGuardUsageFile;
  }

  public static DeadCodeMap getDeadCodeMap() {
    return deadCodeMap;
  }

  public static void setDeadCodeMap(DeadCodeMap map) {
    deadCodeMap = map;
  }

  public static String getBootClasspath() {
    return bootclasspath != null ? bootclasspath : System.getProperty("sun.boot.class.path");
  }

  public static Map<String, String> getPackagePrefixes() {
    return packagePrefixes;
  }

  public static void addPackagePrefix(String pkg, String prefix) {
    packagePrefixes.put(pkg, prefix);
  }

  @VisibleForTesting
  public static void clearPackagePrefixes() {
    packagePrefixes.clear();
  }

  public static String getTemporaryDirectory() throws IOException {
    if (temporaryDirectory != null) {
      return temporaryDirectory;
    }
    File tmpfile = File.createTempFile("j2objc", Long.toString(System.nanoTime()));
    if (!tmpfile.delete()) {
      throw new IOException("Could not delete temp file: " + tmpfile.getAbsolutePath());
    }
    if (!tmpfile.mkdir()) {
      throw new IOException("Could not create temp directory: " + tmpfile.getAbsolutePath());
    }
    temporaryDirectory = tmpfile.getAbsolutePath();
    return temporaryDirectory;
  }

  // Called on exit.  This is done here rather than using File.deleteOnExit(),
  // so the package directories created by the dead-code-eliminator don't have
  // to be tracked.
  public static void deleteTemporaryDirectory() {
    if (temporaryDirectory != null) {
      deleteDir(new File(temporaryDirectory));
      temporaryDirectory = null;
    }
  }

  private static void deleteDir(File dir) {
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) {
        deleteDir(f);
      } else if (f.getName().endsWith(".java")) {
        // Only delete Java files, as other temporary files (like hsperfdata)
        // may also be in tmpdir.
        f.delete();
      }
    }
    dir.delete();  // Will fail if other files in dir, which is fine.
  }
}
