package net.osmand.shared.compat;

import static net.osmand.shared.compat.CommonWordsCompatTest.hex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hexList;
import static net.osmand.shared.compat.SpatialSearchTokenCompatTest.bits;
import static org.junit.Assert.assertTrue;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.search.core.spatial.SpatialPoiSearch;
import net.osmand.search.core.spatial.SpatialSearchContext;
import net.osmand.search.core.spatial.SpatialSearchResult;
import net.osmand.search.core.spatial.SpatialTextSearchAPI;
import net.osmand.search.core.spatial.SpatialTextSearchAccess;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The spatial text search as the search of the app runs it, {@code SearchUICore.init(true)} with
 * {@code SpatialTextSearchAPI}: java writes what it answers to {@code build/spatial-search-api-java.txt}, and
 * {@code SpatialTextSearchAPITest} of the commonTest of OsmAnd-shared asks the copy the same on the jvm and on the
 * iOS simulator.
 * <p>
 * The maps of {@code OSMAND_MAPS_DIR}, as {@link SpatialTextSearchCompatTest} takes them, are all the maps of the
 * search, with {@code regions.ocbf}, and each is searched from its center with the phrases that test makes of its
 * objects and with a part of an object ({@link #extraNamePhrases}), by an English search: each phrase complete, ending
 * with a space, and still being typed; complete with its last word cut short, and with a typo inside and after the key
 * of the name index, as that test makes them; complete by a search in the language of the region. Each query by a
 * new {@code SearchUICore}: java gives the blocks of the name index a word read for an earlier query in the order of
 * a hash set. For each the rows of the result: the spatial result each stands for, as that test writes it, and what
 * the api made of it; the others, as the suggestion of a typo. Then the first poi categories and streets of a map the
 * complete phrases found, each selected in its query and searched as the app searches a row tapped, alone and with a
 * name or a house number typed after it. Each complete phrase is also searched with the types of the address tab and
 * with those of the address search of the aidl api, which the api filters its results by.
 * <p>
 * Behind {@code OSMAND_SEARCH_COMPAT}, see {@link SearchCompat}:
 * <pre>
 * OSMAND_SEARCH_COMPAT=1 OSMAND_MAPS_DIR=&lt;dir of obf&gt; ./gradlew :OsmAnd-java:test --tests "*SpatialTextSearchAPICompatTest" --rerun -i
 * </pre>
 */
public class SpatialTextSearchAPICompatTest {

	private static final File JAVA_DUMP = new File("build/spatial-search-api-java.txt");
	private static final int SELECTIONS_PER_MAP = 4;
	private static final int EXTRA_NAME_PHRASES = 12;
	private static final int EXTRA_NAME_CITIES = 60;
	// "search further" twice: 60 km of the 10 of a category
	private static final int WIDE_RADIUS_LEVEL = 3;
	// the types of the address tab and of the address search of the aidl api
	private static final ObjectType[][] SEARCH_TYPES = {
			{ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE, ObjectType.HOUSE, ObjectType.STREET_INTERSECTION,
					ObjectType.STREET, ObjectType.LOCATION, ObjectType.PARTIAL_LOCATION},
			{ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE, ObjectType.STREET, ObjectType.HOUSE,
					ObjectType.STREET_INTERSECTION}};

	private Writer dump;
	private final Map<String, Map<String, String>> langPhrases = new HashMap<>();
	private final Set<String> languages = new HashSet<>();
	// the names the languages add to the poi categories
	private final Set<String> namesOfLanguages = new HashSet<>();
	private SpatialPoiSearch names;
	private int queries;
	private int typedQueries;
	private int rows;
	private int spatialRows;
	private int categoryRows;
	private int typos;
	private int selected;
	private int selectedRows;
	private int extraNames;

	@Test
	public void searchAnswersTheSame() throws Exception {
		SearchCompat.assumeEnabled();
		List<File> maps = SpatialTextSearchCompatTest.maps();
		Assume.assumeFalse("no maps - set OSMAND_MAPS_DIR=<dir with .obf files>", maps.isEmpty());
		JAVA_DUMP.getParentFile().mkdirs();
		File dir = SearchRulesCompatTest.rulesDir("spatial-search-api-rules");
		SearchApisCompatTest.setUpTypes();
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator(SearchApisCompatTest.poiPhrases,
				langPhrases));
		names = new SpatialPoiSearch(MapPoiTypes.getDefault());
		OsmandRegions regions = PlatformUtil.getOsmandRegions();
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		try {
			File part = new File(JAVA_DUMP.getPath() + ".part");
			try (Writer w = Files.newBufferedWriter(part.toPath(), StandardCharsets.UTF_8)) {
				dump = w;
				write("D", hex(dir.getAbsolutePath()));
				write("G", hex(regions.getReader().getFile().getAbsolutePath()));
				for (Map.Entry<String, String> e : SearchApisCompatTest.poiPhrases.entrySet()) {
					write("X", hex(e.getKey()), hex(e.getValue()));
				}
				List<String> paths = new ArrayList<>();
				for (File f : maps) {
					paths.add(f.getAbsolutePath());
					readers.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
				}
				write("M", hexList(paths));
				for (BinaryMapIndexReader map : readers) {
					runMap(readers, regions, map);
				}
				dump = null;
			}
			Files.move(part.toPath(), JAVA_DUMP.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			for (BinaryMapIndexReader r : readers) {
				r.close();
			}
			// the names of the poi types were this test's
			MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
			net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(
					new net.osmand.shared.osm.MapPoiTypes("src/test/resources/poi_types.xml"));
		}
		System.out.printf("SpatialTextSearchAPICompatTest: %d maps, %d queries (%d with the types of a search), %d rows (%d "
						+ "of the spatial search, %d of poi categories, %d with a part of an object), %d suggestions of a "
						+ "typo; %d categories and streets selected, %d rows; languages %s%n",
				maps.size(), queries, typedQueries, rows, spatialRows, categoryRows, extraNames, typos, selected,
				selectedRows, languages);
		assertTrue("queries", queries > 1000);
		assertTrue("rows of the spatial search", spatialRows > 10000);
		assertTrue("rows of poi categories", categoryRows > 100);
		assertTrue("suggestions of a typo", typos > 10);
		assertTrue("rows with a part of an object", extraNames > 10);
		assertTrue("categories and streets selected", selected > 20);
		assertTrue("rows of the categories and streets selected", selectedRows > 100);
	}

	/**
	 * Searches from the center of [map]: its phrases, then the first poi categories and streets they found selected, and
	 * the commonest kind of poi around the center searched further.
	 */
	private void runMap(List<BinaryMapIndexReader> readers, OsmandRegions regions, BinaryMapIndexReader map)
			throws Exception {
		LatLon center = map.getRegionCenter();
		if (center == null) {
			center = new LatLon(0, 0);
		}
		// the languages the api adds: of the search and of the region of the center
		language("en");
		String regionLang = SpatialTextSearchCompatTest.regionLang(center);
		PoiTranslator lang = null;
		if (regionLang != null) {
			language(regionLang);
			lang = MapPoiTypes.getDefault().getPoiTranslator().getTranslator(regionLang);
		}
		List<Amenity> around = poisAround(map, center);
		Set<String> phrases = new LinkedHashSet<>(SpatialTextSearchCompatTest.phrases(map, center, lang));
		phrases.addAll(extraNamePhrases(map, around));
		write("C", bits(center.getLatitude()), bits(center.getLongitude()));
		// the categories and the streets found, by the object of their row, and the query that found each first
		Map<String, String> categories = new TreeMap<>();
		Map<String, String> streets = new TreeMap<>();
		for (String phrase : phrases) {
			query(readers, regions, center, "en", null, phrase + " ", categories, streets);
			query(readers, regions, center, "en", null, phrase, null, null);
			for (String other : new String[] {SpatialTextSearchCompatTest.incomplete(phrase),
					SpatialTextSearchCompatTest.typo(phrase, true), SpatialTextSearchCompatTest.typo(phrase, false)}) {
				if (other != null) {
					query(readers, regions, center, "en", null, other + " ", null, null);
				}
			}
			if (regionLang != null && !regionLang.equals("en")) {
				query(readers, regions, center, regionLang, null, phrase + " ", null, null);
			}
			for (ObjectType[] types : SEARCH_TYPES) {
				query(readers, regions, center, "en", types, phrase + " ", null, null);
			}
		}
		// a category tapped and a name typed after it, a street tapped and a house number typed after it
		select(readers, regions, center, categories, "a ");
		select(readers, regions, center, streets, "1 ");
		// the commonest kind of poi searched further: more objects of it than the search reads of a phrase
		String kind = commonestKind(around);
		if (kind != null) {
			AbstractPoiType type = MapPoiTypes.getDefault().getAnyPoiTypeByKey(kind);
			String name = type == null ? null : type.getTranslation();
			if (!Algorithms.isEmpty(name)) {
				select(readers, regions, center, name + " ", object(type), "a ", WIDE_RADIUS_LEVEL);
			}
		}
	}

	/** The pois of the box around [center] the phrases of a map are made of. */
	static List<Amenity> poisAround(BinaryMapIndexReader r, LatLon center) throws IOException {
		int x = MapUtils.get31TileNumberX(center.getLongitude());
		int y = MapUtils.get31TileNumberY(center.getLatitude());
		int box = SpatialTextSearchCompatTest.POI_BOX;
		return r.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(x - box, x + box, y - box, y + box, 16,
				BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null));
	}

	/** The kind of most of [pois]. */
	static String commonestKind(List<Amenity> pois) {
		Map<String, Integer> counts = new TreeMap<>();
		for (Amenity a : pois) {
			counts.merge(a.getSubType(), 1, Integer::sum);
		}
		String kind = null;
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			if (kind == null || e.getValue() > counts.get(kind)) {
				kind = e.getKey();
			}
		}
		return kind;
	}

	/**
	 * Phrases the spatial search answers with a part of an object: a house number inside an interpolation of houses of
	 * a street, a poi of [pois] with its ref, a poi with the number of its name after the words of it.
	 */
	static List<String> extraNamePhrases(BinaryMapIndexReader r, List<Amenity> pois) throws IOException {
		Set<String> res = new LinkedHashSet<>();
		List<City> cities = new ArrayList<>(r.getCities(null, CityBlocks.CITY_TOWN_TYPE));
		cities.addAll(r.getCities(null, CityBlocks.VILLAGES_TYPE));
		int interpolations = 0;
		for (City c : cities.subList(0, Math.min(EXTRA_NAME_CITIES, cities.size()))) {
			if (interpolations >= EXTRA_NAME_PHRASES) {
				break;
			}
			r.preloadStreets(c, null, null);
			for (Street s : c.getStreets()) {
				if (interpolations >= EXTRA_NAME_PHRASES) {
					break;
				}
				r.preloadBuildings(s, null, null);
				Set<String> houses = new HashSet<>();
				for (Building b : s.getBuildings()) {
					houses.add(b.getName());
				}
				for (Building b : s.getBuildings()) {
					String hno = b.isInterpolation() ? b.getInterpolationName(0.5) : "";
					if (!hno.isEmpty() && b.belongsToInterpolation(hno) && !houses.contains(hno)) {
						res.add(s.getName() + " " + hno);
						interpolations++;
						break;
					}
				}
			}
		}
		int refs = 0;
		int numbers = 0;
		for (Amenity a : pois) {
			String name = a.getName();
			if (Algorithms.isEmpty(name)) {
				continue;
			}
			String ref = a.getRef();
			if (!Algorithms.isEmpty(ref) && refs < EXTRA_NAME_PHRASES && res.add(name + " " + ref)) {
				refs++;
			}
			int num = Algorithms.extractFirstIntegerNumber(name);
			String words = name.replaceAll("[^\\p{L} ]", " ").replaceAll(" +", " ").trim();
			if (num > 0 && !words.isEmpty() && numbers < EXTRA_NAME_PHRASES && res.add(words + " " + num)) {
				numbers++;
			}
		}
		return new ArrayList<>(res);
	}

	/**
	 * Writes the phrases of [lang] the first time, from {@code OsmAnd/res/values-<lang>/phrases.xml}, which the
	 * translator gives the api for the language, and keeps the names it adds to the poi categories.
	 */
	private void language(String lang) throws Exception {
		if (!languages.add(lang)) {
			return;
		}
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
		Set<String> before = SpatialTextSearchAccess.names(names);
		names.addLanguage(lang);
		Set<String> added = SpatialTextSearchAccess.names(names);
		added.removeAll(before);
		namesOfLanguages.addAll(added);
	}

	private static SearchUICore core(List<BinaryMapIndexReader> readers, OsmandRegions regions, String lang) {
		SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), lang, false);
		core.init(true);
		core.getSearchSettings().setOfflineIndexes(readers);
		core.getSearchSettings().setRegions(regions);
		return core;
	}

	// a query of the search in [lang], with [types] the types of a search or null; "T" lines are the ones with types
	private void query(List<BinaryMapIndexReader> readers, OsmandRegions regions, LatLon center, String lang,
			ObjectType[] types, String text, Map<String, String> categories, Map<String, String> streets) throws Exception {
		SearchUICore core = core(readers, regions, lang);
		if (types == null) {
			write("Q", hex(text), lang);
		} else {
			StringBuilder names = new StringBuilder();
			for (ObjectType type : types) {
				names.append(names.length() == 0 ? "" : ",").append(type.name());
			}
			write("T", hex(text), lang, names.toString());
			core.updateSettings(core.getSearchSettings().setSearchTypes(types));
			typedQueries++;
		}
		queries++;
		SearchResultCollection c = core.immediateSearch(text, center);
		SpatialSearchContext ctx = SpatialTextSearchAccess.context(core.getApiByClass(SpatialTextSearchAPI.class));
		List<SearchResult> rs = c.getCurrentSearchResults();
		List<SpatialSearchResult> spatial = new ArrayList<>();
		StringBuilder others = new StringBuilder();
		String typo = "-";
		for (int i = 0; i < rs.size(); i++) {
			SearchResult r = rs.get(i);
			if (r.spatialResult != null) {
				spatial.add(r.spatialResult);
				continue;
			}
			others.append(i).append(':').append(row(r).replace('\t', '|')).append(';');
			if (r.objectType == ObjectType.SUGGESTION && typo.equals("-")) {
				typo = hex(r.localeName);
				typos++;
			}
		}
		write("A", hex(text), typo, SpatialTextSearchAccess.tokensOf(spatial, words(text)), "-", "-",
				String.valueOf(spatial.size()),
				others.length() == 0 ? "-" : others.toString(), String.valueOf(c.isSkipSorting()));
		rows += rs.size();
		int i = 0;
		for (SearchResult r : rs) {
			if (r.spatialResult == null) {
				continue;
			}
			write("R", String.valueOf(i++), SpatialTextSearchAccess.result(r.spatialResult, ctx), row(r),
					united(r.spatialResult));
			spatialRows++;
			extraNames += r.spatialResult.getExtraNameMatch() == null ? 0 : 1;
			if (r.objectType == ObjectType.STREET && streets != null) {
				streets.putIfAbsent(object(r.object), text);
			}
			if (r.objectType == ObjectType.POI_TYPE) {
				categoryRows++;
				String matched = SpatialTextSearchAccess.matchedName(r.spatialResult, ctx);
				// a category matched by a name of a language added the copy may not find: java adds those names in the
				// order of a hash map
				if (categories != null && !namesOfLanguages.contains(matched)) {
					categories.putIfAbsent(object(r.object), text);
				}
			}
		}
	}

	/** The first [rows] found, each by the object of its row with the query that found it, selected. */
	private void select(List<BinaryMapIndexReader> readers, OsmandRegions regions, LatLon center, Map<String, String> rows,
			String typed) throws Exception {
		int n = 0;
		for (Map.Entry<String, String> e : rows.entrySet()) {
			if (n++ >= SELECTIONS_PER_MAP) {
				break;
			}
			select(readers, regions, center, e.getValue(), e.getKey(), typed, 1);
		}
	}

	/**
	 * The row of [key] found by [text] selected in its query and searched, alone and with [typed] after it, in the radius
	 * of [radiusLevel].
	 */
	private void select(List<BinaryMapIndexReader> readers, OsmandRegions regions, LatLon center, String text,
			String key, String typed, int radiusLevel) throws Exception {
		write("K", hex(text), hex(key), hex(typed), String.valueOf(radiusLevel));
		SearchUICore core = core(readers, regions, "en");
		SearchResult row = null;
		for (SearchResult r : core.immediateSearch(text, center).getCurrentSearchResults()) {
			if (key.equals(object(r.object))) {
				row = r;
				break;
			}
		}
		if (row == null) {
			write("S", "-");
			return;
		}
		selected++;
		core.updateSettings(core.getSearchSettings().setRadiusLevel(radiusLevel));
		core.resetPhrase(text);
		core.selectSearchResult(row);
		String selectedText = core.getPhrase().getText(true);
		for (String t : new String[] {selectedText, selectedText + typed}) {
			SearchResultCollection c = core.immediateSearch(t, center);
			List<SearchResult> rs = c.getCurrentSearchResults();
			write("S", hex(t), String.valueOf(c.isSkipSorting()), String.valueOf(rs.size()));
			for (int i = 0; i < rs.size(); i++) {
				write("B", String.valueOf(i), row(rs.get(i)));
			}
			selectedRows += rs.size();
		}
	}

	/** The words the spatial search makes of [text], as {@code SpatialTextSearch.splitWords} splits them. */
	static int words(String text) {
		int n = 0;
		for (String w : SearchAlgorithms.splitAndNormalize(text, false)) {
			n += w.equals(".") ? 0 : 1;
		}
		return n;
	}

	/**
	 * What the search made of a row: its type, names, place, priority, related objects, object, the words it left and
	 * whether its object is a poi in two settlements of one kind.
	 */
	static String row(SearchResult r) {
		return r.objectType + "\t" + hexOrDash(r.localeName) + "\t"
				+ (r.otherNames == null ? "-" : hexList(new ArrayList<>(r.otherNames))) + "\t"
				+ hexOrDash(r.alternateName) + "\t" + hexOrDash(r.addressName) + "\t" + hexOrDash(r.cityName) + "\t"
				+ SpatialTextSearchAccess.latLon(r.location) + "\t" + SpatialTextSearchAccess.number(r.priority) + "\t"
				+ SpatialTextSearchAccess.number(r.priorityDistance) + "\t" + r.preferredZoom + "\t"
				+ (r.file == null ? "-" : hex(r.file.getRegionName())) + "\t" + object(r.relatedObject) + "\t"
				+ hexOrDash(r.localeRelatedObjectName) + "\t" + parent(r.parentSearchResult) + "\t"
				+ r.spatialSearchVisibleLevel + "\t" + object(r.object) + "\t" + hexList(r.filterUnknownSearchWord(null))
				+ "\t" + twoSettlementsOfOneKind(r.object, r.requiredSearchPhrase.getSettings().getLang());
	}

	/**
	 * Whether the settlement of a poi depends on the order of its tag groups: {@code Amenity.getCityFromTagGroups} keeps
	 * the last name in [lang] of each kind of settlement in the order of a hash map.
	 */
	static boolean twoSettlementsOfOneKind(Object o, String lang) {
		if (!(o instanceof Amenity a) || a.getTagGroups() == null) {
			return false;
		}
		Map<CityType, Set<String>> names = new EnumMap<>(CityType.class);
		for (List<TagValuePair> group : a.getTagGroups().values()) {
			String translated = null;
			String nonTranslated = null;
			CityType type = null;
			for (TagValuePair tv : group) {
				if (tv.tag.endsWith("name:" + lang)) {
					translated = tv.value;
				} else if (tv.tag.endsWith("name")) {
					nonTranslated = tv.value;
				} else if (tv.tag.equals("place")) {
					type = CityType.valueFromString(tv.value);
				}
			}
			String name = Algorithms.isEmpty(translated) ? nonTranslated : translated;
			if (!Algorithms.isEmpty(name) && type != null && type.storedAsSeparateAdminEntity()) {
				names.computeIfAbsent(type, k -> new HashSet<>()).add(name);
			}
		}
		for (Set<String> n : names.values()) {
			if (n.size() > 1) {
				return true;
			}
		}
		return false;
	}

	// the objects deduplication united in a result, in the order they were
	private static String united(SpatialSearchResult r) {
		if (r.unitedObject == null) {
			return "-";
		}
		StringBuilder sb = new StringBuilder();
		for (Object o : r.unitedObject.getObjects()) {
			sb.append(object(o)).append(';');
		}
		return sb.toString();
	}

	private static String parent(SearchResult p) {
		return p == null ? "-" : p.objectType + "," + hexOrDash(p.localeName) + "," + object(p.relatedObject) + ","
				+ hexOrDash(p.localeRelatedObjectName);
	}

	static String object(Object o) {
		if (o == null) {
			return "-";
		} else if (o instanceof MapObject m) {
			return SpatialTextSearchAccess.object(m);
		} else if (o instanceof AbstractPoiType t) {
			return "T," + hex(t.getKeyName());
		} else if (o instanceof TopIndexFilter f) {
			return "F," + hex(f.getFilterId());
		} else if (o instanceof BinaryMapIndexReader r) {
			return "O," + hex(r.getRegionName());
		} else if (o instanceof String s) {
			return "S," + hex(s);
		}
		return "?";
	}

	private static String hexOrDash(String s) {
		return s == null ? "-" : hex(s);
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
