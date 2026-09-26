package net.osmand.binary;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class SearchLocalesTest {

	@Test
	public void mapLocaleIsLanguageAndCountryOfTheData() {
		assertEquals("en_US", SearchLocales.forMap("Us_new-york_new-york-city_northamerica_2.obf"));
		assertEquals("en_GB", SearchLocales.forMap("Gb_england_london_europe"));
		assertEquals("de_CH", SearchLocales.forMap("Switzerland_zurich_europe"));
		assertEquals("it_CH", SearchLocales.forMap("Switzerland_ticino_europe"));
		assertEquals("fr_CH", SearchLocales.forMap("switzerland_lake-geneva_europe"));
		assertEquals("nl_BE", SearchLocales.forMap("Belgium_flanders_europe"));
		assertEquals("fr_CA", SearchLocales.forMap("Canada_quebec_northamerica"));
		assertEquals("en_CA", SearchLocales.forMap("Canada_ontario_northamerica"));
		// a group of CommonWordsMultiIndex ("esl", "nor") is not a language
		assertEquals("ru_RU", SearchLocales.forMap("Russia_moscow_asia"));
		assertEquals("nb_NO", SearchLocales.forMap("Norway_europe"));
		assertEquals("ka_GE", SearchLocales.forMap("Georgia_asia"));
		assertEquals("en_US", SearchLocales.forMap("Us_georgia_northamerica"));
		assertEquals("en_PG", SearchLocales.forMap("Papua-new-guinea_oceania"));
		assertEquals("fr_GN", SearchLocales.forMap("Guinea_africa"));
		assertEquals("", SearchLocales.forMap("World_basemap"));
		assertEquals("", SearchLocales.forMap("usa"));
		assertEquals("", SearchLocales.forMap(null));
	}

	@Test
	public void nameLocaleFollowsTheTagAndTheCountryOfTheMap() {
		assertEquals("it_IT", SearchLocales.forName(null, "it_IT"));
		assertEquals("it_IT", SearchLocales.forName("alt_name", "it_IT"));
		assertEquals("it_IT", SearchLocales.forName("official_name", "it_IT"));
		assertEquals("it_IT", SearchLocales.forName("name:it", "it_IT"));
		assertEquals("de_IT", SearchLocales.forName("name:de", "it_IT"));
		assertEquals("de_IT", SearchLocales.forName("de", "it_IT"));
		assertEquals("hr_IT", SearchLocales.forName("old_name:hr", "it_IT"));
		assertEquals("zh_US", SearchLocales.forName("name:zh-Hant", "en_US"));
		assertEquals("en", SearchLocales.forName("en", ""));
		assertEquals("", SearchLocales.forName("int_name", "de_DE"));
		assertEquals("de_DE", SearchLocales.forName("name:etymology", "de_DE"));
	}

	@Test
	public void malformedLocaleFallsBackToBaseRules() {
		assertEquals("", SearchLocales.normalize("b+hsb"));
		assertEquals("", SearchLocales.normalize("esl1"));
		assertEquals("", SearchLocales.normalize(null));
		assertEquals("en_US", SearchLocales.normalize("en-us"));
		assertEquals("zh_Hant_TW", SearchLocales.normalize("zh-hant-tw"));
		assertEquals("US", SearchLocales.country("en_US"));
		assertEquals("TW", SearchLocales.country("zh_Hant_TW"));
		assertEquals("", SearchLocales.country("en"));
		assertFalse(Abbreviations.isConjunction("and", "b+hsb"));
		assertTrue(Abbreviations.isConjunction("and", "EN-us"));
		assertNotNull(SearchVariantRules.forLocale("not a locale"));
	}

	@Test
	public void buildingSuffixBelongsToItsLanguages() {
		// "12ter" is a French or Italian house number; "Oak Ter" is Oak Terrace in English
		assertFalse(Abbreviations.likelyPartOfBuilding("ter", null, ""));
		assertTrue(Abbreviations.likelyPartOfBuilding("ter", null, "fr_FR"));
		assertTrue(Abbreviations.likelyPartOfBuilding("ter", null, "it_IT"));
		assertFalse(Abbreviations.likelyPartOfBuilding("ter", null, "en_US"));
		assertFalse(Abbreviations.likelyPartOfBuilding("quater", null, "en_GB"));
		assertTrue(Abbreviations.likelyPartOfBuilding("bis", null, "es_ES"));
		assertFalse(Abbreviations.likelyPartOfBuilding("bis", null, "en_US"));
		assertTrue(Abbreviations.likelyPartOfBuilding("apt", null, "en_US"));
		assertFalse(Abbreviations.likelyPartOfBuilding("apt", null, "de_DE"));
	}

	@Test
	public void dictionariesAreReadOnlyAndOverriddenExplicitly() {
		Map<String, String> search = Abbreviations.getSearchabbreviations("en_US");
		try {
			search.put("st", "Stone");
			fail("dictionary must be read-only");
		} catch (UnsupportedOperationException expected) {
			// ok
		}
		String previous = Abbreviations.overrideSearchAbbreviation("en_NZ", "zz", "Zigzag");
		try {
			assertNull(previous);
			assertEquals("Zigzag", Abbreviations.getSearchabbreviations("en_NZ").get("zz"));
			assertNull(Abbreviations.getSearchabbreviations("en_US").get("zz"));
		} finally {
			Abbreviations.overrideSearchAbbreviation("en_NZ", "zz", null);
		}
		assertNull(Abbreviations.getSearchabbreviations("en_NZ").get("zz"));
	}
}
