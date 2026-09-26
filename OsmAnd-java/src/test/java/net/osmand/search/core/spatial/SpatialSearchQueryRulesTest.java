package net.osmand.search.core.spatial;

import org.junit.Assert;
import org.junit.Test;

public class SpatialSearchQueryRulesTest {
	@Test
	public void placeAbbreviationRequiresEnglishStreet() {
		SpatialSearchToken token = new SpatialSearchToken(2, "pl", "Pl", 0);
		Assert.assertTrue(token.matchName("Place", null, "en", "street"));
		Assert.assertFalse(token.matchName("Place", null, "en", "poi"));
		Assert.assertFalse(token.matchName("Place", null, "de", "street"));
		Assert.assertTrue(token.matchName("Pl", null, "de", "poi"));
		SpatialSearchToken mount = new SpatialSearchToken(2, "mt", "Mt", 0);
		Assert.assertTrue(mount.matchName("Mount", null, "en", "locality"));
		Assert.assertFalse(mount.matchName("Mount", null, "en", "street"));

		SpatialSearchToken parkway = new SpatialSearchToken(2, "pkwy", "Pkwy", 0);
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		Assert.assertTrue(parkway.getPrefixMatcher(stats, "en").matchKey("parkway"));
		Assert.assertFalse(parkway.getPrefixMatcher(stats, "de").matchKey("parkway"));
		SpatialSearchToken avenue = new SpatialSearchToken(2, "ave", "Ave", 0);
		Assert.assertTrue(avenue.matchName("Esplanade", null, "en_US", "street"));
		Assert.assertFalse(avenue.matchName("Esplanade", null, "en", "street"));
		Assert.assertTrue(avenue.getPrefixMatcher(stats, "en_US").matchKey("esplanade"));
		Assert.assertFalse(avenue.getPrefixMatcher(stats, "en").matchKey("esplanade"));
	}

	@Test
	public void buildingSuffixFollowsTheMapsTheWordWasReadFrom() {
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		SpatialSearchToken ter = new SpatialSearchToken(2, "ter", "Ter", 0);
		Assert.assertFalse(ter.likelyPartOfBuilding());
		ter.getPrefixMatcher(stats, "en_US");
		Assert.assertFalse(ter.likelyPartOfBuilding());
		Assert.assertTrue(ter.matchName("Terrace", null, "en_US", "street"));
		ter.getPrefixMatcher(stats, "fr_FR");
		Assert.assertTrue(ter.likelyPartOfBuilding());
		Assert.assertFalse(ter.likelyPartOfBuilding("en_US"));
		Assert.assertTrue(ter.likelyPartOfBuilding("fr_FR"));
	}
}
