package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.shared.api.KStringMatcherMode;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The name index of the poi section: {@link net.osmand.shared.binary.BinaryMapIndexReader} against
 * {@link BinaryMapIndexReader}, asked the same questions about the same files.
 *
 * The queries are built out of the names the files actually hold, so that they hit something: each
 * name in full, its first letters, its first two words, and the route ids of any track stored as an
 * amenity. Every query runs in each matcher mode, since the mode decides both which branches of the
 * index are walked and which amenities survive the second pass over the blocks.
 *
 * <b>The results are compared as a set, not as a sequence.</b> The reader reads the blocks nearest
 * first, and blocks at the same distance are left by java in the order its hash map happened to
 * hand them out - an order no other map reproduces. The copy breaks those ties by file offset
 * instead, which is deterministic and reads forward; {@link #copyOrderIsStable} shows that it is
 * the same order every time. Over the files here the two sequences differ in a few percent of the
 * searches and the sets never do, which is what the counters printed at the end say.
 */
@RunWith(Parameterized.class)
public class PoiNameSearchCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static int searchesCompared;
	private static int amenitiesFound;
	private static int orderDiffered;

	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;

	@BeforeClass
	public static void readPoiTypes() {
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
	}

	public PoiNameSearchCompatTest(String name, File file) throws IOException {
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@AfterClass
	public static void searchesWereCompared() {
		System.out.println("PoiNameSearchCompatTest: " + searchesCompared + " searches compared, "
				+ amenitiesFound + " amenities found by name, " + orderDiffered
				+ " of them in a different order");
		assertTrue("searches compared: " + searchesCompared, searchesCompared > 500);
		assertTrue("amenities found: " + amenitiesFound, amenitiesFound > 100);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() {
		List<Object[]> data = new ArrayList<>();
		for (File file : TestObf.files()) {
			data.add(new Object[] {file.getName(), file});
		}
		return data;
	}

	/** Every query in every mode, over the whole file. */
	@Test
	public void nameSearchIsTheSame() throws IOException {
		for (String query : queries()) {
			for (StringMatcherMode mode : StringMatcherMode.values()) {
				String m = "query '" + query + "' mode " + mode;
				List<Amenity> jamenities = searchJava(query, mode, 0, 0);
				List<net.osmand.shared.data.Amenity> kamenities =
						searchCopy(query, KStringMatcherMode.valueOf(mode.name()), 0, 0);
				assertSame(m, jamenities, kamenities);
				searchesCompared++;
				amenitiesFound += jamenities.size();
			}
		}
	}

	/** The copy hands the results back in the same order every time, ties and all. */
	@Test
	public void copyOrderIsStable() throws IOException {
		for (String query : queries()) {
			for (KStringMatcherMode mode : new KStringMatcherMode[] {
					KStringMatcherMode.CHECK_ONLY_STARTS_WITH, KStringMatcherMode.MULTISEARCH}) {
				String m = "query '" + query + "' mode " + mode;
				assertEquals(m + " stable", copyDescribeOrdered(searchCopy(query, mode, 0, 0)),
						copyDescribeOrdered(searchCopy(query, mode, 0, 0)));
			}
		}
	}

	/** The same queries with a point set, which orders the blocks by distance from it. */
	@Test
	public void nameSearchNearAPointIsTheSame() throws IOException {
		int[] point = somePoint();
		if (point == null) {
			return;
		}
		for (String query : queries()) {
			for (StringMatcherMode mode : new StringMatcherMode[] {
					StringMatcherMode.CHECK_ONLY_STARTS_WITH, StringMatcherMode.CHECK_STARTS_FROM_SPACE,
					StringMatcherMode.MULTISEARCH}) {
				String m = "near a point, query '" + query + "' mode " + mode;
				List<Amenity> jamenities = searchJava(query, mode, point[0], point[1]);
				List<net.osmand.shared.data.Amenity> kamenities =
						searchCopy(query, KStringMatcherMode.valueOf(mode.name()), point[0], point[1]);
				assertSame(m, jamenities, kamenities);
				searchesCompared++;
			}
		}
	}

	/** Queries that hit nothing still have to walk the index the same way. */
	@Test
	public void missingNamesAreTheSame() throws IOException {
		for (String query : new String[] {"zzzzzzzz", "щщщщ", "a b c d e f", "'", "-", "42424242"}) {
			for (StringMatcherMode mode : StringMatcherMode.values()) {
				String m = "missing '" + query + "' mode " + mode;
				assertSame(m, searchJava(query, mode, 0, 0),
						searchCopy(query, KStringMatcherMode.valueOf(mode.name()), 0, 0));
				searchesCompared++;
			}
		}
	}

	/** The type tables answer by name too, without touching the index. */
	@Test
	public void typeLookupsAreTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();

		Set<String> queries = new LinkedHashSet<>();
		for (PoiRegion region : java.getPoiIndexes()) {
			for (String category : region.getCategories()) {
				queries.add(category);
				if (category.length() > 2) {
					queries.add(category.substring(0, 2));
				}
			}
			for (List<String> subcats : region.getSubcategories()) {
				for (String subcat : subcats) {
					queries.add(subcat);
				}
			}
			for (PoiSubType subType : region.getSubTypes()) {
				if (subType.name != null && subType.name.length() > 2) {
					queries.add(subType.name.substring(0, 2));
					queries.add(subType.name);
				}
			}
		}
		queries.add("zzzz");
		for (String query : queries) {
			Map<PoiCategory, List<String>> jcats = java.searchPoiCategoriesByName(query, new LinkedHashMap<>());
			Map<net.osmand.shared.osm.PoiCategory, List<String>> kcats =
					copy.searchPoiCategoriesByName(query, new LinkedHashMap<>());
			assertEquals("categories by name '" + query + "'", categories(jcats), copyCategories(kcats));

			List<PoiSubType> jsubs = java.searchPoiSubTypesByPrefix(query);
			List<net.osmand.shared.binary.PoiSubType> ksubs = copy.searchPoiSubTypesByPrefix(query);
			assertEquals("subtypes by prefix '" + query + "'", subTypeNames(jsubs), copySubTypeNames(ksubs));
			searchesCompared++;
		}
		assertEquals("top index subtypes", subTypeNames(java.getTopIndexSubTypes()),
				copySubTypeNames(copy.getTopIndexSubTypes()));
	}

	/**
	 * The queries: every name the file holds, its first letters, its first two words, and any
	 * route id, capped so that the run stays quick.
	 */
	private List<String> queries() throws IOException {
		Set<String> queries = new LinkedHashSet<>();
		for (Amenity amenity : allAmenities()) {
			String name = amenity.getName();
			if (name != null && !name.isEmpty()) {
				queries.add(name);
				if (name.length() > 3) {
					queries.add(name.substring(0, 3));
				}
				String[] words = name.split(" ");
				if (words.length > 1) {
					queries.add(words[0] + " " + words[1]);
					queries.add(words[words.length - 1]);
				}
			}
			String routeId = amenity.getRouteId();
			if (routeId != null && !routeId.isEmpty()) {
				queries.add(routeId);
			}
			if (queries.size() > 40) {
				break;
			}
		}
		return new ArrayList<>(queries);
	}

	private int[] somePoint() throws IOException {
		for (Amenity amenity : allAmenities()) {
			return new int[] {
					net.osmand.util.MapUtils.get31TileNumberX(amenity.getLocation().getLongitude()),
					net.osmand.util.MapUtils.get31TileNumberY(amenity.getLocation().getLatitude())};
		}
		return null;
	}

	private List<Amenity> allAmenities() throws IOException {
		return java.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null));
	}

	private List<Amenity> searchJava(String query, StringMatcherMode mode, int x, int y) throws IOException {
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
				x, y, query, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null);
		req.setMatcherMode(mode);
		return java.searchPoiByName(req);
	}

	private List<net.osmand.shared.data.Amenity> searchCopy(String query, KStringMatcherMode mode, int x, int y) {
		net.osmand.shared.binary.SearchRequest<net.osmand.shared.data.Amenity> req =
				net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(
						x, y, query, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null, null, null);
		req.setMatcherMode(mode);
		return copy.searchPoiByName(req);
	}

	private static void assertSame(String m, List<Amenity> j, List<net.osmand.shared.data.Amenity> k) {
		assertEquals(m + " count", j.size(), k.size());
		List<String> jl = new ArrayList<>(describe(j));
		List<String> kl = new ArrayList<>(copyDescribe(k));
		Collections.sort(jl);
		Collections.sort(kl);
		assertEquals(m + " results", String.join("", jl), String.join("", kl));
		if (!describeOrdered(j).equals(copyDescribeOrdered(k))) {
			orderDiffered++;
		}
	}

	private static List<String> describe(List<Amenity> amenities) {
		List<String> lines = new ArrayList<>();
		for (Amenity a : amenities) {
			lines.add(line(a.getId(), a.getType().getKeyName(), a.getSubType(), a.getName(),
					a.getLocation().getLatitude(), a.getLocation().getLongitude()));
		}
		return lines;
	}

	private static List<String> copyDescribe(List<net.osmand.shared.data.Amenity> amenities) {
		List<String> lines = new ArrayList<>();
		for (net.osmand.shared.data.Amenity a : amenities) {
			lines.add(line(a.getId(), a.getType().getKeyName(), a.getSubType(), a.getName(),
					a.getLocation().getLatitude(), a.getLocation().getLongitude()));
		}
		return lines;
	}

	private static String line(Long id, String type, String subType, String name, double lat, double lon) {
		return id + " " + type + ":" + subType + " '" + name + "' " + lat + "," + lon + "\n";
	}

	private static String describeOrdered(List<Amenity> amenities) {
		return String.join("", describe(amenities));
	}

	private static String copyDescribeOrdered(List<net.osmand.shared.data.Amenity> amenities) {
		return String.join("", copyDescribe(amenities));
	}

	private static Map<String, List<String>> categories(Map<PoiCategory, List<String>> cats) {
		Map<String, List<String>> plain = new LinkedHashMap<>();
		for (Map.Entry<PoiCategory, List<String>> e : cats.entrySet()) {
			plain.put(e.getKey().getKeyName(), e.getValue());
		}
		return plain;
	}

	private static Map<String, List<String>> copyCategories(
			Map<net.osmand.shared.osm.PoiCategory, List<String>> cats) {
		Map<String, List<String>> plain = new LinkedHashMap<>();
		for (Map.Entry<net.osmand.shared.osm.PoiCategory, List<String>> e : cats.entrySet()) {
			plain.put(e.getKey().getKeyName(), e.getValue());
		}
		return plain;
	}

	private static List<String> subTypeNames(List<PoiSubType> subTypes) {
		List<String> names = new ArrayList<>();
		for (PoiSubType s : subTypes) {
			names.add(s.name);
		}
		return names;
	}

	private static List<String> copySubTypeNames(List<net.osmand.shared.binary.PoiSubType> subTypes) {
		List<String> names = new ArrayList<>();
		for (net.osmand.shared.binary.PoiSubType s : subTypes) {
			names.add(s.name);
		}
		return names;
	}
}
