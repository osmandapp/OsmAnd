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

	private static List<String> forms(SearchVariantRules rules, String word) {
		return rules.forms(word).stream().map(f -> f.object() + ":" + f.word()).collect(Collectors.toList());
	}

	@Test
	public void localeRules() {
		// the base holds no word rules: words belong to a language
		SearchVariantRules base = SearchVariantRules.forLocale("");
		assertEquals(0, base.index().size() + base.query().size() + base.common().size());
		// one word, several meanings: the first is the main one
		assertEquals(List.of("street", "saint"), forms("st", "en"));
		assertEquals(List.of("drive", "doctor"), forms("dr", "en"));
		assertEquals(List.of("sankt"), forms("st", "de_DE"));
		assertEquals(List.of("saint"), forms("st", "fr_FR"));
		assertEquals(List.of(), forms("rd", "de_DE"));
		assertEquals(List.of("avenue"), forms("ave", "en"));
		assertEquals(List.of("san", "santo", "santa"), forms("s", "it_IT"));
		assertEquals(List.of("south"), forms("s", "en"));
		assertEquals(List.of("проспект", "проезд"), forms("пр", "ru_RU"));
		assertEquals(List.of("вулиця", "улица"), forms("ул", "uk_UA"));
		// reverse forms: the full word gets the abbreviation, of the same owner
		assertEquals(List.of("st"), forms("street", "en"));
		assertEquals(List.of("st"), forms("saint", "en"));
		assertEquals(List.of("dr"), forms("doctor", "en"));
		assertEquals(List.of("av", "ave"), forms("avenue", "en"));
		assertEquals(List.of("1st"), forms("first", "en"));
		assertEquals(List.of("о"), forms("остров", "ru_RU"));
		// a token is aligned: the reverse form of a word with ß or a diacritic is found by its aligned spelling
		assertEquals(List.of("str"), forms("Straße", "de_DE"));
		assertEquals(List.of("str"), forms("strasse", "de_DE"));
		assertEquals(List.of("urb"), forms("urbanizacion", "es_ES"));
		assertEquals(List.of("jr"), forms("Jirón", "es_PE"));
		assertEquals(List.of("пр"), forms("проезд", "ru_RU"));
		assertEquals(List.of("просп", "пр"), forms("проспект", "ru_RU"));
		SearchVariantRules en = SearchVariantRules.forLocale("en");
		assertEquals(List.of("street:street", "*:saint"), forms(en, "st"));
		assertEquals(List.of("street:st"), forms(en, "street"));
		assertEquals(List.of("*:st"), forms(en, "saint"));
		assertEquals(List.of("street:e"), forms(en, "east"));
		assertEquals(List.of("street:place"), forms(en, "pl"));

		assertTrue(Abbreviations.likelyPartOfBuilding("apt", null, "en"));
		assertTrue(Abbreviations.likelyPartOfBuilding("bis", null, "fr_FR"));
		assertTrue(Abbreviations.likelyPartOfBuilding("д", null, "ru_RU"));
		assertTrue(Abbreviations.isConjunction("и", "ru_RU"));
		assertFalse(Abbreviations.isConjunction("и", "uk_UA"));
		assertTrue(Abbreviations.isConjunction("die", "de_AT"));
		assertFalse(Abbreviations.isConjunction("die", "en_US"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("street", "en_GB"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("saint", "en_GB"));
		assertFalse(Abbreviations.isCommonSkipOtherCnt("street", "de_DE"));
		// titles of German names are no unmatched words: "Dr.-Weber-Straße", "Friedenskapelle St. Josef"
		assertTrue(Abbreviations.isCommonSkipOtherCnt("dr", "de_DE"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("sankt", "de_LI"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("sainte", "fr_FR"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("doctor", "es_ES"));
		// an ignorable word is common too
		assertTrue(Abbreviations.isCommonSkipOtherCnt("the", "en"));
		// CommonWords shares the frequency of a pair of common words: the main form only
		assertEquals("street", Abbreviations.getAbbreviations("en").get("st"));
		assertEquals("drive", Abbreviations.getAbbreviations("en").get("dr"));
		assertNull(Abbreviations.getAbbreviations("en").get("pl"));

		assertEquals("en_US", SearchLocales.forMap("Us_new-york_north-america.obf"));
		assertEquals("de_DE", SearchLocales.forMap("Germany_bayern_europe.obf"));
		assertEquals("it_IT", SearchLocales.forMap("Italy_lombardia_europe.obf"));
	}

	@Test
	public void indexRules() {
		SearchVariantRules german = SearchVariantRules.forLocale("de");
		SearchVariantRules.Rule strasse = german.index().stream()
				.filter(v -> "Hallerstr 36".equals(v.apply("Hallerstraße 36"))).findFirst().orElseThrow();
		assertTrue(strasse.appliesTo("street"));
		assertFalse(strasse.appliesTo("poi"));
		assertNull(strasse.apply("Straße 36"));
		SearchVariantRules italian = SearchVariantRules.forLocale("it");
		assertTrue(italian.index().stream().anyMatch(v -> "SS 42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))));
		assertTrue(italian.index().stream().anyMatch(v -> "SS42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))));
		// no English index rule: a pair of plain words belongs to the query
		assertTrue(SearchVariantRules.forLocale("en").index().isEmpty());
		SearchVariantRules rules = of("<index><rule object=\"street,poi\" from=\"(?iu)\\bStrada\\s+(\\d+)\\b\" "
				+ "to=\"S$1\"/></index>");
		assertEquals("S42", rules.index().get(0).apply("Strada 42"));
		assertTrue(rules.index().get(0).appliesTo("poi"));
		assertFalse(rules.index().get(0).appliesTo("locality"));
	}

	@Test
	public void layersReplaceAWordRuleWhole() {
		// rules_en_US.xml of the test resources: e -> Eastern, hwy disabled, ave -> Esplanade
		assertEquals(List.of("eastern"), forms("e", "en_US"));
		assertEquals(List.of("east"), forms("e", "en"));
		assertEquals(List.of(), forms("hwy", "en_US"));
		assertEquals(List.of("highway"), forms("hwy", "en"));
		assertEquals(List.of("esplanade"), forms("ave", "en_US"));
		assertEquals(List.of("av"), forms("avenue", "en_US"));
		assertTrue(Abbreviations.likelyPartOfBuilding("tower", null, "en_US"));
		assertFalse(Abbreviations.likelyPartOfBuilding("tower", null, "en"));
		assertTrue(Abbreviations.isConjunction("thee", "en_US"));
		assertTrue(Abbreviations.isCommonSkipOtherCnt("eastern", "en_US"));
		assertEquals("CR7", SearchVariantRules.forLocale("en_US").index().get(0).apply("County Road 7"));

		// a lower layer replaces a rule whole: here it changes the order of the meanings
		SearchVariantRules replaced = SearchVariantRules.of("xx", List.of(
				layer("<query><rule from=\"st\"><to object=\"street\">Street</to><to>Saint</to></rule></query>"),
				layer("<query><rule from=\"st\"><to>Saint</to><to object=\"street\">Street</to></rule></query>")));
		assertEquals(List.of("*:saint", "street:street"), forms(replaced, "st"));
		// and one meaning replaces several
		SearchVariantRules one = SearchVariantRules.of("xx", List.of(
				layer("<query><rule from=\"st\"><to object=\"street\">Street</to><to>Saint</to></rule></query>"),
				layer("<query><rule from=\"st\" to=\"Saint\"/></query>")));
		assertEquals(List.of("*:saint"), forms(one, "st"));
		// a rule that no upper layer defines cannot be disabled, a common word that it does not list cannot be removed
		expectLayers(List.of("<query><rule from=\"aa\" to=\"bb\"/></query>",
				"<query><rule from=\"cc\" enabled=\"false\"/></query>"), "no upper layer defines it");
		expectLayers(List.of("<common>aa</common>", "<common enabled=\"false\">bb</common>"), "no upper layer lists it");
		SearchVariantRules uncommon = SearchVariantRules.of("xx", List.of(layer("<common>aa bb</common>"),
				layer("<common enabled=\"false\">aa</common>")));
		assertEquals(List.of("bb"), List.copyOf(uncommon.common()));
	}

	@Test
	public void reverseFormIsOneStep() {
		SearchVariantRules rules = of("<query><rule from=\"st\"><to object=\"street\">Street</to><to>Saint</to></rule>"
				+ "<rule from=\"saint\" to=\"Sanctus\"/></query>");
		// "saint" leads to "st" but never to the forms of "st": "Saint Louis" does not become "Louis Street"
		assertEquals(List.of("*:sanctus", "*:st"), forms(rules, "saint"));
		assertEquals(List.of("*:saint"), forms(rules, "sanctus"));
		assertEquals(List.of("street:st"), forms(rules, "street"));
	}

	@Test
	public void oneFormIsTheAttributeSeveralAreElements() {
		SearchVariantRules short1 = of("<query><rule from=\"pl\" to=\"Place\" object=\"street\"/>"
				+ "<rule from=\"apt\" object=\"building\"/><rule from=\"the\" to=\"\"/></query>");
		assertEquals(List.of("street:place"), forms(short1, "pl"));
		assertEquals(List.of("apt"), List.copyOf(short1.buildings()));
		assertEquals(List.of("the"), List.copyOf(short1.ignorables()));
		expect("<query><rule from=\"pl\"><to object=\"street\">Place</to></rule></query>", "One form is the attribute to");
		expect("<query><rule from=\"apt\"><to object=\"building\"/></rule></query>", "One form is the attribute to");
		expect("<query><rule from=\"the\"><to/></rule></query>", "One form is the attribute to");
		expect("<query><rule from=\"st\" to=\"Street\"><to>Saint</to><to>Sanctus</to></rule></query>",
				"the attribute to or several <to>, not both");
		expect("<query><rule from=\"st\" to=\"Street\"><to>Saint</to></rule></query>",
				"the attribute to or several <to>, not both");
		expect("<query><rule from=\"st\" object=\"street\"><to>Street</to><to>Saint</to></rule></query>",
				"is an attribute of each <to>");
		expect("<query><rule from=\"pl\" object=\"street\"/></query>", "Missing to");
		expect("<query><rule from=\"bis\"/></query>", "Missing to");
		expect("<query><rule from=\"apt\" to=\"Apartment\" object=\"building\"/></query>", "has no form");
		expect("<query><rule from=\"apt\" to=\"\" object=\"building\"/></query>", "has no form");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\"><to>c</to></rule></index>", "An index rule has one form");
		expect("<index><rule from=\"(?iu)a+\"/></index>", "Missing to");
		expect("<query><rule from=\"st\" enabled=\"false\" to=\"Street\"/></query>", "disabled rule holds only");
		expect("<query><rule from=\"apt\" enabled=\"false\" object=\"building\"/></query>", "disabled rule holds only");
	}

	@Test
	public void placeOfARule() {
		expect("<rule from=\"st\" to=\"Street\"/>", "belongs to <index> or <query>");
		expect("<index><rule from=\"dr\" to=\"Drive\"/></index>", "plain word belongs to <query>");
		expect("<query><rule from=\"(?iu)^Cr\\.?$\" to=\"Carrera\"/></query>", "needs a plain word");
		expect("<query><rule from=\"st.\" to=\"Street\"/></query>", "needs a plain word");
		expect("<query><rule from=\"st\" to=\"Street\" mode=\"All\"/></query>", "mode applies to a regexp");
		// a pair belongs to one side: the query form "pl" and the index form "Pl" of "Place" would chain
		expect("<index><rule from=\"(?iu)\\bPlace\\b\" to=\"Pl\"/></index>"
				+ "<query><rule from=\"pl\" to=\"Place\"/></query>", "a pair belongs to one side");
		expect("<index><rule from=\"(?iu)\\bPlace\\b\" to=\"Place\"/></index>"
				+ "<query><rule from=\"pl\" to=\"Place\"/></query>", "a pair belongs to one side");
	}

	@Test
	public void keyIsTheWord() {
		expect("<query><rule from=\"st\" to=\"Street\"/><rule from=\"st\" to=\"Saint\"/></query>", "Duplicate rule");
		expect("<query><rule from=\"St\" to=\"Street\"/><rule from=\"st\" to=\"Saint\"/></query>", "Duplicate rule");
		expect("<query><rule from=\"st\" to=\"Street\"/><rule from=\"st\" enabled=\"false\"/></query>", "Duplicate rule");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\"/><rule from=\"(?iu)a+\" to=\"c\"/></index>", "Duplicate rule");
		// two index rules with one regexp and different owners are different rules
		of("<index><rule object=\"street\" from=\"(?iu)a+\" to=\"b\"/>"
				+ "<rule object=\"poi\" from=\"(?iu)a+\" to=\"c\"/></index>");
		expect("<query><rule from=\"st\" enabled=\"false\"><to>Street</to><to>Saint</to></rule></query>",
				"disabled rule holds only");
		expect("<query><rule from=\"st\"><to enabled=\"false\">Saint</to><to>Street</to></rule></query>",
				"Unknown attribute enabled");
	}

	@Test
	public void formsOfAWord() {
		expect("<query><rule from=\"st\" to=\"St\"/></query>", "repeats the word");
		expect("<query><rule from=\"st\"><to>Street</to><to object=\"street\">Street</to></rule></query>",
				"listed twice");
		// one word for different owners is no repetition
		of("<query><rule from=\"st\"><to object=\"street\">Street</to><to object=\"poi\">Street</to></rule></query>");
		expect("<query><rule from=\"д\"><to object=\"building\">Дом</to><to object=\"locality\">Деревня</to>"
				+ "</rule></query>", "has no form");
		expect("<query><rule from=\"о\"><to/><to object=\"poi\">Остров</to></rule></query>",
				"ignorable word has no forms");
		expect("<query><rule from=\"о\"><to object=\"poi\">Остров</to><to/></rule></query>",
				"ignorable word has no forms");
		expect("<query><rule from=\"bis\"><to object=\"building\"/><to/></rule></query>",
				"house-number qualifier and an ignorable word");
		expect("<query><rule from=\"bis\"><to object=\"building\"/><to object=\"building\"/></rule></query>",
				"listed twice");
		expect("<query><rule from=\"de\" to=\"\" object=\"street\"/></query>", "applies to every object");
		expect("<query><rule from=\"st\" to=\"Saint Louis\"/></query>", "phrase belongs to <index>");
		expect("<query><rule from=\"st\" to=\"Street\" object=\"road\"/></query>", "Unknown object 'road'");
		expect("<query><rule from=\"st\"><to object=\"road\">Street</to><to>Saint</to></rule></query>",
				"Unknown object 'road'");
		expect("<query><rule from=\"st\" to=\"Street\" object=\"*,street\"/></query>", "not listed with others");
		expect("<query><rule from=\"st\"><form>Street</form></rule></query>", "Unexpected search rule tag form");
		expect("<query><rule to=\"a\"/></query>", "Missing from");
		// a one-letter word needs an owner: the reverse form "e" of "East" would match every "e"
		expect("<query><rule from=\"e\" to=\"East\"/></query>", "one-letter word needs an object");
		of("<query><rule from=\"e\" to=\"East\" object=\"street\"/></query>");
		// a house-number qualifier and its form are two meanings of one word ("д. 18", "д. Ивановка")
		SearchVariantRules d = of("<query><rule from=\"д\"><to object=\"building\"/>"
				+ "<to object=\"locality\">Деревня</to></rule></query>");
		assertEquals(List.of("д"), List.copyOf(d.buildings()));
		assertEquals(List.of("locality:деревня"), forms(d, "д"));
	}

	@Test
	public void indexForms() {
		expect("<index><rule from=\"(?iu)a+\" to=\"\"/></index>", "ignorable word) belongs to <query>");
		expect("<index><rule object=\"building\" from=\"(?iu)a+\" to=\"b\"/></index>",
				"house-number qualifier belongs to <query>");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" mode=\"Some\"/></index>", "Invalid mode");
		expect("<index><rule from=\"(?iu)(a\" to=\"b\"/></index>", "Invalid regexp");
		expect("<index><rule from=\"(?iu)a+\" to=\"$1\"/></index>", "refers to a group");
		expect("<index><rule from=\"(?iu)a*\" to=\"b\"/></index>", "matches an empty text");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" enabled=\"false\"/></index>", "disabled rule holds only");
	}

	@Test
	public void removedAttributes() {
		expect("<query><rule from=\"st\" to=\"Street\" replace=\"true\"/></query>", "replace is removed");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" replace=\"false\"/></index>", "replace is removed");
		expect("<query><rule from=\"st\" to=\"Street\" common=\"true\"/></query>", "listed in <common>");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" common=\"true\"/></index>", "listed in <common>");
		expect("<query><rule from=\"st\" to=\"Street\" foo=\"1\"/></query>", "Unknown attribute foo");
		expect("<query><rule from=\"st\" to=\"Street\" enabled=\"maybe\"/></query>", "expected true or false");
	}

	@Test
	public void commonWords() {
		SearchVariantRules rules = of("<common>St street\n  Saint</common>");
		assertEquals(List.of("st", "street", "saint"), List.copyOf(rules.common()));
		expect("<common>st st</common>", "listed twice");
		expect("<common>st.</common>", "common word is a plain word");
		expect("<common foo=\"1\">st</common>", "Unknown attribute foo");
		expect("<common>de</common><query><rule from=\"de\" to=\"\"/></query>",
				"house-number qualifier or an ignorable word");
		expect("<common>bis</common><query><rule from=\"bis\" object=\"building\"/></query>",
				"house-number qualifier or an ignorable word");
	}

	@Test
	public void rulesOfOneLocaleAreConsistent() {
		// an explicit rule that repeats a generated reverse form
		expect("<query><rule from=\"1st\" to=\"First\"/><rule from=\"first\" to=\"1st\"/></query>",
				"reverse forms are generated");
		expectLayers(List.of("<query><rule from=\"о\" to=\"Остров\" object=\"poi\"/></query>",
				"<query><rule from=\"остров\" to=\"О\"/></query>"), "reverse forms are generated");
		// a form that is a house-number qualifier or an ignorable word of the locale
		expect("<query><rule from=\"xx\" to=\"The\"/><rule from=\"the\" to=\"\"/></query>",
				"house-number qualifier or an ignorable word");
		expect("<query><rule from=\"house\" to=\"Apt\"/><rule from=\"apt\" object=\"building\"/></query>",
				"house-number qualifier or an ignorable word");
	}

	@Test
	public void structureOfTheFile() {
		expect("<variant from=\"a\"/>", "Unexpected search rule tag variant");
		expect("<query><index/></query>", "Unexpected search rule tag index");
		expect("<query><rule from=\"aa\"><to>bb</to><rule from=\"cc\"/></rule></query>",
				"Unexpected search rule tag rule");
		try {
			SearchVariantRules.parseLayer(new ByteArrayInputStream(
					"<rules version=\"3\"/>".getBytes(StandardCharsets.UTF_8)), "old.xml");
			fail("version 3 is not supported");
		} catch (Exception expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("Unsupported search rules version"));
		}
	}

	private static SearchVariantRules.Layer layer(String body) {
		try {
			return SearchVariantRules.parseLayer(new ByteArrayInputStream(
					("<rules version=\"4\">" + body + "</rules>").getBytes(StandardCharsets.UTF_8)), "test.xml");
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
		expectLayers(List.of(body), message);
	}

	private static void expectLayers(List<String> bodies, String message) {
		try {
			SearchVariantRules.of("xx", bodies.stream().map(SearchVariantRulesTest::layer).collect(Collectors.toList()));
			fail("expected an error: " + message);
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains(message));
		}
	}
}
