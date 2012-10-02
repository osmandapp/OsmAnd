package com.google.devtools.j2cpp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.devtools.j2objc.J2ObjC.Language;
import com.google.devtools.j2objc.gen.ObjectiveCHeaderGenerator;
import com.google.devtools.j2objc.gen.ObjectiveCImplementationGenerator;

public abstract class CppGenerationTest extends GenerationTest {
	
	protected String getResourceAstring(String resName) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream("test/resources/" + resName)));
		StringBuilder b = new StringBuilder();
		String rs;
		while ((rs = r.readLine()) != null) {
			b.append(rs).append('\n');
		}
		return b.toString();
	}
	
	protected void printHeaderAndSource(String type) throws IOException {
		System.out.println("-----------------------------HEADER-----------------");
		System.out.println(getTranslatedFile(type+".h"));
		System.out.println();
		System.out.println();
		System.out.println("------------------------------------------------");
		System.out.println("-------------------SOURCE-------------------");
		System.out.println(getTranslatedFile(type+".m"));
	}
	
	protected void cppTranslateSourceFile(String source, String typeName) throws IOException {
		CompilationUnit unit = translateType(typeName, source);
		assertNoCompilationErrors(unit);
		String sourceName = typeName + ".java";
		ObjectiveCHeaderGenerator.generate(sourceName, source, unit);
		ObjectiveCImplementationGenerator.generate(sourceName, Language.OBJECTIVE_C, unit, source);
	}
}
