package net.osmand.shared.util

import java.text.Normalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The generated ranges have to keep saying what the jdk says, because [KUnicode.mayHaveDiacritics]
 * decides which names skip normalisation entirely: a code point missing from them would leave a
 * diacritic in a name and make it stop matching a search that the java side answers.
 */
class KUnicodeTest {

	@Test
	fun everyCodePointOfTheBasicPlaneIsClassifiedAsTheJdkDoes() {
		var checked = 0
		var missing = 0
		val firstMissing = StringBuilder()
		for (code in 0..0xFFFF) {
			if (Character.isSurrogate(code.toChar())) {
				continue
			}
			val text = code.toChar().toString()
			val stripChangesIt = jdkStrip(text) != text
			checked++
			if (stripChangesIt && !KUnicode.mayHaveDiacritics(text)) {
				missing++
				if (missing <= 10) {
					firstMissing.append("\n  U+%04X".format(code))
				}
			}
		}
		assertTrue(checked > 60000, "code points checked: $checked")
		assertEquals(0, missing, "code points the ranges miss:$firstMissing")
	}

	@Test
	fun strippingMatchesTheJdkOnEveryCodePointOfTheBasicPlane() {
		var differing = 0
		val firstDifferences = StringBuilder()
		for (code in 0..0xFFFF) {
			if (Character.isSurrogate(code.toChar())) {
				continue
			}
			val text = code.toChar().toString()
			val expected = jdkStrip(text)
			val actual = KUnicode.stripDiacritics(text)
			if (expected != actual) {
				differing++
				if (differing <= 10) {
					firstDifferences.append("\n  U+%04X expected '%s' actual '%s'".format(code, expected, actual))
				}
			}
		}
		assertEquals(0, differing, "code points stripped differently:$firstDifferences")
	}

	@Test
	fun strippingMatchesTheJdkOnWholeNames() {
		for (name in NAMES) {
			assertEquals(jdkStrip(name), KUnicode.stripDiacritics(name), name)
			assertEquals(
				Normalizer.normalize(name, Normalizer.Form.NFC),
				KUnicode.normalizeNFC(name),
				name
			)
		}
	}

	@Test
	fun asciiIsReturnedUntouched() {
		val ascii = "Baker Street 221b"
		assertTrue(ascii === KUnicode.normalizeNFC(ascii))
		assertTrue(ascii === KUnicode.stripDiacritics(ascii))
	}

	private fun jdkStrip(input: String): String {
		val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
		val filtered = StringBuilder(decomposed.length)
		var i = 0
		while (i < decomposed.length) {
			val codePoint = decomposed.codePointAt(i)
			i += Character.charCount(codePoint)
			if (Character.getType(codePoint) != Character.NON_SPACING_MARK.toInt()) {
				filtered.appendCodePoint(codePoint)
			}
		}
		return Normalizer.normalize(filtered, Normalizer.Form.NFC)
	}

	companion object {
		private val NAMES = listOf(
			"Straße des 17. Juni",
			"Rue de l'Église",
			"Đường Nguyễn Huệ",
			"Åkersberga",
			"Ελληνικά",
			"Йошкар-Ола",
			"Ёлкино",
			"İstanbul",
			"Kraków",
			"Þingvellir",
			"Ærøskøbing",
			"Nová Ves",
			"شارع الملك فهد",
			"東京駅",
			"서울역",
			"Á decomposed",
			""
		)
	}
}
