package net.osmand.search.core.spatial;

import net.osmand.binary.SearchRules;
import net.osmand.binary.SearchRulesDictionary;
import org.junit.Assert;
import org.junit.Test;

public class SpatialSearchQueryRulesTest {
	private final SearchRules searchRules = new SearchRules();

	private SearchRulesDictionary rules(String locale) {
		return searchRules.dictionary(locale);
	}

	private SpatialSearchToken token(String word) {
		return new SpatialSearchToken(2, word.toLowerCase(), word, 0);
	}

	@Test
	public void placeAbbreviationRequiresEnglishStreet() {
		SpatialSearchToken pl = token("Pl");
		Assert.assertTrue(pl.matchName("Place", null, rules("en"), "street"));
		Assert.assertFalse(pl.matchName("Place", null, rules("en"), "poi"));
		Assert.assertFalse(pl.matchName("Place", null, rules("de"), "street"));
		Assert.assertTrue(pl.matchName("Pl", null, rules("de"), "poi"));
		SpatialSearchToken mount = token("Mt");
		Assert.assertTrue(mount.matchName("Mount", null, rules("en"), "locality"));
		Assert.assertFalse(mount.matchName("Mount", null, rules("en"), "street"));
	}

	@Test
	public void prefixMatcherFollowsTheLocaleOfTheMap() {
		SpatialSearchContext.SpatialSearchStats stats = new SpatialSearchContext.SpatialSearchStats();
		SpatialSearchToken parkway = token("Pkwy");
		Assert.assertTrue(parkway.getPrefixMatcher(stats, rules("en")).matchKey("parkway"));
		Assert.assertFalse(parkway.getPrefixMatcher(stats, rules("de")).matchKey("parkway"));
		// the first map does not change the matches of the second one
		Assert.assertTrue(parkway.getPrefixMatcher(stats, rules("en")).matchKey("parkway"));
		SpatialSearchToken avenue = token("Ave");
		Assert.assertTrue(avenue.matchName("Esplanade", null, rules("en_US"), "street"));
		Assert.assertFalse(avenue.matchName("Esplanade", null, rules("en"), "street"));
		Assert.assertTrue(avenue.getPrefixMatcher(stats, rules("en_US")).matchKey("esplanade"));
		Assert.assertFalse(avenue.getPrefixMatcher(stats, rules("en")).matchKey("esplanade"));
	}

	@Test
	public void buildingSuffixFollowsTheLocaleOfTheMap() {
		SpatialSearchToken ter = token("Ter");
		Assert.assertFalse(ter.likelyPartOfBuilding(rules("en_US")));
		Assert.assertTrue(ter.likelyPartOfBuilding(rules("fr_FR")));
		Assert.assertTrue(ter.matchName("Terrace", null, rules("en_US"), "street"));
		// a POI category is of no map: any map the word was matched with
		Assert.assertFalse(ter.likelyPartOfBuilding());
		ter.matchName("Ter", null, rules("fr_FR"), "street");
		Assert.assertTrue(ter.likelyPartOfBuilding());
	}
}
