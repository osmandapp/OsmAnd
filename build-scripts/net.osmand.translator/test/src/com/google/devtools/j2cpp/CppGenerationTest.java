package com.google.devtools.j2cpp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.common.io.Files;
import com.google.devtools.j2cpp.gen.CppHeaderGenerator;
import com.google.devtools.j2objc.J2ObjC.Language;
//import com.google.devtools.j2objc.gen.ObjectiveCImplementationGenerator;

public abstract class CppGenerationTest extends GenerationTest {
	
	protected String getResourceAstring(String resName) throws IOException {
		return  Files.toString(new File("test/resources/" + resName), Charset.defaultCharset());
	}
	
	protected void printHeaderAndSource(String type) throws IOException {
		System.out.println("-----------------------------HEADER-----------------");
		System.out.println(getTranslatedFile(type+".h"));
		System.out.println();
//		TODO commented for testing a header only 
//		System.out.println();
//		System.out.println("------------------------------------------------");
//		System.out.println("-------------------SOURCE-------------------");
//		System.out.println(getTranslatedFile(type+".m"));
	}
	
	  protected void assertNoCompilationErrors(CompilationUnit unit) {
	    for (IProblem problem : unit.getProblems()) {
	    	if(problem.isError()){
	    	System.err.println(problem.getMessage());
	    	}
	    }
	    	for (IProblem problem : unit.getProblems()) {
	      assertFalse(problem.getMessage(), problem.isError());
	    }
	  }
	
	protected void cppTranslateSourceFile(String source, String typeName) throws IOException {
		CompilationUnit unit = translateType(typeName, source);
		assertNoCompilationErrors(unit);
		String sourceName = typeName + ".java";
		CppHeaderGenerator.generate(sourceName, source, unit);
//		ObjectiveCImplementationGenerator.generate(sourceName, Language.OBJECTIVE_C, unit, source);
	}
}
