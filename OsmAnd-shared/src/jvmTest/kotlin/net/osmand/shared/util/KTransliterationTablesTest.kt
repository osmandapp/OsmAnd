package net.osmand.shared.util

import net.sf.junidecode.Junidecode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The generated tables have to keep saying exactly what junidecode says, since that is the
 * romanisation android and the server have always produced and the whole point of moving it into
 * common code is that every platform now produces it too.
 *
 * This is why junidecode is still a test dependency of the module after it stopped being a
 * dependency of the code.
 */
class KTransliterationTablesTest {

	@Test
	fun everyCodeUnitRomanisesAsJunidecodeDoes() {
		var checked = 0
		var differing = 0
		val firstDifferences = StringBuilder()
		for (code in 0..0xFFFF) {
			val text = code.toChar().toString()
			val expected = Junidecode.unidecode(text)
			val actual = toLatin(text)
			checked++
			if (expected != actual) {
				differing++
				if (differing <= 10) {
					firstDifferences.append(
						"\n  U+%04X expected '%s' actual '%s'".format(code, expected, actual)
					)
				}
			}
		}
		assertEquals(0x10000, checked)
		assertEquals(0, differing, "code units romanised differently:$firstDifferences")
	}

	@Test
	fun wholeStringsRomaniseAsJunidecodeDoes() {
		val samples = listOf(
			"Кёльн", "Хрещатик", "北京市", "東京", "서울", "Ἀθῆναι", "القاهرة", "ဗန်းမော်",
			"Zürich", "Łódź", "Ærø", "İstanbul", "Ђаковица", "ĳsselmeer", "ß", "Ａ", "ﬁ",
			"улица Ленина, 12", "Straße des 17. Juni", "Piazza dell'Unità d'Italia",
			"", " ", "plain ascii 123", "mixed Кёльн and ascii",
			// a surrogate pair and a lone surrogate, which junidecode walks code unit by code unit
			String(Character.toChars(0x20000)), "\uD840", "emoji 😀 here",
		)
		for (sample in samples) {
			assertEquals(Junidecode.unidecode(sample), toLatin(sample), "romanising '$sample'")
		}
	}

	/** The fast path in [toLatin] is only sound while the table really is the identity there. */
	@Test
	fun theFastPathRangeIsTheIdentity() {
		for (code in IDENTITY_FIRST..IDENTITY_LAST) {
			assertEquals(code.toChar().toString(), romanisationOf(code), "U+%04X".format(code))
		}
		val ascii = "Main Street 42"
		assertTrue(ascii === toLatin(ascii), "a name of plain ascii should not be copied at all")
	}

	@Test
	fun japaneseIsLeftAlone() {
		val japanese = "東京"
		KTransliterationHelper.setJapanese(true)
		try {
			assertEquals(japanese, KTransliterationHelper.transliterate(japanese))
		} finally {
			KTransliterationHelper.setJapanese(false)
		}
		assertEquals(Junidecode.unidecode(japanese), KTransliterationHelper.transliterate(japanese))
	}
}
