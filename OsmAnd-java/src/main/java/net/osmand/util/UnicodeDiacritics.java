package net.osmand.util;

import java.text.Normalizer;
import java.util.BitSet;

public final class UnicodeDiacritics {

	private static final BitSet BMP_MAY_NEED_DIACRITIC_PROCESSING = new BitSet(65536);

	static {
		char[] buf = new char[2];
		for (int cp = 0; cp < 65536; cp++) {
			if (bmpCodePointNeedsDiacriticProcessingInit(cp, buf)) {
				BMP_MAY_NEED_DIACRITIC_PROCESSING.set(cp);
			}
		}
	}

	private UnicodeDiacritics() {
	}

	private static boolean bmpCodePointNeedsDiacriticProcessingInit(int cp, char[] buf) {
		if (Character.getType(cp) == Character.NON_SPACING_MARK) {
			return true;
		}
		int n = Character.toChars(cp, buf, 0);
		String decomposed = Normalizer.normalize(new String(buf, 0, n), Normalizer.Form.NFD);
		for (int i = 0, len = decomposed.length(); i < len; ) {
			int c = decomposed.codePointAt(i);
			i += Character.charCount(c);
			if (Character.getType(c) == Character.NON_SPACING_MARK) {
				return true;
			}
		}
		return false;
	}

	private static boolean stringMayNeedDiacriticProcessing(CharSequence input) {
		for (int i = 0, len = input.length(); i < len; ) {
			int cp = Character.codePointAt(input, i);
			i += Character.charCount(cp);
			if (cp <= 0xFFFF) {
				if (BMP_MAY_NEED_DIACRITIC_PROCESSING.get(cp)) {
					return true;
				}
			} else if (Character.getType(cp) == Character.NON_SPACING_MARK) {
				return true;
			}
		}
		return false;
	}

	public static String stripDiacritics(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}
		if (!stringMayNeedDiacriticProcessing(input)) {
			return input;
		}
		String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
		StringBuilder filtered = new StringBuilder(decomposed.length());
		for (int i = 0, len = decomposed.length(); i < len; ) {
			int cp = decomposed.codePointAt(i);
			i += Character.charCount(cp);
			if (Character.getType(cp) != Character.NON_SPACING_MARK) {
				filtered.appendCodePoint(cp);
			}
		}
		return Normalizer.normalize(filtered.toString(), Normalizer.Form.NFC);
	}
}
