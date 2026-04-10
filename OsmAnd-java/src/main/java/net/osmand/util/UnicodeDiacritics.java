package net.osmand.util;

import java.nio.CharBuffer;
import java.text.Normalizer;
import java.util.BitSet;
import java.util.Locale;

public final class UnicodeDiacritics {

	private static final BitSet BMP_MAY_NEED_DIACRITIC_PROCESSING = new BitSet(65536);

	private static final ThreadLocal<StripDiacriticsSearchTiming> SEARCH_TIMING = new ThreadLocal<>();

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
