package net.osmand.translator.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.osmand.translator.handlers.TranslationHandler;

import org.junit.Test;

import com.google.devtools.j2objc.GenerationTest;

public class TranslatorTest extends GenerationTest {
	
	private String getResourceAstring(String resName) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream("test/resources/"+resName)));
		StringBuilder b = new StringBuilder();
		String rs;
		while((rs = r.readLine()) != null ) {
			b.append(rs).append('\n');
		}
		return b.toString();
	}
	
	public void testSimple() throws IOException{
		String source = "class Test {" +
		    	"boolean b1; boolean b2;" +
		        "Test() { this(true); b2 = true; }" +
		        "Test(boolean b) { b1 = b; }}";
	 String translation = translateSourceFile(getResourceAstring("MapUtils_1.java"), "MapUtils_1", "MapUtils_1.m");
		System.out.println(translation);		
	}

}
