package net.osmand.binary;

import org.junit.Test;

import static org.junit.Assert.*;

public class SearchVariantRulesTest {
	@Test
	public void scopedLocaleRulesAndRoadNumber() {
		// the base holds no word rules: words belong to a language
		assertEquals(0, SearchVariantRules.forLocale("").indexEntries().size());
		assertEquals(0, SearchVariantRules.forLocale("").queryEntries().size());
		assertEquals(15, SearchVariantRules.forLocale("en").indexEntries().size());
		assertEquals("e", Abbreviations.replace("e", ""));
		assertEquals("East", Abbreviations.replace("e", "en"));
		assertEquals("Street", Abbreviations.getAbbreviations("en").get("st"));
		assertEquals("Street Saint", Abbreviations.getSearchabbreviations("en").get("st"));
		assertEquals("Sankt", Abbreviations.getSearchabbreviations("de_DE").get("st"));
		assertEquals("Saint", Abbreviations.getSearchabbreviations("fr_FR").get("st"));
		assertNull(Abbreviations.getSearchabbreviations("de_DE").get("rd"));
		assertEquals("Avenue", Abbreviations.getSearchabbreviations("en").get("ave"));
		assertEquals("Esplanade", Abbreviations.getSearchabbreviations("en_US").get("ave"));
		assertEquals("Eastern", Abbreviations.replace("e", "en_US"));
		assertTrue(Abbreviations.likelyPartOfBuilding("tower", null, "en_US"));
		assertFalse(Abbreviations.likelyPartOfBuilding("tower", null, "en"));
		assertTrue(Abbreviations.isConjunction("thee", "en_US"));
		assertFalse(Abbreviations.isConjunction("thee", "en"));
		assertTrue(Abbreviations.likelyPartOfBuilding("bis", null, "fr_FR"));
		assertTrue(Abbreviations.isConjunction("и", "ru_RU"));
		assertFalse(Abbreviations.isConjunction("и", "uk_UA"));
		assertTrue(Abbreviations.isConjunction("die", "de_AT"));
		assertFalse(Abbreviations.isConjunction("die", "en_US"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("street", "en_GB"));
		assertFalse(Abbreviations.isCommonSkipOtherCnt("street", "de_DE"));
		// titles of German names are no unmatched words: "Dr.-Weber-Straße", "Friedenskapelle St. Josef"
		assertTrue(Abbreviations.isCommonSkipOtherCnt("dr", "de_DE"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("sankt", "de_LI"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("st", "de_AT"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("sainte", "fr_FR"));
		assertEquals("en_US", SearchLocales.forMap("Us_new-york_north-america.obf"));
		assertEquals("de_DE", SearchLocales.forMap("Germany_bayern_europe.obf"));
		assertEquals("it_IT", SearchLocales.forMap("Italy_lombardia_europe.obf"));
		SearchVariantRules german = SearchVariantRules.forLocale("de");
		SearchVariantRules.Variant strasse = german.index().stream()
				.filter(v -> "Hallerstr 36".equals(v.apply("Hallerstraße 36"))).findFirst().orElseThrow();
		assertTrue(strasse.appliesTo("street"));
		assertFalse(strasse.appliesTo("poi"));
		assertEquals("Hallerstr 36", strasse.apply("Hallerstraße 36"));
		assertNull(strasse.apply("Straße 36"));
		assertFalse(SearchVariantRules.forLocale("en_US").index().stream()
				.anyMatch(v -> "Hallerstr 36".equals(v.apply("Hallerstraße 36"))));
		assertEquals("Trinity Pl.", SearchVariantRules.forLocale("en_US").index().stream()
				.filter(v -> "Trinity Pl.".equals(v.apply("Trinity Place"))).findFirst().orElseThrow()
				.apply("Trinity Place"));
		assertFalse(SearchVariantRules.forLocale("en_US").index().stream()
				.anyMatch(v -> "Pkwy".equals(v.apply("Parkway"))));
		assertEquals("Esplanade", SearchVariantRules.forLocale("en_US").queryEntries().stream()
				.filter(e -> e.kind.equals("search") && e.key.equals("ave")).findFirst().orElseThrow().value);
		// a pair is matched on one side only: Place/Pl by the query, so the English index adds no "Pl"
		assertFalse(SearchVariantRules.forLocale("en").index().stream()
				.anyMatch(v -> v.apply("Trinity Place") != null));
		assertEquals("Stop and Shop", SearchVariantRules.forLocale("en").index().stream()
				.filter(v -> "Stop and Shop".equals(v.apply("Stop & Shop"))).findFirst().orElseThrow()
				.apply("Stop & Shop"));

		SearchVariantRules italian = SearchVariantRules.forLocale("it");
		assertEquals("SS 42 del Tonale", italian.index().stream()
				.filter(v -> "SS 42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))).findFirst().orElseThrow()
				.apply("Strada Statale 42 del Tonale"));
		assertEquals("SS42 del Tonale", italian.index().stream()
				.filter(v -> "SS42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))).findFirst().orElseThrow()
				.apply("Strada Statale 42 del Tonale"));

		SearchVariantRules.Variant place = SearchVariantRules.forLocale("en").query().stream()
				.filter(v -> "Place".equals(v.apply("Pl"))).findFirst().orElseThrow();
		assertEquals("Place", place.apply("Pl"));
		assertTrue(place.appliesTo("street"));
		assertFalse(place.appliesTo("poi"));
	}
}
