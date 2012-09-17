package net.osmand.translator.test;

import java.io.File;
import java.io.IOException;

import net.osmand.translator.handlers.TranslationHandler;

import org.junit.Test;

public class TranslatorTest {
	
	@Test
	public void simpleTest() throws IOException{
		TranslationHandler.execute(new File("test/resources/MapUtils.java").getAbsolutePath(), System.out);
		System.out.println("SUCCESS !!!!! ");
	}

}
