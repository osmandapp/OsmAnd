package net.osmand.util;

import net.osmand.CollatorStringMatcher;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class UnicodeDiacriticsTest {

	@Test
	public void testAlignCharsGermanStreet() {
		Assert.assertEquals("auhofstrasse",
				CollatorStringMatcher.alignChars("Auhofstraße").toLowerCase(Locale.ROOT));
	}

	@Test
	public void testStripDiacriticsCafe() {
		Assert.assertEquals("cafe", UnicodeDiacritics.stripDiacritics("café").toLowerCase(Locale.ROOT));
	}

	@Test
	public void testAsciiFastPathReturnsSameInstance() {
		String ascii = "plain road123";
		Assert.assertSame(ascii, UnicodeDiacritics.stripDiacritics(ascii));
	}

	@Test
	public void testCombiningStripped() {
		Assert.assertEquals("e", UnicodeDiacritics.stripDiacritics("\u00e9"));
		Assert.assertEquals("E", UnicodeDiacritics.stripDiacritics("\u0045\u0301"));
	}

	@Test
	public void testSupplementaryPlaneCombiningMarkStripped() {
		Assert.assertEquals("a", UnicodeDiacritics.stripDiacritics("a\uD800\uDDFD"));
	}

	@Test
	public void testKyiivskaStreetVariantMatchesKyivska() {
		String nameFromData = "Київська вулиця";
		String userQuery = "Киівська вулиця";
		Assert.assertEquals(
				UnicodeDiacritics.stripDiacritics(nameFromData).toLowerCase(Locale.ROOT),
				UnicodeDiacritics.stripDiacritics(userQuery).toLowerCase(Locale.ROOT));
	}
}
