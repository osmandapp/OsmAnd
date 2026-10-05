package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import gnu.trove.list.array.TIntArrayList;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The map section of {@link net.osmand.shared.binary.BinaryMapIndexReader} against the original in
 * {@link BinaryMapIndexReader}: every obf the tests ship with is opened in both, and what they read
 * out of it is compared - the sections, their encoding tables, the r-tree roots, and then every map
 * object of every zoom level, field for field and tag for tag.
 *
 * Both readers are opened fresh for every case, so that the state they build up as they read - the
 * encoding table and the box trees are filled in lazily - is the same on both sides.
 */
@RunWith(Parameterized.class)
public class MapSectionCompatTest {

	private static int objectsCompared;

	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;

	public MapSectionCompatTest(String name, File file) throws IOException {
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@AfterClass
	public static void objectsWereCompared() {
		System.out.println("MapSectionCompatTest: " + objectsCompared + " map objects compared");
		assertTrue("objects compared: " + objectsCompared, objectsCompared > 10000);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() {
		List<Object[]> data = new ArrayList<>();
		for (File file : TestObf.files()) {
			data.add(new Object[] {file.getName(), file});
		}
		return data;
	}

	/** What the constructor alone learned: the sections and their zoom levels, no objects read. */
	@Test
	public void sectionsAreTheSame() {
		assertEquals("basemap", java.isBasemap(), copy.isBasemap());
		assertEquals("contains map data", java.containsMapData(), copy.containsMapData());
		assertEquals("map sections", java.getMapIndexes().size(), copy.getMapIndexes().size());
		for (int i = 0; i < java.getMapIndexes().size(); i++) {
			MapIndex j = java.getMapIndexes().get(i);
			net.osmand.shared.binary.MapIndex k = copy.getMapIndexes().get(i);
			String m = "section " + i;
			assertEquals(m + " name", j.getName(), k.getName());
			assertEquals(m + " filePointer", j.getFilePointer(), k.getFilePointer());
			assertEquals(m + " length", j.getLength(), k.getLength());
			assertEquals(m + " partName", j.getPartName(), k.getPartName());
			assertEquals(m + " fieldNumber", j.getFieldNumber(), k.getFieldNumber());
			assertEquals(m + " basemap", j.isBaseMap(), k.isBaseMap());
			assertEquals(m + " centre present", j.getCenterLatLon() == null, k.getCenterLatLon() == null);
			if (j.getCenterLatLon() != null) {
				assertEquals(m + " centre lat", j.getCenterLatLon().getLatitude(),
						k.getCenterLatLon().getLatitude(), 0);
				assertEquals(m + " centre lon", j.getCenterLatLon().getLongitude(),
						k.getCenterLatLon().getLongitude(), 0);
			}
			assertEquals(m + " roots", j.getRoots().size(), k.getRoots().size());
			for (int r = 0; r < j.getRoots().size(); r++) {
				assertRoot(m + " root " + r, j.getRoots().get(r), k.getRoots().get(r));
			}
		}
	}

	/** Whether a place is covered has to be answered the same, or a file is opened for nothing. */
	@Test
	public void coverageIsTheSame() {
		for (MapIndex j : java.getMapIndexes()) {
			for (MapRoot root : j.getRoots()) {
				int zoom = root.getMinZoom();
				int midX = root.getLeft() + (root.getRight() - root.getLeft()) / 2;
				int midY = root.getTop() + (root.getBottom() - root.getTop()) / 2;
				for (int[] point : new int[][] {
						{root.getLeft(), root.getTop()}, {root.getRight(), root.getBottom()},
						{midX, midY}, {root.getLeft() - 1000, root.getTop() - 1000}}) {
					String m = "tile " + point[0] + "," + point[1] + " zoom " + zoom;
					assertEquals(m, java.containsMapData(point[0], point[1], zoom),
							copy.containsMapData(point[0], point[1], zoom));
				}
				String m = "box of root zoom " + zoom;
				assertEquals(m, java.containsMapData(root.getLeft(), root.getTop(),
								root.getRight(), root.getBottom(), zoom),
						copy.containsMapData(root.getLeft(), root.getTop(),
								root.getRight(), root.getBottom(), zoom));
			}
		}
	}

	/** Every object of every zoom level, and the encoding table both sides ended up with. */
	@Test
	public void objectsAreTheSame() throws IOException {
		for (int zoom : zooms()) {
			List<BinaryMapDataObject> jobjects = searchJava(zoom, null);
			List<net.osmand.shared.binary.BinaryMapDataObject> kobjects = searchCopy(zoom, null);
			assertEquals("objects at zoom " + zoom, jobjects.size(), kobjects.size());
			for (int i = 0; i < jobjects.size(); i++) {
				assertObject("zoom " + zoom + " object " + i, jobjects.get(i), kobjects.get(i));
				objectsCompared++;
			}
			comparePairs("zoom " + zoom, jobjects, kobjects);
		}
		for (int i = 0; i < java.getMapIndexes().size(); i++) {
			assertEncodingRules("section " + i, java.getMapIndexes().get(i), copy.getMapIndexes().get(i));
		}
	}

	/**
	 * Restating objects in another section's numbers, which is what lets several files be drawn
	 * together. Both sides start from the same table and see the same objects in the same order,
	 * so every number they hand out has to match, and so does the table they end up with.
	 */
	@Test
	public void adoptedObjectsAreTheSame() throws IOException {
		for (int zoom : zooms()) {
			List<BinaryMapDataObject> jobjects = searchJava(zoom, null);
			List<net.osmand.shared.binary.BinaryMapDataObject> kobjects = searchCopy(zoom, null);
			MapIndex jtarget = new MapIndex();
			net.osmand.shared.binary.MapIndex ktarget = new net.osmand.shared.binary.MapIndex();
			// a table that is not empty, so that adopting has to translate the numbers rather
			// than take the other section's table over wholesale
			jtarget.initMapEncodingRule(0, 1, "highway", "residential");
			ktarget.initMapEncodingRule(0, 1, "highway", "residential");
			for (int i = 0; i < jobjects.size(); i++) {
				String m = "zoom " + zoom + " adopted " + i;
				BinaryMapDataObject j = jtarget.adoptMapObject(jobjects.get(i));
				net.osmand.shared.binary.BinaryMapDataObject k = ktarget.adoptMapObject(kobjects.get(i));
				assertEquals(m + " id", j.getId(), k.getId());
				assertEquals(m + " area", j.isArea(), k.isArea());
				assertArrayEquals(m + " types", j.getTypes(), k.getTypes());
				assertArrayEquals(m + " additionalTypes", j.getAdditionalTypes(), k.getAdditionalTypes());
				assertArrayEquals(m + " coordinates", j.getCoordinates(), k.getCoordinates());
				if (j.getNamesOrder() == null) {
					assertNull(m + " names order", k.getNamesOrder());
				} else {
					assertArrayEquals(m + " names order",
							j.getNamesOrder().toArray(), k.getNamesOrder().toArray());
					assertEquals(m + " ordered names", String.valueOf(j.getOrderedObjectNames()),
							String.valueOf(k.getOrderedObjectNames()));
				}
			}
			assertEncodingRules("adopted at zoom " + zoom, jtarget, ktarget);
		}
	}

	/** A filter has to turn down the same objects, and be asked about the same types. */
	@Test
	public void filteredSearchIsTheSame() throws IOException {
		for (int zoom : zooms()) {
			TIntArrayList jasked = new TIntArrayList();
			List<BinaryMapDataObject> jobjects = searchJava(zoom, (types, index) -> {
				int first = types.size() == 0 ? -1 : types.get(0);
				jasked.add(first);
				return first % 3 == 0;
			});
			TIntArrayList kasked = new TIntArrayList();
			List<net.osmand.shared.binary.BinaryMapDataObject> kobjects = searchCopy(zoom, (types, index) -> {
				int first = types.size() == 0 ? -1 : types.get(0);
				kasked.add(first);
				return first % 3 == 0;
			});
			assertEquals("types offered at zoom " + zoom, jasked.toString(), kasked.toString());
			assertEquals("objects at zoom " + zoom, jobjects.size(), kobjects.size());
			for (int i = 0; i < jobjects.size(); i++) {
				assertObject("filtered zoom " + zoom + " object " + i, jobjects.get(i), kobjects.get(i));
			}
		}
	}

	/** Searching a box the file does not reach has to come back empty on both sides. */
	@Test
	public void emptyBoxIsTheSame() throws IOException {
		SearchRequest<BinaryMapDataObject> jreq = BinaryMapIndexReader.buildSearchRequest(0, 10, 0, 10, 15, null);
		net.osmand.shared.binary.SearchRequest<net.osmand.shared.binary.BinaryMapDataObject> kreq =
				net.osmand.shared.binary.SearchRequest.buildSearchRequest(0, 10, 0, 10, 15, null);
		assertEquals("objects", java.searchMapIndex(jreq).size(), copy.searchMapIndex(kreq).size());
		assertEquals("ocean", jreq.isOcean(), kreq.isOcean());
		assertEquals("land", jreq.isLand(), kreq.isLand());
	}

	/** The zooms the file has levels for, plus one below and one above the finest. */
	private List<Integer> zooms() {
		List<Integer> zooms = new ArrayList<>();
		for (MapIndex index : java.getMapIndexes()) {
			for (MapRoot root : index.getRoots()) {
				for (int zoom : new int[] {root.getMinZoom() - 1, root.getMinZoom(), root.getMaxZoom()}) {
					if (zoom >= 0 && !zooms.contains(zoom)) {
						zooms.add(zoom);
					}
				}
			}
		}
		return zooms;
	}

	private List<BinaryMapDataObject> searchJava(int zoom, BinaryMapIndexReader.SearchFilter filter)
			throws IOException {
		return java.searchMapIndex(BinaryMapIndexReader.buildSearchRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, filter));
	}

	private List<net.osmand.shared.binary.BinaryMapDataObject> searchCopy(
			int zoom, net.osmand.shared.binary.SearchFilter filter) throws IOException {
		return copy.searchMapIndex(net.osmand.shared.binary.SearchRequest.buildSearchRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, filter));
	}

	private static void assertRoot(String m, MapRoot j, net.osmand.shared.binary.MapRoot k) {
		assertEquals(m + " minZoom", j.getMinZoom(), k.getMinZoom());
		assertEquals(m + " maxZoom", j.getMaxZoom(), k.getMaxZoom());
		assertEquals(m + " left", j.getLeft(), k.getLeft());
		assertEquals(m + " right", j.getRight(), k.getRight());
		assertEquals(m + " top", j.getTop(), k.getTop());
		assertEquals(m + " bottom", j.getBottom(), k.getBottom());
		assertEquals(m + " filePointer", j.getFilePointer(), k.getFilePointer());
		assertEquals(m + " length", j.getLength(), k.getLength());
		assertEquals(m + " toString", j.toString(), k.toString());
	}

	private static void assertEncodingRules(String m, MapIndex j, net.osmand.shared.binary.MapIndex k) {
		assertEquals(m + " rules", j.decodingRules.size(), k.decodingRules.size());
		assertEquals(m + " encodingRulesSizeBytes", j.encodingRulesSizeBytes, k.encodingRulesSizeBytes);
		int[] keys = j.decodingRules.keys();
		Arrays.sort(keys);
		int[] copyKeys = k.decodingRules.keys();
		Arrays.sort(copyKeys);
		assertArrayEquals(m + " rule ids", keys, copyKeys);
		for (int key : keys) {
			TagValuePair jt = j.decodeType(key);
			net.osmand.shared.binary.TagValuePair kt = k.decodeType(key);
			assertNotNull(m + " rule " + key, kt);
			assertEquals(m + " rule " + key + " tag", jt.tag, kt.tag);
			assertEquals(m + " rule " + key + " value", jt.value, kt.value);
			assertEquals(m + " rule " + key + " attribute", jt.additionalAttribute, kt.additionalAttribute);
			assertEquals(m + " rule " + key + " registered", j.isRegisteredRule(key), k.isRegisteredRule(key));
			assertEquals(m + " rule " + key + " back to id",
					j.getRule(jt.tag, jt.value), k.getRule(jt.tag, jt.value));
		}
		assertEquals(m + " nameEncodingType", j.nameEncodingType, k.nameEncodingType);
		assertEquals(m + " nameEnEncodingType", j.nameEnEncodingType, k.nameEnEncodingType);
		assertEquals(m + " refEncodingType", j.refEncodingType, k.refEncodingType);
		assertEquals(m + " coastlineEncodingType", j.coastlineEncodingType, k.coastlineEncodingType);
		assertEquals(m + " coastlineBrokenEncodingType", j.coastlineBrokenEncodingType, k.coastlineBrokenEncodingType);
		assertEquals(m + " landEncodingType", j.landEncodingType, k.landEncodingType);
		assertEquals(m + " onewayAttribute", j.onewayAttribute, k.onewayAttribute);
		assertEquals(m + " onewayReverseAttribute", j.onewayReverseAttribute, k.onewayReverseAttribute);
		assertEquals(m + " positiveLayers", sorted(j.positiveLayers.toArray()), sorted(k.positiveLayers));
		assertEquals(m + " negativeLayers", sorted(j.negativeLayers.toArray()), sorted(k.negativeLayers));
	}

	private static void assertObject(String m, BinaryMapDataObject j,
			net.osmand.shared.binary.BinaryMapDataObject k) {
		assertEquals(m + " id", j.getId(), k.getId());
		assertEquals(m + " area", j.isArea(), k.isArea());
		assertEquals(m + " objectType", j.getObjectType(), k.getObjectType());
		assertArrayEquals(m + " coordinates", j.getCoordinates(), k.getCoordinates());
		assertEquals(m + " inner coordinates", Arrays.deepToString(j.getPolygonInnerCoordinates()),
				Arrays.deepToString(k.getPolygonInnerCoordinates()));
		assertArrayEquals(m + " types", j.getTypes(), k.getTypes());
		assertArrayEquals(m + " additionalTypes", j.getAdditionalTypes(), k.getAdditionalTypes());
		assertEquals(m + " name", j.getName(), k.getName());
		assertEquals(m + " ordered names", String.valueOf(j.getOrderedObjectNames()),
				String.valueOf(k.getOrderedObjectNames()));
		assertEquals(m + " simple layer", j.getSimpleLayer(), k.getSimpleLayer());
		assertEquals(m + " deleted", j.isDeleted(), k.isDeleted());
		assertEquals(m + " cycle", j.isCycle(), k.isCycle());
		assertEquals(m + " points", j.getPointsLength(), k.getPointsLength());
		assertEquals(m + " label specified", j.isLabelSpecified(), k.isLabelSpecified());
		if (j.isLabelSpecified()) {
			assertEquals(m + " labelX", j.getLabelX(), k.getLabelX());
			assertEquals(m + " labelY", j.getLabelY(), k.getLabelY());
			assertEquals(m + " label lat", j.getLabelLatLon().getLatitude(),
					k.getLabelLatLon().getLatitude(), 0);
			assertEquals(m + " label lon", j.getLabelLatLon().getLongitude(),
					k.getLabelLatLon().getLongitude(), 0);
		}
		if (j.getNamesOrder() == null) {
			assertNull(m + " names order", k.getNamesOrder());
		} else {
			assertNotNull(m + " names order", k.getNamesOrder());
			assertArrayEquals(m + " names order", j.getNamesOrder().toArray(), k.getNamesOrder().toArray());
		}
		// the tags themselves, decoded through each side's own encoding table
		assertEquals(m + " tags", tags(j), tags(k));
		for (String tag : new String[] {"name", "name:en", "ref", "highway", "natural", "layer"}) {
			assertEquals(m + " tag value " + tag, j.getTagValue(tag), k.getTagValue(tag));
			assertEquals(m + " additional tag value " + tag,
					j.getAdditionalTagValue(tag), k.getAdditionalTagValue(tag));
		}
	}

	/**
	 * Objects of one search against each other, which is what {@code compareBinary} is for: it
	 * decides whether two files hold the same object, so both sides have to agree on it, both at
	 * full precision and with the shape simplified.
	 */
	private static void comparePairs(String m, List<BinaryMapDataObject> j,
			List<net.osmand.shared.binary.BinaryMapDataObject> k) {
		for (int i = 0; i + 1 < j.size(); i += 17) {
			for (int precision : new int[] {0, 5, 1000}) {
				String pm = m + " pair " + i + " precision " + precision;
				assertEquals(pm, j.get(i).compareBinary(j.get(i + 1), precision),
						k.get(i).compareBinary(k.get(i + 1), precision));
				assertEquals(pm + " with itself",
						j.get(i).compareBinary(j.get(i), precision),
						k.get(i).compareBinary(k.get(i), precision));
			}
		}
	}

	private static Map<String, String> tags(BinaryMapDataObject o) {
		Map<String, String> tags = new LinkedHashMap<>();
		for (int type : o.getTypes()) {
			TagValuePair tp = o.getMapIndex().decodeType(type);
			tags.put(tp.tag, tp.value);
		}
		for (int type : o.getAdditionalTypes()) {
			TagValuePair tp = o.getMapIndex().decodeType(type);
			tags.put("+" + tp.tag, tp.value);
		}
		return tags;
	}

	private static Map<String, String> tags(net.osmand.shared.binary.BinaryMapDataObject o) {
		Map<String, String> tags = new LinkedHashMap<>();
		for (int type : o.getTypes()) {
			net.osmand.shared.binary.TagValuePair tp = o.getMapIndex().decodeType(type);
			tags.put(tp.tag, tp.value);
		}
		for (int type : o.getAdditionalTypes()) {
			net.osmand.shared.binary.TagValuePair tp = o.getMapIndex().decodeType(type);
			tags.put("+" + tp.tag, tp.value);
		}
		return tags;
	}

	private static String sorted(int[] values) {
		int[] copy = values.clone();
		Arrays.sort(copy);
		return Arrays.toString(copy);
	}

	private static String sorted(Set<Integer> values) {
		List<Integer> copy = new ArrayList<>(values);
		Collections.sort(copy);
		int[] plain = new int[copy.size()];
		for (int i = 0; i < plain.length; i++) {
			plain[i] = copy.get(i);
		}
		return Arrays.toString(plain);
	}

}
