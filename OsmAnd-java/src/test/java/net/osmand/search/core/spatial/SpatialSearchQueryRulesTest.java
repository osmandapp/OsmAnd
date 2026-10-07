package net.osmand.search.core.spatial;

import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;

import net.osmand.search.rules.SearchModRules;
import org.junit.Assert;
import org.junit.Test;

public class SpatialSearchQueryRulesTest {
	private final SearchModRules searchRules = new SearchModRules();

	private SpatialSearchToken token(String word) {
		return new SpatialSearchToken(searchRules, 2, word.toLowerCase(), word, 0);
	}

	@Test
	public void placeAbbreviationRequiresEnglishStreet() {
		SpatialSearchToken pl = token("Pl");
		Assert.assertTrue(pl.matchName("Place", null, "en", SpatialSearchToken.STREET_TYPE));
		Assert.assertFalse(pl.matchName("Place", null, "en", SpatialSearchToken.POI_TYPE));
		Assert.assertFalse(pl.matchName("Place", null, "de", SpatialSearchToken.STREET_TYPE));
		Assert.assertTrue(pl.matchName("Pl", null, "de", SpatialSearchToken.POI_TYPE));
		SpatialSearchToken mount = token("Mt");
		Assert.assertTrue(mount.matchName("Mount", null, "en", CityBlocks.CITY_TOWN_TYPE.index));
		Assert.assertFalse(mount.matchName("Mount", null, "en", SpatialSearchToken.STREET_TYPE));
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
		Assert.assertTrue(avenue.matchName("Esplanade", null, "en_US", SpatialSearchToken.STREET_TYPE));
		Assert.assertFalse(avenue.matchName("Esplanade", null, "en", SpatialSearchToken.STREET_TYPE));
		Assert.assertTrue(avenue.getPrefixMatcher(stats, "en_US").matchKey("esplanade"));
		Assert.assertFalse(avenue.getPrefixMatcher(stats, "en").matchKey("esplanade"));
	}

	@Test
	public void buildingSuffixFollowsTheLocaleOfTheMap() {
		Assert.assertFalse(token("Ter").likelyPartOfBuilding("en_US"));
		Assert.assertTrue(token("Ter").likelyPartOfBuilding("fr_FR"));
		SpatialSearchToken ter = token("Ter");
		Assert.assertTrue(ter.matchName("Terrace", null, "en_US", SpatialSearchToken.STREET_TYPE));
		// a POI category is of no map: any map the word was matched with
		Assert.assertFalse(ter.likelyPartOfBuilding());
		ter.matchName("Ter", null, "fr_FR", SpatialSearchToken.STREET_TYPE);
		Assert.assertTrue(ter.likelyPartOfBuilding());
	}
}
