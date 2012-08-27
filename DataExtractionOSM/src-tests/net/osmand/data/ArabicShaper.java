package net.osmand.data;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

public class ArabicShaper {
	
	
	public static void main(String[] args) throws ArabicShapingException {

		ArabicShaping as = new ArabicShaping(ArabicShaping.LETTERS_SHAPE | 
				ArabicShaping.LENGTH_GROW_SHRINK);
		String s = "אנשים 12";
//		for (int i = 0; i < s.length(); i++) {
//			System.out.println(s.charAt(i));
//		}
		Bidi bd = new Bidi(s.length(), s.length());
		bd.setPara(s, Bidi.LEVEL_DEFAULT_LTR, null);
		System.out.println(bd.baseIsLeftToRight());
		String r = as.shape(s);
//		for (int i = 0; i < r.length(); i++) {
//			System.out.println(r.charAt(i));
//		}
		System.out.println(r);
	}

}
