package net.osmand.util;

import java.nio.CharBuffer;
import java.text.Normalizer;
import java.util.BitSet;

public final class UnicodeDiacritics {

	private static final BitSet BMP_MAY_NEED_DIACRITIC_PROCESSING = new BitSet(65536);

	static {
		char[] buf = new char[2];
		for (int cp = 0; cp < 65536; cp++) {
			if (codePointMayNeedDiacriticProcessingFill(cp, buf)) {
				BMP_MAY_NEED_DIACRITIC_PROCESSING.set(cp);
			}
		}
	}

	private UnicodeDiacritics() {
	}

	private static boolean codePointMayNeedDiacriticProcessingFill(int cp, char[] buf) {
		int type = Character.getType(cp);
		if (type == Character.NON_SPACING_MARK
				|| type == Character.COMBINING_SPACING_MARK
				|| type == Character.ENCLOSING_MARK) {
			return true;
		}
		int n = Character.toChars(cp, buf, 0);
		return !Normalizer.isNormalized(CharBuffer.wrap(buf, 0, n), Normalizer.Form.NFD);
	}

	private static boolean supplementaryMayNeedDiacriticProcessing(int cp) {
		int type = Character.getType(cp);
		if (type == Character.NON_SPACING_MARK
				|| type == Character.COMBINING_SPACING_MARK
				|| type == Character.ENCLOSING_MARK) {
			return true;
		}
		return !Normalizer.isNormalized(new String(Character.toChars(cp)), Normalizer.Form.NFD);
	}

	private static boolean stringMayNeedDiacriticProcessing(CharSequence input) {
		for (int i = 0, len = input.length(); i < len; ) {
			int cp = Character.codePointAt(input, i);
			i += Character.charCount(cp);
			if (cp < 65536) {
				if (BMP_MAY_NEED_DIACRITIC_PROCESSING.get(cp)) {
					return true;
				}
			} else if (supplementaryMayNeedDiacriticProcessing(cp)) {
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
		StringBuilder result = new StringBuilder(decomposed.length());
		for (int i = 0, len = decomposed.length(); i < len; ) {
			int cp = decomposed.codePointAt(i);
			i += Character.charCount(cp);
			int type = Character.getType(cp);
			if (type != Character.NON_SPACING_MARK
					&& type != Character.COMBINING_SPACING_MARK
					&& type != Character.ENCLOSING_MARK) {
				result.appendCodePoint(cp);
			}
		}
		return result.toString();
	}
}
