package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.bits;
import static net.osmand.shared.compat.SearchPhraseCompatTest.call;
import static net.osmand.shared.compat.SearchPhraseCompatTest.field;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hexList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.CollatorStringMatcher;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The search apis of {@code SearchCoreFactory} in OsmAnd-shared against the originals in
 * OsmAnd-java, each on its own, without the core that sorts and merges what they find.
 *
 * Each of the 60 cases of {@code SearchUICoreTest} is typed in, cut where the apis change what they
 * do, and searched in whole at the first two radius levels, over the obf files of the case, with its
 * settings and the english names of the poi types. Every api answers whether it runs, with which
 * priority and in which radius, and searches; everything it publishes is compared field by field,
 * with the objects behind it and its parents. Then the results are selected, as the ui selects
 * them, and the apis search the phrase that follows, twice over. The names of the poi types are
 * typed in as well, for the two apis of poi types, and coordinates are formatted.
 *
 * It writes the phrases, with what java answered, to {@code build/search-apis-java.txt}, which
 * {@code SearchCoreFactoryTest} in OsmAnd-shared holds the copy to on Kotlin/Native, with the
 * collation keys of the words java compared: all of them but two in three of the cut phrases.
 * {@code OSMAND_SEARCH_APIS_CASE} runs only the cases whose file names start with it, and writes
 * no dump. The obf files of the cases are small and old, without postcodes, poi tag groups or the
 * newer sections; {@code OSMAND_SEARCH_APIS_MAPS} adds a case for each real map it names, separated
 * by {@code :}, made of the names in it:
 * <pre>
 * OSMAND_SEARCH_APIS_MAPS=/maps/Slovakia_europe.obf:/maps/Ukraine_kyiv_europe.obf \
 *   ./gradlew :OsmAnd-java:test --tests "*SearchApisCompatTest" --rerun
 * </pre>
 */
public class SearchApisCompatTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File PHRASES = new File("src/test/resources/phrases/en/phrases.xml");
	private static final File SEARCH_CASES = new File("src/test/resources/search");

	/** Read by {@code SearchCoreFactoryTest} in OsmAnd-shared, from {@code ../OsmAnd-java/build}. */
	private static final File JAVA_DUMP = new File("build/search-apis-java.txt");

	/** The apis in the order {@code SearchUICore.init} adds them, and the one of regions it leaves out. */
	static final String[] APIS = {"SearchAmenityByNameAPI", "SearchLocationAndUrlAPI", "SearchAmenityTypesAPI",
			"SearchAmenityByTypeAPI", "SearchBuildingAndIntersectionsByStreetAPI", "SearchStreetByCityAPI",
			"SearchAddressByNameAPI", "SearchAddressByNameAPI", "SearchRegionByNameAPI"};
	static final int TYPES_API = 2;
	static final int BY_TYPE_API = 3;

	/** Typed in after a case, alone: locations, links and codes the cases do not have. */
	private static final String[] LOCATIONS = {"52.3702, 4.8952", "52.3702 4.8952", "N 52° 22.212′ E 4° 53.712′",
			"-33.8688, 151.2093", "40.76712", "-40.7", "S40.7", "n52", "12.", "4.8952", "1234567890123456",
			"52.123455, 4.000025", "0.015625, 0.046875", "9C3XGV4C+XV", "GV4C+XV", "GV4C+XV New York", "8FVC9G8F+6X",
			"geo:52.3702,4.8952?z=12", "https://www.openstreetmap.org/#map=15/52.3702/4.8952",
			"https://maps.google.com/?q=52.3702,4.8952", "https://goo.gl/maps/abc", "https://osmand.net/map?pin=52.3702,4.8952",
			"geo:0,0?q=Amsterdam", "31U 628000 5804000", "31U FU 28000 04000"};

	private static java.io.Writer out;
	/**
	 * Whether the answers of the phrase being run go into the dump: not those of every typed prefix,
	 * to keep it small. The phrase itself always does: the settlements and streets the apis load
	 * stay with the api of addresses, and get their streets and houses, for the phrases after it.
	 */
	private static boolean dumping = true;
	private static List<String> caseLines;
	private static final long[] resultsByApi = new long[APIS.length];
	private static final long[] charsByApi = new long[APIS.length];
	static final Map<String, String> poiPhrases = new TreeMap<>();
	private static final Map<String, BinaryMapIndexReader> javaReaders = new LinkedHashMap<>();
	private static final Map<String, net.osmand.shared.binary.BinaryMapIndexReader> copyReaders = new LinkedHashMap<>();
	private static final List<Case> cases = new ArrayList<>();
	private static final Random random = new Random(5);
	private static int phrasesRun;
	private static int resultsCompared;
	private static int selections;
	private static int ties;
	private static int otherDuplicates;

	/** A case of {@code SearchUICoreTest}: its settings, obf files and phrases. */
	private static final class Case {
		final String name;
		final String settings;
		final List<String> files;
		final List<String> phrases;

		Case(String name, String settings, List<String> files, List<String> phrases) {
			this.name = name;
			this.settings = settings;
			this.files = files;
			this.phrases = phrases;
		}

		boolean hasPoiTypePhrase() {
			for (String p : phrases) {
				if (p.startsWith("POI_TYPE:")) {
					return true;
				}
			}
			return false;
		}
	}

	/** One side's apis, phrases and the results each api published for each phrase, in order. */
	private static final class Side {
		final List<Object> apis;
		final List<Object> phrases = new ArrayList<>();
		final List<List<List<Object>>> results = new ArrayList<>();

		Side(List<Object> apis) {
			this.apis = apis;
		}
	}

	@BeforeClass
	public static void open() throws Exception {
		assertTrue(REGIONS + " is missing; :OsmAnd-java:processResources downloads it", REGIONS.exists());
		PlatformUtil.setOsmandRegions(new OsmandRegions(REGIONS.getPath()));
		net.osmand.shared.binary.CommonWords.Companion.setOsmandRegions(
				new net.osmand.shared.map.OsmandRegions(REGIONS.getPath()));
		for (Map.Entry<String, String> e : Algorithms.parseStringsXml(PHRASES).entrySet()) {
			if (e.getKey().startsWith("poi_")) {
				poiPhrases.put(e.getKey(), e.getValue());
			}
		}
		assertTrue("poi names in " + PHRASES + ": " + poiPhrases.size(), poiPhrases.size() > 1000);
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		MapPoiTypes.getDefault().setPoiTranslator(new JavaTranslator());
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().setPoiTranslator(new CopyTranslator());

		for (File f : TestObf.searchFiles()) {
			javaReaders.put(f.getName(), new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
			copyReaders.put(f.getName(), new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
		}
		File[] files = SEARCH_CASES.listFiles((dir, name) -> name.endsWith(".json"));
		assertTrue("search cases in " + SEARCH_CASES + "; collectTestResources copies them", files != null && files.length > 50);
		Arrays.sort(files);
		for (File f : files) {
			JSONObject json = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
			List<String> texts = new ArrayList<>();
			if (json.has("phrase")) {
				texts.add(json.getString("phrase"));
			}
			JSONArray arr = json.optJSONArray("phrases");
			for (int i = 0; arr != null && i < arr.length(); i++) {
				texts.add(arr.getString(i));
			}
			List<String> obfs = new ArrayList<>();
			JSONArray named = json.optJSONArray("files");
			if (named != null) {
				for (int i = 0; i < named.length(); i++) {
					obfs.add(named.getString(i).replace(".gz", ""));
				}
			} else {
				obfs.add(f.getName().replace(".json", ".obf"));
			}
			obfs.removeIf(o -> !javaReaders.containsKey(o));
			cases.add(new Case(f.getName(), json.getJSONObject("settings").toString(), obfs, texts));
		}
	}

	@AfterClass
	public static void close() throws IOException {
		// the names of the poi types were this test's
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
		System.out.println("SearchApisCompatTest: " + cases.size() + " cases, " + phrasesRun + " phrases, "
				+ selections + " selections, " + resultsCompared + " results compared, " + ties + " ties of poi types in the dumped phrases, "
				+ otherDuplicates + " searches with the other record of a poi stored twice");
		for (int a = 0; a < APIS.length; a++) {
			System.out.println("  " + APIS[a] + ": " + resultsByApi[a] + " results, " + charsByApi[a] / 1024 + " KB");
		}
		for (BinaryMapIndexReader r : javaReaders.values()) {
			r.close();
		}
		for (net.osmand.shared.binary.BinaryMapIndexReader r : copyReaders.values()) {
			r.close();
		}
	}

	/**
	 * Compares the apis over all cases and writes the dump; with {@code OSMAND_SEARCH_APIS_CASE}
	 * only the cases whose file names start with it, and no dump.
	 */
	@Test
	public void apisAnswerTheSame() throws IOException {
		String only = System.getenv("OSMAND_SEARCH_APIS_CASE");
		JAVA_DUMP.getParentFile().mkdirs();
		try (java.io.Writer w = only != null ? null : Files.newBufferedWriter(JAVA_DUMP.toPath(), StandardCharsets.UTF_8)) {
			out = w;
			for (Map.Entry<String, String> e : poiPhrases.entrySet()) {
				emit("X " + hex(e.getKey()) + "\t" + hex(e.getValue()));
			}
			for (int i = 0; i < cases.size(); i++) {
				if (only == null || cases.get(i).name.startsWith(only)) {
					runCase(i, cases.get(i));
				}
			}
			String maps = System.getenv("OSMAND_SEARCH_APIS_MAPS");
			if (maps != null && !maps.isEmpty()) {
				String[] paths = maps.split(":");
				for (int i = 0; i < paths.length; i++) {
					runCase(1000 + i, mapCase(paths[i]));
				}
			}
			if (only == null) {
				runPoiTypeNames();
				latLonIsFormattedTheSame();
				assertTrue("phrases run: " + phrasesRun, phrasesRun > 1000);
			}
		} finally {
			out = null;
		}
	}

	@Test
	public void latLonIsFormattedTheSame() {
		Object j = new SearchCoreFactory.SearchLocationAndUrlAPI(new SearchCoreFactory.SearchAmenityByNameAPI());
		Object k = new net.osmand.shared.search.core.SearchCoreFactory.SearchLocationAndUrlAPI(
				new net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityByNameAPI());
		DecimalFormat format = new DecimalFormat("0.0####", new DecimalFormatSymbols(Locale.US));
		List<Double> values = new ArrayList<>(Arrays.asList(0.0, -0.0, 1e-6, -1e-6, 5e-6, 1.5e-5, 2.5e-5, 0.015625,
				0.046875, 1.000005, 1.000025, 52.123455, 12.5, 180.0, -180.0, 90.0, 1e13, 9.2e13, 1.2345678901234567e14,
				1e15, 1e17, 1.2345678901234567e20, 1e23, Double.MAX_VALUE, Double.MIN_VALUE, -Double.MIN_VALUE,
				Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 99999.999995, 9.999995, -9.999995));
		Random r = new Random(11);
		for (int i = 0; i < 20000; i++) {
			// ties of the sixth digit, which the exact value decides
			long n = r.nextInt(360_000_000) - 180_000_000L;
			values.add((n * 10 + 5) / 1e6);
			values.add(r.nextDouble() * 360 - 180);
			values.add(Math.round((r.nextDouble() * 180 - 90) * 1e7) / 1e7);
			values.add(Double.longBitsToDouble(r.nextLong()));
		}
		for (double v : values) {
			String java = (String) call(j, "formatLatLon", v);
			assertEquals(bits(v), format.format(v), java);
			// the JDK rounds the double nearest to 0.000005 down, the copy rounds its exact value
			String copy = Math.abs(v) == 5e-6 ? (v < 0 ? "-0.00001" : "0.00001") : java;
			assertEquals(v + " " + bits(v), copy, call(k, "formatLatLon", v));
			emit("D " + bits(v) + "\t" + hex(copy));
		}
	}

	/**
	 * A line of the dump, when it is being written; the answers of a phrase only when they are
	 * dumped too. Into the lines of the case being run, if one is.
	 */
	private static void emit(String line) {
		if (out != null && (dumping || line.startsWith("Q "))) {
			if (caseLines != null) {
				caseLines.add(line);
			} else {
				write(line);
			}
		}
	}

	private static void write(String line) {
		try {
			out.write(line);
			out.write('\n');
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/** The C line of a case, the collation keys of its words, then its phrases, so they can be read in one go. */
	private static void writeCase(String caseLine, SearchPhraseCompatTest.NotingCollator collator) {
		List<String> lines = caseLines;
		caseLines = null;
		if (out != null) {
			write(caseLine);
			write("K " + collator.keys());
			for (String l : lines) {
				write(l);
			}
		}
	}

	// the cases

	private void runCase(int index, Case c) {
		dumping = true;
		boolean poiTypes = c.hasPoiTypePhrase();
		SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = poiTypes;
		net.osmand.shared.search.core.SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = poiTypes;
		try {
			SearchPhraseCompatTest.NotingCollator collator = new SearchPhraseCompatTest.NotingCollator();
			SearchSettings js = SearchSettings.parseJSON(new JSONObject(c.settings));
			net.osmand.shared.search.core.SearchSettings ks = copySettings(c.settings);
			List<BinaryMapIndexReader> jr = new ArrayList<>();
			List<net.osmand.shared.binary.BinaryMapIndexReader> kr = new ArrayList<>();
			for (String f : c.files) {
				jr.add(javaReaders.get(f));
				kr.add(copyReaders.get(f));
			}
			js.setOfflineIndexes(jr);
			ks.setOfflineIndexes(kr);
			Side j = new Side(javaApis());
			Side k = new Side(copyApis());
			caseLines = new ArrayList<>();
			for (String text : c.phrases) {
				if (text.startsWith("POI_TYPE:")) {
					String[] arr = text.split("[\\\\{}]");
					int empty = typed(j, k, js, ks, collator, "", 1, true);
					List<Object> found = j.results.get(empty).get(TYPES_API);
					for (int r = 0; r < found.size(); r++) {
						if (arr.length > 1 && arr[1].equals(((SearchResult) found.get(r)).localeName)) {
							selected(j, k, empty, TYPES_API, r, arr.length > 2 ? arr[2] : "", false);
							break;
						}
					}
					continue;
				}
				// the first phrase of a case is typed in as well
				List<Integer> cuts = text.equals(c.phrases.get(0)) ? cuts(text) : new ArrayList<>();
				for (int n = 0; n < cuts.size(); n++) {
					// every third prefix of a phrase goes into the dump
					dumping = n % 3 == 0;
					typed(j, k, js, ks, collator, text.substring(0, cuts.get(n)), 1, false);
				}
				dumping = true;
				int whole = typed(j, k, js, ks, collator, text, 1, true);
				typed(j, k, js, ks, collator, text, 2, false);
				for (int child : selectEach(j, k, whole, true)) {
					selectEach(j, k, child, false);
				}
			}
			if (index % 10 == 0) {
				for (String text : LOCATIONS) {
					typed(j, k, js, ks, collator, text, 1, false);
				}
			}
			writeCase("C " + index + "\t" + hex(c.name) + "\t" + hex(c.settings) + "\t" + hexList(c.files) + "\t" + poiTypes, collator);
		} finally {
			caseLines = null;
			dumping = true;
			SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false;
			net.osmand.shared.search.core.SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false;
		}
	}

	/**
	 * A case made of a real map, which the cases of the tests are not: the settlements, villages and
	 * postcodes of the file, streets of two settlements with and without a house number, pois around
	 * its centre, the values of its top index and a few types of pois, searched from its centre.
	 */
	private static Case mapCase(String path) throws IOException {
		File f = new File(path);
		BinaryMapIndexReader r = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
		javaReaders.put(path, r);
		copyReaders.put(path, new net.osmand.shared.binary.BinaryMapIndexReader(path));
		List<net.osmand.data.City> cities = r.getCities(null, net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks.CITY_TOWN_TYPE, null, null);
		net.osmand.data.LatLon center = r.getRegionCenter();
		if (center == null && !cities.isEmpty()) {
			center = cities.get(0).getLocation();
		}
		assertTrue(path + " has no centre", center != null);
		List<String> phrases = new ArrayList<>();
		for (int i = 0; i < cities.size() && i < 6 * spread(cities.size(), 6); i += spread(cities.size(), 6)) {
			net.osmand.data.City c = cities.get(i);
			phrases.add(c.getName());
			if (i < 2 * spread(cities.size(), 6)) {
				r.preloadStreets(c, null, null);
				List<net.osmand.data.Street> streets = c.getStreets();
				for (int k = 0; k < streets.size() && k < 4 * spread(streets.size(), 4); k += spread(streets.size(), 4)) {
					String street = streets.get(k).getName();
					if (!street.startsWith("<")) {
						phrases.add(street + " " + c.getName());
						phrases.add("1 " + street);
					}
				}
			}
		}
		for (net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks type : new net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks[] {
				net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks.VILLAGES_TYPE,
				net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks.POSTCODES_TYPE}) {
			List<net.osmand.data.City> l = r.getCities(null, type, null, null);
			for (int i = 0; i < l.size() && i < 3 * spread(l.size(), 3); i += spread(l.size(), 3)) {
				phrases.add(l.get(i).getName());
			}
		}
		int x = net.osmand.util.MapUtils.get31TileNumberX(center.getLongitude());
		int y = net.osmand.util.MapUtils.get31TileNumberY(center.getLatitude());
		List<net.osmand.data.Amenity> pois = r.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(x - 20000, x + 20000, y - 20000,
				y + 20000, -1, BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null));
		List<String> named = new ArrayList<>();
		for (net.osmand.data.Amenity a : pois) {
			if (!a.getName().isEmpty() && !named.contains(a.getName())) {
				named.add(a.getName());
			}
		}
		for (int i = 0; i < named.size() && i < 8 * spread(named.size(), 8); i += spread(named.size(), 8)) {
			phrases.add(named.get(i));
		}
		for (net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType sub : r.getTopIndexSubTypes()) {
			if (sub.possibleValues != null && !sub.possibleValues.isEmpty()) {
				phrases.add(sub.possibleValues.get(0));
			}
		}
		phrases.addAll(Arrays.asList("cafe", "atm", "fuel "));
		JSONObject settings = new JSONObject();
		settings.put("lat", center.getLatitude());
		settings.put("lon", center.getLongitude());
		settings.put("lang", "");
		settings.put("radiusLevel", 1);
		settings.put("totalLimit", -1);
		return new Case(f.getName(), settings.toString(), Collections.singletonList(path), phrases);
	}

	/** The step that takes [n] of [size] things, spread over them. */
	private static int spread(int size, int n) {
		return Math.max(1, size / n);
	}

	/**
	 * Where a phrase is cut to be typed in: after the first and the third letter of each word, at its
	 * end, and after the character that follows it; the thresholds the apis check are at one and
	 * three letters and at a complete word.
	 */
	static List<Integer> cuts(String text) {
		Set<Integer> cuts = new java.util.TreeSet<>();
		int start = -1;
		for (int i = 0; i <= text.length(); i++) {
			boolean delimiter = i == text.length() || text.charAt(i) == ' ' || text.charAt(i) == ',';
			if (!delimiter && start < 0) {
				start = i;
			} else if (delimiter && start >= 0) {
				cuts.add(Math.min(start + 1, i));
				cuts.add(Math.min(start + 3, i));
				cuts.add(i);
				cuts.add(Math.min(i + 1, text.length()));
				start = -1;
			}
		}
		cuts.remove(text.length());
		cuts.remove(0);
		return new ArrayList<>(cuts);
	}

	/** The names of the poi types typed in, for the two apis of poi types, over the first case. */
	private void runPoiTypeNames() {
		Case c = cases.get(0);
		SearchPhraseCompatTest.NotingCollator collator = new SearchPhraseCompatTest.NotingCollator();
		SearchSettings js = SearchSettings.parseJSON(new JSONObject(c.settings));
		net.osmand.shared.search.core.SearchSettings ks = copySettings(c.settings);
		List<Object> ja = javaApis();
		List<Object> ka = copyApis();
		Set<String> names = new LinkedHashSet<>();
		for (String v : poiPhrases.values()) {
			for (String name : v.split(";")) {
				if (!name.trim().isEmpty() && random.nextInt(16) == 0) {
					names.add(name.trim());
				}
			}
		}
		caseLines = new ArrayList<>();
		int qid = 0;
		for (String name : names) {
			for (String text : new String[] {name, name + " ", name.substring(0, Math.min(name.length(), 4)), name + " 1"}) {
				SearchPhrase jp = SearchPhrase.emptyPhrase(js, collator).generateNewPhrase(text, js);
				Object kp = copyPhrase(ks, text);
				prepare(jp);
				prepare(kp);
				emit("Q " + qid + "\t-1\tT\t-1\t-1\t" + hex(text) + "\t1\tfalse\ttrue");
				for (int a = TYPES_API; a <= BY_TYPE_API; a++) {
					runApi(qid, a, ja.get(a), ka.get(a), jp, kp, null, null);
				}
				phrasesRun++;
				qid++;
			}
		}
		writeCase("C -1\t" + hex(c.name) + "\t" + hex(c.settings) + "\t-\tfalse", collator);
	}

	/** Types [text] in at [radius] on both sides and runs every api; the id of the phrase. */
	private int typed(Side j, Side k, SearchSettings js, net.osmand.shared.search.core.SearchSettings ks,
			SearchPhraseCompatTest.NotingCollator collator, String text, int radius, boolean keep) {
		SearchSettings jsr = radius == 1 ? js : js.setRadiusLevel(radius);
		net.osmand.shared.search.core.SearchSettings ksr = radius == 1 ? ks : ks.setRadiusLevel(radius);
		SearchPhrase jp = SearchPhrase.emptyPhrase(jsr, collator).generateNewPhrase(text, jsr);
		Object kp = copyPhrase(ksr, text);
		int qid = j.phrases.size();
		emit("Q " + qid + "\t-1\tT\t-1\t-1\t" + hex(text) + "\t" + radius + "\t" + keep + "\t" + dumping);
		run(j, k, qid, jp, kp, keep);
		return qid;
	}

	/**
	 * Selects the result [r] of api [api] for phrase [parent], as the ui selects it, or, with
	 * [text], types [text] in after it as the cases of poi types do; the id of the phrase.
	 */
	private int selected(Side j, Side k, int parent, int api, int r, String text, boolean keep) {
		SearchPhrase jparent = (SearchPhrase) j.phrases.get(parent);
		Object kparent = k.phrases.get(parent);
		SearchResult jres = (SearchResult) j.results.get(parent).get(api).get(r);
		// the same result on the other side, which is not always at the same place
		String line = resultLine(jres);
		Object kres = null;
		for (Object o : k.results.get(parent).get(api)) {
			if (resultLine(o).equals(line)) {
				kres = o;
				break;
			}
		}
		for (Object o : k.results.get(parent).get(api)) {
			if (kres == null && withoutSubtypes(Collections.singletonList(resultLine(o))).equals(withoutSubtypes(Collections.singletonList(line)))) {
				kres = o;
			}
		}
		assertTrue(line, kres != null);
		SearchPhrase jp;
		Object kp;
		if (text == null) {
			jp = jparent.selectWord(jres, jparent.getSettings());
			kp = call(kparent, "selectWord", kres, call(kparent, "getSettings"));
		} else {
			jp = jparent.generateNewPhrase(text, jparent.getSettings());
			jp.getWords().add(new SearchWord(jres.localeName, jres));
			kp = call(kparent, "generateNewPhrase", text, call(kparent, "getSettings"));
			@SuppressWarnings("unchecked")
			List<Object> words = (List<Object>) call(kp, "getWords");
			words.add(new net.osmand.shared.search.core.SearchWord(
					(String) field(kres, "localeName"), (net.osmand.shared.search.core.SearchResult) kres));
		}
		int qid = j.phrases.size();
		emit("Q " + qid + "\t" + parent + "\t" + (text == null ? "S" : "W") + "\t" + api + "\t" + r + "\t"
				+ hex(text == null ? "" : text) + "\t" + jp.getRadiusLevel() + "\t" + keep + "\t" + dumping);
		selections++;
		run(j, k, qid, jp, kp, keep);
		return qid;
	}

	/** Selects the first result of each type each api found for phrase [parent]; the ids of the phrases. */
	private List<Integer> selectEach(Side j, Side k, int parent, boolean keep) {
		List<Integer> children = new ArrayList<>();
		for (int a = 0; a < APIS.length; a++) {
			Set<ObjectType> seen = new LinkedHashSet<>();
			List<Object> found = j.results.get(parent).get(a);
			for (int r = 0; r < found.size(); r++) {
				SearchResult res = (SearchResult) found.get(r);
				if (res.objectType == null || res.objectType.name().startsWith("SEARCH_") || res.localeName == null
						|| !seen.add(res.objectType)) {
					continue;
				}
				children.add(selected(j, k, parent, a, r, null, keep));
			}
		}
		return children;
	}

	/** Runs every api for the phrase [qid] on both sides, as the core prepares and runs them. */
	private void run(Side j, Side k, int qid, SearchPhrase jp, Object kp, boolean keep) {
		prepare(jp);
		prepare(kp);
		j.phrases.add(jp);
		k.phrases.add(kp);
		List<List<Object>> jresults = new ArrayList<>();
		List<List<Object>> kresults = new ArrayList<>();
		for (int a = 0; a < APIS.length; a++) {
			List<Object> jr = new ArrayList<>();
			List<Object> kr = new ArrayList<>();
			runApi(qid, a, j.apis.get(a), k.apis.get(a), jp, kp, jr, kr);
			jresults.add(jr);
			kresults.add(kr);
		}
		j.results.add(keep ? jresults : null);
		k.results.add(keep ? kresults : null);
		phrasesRun++;
	}

	/** {@code SearchUICore.preparePhrase}: the files of the selected words first, then by distance. */
	private static void prepare(Object phrase) {
		for (Object w : (List<?>) call(phrase, "getWords")) {
			Object res = call(w, "getResult");
			if (res != null && field(res, "file") != null) {
				call(phrase, "selectFile", field(res, "file"));
			}
		}
		call(phrase, "sortFiles");
	}

	private void runApi(int qid, int a, Object japi, Object kapi, SearchPhrase jp, Object kp,
			List<Object> jfound, List<Object> kfound) {
		List<Object> jr = jfound == null ? new ArrayList<>() : jfound;
		List<Object> kr = kfound == null ? new ArrayList<>() : kfound;
		// which types could be taken matters only where the copy is held to java on Kotlin/Native
		String tie = a == BY_TYPE_API && out != null && dumping ? tie(japi, jp) : "-";
		String javaLine = apiLine(japi, jp, javaMatcher(jr, jp)) + "\t" + extras(japi, jp) + "\t" + tie;
		String copyLine = apiLine(kapi, kp, copyMatcher(kr, kp)) + "\t" + extras(kapi, kp) + "\t" + tie;
		String m = qid + " " + APIS[a] + " " + jp;
		if (!javaLine.equals(copyLine)) {
			List<String> jl = new ArrayList<>();
			List<String> kl = new ArrayList<>();
			for (Object o : jr) {
				jl.add(resultLine(o));
			}
			for (Object o : kr) {
				kl.add(resultLine(o));
			}
			System.out.println("MISMATCH " + m + "\njava " + javaLine + "\ncopy " + copyLine);
			for (int r = 0; r < Math.max(jl.size(), kl.size()); r++) {
				String x = r < jl.size() ? jl.get(r) : "-";
				String y = r < kl.size() ? kl.get(r) : "-";
				System.out.println((x.equals(y) ? "  = " : "  J ") + x + (x.equals(y) ? "" : "\n  K " + y));
			}
		}
		assertEquals(m, javaLine, copyLine);
		emit("A " + qid + "\t" + a + "\t" + javaLine);
		assertEquals(m + " results " + describeAll(jr) + " / " + describeAll(kr), jr.size(), kr.size());
		List<String> javaResults = new ArrayList<>();
		List<String> copyResults = new ArrayList<>();
		for (int r = 0; r < jr.size(); r++) {
			javaResults.add(resultLine(jr.get(r)));
			copyResults.add(resultLine(kr.get(r)));
			emit("R " + qid + "\t" + a + "\t" + r + "\t" + javaResults.get(r));
			resultsCompared++;
			resultsByApi[a]++;
			charsByApi[a] += javaResults.get(r).length();
		}
		List<String> jo = inOrder(a, javaResults);
		List<String> ko = inOrder(a, copyResults);
		if (!jo.equals(ko)) {
			List<String> onlyJava = new ArrayList<>(jo);
			onlyJava.removeAll(ko);
			List<String> onlyCopy = new ArrayList<>(ko);
			onlyCopy.removeAll(jo);
			System.out.println("MISMATCH " + m + "\nonly java " + onlyJava.size() + ":\n  " + String.join("\n  ", onlyJava.subList(0, Math.min(5, onlyJava.size())))
					+ "\nonly copy " + onlyCopy.size() + ":\n  " + String.join("\n  ", onlyCopy.subList(0, Math.min(5, onlyCopy.size()))));
			for (int r = 0; r < jo.size(); r++) {
				if (!jo.get(r).equals(ko.get(r))) {
					System.out.println("first at " + r + "\n  J " + jo.get(r) + "\n  K " + ko.get(r));
					break;
				}
			}
		}
		if (!jo.equals(ko) && a == 0 && withoutSubtypes(jo).equals(withoutSubtypes(ko))) {
			// a poi stored twice under one id, which the api takes once: the one the reader reads first
			otherDuplicates++;
			return;
		}
		assertEquals(m, jo, ko);
		if (!"-".equals(tie) && !"none".equals(tie) && !"one".equals(tie)) {
			ties++;
		}
	}

	/**
	 * The results of the api of pois by name with the subtypes of the pois left out, sorted. A poi
	 * stored twice, as a bank and as an atm, has one id, and the api publishes the one of the two the
	 * reader hands it first, which, with the order of the reader, is not always the same one.
	 */
	static List<String> withoutSubtypes(List<String> results) {
		List<String> l = new ArrayList<>();
		for (String r : results) {
			l.add(r.replaceAll("(A[-0-9a-z]+:[^: ]*:)'[^: ]*:", "$1*:"));
		}
		Collections.sort(l);
		return l;
	}

	/**
	 * The results in the order they are compared in. The name index of pois hands out the pois of
	 * one file nearest first, and java leaves the ones at the same distance in the order of its
	 * hash map, which the copy of the reader does not reproduce: it takes them by offset. So the
	 * results of the api of pois by name are compared per file in any order.
	 */
	static List<String> inOrder(int api, List<String> results) {
		if (api != 0) {
			return results;
		}
		List<String> ordered = new ArrayList<>();
		List<String> file = new ArrayList<>();
		for (String r : results) {
			if (r.startsWith("SEARCH_API_REGION_FINISHED")) {
				Collections.sort(file);
				ordered.addAll(file);
				file.clear();
				ordered.add(r);
			} else {
				file.add(r);
			}
		}
		Collections.sort(file);
		ordered.addAll(file);
		return ordered;
	}

	private static List<String> describeAll(List<Object> results) {
		List<String> l = new ArrayList<>();
		for (Object r : results) {
			l.add(field(r, "objectType") + " " + field(r, "localeName"));
		}
		return l;
	}

	// what an api answers and publishes, the same text for both sides

	/**
	 * Whether the api runs for the phrase and with which priority, what search returned or threw,
	 * whether it can search further and in which radii: in the order the core asks. Like the core,
	 * it searches only when the api runs for the phrase.
	 */
	static String apiLine(Object api, Object phrase, Object matcher) {
		StringBuilder sb = new StringBuilder();
		String available = outcome(() -> call(api, "isSearchAvailable", phrase));
		String priority = outcome(() -> call(api, "getSearchPriority", phrase));
		sb.append(available).append(' ').append(priority).append(' ');
		sb.append("true".equals(available) && !"-1".equals(priority) ? outcome(() -> call(api, "search", phrase, matcher)) : "-").append(' ');
		sb.append(outcome(() -> call(api, "isSearchMoreAvailable", phrase))).append(' ');
		sb.append(outcome(() -> call(api, "getMinimalSearchRadius", phrase))).append(' ');
		sb.append(outcome(() -> call(api, "getNextSearchRadius", phrase))).append(' ');
		sb.append(call(matcher, "getCount"));
		return sb.toString();
	}

	/** What the api of poi types by type found out, and the poi type the phrase keeps. */
	static String extras(Object api, Object phrase) {
		StringBuilder sb = new StringBuilder();
		if (api.getClass().getSimpleName().equals("SearchAmenityByTypeAPI")) {
			sb.append(describe(call(api, "getUnselectedPoiType"))).append(' ').append(v(call(api, "getNameFilter"))).append(' ');
		}
		sb.append(describe(call(phrase, "getUnselectedPoiType")));
		return sb.toString();
	}

	private static String outcome(java.util.concurrent.Callable<Object> c) {
		try {
			return String.valueOf(c.call());
		} catch (Throwable t) {
			Throwable cause = t;
			while (cause instanceof AssertionError && cause.getCause() != null) {
				cause = cause.getCause();
			}
			return "threw:" + cause.getClass().getSimpleName();
		}
	}

	/** A result the api published: all of it, with the objects behind it and its parents. */
	static String resultLine(Object r) {
		Object type = field(r, "objectType");
		StringBuilder sb = new StringBuilder();
		sb.append(type).append(' ');
		if (type != null && type.toString().startsWith("SEARCH_")) {
			sb.append(describe(field(r, "object"))).append(' ').append(describe(field(r, "file")));
		} else {
			for (String f : new String[] {"localeName", "alternateName", "otherNames", "localeRelatedObjectName"}) {
				sb.append(v(field(r, f))).append(' ');
			}
			sb.append(describe(field(r, "relatedObject"))).append(' ');
			sb.append(bits((Double) field(r, "distRelatedObjectName"))).append(' ');
			sb.append(v(field(r, "location"))).append(' ');
			sb.append(bits((Double) field(r, "priority"))).append(' ');
			sb.append(bits((Double) field(r, "priorityDistance"))).append(' ');
			sb.append(field(r, "preferredZoom")).append(' ');
			sb.append(describe(field(r, "file"))).append(' ');
			sb.append(describe(field(r, "object"))).append(' ');
			sb.append(v(field(r, "cityName"))).append(' ');
			sb.append(v(field(r, "wordsSpan"))).append(' ');
			sb.append(field(r, "firstUnknownWordMatches")).append(' ');
			Object words = field(r, "otherWordsMatch");
			sb.append(words == null ? "null" : v(new ArrayList<>((Collection<?>) words))).append(' ');
			sb.append(bits((Double) call(r, "getUnknownPhraseMatchWeight"))).append(' ');
			sb.append(call(r, "hasImpreciseCoordinates")).append(' ');
			sb.append(v(call(field(r, "requiredSearchPhrase"), "getText", true)));
		}
		sb.append(' ').append(parents(r));
		return sb.toString();
	}

	private static String parents(Object r) {
		StringBuilder sb = new StringBuilder();
		Object p = field(r, "parentSearchResult");
		for (int depth = 0; p != null && depth < 6; depth++) {
			sb.append('<').append(field(p, "objectType")).append(':').append(v(field(p, "localeName"))).append(':')
					.append(describe(field(p, "object")));
			p = field(p, "parentSearchResult");
		}
		return sb.length() == 0 ? "-" : sb.toString();
	}

	/** An object behind a result, by what it is. */
	static String describe(Object o) {
		if (o == null) {
			return "-";
		}
		String n = o.getClass().getSimpleName();
		switch (n) {
			case "City":
				return "C" + v(call(o, "getId")) + ":" + v(call(o, "getName")) + ":" + call(o, "getType") + ":"
						+ v(call(o, "getLocation")) + ":" + Arrays.toString((int[]) call(o, "getBbox31")) + ":"
						+ ((List<?>) call(o, "getStreets")).size();
			case "Street":
				Object city = call(o, "getCity");
				return "S" + v(call(o, "getId")) + ":" + v(call(o, "getName")) + ":" + v(call(o, "getLocation")) + ":"
						+ (city == null ? "-" : v(call(city, "getId")) + v(call(city, "getName"))) + ":"
						+ ((List<?>) call(o, "getBuildings")).size() + ":" + ((List<?>) call(o, "getIntersectedStreets")).size();
			case "Building":
				return "B" + v(call(o, "getId")) + ":" + v(call(o, "getName")) + ":" + v(call(o, "getName2")) + ":"
						+ call(o, "getInterpolationType") + ":" + call(o, "getInterpolationInterval") + ":"
						+ v(call(o, "getLocation")) + ":" + v(call(o, "getLatLon2")) + ":" + v(call(o, "getPostcode"));
			case "Amenity":
				Object t = call(o, "getType");
				return "A" + v(call(o, "getId")) + ":" + (t == null ? "-" : call(t, "getKeyName")) + ":"
						+ v(call(o, "getSubType")) + ":" + v(call(o, "getName")) + ":" + v(call(o, "getLocation"));
			case "PoiType":
			case "PoiCategory":
			case "PoiFilter":
			case "PoiAdditionalCustomFilter":
				StringBuilder sb = new StringBuilder("T" + n + ":" + call(o, "getKeyName"));
				if (n.equals("PoiAdditionalCustomFilter")) {
					List<String> keys = new ArrayList<>();
					for (Object a : (List<?>) field(o, "additionalPoiTypes")) {
						keys.add((String) call(a, "getKeyName"));
					}
					Collections.sort(keys);
					sb.append(keys);
				}
				return sb.toString();
			case "TopIndexFilter":
				return "I" + v(call(o, "getFilterId")) + ":" + v(call(o, "getName"));
			case "BinaryMapIndexReader":
				return "R" + v(o);
			case "LatLon":
			case "KLatLon":
				return "L" + v(o);
			case "GeoParsedPoint":
			case "KGeoParsedPoint":
				return "G" + bits((Double) call(o, "getLatitude")) + ":" + bits((Double) call(o, "getLongitude")) + ":"
						+ call(o, "getZoom") + ":" + v(call(o, "getLabel"));
			default:
				if (n.endsWith("API")) {
					return "P" + n;
				}
				if (hasMethod(o, "getFilterId")) {
					return "F" + v(call(o, "getFilterId")) + ":" + v(call(o, "getName"));
				}
				return "?" + n;
		}
	}

	/**
	 * Any value either side gives, written the same way for both: text escaped and quoted, to keep
	 * the dump small, the rest as {@code SearchPhraseCompatTest} writes it.
	 */
	static String v(Object o) {
		if (o instanceof String) {
			String t = (String) o;
			StringBuilder sb = new StringBuilder("'");
			for (int i = 0; i < t.length(); i++) {
				char c = t.charAt(i);
				int e = "\\ \t\n\r,[]:<|".indexOf(c);
				if (e >= 0) {
					sb.append('\\').append("\\stnrcbBol|".charAt(e));
				} else {
					sb.append(c);
				}
			}
			return sb.toString();
		}
		if (o instanceof Collection) {
			StringBuilder sb = new StringBuilder("[");
			for (Object e : (Collection<?>) o) {
				sb.append(v(e)).append(',');
			}
			return sb.append(']').toString();
		}
		return SearchPhraseCompatTest.str(o);
	}

	private static boolean hasMethod(Object o, String name) {
		for (java.lang.reflect.Method m : o.getClass().getMethods()) {
			if (m.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * How many poi types the api of pois by type could take for the phrase, of the ones with the
	 * most words that match: more than one, and which it takes depends on the order of java's
	 * {@code HashMap} of their names. "-" when the api does not choose.
	 */
	private static String tie(Object api, SearchPhrase phrase) {
		if (phrase.isLastWord(ObjectType.POI_TYPE) || !phrase.isNoSelectedType() || phrase.getFirstUnknownSearchWord().length() <= 1) {
			return "-";
		}
		SearchCoreFactory.SearchAmenityTypesAPI typesApi = (SearchCoreFactory.SearchAmenityTypesAPI) field(api, "searchAmenityTypesAPI");
		call(typesApi, "initPoiTypes");
		NameStringMatcher nm = phrase.getFirstUnknownNameStringMatcher();
		NameStringMatcher nmAdditional = new NameStringMatcher(phrase.getFirstUnknownSearchWord(),
				CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
		int most = 0;
		Set<String> candidates = new LinkedHashSet<>();
		for (Object ptr : typesApi.getPoiTypeResults(nm, nmAdditional).values()) {
			for (Object w : (Set<?>) field(ptr, "foundWords")) {
				String foundName = (String) w;
				CollatorStringMatcher csm = new CollatorStringMatcher(foundName, CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				if (!csm.matches(phrase.getUnknownSearchPhrase())) {
					continue;
				}
				int words = SearchPhrase.countWords(foundName);
				String key = ((AbstractPoiType) field(ptr, "pt")).getKeyName();
				if (words > most) {
					most = words;
					candidates.clear();
				}
				if (words == most) {
					candidates.add(key);
				}
			}
		}
		return candidates.isEmpty() ? "none" : candidates.size() == 1 ? "one" : "tie" + candidates.size();
	}

	// the two sides

	static List<Object> javaApis() {
		MapPoiTypes types = MapPoiTypes.getDefault();
		SearchCoreFactory.SearchAmenityByNameAPI amenitiesApi = new SearchCoreFactory.SearchAmenityByNameAPI();
		SearchCoreFactory.SearchAmenityTypesAPI typesApi = new SearchCoreFactory.SearchAmenityTypesAPI(types);
		SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI streetsApi = new SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI();
		SearchCoreFactory.SearchStreetByCityAPI cityApi = new SearchCoreFactory.SearchStreetByCityAPI(streetsApi);
		SearchCoreFactory.TownCitiesCache cache = new SearchCoreFactory.TownCitiesCache();
		return new ArrayList<>(Arrays.asList(amenitiesApi,
				new SearchCoreFactory.SearchLocationAndUrlAPI(amenitiesApi, () -> false), typesApi,
				new SearchCoreFactory.SearchAmenityByTypeAPI(types, typesApi), streetsApi, cityApi,
				new SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, false, cache),
				new SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, true, cache),
				new SearchCoreFactory.SearchRegionByNameAPI()));
	}

	private static List<Object> copyApis() {
		net.osmand.shared.osm.MapPoiTypes types = net.osmand.shared.osm.MapPoiTypes.Companion.getDefault();
		net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityByNameAPI amenitiesApi =
				new net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityByNameAPI();
		net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityTypesAPI typesApi =
				new net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityTypesAPI(types);
		net.osmand.shared.search.core.SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI streetsApi =
				new net.osmand.shared.search.core.SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI();
		net.osmand.shared.search.core.SearchCoreFactory.SearchStreetByCityAPI cityApi =
				new net.osmand.shared.search.core.SearchCoreFactory.SearchStreetByCityAPI(streetsApi);
		net.osmand.shared.search.core.SearchCoreFactory.TownCitiesCache cache =
				new net.osmand.shared.search.core.SearchCoreFactory.TownCitiesCache();
		return new ArrayList<>(Arrays.asList(amenitiesApi, copyLocationApi(amenitiesApi), typesApi,
				new net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityByTypeAPI(types, typesApi), streetsApi, cityApi,
				new net.osmand.shared.search.core.SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, false, cache),
				new net.osmand.shared.search.core.SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, true, cache),
				new net.osmand.shared.search.core.SearchCoreFactory.SearchRegionByNameAPI()));
	}

	/** The copy's api of locations and links, told there is no internet: a kotlin lambda OsmAnd-java sees only when it runs. */
	private static Object copyLocationApi(Object amenitiesApi) {
		try {
			Class<?> function = Class.forName("kotlin.jvm.functions.Function0");
			Object offline = Proxy.newProxyInstance(function.getClassLoader(), new Class<?>[] {function},
					(proxy, method, args) -> {
						switch (method.getName()) {
							case "invoke":
								return Boolean.FALSE;
							case "hashCode":
								return System.identityHashCode(proxy);
							case "equals":
								return proxy == args[0];
							default:
								return "offline";
						}
					});
			return net.osmand.shared.search.core.SearchCoreFactory.SearchLocationAndUrlAPI.class
					.getConstructor(amenitiesApi.getClass(), function).newInstance(amenitiesApi, offline);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static net.osmand.shared.search.core.SearchSettings copySettings(String json) {
		try {
			Object parser = Class.forName("kotlinx.serialization.json.Json").getField("Default").get(null);
			Object element = call(parser, "parseToJsonElement", json);
			return (net.osmand.shared.search.core.SearchSettings) call(
					net.osmand.shared.search.core.SearchSettings.Companion, "parseJSON", element);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static Object copyPhrase(net.osmand.shared.search.core.SearchSettings s, String text) {
		return net.osmand.shared.search.core.SearchPhrase.Companion.emptyPhrase(s).generateNewPhrase(text, s);
	}

	private static SearchResultMatcher javaMatcher(List<Object> found, SearchPhrase phrase) {
		ResultMatcher<SearchResult> recorder = new ResultMatcher<SearchResult>() {
			@Override
			public boolean publish(SearchResult object) {
				found.add(object);
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		return new SearchResultMatcher(recorder, phrase, 0, new AtomicInteger(0), -1);
	}

	private static Object copyMatcher(List<Object> found, Object phrase) {
		net.osmand.shared.binary.ResultMatcher<net.osmand.shared.search.core.SearchResult> recorder =
				new net.osmand.shared.binary.ResultMatcher<net.osmand.shared.search.core.SearchResult>() {
					@Override
					public boolean publish(net.osmand.shared.search.core.SearchResult obj) {
						found.add(obj);
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				};
		// stately's atomic is java's on the jvm
		return new net.osmand.shared.search.SearchUICore.SearchResultMatcher(recorder,
				(net.osmand.shared.search.core.SearchPhrase) phrase, 0, new AtomicInteger(0), -1);
	}

	// the english names of the poi types, as SearchUICoreTest gives them

	private static String translation(String keyName) {
		String val = poiPhrases.get("poi_" + keyName);
		if (val != null) {
			int ind = val.indexOf(';');
			if (ind > 0) {
				return val.substring(0, ind);
			}
		}
		return val;
	}

	private static String synonyms(String keyName) {
		String val = poiPhrases.get("poi_" + keyName);
		if (val != null) {
			int ind = val.indexOf(';');
			if (ind > 0) {
				return val.substring(ind + 1);
			}
			return "";
		}
		return null;
	}

	/** The english names of the poi types, as {@code SearchUICoreTest} gives them. */
	static final class JavaTranslator implements MapPoiTypes.PoiTranslator {

		@Override
		public String getTranslation(AbstractPoiType type) {
			AbstractPoiType base = type.getBaseLangType();
			if (base != null) {
				return getTranslation(base) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getTranslation(type.getFormattedKeyName());
		}

		@Override
		public String getTranslation(String keyName) {
			return translation(keyName);
		}

		@Override
		public String getEnTranslation(AbstractPoiType type) {
			AbstractPoiType base = type.getBaseLangType();
			if (base != null) {
				return getEnTranslation(base) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getEnTranslation(type.getFormattedKeyName());
		}

		@Override
		public String getEnTranslation(String keyName) {
			return translation(keyName);
		}

		@Override
		public String getSynonyms(AbstractPoiType type) {
			AbstractPoiType base = type.getBaseLangType();
			if (base != null) {
				return getSynonyms(base);
			}
			return getSynonyms(type.getFormattedKeyName());
		}

		@Override
		public String getSynonyms(String keyName) {
			return synonyms(keyName);
		}

		@Override
		public String getAllLanguagesTranslationSuffix() {
			return "all languages";
		}
	}

	private static final class CopyTranslator implements net.osmand.shared.osm.PoiTranslator {

		@Override
		public String getTranslation(net.osmand.shared.osm.AbstractPoiType type) {
			net.osmand.shared.osm.AbstractPoiType base = type.getBaseLangType();
			if (base != null) {
				return getTranslation(base) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getTranslation(type.getFormattedKeyName());
		}

		@Override
		public String getTranslation(String keyName) {
			return translation(keyName);
		}

		@Override
		public String getEnTranslation(net.osmand.shared.osm.AbstractPoiType type) {
			net.osmand.shared.osm.AbstractPoiType base = type.getBaseLangType();
			if (base != null) {
				return getEnTranslation(base) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getEnTranslation(type.getFormattedKeyName());
		}

		@Override
		public String getEnTranslation(String keyName) {
			return translation(keyName);
		}

		@Override
		public String getSynonyms(net.osmand.shared.osm.AbstractPoiType type) {
			net.osmand.shared.osm.AbstractPoiType base = type.getBaseLangType();
			if (base != null) {
				return getSynonyms(base);
			}
			return getSynonyms(type.getFormattedKeyName());
		}

		@Override
		public String getSynonyms(String keyName) {
			return synonyms(keyName);
		}

		@Override
		public String getAllLanguagesTranslationSuffix() {
			return "all languages";
		}
	}

}
