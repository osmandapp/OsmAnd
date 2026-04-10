package net.osmand.util;

import java.nio.CharBuffer;
import java.text.Normalizer;

/**
 * Strips Unicode combining characters (diacritics) via NFD decomposition, matching the fast path
 * used in OsmAnd-core {@code ICU::stripDiacritics} (see osmandapp/OsmAnd-core#1031).
 * <p>
 * BMP entries are cached lazily (see review: avoid huge one-time cost at class load). Supplementary
 * code points are checked on demand without precomputing the whole BMP at class initialization.
 */
public final class UnicodeDiacritics {

	private static final byte BMP_UNKNOWN = 0;
	private static final byte BMP_DOES_NOT_NEED = 1;
	private static final byte BMP_NEEDS = 2;

	private static final byte[] BMP_DIACRITIC_CACHE = new byte[65536];
	private static final char[] NORM_BUF = new char[2];

	private UnicodeDiacritics() {
	}

	private static boolean bmpCodePointNeedsDiacriticProcessing(int cp) {
		byte v = BMP_DIACRITIC_CACHE[cp];
		if (v != BMP_UNKNOWN) {
			return v == BMP_NEEDS;
		}
		synchronized (NORM_BUF) {
			v = BMP_DIACRITIC_CACHE[cp];
			if (v != BMP_UNKNOWN) {
				return v == BMP_NEEDS;
			}
			boolean needs = computeMayNeedDiacriticProcessingNoSync(cp);
			BMP_DIACRITIC_CACHE[cp] = needs ? BMP_NEEDS : BMP_DOES_NOT_NEED;
			return needs;
		}
	}

	private static boolean computeMayNeedDiacriticProcessingNoSync(int cp) {
		int type = Character.getType(cp);
		if (type == Character.NON_SPACING_MARK
				|| type == Character.COMBINING_SPACING_MARK
				|| type == Character.ENCLOSING_MARK) {
			return true;
		}
		int n = Character.toChars(cp, NORM_BUF, 0);
		return !Normalizer.isNormalized(CharBuffer.wrap(NORM_BUF, 0, n), Normalizer.Form.NFD);
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
				if (bmpCodePointNeedsDiacriticProcessing(cp)) {
					return true;
				}
			} else if (supplementaryMayNeedDiacriticProcessing(cp)) {
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
