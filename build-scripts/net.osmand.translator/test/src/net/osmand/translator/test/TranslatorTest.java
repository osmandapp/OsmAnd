package net.osmand.translator.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.osmand.translator.handlers.TranslationHandler;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

import com.google.devtools.j2cpp.CppGenerationTest;
import com.google.devtools.j2cpp.GenerationTest;
import com.google.devtools.j2objc.J2ObjC.Language;
import com.google.devtools.j2objc.gen.ObjectiveCHeaderGenerator;
import com.google.devtools.j2objc.gen.ObjectiveCImplementationGenerator;

public class TranslatorTest extends CppGenerationTest {
	
	
//	public void testSimple() throws IOException{
//		cppTranslateSourceFile(getResourceAstring("MapUtils_1.java"), "MapUtils_1");
//		printHeaderAndSource("MapUtils_1");
//	}
	
	public void testMapUtils() throws IOException{
		cppTranslateSourceFile(getResourceAstring("MapUtils_2.java"), "MapUtils_2");
		printHeaderAndSource("MapUtils_2");
	}

	

}
