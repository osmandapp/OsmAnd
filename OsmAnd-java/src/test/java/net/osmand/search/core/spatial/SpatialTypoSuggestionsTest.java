package net.osmand.search.core.spatial;

import org.junit.Assert;
import org.junit.Test;

public class SpatialTypoSuggestionsTest {

	@Test
	public void testOneEdit() {
		Assert.assertTrue(SpatialTypoSuggestions.isOneEdit("manhatan", "manhattan")); // missing letter
		Assert.assertTrue(SpatialTypoSuggestions.isOneEdit("trrier", "trier")); // extra letter
		Assert.assertTrue(SpatialTypoSuggestions.isOneEdit("bodenmeis", "bodenmais")); // substitution
		Assert.assertTrue(SpatialTypoSuggestions.isOneEdit("freibrug", "freiburg")); // neighbouring letters swapped
		Assert.assertFalse(SpatialTypoSuggestions.isOneEdit("freiburg", "freiburg"));
		Assert.assertFalse(SpatialTypoSuggestions.isOneEdit("melborn", "melbourne")); // two edits
		Assert.assertFalse(SpatialTypoSuggestions.isOneEdit("abcd", "badc"));
	}

	@Test
	public void testOneEditPrefix() {
		// a word still being typed: the beginning of the name is one edit away
		Assert.assertTrue(SpatialTypoSuggestions.isOneEditPrefix("manhata", "manhattan"));
		Assert.assertTrue(SpatialTypoSuggestions.isOneEditPrefix("freibr", "freiburg"));
		Assert.assertFalse(SpatialTypoSuggestions.isOneEditPrefix("freiburg", "freiburg"));
		Assert.assertFalse(SpatialTypoSuggestions.isOneEditPrefix("zzzz", "freiburg"));
	}

	@Test
	public void testTypoKeyVariants() {
		// typos inside the first 4 letters (the index key): missing, swapped, replaced, extra letter
		Assert.assertTrue(SpatialTypoSuggestions.keyVariants("lepzig", 4).contains("leipzig"));
		Assert.assertTrue(SpatialTypoSuggestions.keyVariants("dersden", 4).contains("dresden"));
		Assert.assertTrue(SpatialTypoSuggestions.keyVariants("mnuchen", 4).contains("munchen"));
		Assert.assertTrue(SpatialTypoSuggestions.keyVariants("trrier", 4).contains("trier"));
		Assert.assertFalse(SpatialTypoSuggestions.keyVariants("freibrug", 4).contains("freiburg")); // edit after the key
	}

	@Test
	public void testReplaceWord() {
		// the capital letter of the corrected word stays, the incomplete dot is dropped
		Assert.assertEquals("Freiburg Munster", SpatialTypoSuggestions.replaceWord("Freibrug Munster", "Freibrug", "freiburg"));
		Assert.assertEquals("bad kreuznach", SpatialTypoSuggestions.replaceWord("bad kreutznach.", "kreutznach.", "kreuznach"));
		Assert.assertNull(SpatialTypoSuggestions.replaceWord("freiburg", "trier", "trier"));
	}

	@Test
	public void testTypoKey() {
		Assert.assertEquals("strasse", SpatialTypoSuggestions.typoKey("Straße"));
		Assert.assertEquals("kossen", SpatialTypoSuggestions.typoKey("Kössen"));
	}
}
