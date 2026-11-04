package net.osmand.shared.search.core.spatial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `SpatialTypoSuggestionsTest` of OsmAnd-java. */
class SpatialTypoSuggestionsTest {

	@Test
	fun testOneEdit() {
		assertTrue(SpatialTypoSuggestions.isOneEdit("manhatan", "manhattan")) // missing letter
		assertTrue(SpatialTypoSuggestions.isOneEdit("trrier", "trier")) // extra letter
		assertTrue(SpatialTypoSuggestions.isOneEdit("bodenmeis", "bodenmais")) // substitution
		assertTrue(SpatialTypoSuggestions.isOneEdit("freibrug", "freiburg")) // neighbouring letters swapped
		assertFalse(SpatialTypoSuggestions.isOneEdit("freiburg", "freiburg"))
		assertFalse(SpatialTypoSuggestions.isOneEdit("melborn", "melbourne")) // two edits
		assertFalse(SpatialTypoSuggestions.isOneEdit("abcd", "badc"))
	}

	@Test
	fun testOneEditPrefix() {
		// a word still being typed: the beginning of the name is one edit away
		assertTrue(SpatialTypoSuggestions.isOneEditPrefix("manhata", "manhattan"))
		assertTrue(SpatialTypoSuggestions.isOneEditPrefix("freibr", "freiburg"))
		assertFalse(SpatialTypoSuggestions.isOneEditPrefix("freiburg", "freiburg"))
		assertFalse(SpatialTypoSuggestions.isOneEditPrefix("zzzz", "freiburg"))
	}

	@Test
	fun testTypoKeyVariants() {
		// typos inside the first 4 letters (the index key): missing, swapped, replaced, extra letter
		assertTrue(SpatialTypoSuggestions.keyVariants("lepzig", 4).contains("leipzig"))
		assertTrue(SpatialTypoSuggestions.keyVariants("dersden", 4).contains("dresden"))
		assertTrue(SpatialTypoSuggestions.keyVariants("mnuchen", 4).contains("munchen"))
		assertTrue(SpatialTypoSuggestions.keyVariants("trrier", 4).contains("trier"))
		assertFalse(SpatialTypoSuggestions.keyVariants("freibrug", 4).contains("freiburg")) // edit after the key
	}

	@Test
	fun testReplaceWord() {
		// the capital letter of the corrected word stays, the incomplete dot is dropped
		assertEquals("Freiburg Munster", SpatialTypoSuggestions.replaceWord("Freibrug Munster", "Freibrug", "freiburg"))
		assertEquals("bad kreuznach", SpatialTypoSuggestions.replaceWord("bad kreutznach.", "kreutznach.", "kreuznach"))
		assertNull(SpatialTypoSuggestions.replaceWord("freiburg", "trier", "trier"))
	}

	@Test
	fun testTypoKey() {
		assertEquals("strasse", SpatialTypoSuggestions.typoKey("Straße"))
		assertEquals("kossen", SpatialTypoSuggestions.typoKey("Kössen"))
	}
}
