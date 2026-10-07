package net.osmand.search.core.spatial;

import net.osmand.binary.SearchRules;
import org.junit.Assert;
import org.junit.Test;

public class SpatialSearchQueryRulesTest {
	private final SearchRules searchRules = new SearchRules();

	private SpatialSearchToken token(String word) {
		return new SpatialSearchToken(searchRules, 2, word.toLowerCase(), word, 0);
	}

	@Test
	public void placeAbbreviationRequiresEnglishStreet() {
		SpatialSearchToken pl = token("Pl");
		Assert.assertTrue(pl.matchName("Place", null, "en", "street"));
		Assert.assertFalse(pl.matchName("Place", null, "en", "poi"));
		Assert.assertFalse(pl.matchName("Place", null, "de", "street"));
		Assert.assertTrue(pl.matchName("Pl", null, "de", "poi"));
		SpatialSearchToken mount = token("Mt");
		Assert.assertTrue(mount.matchName("Mount", null, "en", "locality"));
		Assert.assertFalse(mount.matchName("Mount", null, "en", "street"));
	}

	@Test
	public void prefixMatcherFollowsTheLocaleOfTheMap() {
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		SpatialSearchToken parkway = token("Pkwy");
		Assert.assertTrue(parkway.getPrefixMatcher(stats, "en").matchKey("parkway"));
		Assert.assertFalse(parkway.getPrefixMatcher(stats, "de").matchKey("parkway"));
		// the first map does not change the matches of the second one
		Assert.assertTrue(parkway.getPrefixMatcher(stats, "en").matchKey("parkway"));
		SpatialSearchToken avenue = token("Ave");
		Assert.assertTrue(avenue.matchName("Esplanade", null, "en_US", "street"));
		Assert.assertFalse(avenue.matchName("Esplanade", null, "en", "street"));
		Assert.assertTrue(avenue.getPrefixMatcher(stats, "en_US").matchKey("esplanade"));
		Assert.assertFalse(avenue.getPrefixMatcher(stats, "en").matchKey("esplanade"));
	}

	@Test
	public void buildingSuffixFollowsTheLocaleOfTheMap() {
		Assert.assertFalse(token("Ter").likelyPartOfBuilding("en_US"));
		Assert.assertTrue(token("Ter").likelyPartOfBuilding("fr_FR"));
		SpatialSearchToken ter = token("Ter");
		Assert.assertTrue(ter.matchName("Terrace", null, "en_US", "street"));
		// a POI category is of no map: any map the word was matched with
		Assert.assertFalse(ter.likelyPartOfBuilding());
		ter.matchName("Ter", null, "fr_FR", "street");
		Assert.assertTrue(ter.likelyPartOfBuilding());
	}
}
