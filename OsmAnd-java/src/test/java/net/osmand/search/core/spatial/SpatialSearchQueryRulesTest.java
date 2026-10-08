package net.osmand.search.core.spatial;

import org.junit.Assert;
import org.junit.Test;

public class SpatialSearchQueryRulesTest {
	@Test
	public void placeAbbreviationRequiresEnglishStreet() {
		// "dr" -> "Drive" is a form of English street names only ("dr" of German maps is an index pair, Doktor)
		SpatialSearchToken token = new SpatialSearchToken(2, "dr", "Dr", 0);
		Assert.assertTrue(token.matchName("Drive", null, "en", "street"));
		Assert.assertFalse(token.matchName("Drive", null, "en", "poi"));
		Assert.assertFalse(token.matchName("Drive", null, "de", "street"));
		Assert.assertTrue(token.matchName("Dr", null, "de", "poi"));
		SpatialSearchToken street = new SpatialSearchToken(2, "st", "St", 0);
		Assert.assertTrue(street.matchName("Street", null, "en", "street"));
		Assert.assertFalse(street.matchName("Street", null, "en", "locality"));
		// a pair of <index> has no query form: the OBF has its alternative name
		Assert.assertFalse(new SpatialSearchToken(2, "pl", "Pl", 0).matchName("Place", null, "en", "street"));

		SpatialSearchToken drive = new SpatialSearchToken(2, "dr", "Dr", 0);
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		Assert.assertTrue(drive.getPrefixMatcher(stats, "en").matchKey("drive"));
		Assert.assertFalse(drive.getPrefixMatcher(stats, "de").matchKey("drive"));
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
		// "ter" -> "Terrace" of English maps is an index pair: no query form
		Assert.assertFalse(ter.matchName("Terrace", null, "en_US", "street"));
		ter.getPrefixMatcher(stats, "fr_FR");
		Assert.assertTrue(ter.likelyPartOfBuilding());
		Assert.assertFalse(ter.likelyPartOfBuilding("en_US"));
		Assert.assertTrue(ter.likelyPartOfBuilding("fr_FR"));
	}

	@Test
	public void formOfAnOwnerIsReadOnlyFromItsIndex() {
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		// "dr" -> "drive" is a form of street names: the keys of a POI index are not read for it
		SpatialSearchToken drive = new SpatialSearchToken(2, "dr", "Dr", 0);
		Assert.assertTrue(drive.getPrefixMatcher(stats, "en", false).matchKey("drive"));
		Assert.assertFalse(drive.getPrefixMatcher(stats, "en", true).matchKey("drive"));
		// "dr" -> "doctor" is a form of every owner: both indexes have it
		Assert.assertTrue(drive.getPrefixMatcher(stats, "en", false).matchKey("doctor"));
		Assert.assertTrue(drive.getPrefixMatcher(stats, "en", true).matchKey("doctor"));
	}

	@Test
	public void kindOfTheMatchWithARealName() {
		Assert.assertEquals(SpatialSearchToken.MatchKind.EXACT,
				new SpatialSearchToken(2, "washington", "Washington", 0).matchKind("Washington Place", "en", "street"));
		Assert.assertEquals(SpatialSearchToken.MatchKind.FORM,
				new SpatialSearchToken(2, "dr", "Dr", 0).matchKind("Washington Drive", "en", "street"));
		Assert.assertNull(new SpatialSearchToken(2, "dr", "Dr", 0).matchKind("Washington Drive", "en", "poi"));
		// a mirror pair (master) is a form on old and new maps alike
		Assert.assertEquals(SpatialSearchToken.MatchKind.FORM,
				new SpatialSearchToken(2, "blvd", "Blvd", 0).matchKind("Sunset Boulevard", "en", "street"));
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
