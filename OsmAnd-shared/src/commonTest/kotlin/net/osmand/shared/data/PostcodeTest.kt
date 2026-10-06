package net.osmand.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Postcode formats on every platform the shared code runs on. The rules are regular expressions,
 * which on Kotlin/Native run on a different engine than `java.util.regex`; the answers here are
 * the ones `net.osmand.data.Postcode` in OsmAnd-java gives, and `PostcodeCompatTest` there holds
 * the two side by side over every country.
 */
class PostcodeTest {

	@Test
	fun normalize() {
		val cases = listOf(
			Triple("k1a0b1", "Canada", "K1A 0B1"),
			Triple("K1A 0B1 x", "Canada", "K1A 0B1 X"),
			Triple("12345", "Us", "12345"),
			// the optional second group does not take part, and the rest of the text stays
			Triple("12345-6789", "Us", "123456789"),
			Triple("us-12345 6789", "Us", "123456789"),
			Triple("1101 dl", "Netherlands", "1101DL"),
			Triple("nl-1101-DL", "Netherlands", "1101DL"),
			Triple("sw1a1aa", "GB", "SW1A 1AA"),
			Triple("seoul 110-000", "South-korea", "SEOUL 110000"),
			Triple("02-123", "Poland", "02-123"),
			Triple("de-10115", "Germany", "10115"),
			Triple("01 001", "Ukraine", "01001"),
			Triple("12345", "Narnia", "12345"),
			Triple("abc", "Russia", "ABC")
		)
		for ((postcode, country, expected) in cases) {
			assertEquals(expected, Postcode.normalize(postcode, country), "$country '$postcode'")
		}
	}

	@Test
	fun looksLikePostcodeStart() {
		val cases = listOf(
			Triple("1", "Canada", false),
			Triple("1st", "Canada", true),
			Triple("a1", "Narnia", true),
			Triple("street", "Canada", false),
			Triple("12", "Narnia", true),
			Triple("k1a", "Canada", true),
			Triple("abc", "Russia", false),
			Triple("12345", "Us", true)
		)
		for ((s, country, expected) in cases) {
			assertEquals(expected, Postcode.looksLikePostcodeStart(s, country), "$country '$s'")
		}
	}
}
