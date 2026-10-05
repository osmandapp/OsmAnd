package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import net.osmand.Location;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.shared.binary.BinaryAmenityIndexRepository;
import net.osmand.util.MapUtils;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link BinaryAmenityIndexRepository} against what {@code AmenityIndexRepositoryBinary} in the
 * android app does. The app class cannot be reached from here - it needs an {@code
 * OsmandApplication} - so the java side of each comparison is the reader call the app class makes,
 * written out with the same arguments.
 *
 * That is the point of the test: the repository is a thin wrapper whose whole job is to pass the
 * numbers on, and its methods take them in a different order than the requests it builds do -
 * {@code searchAmenities} is handed top, left, bottom, right and has to build a request out of
 * left, right, top, bottom. A wrapper that swapped a pair would still read a box and still return
 * amenities, only the wrong ones.
 */
@RunWith(Parameterized.class)
public class TravelRepositoryCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static int boxSearches;
	private static int nameSearches;
	private static int amenitiesCompared;

	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;
	private final BinaryAmenityIndexRepository repository;
	private final File file;

	@BeforeClass
	public static void readPoiTypes() {
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
	}

	public TravelRepositoryCompatTest(String name, File file) throws IOException {
		this.file = file;
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
		this.repository = new BinaryAmenityIndexRepository(file.getPath(), () -> copy);
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@AfterClass
	public static void searchesWereCompared() {
		System.out.println("TravelRepositoryCompatTest: " + boxSearches + " box searches, "
				+ nameSearches + " name searches, " + amenitiesCompared + " amenities compared");
		assertTrue("amenities compared: " + amenitiesCompared, amenitiesCompared > 1000);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() {
		List<Object[]> data = new ArrayList<>();
		for (File file : TestObf.files()) {
			data.add(new Object[] {file.getName(), file});
		}
		return data;
	}

	/** The sections the repository hands out, and the file it says it is. */
	@Test
	public void sectionsAreTheSame() {
		assertEquals("file", file.getName(), repository.toString());
		assertEquals("poi sections", java.getPoiIndexes().size(), repository.getReaderPoiIndexes().size());
		for (int i = 0; i < java.getPoiIndexes().size(); i++) {
			PoiRegion j = java.getPoiIndexes().get(i);
			net.osmand.shared.binary.PoiRegion k = repository.getReaderPoiIndexes().get(i);
			assertEquals("section " + i + " left31", j.getLeft31(), k.getLeft31());
			assertEquals("section " + i + " right31", j.getRight31(), k.getRight31());
			assertEquals("section " + i + " top31", j.getTop31(), k.getTop31());
			assertEquals("section " + i + " bottom31", j.getBottom31(), k.getBottom31());
		}
		// the app's rule, written out: a file whose name starts with world_ or holds "basemap"
		String lower = file.getName().toLowerCase();
		boolean worldMap = lower.startsWith("world_") || lower.contains("basemap");
		assertEquals("world map", worldMap, repository.isWorldMap());
	}

	/** A point inside the poi section, its corners, and points well outside it. */
	@Test
	public void checkContainsIsTheSame() {
		for (PoiRegion region : java.getPoiIndexes()) {
			for (double[] point : samplePoints(region)) {
				double lat = point[0];
				double lon = point[1];
				int x31 = MapUtils.get31TileNumberX(lon);
				int y31 = MapUtils.get31TileNumberY(lat);
				assertEquals("contains " + lat + " " + lon,
						java.containsPoiData(x31, y31, x31, y31),
						repository.checkContains(lat, lon));
				// the app hands these over as top, left, bottom, right
				assertEquals("contains box " + lat + " " + lon,
						java.containsPoiData(x31, y31, x31, y31),
						repository.checkContainsInt(y31, x31, y31, x31));
			}
			assertEquals("contains whole section",
					java.containsPoiData(region.getLeft31(), region.getTop31(),
							region.getRight31(), region.getBottom31()),
					repository.checkContainsInt(region.getTop31(), region.getLeft31(),
							region.getBottom31(), region.getRight31()));
		}
	}

	/** Whether a map search would reach the file at all, which decides if it is opened at all. */
	@Test
	public void mapSectionIntersectsIsTheSame() {
		for (PoiRegion region : java.getPoiIndexes()) {
			for (double[] point : samplePoints(region)) {
				for (int zoom : new int[] {15, 16, 17}) {
					net.osmand.shared.binary.SearchRequest<net.osmand.shared.binary.BinaryMapDataObject> req =
							net.osmand.shared.binary.SearchRequest.buildSearchRequest(0, 0, 0, 0, zoom, null);
					req.setBBoxRadius(point[0], point[1], 1000);
					assertEquals("map intersects " + point[0] + " " + point[1] + " zoom " + zoom,
							java.containsMapData(req.left, req.top, req.right, req.bottom, zoom),
							repository.isMapSectionIntersects(req));
				}
			}
		}
	}

	/** Whether a request's box touches any poi section of the file. */
	@Test
	public void poiSectionIntersectsIsTheSame() {
		for (PoiRegion region : java.getPoiIndexes()) {
			for (double[] point : samplePoints(region)) {
				net.osmand.shared.binary.SearchRequest<net.osmand.shared.data.Amenity> req =
						net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(
								0, 0, "", 0, 0, 0, 0, null, null, null);
				req.setBBoxRadius(point[0], point[1], 1000);
				boolean expected = false;
				for (PoiRegion r : java.getPoiIndexes()) {
					if (req.intersects(r.getLeft31(), r.getTop31(), r.getRight31(), r.getBottom31())) {
						expected = true;
						break;
					}
				}
				assertEquals("intersects " + point[0] + " " + point[1],
						expected, repository.isPoiSectionIntersects(req));
			}
		}
	}

	/** A box search, at the zooms the app uses, through the wrapper and through the reader. */
	@Test
	public void boxSearchIsTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();
		for (PoiRegion region : java.getPoiIndexes()) {
			for (int zoom : new int[] {-1, 15, 16}) {
				int left = region.getLeft31();
				int right = region.getRight31();
				int top = region.getTop31();
				int bottom = region.getBottom31();

				SearchRequest<Amenity> jreq = BinaryMapIndexReader.buildSearchPoiRequest(
						left, right, top, bottom, zoom,
						BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null);
				List<Amenity> jamenities = java.searchPoi(jreq);

				// the app hands the wrapper top, left, bottom, right
				List<net.osmand.shared.data.Amenity> kamenities = repository.searchAmenities(
						top, left, bottom, right, zoom,
						net.osmand.shared.binary.SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER,
						null, null, null, -1);

				String m = file.getName() + " zoom " + zoom;
				assertEquals(m + " amenities", ids(jamenities), copyIds(kamenities));
				boxSearches++;
				amenitiesCompared += jamenities.size();
			}
		}
	}

	/** The same box, this time asking only for one category, so the type filter is exercised too. */
	@Test
	public void filteredBoxSearchIsTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();
		for (PoiRegion region : java.getPoiIndexes()) {
			for (String category : region.getCategories()) {
				SearchRequest<Amenity> jreq = BinaryMapIndexReader.buildSearchPoiRequest(
						region.getLeft31(), region.getRight31(), region.getTop31(), region.getBottom31(),
						-1, javaCategoryFilter(category), null);
				List<Amenity> jamenities = java.searchPoi(jreq);

				List<net.osmand.shared.data.Amenity> kamenities = repository.searchAmenities(
						region.getTop31(), region.getLeft31(), region.getBottom31(), region.getRight31(),
						-1, copyCategoryFilter(category), null, null, null, -1);

				assertEquals(file.getName() + " category " + category, ids(jamenities), copyIds(kamenities));
				boxSearches++;
			}
		}
	}

	/** A search by name, which goes through the poi name index. */
	@Test
	public void nameSearchIsTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();
		for (String query : queries()) {
			for (PoiRegion region : java.getPoiIndexes()) {
				SearchRequest<Amenity> jreq = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0, query, region.getLeft31(), region.getRight31(),
						region.getTop31(), region.getBottom31(), null);
				List<Amenity> jamenities = java.searchPoiByName(jreq);

				List<net.osmand.shared.data.Amenity> kamenities = repository.searchAmenitiesByName(
						0, 0, region.getLeft31(), region.getTop31(), region.getRight31(),
						region.getBottom31(), query, null);

				// the order of blocks that sit the same distance away is java's hash order, which
				// the copy does not reproduce - see PoiNameSearchCompatTest
				assertEquals(file.getName() + " query '" + query + "'",
						new HashSet<>(ids(jamenities)), new HashSet<>(copyIds(kamenities)));
				nameSearches++;
				amenitiesCompared += jamenities.size();
			}
		}
	}

	/** A search along a path, the one the app makes while navigating. */
	@Test
	public void pathSearchIsTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();
		List<Amenity> all = new ArrayList<>();
		for (PoiRegion region : java.getPoiIndexes()) {
			all.addAll(java.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
					region.getLeft31(), region.getRight31(), region.getTop31(), region.getBottom31(),
					-1, BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null)));
		}
		if (all.size() < 4) {
			return;
		}
		List<Location> jroute = new ArrayList<>();
		List<net.osmand.shared.data.KLocation> kroute = new ArrayList<>();
		int step = Math.max(1, all.size() / 12);
		for (int i = 0; i < all.size(); i += step) {
			double lat = all.get(i).getLocation().getLatitude();
			double lon = all.get(i).getLocation().getLongitude();
			jroute.add(new Location("test", lat, lon));
			kroute.add(new net.osmand.shared.data.KLocation("test", lat, lon));
		}
		for (double radius : new double[] {10, 500, 5000}) {
			SearchRequest<Amenity> jreq = BinaryMapIndexReader.buildSearchPoiRequest(
					jroute, radius, BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null);
			List<Amenity> jamenities = java.searchPoi(jreq);

			List<net.osmand.shared.data.Amenity> kamenities = repository.searchAmenitiesOnThePath(
					kroute, radius, net.osmand.shared.binary.SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, null);

			assertEquals(file.getName() + " path radius " + radius, ids(jamenities), copyIds(kamenities));
			amenitiesCompared += jamenities.size();
		}
	}

	/** Subtypes by prefix, which the app asks for while the user types. */
	@Test
	public void subTypesByPrefixAreTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();
		for (String prefix : new String[] {"a", "b", "ro", "wat"}) {
			List<PoiSubType> jsubtypes = java.searchPoiSubTypesByPrefix(prefix);
			List<net.osmand.shared.binary.PoiSubType> ksubtypes = repository.searchPoiSubTypesByPrefix(prefix);
			assertEquals(file.getName() + " prefix '" + prefix + "' count",
					jsubtypes.size(), ksubtypes.size());
			for (int i = 0; i < jsubtypes.size(); i++) {
				assertEquals(file.getName() + " prefix '" + prefix + "' name " + i,
						jsubtypes.get(i).name, ksubtypes.get(i).name);
			}
		}
	}

	/** An empty prefix throws in the reader; the repository swallows it, as the app's does. */
	@Test
	public void emptyPrefixIsSwallowed() {
		try {
			java.searchPoiSubTypesByPrefix("");
			fail("the reader should refuse an empty prefix");
		} catch (IllegalArgumentException | IOException expected) {
			// this is what the app's repository catches
		}
		assertTrue("empty prefix", repository.searchPoiSubTypesByPrefix("").isEmpty());
	}

	/**
	 * The subtypes a file uses that the shipped poi_types.xml has no type for. There is no java
	 * counterpart to compare with - the app computes this inside its own cache - so the two halves
	 * of the definition are checked instead: everything reported is unknown, nothing unknown is
	 * left out.
	 */
	@Test
	public void deltaSubcategoriesAreEveryUnknownSubtype() throws IOException {
		copy.initCategories();
		net.osmand.shared.osm.MapPoiTypes types = net.osmand.shared.osm.MapPoiTypes.getDefault();
		Map<String, List<String>> delta = new HashMap<>();
		for (net.osmand.shared.binary.PoiRegion region : copy.getPoiIndexes()) {
			repository.calculateDeltaSubcategories(region, types, delta);
		}
		MapPoiTypes javaTypes = MapPoiTypes.getDefault();
		for (PoiRegion region : java.getPoiIndexes()) {
			List<String> categories = region.getCategories();
			for (int i = 0; i < categories.size(); i++) {
				String categoryName = categories.get(i);
				PoiCategory category = javaTypes.getPoiCategoryByName(categoryName);
				Set<String> reported = new LinkedHashSet<>(
						delta.getOrDefault(categoryName, Collections.emptyList()));
				for (String subCategory : region.getSubcategories().get(i)) {
					PoiType known = category == null ? null : category.getPoiTypeByKeyName(subCategory);
					String m = file.getName() + " " + categoryName + "/" + subCategory;
					if (known == null) {
						assertTrue(m + " should be reported as unknown", reported.contains(subCategory));
					} else {
						assertFalse(m + " is known and should not be reported", reported.contains(subCategory));
					}
				}
			}
		}
	}

	/** A closed file answers nothing rather than throwing: the app keeps the repository around. */
	@Test
	public void closedFileSearchesNothing() {
		BinaryAmenityIndexRepository closed =
				new BinaryAmenityIndexRepository(file.getPath(), () -> null);
		assertFalse("contains", closed.checkContains(0, 0));
		assertFalse("contains box", closed.checkContainsInt(0, 0, 0, 0));
		assertTrue("poi sections", closed.getReaderPoiIndexes().isEmpty());
		assertTrue("by name", closed.searchAmenitiesByName(0, 0, 0, 0, 0, 0, "x", null).isEmpty());
		assertTrue("box", closed.searchAmenities(0, 0, 0, 0, -1, null, null, null, null, -1).isEmpty());
		assertTrue("subtypes", closed.searchPoiSubTypesByPrefix("a").isEmpty());
		closed.close();
	}

	/** Names that the files are likely to hold at least some of, plus one that nothing matches. */
	private static List<String> queries() {
		return Arrays.asList("a", "st", "ka", "zzzznothing");
	}

	/** The middle of the section, its corners, and a point a long way outside it. */
	private static List<double[]> samplePoints(PoiRegion region) {
		List<double[]> points = new ArrayList<>();
		int[][] tiles = {
				{(int) (((long) region.getLeft31() + region.getRight31()) / 2),
						(int) (((long) region.getTop31() + region.getBottom31()) / 2)},
				{region.getLeft31(), region.getTop31()},
				{region.getRight31(), region.getBottom31()},
		};
		for (int[] tile : tiles) {
			points.add(new double[] {MapUtils.get31LatitudeY(tile[1]), MapUtils.get31LongitudeX(tile[0])});
		}
		points.add(new double[] {-60.0, 150.0}); // nothing in the test files is near here
		return points;
	}

	private static List<Long> ids(List<Amenity> amenities) {
		List<Long> ids = new ArrayList<>();
		for (Amenity amenity : amenities) {
			ids.add(amenity.getId());
		}
		return ids;
	}

	private static List<Long> copyIds(List<net.osmand.shared.data.Amenity> amenities) {
		List<Long> ids = new ArrayList<>();
		for (net.osmand.shared.data.Amenity amenity : amenities) {
			ids.add(amenity.getId());
		}
		return ids;
	}

	private static BinaryMapIndexReader.SearchPoiTypeFilter javaCategoryFilter(String category) {
		return new BinaryMapIndexReader.SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return type != null && category.equals(type.getKeyName());
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
	}

	private static net.osmand.shared.binary.SearchPoiTypeFilter copyCategoryFilter(String category) {
		return new net.osmand.shared.binary.SearchPoiTypeFilter() {
			@Override
			public boolean accept(net.osmand.shared.osm.PoiCategory type, String subcategory) {
				return type != null && category.equals(type.getKeyName());
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
	}
}
