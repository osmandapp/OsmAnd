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

	// Frozen Android answers remain the mobile regression oracle. Desktop runs the same cases
	// against the literal Java algorithm with the real, unmodified JVM collator.
	private val desktopSpaces = primaryCollator().equals(" ", "")

	@Test
	fun containsMatchesPlatformSpaceRules() {
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
	fun allModesMatchPlatformUnicodeSpaces() {
		// Recorded from OsmAnd's Java matcher on Android API 37 in en-US/de-DE/ru-RU.
		// Each answer string follows the explicit mode order below; 1 means a match.
		val modes = listOf(
			KStringMatcherMode.CHECK_ONLY_STARTS_WITH,
			KStringMatcherMode.CHECK_STARTS_FROM_SPACE,
			KStringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING,
			KStringMatcherMode.CHECK_EQUALS_FROM_SPACE,
			KStringMatcherMode.CHECK_CONTAINS,
			KStringMatcherMode.CHECK_EQUALS,
			KStringMatcherMode.MULTISEARCH
		)
		val spaces = " \u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000"
		for (space in spaces) {
			val cases = listOf(
				Triple("new york", "New${space}York", "1101111"),
				Triple("newyork", "New${space}York", "0000000"),
				Triple("new${space}york", "New York", "1101111"),
				Triple("new york", "Café New${space}York Deli", "0111100"),
				Triple("Four", "Grifo${space}Uris Bell", "0000000"),
				Triple("york", "New${space}York", "0111100"),
				Triple("four$space", "four square", "1100100"),
				Triple("four$space", "fourteen", "0000000"),
				Triple("four$space$space", "four  square", "1100100"),
				Triple("st john", "St$space${space}John", "0000000"),
				Triple("$space", "Auhofstraße", "0000000"),
				Triple("$space", "$space", "1101111"),
				Triple("", "New${space}York", "1111110"),
				Triple("new york.", "New${space}York Deli", "1101110"),
				Triple("new$space", "new york", "1100100"),
				Triple("new york", "New${space}Yorker", "1100100")
			)
			for ((query, name, answers) in cases) {
				for ((index, mode) in modes.withIndex()) {
					val matcher = KCollatorStringMatcher(query, mode)
					val expected = if (desktopSpaces)
						KCollatorBenchmarkTest.LiteralCollatorMatcher(matcher.part.text, matcher.mode).matches(name)
					else answers[index] == '1'
					val label = "${mode.name}: '$query' in '$name'"
					assertEquals(expected, matcher.matches(name), label)
					// Java's instance path resolves an incomplete dot before its static matcher.
					assertEquals(expected, KCollatorStringMatcher.cmatches(name, matcher.part.text, matcher.mode), label)
					assertEquals(expected, KCollatorStringMatcher.cmatchesPrepared(
						KCollatorStringMatcher.PreparedName(name), matcher.part, matcher.mode), label)
				}
			}
		}
	}

	private fun assertContains(androidExpected: Boolean, query: String, name: String) {
		val mode = KStringMatcherMode.CHECK_CONTAINS
		val matcher = KCollatorStringMatcher(query, mode)
		val expected = if (desktopSpaces)
			KCollatorBenchmarkTest.LiteralCollatorMatcher(query, mode).matches(name) else androidExpected
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
