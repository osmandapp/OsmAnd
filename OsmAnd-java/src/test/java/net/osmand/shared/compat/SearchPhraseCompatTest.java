package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchExportSettings;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.SearchPhraseDataType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.util.KMapUtils;
import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * The phrase, the results and the settings of the search in OsmAnd-shared against the originals in
 * OsmAnd-java: {@link net.osmand.shared.search.core.SearchPhrase}, {@code SearchResult},
 * {@code SearchSettings}, {@code RegionPriorityProvider}, {@code TopIndexFilter} and
 * {@code ObjectType}, with {@code KTreeSet} and {@link KMapUtils#calculate31BboxUsingRhumb}.
 *
 * The phrases are those of the 60 cases of {@code SearchUICoreTest}, with their settings, and
 * phrases made of their words with the characters that split words, braces, abbreviations,
 * conjunctions, house numbers and Open Location Codes. Each is typed in whole and compared getter by
 * getter, and so is what the name matchers of its words say about the names of the expected results.
 * Then results are made of those names, with the cities, streets and pois behind them, their parents
 * and their distances, and a phrase counts its words in them, weighs them and selects them.
 *
 * The files a phrase searches are the obf files of the tests, picked by distance, by region and in
 * order.
 *
 * {@link #javaDumpIsWritten} writes all of it, the input with java's answers, to
 * {@code build/search-phrase-java.txt}, which {@code SearchPhraseTest} in OsmAnd-shared holds the
 * copy to on Kotlin/Native. With it go the collation keys of the words java compared, so that the
 * copy can be held to java's collator there as well.
 */
public class SearchPhraseCompatTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File SEARCH_CASES = new File("src/test/resources/search");

	/** Read by {@code SearchPhraseTest} in OsmAnd-shared, from {@code ../OsmAnd-java/build}. */
	private static final File JAVA_DUMP = new File("build/search-phrase-java.txt");

	private static final String[] TRICKY_PHRASES = {
			"", " ", "  ", "a", "a ", " a", "a  b", "a,b", "a, b", "a ,b", "a,,b", ",", ", ", ",a", "a,",
			"a\tb", "a\nb", "a\rb", "a;", "a; b", "a b", "a b", "a\u000bb", "a\fb",
			"main st", "main st ", "Main St.", "st. george", "ул. Ленина", "пр-т Мира 5", "вул. Хрещатик 22",
			"42 wallaby way sydney", "221b baker street", "1-3 rue de la paix", "O'Connell Street",
			"O’Connell Street", "\"Quoted\" Name", "«Кавычки»", "Café de Flore", "straße", "Hauptstraße 1",
			"san jose", "saint-jean", "new york and new jersey", "a and b", "and", "and ", "und", "и", "і ",
			"الرياض شارع الملك فهد", "ٱلْقَاهِرَة", "東京都 新宿区", "Frankfurt (Oder)", "Frankfurt (Oder) ",
			"(a) b", "a (b) c", "a (b", "a b)", "9C3XGV4C+XV", "9C3XGV4C+XV london", "GV4C+XV", "8FVC9G8F+6X",
			"12", "12 ", "12a", "12a main", "main 12a", "no 12", "#5", "5th ave", "3rd", "1st street",
			"n", "north", "northwest", "nw", "e", "st", "St", "ST", "dr.", "mt.", "ft", "rd", "ave",
			"a.", "a. ", "b.c.", "..", ". ", "-", "- ", "a-b", "a - b", "a–b", "a—b", "a/b", "a\\b",
	};

	private static final String[] RESULT_TYPES = {"CITY", "VILLAGE", "POSTCODE", "STREET", "HOUSE",
			"STREET_INTERSECTION", "POI", "BOUNDARY", "LOCATION", "REGION"};

	private static final List<Settings> settings = new ArrayList<>();
	private static final List<PhraseCase> phrases = new ArrayList<>();
	private static final List<String> dump = new ArrayList<>();
	private static int resultsCompared;
	private static int selectionsCompared;

	private static OsmandRegions javaRegions;
	private static net.osmand.shared.map.OsmandRegions copyRegions;

	/** One case's settings, as its json and as both read it. */
	private static final class Settings {
		final String json;
		final SearchSettings java;
		final net.osmand.shared.search.core.SearchSettings copy;

		Settings(String json) {
			this.json = json;
			this.java = SearchSettings.parseJSON(new JSONObject(json));
			this.copy = parseCopy(json);
		}
	}

	/** A phrase to type in with settings, and the names of the results it should find. */
	private static final class PhraseCase {
		final int settings;
		final String text;
		final List<String> names;
		final List<String> expected;

		PhraseCase(int settings, String text, List<String> names, List<String> expected) {
			this.settings = settings;
			this.text = text;
			this.names = names;
			this.expected = expected;
		}
	}

	@BeforeClass
	public static void open() throws IOException {
		assertTrue(REGIONS + " is missing; :OsmAnd-java:processResources downloads it", REGIONS.exists());
		PlatformUtil.setOsmandRegions(new OsmandRegions(REGIONS.getPath()));
		net.osmand.shared.binary.CommonWords.Companion.setOsmandRegions(
				new net.osmand.shared.map.OsmandRegions(REGIONS.getPath()));
		javaRegions = new OsmandRegions(REGIONS.getPath());
		copyRegions = new net.osmand.shared.map.OsmandRegions(REGIONS.getPath());
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));

		File[] cases = SEARCH_CASES.listFiles((dir, name) -> name.endsWith(".json"));
		assertTrue("search cases in " + SEARCH_CASES + "; collectTestResources copies them", cases != null && cases.length > 50);
		Arrays.sort(cases);
		List<String> words = new ArrayList<>();
		for (File f : cases) {
			JSONObject json = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
			int s = addSettings(json.getJSONObject("settings").toString());
			List<String> texts = new ArrayList<>();
			if (json.has("phrase")) {
				texts.add(json.getString("phrase"));
			}
			JSONArray arr = json.optJSONArray("phrases");
			for (int i = 0; arr != null && i < arr.length(); i++) {
				texts.add(arr.getString(i));
			}
			JSONArray results = json.optJSONArray("results");
			for (int i = 0; i < texts.size(); i++) {
				List<String> expected = new ArrayList<>();
				if (results != null && i < results.length()) {
					JSONArray r = results.optJSONArray(i);
					for (int j = 0; r != null && j < r.length(); j++) {
						expected.add(r.getString(j));
					}
				}
				List<String> names = new ArrayList<>();
				for (String e : expected) {
					for (String part : stripWeight(e).split(", ")) {
						if (!part.isEmpty() && !names.contains(part)) {
							names.add(part);
						}
					}
				}
				String text = texts.get(i);
				phrases.add(new PhraseCase(s, text, names, expected));
				words.addAll(Arrays.asList(text.split(" ")));
			}
		}
		List<String> variants = new ArrayList<>(Arrays.asList(
				"{}", "{\"lat\":\"52.1\",\"lon\":\"4.5\"}", "{\"lat\":52.1,\"lon\":4.5}", "{\"lat\":\"52.1\"}",
				"{\"radiusLevel\":\"3\",\"totalLimit\":\"10\"}", "{\"radiusLevel\":2.9,\"totalLimit\":-1.5}",
				"{\"radiusLevel\":\"x\",\"totalLimit\":true}", "{\"transliterateIfMissing\":\"TRUE\",\"emptyQueryAllowed\":\"no\"}",
				"{\"transliterateIfMissing\":true,\"emptyQueryAllowed\":1,\"sortByName\":\"false\"}",
				"{\"lang\":\"de\",\"appLang\":\"ru\",\"regionLang\":\"be,ru\"}",
				"{\"searchTypes\":[\"POI\",\"CITY\",\"STREET\"]}", "{\"searchTypes\":[]}"));
		for (String v : variants) {
			addSettings(v);
		}
		int fixturePhrases = phrases.size();
		// the tricky phrases with the settings of each language the cases have
		List<Integer> languages = new ArrayList<>();
		List<String> seen = new ArrayList<>();
		for (int i = 0; i < settings.size(); i++) {
			String lang = settings.get(i).java.getRegionLang();
			if (!seen.contains(lang)) {
				seen.add(lang);
				languages.add(i);
			}
		}
		for (int s : languages) {
			for (String t : TRICKY_PHRASES) {
				phrases.add(new PhraseCase(s, t, Arrays.asList(t, "Main Street", "Frankfurt", "12a"), new ArrayList<>()));
			}
		}
		// phrases made of the words of the cases, split every way the phrase splits words
		Random random = new Random(42);
		String[] glue = {" ", "  ", ",", ", ", " , ", "\t", "-", " - ", ";", "."};
		for (int i = 0; i < 3000; i++) {
			PhraseCase base = phrases.get(random.nextInt(fixturePhrases));
			StringBuilder sb = new StringBuilder();
			int n = 1 + random.nextInt(5);
			for (int w = 0; w < n; w++) {
				if (w > 0) {
					sb.append(glue[random.nextInt(glue.length)]);
				}
				String word = words.get(random.nextInt(words.size()));
				sb.append(random.nextInt(4) == 0 ? word.toUpperCase() : word);
			}
			if (random.nextInt(3) == 0) {
				sb.append(glue[random.nextInt(glue.length)]);
			}
			phrases.add(new PhraseCase(base.settings, sb.toString(), base.names, base.expected));
		}
	}

	/** The copy's settings from json that kotlinx reads: OsmAnd-java sees kotlinx only when it runs. */
	private static net.osmand.shared.search.core.SearchSettings parseCopy(String json) {
		try {
			Object parser = Class.forName("kotlinx.serialization.json.Json").getField("Default").get(null);
			Object element = call(parser, "parseToJsonElement", json);
			return (net.osmand.shared.search.core.SearchSettings) call(
					net.osmand.shared.search.core.SearchSettings.Companion, "parseJSON", element);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** A {@code KTreeSet}, which OsmAnd-java sees only as the set it is when it runs. */
	@SuppressWarnings("unchecked")
	private static Set<String> treeSet(Comparator<String> comparator) {
		try {
			return (Set<String>) Class.forName("net.osmand.shared.util.collections.KTreeSet")
					.getConstructor(Comparator.class).newInstance(comparator);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static int addSettings(String json) {
		settings.add(new Settings(json));
		return settings.size() - 1;
	}

	@AfterClass
	public static void wereCompared() {
		System.out.println("SearchPhraseCompatTest: " + settings.size() + " settings, " + phrases.size()
				+ " phrases, " + selectionsCompared + " selections, " + resultsCompared + " results compared");
	}

	@Test
	public void settingsAreTheSame() {
		for (int i = 0; i < settings.size(); i++) {
			// read anew: the other tests change the settings they share
			Settings s = new Settings(settings.get(i).json);
			String javaLine = settingsLine(s.java);
			assertEquals(s.json, javaLine, settingsLine(s.copy));
			// private, with no getter to read on Kotlin/Native
			assertEquals(s.json, field(s.java, "sortByName"), field(s.copy, "sortByName"));
			dump.add("S " + i + "\t" + CommonWordsCompatTest.hex(s.json) + "\t" + javaLine);
		}
		for (String broken : new String[] {"{\"lat\":\"x\",\"lon\":\"4\"}", "{\"lat\":true,\"lon\":\"4\"}",
				"{\"lang\":5}", "{\"lat\":null,\"lon\":\"4\"}", "{\"searchTypes\":\"POI\"}",
				"{\"searchTypes\":[\"NOT_A_TYPE\"]}", "{\"searchTypes\":[5]}"}) {
			assertEquals(broken, thrown(() -> SearchSettings.parseJSON(new JSONObject(broken))) != null,
					thrown(() -> parseCopy(broken)) != null);
		}
	}

	/** The setters, each on a copy, and where the language of the region comes from. */
	@Test
	public void settingsChangeTheSameWay() {
		for (int i = 0; i < settings.size(); i += 3) {
			SearchSettings j = settings.get(i).java;
			net.osmand.shared.search.core.SearchSettings k = settings.get(i).copy;
			j.setRegions(javaRegions);
			k.setRegions(copyRegions);
			LatLon jl = j.getOriginalLocation() == null ? new LatLon(52.37, 4.89) : j.getOriginalLocation();
			KLatLon kl = new KLatLon(jl.getLatitude(), jl.getLongitude());
			List<Object[]> steps = new ArrayList<>();
			steps.add(new Object[] {j.setLang("de", true), k.setLang("de", true)});
			steps.add(new Object[] {j.setLangs("en", "fr", false), k.setLangs("en", "fr", false)});
			steps.add(new Object[] {j.setRadiusLevel(4), k.setRadiusLevel(4)});
			steps.add(new Object[] {j.setTotalLimit(7), k.setTotalLimit(7)});
			steps.add(new Object[] {j.setOriginalLocation(jl), k.setOriginalLocation(kl)});
			LatLon far = new LatLon(jl.getLatitude() + 0.2, jl.getLongitude() - 0.3);
			LatLon near = new LatLon(jl.getLatitude() + 0.01, jl.getLongitude());
			SearchSettings jmoved = j.setOriginalLocation(jl);
			net.osmand.shared.search.core.SearchSettings kmoved = k.setOriginalLocation(kl);
			steps.add(new Object[] {jmoved.setOriginalLocation(far), kmoved.setOriginalLocation(klatlon(far))});
			steps.add(new Object[] {jmoved.setOriginalLocation(near), kmoved.setOriginalLocation(klatlon(near))});
			steps.add(new Object[] {j.setSearchBBox31(new QuadRect(1, 2, 3, 4)), k.setSearchBBox31(new KQuadRect(1, 2, 3, 4))});
			steps.add(new Object[] {j.setSearchTypes(ObjectType.POI, ObjectType.CITY),
					k.setSearchTypes(net.osmand.shared.search.core.ObjectType.POI, net.osmand.shared.search.core.ObjectType.CITY)});
			steps.add(new Object[] {j.resetSearchTypes(), k.resetSearchTypes()});
			steps.add(new Object[] {j.setEmptyQueryAllowed(true), k.setEmptyQueryAllowed(true)});
			steps.add(new Object[] {j.setSortByName(true), k.setSortByName(true)});
			steps.add(new Object[] {j.setSortByName(false), k.setSortByName(false)});
			SearchSettings jexport = new SearchSettings(j);
			net.osmand.shared.search.core.SearchSettings kexport = new net.osmand.shared.search.core.SearchSettings(k);
			SearchSettings jexported = jexport.setExportSettings(new SearchExportSettings(false, true, 5));
			net.osmand.shared.search.core.SearchSettings kexported = kexport.setExportSettings(
					new net.osmand.shared.search.core.SearchExportSettings(false, true, 5));
			steps.add(new Object[] {jexport, kexport});
			steps.add(new Object[] {jexported, kexported});
			for (Object[] step : steps) {
				assertEquals(settingsLine(step[0]), settingsLine(step[1]));
			}
			SearchSettings jsorted = new SearchSettings(j);
			net.osmand.shared.search.core.SearchSettings ksorted = new net.osmand.shared.search.core.SearchSettings(k);
			jsorted.setSortType(SearchSettings.SortType.ONLY_BY_DISTANCE);
			ksorted.setSortType(net.osmand.shared.search.core.SearchSettings.SortType.ONLY_BY_DISTANCE);
			jsorted.updateSearchTypes(ObjectType.WPT);
			ksorted.updateSearchTypes(net.osmand.shared.search.core.ObjectType.WPT);
			assertEquals(settingsLine(jsorted), settingsLine(ksorted));
		}
	}

	@Test
	public void phrasesAreTheSame() {
		for (int i = 0; i < phrases.size(); i++) {
			PhraseCase c = phrases.get(i);
			SearchPhrase j = javaPhrase(c);
			net.osmand.shared.search.core.SearchPhrase k = copyPhrase(c);
			String javaLine = phraseLine(j, c.names);
			assertEquals(c.text, javaLine, phraseLine(k, c.names));
			assertEquals(c.text, staticLine(c.text, false), staticLine(c.text, true));
			dump.add("P " + i + "\t" + c.settings + "\t" + CommonWordsCompatTest.hex(c.text) + "\t" + hexList(c.names)
					+ "\t" + javaLine + "\t" + staticLine(c.text, false));
		}
	}

	/**
	 * A phrase selects a result for its first words and goes on with the rest, the way the search
	 * does when it finds a street and then looks for the house on it; then more is typed after it.
	 */
	@Test
	public void selectedWordsAreTheSame() {
		selectionsCompared = 0;
		for (int i = 0; i < phrases.size(); i++) {
			PhraseCase c = phrases.get(i);
			for (String spec : specs(c, i, 3)) {
				NotingCollator collator = new NotingCollator();
				SearchPhrase j = javaPhrase(c, collator);
				net.osmand.shared.search.core.SearchPhrase k = copyPhrase(c);
				String javaLine = selectedLine(j, buildJava(spec, j));
				assertEquals(c.text + " " + spec, javaLine, selectedLine(k, buildCopy(spec, k)));
				dump.add("W " + i + "\t" + spec + "\t" + javaLine + "\t" + collator.keys());
				selectionsCompared++;
			}
		}
	}

	@Test
	public void resultsAreTheSame() {
		resultsCompared = 0;
		for (int i = 0; i < phrases.size(); i++) {
			PhraseCase c = phrases.get(i);
			for (String spec : specs(c, i, 12)) {
				NotingCollator collator = new NotingCollator();
				SearchPhrase j = javaPhrase(c, collator);
				net.osmand.shared.search.core.SearchPhrase k = copyPhrase(c);
				String javaLine = resultLine(j, spec, true);
				assertEquals(c.text + " " + spec, javaLine, resultLine(k, spec, false));
				dump.add("R " + i + "\t" + spec + "\t" + javaLine + "\t" + collator.keys());
				resultsCompared++;
			}
		}
		assertTrue("results compared: " + resultsCompared, resultsCompared > 20000);
	}

	@Test
	public void boxesAndRadiiAreTheSame() {
		Random random = new Random(7);
		for (int i = 0; i < 20000; i++) {
			double lat = i < 10 ? new double[] {0, 85.0511, -85.0511, 89.9, -89.9, 60, -60, 0.0001, 45, -45}[i]
					: random.nextDouble() * 170 - 85;
			double lon = i < 10 ? new double[] {0, 180, -180, 179.99, -179.99, 30, -30, 0, 0.5, -0.5}[i]
					: random.nextDouble() * 360 - 180;
			int radius = new int[] {0, 50, 1000, 50050, 500050, 1000050, 5000000}[random.nextInt(7)];
			String javaLine = boxLine(MapUtils.calculate31BboxUsingRhumb(radius, new LatLon(lat, lon)));
			assertEquals(lat + " " + lon + " " + radius, javaLine,
					boxLine(KMapUtils.INSTANCE.calculate31BboxUsingRhumb(radius, new KLatLon(lat, lon))));
			dump.add("B " + bits(lat) + "\t" + bits(lon) + "\t" + radius + "\t" + javaLine);
		}
	}

	/** Which files a phrase searches, by distance, by region and in which order. */
	@Test
	public void filesAreChosenTheSameWay() throws IOException {
		List<File> files = new ArrayList<>(TestObf.searchFiles());
		files.addAll(TestObf.files());
		List<BinaryMapIndexReader> java = new ArrayList<>();
		List<net.osmand.shared.binary.BinaryMapIndexReader> copy = new ArrayList<>();
		try {
			for (File f : files) {
				java.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
				copy.add(new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
			}
			int compared = 0;
			List<String> paths = new ArrayList<>();
			for (File f : files) {
				paths.add(hex(f.getPath()));
			}
			dump.add("O " + String.join(",", paths));
			for (int i = 0; i < phrases.size(); i += 7) {
				PhraseCase c = phrases.get(i);
				Settings s = settings.get(c.settings);
				SearchSettings js = new SearchSettings(s.java);
				net.osmand.shared.search.core.SearchSettings ks = new net.osmand.shared.search.core.SearchSettings(s.copy);
				js.setOfflineIndexes(i % 2 == 0 ? java : java.subList(0, java.size() / 2));
				ks.setOfflineIndexes(i % 2 == 0 ? copy : copy.subList(0, copy.size() / 2));
				SearchPhrase j = SearchPhrase.emptyPhrase(js).generateNewPhrase(c.text, js);
				net.osmand.shared.search.core.SearchPhrase k = net.osmand.shared.search.core.SearchPhrase.Companion
						.emptyPhrase(ks).generateNewPhrase(c.text, ks);
				String javaLine = filesLine(j, java);
				assertEquals(c.text, javaLine, filesLine(k, copy));
				dump.add("F " + i + "\t" + (i % 2 == 0 ? "all" : "half") + "\t" + javaLine);
				compared++;
			}
			assertTrue("phrases with files: " + compared, compared > 100);
		} finally {
			for (BinaryMapIndexReader r : java) {
				r.close();
			}
			for (net.osmand.shared.binary.BinaryMapIndexReader r : copy) {
				r.close();
			}
		}
	}

	@Test
	public void topIndexFiltersAreTheSame() {
		MapPoiTypes jtypes = MapPoiTypes.getDefault();
		net.osmand.shared.osm.MapPoiTypes ktypes = net.osmand.shared.osm.MapPoiTypes.Companion.getDefault();
		String[] tags = {"top_index_brand", "top_index_operator", "top_index_network", "top_index_nothing", "brand"};
		String[] values = {"McDonald's", "mcdonald's", "MCDONALD'S", "Bank of America", "\"Quoted\" Co: Ltd",
				"Café", "CAFÉ", "Сбербанк", "СБЕРБАНК", "İstanbul", "istanbul", "ß", "SS", ""};
		for (String tag : tags) {
			PoiSubType jsub = new PoiSubType();
			jsub.name = tag;
			net.osmand.shared.binary.PoiSubType ksub = new net.osmand.shared.binary.PoiSubType();
			ksub.name = tag;
			for (String value : values) {
				TopIndexFilter j = new TopIndexFilter(jsub, jtypes, value);
				net.osmand.shared.search.core.TopIndexFilter k = new net.osmand.shared.search.core.TopIndexFilter(ksub, ktypes, value);
				String javaLine = topIndexLine(j, jsub, values);
				assertEquals(tag + " " + value, javaLine, topIndexLine(k, ksub, values));
				StringBuilder equal = new StringBuilder();
				for (String other : values) {
					TopIndexFilter jo = new TopIndexFilter(jsub, jtypes, other);
					net.osmand.shared.search.core.TopIndexFilter ko = new net.osmand.shared.search.core.TopIndexFilter(ksub, ktypes, other);
					String javaEqual = j.equals(jo) + " " + (j.hashCode() == jo.hashCode());
					assertEquals(value + " = " + other, javaEqual, k.equals(ko) + " " + (k.hashCode() == ko.hashCode()));
					equal.append(javaEqual).append(',');
				}
				dump.add("T " + hex(tag) + "\t" + hex(value) + "\t" + hexList(Arrays.asList(values)) + "\t" + javaLine + "\t" + equal);
			}
		}
		for (PhraseCase c : phrases) {
			for (String s : c.text.split(" ")) {
				String javaKey = TopIndexFilter.getValueKey(s);
				assertEquals(s, javaKey, net.osmand.shared.search.core.TopIndexFilter.getValueKey(s));
				dump.add("V " + CommonWordsCompatTest.hex(s) + "\t" + CommonWordsCompatTest.hex(javaKey));
			}
		}
	}

	@Test
	public void objectTypesAreTheSame() {
		assertEquals(ObjectType.values().length, net.osmand.shared.search.core.ObjectType.values().length);
		for (ObjectType t : ObjectType.values()) {
			net.osmand.shared.search.core.ObjectType k = net.osmand.shared.search.core.ObjectType.valueOf(t.name());
			assertEquals(t.name(), t.ordinal(), k.ordinal());
			assertEquals(t.name(), t.hasLocation(), k.hasLocation());
			assertEquals(t.name(), ObjectType.isAddress(t), net.osmand.shared.search.core.ObjectType.isAddress(k));
			assertEquals(t.name(), ObjectType.isTopVisible(t), net.osmand.shared.search.core.ObjectType.isTopVisible(k));
			assertEquals(t.name(), ObjectType.getTypeWeight(t), net.osmand.shared.search.core.ObjectType.getTypeWeight(k));
			ObjectType je = ObjectType.getExclusiveSearchType(t);
			net.osmand.shared.search.core.ObjectType ke = net.osmand.shared.search.core.ObjectType.getExclusiveSearchType(k);
			assertEquals(t.name(), je == null ? null : je.name(), ke == null ? null : ke.name());
		}
		assertEquals(ObjectType.getTypeWeight(null), net.osmand.shared.search.core.ObjectType.getTypeWeight(null));
	}

	/** java's {@code TreeSet} with the collator, as the phrase keeps the words a result matched. */
	@Test
	public void treeSetIsJavasTreeSet() {
		SearchPhrase j = SearchPhrase.emptyPhrase();
		net.osmand.shared.search.core.SearchPhrase k = net.osmand.shared.search.core.SearchPhrase.emptyPhrase();
		List<String> words = new ArrayList<>();
		for (PhraseCase c : phrases.subList(0, 300)) {
			words.addAll(Arrays.asList(c.text.split(" ")));
		}
		Random random = new Random(3);
		for (int round = 0; round < 500; round++) {
			TreeSet<String> jset = new TreeSet<>(j.getCollator());
			Set<String> kset = treeSet((a, b) -> k.getCollator().compare(a, b));
			for (int op = 0; op < 40; op++) {
				String w = words.get(random.nextInt(words.size()));
				if (random.nextInt(3) == 0) {
					w = w.toUpperCase();
				}
				switch (random.nextInt(4)) {
					case 0:
						assertEquals("remove " + w, jset.remove(w), kset.remove(w));
						break;
					case 1:
						assertEquals("contains " + w, jset.contains(w), kset.contains(w));
						break;
					default:
						assertEquals("add " + w, jset.add(w), kset.add(w));
				}
				assertEquals(new ArrayList<>(jset), new ArrayList<>(kset));
			}
			Iterator<String> it = kset.iterator();
			Iterator<String> jt = jset.iterator();
			while (it.hasNext()) {
				String w = it.next();
				assertEquals(jt.next(), w);
				if (w.length() % 2 == 0) {
					it.remove();
					jt.remove();
				}
			}
			assertEquals(new ArrayList<>(jset), new ArrayList<>(kset));
		}
	}

	@Test
	public void javaDumpIsWritten() throws IOException {
		dump.clear();
		settingsAreTheSame();
		phrasesAreTheSame();
		selectedWordsAreTheSame();
		resultsAreTheSame();
		boxesAndRadiiAreTheSame();
		topIndexFiltersAreTheSame();
		filesAreChosenTheSameWay();
		JAVA_DUMP.getParentFile().mkdirs();
		try (java.io.BufferedWriter out = Files.newBufferedWriter(JAVA_DUMP.toPath(), StandardCharsets.UTF_8)) {
			for (String line : dump) {
				out.write(line);
				out.write('\n');
			}
		}
	}

	// the phrases

	private static SearchPhrase javaPhrase(PhraseCase c) {
		SearchSettings s = settings.get(c.settings).java;
		return SearchPhrase.emptyPhrase(s).generateNewPhrase(c.text, s);
	}

	private static SearchPhrase javaPhrase(PhraseCase c, net.osmand.Collator collator) {
		SearchSettings s = settings.get(c.settings).java;
		return SearchPhrase.emptyPhrase(s, collator).generateNewPhrase(c.text, s);
	}

	/**
	 * java's collator, which notes the words it compares: the dump carries their collation keys,
	 * so that {@code SearchPhraseTest} can compare them on Kotlin/Native the way java did, apart
	 * from what the collator of the platform says.
	 */
	static final class NotingCollator implements net.osmand.Collator {
		private final net.osmand.Collator collator = OsmAndCollator.primaryCollator();
		private final java.text.Collator keys = keyCollator();
		private final Set<String> words = new LinkedHashSet<>();

		@Override
		public int compare(Object o1, Object o2) {
			return compare((String) o1, (String) o2);
		}

		@Override
		public int compare(String source, String target) {
			words.add(source);
			words.add(target);
			return collator.compare(source, target);
		}

		@Override
		public boolean equals(String source, String target) {
			words.add(source);
			words.add(target);
			return collator.equals(source, target);
		}

		/** {@code word:key,...}, the keys ordered as the collator orders their words. */
		String keys() {
			List<String> list = new ArrayList<>(words);
			// the keys order the words as the collator does: in its order, each pair of neighbours agrees
			List<String> sorted = new ArrayList<>(list);
			sorted.sort(collator::compare);
			for (int i = 1; i < sorted.size(); i++) {
				String a = sorted.get(i - 1);
				String b = sorted.get(i);
				assertEquals(a + " " + b, Integer.signum(collator.compare(a, b)),
						Integer.signum(keys.getCollationKey(a).compareTo(keys.getCollationKey(b))));
			}
			StringBuilder sb = new StringBuilder();
			for (String w : list) {
				StringBuilder key = new StringBuilder();
				for (byte b : keys.getCollationKey(w).toByteArray()) {
					key.append(String.format("%02x", b & 0xff));
				}
				sb.append(sb.length() > 0 ? "," : "").append(hex(w)).append(':').append(key.length() == 0 ? "-" : key);
			}
			return sb.length() == 0 ? "-" : sb.toString();
		}

		/** The collator {@code OsmAndCollator.primaryCollator} wraps. */
		private static java.text.Collator keyCollator() {
			String language = java.util.Locale.getDefault().getLanguage();
			java.text.Collator c = language.equals("ro") || language.equals("cs") || language.equals("sk")
					? java.text.Collator.getInstance(java.util.Locale.US) : java.text.Collator.getInstance();
			c.setStrength(java.text.Collator.PRIMARY);
			return c;
		}
	}

	private static net.osmand.shared.search.core.SearchPhrase copyPhrase(PhraseCase c) {
		net.osmand.shared.search.core.SearchSettings s = settings.get(c.settings).copy;
		return net.osmand.shared.search.core.SearchPhrase.emptyPhrase(s).generateNewPhrase(c.text, s);
	}

	/** Every getter of the phrase, and what the matchers of its words say about [names]. */
	static String phraseLine(Object p, List<String> names) {
		StringBuilder sb = new StringBuilder();
		for (String getter : new String[] {"getFullSearchPhrase", "getUnknownSearchPhrase", "getFirstUnknownSearchWord",
				"getUnknownSearchWords", "isLastUnknownSearchWordComplete", "isFirstUnknownSearchWordComplete",
				"hasMoreThanOneUnknownSearchWord", "isUnknownSearchWordPresent", "getLastUnknownSearchWord",
				"getUnknownWordToSearch", "isMainUnknownSearchWordComplete", "getUnknownWordToSearchBuilding",
				"getTextWithoutLastWord", "getStringRerpresentation", "isEmpty", "isNoSelectedType", "getWords",
				"getWordLocation", "getLastTokenLocation", "get1km31Rect", "getRadiusLevel"}) {
			sb.append(str(call(p, getter))).append(' ');
		}
		sb.append(str(call(p, "getText", true))).append(' ').append(str(call(p, "getText", false))).append(' ');
		sb.append(str(call(p, "getRadiusSearch", 1000))).append(' ').append(str(call(p, "getNextRadiusSearch", 1000))).append(' ');
		sb.append(str(call(p, "getRadiusBBoxToSearch", 5000))).append(' ');
		int others = ((List<?>) call(p, "getUnknownSearchWords")).size();
		for (String name : names) {
			sb.append(matches(call(p, "getMainUnknownNameStringMatcher"), name));
			sb.append(matches(call(p, "getFirstUnknownNameStringMatcher"), name));
			sb.append(matches(call(p, "getUnknownWordToSearchBuildingNameMatcher"), name));
			for (int i = 0; i < others; i++) {
				sb.append(matches(call(p, "getUnknownNameStringMatcher", i), name));
			}
			sb.append(',');
		}
		return sb.toString();
	}

	private static char matches(Object matcher, String name) {
		return Boolean.TRUE.equals(call(matcher, "matches", name)) ? '1' : '0';
	}

	/** The static helpers of the phrase on [text]. */
	static String staticLine(String text, boolean ofCopy) {
		List<String> words = new ArrayList<>(Arrays.asList(text.split(" ")));
		if (ofCopy) {
			net.osmand.shared.search.core.SearchPhrase.Companion k = net.osmand.shared.search.core.SearchPhrase.Companion;
			return k.countWords(text) + " " + str(k.splitWords(text, new ArrayList<>(), SearchPhrase.ALLDELIMITERS))
					+ " " + str(k.splitWords(text, new ArrayList<>(), SearchPhrase.ALLDELIMITERS_WITH_HYPHEN))
					+ " " + str(k.splitWords(text, new ArrayList<>(), "-"))
					+ " " + str(k.stripBraces(text)) + " " + str(k.stripBraces(words))
					+ " " + str(k.selectMainUnknownWordToSearch(words)) + " " + str(words);
		}
		return SearchPhrase.countWords(text) + " " + str(SearchPhrase.splitWords(text, new ArrayList<>(), SearchPhrase.ALLDELIMITERS))
				+ " " + str(SearchPhrase.splitWords(text, new ArrayList<>(), SearchPhrase.ALLDELIMITERS_WITH_HYPHEN))
				+ " " + str(SearchPhrase.splitWords(text, new ArrayList<>(), "-"))
				+ " " + str(SearchPhrase.stripBraces(text)) + " " + str(SearchPhrase.stripBraces(words))
				+ " " + str(SearchPhrase.selectMainUnknownWordToSearch(words)) + " " + str(words);
	}

	/** The phrase after the result is selected, what it matched, and the phrase typed on after it. */
	static String selectedLine(Object phrase, Object res) {
		StringBuilder sb = new StringBuilder();
		sb.append(str(call(phrase, "countUnknownWordsMatchMainResult", res))).append(' ');
		@SuppressWarnings("unchecked")
		List<String> left = (List<String>) call(res, "filterUnknownSearchWord", (Object) null);
		sb.append(str(left)).append(' ');
		String last = (String) call(phrase, "getLastUnknownSearchWord");
		boolean lastComplete = (Boolean) call(phrase, "isLastUnknownSearchWordComplete") || !left.contains(last);
		Object selected = call(phrase, "selectWord", res, left, lastComplete);
		sb.append(phraseLine(selected, new ArrayList<>())).append(" | ");
		Object plain = call(phrase, "selectWord", res, call(phrase, "getSettings"));
		sb.append(phraseLine(plain, new ArrayList<>())).append(" | ");
		String typed = call(selected, "getText", true) + "12 ";
		Object next = call(selected, "generateNewPhrase", typed, call(selected, "getSettings"));
		sb.append(phraseLine(next, new ArrayList<>())).append(" | ");
		Object shorter = call(selected, "generateNewPhrase", typed.substring(0, typed.length() / 2), call(selected, "getSettings"));
		sb.append(phraseLine(shorter, new ArrayList<>())).append(" | ");
		call(next, "syncWordsWithResults");
		sb.append(str(call(next, "getWords"))).append(' ').append(str(call(next, "getExclusiveSearchType")));
		for (String t : new String[] {"CITY", "STREET", "HOUSE", "POI", "UNKNOWN_NAME_FILTER"}) {
			sb.append(' ').append(str(call(next, "hasObjectType", objectType(next, t))));
			sb.append(str(call(next, "isLastWord", typesArray(next, t))));
		}
		return sb.toString();
	}

	private static Object objectType(Object phrase, String name) {
		return phrase instanceof SearchPhrase ? ObjectType.valueOf(name) : net.osmand.shared.search.core.ObjectType.valueOf(name);
	}

	private static Object typesArray(Object phrase, String name) {
		if (phrase instanceof SearchPhrase) {
			return new ObjectType[] {ObjectType.valueOf(name)};
		}
		return new net.osmand.shared.search.core.ObjectType[] {net.osmand.shared.search.core.ObjectType.valueOf(name)};
	}

	/**
	 * The phrase counts its words in a result and weighs it; then the same with some words already
	 * matched and another name; then the braces of its names are taken off and put back.
	 */
	static String resultLine(Object phrase, String spec, boolean ofJava) {
		StringBuilder sb = new StringBuilder();
		Object res = ofJava ? buildJava(spec, (SearchPhrase) phrase) : buildCopy(spec, (net.osmand.shared.search.core.SearchPhrase) phrase);
		sb.append(str(call(phrase, "countUnknownWordsMatchMainResult", res))).append(' ');
		sb.append(resultState(res)).append(' ');
		Object location = call(call(phrase, "getSettings"), "getOriginalLocation");
		sb.append(str(call(res, "getSearchDistance", location))).append(' ');
		sb.append(str(call(res, "getSearchDistance", location, 0.001))).append(" | ");

		Object res2 = ofJava ? buildJava(spec, (SearchPhrase) phrase) : buildCopy(spec, (net.osmand.shared.search.core.SearchPhrase) phrase);
		sb.append(str(call(phrase, "countUnknownWordsMatchMainResult", res2, "Main Street", 2))).append(' ');
		sb.append(resultState(res2)).append(" | ");

		Object res3 = ofJava ? buildJava(spec, (SearchPhrase) phrase) : buildCopy(spec, (net.osmand.shared.search.core.SearchPhrase) phrase);
		Object backup = call(res3, "stripBracesNames");
		sb.append(str(backup)).append(' ').append(names(res3)).append(' ');
		call(res3, "restoreBraceNames", backup);
		sb.append(names(res3));
		return sb.toString();
	}

	private static String resultState(Object res) {
		Object complete = call(res, "getCompleteMatchRes");
		return str(call(res, "getUnknownPhraseMatchWeight")) + " "
				+ str(field(complete, "allWordsEqual")) + str(field(complete, "allWordsInPhraseAreInResult")) + " "
				+ str(call(res, "getFoundWordCount")) + " " + str(call(res, "getOtherWordsMatch")) + " "
				+ str(call(res, "filterUnknownSearchWord", (Object) null)) + " " + str(call(res, "getDepth")) + " "
				+ str(call(res, "toString")) + " " + str(call(res, "isFullPhraseEqualLocaleName")) + " "
				+ str(call(res, "getResourceType"));
	}

	private static String names(Object res) {
		return str(field(res, "localeName")) + " " + str(field(res, "alternateName")) + " " + str(field(res, "otherNames"));
	}

	// the results

	/**
	 * Results made of the names of the results a case expects, or of the words of the phrase: a
	 * settlement, a street in one, a house on a street, a poi; near the place the search is run
	 * from and far from it, and under a parent of the same name or of another one.
	 */
	private static List<String> specs(PhraseCase c, int index, int max) {
		List<String> specs = new ArrayList<>();
		Settings s = settings.get(c.settings);
		LatLon origin = s.java.getOriginalLocation() == null ? new LatLon(40.7, -74.0) : s.java.getOriginalLocation();
		List<String> names = new ArrayList<>();
		for (String e : c.expected) {
			names.add(stripWeight(e));
		}
		names.add(c.text.trim());
		String[] words = c.text.trim().split(" ");
		if (words.length > 1) {
			names.add(words[words.length - 1] + ", " + words[0]);
		}
		for (int n = 0; n < names.size() && specs.size() < max; n++) {
			String[] parts = names.get(n).split(", ");
			String type = n < c.expected.size() ? typeOf(c.expected.get(n)) : RESULT_TYPES[(index + n) % RESULT_TYPES.length];
			double km = n < c.expected.size() ? kmOf(c.expected.get(n)) : (index + n) % 4 * 11.5;
			LatLon at = MapUtils.rhumbDestinationPoint(origin, km * 1000, 45 + 90 * (n % 4));
			String city = parts[parts.length - 1];
			String self = level(type, parts[0], parts.length > 2 ? parts[1] : "-", parts.length > 1 ? parts[1] + ";" + parts[0].toUpperCase() : "-",
					parts.length > 1 ? city : "-", at, objectOf(type, parts, at, index + n));
			StringBuilder spec = new StringBuilder(self);
			if (parts.length > 2 && ("HOUSE".equals(type) || "STREET_INTERSECTION".equals(type))) {
				spec.append('|').append(level("STREET", parts[1], "-", "-", city, at, "S:" + hex(city) + ":" + bits(at.getLatitude()) + ":" + bits(at.getLongitude()) + ":-"));
			}
			if (parts.length > 1) {
				String parentCity = (index + n) % 3 == 0 ? city + "burg" : city;
				LatLon cityAt = MapUtils.rhumbDestinationPoint(at, 2500, 200);
				spec.append('|').append(level("CITY", parentCity, "-", "-", "-", cityAt, cityObject(parentCity, cityAt, (index + n) % 2 == 0)));
			}
			specs.add(spec.toString());
		}
		return specs;
	}

	private static String objectOf(String type, String[] parts, LatLon at, int n) {
		String city = parts[parts.length - 1];
		switch (type) {
			case "CITY":
			case "VILLAGE":
			case "BOUNDARY":
			case "POSTCODE":
				return cityObject(parts[0], at, n % 2 == 0);
			case "STREET":
			case "HOUSE":
			case "STREET_INTERSECTION":
				return "S:" + hex(city) + ":" + bits(at.getLatitude() + 0.01) + ":" + bits(at.getLongitude()) + ":" + (n % 2 == 0 ? bbox(at) : "-");
			case "POI":
				String[][] pois = {{"sustenance", "restaurant", "-"}, {"transportation", "bus_stop", "-"},
						{"osmwiki", "wiki_place", "-"}, {"routes", "route_article", "2400"}, {"tourism", "museum", "3900"}};
				String[] poi = pois[n % pois.length];
				return "A:" + hex(poi[0]) + ":" + hex(poi[1]) + ":" + ("-".equals(poi[2]) ? "-" : hex(poi[2]));
			case "LOCATION":
				String[][] poiTypes = {{"type", "restaurant"}, {"category", "shop"}, {"filter", "sustenance"}, {"type", "bus_stop"}};
				String[] pt = poiTypes[n % poiTypes.length];
				return "T:" + pt[0] + ":" + hex(pt[1]);
			default:
				return "-";
		}
	}

	private static String cityObject(String name, LatLon at, boolean withBox) {
		return "C:" + hex(name) + ":" + bits(at.getLatitude()) + ":" + bits(at.getLongitude()) + ":" + (withBox ? bbox(at) : "-");
	}

	private static String bbox(LatLon at) {
		QuadRect r = MapUtils.calculate31BboxUsingRhumb(3000, at);
		return (int) r.left + "," + (int) r.top + "," + (int) r.right + "," + (int) r.bottom;
	}

	private static String level(String type, String name, String alt, String others, String cityName, LatLon at, String object) {
		return type + ";" + hex(name) + ";" + ("-".equals(alt) ? "-" : hex(alt)) + ";"
				+ ("-".equals(others) ? "-" : hexList(Arrays.asList(others.split(";")))) + ";"
				+ ("-".equals(cityName) ? "-" : hex(cityName)) + ";" + bits(at.getLatitude()) + ";" + bits(at.getLongitude()) + ";" + object;
	}

	/** A result of java's, its parents after it, from the text {@link #specs} makes. */
	static SearchResult buildJava(String spec, SearchPhrase phrase) {
		SearchResult first = null;
		SearchResult child = null;
		for (String levelSpec : spec.split("\\|")) {
			String[] f = levelSpec.split(";", -1);
			SearchResult r = new SearchResult(phrase);
			r.objectType = ObjectType.valueOf(f[0]);
			r.localeName = unhex(f[1]);
			r.alternateName = "-".equals(f[2]) ? null : unhex(f[2]);
			r.otherNames = "-".equals(f[3]) ? null : unhexList(f[3]);
			r.cityName = "-".equals(f[4]) ? null : unhex(f[4]);
			r.location = new LatLon(unbits(f[5]), unbits(f[6]));
			String[] o = f[7].split(":", -1);
			switch (o[0]) {
				case "C":
					r.object = javaCity(o);
					break;
				case "S":
					Street street = new Street(javaCity(o));
					street.setName(r.localeName);
					street.setLocation(r.location.getLatitude(), r.location.getLongitude());
					r.object = street;
					if (r.objectType == ObjectType.STREET_INTERSECTION) {
						r.localeRelatedObjectName = r.cityName;
						r.relatedObject = street;
					}
					break;
				case "A":
					Amenity a = new Amenity();
					a.setType(MapPoiTypes.getDefault().getPoiCategoryByName(unhex(o[1])));
					a.setSubType(unhex(o[2]));
					a.setName(r.localeName);
					a.setLocation(r.location);
					if (!"-".equals(o[3])) {
						a.setAdditionalInfo(Amenity.TRAVEL_ELO, unhex(o[3]));
					}
					r.object = a;
					break;
				case "T":
					PoiCategory category = MapPoiTypes.getDefault().getPoiCategoryByName(unhex(o[2]));
					r.object = "type".equals(o[1]) ? MapPoiTypes.getDefault().getAnyPoiTypeByKey(unhex(o[2]))
							: "category".equals(o[1]) ? category : category.getPoiFilters().get(0);
					break;
				default:
			}
			if (first == null) {
				first = r;
			} else {
				child.parentSearchResult = r;
			}
			child = r;
		}
		return first;
	}

	private static City javaCity(String[] o) {
		City city = new City(City.CityType.CITY);
		city.setName(unhex(o[1]));
		city.setLocation(unbits(o[2]), unbits(o[3]));
		if (!"-".equals(o[4])) {
			String[] b = o[4].split(",");
			city.setBbox31(new int[] {Integer.parseInt(b[0]), Integer.parseInt(b[1]), Integer.parseInt(b[2]), Integer.parseInt(b[3])});
		}
		return city;
	}

	/** The same result of the copy's. */
	static net.osmand.shared.search.core.SearchResult buildCopy(String spec, net.osmand.shared.search.core.SearchPhrase phrase) {
		net.osmand.shared.search.core.SearchResult first = null;
		net.osmand.shared.search.core.SearchResult child = null;
		net.osmand.shared.osm.MapPoiTypes types = net.osmand.shared.osm.MapPoiTypes.Companion.getDefault();
		for (String levelSpec : spec.split("\\|")) {
			String[] f = levelSpec.split(";", -1);
			net.osmand.shared.search.core.SearchResult r = new net.osmand.shared.search.core.SearchResult(phrase);
			r.objectType = net.osmand.shared.search.core.ObjectType.valueOf(f[0]);
			r.localeName = unhex(f[1]);
			r.alternateName = "-".equals(f[2]) ? null : unhex(f[2]);
			r.otherNames = "-".equals(f[3]) ? null : unhexList(f[3]);
			r.cityName = "-".equals(f[4]) ? null : unhex(f[4]);
			r.location = new KLatLon(unbits(f[5]), unbits(f[6]));
			String[] o = f[7].split(":", -1);
			switch (o[0]) {
				case "C":
					r.object = copyCity(o);
					break;
				case "S":
					net.osmand.shared.data.Street street = new net.osmand.shared.data.Street(copyCity(o));
					street.setName(r.localeName);
					street.setLocation(r.location.getLatitude(), r.location.getLongitude());
					r.object = street;
					if (r.objectType == net.osmand.shared.search.core.ObjectType.STREET_INTERSECTION) {
						r.localeRelatedObjectName = r.cityName;
						r.relatedObject = street;
					}
					break;
				case "A":
					net.osmand.shared.data.Amenity a = new net.osmand.shared.data.Amenity();
					a.setType(types.getPoiCategoryByName(unhex(o[1])));
					a.setSubType(unhex(o[2]));
					a.setName(r.localeName);
					a.setLocation(r.location);
					if (!"-".equals(o[3])) {
						a.setAdditionalInfo(net.osmand.shared.data.Amenity.TRAVEL_ELO, unhex(o[3]));
					}
					r.object = a;
					break;
				case "T":
					net.osmand.shared.osm.PoiCategory category = types.getPoiCategoryByName(unhex(o[2]));
					r.object = "type".equals(o[1]) ? types.getAnyPoiTypeByKey(unhex(o[2]))
							: "category".equals(o[1]) ? category : category.getPoiFilters().get(0);
					break;
				default:
			}
			if (first == null) {
				first = r;
			} else {
				child.parentSearchResult = r;
			}
			child = r;
		}
		return first;
	}

	private static net.osmand.shared.data.City copyCity(String[] o) {
		net.osmand.shared.data.City city = new net.osmand.shared.data.City(net.osmand.shared.data.CityType.CITY);
		city.setName(unhex(o[1]));
		city.setLocation(unbits(o[2]), unbits(o[3]));
		if (!"-".equals(o[4])) {
			String[] b = o[4].split(",");
			city.setBbox31(new int[] {Integer.parseInt(b[0]), Integer.parseInt(b[1]), Integer.parseInt(b[2]), Integer.parseInt(b[3])});
		}
		return city;
	}

	private static String typeOf(String expected) {
		int i = expected.indexOf("[[");
		if (i >= 0) {
			String[] f = expected.substring(i + 2).split(", ");
			for (String t : RESULT_TYPES) {
				if (f.length > 1 && f[1].equals(t)) {
					return t;
				}
			}
			if (f.length > 1 && "POI_TYPE".equals(f[1])) {
				return "POI";
			}
		}
		return "STREET";
	}

	private static double kmOf(String expected) {
		int i = expected.lastIndexOf(", ");
		int j = expected.lastIndexOf(" km");
		if (expected.contains("[[") && i >= 0 && j > i) {
			try {
				return Double.parseDouble(expected.substring(i + 2, j));
			} catch (NumberFormatException e) {
				return 3;
			}
		}
		return 3;
	}

	private static String stripWeight(String expected) {
		int i = expected.indexOf('[');
		return (i >= 0 ? expected.substring(0, i) : expected).trim();
	}

	// the files

	private static String filesLine(Object phrase, List<?> readers) {
		StringBuilder sb = new StringBuilder();
		Object[] types = phrase instanceof SearchPhrase ? SearchPhraseDataType.values()
				: net.osmand.shared.search.core.SearchPhrase.SearchPhraseDataType.values();
		for (Object dt : types) {
			for (int meters : new int[] {0, 1000, 20000, 400000}) {
				sb.append(fileNames(call(phrase, "getRadiusOfflineIndexes", meters, dt), readers)).append(' ');
			}
			sb.append(fileNames(call(phrase, "getRadiusOfflineIndexes", 0, 60000, dt), readers)).append(' ');
			sb.append(fileNames(call(phrase, "getRadiusOfflineIndexes", 100000, 300000, dt), readers)).append(' ');
		}
		for (Object r : readers) {
			sb.append(call(phrase, "getRegionPriority", r)).append(',');
		}
		call(phrase, "sortFiles");
		sb.append(' ').append(fileNames(((List<?>) call(phrase, "getOfflineIndexes")).iterator(), readers));
		call(phrase, "selectFile", readers.get(0));
		sb.append(' ').append(fileNames(((List<?>) call(phrase, "getOfflineIndexes")).iterator(), readers));
		return sb.toString();
	}

	/** The files as their places in [readers], which the dump lists by path. */
	private static String fileNames(Object iterator, List<?> readers) {
		StringBuilder sb = new StringBuilder("[");
		Iterator<?> it = (Iterator<?>) iterator;
		while (it.hasNext()) {
			Object r = it.next();
			int i = 0;
			while (readers.get(i) != r) {
				i++;
			}
			sb.append(i).append(',');
		}
		return sb.append(']').toString();
	}

	// the rest

	static String settingsLine(Object s) {
		StringBuilder sb = new StringBuilder();
		for (String getter : new String[] {"getOriginalLocation", "getRegionLang", "getRadiusLevel", "getTotalLimit",
				"getAppLang", "getLang", "isTransliterate", "getSearchTypes", "isCustomSearch", "isEmptyQueryAllowed",
				"getSearchBBox31", "getSortType", "isExportObjects", "hasRegionPriority", "getOfflineIndexes"}) {
			sb.append(str(call(s, getter))).append(' ');
		}
		Object export = call(s, "getExportSettings");
		if (export != null) {
			sb.append(str(call(export, "isExportEmptyCities"))).append(str(call(export, "isExportBuildings")))
					.append(str(call(export, "getMaxDistance")));
		}
		for (String t : new String[] {"POI", "CITY", "WPT"}) {
			Object type = s instanceof SearchSettings ? ObjectType.valueOf(t) : net.osmand.shared.search.core.ObjectType.valueOf(t);
			sb.append(' ').append(str(call(s, "hasCustomSearchType", type)));
		}
		return sb.toString();
	}

	private static String boxLine(Object rect) {
		return str(rect);
	}

	private static String topIndexLine(Object filter, Object subType, String[] values) {
		StringBuilder sb = new StringBuilder();
		sb.append(str(call(filter, "getTag"))).append(' ').append(str(call(filter, "getFilterId"))).append(' ')
				.append(str(call(filter, "getName"))).append(' ').append(str(call(filter, "getIconResource"))).append(' ')
				.append(str(call(filter, "getValue"))).append(' ');
		for (String v : values) {
			sb.append(str(call(filter, "accept", subType, v)));
		}
		return sb.toString();
	}

	private static KLatLon klatlon(LatLon l) {
		return new KLatLon(l.getLatitude(), l.getLongitude());
	}

	/** Any value either side returns, written the same way for both. */
	static String str(Object v) {
		if (v == null) {
			return "null";
		}
		if (v instanceof String) {
			return CommonWordsCompatTest.hex((String) v);
		}
		if (v instanceof Double) {
			return bits((Double) v);
		}
		if (v instanceof LatLon) {
			return "(" + bits(((LatLon) v).getLatitude()) + "," + bits(((LatLon) v).getLongitude()) + ")";
		}
		if (v instanceof KLatLon) {
			return "(" + bits(((KLatLon) v).getLatitude()) + "," + bits(((KLatLon) v).getLongitude()) + ")";
		}
		if (v instanceof QuadRect) {
			QuadRect r = (QuadRect) v;
			return "(" + bits(r.left) + "," + bits(r.top) + "," + bits(r.right) + "," + bits(r.bottom) + ")";
		}
		if (v instanceof KQuadRect) {
			KQuadRect r = (KQuadRect) v;
			return "(" + bits(r.getLeft()) + "," + bits(r.getTop()) + "," + bits(r.getRight()) + "," + bits(r.getBottom()) + ")";
		}
		if (v instanceof Enum) {
			return ((Enum<?>) v).name();
		}
		if (v.getClass().getSimpleName().equals("SearchWord")) {
			return str(call(v, "getWord")) + ":" + str(call(v, "getType")) + ":" + str(call(v, "getLocation"));
		}
		if (v instanceof BinaryMapIndexReader) {
			return ((BinaryMapIndexReader) v).getFile().getName();
		}
		if (v instanceof net.osmand.shared.binary.BinaryMapIndexReader) {
			return ((net.osmand.shared.binary.BinaryMapIndexReader) v).getFile().name();
		}
		if (v instanceof Collection) {
			StringBuilder sb = new StringBuilder("[");
			for (Object o : (Collection<?>) v) {
				sb.append(str(o)).append(',');
			}
			return sb.append(']').toString();
		}
		if (v.getClass().isArray()) {
			StringBuilder sb = new StringBuilder("[");
			for (int i = 0; i < Array.getLength(v); i++) {
				sb.append(str(Array.get(v, i))).append(',');
			}
			return sb.append(']').toString();
		}
		return v.toString();
	}

	static String bits(double v) {
		return Long.toHexString(Double.doubleToRawLongBits(v));
	}

	static double unbits(String s) {
		return Double.longBitsToDouble(Long.parseUnsignedLong(s, 16));
	}

	static String hex(String s) {
		return CommonWordsCompatTest.hex(s);
	}

	static String unhex(String h) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < h.length(); i += 4) {
			sb.append((char) Integer.parseInt(h.substring(i, i + 4), 16));
		}
		return sb.toString();
	}

	static String hexList(List<String> list) {
		if (list.isEmpty()) {
			return "-";
		}
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(sb.length() > 0 ? "," : "").append(hex(s));
		}
		return sb.toString();
	}

	static List<String> unhexList(String h) {
		List<String> list = new ArrayList<>();
		if (!"-".equals(h)) {
			for (String s : h.split(",")) {
				list.add(unhex(s));
			}
		}
		return list;
	}

	/**
	 * Calls a public method by name on either side, or java's package private {@code selectWord},
	 * or the copy's internal one, which the jvm knows by a longer name.
	 */
	static Object call(Object o, String name, Object... args) {
		StringBuilder key = new StringBuilder(o.getClass().getName()).append('.').append(name);
		for (Object a : args) {
			key.append(',').append(a == null ? "null" : a.getClass().getName());
		}
		Method found = METHODS.get(key.toString());
		for (Class<?> c = o.getClass(); c != null && found == null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				boolean named = m.getName().equals(name) || m.getName().startsWith(name + "$");
				if (named && m.getParameterCount() == args.length && fits(m.getParameterTypes(), args)) {
					found = m;
					found.setAccessible(true);
					METHODS.put(key.toString(), found);
					break;
				}
			}
		}
		if (found == null) {
			throw new AssertionError("no " + name + " with " + args.length + " arguments in " + o.getClass());
		}
		try {
			return found.invoke(o, args);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(name + ": " + e.getCause(), e.getCause() == null ? e : e.getCause());
		}
	}

	private static boolean fits(Class<?>[] types, Object[] args) {
		for (int i = 0; i < types.length; i++) {
			Class<?> t = types[i];
			Object a = args[i];
			if (a == null) {
				if (t.isPrimitive()) {
					return false;
				}
				continue;
			}
			if (t.isPrimitive()) {
				Map<Class<?>, Class<?>> boxes = new LinkedHashMap<>();
				boxes.put(int.class, Integer.class);
				boxes.put(double.class, Double.class);
				boxes.put(boolean.class, Boolean.class);
				if (boxes.get(t) != a.getClass()) {
					return false;
				}
			} else if (!t.isInstance(a)) {
				return false;
			}
		}
		return true;
	}

	private static final Map<String, Method> METHODS = new java.util.concurrent.ConcurrentHashMap<>();
	private static final Map<String, java.lang.reflect.Field> FIELDS = new java.util.concurrent.ConcurrentHashMap<>();

	static Object field(Object o, String name) {
		java.lang.reflect.Field cached = FIELDS.get(o.getClass().getName() + "." + name);
		if (cached != null) {
			try {
				return cached.get(o);
			} catch (IllegalAccessException e) {
				throw new AssertionError(e);
			}
		}
		for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
			try {
				java.lang.reflect.Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				FIELDS.put(o.getClass().getName() + "." + name, f);
				return f.get(o);
			} catch (NoSuchFieldException e) {
				// up the classes
			} catch (IllegalAccessException e) {
				throw new AssertionError(e);
			}
		}
		throw new AssertionError("no field " + name + " in " + o.getClass());
	}

	private static Throwable thrown(Runnable r) {
		try {
			r.run();
			return null;
		} catch (RuntimeException | AssertionError e) {
			return e;
		}
	}
}
