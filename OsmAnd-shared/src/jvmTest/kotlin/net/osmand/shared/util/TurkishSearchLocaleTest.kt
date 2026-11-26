package net.osmand.shared.util

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.search.core.spatial.SpatialTypoSuggestions
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurkishSearchLocaleTest {

	@Test
	fun matchesCategoryAndReplacesWordUsingTheDefaultLocale() {
		val previousLocale = Locale.getDefault()
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"))
			val name = KSearchAlgorithms.alignChars("İnsan yapımı")
			val matcher = KCollatorStringMatcher("ı", KStringMatcherMode.CHECK_ONLY_STARTS_WITH)
			assertTrue(matcher.matches(name))
			assertEquals("Istanbul", SpatialTypoSuggestions.replaceWord("Istanbul", "ıstanbul", "istanbul"))
			assertEquals("Izmit", SpatialTypoSuggestions.replaceWord("Izmit", "ızmit", "izmit"))
		} finally {
			Locale.setDefault(previousLocale)
		}
	}
}
