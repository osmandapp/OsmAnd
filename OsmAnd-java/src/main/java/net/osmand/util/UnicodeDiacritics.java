package net.osmand.util;

import java.text.Normalizer;
import java.util.BitSet;
import java.util.Locale;

/**
 * Same semantics as OsmAnd-core {@code ICU::stripDiacritics} (b7285ce): NFD, drop only Mn, NFC;
 * BMP bitset fast path. Optional search-scoped timing via {@link #beginStripDiacriticsSearchTiming()}.
 */
public final class UnicodeDiacritics {

	private static final BitSet BMP_MAY_NEED_DIACRITIC_PROCESSING = new BitSet(65536);

	private static final ThreadLocal<StripDiacriticsSearchTiming> SEARCH_TIMING = new ThreadLocal<>();

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

	public static void beginStripDiacriticsSearchTiming() {
		SEARCH_TIMING.set(new StripDiacriticsSearchTiming());
	}

	public static StripDiacriticsSearchTiming endStripDiacriticsSearchTiming() {
		StripDiacriticsSearchTiming t = SEARCH_TIMING.get();
		SEARCH_TIMING.remove();
		return t;
	}

	public static final class StripDiacriticsSearchTiming {
		long sumNanos;
		int count;

		public int getInvocationCount() {
			return count;
		}

		public String formatLogLine() {
			double summMs = sumNanos / 1_000_000.0;
			double averMs = count == 0 ? 0.0 : (sumNanos / (double) count) / 1_000_000.0;
			return String.format(Locale.US, "stripDiacritics: summ: %.3f ms, cnt: %d, aver: %.9f ms",
					summMs, count, averMs);
		}
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
			if ((cp <= 0xFFFF && BMP_MAY_NEED_DIACRITIC_PROCESSING.get(cp))
					|| (cp > 0xFFFF && Character.getType(cp) == Character.NON_SPACING_MARK)) {
				return true;
			}
			i += step;
		}
		return false;
	}

	public static String stripDiacritics(String input) {
		StripDiacriticsSearchTiming timing = SEARCH_TIMING.get();
		if (timing != null) {
			long t0 = System.nanoTime();
			try {
				return stripDiacriticsBody(input);
			} finally {
				timing.sumNanos += System.nanoTime() - t0;
				timing.count++;
			}
		}
		return stripDiacriticsBody(input);
	}

	private static String stripDiacriticsBody(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}
		if (!stringMayNeedDiacriticProcessing(input)) {
			return input;
		}
		return stripDiacriticsNormalizeAndStrip(input);
	}

	private static String stripDiacriticsNormalizeAndStrip(String input) {
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
		return result.equals(input) ? input : result;
	}
}
