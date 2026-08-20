package net.osmand.util;

import java.text.Normalizer;
import java.util.BitSet;

/**
 * Same semantics as OsmAnd-core {@code ICU::stripDiacritics} (b7285ce): NFD, drop only Mn, NFC;
 * BMP bitset fast path.
 */
public final class UnicodeDiacritics {

	private static final boolean[] BMP_MAY_NEED_DIACRITIC_PROCESSING = new boolean[65536]; // BitSet is 4x slower
	private String lastKey;
	private String lastValue;

	private static UnicodeDiacritics instance;

	public static UnicodeDiacritics getInstance() {
		if (instance == null) {
			instance = new UnicodeDiacritics();
		}
		return instance;
	}

	private UnicodeDiacritics() {
		char[] buf = new char[2];
		for (int cp = 0; cp < 65536; cp++) {
			if (bmpCodePointNeedsDiacriticProcessingInit(cp, buf)) {
				BMP_MAY_NEED_DIACRITIC_PROCESSING[cp] = true;
			}
		}
	}

	private boolean bmpCodePointNeedsDiacriticProcessingInit(int cp, char[] buf) {
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

	private boolean stringMayNeedDiacriticProcessing(String input) {
		for (int i = 0, len = input.length(); i < len; ) {
			char ch = input.charAt(i);
			int cp;
			int step;
			if (Character.isHighSurrogate(ch) && i + 1 < len && Character.isLowSurrogate(input.charAt(i + 1))) {
				cp = Character.toCodePoint(ch, input.charAt(i + 1));
				step = 2;
			} else {
				cp = ch;
				step = 1;
			}
			if ((cp <= 0xFFFF && BMP_MAY_NEED_DIACRITIC_PROCESSING[cp])
					|| (cp > 0xFFFF && Character.getType(cp) == Character.NON_SPACING_MARK)) {
				return true;
			}
			i += step;
		}
		return false;
	}

	public String stripDiacritics(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}
		if (!stringMayNeedDiacriticProcessing(input)) {
			return input;
		}
		if (input.equals(lastKey)) {
			return lastValue;
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
		String result = Normalizer.normalize(filtered.toString(), Normalizer.Form.NFC);
		lastKey = input;
		lastValue = result;
		return result.equals(input) ? input : result;
	}
}
