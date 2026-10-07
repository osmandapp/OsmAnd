package net.osmand.search.core.spatial;

import org.junit.Assert;
import org.junit.Test;

public class SpatialSearchQueryRulesTest {

	private static SpatialSearchToken token(String word, String locale) {
		SpatialSearchToken t = new SpatialSearchToken(2, word.toLowerCase(), word, 0);
		t.setLocale(locale);
		return t;
	}

	@Test
	public void placeAbbreviationRequiresEnglishStreet() {
		SpatialSearchToken pl = token("Pl", "en");
		Assert.assertTrue(pl.matchName("Place", null, "street"));
		Assert.assertFalse(pl.matchName("Place", null, "poi"));
		pl.setLocale("de");
		Assert.assertFalse(pl.matchName("Place", null, "street"));
		Assert.assertTrue(pl.matchName("Pl", null, "poi"));
		SpatialSearchToken mount = token("Mt", "en");
		Assert.assertTrue(mount.matchName("Mount", null, "locality"));
		Assert.assertFalse(mount.matchName("Mount", null, "street"));
	}

	@Test
	public void prefixMatcherFollowsTheLocale() {
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		SpatialSearchToken parkway = token("Pkwy", "en");
		Assert.assertTrue(parkway.getPrefixMatcher(stats).matchKey("parkway"));
		parkway.setLocale("de");
		Assert.assertFalse(parkway.getPrefixMatcher(stats).matchKey("parkway"));
		SpatialSearchToken avenue = token("Ave", "en_US");
		Assert.assertTrue(avenue.matchName("Esplanade", null, "street"));
		Assert.assertTrue(avenue.getPrefixMatcher(stats).matchKey("esplanade"));
		avenue.setLocale("en");
		Assert.assertFalse(avenue.matchName("Esplanade", null, "street"));
		Assert.assertFalse(avenue.getPrefixMatcher(stats).matchKey("esplanade"));
	}

	@Test
	public void buildingSuffixFollowsTheLocale() {
		SpatialSearchToken ter = token("Ter", "en_US");
		Assert.assertFalse(ter.likelyPartOfBuilding());
		Assert.assertTrue(ter.matchName("Terrace", null, "street"));
		ter.setLocale("fr_FR");
		Assert.assertTrue(ter.likelyPartOfBuilding());
	}
}
