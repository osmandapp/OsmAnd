package net.osmand.shared.util

import net.osmand.shared.api.KStringMatcherMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [KCollatorStringMatcher.cmatchesPrepared], which the walk over the string table of a name index
 * uses, answers what [KCollatorStringMatcher.cmatches] answers for the same two strings, in every
 * mode: with hyphens on either side, the letters the collator expands, accents, case, dots and
 * empty strings.
 */
class KCollatorStringMatcherTest {

	private val names = listOf(
		"", "a", "b", "ab", "a-b", "a b", "-", "--", "-1", "1-", "12-3", "st.", "st", "St. Pölten",
		"Sankt Pölten", "Auhofstraße", "auhofstrasse", "Straße", "strasse", "Ærø", "aero", "œuvre", "oeuvre",
		"Þingvellir", "thingvellir", "Йошкар-Ола", "йошкар ола", "Йошкар", "ола", "Ёлкино", "елкино",
		"Красно село", "кра", "Кра", "красно", "Łążek", "lazek", "Łaz", "Bahnhofplatz 3", "bahnhof",
		"Rue de la Paix", "rue", "paix", "Noord-Holland", "noordholland", "holland", "0-9", "1st Street",
		"ελληνικά", "ελλην", "東京", "東"
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
}
