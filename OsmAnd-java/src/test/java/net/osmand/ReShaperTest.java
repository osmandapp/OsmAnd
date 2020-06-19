package net.osmand;

import java.text.Normalizer;

import org.junit.Test;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

import net.osmand.Reshaper;

public class ReShaperTest {


	// 
	
//	Source  : ⁦ه ‎ە ‎ی ‎ب ‎ە ‎
//	Expected: ⁦ه ‎ە ‎ی ‎ب ‎ە ‎
//	Reshaped: ⁦ە ‎ﺐ ‎ﯾ ‎ە ‎ﻩ 
	@Test
	public void testArabName() throws ArabicShapingException {
		// https://www.compart.com/en/unicode/U+FCD8
//		String source = "\uFEEB\u06d5";
//		System.out.println(new ArabicShaping(0).shape(s));
//		System.out.println("\uFEEB\u06d5");
		String source = "هەیبە";
		String expected = "ەﺐﯾەﻩ";
		String res = Reshaper.reshape(source);
		Reshaper.check(source, res, expected);
	}
	
	
	@Test
	public void test2() throws ArabicShapingException {
		Reshaper.test2();
	}
	
	@Test
	public void test3() throws ArabicShapingException {
		Reshaper.test3();
	}
	
	@Test
	public void test4() throws ArabicShapingException {
		Reshaper.test4();
	}
	
	@Test
	public void test5() throws ArabicShapingException {
		Reshaper.test5();
	}
	
	

}
