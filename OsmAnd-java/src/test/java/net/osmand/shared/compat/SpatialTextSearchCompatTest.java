package net.osmand.shared.compat;

import static net.osmand.shared.compat.CommonWordsCompatTest.hex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hexList;
import static net.osmand.shared.compat.SpatialSearchTokenCompatTest.bits;
import static org.junit.Assert.assertTrue;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.map.WorldRegion;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.search.core.spatial.SpatialPoiSearch;
import net.osmand.search.core.spatial.SpatialPoiSearch.SpatialPoiType;
import net.osmand.search.core.spatial.SpatialSearchContext;
import net.osmand.search.core.spatial.SpatialSearchResult;
import net.osmand.search.core.spatial.SpatialTextSearch;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.search.core.spatial.SpatialTextSearchAccess;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * The spatial text search, {@code SpatialTextSearch}, over real maps: java writes what it answers to
 * {@code build/spatial-search-java.txt}, and {@code SpatialTextSearchTest} of the commonTest of
 * OsmAnd-shared asks the copy the same on the jvm and on the iOS simulator. The copy keeps the
 * package-private part of the search internal, out of reach of this test.
 * <p>
 * The maps of the search cases of the tests are of 2020, without the names in their name index, so
 * the spatial search finds nothing in them but categories. The maps are taken from
 * {@code OSMAND_MAPS_DIR}, {@code ~/osmand/maps} by default, as {@code SpatialSearchPreferencesTest}
 * takes them; without them the test is skipped. Each map is searched from its center with phrases made
 * of its own objects: settlements, streets alone, with a house and with a settlement, the names of the
 * poi around the center alone and with a settlement, the names of their categories alone and with a
 * settlement. Each phrase with the settings of {@link #settings}: by default, without the point,
 * without the score, by the old intersections with and without the skip tree, as a suggestion and for
 * the poi categories only; and by default with its last word cut short and still being typed, and with
 * a typo in its longest word inside and after the key of the name index. Then the first phrases of every
 * map over all the maps at once. For each the words as the search left them, the combinations, the
 * suggestion of a typo and every result; last, the amenities of the poi categories the results of a
 * map name, read by {@code searchPoiByCategory} around the center. The poi types of the search, by id, as
 * they are made at first, after the names of a map's languages are added and after the maps add the values of
 * their top index.
 * <p>
 * Before the phrases of a map the search adds the names of the poi categories in English and in the language
 * of the region of its center, as {@code SpatialTextSearchAPI} does for the language of the app and of the
 * search location, from {@code OsmAnd/res/values-<lang>/phrases.xml}; the phrases of a map have the names of
 * the categories of its poi in that language too.
 * <p>
 * Each query and each category is searched by a new {@code SpatialTextSearch}: java gives the blocks of the
 * name index a word read for an earlier query in the order of a hash set ({@code NameIndexReader.getMatchedPrefixes}),
 * the copy in the order of the file.
 * <p>
 * Behind {@code OSMAND_SEARCH_COMPAT}, see {@link SearchCompat}:
 * <pre>
 * OSMAND_SEARCH_COMPAT=1 OSMAND_MAPS_DIR=&lt;dir of obf&gt; ./gradlew :OsmAnd-java:test --tests "*SpatialTextSearchCompatTest" --rerun -i
 * </pre>
 */
public class SpatialTextSearchCompatTest {

	private static final File JAVA_DUMP = new File("build/spatial-search-java.txt");
	static final int VARIANTS = 7;
	static final int CATEGORY_ZOOM = 15;
	static final double CATEGORY_BOX = 0.05;
	static final int CATEGORY_LIMIT = 100;
	private static final int CATEGORIES_PER_MAP = 4;
	private static final int CITIES = 6;
	private static final int STREETS_PER_CITY = 2;
	private static final int POIS = 8;
	static final int POI_BOX = 1 << 17;
	private static final int ALL_MAPS_PHRASES = 10;

	private Writer dump;
	private final Map<String, Map<String, String>> langPhrases = new HashMap<>();
	private final Set<String> namesOfLanguages = new HashSet<>();
	private int queries;
	private int results;
	private int failures;
	private int typos;
	private int categoryResults;
	private int otherLanguageResults;
	private int oldResults;
	private int categories;
	private int amenities;

	/**
	 * The settings of a variant: 0 by default, 1 the same without the point, 2 without the score, 3 by the
	 * old intersections, 4 by them without the skip tree, 5 as a suggestion, 6 for the poi categories only.
	 */
	static SpatialTextSearchSettings settings(int variant) {
		SpatialTextSearchSettings s = variant == 5 ? SpatialTextSearchSettings.suggestionSettings()
				: variant == 6 ? SpatialTextSearchSettings.searchPoiCategoriesSettings(CATEGORY_ZOOM, null)
				: SpatialTextSearchSettings.defaultSettings();
		if (variant == 2) {
			s.SCORE_RANKING = false;
		}
		if (variant == 3 || variant == 4) {
			s.DEV_USE_PIPELINE = false;
		}
		if (variant == 4) {
			s.DEV_USE_SKIP_HASH_TREE = false;
		}
		return s;
	}

	@Test
	public void searchAnswersTheSame() throws Exception {
		SearchCompat.assumeEnabled();
		List<File> maps = maps();
		Assume.assumeFalse("no maps - set OSMAND_MAPS_DIR=<dir with .obf files>", maps.isEmpty());
		JAVA_DUMP.getParentFile().mkdirs();
		File dir = SearchRulesCompatTest.rulesDir("spatial-search-rules");
		SearchApisCompatTest.setUpTypes();
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator(SearchApisCompatTest.poiPhrases,
				langPhrases));
		try {
			SpatialPoiSearch poiSearch = new SpatialPoiSearch(MapPoiTypes.getDefault());
			File part = new File(JAVA_DUMP.getPath() + ".part");
			try (Writer w = Files.newBufferedWriter(part.toPath(), StandardCharsets.UTF_8)) {
				dump = w;
				write("D", hex(dir.getAbsolutePath()));
				for (Map.Entry<String, String> e : SearchApisCompatTest.poiPhrases.entrySet()) {
					write("X", hex(e.getKey()), hex(e.getValue()));
				}
				List<String> firstPhrases = new ArrayList<>();
				write("P", SpatialTextSearchAccess.poiTypes(poiSearch));
				for (File map : maps) {
					firstPhrases.addAll(runMap(poiSearch, List.of(map), null));
					write("P", SpatialTextSearchAccess.poiTypes(poiSearch));
				}
				runMap(poiSearch, maps, firstPhrases);
				dump = null;
			}
			Files.move(part.toPath(), JAVA_DUMP.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			// the names of the poi types were this test's
			MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
			net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(
					new net.osmand.shared.osm.MapPoiTypes("src/test/resources/poi_types.xml"));
		}
		System.out.printf("SpatialTextSearchCompatTest: %d maps, %d queries (%d failed), %d results (%d of poi categories, "
						+ "%d matched by a name of another language, %d by the old intersections), %d suggestions of a typo; "
						+ "%d categories read, %d amenities; languages %s%n",
				maps.size(), queries, failures, results, categoryResults, otherLanguageResults, oldResults, typos, categories,
				amenities, langPhrases.keySet());
		assertTrue("queries", queries > 1000);
		assertTrue("results", results > 10000);
		assertTrue("results of poi categories", categoryResults > 100);
		assertTrue("results of poi categories by a name of another language", otherLanguageResults > 10);
		assertTrue("results by the old intersections", oldResults > 1000);
		assertTrue("suggestions of a typo", typos > 20);
		assertTrue("categories read", categories > 10);
		assertTrue("amenities of categories", amenities > 100);
	}

	/** The maps of {@code OSMAND_MAPS_DIR}, {@code ~/osmand/maps} by default, by name. */
	static List<File> maps() {
		String dir = System.getenv("OSMAND_MAPS_DIR");
		if (dir == null || dir.isEmpty()) {
			dir = System.getProperty("user.home") + "/osmand/maps";
		}
		File[] files = new File(dir).listFiles((d, name) -> name.endsWith(".obf"));
		if (files == null) {
			return List.of();
		}
		Arrays.sort(files);
		return Arrays.asList(files);
	}

	/**
	 * Searches [maps] from the center of the first of them: with [phrases], or with the phrases made of the
	 * objects of the map. Returns the first of the phrases it made.
	 */
	private List<String> runMap(SpatialPoiSearch poiSearch, List<File> maps, List<String> phrases) throws Exception {
		List<String> paths = new ArrayList<>();
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		try {
			for (File f : maps) {
				paths.add(f.getAbsolutePath());
				readers.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
			}
			LatLon location = readers.get(0).getRegionCenter();
			if (location == null) {
				location = new LatLon(0, 0);
			}
			PoiTranslator lang = null;
			if (phrases == null) {
				language(poiSearch, "en");
				String regionLang = regionLang(location);
				if (regionLang != null) {
					language(poiSearch, regionLang);
					lang = MapPoiTypes.getDefault().getPoiTranslator().getTranslator(regionLang);
				}
				write("P", SpatialTextSearchAccess.poiTypes(poiSearch));
			}
			List<String> own = phrases == null ? phrases(readers.get(0), location, lang) : phrases;
			write("C", hexList(paths), bits(location.getLatitude()), bits(location.getLongitude()));
			Set<String> keys = new TreeSet<>();
			for (String phrase : own) {
				for (int v = 0; v < (phrases == null ? VARIANTS : 2); v++) {
					query(poiSearch, readers, location, v, phrase, keys);
				}
				if (phrases == null) {
					for (String other : new String[] {incomplete(phrase), typo(phrase, true), typo(phrase, false)}) {
						if (other != null) {
							query(poiSearch, readers, location, 0, other, keys);
						}
					}
				}
			}
			int n = 0;
			for (String key : keys) {
				if (n++ >= CATEGORIES_PER_MAP) {
					break;
				}
				category(poiSearch, readers, location, key);
			}
			return own.subList(0, Math.min(ALL_MAPS_PHRASES, own.size()));
		} finally {
			for (BinaryMapIndexReader r : readers) {
				r.close();
			}
		}
	}

	/**
	 * Adds the names of the poi categories in [lang] to the search, as {@code SpatialTextSearchAPI} does: writes the
	 * phrases of the language the first time, from {@code OsmAnd/res/values-<lang>/phrases.xml}; without them the
	 * search adds nothing.
	 */
	private void language(SpatialPoiSearch poiSearch, String lang) throws Exception {
		if (!langPhrases.containsKey(lang)) {
			File file = new File("../OsmAnd/res/values" + ("en".equals(lang) ? "" : "-" + lang) + "/phrases.xml");
			if (file.exists()) {
				Map<String, String> poi = new HashMap<>();
				for (Map.Entry<String, String> e : Algorithms.parseStringsXml(file).entrySet()) {
					if (e.getKey().startsWith("poi_")) {
						poi.put(e.getKey(), e.getValue());
						write("Y", lang, hex(e.getKey()), hex(e.getValue()));
					}
				}
				langPhrases.put(lang, poi);
			}
		}
		write("L", lang);
		Set<String> before = SpatialTextSearchAccess.names(poiSearch);
		poiSearch.addLanguage(lang);
		Set<String> added = SpatialTextSearchAccess.names(poiSearch);
		added.removeAll(before);
		namesOfLanguages.addAll(added);
	}

	/** The first language of the region of [location], as {@code SearchSettings} finds it for the search. */
	static String regionLang(LatLon location) throws IOException {
		Map.Entry<WorldRegion, BinaryMapDataObject> e = PlatformUtil.getOsmandRegions().getSmallestBinaryMapDataObjectAt(location);
		String lang = e == null ? null : e.getKey().getParams().getRegionLang();
		return Algorithms.isEmpty(lang) ? null : lang.split(",")[0];
	}

	/**
	 * Phrases of the objects of a map: settlements, streets alone, with a house and with a settlement, the
	 * names of the poi around [center] alone and with a settlement, the names of their categories, also in
	 * [lang] when there is one.
	 */
	static List<String> phrases(BinaryMapIndexReader r, LatLon center, PoiTranslator lang) throws IOException {
		Random random = new Random(26);
		Set<String> res = new LinkedHashSet<>();
		List<City> cities = new ArrayList<>(r.getCities(null, CityBlocks.CITY_TOWN_TYPE));
		cities.addAll(r.getCities(null, CityBlocks.VILLAGES_TYPE));
		List<City> sample = sample(cities, CITIES, random);
		for (City c : sample) {
			res.add(c.getName());
			r.preloadStreets(c, null, null);
			for (Street s : sample(new ArrayList<>(c.getStreets()), STREETS_PER_CITY, random)) {
				res.add(s.getName());
				r.preloadBuildings(s, null, null);
				List<Building> blds = s.getBuildings();
				if (blds.isEmpty()) {
					res.add(s.getName() + " " + c.getName());
				} else {
					Building b = blds.get(random.nextInt(blds.size()));
					res.add(s.getName() + " " + b.getName());
					res.add(b.getName() + " " + s.getName() + " " + c.getName());
				}
			}
		}
		int x = MapUtils.get31TileNumberX(center.getLongitude());
		int y = MapUtils.get31TileNumberY(center.getLatitude());
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(x - POI_BOX, x + POI_BOX, y - POI_BOX,
				y + POI_BOX, 16, BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null);
		List<Amenity> named = new ArrayList<>();
		for (Amenity a : r.searchPoi(req)) {
			if (!Algorithms.isEmpty(a.getName())) {
				named.add(a);
			}
		}
		String city = sample.isEmpty() ? null : sample.get(0).getName();
		for (Amenity a : sample(named, POIS, random)) {
			res.add(a.getName());
			String kind = a.getSubTypeStr();
			res.add(kind);
			if (city != null) {
				res.add(a.getName() + " " + city);
				res.add(kind + " " + city);
			}
			AbstractPoiType type = lang == null ? null : a.getType().getPoiTypeByKeyName(a.getSubType());
			String translated = type == null ? null : lang.getTranslation(type);
			if (!Algorithms.isEmpty(translated)) {
				String name = translated.split(";")[0];
				res.add(name);
				if (city != null) {
					res.add(name + " " + city);
				}
			}
		}
		res.removeIf(String::isBlank);
		return new ArrayList<>(res);
	}

	private static <T> List<T> sample(List<T> values, int count, Random random) {
		List<T> res = new ArrayList<>();
		for (int n = 0; n < count && !values.isEmpty(); n++) {
			res.add(values.get(random.nextInt(values.size())));
		}
		return res;
	}

	private void query(SpatialPoiSearch poiSearch, List<BinaryMapIndexReader> readers, LatLon location, int variant,
			String phrase, Set<String> keys) throws Exception {
		SpatialSearchContext ctx = new SpatialSearchContext(settings(variant), readers, poiSearch,
				variant == 1 ? null : location);
		ctx.getStats().printLogs = false;
		SpatialTextSearch search = new SpatialTextSearch();
		write("Q", String.valueOf(variant), hex(phrase));
		queries++;
		SpatialSearchResults res;
		try {
			res = search.searchAPI(phrase, ctx);
		} catch (RuntimeException e) {
			failures++;
			write("A", "!", e.getClass().getSimpleName());
			return;
		}
		write("A", hex(res.input), res.typoSuggestion == null ? "-" : hex(res.typoSuggestion),
				SpatialTextSearchAccess.tokens(res.tokens), SpatialTextSearchAccess.combinations(res.combinations),
				SpatialTextSearchAccess.stats(res.stats), String.valueOf(res.mainResults.size()));
		typos += res.typoSuggestion == null ? 0 : 1;
		for (int i = 0; i < res.mainResults.size(); i++) {
			SpatialSearchResult r = res.mainResults.get(i);
			write("R", String.valueOf(i), SpatialTextSearchAccess.result(r, ctx));
			results++;
			oldResults += variant == 3 || variant == 4 ? 1 : 0;
			SpatialPoiType category = r.getPoiCategory(poiSearch);
			if (category != null) {
				categoryResults++;
				if (namesOfLanguages.contains(r.getPoiCategoryMatchedName(poiSearch))) {
					otherLanguageResults++;
				}
				if (variant == 0) {
					keys.add(category.getKey());
				}
			}
		}
	}

	private void category(SpatialPoiSearch poiSearch, List<BinaryMapIndexReader> readers, LatLon location, String key)
			throws Exception {
		QuadRect box = new QuadRect(location.getLongitude() - CATEGORY_BOX, location.getLatitude() + CATEGORY_BOX,
				location.getLongitude() + CATEGORY_BOX, location.getLatitude() - CATEGORY_BOX);
		SpatialSearchContext ctx = new SpatialSearchContext(
				SpatialTextSearchSettings.searchPoiByCategorySettings(CATEGORY_ZOOM, box), readers, poiSearch, location);
		ctx.getStats().printLogs = false;
		write("K", hex(key));
		categories++;
		List<Amenity> list = new SpatialTextSearch().searchPoiByCategory(ctx, key, box, CATEGORY_ZOOM, CATEGORY_LIMIT);
		StringBuilder sb = new StringBuilder();
		for (Amenity a : list) {
			sb.append(SpatialTextSearchAccess.object(a)).append(',').append(hex(a.getSubType())).append(',')
					.append(SpatialTextSearchAccess.latLon(a.getLocation())).append(';');
		}
		amenities += list.size();
		write("B", String.valueOf(list.size()), sb.toString());
	}

	/** The phrase with its last word cut short and still being typed, null when the word is too short. */
	static String incomplete(String phrase) {
		int space = phrase.lastIndexOf(' ');
		String last = phrase.substring(space + 1);
		if (last.length() < 4) {
			return null;
		}
		return phrase.substring(0, space + 1) + last.substring(0, last.length() / 2 + 1) + ".";
	}

	/**
	 * The phrase with two letters of its longest word swapped: the second and the third, inside the key of
	 * the name index, or the last two, after it. Null when the word is shorter than 5 letters.
	 */
	static String typo(String phrase, boolean inKey) {
		String[] words = phrase.split(" ");
		int longest = 0;
		for (int i = 1; i < words.length; i++) {
			if (words[i].length() > words[longest].length()) {
				longest = i;
			}
		}
		String w = words[longest];
		if (w.length() < 5 || !w.chars().allMatch(Character::isLetter)) {
			return null;
		}
		int i = inKey ? 1 : w.length() - 2;
		words[longest] = w.substring(0, i) + w.charAt(i + 1) + w.charAt(i) + w.substring(i + 2);
		return String.join(" ", words);
	}

	private void write(String kind, String... fields) throws Exception {
		dump.write(kind);
		for (String f : fields) {
			dump.write('\t');
			dump.write(f);
		}
		dump.write('\n');
	}
}
