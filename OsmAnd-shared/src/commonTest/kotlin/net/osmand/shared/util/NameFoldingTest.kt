package net.osmand.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [KSearchAlgorithms.alignChars] and [KCollationKey.lowercaseAndAlignChars] skip the steps that
 * would return the text as it is, and [KCollationKey.lowercase] lowercases char by char. They answer
 * what the steps and [String.lowercase] answer: for every char of the basic plane, alone and between
 * two letters, and for the text they leave to the steps.
 */
class NameFoldingTest {

	private val tricky = listOf(
		"", "İstanbul", "ΟΔΟΣ", "ΟΔΟΣ ΑΘΗΝΩΝ", "Σοφία", "ΣΑΣ", "\uD801\uDC00", "a\uD801\uDC00B", "\uD801", "\uDC00a",
		"Rue de l'Église", "OʼConnell", "Straße", "ẞ", "«Кафе»", "شَارِع", "ﺍﻟﻜﻮﻳﺖ",
		"Ærøskøbing", "Kraków", "Đường", "Ǆ"
	)

	@Test
	fun foldsAsTheSteps() {
		var compared = 0
		for (code in 0..0xFFFF) {
			val c = code.toChar()
			for (text in listOf(c.toString(), "A${c}b")) {
				check(text)
				compared++
			}
		}
		for (text in tricky) {
			check(text)
			compared++
		}
		println("NameFoldingTest: $compared texts compared")
	}

	private fun check(text: String) {
		assertEquals(text.lowercase(), KCollationKey.lowercase(text), "lowercase of $text")
		assertEquals(steps(text), KSearchAlgorithms.alignChars(text), "alignChars of $text")
		assertEquals(
			steps(text.lowercase()), KCollationKey.lowercaseAndAlignChars(text), "lowercaseAndAlignChars of $text"
		)
	}

	/** The steps of `SearchAlgorithms.alignChars` in OsmAnd-java. */
	private fun steps(fullText: String): String {
		var result = fullText
		if (KArabicNormalizer.isSpecialArabic(result)) {
			result = KArabicNormalizer.normalize(result) ?: result
		}
		result = KSearchAlgorithms.removeApostrophes(result)
		result = KSearchAlgorithms.replaceGermanSS(result)
		result = KSearchAlgorithms.removeQuotes(result)
		return KUnicode.stripDiacritics(result)
	}
}
