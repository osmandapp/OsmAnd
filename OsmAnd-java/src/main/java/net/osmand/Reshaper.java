package net.osmand;

import java.io.UnsupportedEncodingException;

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
//		if(true) {
//			return s;
//		}
		try {
			ArabicShaping as = new ArabicShaping(ArabicShaping.LETTERS_SHAPE | ArabicShaping.LENGTH_GROW_SHRINK
					);
			try {
				s = as.shape(s);
			} catch (ArabicShapingException e) {
				LOG.error(e.getMessage(), e);
			}
			Bidi line = new Bidi(s.length(), s.length());
			line.setPara(s, Bidi.LEVEL_DEFAULT_LTR, null);
			byte direction = line.getDirection();
	        if (direction != Bidi.MIXED) {
	            // unidirectional
	        	if(line.isLeftToRight()) {
	        		return s;
	        	} else {
	        		char[] chs = new char[s.length()];
	        		for(int i = 0; i< chs.length ; i++) {
	        			chs[i] = s.charAt(chs.length - i - 1);
	        		}
	        		return new String(chs);
	        	}
			} else {
				// // mixed-directional
				int count = line.countRuns();
				// if (styleRunCount <= 1) {
				// int style = styleRuns[0].style;
				// // iterate over directional runs
				// for (i = 0; i < count; ++i) {
				// run = line.getVisualRun(i);
				// renderRun(text, run.getStart(), run.getLimit(),
				// run.getDirection(), style);
				// }
				// }
				StringBuilder res = new StringBuilder();
				// iterate over both directional and style runs
				for (int i = 0; i < count; ++i) {
					BidiRun run = line.getVisualRun(i);

					int st = run.getStart();
					int e = run.getLimit();
					int j = run.getDirection() == Bidi.LTR ? st : e - 1;
					int l = run.getDirection() == Bidi.LTR ? e : st - 1;
					boolean plus = run.getDirection() == Bidi.LTR;
					while (j != l) {
						res.append(s.charAt(j));
						if (plus) {
							j++;
						} else {
							j--;
						}
					}
				}
				return res.toString();
			}
		} catch (RuntimeException e) {
			LOG.error(e.getMessage(), e);
			return s;
		}

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
//		test3();
	}
	
	private static void test3() {
		String s = "מרכז מסחרי/השלום (40050)";
		String reshape = reshape(s);
		String expected = "(40050) םולשה/ירחסמ זכרמ";
		if (!reshape.equals(expected)) {
			throw new IllegalArgumentException(String.format("Bug: expected '%s', reshaped '%s'", expected, reshape));
		}
	}

	private static void test2() {
		String s = "گچ پژ نمکی باللغة العربي";
		String reshape = reshape(s);
		String expected = "ﻲﺑﺮﻌﻟﺍ ﺔﻐﻠﻟﺎﺑ ﯽﮑﻤﻧ ﮋﭘ ﭻﮔ";
		if (!reshape.equals(expected)) {
			throw new IllegalArgumentException(String.format("Bug: expected '%s', reshaped '%s'", expected, reshape));
		}
	}

}
