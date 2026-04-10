package net.osmand.util;

import java.text.Normalizer;

/**
 * Strips Unicode combining characters (diacritics) via NFD decomposition, matching the fast path
 * used in OsmAnd-core {@code ICU::stripDiacritics} (see osmandapp/OsmAnd-core#1031).
 * <p>
 * A BMP lookup table mirrors {@code s_isUnsafeChar}: code units that are combining marks or that
 * are not already NFD-normalized, so most strings skip {@link Normalizer} work entirely.
 */
public final class UnicodeDiacritics {

	private static final boolean[] BMP_NEEDS_DIACRITIC_PROCESSING = new boolean[65536];

	static {
		for (int i = 0; i < 65536; i++) {
			char ch = (char) i;
			int type = Character.getType(ch);
			if (type == Character.NON_SPACING_MARK
					|| type == Character.COMBINING_SPACING_MARK
					|| type == Character.ENCLOSING_MARK
					|| !Normalizer.isNormalized(String.valueOf(ch), Normalizer.Form.NFD)) {
				BMP_NEEDS_DIACRITIC_PROCESSING[i] = true;
			}
		}
	}

	private UnicodeDiacritics() {
	}

	private static boolean stringMayNeedDiacriticProcessing(CharSequence input) {
		for (int i = 0, len = input.length(); i < len; i++) {
			if (BMP_NEEDS_DIACRITIC_PROCESSING[input.charAt(i) & 0xffff]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes diacritical marks (combining characters) after NFD normalization.
	 * Does not apply full transliteration (unlike accent-stripping transliterators); aligns with
	 * core {@code stripDiacritics} used for collator / search alignment.
	 */
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
