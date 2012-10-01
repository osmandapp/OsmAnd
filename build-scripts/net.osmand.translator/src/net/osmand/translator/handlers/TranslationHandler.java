package net.osmand.translator.handlers;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.devtools.j2objc.J2ObjC;
import com.google.devtools.j2objc.Options;
import com.google.devtools.j2objc.gen.ObjectiveCHeaderGenerator;
import com.google.devtools.j2objc.gen.ObjectiveCImplementationGenerator;
import com.google.devtools.j2objc.sym.Symbols;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ASTNodeException;
import com.google.devtools.j2objc.util.NameTable;


public class TranslationHandler {

	public static void execute(String inFile, PrintStream out) throws IOException {
//		BufferedReader fr = new BufferedReader(new FileReader(new File(inFile)));
//		String readLine;
//		String buf = "";
//		while ((readLine = fr.readLine())  != null) {
//			buf += readLine;
//		}
//		fr.close();
		
		String source = Files.toString(new File(inFile), Charset.defaultCharset());		
		CompilationUnit unit = parse(inFile, source);		
	    
		AST ast = unit.getAST();
	    try {
	      unit.recordModifications();
	      NameTable.initialize(unit);
	      Types.initialize(unit);
	      Symbols.initialize(unit);
	      String newSource = J2ObjC.translate(unit, source);
	      if (unit.types().isEmpty()) {
	        System.out.println("skipping dead file " + inFile);
	      } else {
	        if (Options.printConvertedSources()) {
	          try {
	              File outputFile = new File(Options.getOutputDirectory(), inFile);
	              outputFile.getParentFile().mkdirs();
	              Files.write(newSource, outputFile, Charset.defaultCharset());
	            } catch (IOException e) {
	             e.printStackTrace();
	            }
	        }

	       System.out.println("writing output file(s) to " + Options.getOutputDirectory().getAbsolutePath());
	       ObjectiveCHeaderGenerator.generate(inFile, source, unit);
	       ObjectiveCImplementationGenerator.generate(inFile, Options.getLanguage(), unit, source);
	      }
	    } catch (ASTNodeException e) {
	      e.printStackTrace();
	    } finally {
	    	NameTable.cleanup();
	        Symbols.cleanup();
	        Types.cleanup();
	    }
	}

	  private static CompilationUnit parse(String filename, String source) {
		    System.out.println("parsing " + filename);
		    ASTParser parser = ASTParser.newParser(AST.JLS3);
		    String[] args = new String[]{filename};
		    try {
				Options.load(args);
			} catch (IOException e) {
				e.printStackTrace();
			}
		    Map<String, String> compilerOptions = Options.getCompilerOptions();
		    parser.setCompilerOptions(compilerOptions);
		    parser.setSource(source.toCharArray());
		    parser.setResolveBindings(true);
		    setPaths(parser);
		    parser.setUnitName(filename);
		    CompilationUnit unit = (CompilationUnit) parser.createAST(null);

		    for (IProblem problem : getCompilationErrors(unit)) {
		      if (problem.isError()) {
		        System.out.println(String.format("%s:%s: %s",
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
	
	
///////////////////////////////////////////////////////
////////////////////// OLD ////////////////////////////
///////////////////////////////////////////////////////
	

	private static CompilationUnit parse(String source) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
//	    parser.setCompilerOptions(Options.getCompilerOptions());
	    parser.setSource(source.toCharArray());
	    parser.setResolveBindings(true);
//	    parser.setUnitName(name + ".java");
//	    parser.setEnvironment(new String[] { getComGoogleDevtoolsJ2objcPath() },
//	        new String[] { tempDir.getAbsolutePath() }, null, true);
	    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
//	    assertNoCompilationErrors(unit);
	    return unit;
	}

	private void printProjectInfo(IProject project) throws CoreException, JavaModelException {
		System.out.println("Working in project " + project.getName());
		// Check if we have a Java project
		if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
			IJavaProject javaProject = JavaCore.create(project);
			printPackageInfos(javaProject);
		}
	}

	private void printPackageInfos(IJavaProject javaProject) throws JavaModelException {
		IPackageFragment[] packages = javaProject.getPackageFragments();
		for (IPackageFragment mypackage : packages) {
			// Package fragments include all packages in the
			// classpath
			// We will only look at the package from the source
			// folder
			// K_BINARY would include also included JARS, e.g.
			// rt.jar
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				System.out.println("Package " + mypackage.getElementName());
				printICompilationUnitInfo(mypackage);

			}

		}
	}

	private void printICompilationUnitInfo(IPackageFragment mypackage) throws JavaModelException {
		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
			printCompilationUnitDetails(unit);
		}
	}

	private void printCompilationUnitDetails(ICompilationUnit unit) throws JavaModelException {
		System.out.println("Source file " + unit.getElementName());
		Document doc = new Document(unit.getSource());
		System.out.println("Has number of lines: " + doc.getNumberOfLines());
		printIMethods(unit);
	}

	private void printIMethods(ICompilationUnit unit) throws JavaModelException {
		IType[] allTypes = unit.getAllTypes();
		for (IType type : allTypes) {
			printIMethodDetails(type);
		}
	}

	private void printIMethodDetails(IType type) throws JavaModelException {
		IMethod[] methods = type.getMethods();
		for (IMethod method : methods) {

			System.out.println("Method name " + method.getElementName());
			System.out.println("Signature " + method.getSignature());
			System.out.println("Return Type " + method.getReturnType());

		}
	}
}
