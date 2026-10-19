package net.osmand.search.core.spatial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.search.rules.SearchModRules;
import net.osmand.util.Algorithms;

/**
 * POI category matching of the words of a query, https://github.com/osmandapp/OsmAnd/issues/25941.
 * No maps are loaded: only the categories each query word resolves to are checked.
 */
public class SpatialPoiCategorySynonymsTest {

	private static final File EN_PHRASES = new File("src/test/resources/phrases/en/phrases.xml");
	private static final LatLon BRATISLAVA = new LatLon(48.1486, 17.1077);
	private static final SearchModRules RULES = new SearchModRules();

	/**
	 * The phrases of a language name the categories only on the maps of that language, the rules locale of the map
	 * (rules.xml {@code <locales>}), never by the language of the user. The web search indexed the phrases of every
	 * language for every map: "Billa" (brand) listed water taps by the Galician "Billa de auga", "Bank" benches by the
	 * Swedish "Bänk"; the answers for the locales en and de were the same. Live results 2026-10-17 from Berlin: 630 of
	 * 640 objects for "Billa" are water taps, 523 of 770 for "Paris" are bookmakers.
	 */
	@Test
	public void testPhrasesOfALanguageNameCategoriesOnItsMaps() throws IOException, XmlPullParserException {
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes());

		// Austria_vienna: German phrases, no Galician, Swedish or French ones
		Set<String> de = Set.of("de");
		assertNoCategory(poiSearch, de, "Billa", "billa", "water_tap");
		assertNoCategory(poiSearch, de, "Paris", "paris", "bookmaker");
		assertNoCategory(poiSearch, de, "Bank", "bank", "bench");
		assertHasCategory(poiSearch, de, "Bank", "bank", "bank");
		assertHasCategory(poiSearch, de, "Kino", "kino", "cinema");
		assertHasCategory(poiSearch, de, "Sitzbank", "sitzbank", "bench");
		// a map of another language has no German names
		assertNoCategory(poiSearch, Set.of("fr"), "Kino", "kino", "cinema");
		assertNoCategory(poiSearch, Set.of(), "Kino", "kino", "cinema");

		// a map in Galicia: the Galician phrase is a water tap, its word "Billa" is not
		assertHasCategory(poiSearch, Set.of("gl"), "Billa de auga", "billa", "water_tap");
		assertHasCategory(poiSearch, Set.of("gl"), "Billa de auga", "auga", "water_tap");
		assertNoCategory(poiSearch, Set.of("gl"), "Billa", "billa", "water_tap");
	}

	/**
	 * A phrase of the language of the map is matched whole, every word in a row: on a French map "Paris" is the city
	 * and not a word of "Preneur de paris" (bookmaker). The last word may be still typed.
	 */
	@Test
	public void testPhraseOfTheMapLanguageIsMatchedWhole() throws IOException, XmlPullParserException {
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes());
		Set<String> fr = Set.of("fr");
		assertNoCategory(poiSearch, fr, "Paris", "paris", "bookmaker");
		assertHasCategory(poiSearch, fr, "Preneur de paris", "paris", "bookmaker");
		assertHasCategory(poiSearch, fr, "Preneur de par.", "preneur", "bookmaker");
		// a French synonym that is no word of the English names
		assertHasCategory(poiSearch, fr, "Ciné", "cine", "cinema");
		assertNoCategory(poiSearch, Set.of("de"), "Ciné", "cine", "cinema");
	}

	/** "Kino" (de) names the cinemas of a German map, not those of a French map searched with it. */
	@Test
	public void testCategoryOfALanguageIsTheTypeOfObjectsOfItsMaps() throws IOException, XmlPullParserException {
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes());
		TIntArrayList cinema = new TIntArrayList(new int[] { poiSearch.getByKey("cinema").id });
		SpatialSearchToken kino = tokens(poiSearch, Set.of("de", "fr"), "Kino").get(0);
		Assert.assertTrue(kino.matchPoiCategoryKeys(cinema, "de_AT"));
		Assert.assertFalse(kino.matchPoiCategoryKeys(cinema, "fr_FR"));
		// English names the category for every map
		SpatialSearchToken en = tokens(poiSearch, Set.of("de", "fr"), "Cinema").get(0);
		Assert.assertTrue(en.matchPoiCategoryKeys(cinema, "de_AT"));
		Assert.assertTrue(en.matchPoiCategoryKeys(cinema, "fr_FR"));
	}

	/**
	 * A category named by several words is the whole phrase: "Hardware store" is the hardware category, and "store"
	 * is its word, not one more category or a word of a name. Read alone, "store" matches the generic shop
	 * categories and the village Štore (Slovenia), so the web search for "Bratislava Hardware store" finds 4 shops
	 * instead of the 62 of "Bratislava hardware", and from Bratislava "Hardware store" lists shops named "...Store"
	 * in Budapest and Katowice first.
	 * <p>
	 * "Store" alone stays the generic category.
	 */
	@Test
	public void testCategoryNameOfSeveralWordsIsMatchedAsPhrase() throws IOException, XmlPullParserException {
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes());

		assertCategories(poiSearch, "Hardware store", "hardware", "hardware");
		assertCategories(poiSearch, "Hardware store", "store", "hardware");
		assertCategories(poiSearch, "Liquor store", "liquor", "alcohol");
		assertCategories(poiSearch, "Liquor store", "store", "alcohol");

		assertHasCategory(poiSearch, "Store", "store", "shop");
	}

	/**
	 * Two different names of one category in the query are the category and a name: "Church Friedhofskapelle" on a
	 * German map is a church named "Friedhofskapelle", not the churches twice (that listed every church of
	 * Liechtenstein before the chapels named so). The category stays with the most general name: the translator
	 * before the phrases of the map, the order in phrases.xml in each, whatever the order of the words. Words of one
	 * name ("Hardware store") stay the category together.
	 */
	@Test
	public void testCategoryNamedTwiceIsCategoryAndName() throws IOException, XmlPullParserException {
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes());
		Set<String> de = Set.of("de");
		String church = "religion_christian";
		assertHasCategory(poiSearch, de, "Church Friedhofskapelle", "church", church);
		assertNoCategory(poiSearch, de, "Church Friedhofskapelle", "friedhofskapelle", church);
		assertHasCategory(poiSearch, de, "Friedhofskapelle church", "church", church);
		assertNoCategory(poiSearch, de, "Friedhofskapelle church", "friedhofskapelle", church);
		// both phrases of the map: "Kirche" is listed before "friedhofskapelle"
		assertHasCategory(poiSearch, de, "Friedhofskapelle Kirche", "kirche", church);
		assertNoCategory(poiSearch, de, "Friedhofskapelle Kirche", "friedhofskapelle", church);
		// both of the translator: "church" before "chapel"
		assertHasCategory(poiSearch, Set.of(), "Church Chapel", "church", church);
		assertNoCategory(poiSearch, Set.of(), "Church Chapel", "chapel", church);
		// one word alone, or the same name twice, is the category
		assertHasCategory(poiSearch, de, "Friedhofskapelle", "friedhofskapelle", church);
		assertHasCategory(poiSearch, de, "Kirche kirche", "kirche", church);
		// the words of one name
		assertHasCategory(poiSearch, de, "Hardware store", "hardware", "hardware");
		assertHasCategory(poiSearch, de, "Hardware store", "store", "hardware");
		assertHasCategory(poiSearch, Set.of("fr"), "Preneur de paris", "paris", "bookmaker");
		assertHasCategory(poiSearch, Set.of("fr"), "Preneur de paris", "preneur", "bookmaker");
	}

	private static void assertHasCategory(SpatialPoiSearch poiSearch, Set<String> languages, String query, String word,
			String key) {
		Map<String, Set<String>> res = categoriesByWord(tokens(poiSearch, languages, query), poiSearch);
		Assert.assertTrue("'" + word + "' in '" + query + "' on maps " + languages + " must be " + key + ": " + res,
				res.get(word).contains(key));
	}

	private static void assertNoCategory(SpatialPoiSearch poiSearch, Set<String> languages, String query, String word,
			String key) {
		Map<String, Set<String>> res = categoriesByWord(tokens(poiSearch, languages, query), poiSearch);
		Assert.assertFalse("'" + word + "' in '" + query + "' on maps " + languages + " must not be " + key + ": " + res,
				res.get(word).contains(key));
	}

	/** the words of the query after the categories of the maps of these languages */
	private static List<SpatialSearchToken> tokens(SpatialPoiSearch poiSearch, Set<String> languages, String query) {
		List<SpatialSearchToken> tokens = new ArrayList<>();
		for (String w : query.split(" ")) {
			tokens.add(new SpatialSearchToken(RULES, 2, w.toLowerCase(), w, tokens.size()));
		}
		SpatialSearchContext ctx = new SpatialSearchContext(SpatialTextSearchSettings.defaultSettings(),
				Collections.emptyList(), poiSearch, BRATISLAVA);
		poiSearch.processPoiCategories(ctx, tokens, languages);
		return tokens;
	}

	private static void assertCategories(SpatialPoiSearch poiSearch, String query, String word, String... keys)
			throws IOException {
		Map<String, Set<String>> res = categoriesByWord(poiSearch, query);
		Assert.assertEquals("Categories of '" + word + "' in '" + query + "': " + res,
				new TreeSet<>(Arrays.asList(keys)), res.get(word));
	}

	private static void assertHasCategory(SpatialPoiSearch poiSearch, String query, String word, String key)
			throws IOException {
		Map<String, Set<String>> res = categoriesByWord(poiSearch, query);
		Assert.assertTrue("'" + word + "' in '" + query + "' must be " + key + ": " + res,
				res.get(word).contains(key));
	}

	private static Map<String, Set<String>> categoriesByWord(SpatialPoiSearch poiSearch, String query)
			throws IOException {
		SpatialSearchContext ctx = new SpatialSearchContext(SpatialTextSearchSettings.defaultSettings(),
				Collections.emptyList(), poiSearch, BRATISLAVA);
		SpatialSearchResults res = new SpatialTextSearch().searchAPI(query, ctx);
		return categoriesByWord(res.tokens, poiSearch);
	}

	private static Map<String, Set<String>> categoriesByWord(List<SpatialSearchToken> tokens,
			SpatialPoiSearch poiSearch) {
		Map<String, Set<String>> categories = new LinkedHashMap<>();
		for (SpatialSearchToken t : tokens) {
			Set<String> keys = new TreeSet<>();
			for (int id : t.poiCategoryIds) {
				keys.add(poiSearch.getById(id).getKey());
			}
			categories.put(t.wordNoDot, keys);
		}
		return categories;
	}

	private static MapPoiTypes poiTypes() throws IOException, XmlPullParserException {
		Map<String, Map<String, String>> languages = new LinkedHashMap<>();
		languages.put("gl", Map.of("poi_water_tap", "Billa de auga"));
		languages.put("fr", Map.of("poi_bookmaker", "Preneur de paris", "poi_cinema", "Cinéma;ciné"));
		languages.put("sv", Map.of("poi_bench", "Bänk"));
		languages.put("nl", Map.of("poi_bench_yes", "Bank: ja"));
		languages.put("de", Map.of("poi_cinema", "Kino;Filmtheater", "poi_bench", "Sitzbank;Parkbank",
				"poi_bank", "Bank;bankstelle", "poi_religion_christian", "Christentum;Kirche;kapelle;friedhofskapelle"));
		MapPoiTypes types = new MapPoiTypes(null);
		types.init();
		types.setPoiTranslator(new LanguagesTranslator(Algorithms.parseStringsXml(EN_PHRASES), languages));
		return types;
	}

	/**
	 * English as the translation and synonyms, as OsmAndServer MapPoiTypesTranslator gives them, and the phrases of
	 * each language apart. Phrases are looked up by the key of the type, as the app does (the server looks them up
	 * by the icon key, so "poi_shop_hardware" is missing and "Hardware store" is never indexed there).
	 */
	private static class LanguagesTranslator implements PoiTranslator {

		private final Map<String, Set<String>> phrases = new LinkedHashMap<>();
		private final Map<String, Map<String, String>> languages;
		private final Map<String, String> en;

		LanguagesTranslator(Map<String, String> en, Map<String, Map<String, String>> languages) {
			this.en = en;
			this.languages = languages;
			for (Map.Entry<String, String> e : en.entrySet()) {
				phrases.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>())
						.addAll(Arrays.asList(e.getValue().split(";")));
			}
		}

		@Override
		public String getPhrases(AbstractPoiType type, String lang) {
			return languages.getOrDefault(lang, Map.of()).get("poi_" + type.getFormattedKeyName());
		}

		@Override
		public String getTranslation(AbstractPoiType type) {
			return getTranslation(type.getFormattedKeyName());
		}

		@Override
		public String getTranslation(String keyName) {
			Set<String> set = phrases.get("poi_" + keyName);
			return set == null ? null : set.iterator().next();
		}

		@Override
		public String getEnTranslation(AbstractPoiType type) {
			return getEnTranslation(type.getFormattedKeyName());
		}

		@Override
		public String getEnTranslation(String keyName) {
			String phrase = en.get("poi_" + keyName);
			return phrase == null ? null : phrase.split(";")[0];
		}

		@Override
		public String getSynonyms(AbstractPoiType type) {
			return getSynonyms(type.getFormattedKeyName());
		}

		@Override
		public String getSynonyms(String keyName) {
			Set<String> set = phrases.get("poi_" + keyName);
			if (set == null) {
				return null;
			}
			List<String> all = new ArrayList<>(set);
			return String.join(";", all.subList(1, all.size()));
		}

		@Override
		public String getAllLanguagesTranslationSuffix() {
			return "all languages";
		}
	}
}
