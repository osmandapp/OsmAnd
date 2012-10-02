package net.osmand.translator.test;

import java.io.IOException;

import com.google.devtools.j2cpp.CppGenerationTest;

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
