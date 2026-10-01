package net.osmand.binary;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SearchVariantRulesTest {
	private static List<String> forms(String word, String locale) {
		return Abbreviations.getQueryForms(word, locale).stream().map(Abbreviations.QueryForm::word)
				.collect(Collectors.toList());
	}

	@Test
	public void scopedLocaleRulesAndRoadNumber() {
		// the base holds no word rules: words belong to a language
		SearchVariantRules base = SearchVariantRules.forLocale("");
		assertEquals(0, base.index().size() + base.normalizations().size() + base.query().size()
				+ base.buildings().size() + base.ignorables().size());
		assertEquals(15, SearchVariantRules.forLocale("en").normalizations().size());
		assertEquals("e", Abbreviations.replace("e", ""));
		assertEquals("East", Abbreviations.replace("e", "en"));
		assertEquals("Street", Abbreviations.getAbbreviations("en").get("st"));
		// one word, several alternatives: the rule outside <index>/<query> and the extension of <query>
		assertEquals(List.of("street", "saint"), forms("st", "en"));
		assertEquals(List.of("sankt"), forms("st", "de_DE"));
		assertEquals(List.of("saint"), forms("st", "fr_FR"));
		assertEquals(List.of(), forms("rd", "de_DE"));
		assertEquals(List.of("avenue"), forms("ave", "en"));
		assertEquals(List.of("esplanade"), forms("ave", "en_US"));
		assertEquals(List.of("san", "santo", "santa"), forms("s", "it_IT"));
		assertEquals(List.of("south"), forms("s", "en"));
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
		SearchVariantRules.Rule strasse = german.index().stream()
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
		assertEquals(List.of("esplanade"), forms("ave", "en_US"));
		// a pair is matched on one side only: Place/Pl by the query, so the English index adds no "Pl"
		assertFalse(SearchVariantRules.forLocale("en").index().stream()
				.anyMatch(v -> v.apply("Trinity Place") != null));
		// "& -> and" of POI names was removed: ~1.3 MB of POI index for too few queries
		assertFalse(SearchVariantRules.forLocale("en").index().stream()
				.anyMatch(v -> v.apply("Stop & Shop") != null));

		SearchVariantRules italian = SearchVariantRules.forLocale("it");
		assertEquals("SS 42 del Tonale", italian.index().stream()
				.filter(v -> "SS 42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))).findFirst().orElseThrow()
				.apply("Strada Statale 42 del Tonale"));
		assertEquals("SS42 del Tonale", italian.index().stream()
				.filter(v -> "SS42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))).findFirst().orElseThrow()
				.apply("Strada Statale 42 del Tonale"));

		// only a locale with a replace="true" rule rewrites a stored name; English does, a word of another language does not
		assertEquals("East Main Street", Abbreviations.replaceAll("E Main St", "en", "street"));
		assertEquals("Eastern Main Street", Abbreviations.replaceAll("E Main St", "en_US", "street"));
		assertEquals("E Main St", Abbreviations.replaceAll("E Main St", "de_DE", "street"));
		assertEquals("E Main St", Abbreviations.replaceAll("E Main St", "", "street"));
		assertTrue(SearchVariantRules.forLocale("es_ES").normalizations().isEmpty());
		assertTrue(Abbreviations.getAbbreviations("fr_FR").isEmpty());
		// the replaced words of a title stay common words for a language that does not rewrite names
		assertTrue(Abbreviations.isCommonSkipOtherCnt("doctor", "es_ES"));

		SearchVariantRules.Rule place = SearchVariantRules.forLocale("en").query().stream()
				.filter(v -> "Place".equals(v.apply("Pl"))).findFirst().orElseThrow();
		assertEquals("Place", place.apply("Pl"));
		assertTrue(place.appliesTo("street"));
		assertFalse(place.appliesTo("poi"));
	}

	@Test
	public void ruleOutsideIndexAndQueryWorksOnBothSides() {
		SearchVariantRules en = SearchVariantRules.forLocale("en");
		assertTrue(en.index().isEmpty());
		// "st": the rule outside <index>/<query> (replace) and the rule of <query> are alternatives of one word
		assertEquals(List.of("street", "saint"), forms("st", "en"));
		assertTrue(en.query().stream().anyMatch(r -> r.scope == SearchVariantRules.Scope.BOTH && r.replace));
		// a lower layer replaces an upper rule by its key (object, from, to), a disabled rule is gone
		assertTrue(Abbreviations.getAbbreviations("en").containsKey("hwy"));
		assertFalse(Abbreviations.getAbbreviations("en_US").containsKey("hwy"));
		assertEquals("Eastern", Abbreviations.getAbbreviations("en_US").get("e"));
		assertEquals("East", Abbreviations.getAbbreviations("en").get("e"));
		assertEquals(List.of("esplanade"), forms("ave", "en_US"));
		// without replace a rule outside <index> and <query> adds the form to the index and to the query
		SearchVariantRules both = of("<rule from=\"dr\" to=\"Doctor\" common=\"true\"/>");
		assertEquals(1, both.index().size());
		assertEquals(1, both.query().size());
		assertTrue(both.normalizations().isEmpty());
		assertEquals("Doctor", both.index().get(0).apply("Dr"));
	}

	@Test
	public void keyIsObjectFromAndTo() {
		SearchVariantRules rules = of("<rule from=\"st\" to=\"Street\" replace=\"true\"/>"
				+ "<query><rule from=\"st\" to=\"Saint\"/><rule object=\"street\" from=\"st\" to=\"Street\"/></query>");
		assertEquals(3, rules.query().size());
		expect("<rule from=\"a\" to=\"b\"/><query><rule from=\"a\" to=\"b\"/></query>", "Duplicate rule");
		expect("<query><rule from=\"a\" to=\"b\"/><rule from=\"a\" to=\"b\" common=\"true\"/></query>",
				"Duplicate rule");
		// the same rule once enabled and once disabled is a duplicate too
		expect("<rule from=\"a\" to=\"b\"/><rule from=\"a\" to=\"b\" enabled=\"false\"/>", "Duplicate rule");
		// a lower layer replaces the upper rule of the key and moves it to its own place
		SearchVariantRules moved = SearchVariantRules.of("xx", List.of(
				layer("<query><rule from=\"a\" to=\"b\"/></query>"),
				layer("<rule from=\"a\" to=\"b\" replace=\"true\"/>")));
		assertEquals(1, moved.normalizations().size());
		assertEquals(1, moved.query().size());
		// a lower layer changes the to of a rule by disabling it and adding the new one
		SearchVariantRules changed = SearchVariantRules.of("xx", List.of(
				layer("<query><rule from=\"a\" to=\"b\"/></query>"),
				layer("<query><rule from=\"a\" to=\"b\" enabled=\"false\"/><rule from=\"a\" to=\"c\"/></query>")));
		assertEquals(1, changed.query().size());
		assertEquals("c", changed.query().get(0).to());
		try {
			SearchVariantRules.of("xx", List.of(layer("<rule from=\"a\" to=\"b\"/>"),
					layer("<rule from=\"a\" to=\"c\" enabled=\"false\"/>")));
			fail("a rule that no upper layer defines cannot be disabled");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("no upper layer defines it"));
		}
	}

	@Test
	public void replaceAndCommonBelongToTheirPlaces() {
		expect("<index><rule from=\"a\" to=\"b\" replace=\"true\"/></index>", "replace belongs");
		expect("<query><rule from=\"a\" to=\"b\" replace=\"true\"/></query>", "replace belongs");
		expect("<index><rule from=\"a\" to=\"b\" common=\"true\"/></index>", "common does not apply to <index>");
		expect("<query><rule from=\"(?iu)^a$\" to=\"b\" common=\"true\"/></query>", "common needs a plain word");
		expect("<rule from=\"a\" to=\"b\" replace=\"maybe\"/>", "expected true or false");
		// false is the default and is accepted everywhere
		of("<index><rule from=\"a\" to=\"b\" replace=\"false\"/></index><query><rule from=\"c\" to=\"d\" "
				+ "replace=\"false\" common=\"false\"/></query>");
	}

	@Test
	public void buildingAndIgnorableRules() {
		expect("<rule object=\"building\" from=\"a\"/>", "building rule belongs to <query>");
		expect("<index><rule object=\"building\" from=\"a\"/></index>", "building rule belongs to <query>");
		expect("<query><rule object=\"building\" from=\"a\" to=\"b\"/></query>", "building rule holds only from");
		expect("<query><rule object=\"building\" from=\"(?iu)^a$\"/></query>", "building rule needs a plain word");
		expect("<rule from=\"a\" to=\"\"/>", "ignorable word) belongs to <query>");
		expect("<index><rule from=\"a\" to=\"\"/></index>", "ignorable word) belongs to <query>");
		expect("<query><rule object=\"street\" from=\"a\" to=\"\"/></query>", "plain word of any object");
		expect("<query><rule from=\"(?iu)^a$\" to=\"\"/></query>", "plain word of any object");
		expect("<query><rule from=\"a\" to=\"\" common=\"true\"/></query>", "common already");
		SearchVariantRules rules = of("<query><rule object=\"building\" from=\"bis\"/><rule from=\"de\" to=\"\"/></query>");
		assertEquals(1, rules.buildings().size());
		assertEquals(1, rules.ignorables().size());
		assertTrue(rules.query().isEmpty());
	}

	@Test
	public void targetsAndSources() {
		expect("<query><rule from=\"a\" to=\"b c\"/></query>", "phrase belongs to <index>");
		expect("<rule from=\"a\" to=\"b c\"/>", "phrase belongs to <index>");
		expect("<rule from=\"(?iu)^a$\" to=\"b\"/>", "needs a plain word");
		expect("<query><rule from=\"a\"/></query>", "Missing to");
		expect("<query><rule to=\"a\"/></query>", "Missing from");
		expect("<query><rule from=\"a\" to=\"A\"/></query>", "changes nothing");
		expect("<index><rule from=\"a\" to=\"b\" mode=\"All\"/></index>", "mode applies to a regexp");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" mode=\"Some\"/></index>", "Invalid mode");
		expect("<index><rule from=\"(?iu)(a\" to=\"b\"/></index>", "Invalid regexp");
		expect("<index><rule from=\"(?iu)a+\" to=\"$1\"/></index>", "refers to a group");
		expect("<index><rule from=\"(?iu)a*\" to=\"b\"/></index>", "matches an empty text");
		expect("<query><rule from=\"a\" to=\"b\" foo=\"1\"/></query>", "Unknown attribute foo");
		expect("<query><rule from=\"a\" to=\"b\" object=\"road\"/></query>", "Unknown object 'road'");
		expect("<query><rule from=\"a\" to=\"b\" object=\"*,street\"/></query>", "not listed with others");
		expect("<query><rule from=\"a\" to=\"b\" enabled=\"false\" replace=\"true\"/></query>",
				"disabled rule holds only");
		// a phrase and a regexp with a group are fine in <index>
		SearchVariantRules rules = of("<index><rule object=\"street,poi\" "
				+ "from=\"(?iu)\\bStrada\\s+(\\d+)\\b\" to=\"S$1\"/></index>");
		assertEquals("S42", rules.index().get(0).apply("Strada 42"));
		assertTrue(rules.index().get(0).appliesTo("poi"));
		assertFalse(rules.index().get(0).appliesTo("locality"));
	}

	@Test
	public void structureOfTheFile() {
		expect("<variant from=\"a\" to=\"b\"/>", "Unexpected search rule tag variant");
		expect("<query><index/></query>", "Unexpected search rule tag index");
		expect("<query><rule from=\"a\" to=\"b\"><rule from=\"c\" to=\"d\"/></rule></query>",
				"Unexpected search rule tag rule");
		try {
			SearchVariantRules.parseLayer(new ByteArrayInputStream(
					"<rules version=\"2\"/>".getBytes(StandardCharsets.UTF_8)), "old.xml");
			fail("version 2 is not supported");
		} catch (Exception expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("Unsupported search rules version"));
		}
	}

	@Test
	public void rulesOfOneLocaleAreConsistent() {
		expectLocale(List.of("<rule from=\"a\" to=\"b\" replace=\"true\"/><rule from=\"a\" to=\"c\" replace=\"true\"/>"),
				"replaced twice");
		// different objects do not overlap
		of("<rule object=\"street\" from=\"a\" to=\"b\" replace=\"true\"/>"
				+ "<rule object=\"poi\" from=\"a\" to=\"c\" replace=\"true\"/>");
		expectLocale(List.of("<rule object=\"street\" from=\"a\" to=\"b\" replace=\"true\"/>"
				+ "<rule from=\"a\" to=\"c\" replace=\"true\"/>"), "replaced twice");
		expectLocale(List.of("<query><rule object=\"building\" from=\"a\"/><rule from=\"a\" to=\"\"/></query>"),
				"building qualifier and an ignorable word");
		expectLocale(List.of("<query><rule from=\"a\" to=\"b\"/><rule from=\"a\" to=\"\"/></query>"),
				"ignorable word and has the form");
	}

	private static SearchVariantRules.Layer layer(String body) {
		try {
			return SearchVariantRules.parseLayer(new ByteArrayInputStream(
					("<rules version=\"3\">" + body + "</rules>").getBytes(StandardCharsets.UTF_8)), "test.xml");
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static SearchVariantRules of(String body) {
		return SearchVariantRules.of("xx", List.of(layer(body)));
	}

	private static void expect(String body, String message) {
		try {
			of(body);
			fail("expected an error: " + message);
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains(message));
		}
	}

	private static void expectLocale(List<String> bodies, String message) {
		try {
			SearchVariantRules.of("xx", bodies.stream().map(SearchVariantRulesTest::layer).collect(Collectors.toList()));
			fail("expected an error: " + message);
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains(message));
		}
	}
}
