package net.osmand.search.core.spatial;

import org.junit.Assert;
import org.junit.Test;

public class SpatialTypoSuggestionTest {

	@Test
	public void testOneEdit() {
		Assert.assertTrue(SpatialSearchContext.isOneEdit("manhatan", "manhattan")); // missing letter
		Assert.assertTrue(SpatialSearchContext.isOneEdit("trrier", "trier")); // extra letter
		Assert.assertTrue(SpatialSearchContext.isOneEdit("bodenmeis", "bodenmais")); // substitution
		Assert.assertTrue(SpatialSearchContext.isOneEdit("freibrug", "freiburg")); // neighbouring letters swapped
		Assert.assertFalse(SpatialSearchContext.isOneEdit("freiburg", "freiburg"));
		Assert.assertFalse(SpatialSearchContext.isOneEdit("melborn", "melbourne")); // two edits
		Assert.assertFalse(SpatialSearchContext.isOneEdit("abcd", "badc"));
	}

	@Test
	public void testOneEditPrefix() {
		// a word still being typed: the beginning of the name is one edit away
		Assert.assertTrue(SpatialSearchContext.isOneEditPrefix("manhata", "manhattan"));
		Assert.assertTrue(SpatialSearchContext.isOneEditPrefix("freibr", "freiburg"));
		Assert.assertFalse(SpatialSearchContext.isOneEditPrefix("freiburg", "freiburg"));
		Assert.assertFalse(SpatialSearchContext.isOneEditPrefix("zzzz", "freiburg"));
	}

	@Test
	public void testTypoKey() {
		Assert.assertEquals("strasse", SpatialSearchContext.typoKey("Straße"));
		Assert.assertEquals("kossen", SpatialSearchContext.typoKey("Kössen"));
	}
}
