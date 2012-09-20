package net.osmand.translator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.devtools.j2objc.Options;
import com.google.devtools.j2objc.Plugin;
import com.google.devtools.j2objc.gen.ObjectiveCHeaderGenerator;
import com.google.devtools.j2objc.gen.ObjectiveCImplementationGenerator;
import com.google.devtools.j2objc.sym.Symbols;
import com.google.devtools.j2objc.translate.AnonymousClassConverter;
import com.google.devtools.j2objc.translate.Autoboxer;
import com.google.devtools.j2objc.translate.DeadCodeEliminator;
import com.google.devtools.j2objc.translate.DestructorGenerator;
import com.google.devtools.j2objc.translate.GwtConverter;
import com.google.devtools.j2objc.translate.InitializationNormalizer;
import com.google.devtools.j2objc.translate.InnerClassExtractor;
import com.google.devtools.j2objc.translate.JavaToIOSMethodTranslator;
import com.google.devtools.j2objc.translate.JavaToIOSTypeConverter;
import com.google.devtools.j2objc.translate.Rewriter;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ASTNodeException;
import com.google.devtools.j2objc.util.DeadCodeMap;
import com.google.devtools.j2objc.util.NameTable;
import com.google.devtools.j2objc.util.ProGuardUsageParser;

/**
 * Translation tool for generating cpp source files from Java sources.
 */
public class J2ObjC {
  private static String currentFileName;
  private static CompilationUnit currentUnit;
  private static int nFiles = 0;
  private static int nErrors = 0;
  private static int nWarnings = 0;

  public enum Language {
    OBJECTIVE_C(".h"), OBJECTIVE_CPP(".cpp");

    private final String suffix;

    private Language(String suffix) {
      this.suffix = suffix;
    }

    public String getSuffix() {
      return suffix;
    }
  }

  static {
    // Always enable assertions in translator.
    ClassLoader loader = J2ObjC.class.getClassLoader();
    if (loader != null) {
      loader.setPackageAssertionStatus(J2ObjC.class.getPackage().getName(), true);
    }
  }

  private static final Logger logger = Logger.getLogger(J2ObjC.class.getName());

  /**
   * Parse a specified Java source file and generate Objective C header(s)
   * and implementation file(s) from it.
   *
   * @param filename the source file to translate
   */
  void translate(String filename) throws IOException {
    long startTime = System.currentTimeMillis();
    int beginningErrorLevel = getCurrentErrorLevel();
    logger.finest("reading " + filename);

    // Read file
    currentFileName = filename;
    String source = getSource(filename);
    if (source == null) {
      error("no such file: " + filename);
      return;
    }
    long readTime = System.currentTimeMillis();

    // Parse and resolve source
    currentUnit = parse(filename, source);
    long compileTime = System.currentTimeMillis();
    if (getCurrentErrorLevel() > beginningErrorLevel) {
      return; // Continue to next file.
    }

    logger.finest("translating " + filename);
    long translateTime = 0L;
    initializeTranslation(currentUnit);
    try {
      String newSource = translate(currentUnit, source);
      translateTime = System.currentTimeMillis();

      if (currentUnit.types().isEmpty()) {
        logger.finest("skipping dead file " + filename);
      } else {
        if (Options.printConvertedSources()) {
          saveConvertedSource(filename, newSource);
        }

        logger.finest(
            "writing output file(s) to " + Options.getOutputDirectory().getAbsolutePath());

        // write header
        ObjectiveCHeaderGenerator.generate(filename, source, currentUnit);

        // write implementation file
        ObjectiveCImplementationGenerator.generate(
            filename, Options.getLanguage(), currentUnit, source);
      }
    } catch (ASTNodeException e) {
      error(e);
    } finally {
      cleanup();
    }

    long endTime = System.currentTimeMillis();
    printTimingInfo(readTime - startTime, compileTime - readTime, translateTime - compileTime,
        endTime - translateTime, endTime - startTime);
  }

  private static CompilationUnit parse(String filename, String source) {
    logger.finest("parsing " + filename);
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    Map<String, String> compilerOptions = Options.getCompilerOptions();
    parser.setCompilerOptions(compilerOptions);
    parser.setSource(source.toCharArray());
    parser.setResolveBindings(true);
    setPaths(parser);
    parser.setUnitName(filename);
    CompilationUnit unit = (CompilationUnit) parser.createAST(null);

    for (IProblem problem : getCompilationErrors(unit)) {
      if (problem.isError()) {
        error(String.format("%s:%s: %s",
            filename, problem.getSourceLineNumber(), problem.getMessage()));
      }
    }
    return unit;
  }

  private static List<IProblem> getCompilationErrors(CompilationUnit unit) {
    List<IProblem> errors = Lists.newArrayList();
    for (IProblem problem : unit.getProblems()) {
      if (problem.isError()) {
        if (((problem.getID() & IProblem.ImportRelated) > 0) && Options.ignoreMissingImports()) {
          continue;
        } else {
          errors.add(problem);
        }
      }
    }
    return errors;
  }

  private void cleanup() {
    NameTable.cleanup();
    Symbols.cleanup();
    Types.cleanup();
  }

  /**
   * Removes dead types and methods, declared in a dead code map.
   *
   * @param unit the compilation unit created by ASTParser
   * @param source the Java source used by ASTParser
   * @return the rewritten source
   * @throws AssertionError if the dead code eliminator makes invalid edits
   */
  public static String removeDeadCode(CompilationUnit unit, String source) {
    if (Options.getDeadCodeMap() == null) {
      return source;
    }
    logger.finest("removing dead code");
    new DeadCodeEliminator(Options.getDeadCodeMap()).run(unit);

    Document doc = new Document(source);
    TextEdit edit = unit.rewrite(doc, Options.getCompilerOptions());
    try {
      edit.apply(doc);
    } catch (MalformedTreeException e) {
      throw new AssertionError(e);
    } catch (BadLocationException e) {
      throw new AssertionError(e);
    }
    return doc.get();
  }

  private String[] removeDeadCode(String[] files) throws IOException {
    loadDeadCodeMap();
    if (Options.getDeadCodeMap() != null) {
      for (int i = 0; i < files.length; i++) {
        String filename = files[i];
        logger.finest("reading " + filename);

        if (filename.endsWith(".java")) {
          // Read file
          String source = getSource(filename);
          if (source == null) {
            error("no such file: " + filename);
            return files;
          }

          String newPath = removeDeadCode(filename, source);
          if (!filename.equals(newPath)) {
            files[i] = newPath;
          }
        } else if (filename.endsWith(".jar")) {
          File f = new File(filename);
          if (f.exists() && f.isFile()) {
            ZipFile zfile = new ZipFile(f);
            try {
              Enumeration<? extends ZipEntry> enumerator = zfile.entries();
              while (enumerator.hasMoreElements()) {
                ZipEntry entry = enumerator.nextElement();
                String path = entry.getName();
                if (path.endsWith(".java")) {
                  removeDeadCode(path, getSource(path));
                }
              }
            } finally {
              zfile.close();  // Also closes input stream.
            }
          }
        }
      }
    }
    Options.insertSourcePath(0, Options.getTemporaryDirectory());
    return files;
  }

  private String removeDeadCode(String path, String source) throws IOException {
    long startTime = System.currentTimeMillis();
    int beginningErrorLevel = getCurrentErrorLevel();

    // Parse and resolve source
    CompilationUnit unit = parse(path, source);
    if (getCurrentErrorLevel() > beginningErrorLevel) {
      return path;
    }
    initializeTranslation(unit);
    String newSource = removeDeadCode(unit, source);
    if (!newSource.equals(source)) {
      // Save the new source to the tmpdir and update the files list.
      String pkg = unit.getPackage().getName().getFullyQualifiedName();
      File packageDir = new File(Options.getTemporaryDirectory(),
          pkg.replace('.', File.separatorChar));
      packageDir.mkdirs();
      int index = path.lastIndexOf(File.separatorChar);
      String outputName = index >= 0 ? path.substring(index + 1) : path;
      File outFile = new File(packageDir, outputName);
      Files.write(newSource, outFile, Charsets.UTF_8);
      path = outFile.getAbsolutePath();
    }

    long elapsedTime = System.currentTimeMillis() - startTime;
    if (logger.getLevel().intValue() <= Level.FINE.intValue()) {
      System.out.println(
        String.format("dead-code elimination time: %.3f", inSeconds(elapsedTime)));
    }
    return path;
  }

  /**
   * Translates a parsed source file, modifying the compilation unit by
   * substituting core Java type and method references with iOS equivalents.
   * For example, <code>java.lang.Object</code> maps to <code>NSObject</code>,
   * and <code>java.lang.String</code> to <code>NSString</code>. The source is
   * also modified to add support for iOS memory management, extract inner
   * classes, etc.
   * <p>
   * Note: the returned source file doesn't need to be re-parsed, since the
   * compilation unit already reflects the changes (it's useful, though,
   * for dumping intermediate stages).
   * </p>
   *
   * @param unit the compilation unit created by ASTParser
   * @param source the Java source used by ASTParser
   * @return the rewritten source
   * @throws AssertionError if the translator makes invalid edits
   */
  public static String translate(CompilationUnit unit, String source) {

    // Update code that has GWT references.
    new GwtConverter().run(unit);

    // Modify AST to be more compatible with Objective C
    new Rewriter().run(unit);

    // Add auto-boxing conversions.
    new Autoboxer(unit.getAST()).run(unit);

    // Normalize init statements
    new InitializationNormalizer().run(unit);

    // Extract inner and anonymous classes
    new AnonymousClassConverter(unit).run(unit);
    new InnerClassExtractor(unit).run(unit);

    // Translate core Java type use to similar iOS types
    new JavaToIOSTypeConverter().run(unit);
    Map<String, String> methodMappings = Options.getMethodMappings();
    if (methodMappings.isEmpty()) {
      // Method maps are loaded here so tests can call translate() directly.
      loadMappingFiles();
    }
    new JavaToIOSMethodTranslator(unit.getAST(), methodMappings).run(unit);

    // Add dealloc/finalize method(s), if necessary.  This is done
    // after inner class extraction, so that each class releases
    // only its own instance variables.
    new DestructorGenerator().run(unit);

    for (Plugin plugin : Options.getPlugins()) {
      plugin.processUnit(unit);
    }

    // Verify all modified nodes have type bindings
    Types.verifyNode(unit);

    Document doc = new Document(source);
    TextEdit edit = unit.rewrite(doc, Options.getCompilerOptions());
    try {
      edit.apply(doc);
    } catch (MalformedTreeException e) {
      throw new AssertionError(e);
    } catch (BadLocationException e) {
      throw new AssertionError(e);
    }
    return doc.get();
  }

  public static void initializeTranslation(CompilationUnit unit) {
    unit.recordModifications();
    NameTable.initialize(unit);
    Types.initialize(unit);
    Symbols.initialize(unit);
  }

  private void saveConvertedSource(String filename, String content) {
    try {
      File outputFile = new File(Options.getOutputDirectory(), filename);
      outputFile.getParentFile().mkdirs();
      Files.write(content, outputFile, Charset.defaultCharset());
    } catch (IOException e) {
      error(e.getMessage());
    }
  }

  private static void setPaths(ASTParser parser) {
    // Add existing boot classpath after declared path, so that core files
    // referenced, but not being translated, are included.  This only matters
    // when compiling the JRE emulation library sources.
    List<String> fullClasspath = Lists.newArrayList();
    String[] classpathEntries = Options.getClassPathEntries();
    for (int i = 0; i < classpathEntries.length; i++) {
      fullClasspath.add(classpathEntries[i]);
    }
    String bootclasspath = Options.getBootClasspath();
    for (String path : bootclasspath.split(":")) {
      // JDT requires that all path elements exist and can hold class files.
      File f = new File(path);
      if (f.exists() && (f.isDirectory() || path.endsWith(".jar"))) {
        fullClasspath.add(path);
      }
    }
    parser.setEnvironment(fullClasspath.toArray(new String[0]), Options.getSourcePathEntries(),
        null, true);

    // Workaround for ASTParser.setEnvironment() bug, which ignores its
    // last parameter.  This has been fixed in the Eclipse post-3.7 Java7
    // branch.
    try {
      Field field = parser.getClass().getDeclaredField("bits");
      field.setAccessible(true);
      int bits = field.getInt(parser);
      // Turn off CompilationUnitResolver.INCLUDE_RUNNING_VM_BOOTCLASSPATH
      bits &= ~0x20;
      field.setInt(parser, bits);
    } catch (Exception e) {
      // should never happen, since only the one known class is manipulated
      e.printStackTrace();
      System.exit(1);
    }
  }

  private String getSource(String path) throws IOException {
    File file = findSourceFile(path);
    if (file == null) {
      return findArchivedSource(path);
    } else {
      return Files.toString(file, Charset.defaultCharset());
    }
  }

  private File findSourceFile(String filename) {
    File f = getFileOrNull(filename);
    if (f != null) {
      return f;
    }
    for (String pathEntry : Options.getSourcePathEntries()) {
      f = getFileOrNull(pathEntry + File.separatorChar + filename);
      if (f != null) {
        return f;
      }
    }
    return null;
  }

  private String findArchivedSource(String path) throws IOException {
    for (String pathEntry : Options.getSourcePathEntries()) {
      File f = new File(pathEntry);
      if (f.exists() && f.isFile()) {
        ZipFile zfile;
        try {
          zfile = new ZipFile(f);
        } catch (ZipException e) {
          // Not a zip or jar file, so skip it.
          continue;
        }
        ZipEntry entry = zfile.getEntry(path);
        if (entry == null) {
          continue;
        }
        try {
          Reader in = new InputStreamReader(zfile.getInputStream(entry));
          return CharStreams.toString(in);
        } finally {
          zfile.close();  // Also closes input stream.
        }
      }
    }
    return null;
  }

  private File getFileOrNull(String fileName) {
    File f = new File(fileName);
    return f.exists() ? f : null;
  }

  private static void translateSourceJar(J2ObjC compiler, String jarPath) throws IOException {
    File f = new File(jarPath);
    if (f.exists() && f.isFile()) {
      ZipFile zfile = new ZipFile(f);
      try {
        Enumeration<? extends ZipEntry> enumerator = zfile.entries();
        while (enumerator.hasMoreElements()) {
          ZipEntry entry = enumerator.nextElement();
          String path = entry.getName();
          if (path.endsWith(".java")) {
            printInfo("translating " + path);
            compiler.translate(path);
            nFiles++;
          }
        }
      } catch (ZipException e) {
        // Not a zip or jar file, so skip it.
        return;
      } finally {
        zfile.close();  // Also closes input stream.
      }
    }
  }

  /**
   * Report an error during translation.
   */
  public static void error(String message) {
    System.err.println("error: " + message);
    error();
  }

  /**
   * Increment the error counter, but don't display an error diagnostic.
   * This should only be directly called via tests that are testing
   * error conditions.
   */
  public static void error() {
    nErrors++;
  }

  /**
   * Report an ASTVisitor error.
   */
  public static void error(ASTNodeException e) {
    System.err.println(String.format("Internal error, translating %s, line %d\nStack trace:",
        currentFileName, currentUnit.getLineNumber(e.getSourcePosition())));
    nErrors++;
    e.getCause().printStackTrace(System.err);
  }

  /**
   * Report a warning during translation.
   */
  public static void warning(String message) {
    System.err.println("warning: " + message);
    if (Options.treatWarningsAsErrors()) {
      nErrors++;
    } else {
      nWarnings++;
    }
  }

  /**
   * Report an error with a specific AST node.
   */
  public static void error(ASTNode node, String message) {
    int line = getNodeLine(node);
    error(String.format("%s:%s: %s", currentFileName, line, message));
  }

  /**
   * Report a warning with a specific AST node.
   */
  public static void warning(ASTNode node, String message) {
    int line = getNodeLine(node);
    warning(String.format("%s:%s: %s", currentFileName, line, message));
  }

  private int getCurrentErrorLevel() {
    return Options.treatWarningsAsErrors() ? nErrors + nWarnings : nErrors;
  }

  private static int getNodeLine(ASTNode node) {
    CompilationUnit unit = (CompilationUnit) node.getRoot();
    return unit.getLineNumber(node.getStartPosition());
  }

  private static void loadDeadCodeMap() {
    DeadCodeMap map = null;
    File file = Options.getProGuardUsageFile();
    if (file != null) {
      try {
        map = ProGuardUsageParser.parse(Files.newReaderSupplier(file, Charset.defaultCharset()));
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
    Options.setDeadCodeMap(map);
  }

  private static void loadMappingFiles() {
    for (String resourceName : Options.getMappingFiles()) {
      Properties mappings = new Properties();
      try {
        mappings.load(J2ObjC.class.getResourceAsStream(resourceName));
      } catch (IOException e) {
        throw new AssertionError(e);
      }

      Enumeration<?> keyIterator = mappings.propertyNames();
      while (keyIterator.hasMoreElements()) {
        String javaMethod = (String) keyIterator.nextElement();
        String iosMethod = mappings.getProperty(javaMethod);
        Options.getMethodMappings().put(javaMethod, iosMethod);
      }
    }
  }

  @VisibleForTesting
  static void reset() {
    nErrors = 0;
    nWarnings = 0;
    nFiles = 0;
    currentFileName = null;
    currentUnit = null;
  }

  public static int getErrorCount() {
    return nErrors;
  }

  public static int getWarningCount() {
    return nWarnings;
  }

  private static void exit() {
    printInfo(String.format("Translated %d %s: %d errors, %d warnings",
        nFiles, nFiles == 1 ? "file" : "files", nErrors, nWarnings));
    Options.deleteTemporaryDirectory();
    System.exit(nErrors);
  }

  private static void printInfo(String msg) {
    if (logger.getLevel().intValue() <= Level.INFO.intValue()) {
      System.out.println(msg);
    }
  }

  /**
   * Prints time spent in each translation step.  Values are in milliseconds, but displayed
   * as fractional seconds.
   */
  private static void printTimingInfo(long read, long compile, long translate,
      long write, long total) {
    if (logger.getLevel().intValue() <= Level.FINE.intValue()) {
      System.out.println(
        String.format("time: read=%.3f compile=%.3f translate=%.3f write=%.3f total=%.3f",
        inSeconds(read), inSeconds(compile), inSeconds(translate),
        inSeconds(write), inSeconds(total)));
    }
  }

  private static float inSeconds(long milliseconds) {
    return (float) milliseconds / 1000;
  }

  public static String getFileHeader(String sourceFileName) {
    // Template parameters are: source file, user name, date.
    String username = System.getProperty("user.name");
    Date now = new Date();
    String generationDate = DateFormat.getDateInstance(DateFormat.SHORT).format(now);
    return String.format(Options.getFileHeader(), sourceFileName, username, generationDate);
  }

  private static class JarFileLoader extends URLClassLoader {
    public JarFileLoader() {
      super(new URL[]{});
    }

    public void addJarFile(String path) throws MalformedURLException {
      String urlPath = "jar:file://" + path + "!/";
      addURL(new URL(urlPath));
    }
  }

  private static void initPlugins(String[] pluginPaths, String pluginOptionString)
      throws IOException {
    JarFileLoader classLoader = new JarFileLoader();
    for (String path : pluginPaths) {
      if (path.endsWith(".jar")) {
        JarInputStream jarStream = new JarInputStream(new FileInputStream(path));
        classLoader.addJarFile(new File(path).getAbsolutePath());

        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
          String entryName = entry.getName();
          if (!entryName.endsWith(".class")) {
            continue;
          }

          String className =
              entryName.replaceAll("/", "\\.").substring(0, entryName.length() - ".class".length());

          try {
            Class<?> clazz = classLoader.loadClass(className);
            if (Plugin.class.isAssignableFrom(clazz)) {
              Constructor<?> cons = clazz.getDeclaredConstructor();
              Plugin plugin = (Plugin) cons.newInstance();
              plugin.initPlugin(pluginOptionString);
              Options.getPlugins().add(plugin);
            }
          } catch (Exception e) {
            throw new IOException("plugin exception: ", e);
          }
        }
      } else {
        logger.warning("Don't understand plugin path entry: " + path);
      }
    }
  }

  public static void error(Exception e) {
    logger.log(Level.SEVERE, "Exiting due to exception", e);
    System.exit(1);
  }

  /**
   * Entry point for tool.
   *
   * @param args command-line arguments: flags and source file names
   * @throws IOException
   */
  public static void main(String[] args) {
    String[] files = null;
    try {
      files = Options.load(args);
    } catch (IOException e) {
      error(e.getMessage());
      System.exit(1);
    }
    J2ObjC compiler = new J2ObjC();

    try {
      initPlugins(Options.getPluginPathEntries(), Options.getPluginOptionString());
    } catch (IOException e) {
      error(e);
    }

    // Remove dead-code first, so modified file paths are replaced in the
    // translation list.
    int beginningErrorLevel = compiler.getCurrentErrorLevel();
    try {
      files = compiler.removeDeadCode(files);
    } catch (IOException e) {
      error(e.getMessage());
    }
    if (compiler.getCurrentErrorLevel() > beginningErrorLevel) {
      return;
    }

    nFiles = 0;
    for (int i = 0; i < files.length; i++) {
      String file = files[i];
      try {
        if (file.endsWith(".java")) {  // Eclipse may send all project entities.
          printInfo("translating " + file);
          compiler.translate(file);
          nFiles++;
        } else if (file.endsWith(".jar")) {
          translateSourceJar(compiler, file);
        }
      } catch (IOException e) {
        error(e.getMessage());
      }
    }

    for (Plugin plugin : Options.getPlugins()) {
      plugin.endProcessing(Options.getOutputDirectory());
    }

    exit();
  }
}
