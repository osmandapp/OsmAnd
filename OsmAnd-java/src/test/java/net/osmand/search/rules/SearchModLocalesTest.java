package net.osmand.search.rules;

import org.junit.Test;

import static org.junit.Assert.*;

public class SearchModLocalesTest {
	private final SearchModRules searchRules = new SearchModRules();
	private final SearchModLocales locales = searchRules.locales();

	@Test
	public void mapLocaleIsLanguageAndCountryOfTheData() {
		assertEquals("en_US", locales.forMap("Us_new-york_new-york-city_northamerica_2.obf"));
		assertEquals("en_GB", locales.forMap("Gb_england_london_europe"));
		assertEquals("de_CH", locales.forMap("Switzerland_zurich_europe"));
		assertEquals("it_CH", locales.forMap("Switzerland_ticino_europe"));
		assertEquals("fr_CH", locales.forMap("switzerland_lake-geneva_europe"));
		assertEquals("nl_BE", locales.forMap("Belgium_flanders_europe"));
		assertEquals("fr_CA", locales.forMap("Canada_quebec_northamerica"));
		assertEquals("en_CA", locales.forMap("Canada_ontario_northamerica"));
		// a statistics group ("esl", "nor") is not a language
		assertEquals("ru_RU", locales.forMap("Russia_moscow_asia"));
		assertEquals("nb_NO", locales.forMap("Norway_europe"));
		assertEquals("ka_GE", locales.forMap("Georgia_asia"));
		assertEquals("en_US", locales.forMap("Us_georgia_northamerica"));
		assertEquals("en_PG", locales.forMap("Papua-new-guinea_oceania"));
		assertEquals("fr_GN", locales.forMap("Guinea_africa"));
		assertEquals("", locales.forMap("World_basemap"));
		assertEquals("", locales.forMap("usa"));
		assertEquals("", locales.forMap(null));
	}

	@Test
	public void groupAndTranslitComeFromTheSameTable() {
		assertEquals("esl", locales.groupForMap("Russia_moscow_asia_2.obf"));
		assertEquals("de", locales.groupForMap("Switzerland_zurich_europe"));
		assertEquals("it", locales.groupForMap("Switzerland_ticino_europe"));
		assertEquals("mag", locales.groupForMap("Morocco_africa"));
		assertEquals("oth", locales.groupForMap("Antarctica"));
		assertNull(locales.groupForMap("World_basemap"));
		assertEquals("ja", locales.translitForMap("Japan_kanto_asia"));
		assertEquals("zh", locales.translitForMap("China_asia"));
		assertNull(locales.translitForMap("Taiwan_asia"));
	}

	@Test
	public void mapPrefixKeepsSubregionWithItsOwnLocale() {
		assertEquals("Switzerland_ticino", locales.mapPrefix("Switzerland_ticino_europe_2.obf"));
		assertEquals("Belgium_flanders", locales.mapPrefix("Belgium_flanders_europe_2.obf"));
		assertEquals("Switzerland", locales.mapPrefix("Switzerland_zurich_europe_2.obf"));
		assertEquals("Us", locales.mapPrefix("Us_new-york_northamerica_2.obf"));
		assertNull(locales.mapPrefix("World_basemap_2.obf"));
		assertNull(locales.mapPrefix(null));
		// the region name written by BinaryMerger keeps the locale of the source map
		assertEquals("it_CH", locales.forMap(locales.mapPrefix("Switzerland_ticino_europe_2.obf")));
		assertEquals("nl_BE", locales.forMap(locales.mapPrefix("Belgium_flanders_europe_2.obf")));
	}

	@Test
	public void nameLocaleFollowsTheTagAndTheCountryOfTheMap() {
		assertEquals("it_IT", locales.forName(null, "it_IT"));
		assertEquals("it_IT", locales.forName("alt_name", "it_IT"));
		assertEquals("it_IT", locales.forName("official_name", "it_IT"));
		assertEquals("it_IT", locales.forName("name:it", "it_IT"));
		assertEquals("de_IT", locales.forName("name:de", "it_IT"));
		assertEquals("de_IT", locales.forName("de", "it_IT"));
		assertEquals("hr_IT", locales.forName("old_name:hr", "it_IT"));
		assertEquals("zh_US", locales.forName("name:zh-Hant", "en_US"));
		assertEquals("en", locales.forName("en", ""));
		assertEquals("", locales.forName("int_name", "de_DE"));
		assertEquals("de_DE", locales.forName("name:etymology", "de_DE"));
	}

	@Test
	public void localeNormalization() {
		assertEquals("", locales.normalize(null));
		assertEquals("en_US", locales.normalize("en-us"));
		assertEquals("zh_Hant_TW", locales.normalize("zh-hant-tw"));
		assertEquals("US", locales.country("en_US"));
		assertEquals("TW", locales.country("zh_Hant_TW"));
		assertEquals("", locales.country("en"));
		assertTrue(searchRules.dictionary("EN-us").isIgnorable("and"));
		assertNotNull(searchRules.rules("not a locale"));
	}

	@Test
	public void buildingSuffixBelongsToItsLanguages() {
		// "12ter" is a French or Italian house number; "Oak Ter" is Oak Terrace in English
		assertFalse(searchRules.dictionary("").likelyPartOfBuilding("ter", null));
		assertTrue(searchRules.dictionary("fr_FR").likelyPartOfBuilding("ter", null));
		assertTrue(searchRules.dictionary("it_IT").likelyPartOfBuilding("ter", null));
		assertFalse(searchRules.dictionary("en_US").likelyPartOfBuilding("ter", null));
		assertFalse(searchRules.dictionary("en_GB").likelyPartOfBuilding("quater", null));
		assertTrue(searchRules.dictionary("es_ES").likelyPartOfBuilding("bis", null));
		assertFalse(searchRules.dictionary("en_US").likelyPartOfBuilding("bis", null));
		assertTrue(searchRules.dictionary("en_US").likelyPartOfBuilding("apt", null));
		assertFalse(searchRules.dictionary("de_DE").likelyPartOfBuilding("apt", null));
	}
}
