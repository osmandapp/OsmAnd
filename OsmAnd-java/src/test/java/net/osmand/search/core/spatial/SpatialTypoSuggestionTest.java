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
	public void testTypoKeyVariants() {
		// typos inside the first 4 letters (the index key): missing, swapped, replaced, extra letter
		Assert.assertTrue(SpatialSearchContext.typoKeyVariants("lepzig", 4).contains("leipzig"));
		Assert.assertTrue(SpatialSearchContext.typoKeyVariants("dersden", 4).contains("dresden"));
		Assert.assertTrue(SpatialSearchContext.typoKeyVariants("mnuchen", 4).contains("munchen"));
		Assert.assertTrue(SpatialSearchContext.typoKeyVariants("trrier", 4).contains("trier"));
		Assert.assertFalse(SpatialSearchContext.typoKeyVariants("freibrug", 4).contains("freiburg")); // edit after the key
	}

	@Test
	public void testTypoKey() {
		Assert.assertEquals("strasse", SpatialSearchContext.typoKey("Straße"));
		Assert.assertEquals("kossen", SpatialSearchContext.typoKey("Kössen"));
	}
}
