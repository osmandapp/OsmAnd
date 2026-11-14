package net.osmand.shared.util

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.search.core.NameStringMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [KCollatorStringMatcher.cmatchesPrepared], which the walk over the string table of a name index
 * uses, answers what [KCollatorStringMatcher.cmatches] answers for the same two strings, in every
 * mode: with hyphens on either side, the letters the collator expands, accents, case, dots and
 * empty strings. [NameStringMatcher.matchesPrepared], which the api of poi types uses, answers what
 * [NameStringMatcher.matches] answers, also for a part that ends in the incomplete dot.
 */
class KCollatorStringMatcherTest {

	@Test
	fun containsPreservesSpaces() {
		val cases = listOf(
			Triple("Four", "Grifo Uris Bell", false),
			Triple("Four", "Shower at \"Grifo Uris Bell\"", false),
			Triple("Grifo Uris Bell", "Shower at \"Grifo Uris Bell\"", true),
			Triple("newyork", "New York", false),
			Triple("fo ur", "four", false),
			Triple("four ", "four square", true),
			Triple("four ", "fourteen", false),
			Triple("four  ", "four  square", true),
			Triple("st john", "St  John", false),
			Triple(" ", "Auhofstraße", false),
			Triple(" ", " ", true),
			Triple("", "New York", true)
		)
		for ((query, name, expected) in cases) {
			assertContains(expected, query, name)
		}
	}

	@Test
	fun containsFoldsAndroidUnicodeSpaces() {
		// Observed with Android API 37 java.text.Collator PRIMARY in en-US/de-DE/ru-RU.
		val spaces = " \u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000"
		for (space in spaces) {
			assertContains(true, "new york", "New${space}York")
			assertContains(true, "new york", "Café New${space}York Deli")
			assertContains(true, "new${space}york", "New York")
			assertContains(false, "Four", "Grifo${space}Uris Bell")
			assertContains(false, "newyork", "New${space}York")
			assertContains(true, "four$space", "four square")
			assertContains(false, "four$space", "fourteen")
		}
		for (control in "\t\n\u200B\uFEFF") {
			assertContains(false, "new york", "New${control}York")
		}
	}

	private fun assertContains(expected: Boolean, query: String, name: String) {
		val mode = KStringMatcherMode.CHECK_CONTAINS
		val matcher = KCollatorStringMatcher(query, mode)
		val label = "'$query' in '$name'"
		assertEquals(expected, matcher.matches(name), label)
		assertEquals(expected, KCollatorStringMatcher.cmatches(name, query, mode), label)
		assertEquals(expected, KCollatorStringMatcher.cmatchesPrepared(
			KCollatorStringMatcher.PreparedName(name), matcher.part, mode), label)
	}

	private val names = listOf(
		"", "a", "b", "ab", "a-b", "a b", "-", "--", "-1", "1-", "12-3", "st.", "st", "St. Pölten",
		"Sankt Pölten", "Auhofstraße", "auhofstrasse", "Straße", "strasse", "Ærø", "aero", "œuvre", "oeuvre",
		"Þingvellir", "thingvellir", "Йошкар-Ола", "йошкар ола", "Йошкар", "ола", "Ёлкино", "елкино",
		"Красно село", "кра", "Кра", "красно", "Łążek", "lazek", "Łaz", "Bahnhofplatz 3", "bahnhof",
		"Rue de la Paix", "rue", "paix", "Noord-Holland", "noordholland", "holland", "0-9", "1st Street",
		"ελληνικά", "ελλην", "東京", "東", "fuel_station", "fuel station", "fuel", "bank;atm;cash", "atm"
	)

	@Test
	fun preparedMatchesAsCmatches() {
		var compared = 0
		for (full in names) {
			val prepared = KCollatorStringMatcher.PreparedName(full)
			for (part in names) {
				val partKey = KCollationKey.of(part)
				for (mode in KStringMatcherMode.entries) {
					assertEquals(
						KCollatorStringMatcher.cmatches(full, part, mode),
						KCollatorStringMatcher.cmatchesPrepared(prepared, partKey, mode),
						"'$full' '$part' $mode"
					)
					compared++
				}
			}
		}
		assertEquals(names.size * names.size * KStringMatcherMode.entries.size, compared)
	}

	@Test
	fun preparedKeyIsThePartKey() {
		for (name in names) {
			assertEquals(KCollationKey.of(name).toString(), KCollatorStringMatcher.PreparedName(name).key.toString(), name)
		}
	}

	@Test
	fun nameMatcherPreparedMatchesAsMatches() {
		val parts = names + listOf(".", "..", "a.", "St.", "кра.", "Rue de.")
		val prepared = names.filter { it.isNotEmpty() }.map { it to KCollatorStringMatcher.PreparedName(it) }
		var compared = 0
		for (part in parts) {
			for (mode in KStringMatcherMode.entries) {
				val nm = NameStringMatcher(part, mode)
				for ((full, preparedFull) in prepared) {
					assertEquals(nm.matches(full), nm.matchesPrepared(preparedFull), "'$full' '$part' $mode")
					compared++
				}
			}
		}
		assertEquals(parts.size * KStringMatcherMode.entries.size * (names.size - 1), compared)
	}
}
