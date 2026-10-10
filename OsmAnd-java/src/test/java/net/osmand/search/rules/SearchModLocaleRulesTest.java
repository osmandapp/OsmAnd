package net.osmand.search.rules;

import net.osmand.search.rules.SearchModRules.SearchModRuleOwner;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SearchModLocaleRulesTest {
	private final SearchModRules searchRules = new SearchModRules();

	private List<String> forms(String word, String locale) {
		return searchRules.dictionary(locale).getQueryForms(word).stream().map(SearchModDictionary.QueryForm::word)
				.collect(Collectors.toList());
	}

	private List<String> forms(SearchModLocaleRules rules, String word) {
		return rules.forms(word).stream().map(f -> f.object() + ":" + f.word()).collect(Collectors.toList());
	}

	@Test
	public void localeRules() {
		// the base holds no word rules: words belong to a language; it holds the rules of glued words of every name
		SearchModLocaleRules base = searchRules.rules("");
		assertEquals(0, base.index().size() + base.query().size() + base.skipPenalty().size() + base.classes().size());
		assertEquals(2, base.unglues().size());
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
		SearchModLocaleRules en = searchRules.rules("en");
		assertEquals(List.of("street:street", "*:saint"), forms(en, "st"));
		assertEquals(List.of("street:st"), forms(en, "street"));
		assertEquals(List.of("*:st"), forms(en, "saint"));
		assertEquals(List.of("street:e"), forms(en, "east"));
		assertEquals(List.of("street:place"), forms(en, "pl"));

		assertTrue(searchRules.dictionary("en").likelyPartOfBuilding("apt", null));
		assertTrue(searchRules.dictionary("fr_FR").likelyPartOfBuilding("bis", null));
		assertTrue(searchRules.dictionary("ru_RU").likelyPartOfBuilding("д", null));
		assertTrue(searchRules.dictionary("ru_RU").isIgnorable("и"));
		assertFalse(searchRules.dictionary("uk_UA").isIgnorable("и"));
		assertTrue(searchRules.dictionary("de_AT").isIgnorable("die"));
		assertFalse(searchRules.dictionary("en_US").isIgnorable("die"));
		assertTrue(searchRules.dictionary("en_GB").isCommonSkipOtherCnt("street", SearchModRuleOwner.STREET));
		assertTrue(searchRules.dictionary("en_GB").isCommonSkipOtherCnt("saint", SearchModRuleOwner.POI));
		assertFalse(searchRules.dictionary("de_DE").isCommonSkipOtherCnt("street", SearchModRuleOwner.STREET));
		// titles of German names are no unmatched words: "Dr.-Weber-Straße", "Friedenskapelle St. Josef"
		assertTrue(searchRules.dictionary("de_DE").isCommonSkipOtherCnt("dr", SearchModRuleOwner.STREET));
		assertTrue(searchRules.dictionary("de_LI").isCommonSkipOtherCnt("sankt", SearchModRuleOwner.POI));
		assertTrue(searchRules.dictionary("fr_FR").isCommonSkipOtherCnt("sainte", SearchModRuleOwner.LOCALITY));
		assertTrue(searchRules.dictionary("es_ES").isCommonSkipOtherCnt("doctor", SearchModRuleOwner.STREET));
		// an ignorable word never penalizes a name of any owner
		assertTrue(searchRules.dictionary("en").isCommonSkipOtherCnt("the", SearchModRuleOwner.POI));

		assertEquals("en_US", searchRules.locales().forMap("Us_new-york_north-america.obf"));
		assertEquals("de_DE", searchRules.locales().forMap("Germany_bayern_europe.obf"));
		assertEquals("it_IT", searchRules.locales().forMap("Italy_lombardia_europe.obf"));
	}

	@Test
	public void indexRules() {
		SearchModLocaleRules german = searchRules.rules("de");
		SearchModLocaleRules.Rule strasse = german.index().stream()
				.filter(v -> "Hallerstr 36".equals(v.apply("Hallerstraße 36"))).findFirst().orElseThrow();
		assertTrue(strasse.appliesTo(SearchModRuleOwner.STREET));
		assertFalse(strasse.appliesTo(SearchModRuleOwner.POI));
		assertNull(strasse.apply("Straße 36"));
		SearchModLocaleRules italian = searchRules.rules("it");
		assertTrue(italian.index().stream().anyMatch(v -> "SS 42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))));
		assertTrue(italian.index().stream().anyMatch(v -> "SS42 del Tonale".equals(v.apply("Strada Statale 42 del Tonale"))));
		SearchModLocaleRules rules = of("<index><rule object=\"street,poi\" from=\"(?iu)\\bStrada\\s+(\\d+)\\b\" "
				+ "to=\"S$1\"/></index>");
		assertEquals("S42", rules.index().get(0).apply("Strada 42"));
		assertTrue(rules.index().get(0).appliesTo(SearchModRuleOwner.POI));
		assertFalse(rules.index().get(0).appliesTo(SearchModRuleOwner.LOCALITY));
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
		assertTrue(searchRules.dictionary("en_US").likelyPartOfBuilding("tower", null));
		assertFalse(searchRules.dictionary("en").likelyPartOfBuilding("tower", null));
		assertTrue(searchRules.dictionary("en_US").isIgnorable("thee"));
		assertTrue(searchRules.dictionary("en_US").isCommonSkipOtherCnt("eastern", SearchModRuleOwner.STREET));
		assertEquals("CR7", searchRules.rules("en_US").index().get(0).apply("County Road 7"));

		// a lower layer replaces a rule whole: here it changes the order of the meanings
		SearchModLocaleRules replaced = new SearchModLocaleRules("xx", List.of(
				layer("<query><rule from=\"st\"><to object=\"street\">Street</to><to>Saint</to></rule></query>"),
				layer("<query><rule from=\"st\"><to>Saint</to><to object=\"street\">Street</to></rule></query>")));
		assertEquals(List.of("*:saint", "street:street"), forms(replaced, "st"));
		// and one meaning replaces several
		SearchModLocaleRules one = new SearchModLocaleRules("xx", List.of(
				layer("<query><rule from=\"st\"><to object=\"street\">Street</to><to>Saint</to></rule></query>"),
				layer("<query><rule from=\"st\" to=\"Saint\"/></query>")));
		assertEquals(List.of("*:saint"), forms(one, "st"));
		// a rule or a word that no upper layer defines cannot be disabled
		expectLayers(List.of("<query><rule from=\"aa\" to=\"bb\"/></query>",
				"<query><rule from=\"cc\" enabled=\"false\"/></query>"), "no upper layer defines it");
		expectLayers(List.of("<query><skipPenalty>aa</skipPenalty></query>",
				"<query><skipPenalty enabled=\"false\">bb</skipPenalty></query>"), "no upper layer defines it");
		SearchModLocaleRules uncommon = new SearchModLocaleRules("xx", List.of(
				layer("<query><skipPenalty>aa bb</skipPenalty></query>"),
				layer("<query><skipPenalty enabled=\"false\">aa</skipPenalty></query>")));
		assertEquals(List.of("bb"), List.copyOf(uncommon.skipPenalty().keySet()));
	}

	@Test
	public void reverseFormIsOneStep() {
		SearchModLocaleRules rules = of("<query><rule from=\"st\"><to object=\"street\">Street</to><to>Saint</to></rule>"
				+ "<rule from=\"saint\" to=\"Sanctus\"/></query>");
		// "saint" leads to "st" but never to the forms of "st": "Saint Louis" does not become "Louis Street"
		assertEquals(List.of("*:sanctus", "*:st"), forms(rules, "saint"));
		assertEquals(List.of("*:saint"), forms(rules, "sanctus"));
		assertEquals(List.of("street:st"), forms(rules, "street"));
	}

	@Test
	public void oneFormIsTheAttributeSeveralAreElements() {
		SearchModLocaleRules short1 = of("<query><rule from=\"pl\" to=\"Place\" object=\"street\"/>"
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
		expect("<query><rule from=\"st\" to=\"Street\" mode=\"All\"/></query>", "mode applies to a regexp");
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
		SearchModLocaleRules d = of("<query><rule from=\"д\"><to object=\"building\"/>"
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
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" enabled=\"false\"/></index>", "disabled rule holds only");
	}

	@Test
	public void removedAttributes() {
		expect("<query><rule from=\"st\" to=\"Street\" replace=\"true\"/></query>", "replace is removed");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" replace=\"false\"/></index>", "replace is removed");
		expect("<query><rule from=\"st\" to=\"Street\" common=\"true\"/></query>", "listed in <skipPenalty>");
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" common=\"true\"/></index>", "listed in <skipPenalty>");
		expect("<query><rule from=\"st\" to=\"Street\" foo=\"1\"/></query>", "Unknown attribute foo");
		expect("<query><rule from=\"st\" to=\"Street\" enabled=\"maybe\"/></query>", "expected true or false");
	}

	@Test
	public void skipPenaltyWords() {
		SearchModLocaleRules rules = of("<query><skipPenalty>St street\n  Saint</skipPenalty>"
				+ "<skipPenalty object=\"street\">de la</skipPenalty><skipPenalty object=\"poi\">de</skipPenalty></query>");
		assertEquals(List.of("st", "street", "saint", "de", "la"), List.copyOf(rules.skipPenalty().keySet()));
		assertEquals(List.of("street", "poi"), rules.skipPenalty().get("de"));
		assertEquals(List.of("*"), rules.skipPenalty().get("st"));
		expect("<query><skipPenalty>st st</skipPenalty></query>", "listed twice");
		expect("<query><skipPenalty>st</skipPenalty><skipPenalty object=\"street\">st</skipPenalty></query>",
				"overlapping object");
		expect("<query><skipPenalty foo=\"1\">st</skipPenalty></query>", "Unknown attribute foo");
		expect("<query><skipPenalty object=\"building\">st</skipPenalty></query>", "object=\"building\"");
		expect("<query><skipPenalty object=\"road\">st</skipPenalty></query>", "Unknown object 'road'");
		// an ignorable word already never penalizes a name; a house-number qualifier is no word of a name
		expect("<query><skipPenalty>de</skipPenalty><rule from=\"de\" to=\"\"/></query>",
				"house-number qualifier or an ignorable word");
		expect("<query><skipPenalty>bis</skipPenalty><rule from=\"bis\" object=\"building\"/></query>",
				"house-number qualifier or an ignorable word");
		// a word of class 1 or 2 never penalizes a name either
		expect("<index><class1>rue</class1></index><query><skipPenalty>rue</skipPenalty></query>", "<class1>");
		// to="" is an ignorable word of every owner: the penalty of some owners is <skipPenalty object>
		expect("<query><rule from=\"de\" to=\"\" object=\"street\"/></query>", "<skipPenalty object=\"street\">");
		expect("<common>st</common>", "<common> is replaced by <skipPenalty>");

		assertTrue(searchRules.dictionary("en").isCommonSkipOtherCnt("st", SearchModRuleOwner.POI));
		assertFalse(searchRules.dictionary("en").isCommonSkipOtherCnt("main", SearchModRuleOwner.STREET));
	}

	@Test
	public void classesOfIndexWords() {
		SearchModLocaleRules rules = of("<index><class0>Parkway</class0><class1>улица ул</class1><class2>Straße</class2>"
				+ "</index>");
		assertEquals(Integer.valueOf(0), rules.wordClass("parkway"));
		assertEquals(Integer.valueOf(1), rules.wordClass("Улица"));
		// words are aligned as the statistics of common_words_groups.tsv
		assertEquals(Integer.valueOf(2), rules.wordClass("strasse"));
		assertNull(rules.wordClass("road"));
		expect("<index><class1>rue</class1><class2>rue</class2></index>", "listed twice");
		expect("<query><class1>rue</class1></query>", "Unexpected search rule tag class1");
		expect("<index><class0>the</class0></index><query><rule from=\"the\" to=\"\"/></query>", "<class0>");
		// a lower layer moves a word to its class, enabled="false" returns it to the statistics
		SearchModLocaleRules moved = new SearchModLocaleRules("xx", List.of(layer("<index><class2>rue</class2></index>"),
				layer("<index><class1>rue</class1></index>")));
		assertEquals(Integer.valueOf(1), moved.wordClass("rue"));
		SearchModLocaleRules removed = new SearchModLocaleRules("xx", List.of(layer("<index><class2>rue</class2></index>"),
				layer("<index><class2 enabled=\"false\">rue</class2></index>")));
		assertNull(removed.wordClass("rue"));
		expectLayers(List.of("<index><class2>rue</class2></index>",
				"<index><class1 enabled=\"false\">via</class1></index>"), "no upper layer defines it");
	}

	@Test
	public void unglueRules() {
		SearchModLocaleRules base = searchRules.rules("");
		assertEquals(List.of("Atelier Anaïs"), unglue(base, "L'Atelier d'Anaïs"));
		assertEquals(List.of("Hoffmann"), unglue(base, "E.T.A. Hoffmann"));
		// the rules split a word of two glues in one pass
		assertEquals(List.of("Wijkopenauto nl"), unglue(base, "Wijkopenauto's.nl"));
		assertEquals(List.of("Atelier"), unglue(base, "L'Atelier..."));
		assertEquals(List.of("Résidence de ambassadeur rice Australie"), unglue(base, "Résidence de l'ambassadeur.rice d'Australie"));
		// an apostrophe glues only Latin words, a dot every word, a word with a digit is never split
		assertEquals(List.of(), unglue(base, "Об'єднання"));
		assertEquals(List.of("Чайковский"), unglue(base, "П.И.Чайковский"));
		assertEquals(List.of(), unglue(base, "St.42"));
		assertEquals(List.of(), unglue(base, "Main Street"));
		// the statistics of generation tell the rules apart by file, object and from (rules-spec.md, 4.3)
		assertEquals("[rules.xml unglue ']", base.unglue("L'Atelier d'Anaïs").ids().toString());
		assertEquals("[rules.xml unglue ., rules.xml unglue ']", base.unglue("Wijkopenauto's.nl").ids().toString());
		// one name of two rules is one alternative name, counted for each rule
		SearchModLocaleRules same = of("<index><unglue glue=\".\" minPart=\"3\"/><unglue glue=\"'\" minPart=\"3\"/></index>");
		assertEquals(List.of("Hoffmann"), unglue(same, "A.'B Hoffmann"));
		assertEquals("[test.xml unglue ., test.xml unglue ']", same.unglue("A.'B Hoffmann").ids().toString());
		SearchModLocaleRules noApostrophe = new SearchModLocaleRules("xx", List.of(
				layer("<index><unglue glue=\".\"/><unglue glue=\"'\" script=\"Latin\"/></index>"),
				layer("<index><unglue glue=\"'\" enabled=\"false\"/></index>")));
		assertEquals(List.of(), unglue(noApostrophe, "L'Atelier"));
		assertEquals(List.of("Mak by"), unglue(noApostrophe, "Mak.by"));
		// a part takes the minPart of the glues around it: "Ab" is too short for the dot
		SearchModLocaleRules limits = of("<index><unglue glue=\".\" minPart=\"4\"/><unglue glue=\"'\"/></index>");
		assertEquals(List.of("Cdef Gh"), unglue(limits, "Ab.Cdef'Gh"));
		assertEquals(List.of("Abcd Gh"), unglue(limits, "Abcd'Gh"));
		expect("<index><unglue glue=\"ab\"/></index>", "one character");
		expect("<index><unglue glue=\"a\"/></index>", "one character");
		expect("<index><unglue glue=\".\" script=\"Klingon\"/></index>", "Unknown script");
		expect("<index><unglue glue=\".\" minPart=\"0\"/></index>", "Invalid minPart");
		expect("<index><unglue glue=\".\"/><unglue glue=\".\" minPart=\"3\"/></index>", "Duplicate rule");
	}

	@Test
	public void keysOfAnIndexRule() {
		SearchModLocaleRules rules = of("<index><rule object=\"street\" from=\"(?iu)\\bCounty\\s+Road\\s+(\\d+)\\b\" "
				+ "to=\"CR$1\" keys=\"always\"/><rule object=\"street\" from=\"(?iu)\\bStrada\\s+(\\d+)\\b\" "
				+ "to=\"S$1\"/></index>");
		assertTrue(rules.index().get(0).alwaysKeys());
		assertFalse(rules.index().get(1).alwaysKeys());
		expect("<index><rule from=\"(?iu)a+\" to=\"b\" keys=\"some\"/></index>", "Invalid keys");
		expect("<index><rule from=\"(?iu)a+\" keys=\"always\" enabled=\"false\"/></index>",
				"disabled rule holds only");
	}

	@Test
	public void localesBelongToTheBase() {
		SearchModLocales table = searchRules.locales();
		assertEquals("en_US", table.forMap("us"));
		assertEquals("en", table.groupForMap("us"));
		assertEquals("esl", table.groupForMap("ukraine"));
		// a group by locale is stronger than a group by language: Maghreb is Arabic, a group of its own
		assertEquals("mag", table.groupForMap("morocco"));
		assertEquals("ar", table.groupForMap("egypt"));
		assertEquals("es", table.groupForMap("andorra"));
		// a map without a rules locale can still have a group
		assertEquals("", table.forMap("antarctica"));
		assertEquals("oth", table.groupForMap("antarctica"));
		assertEquals("ja", table.translitForMap("japan"));
		assertNull(table.translitForMap("italy"));
		expect("<locales><map locale=\"en_US\" prefixes=\"us\"/></locales>", "<locales> belongs to rules.xml");
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
			new SearchModRulesParser("old.xml", null).parse(new ByteArrayInputStream(
					"<rules version=\"4\"/>".getBytes(StandardCharsets.UTF_8)));
			fail("version 4 is not supported");
		} catch (Exception expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("Unsupported search rules version"));
		}
	}

	@Test
	public void everyRulesFileLoads() throws IOException {
		// the layers of every locale with a file validate together: mirror pairs, query rules and index rules
		for (String locale : List.of("bg_BG", "ca_ES", "de_DE", "en_US", "es_ES", "es_CO", "es_PE", "fr_FR", "it_IT",
				"mk_MK", "nl_NL", "pt_PT", "ru_RU", "sr_RS", "uk_UA")) {
			assertNotNull(locale, searchRules.rules(locale));
			Map<String, String> files = new LinkedHashMap<>();
			files.put(SearchModLocaleRules.BASE_FILE, resource(SearchModLocaleRules.BASE_FILE));
			String suffix = "";
			for (String part : locale.split("_")) {
				suffix += "_" + part;
				String xml = resource("rules" + suffix + ".xml");
				if (xml != null) {
					files.put("rules" + suffix + ".xml", xml);
				}
			}
			validate(locale, files);
		}
		// a mirror pair of Colombia shares "Calle" with the pair of Spanish; the one-letter "c" of Spanish is disabled
		// (it took the house letter of "86 C Calle de Velázquez")
		assertEquals(List.of("street:cl", "street:cll"), forms(searchRules.rules("es_CO"), "calle"));
		assertEquals(List.of(), forms(searchRules.rules("es_ES"), "c"));
	}

	@Test
	public void mirrorPairWorksOnBothSides() {
		SearchModLocaleRules rules = of("<rule object=\"street\" from=\"blvd\" to=\"Boulevard\"/>"
				+ "<rule from=\"dr\" to=\"Doktor\"/>");
		// [run]: a form and its reverse form, as a rule of <query>
		assertEquals(List.of("street:boulevard"), forms(rules, "blvd"));
		assertEquals(List.of("street:blvd"), forms(rules, "boulevard"));
		// [gen]: alternative names both ways, the dot of an abbreviation included, only whole words
		List<SearchModLocaleRules.Rule> index = rules.index();
		assertEquals(4, index.size());
		SearchModLocaleRules.Rule abbreviation = index.get(0);
		SearchModLocaleRules.Rule full = index.get(1);
		assertEquals("test.xml street blvd→Boulevard", abbreviation.id().toString());
		assertEquals("test.xml street Boulevard→blvd", full.id().toString());
		assertEquals("Sunset Boulevard", abbreviation.apply("Sunset Blvd."));
		assertEquals("Sunset Boulevard", abbreviation.apply("Sunset BLVD"));
		assertEquals("Sunset blvd", full.apply("Sunset Boulevard"));
		assertNull(full.apply("Boulevardier Cafe"));
		assertTrue(abbreviation.appliesTo(SearchModRuleOwner.STREET));
		assertFalse(abbreviation.appliesTo(SearchModRuleOwner.POI));
		assertEquals("Doktor-Weber-Straße", index.get(2).apply("Dr.-Weber-Straße"));
		assertEquals("Ludwig dr Allee", index.get(3).apply("Ludwig Doktor Allee"));
		assertNull(index.get(3).apply("Doktorandenweg"));
	}

	@Test
	public void mirrorPairsShareTheirFullWord() {
		// several abbreviations of one word are synonyms: a chain through the full word stays in its meaning
		SearchModLocaleRules rules = of("<rule object=\"street\" from=\"av\" to=\"Avenida\"/>"
				+ "<rule object=\"street\" from=\"avda\" to=\"Avenida\"/>"
				+ "<query><rule object=\"street\" from=\"ave\" to=\"Avenida\"/></query>");
		assertEquals(List.of("street:av", "street:avda", "street:ave"), forms(rules, "avenida"));
		assertEquals(4, rules.index().size());
	}

	@Test
	public void mirrorPairLayers() {
		String base = "<rule object=\"street\" from=\"blvd\" to=\"Boulevard\"/>";
		// a lower layer replaces the pair by a rule of <query> of the same word: the alternative names go too
		SearchModLocaleRules query = new SearchModLocaleRules("xx", List.of(layer(base),
				layer("<query><rule from=\"blvd\" to=\"Boulevard\" object=\"street\"/></query>")));
		assertEquals(0, query.index().size());
		assertEquals(List.of("street:boulevard"), forms(query, "blvd"));
		SearchModLocaleRules disabled = new SearchModLocaleRules("xx", List.of(layer(base),
				layer("<rule from=\"blvd\" enabled=\"false\"/>")));
		assertEquals(0, disabled.index().size());
		assertEquals(List.of(), forms(disabled, "blvd"));
		// and back: a lower layer makes a rule of <query> a mirror pair
		SearchModLocaleRules mirror = new SearchModLocaleRules("xx", List.of(
				layer("<query><rule from=\"blvd\" to=\"Boulevard\" object=\"street\"/></query>"), layer(base, "low.xml")));
		assertEquals("low.xml street blvd→Boulevard", mirror.index().get(0).id().toString());
	}

	@Test
	public void mirrorPairValidation() {
		expect("<rule from=\"st\"><to object=\"street\">Street</to><to>Saint</to></rule>", "one form");
		expect("<rule from=\"the\" to=\"\"/>", "ignorable word belongs to <query>");
		expect("<rule from=\"apt\" object=\"building\"/>", "house-number qualifier belongs to <query>");
		expect("<rule from=\"blvd\" to=\"blvd\"/>", "repeats the word");
		expect("<rule from=\"e\" to=\"East\"/>", "one-letter word needs an object");
		// the reverse pair, written by hand
		expect("<rule from=\"blvd\" to=\"Boulevard\"/><rule from=\"boulevard\" to=\"blvd\"/>", "");
		// one word in one file once, whether a mirror pair or a rule of <query>
		expect("<rule from=\"blvd\" to=\"Boulevard\"/><query><rule from=\"blvd\" to=\"Bulevar\"/></query>",
				"Duplicate rule");
		// the word of a pair means one thing: no other rule may reach it
		expect("<rule from=\"str\" to=\"Street\"/><query><rule from=\"s\" to=\"str\" object=\"street\"/></query>",
				"is a form of");
		// the form of a pair has no rule of its own
		expect("<rule from=\"blvd\" to=\"Boulevard\"/><query><rule from=\"boulevard\" to=\"Bd\"/></query>",
				"has its own rule");
		expect("<rule from=\"apt\" to=\"Apartment\"/><query><rule from=\"apartment\" object=\"building\"/></query>",
				"");
	}

	private List<String> unglue(SearchModLocaleRules rules, String name) {
		SearchModLocaleRules.Unglued unglued = rules.unglue(name);
		return unglued == null ? List.of() : List.of(unglued.name());
	}

	@Test
	public void penaltyFreeWordsAreAligned() {
		// a word of a name keeps its diacritics: "école" and "ecole" are one word of the dictionaries
		String locale = "fr_ZZ";
		SearchModLocaleRules rules = new SearchModLocaleRules(locale, List.of(layer("<index><class1>école</class1></index>"
				+ "<query><skipPenalty object=\"street\">straße</skipPenalty>"
				+ "<skipPenalty object=\"poi\">strasse</skipPenalty></query>")));
		assertEquals(List.of("street", "poi"), rules.skipPenalty().get("strasse"));
		SearchModDictionary dictionary = new SearchModDictionary(locale, rules);
		assertTrue(dictionary.isCommonSkipOtherCnt("école", SearchModRuleOwner.STREET));
		assertTrue(dictionary.isCommonSkipOtherCnt("ecole", SearchModRuleOwner.POI));
		// the owners of two spellings are merged
		assertTrue(dictionary.isCommonSkipOtherCnt("straße", SearchModRuleOwner.STREET));
		assertTrue(dictionary.isCommonSkipOtherCnt("strasse", SearchModRuleOwner.POI));
		assertFalse(dictionary.isCommonSkipOtherCnt("straße", SearchModRuleOwner.LOCALITY));
		expect("<index><class1>école</class1></index><query><skipPenalty>ecole</skipPenalty></query>",
				"<class1>");
	}

	private SearchModRulesParser.Layer layer(String body) {
		return layer(body, "test.xml");
	}

	private SearchModRulesParser.Layer layer(String body, String file) {
		try {
			SearchModLocales locales = SearchModLocaleRules.BASE_FILE.equals(file) ? new SearchModLocales() : null;
			return new SearchModRulesParser(file, locales).parse(new ByteArrayInputStream(
					("<rules version=\"5\">" + body + "</rules>").getBytes(StandardCharsets.UTF_8)));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private SearchModLocaleRules of(String body) {
		return new SearchModLocaleRules("xx", List.of(layer(body)));
	}

	private void expect(String body, String message) {
		expectLayers(List.of(body), message);
	}

	private void expectBase(String body, String message) {
		try {
			check(new SearchModRulesValidator(SearchModLocaleRules.BASE_FILE, new SearchModLocales()), xml(body));
			fail("expected an error: " + message);
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains(message));
		}
	}

	private void expectLayers(List<String> bodies, String message) {
		try {
			Map<String, String> files = new LinkedHashMap<>();
			for (String body : bodies) {
				files.put("test" + files.size() + ".xml", xml(body));
			}
			validate("xx", files);
			fail("expected an error: " + message);
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains(message));
		}
	}

	private String xml(String body) {
		return "<rules version=\"5\">" + body + "</rules>";
	}

	private void check(SearchModRulesValidator validator, String xml) {
		try {
			validator.check(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** checks every file (name -> xml) strictly, then the layers and the rules of the locale together */
	private void validate(String locale, Map<String, String> files) {
		List<SearchModRulesParser.Layer> layers = new ArrayList<>();
		for (Map.Entry<String, String> f : files.entrySet()) {
			SearchModLocales locales = SearchModLocaleRules.BASE_FILE.equals(f.getKey()) ? new SearchModLocales() : null;
			check(new SearchModRulesValidator(f.getKey(), locales), f.getValue());
			try {
				layers.add(new SearchModRulesParser(f.getKey(), locales).parse(
						new ByteArrayInputStream(f.getValue().getBytes(StandardCharsets.UTF_8))));
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		SearchModRulesValidator validator = new SearchModRulesValidator("", null);
		validator.checkLayers(layers);
		validator.checkConsistency(new SearchModLocaleRules(locale, layers), locale);
	}

	private String resource(String file) throws IOException {
		try (InputStream in = SearchModRules.class.getResourceAsStream(file)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
