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

	@Test
	public void formOfAnOwnerIsReadOnlyFromItsIndex() {
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		// "pkwy" -> "parkway" is a form of street names: the keys of a POI index are not read for it
		SpatialSearchToken parkway = new SpatialSearchToken(2, "pkwy", "Pkwy", 0);
		Assert.assertTrue(parkway.getPrefixMatcher(stats, "en", false).matchKey("parkway"));
		Assert.assertFalse(parkway.getPrefixMatcher(stats, "en", true).matchKey("parkway"));
		// "mt" -> "mount" is a form of localities: an address index has them
		SpatialSearchToken mount = new SpatialSearchToken(2, "mt", "Mt", 0);
		Assert.assertTrue(mount.getPrefixMatcher(stats, "en", false).matchKey("mount"));
		Assert.assertFalse(mount.getPrefixMatcher(stats, "en", true).matchKey("mount"));
	}

	@Test
	public void kindOfTheMatchWithARealName() {
		Assert.assertEquals(SpatialSearchToken.MatchKind.EXACT,
				new SpatialSearchToken(2, "washington", "Washington", 0).matchKind("Washington Place", "en", "street"));
		Assert.assertEquals(SpatialSearchToken.MatchKind.FORM,
				new SpatialSearchToken(2, "pl", "Pl", 0).matchKind("Washington Place", "en", "street"));
		Assert.assertNull(new SpatialSearchToken(2, "pl", "Pl", 0).matchKind("Washington Place", "en", "poi"));
		Assert.assertEquals(SpatialSearchToken.MatchKind.REVERSE_FORM,
				new SpatialSearchToken(2, "street", "Street", 0).matchKind("Main St", "en", "street"));
		Assert.assertEquals(SpatialSearchToken.MatchKind.NUMBER,
				new SpatialSearchToken(2, "4", "4", 0).matchKind("4th Avenue", "en", "street"));
		// found by the key of an alternative name of <index>: no word of the real name
		Assert.assertNull(new SpatialSearchToken(2, "ss42", "SS42", 0)
				.matchKind("Strada Statale 42 del Tonale", "it_IT", "street"));
	}

	@Test
	public void buildingSuffixDoesNotDependOnWarmPrefixCache() {
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		// cold: the name index of a French map is read for the word
		SpatialSearchToken cold = new SpatialSearchToken(2, "ter", "Ter", 0);
		cold.getPrefixMatcher(stats, "fr_FR");
		// warm: the keys of the same word come from the cache of that index, no matcher is built
		SpatialSearchToken warm = new SpatialSearchToken(2, "ter", "Ter", 1);
		warm.addReadLocale("fr_FR");
		Assert.assertTrue(cold.likelyPartOfBuilding());
		Assert.assertEquals(cold.likelyPartOfBuilding(), warm.likelyPartOfBuilding());
	}
}
