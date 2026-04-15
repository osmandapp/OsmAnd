package net.osmand.util;

import net.osmand.CollatorStringMatcher;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class UnicodeDiacriticsTest {

	private static String cp(int... codePoints) {
		StringBuilder sb = new StringBuilder();
		for (int c : codePoints) {
			sb.appendCodePoint(c);
		}
		return sb.toString();
	}

	@Test
	public void testAlignCharsGermanStreet() {
		Assert.assertEquals("auhofstrasse",
				CollatorStringMatcher.alignChars("Auhofstraße").toLowerCase(Locale.ROOT));
	}

	@Test
	public void testStripDiacriticsCafe() {
		Assert.assertEquals("cafe", UnicodeDiacritics.getInstance().stripDiacritics("café").toLowerCase(Locale.ROOT));
	}

	@Test
	public void testAsciiFastPathReturnsSameInstance() {
		String ascii = "plain road123";
		Assert.assertSame(ascii, UnicodeDiacritics.getInstance().stripDiacritics(ascii));
	}

	@Test
	public void testCombiningStripped() {
		Assert.assertEquals("e", UnicodeDiacritics.getInstance().stripDiacritics("\u00e9"));
		Assert.assertEquals("E", UnicodeDiacritics.getInstance().stripDiacritics("\u0045\u0301"));
	}

	@Test
	public void testSupplementaryPlaneCombiningMarkStripped() {
		Assert.assertEquals("a", UnicodeDiacritics.getInstance().stripDiacritics("a\uD800\uDDFD"));
	}

	@Test
	public void testKyiivskaStreetVariantMatchesKyivska() {
		String nameFromData = "Київська вулиця";
		String userQuery = "Киівська вулиця";
		Assert.assertEquals(
				UnicodeDiacritics.getInstance().stripDiacritics(nameFromData).toLowerCase(Locale.ROOT),
				UnicodeDiacritics.getInstance().stripDiacritics(userQuery).toLowerCase(Locale.ROOT));
	}

	@Test
	public void testStripDiacriticsMatchesOsmAndCoreCases() {
		String supplementaryMnOnly = cp(0x1E000);
		String supplementaryMn = cp('A', 0x1E000);
		String supplementaryMc = cp('A', 0x1D165);

		Object[][] cases = new Object[][] {
				{ "empty", "", "" },
				{ "ascii", "Plain ASCII street 123", "Plain ASCII street 123" },
				{ "digits-punctuation", "1234-_. /?!", "1234-_. /?!" },
				{ "latin-precomposed-e-acute", "\u00E9", "e" },
				{ "latin-precomposed-a-ring", "\u00C5", "A" },
				{ "latin-creme-brulee", "Cr\u00E8me Br\u00FBl\u00E9e", "Creme Brulee" },
				{ "latin-a-circumflex-acute", "\u1EA5", "a" },
				{ "latin-decomposed-e-acute", "e\u0301", "e" },
				{ "latin-decomposed-a-ring", "A\u030A", "A" },
				{ "latin-multi-mark", "a\u0323\u0301", "a" },
				{ "latin-unchanged-specials", "\u00DF\u00E6\u0153\u00F8\u0142", "\u00DF\u00E6\u0153\u00F8\u0142" },
				{ "greek-tonos", "\u03AC", "\u03B1" },
				{ "cyrillic-breve", "\u0439", "\u0438" },
				{ "arabic-harakat", "\u0645\u064F\u062D\u064E\u0645\u064E\u0651\u062F", "\u0645\u062D\u0645\u062F" },
				{ "hangul-ga", "\uAC00", "\uAC00" },
				{ "hangul-han", "\uD55C", "\uD55C" },
				{ "hangul-mixed", "\uD55C\uAE00 \uD14C\uC2A4\uD2B8", "\uD55C\uAE00 \uD14C\uC2A4\uD2B8" },
				{ "cjk-unchanged", "\u6771\u4EAC\u6F22\u5B57", "\u6771\u4EAC\u6F22\u5B57" },
				{ "indic-spacing-mark-aa", "\u0915\u093E", "\u0915\u093E" },
				{ "indic-spacing-mark-i", "\u0915\u093F", "\u0915\u093F" },
				{ "indic-nonspacing-mark-u", "\u0915\u0941", "\u0915" },
				{ "indic-anusvara", "\u0915\u0902", "\u0915" },
				{ "indic-nukta", "\u0915\u093C", "\u0915" },
				{ "isolated-combining-mark", "\u0301", "" },
				{ "leading-combining-mark", "\u0301a", "a" },
				{ "trailing-combining-mark", "a\u0301", "a" },
				{ "wrapping-combining-marks", "\u0301a\u0301", "a" },
				{ "supplementary-mn-mark-only", supplementaryMnOnly, "" },
				{ "supplementary-mn-mark", supplementaryMn, "A" },
				{ "supplementary-mc-mark", supplementaryMc, supplementaryMc },
				{ "mixed-scripts", "Cr\u00E8me, \u0645\u064F\u062D\u064E\u0645\u064E\u0651\u062F, \uD55C\uAE00, \u0915\u093E!",
						"Creme, \u0645\u062D\u0645\u062F, \uD55C\uAE00, \u0915\u093E!" },
		};

		for (Object[] row : cases) {
			String name = (String) row[0];
			String in = (String) row[1];
			String expected = (String) row[2];
			String actual = UnicodeDiacritics.getInstance().stripDiacritics(in);
			Assert.assertEquals("case: " + name, expected, actual);
		}
	}
}
