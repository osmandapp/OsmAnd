package net.osmand;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.logging.Log;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;

public class Reshaper {
	private final static Log LOG = PlatformUtil.getLog(Reshaper.class);
	
	public static String reshape(byte[] bytes) {
		try {
			return reshape(new String(bytes, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
	public static String reshape(String s) {
		try {
			ArabicShaping as = new ArabicShaping(ArabicShaping.LETTERS_SHAPE |ArabicShaping.LENGTH_GROW_SHRINK);
			try {
				s = as.shape(s);
			} catch (ArabicShapingException e) {
				LOG.error(e.getMessage(), e);
			}
			Bidi line = new Bidi(s.length(), s.length());
			line.setPara(s,  Bidi.LEVEL_DEFAULT_LTR, null);
//			line.setPara(s, Bidi.LEVEL_DEFAULT_LTR, null);
//			s = line.writeReordered(Bidi.DO_MIRRORING);
//			s = reordered;
			byte direction = line.getDirection();
	        if (direction != Bidi.MIXED) {
	            // unidirectional
				if (line.isLeftToRight()) {
					return s;
				} else {
	        		char[] chs = new char[s.length()];
	        		for(int i = 0; i< chs.length ; i++) {
//	        			chs[i] = s.charAt(chs.length - i - 1);
	        			chs[i] = mirror(s.charAt(chs.length - i - 1));
	        		}
	        		return new String(chs);
	        	}
			} else {
				// mixed-directional
//				System.out.println(s);
//				printSplit("Split", s);
				int count = line.countRuns();
				StringBuilder res = new StringBuilder();
				// iterate over both directional and style runs
				for (int i = 0; i < count; ++i) {
					StringBuilder runs = new StringBuilder();
					BidiRun run = line.getVisualRun(i);
					boolean ltr = run.getDirection() == Bidi.LTR;
					int start = run.getStart();
					int limit = run.getLimit();
					int begin = ltr ? start : limit - 1;
					int end = ltr ? limit : start - 1;
					int ind = begin;
					while (ind != end) {
						char ch = s.charAt(ind);
						if (!ltr) {
							ch = mirror(ch);
						}
						res.append(ch);
						runs.append(ch);
						if (ltr) {
							ind++;
						} else {
							ind--;
						}
						
					}
					printSplit(run.getDirection() + " " + run.getEmbeddingLevel(), runs.toString());
				}
				return res.toString();
			}
		} catch (RuntimeException e) {
			LOG.error(e.getMessage(), e);
			return s;
		}

	}
	private static char mirror(char ch) {
		switch (ch) { 
			case '(': ch = ')'; break;
			case ')': ch = '('; break;
			case '[': ch = ']'; break;
			case ']': ch = '['; break;
		}
		return ch;
	}
	public static void main(String[] args) {
//		char[] c = new char[] {'א', 'ד','ם', ' ', '1', '2'} ;
//		String reshape = "אדם";
//		char[] c = new char[] {'א', 'ד','ם'} ;
//		String reshape = reshape(new String(c));
//		for (int i = 0; i < reshape.length(); i++) {
//			System.out.println(reshape.charAt(i));
//		}
		test2();
		test3();
		test4();
		test5();
	}
	
	private static void test3() {
		String s = "מרכז מסחרי/השלום (40050)";
		String reshape = reshape(s);
		String expected = "(40050) םולשה/ירחסמ זכרמ";
		check(s, reshape, expected);
	}
	
	private static void test5() {
		String s = "מרכז מסחרי/השלום (מרז)";
		String reshape = reshape(s);
		String expected = "(זרמ) םולשה/ירחסמ זכרמ";
		check(s, reshape, expected);
	}
	private static void check(String source, String reshape, String expected) {
		printSplit("Source  ", source);
		printSplit("Expected", expected);
		printSplit("Reshaped", reshape);
		System.out.println(reshape);
		if (!reshape.equals(expected)) {
			throw new IllegalArgumentException(String.format("Bug: expected '%s', reshaped '%s'", expected, reshape));
		}
	}
	private static void printSplit(String p, String source) {
		System.out.print(p);
		System.out.print(": \u2066");
		for(int i = 0; i < source.length(); i++) {
			System.out.print(source.charAt(i));
			System.out.print(" \u200e");
		}
//		System.out.println(Arrays.toString(source.toCharArray()));
		System.out.println();
		System.out.flush();
	}

	private static void test2() {
		String s = "گچ پژ نمکی باللغة العربي";
		String reshape = reshape(s);
		String expected1 = "ﻲﺑﺮﻌﻟﺍ ﺔﻐﻠﻟﺎﺑ ﯽﮑﻤﻧ ﮋﭘ ﭻﮔ";
		String expected2 = "ﻲﺑﺮﻌﻟﺍ ﺔﻐﻠﻟﺎﺑ یکﻤﻧ ژپ چگ";
		check(s, reshape, expected1);
	}
	
	private static void test4() {
		String s = "Abc (123)";
		check(s, reshape(s), s);
	}

}