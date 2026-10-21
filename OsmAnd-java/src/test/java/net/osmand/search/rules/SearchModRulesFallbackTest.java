package net.osmand.search.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SearchModRulesFallbackTest {

	@Test
	public void testMissingRulesDoNotStopSearch() {
		SearchModRules rules = new SearchModRules("/net/osmand/search/no-such-dir/");
		assertEquals("", rules.locales().forMap("Germany_bayern_europe.obf"));
		assertNotNull(rules.rules("de_DE"));
		assertFalse(rules.dictionary("en_US").isIgnorable("and"));
	}
}
