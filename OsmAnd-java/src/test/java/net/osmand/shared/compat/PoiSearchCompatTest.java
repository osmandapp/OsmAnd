package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import gnu.trove.set.hash.TLongHashSet;

import net.osmand.Location;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.search.core.HashQuadTree;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The poi section of {@link net.osmand.shared.binary.BinaryMapIndexReader} against the original:
 * every obf the tests ship with is opened in both, and every amenity both readers hand back is
 * compared field for field, tag for tag.
 *
 * The searches are the ones the app makes: a box at a zoom, a box with a type filter, a box with a
 * filter over a top index attribute, and a search along a path. The zoom matters on its own,
 * because below zoom 16 the reader thins dense areas out to one amenity per tile, and a copy that
 * rounded that differently would drop different ones.
 */
@RunWith(Parameterized.class)
public class PoiSearchCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static int amenitiesCompared;
	private static int withTagGroups;
	private static int topIndexFiltersTried;
	private static int onPathCompared;

	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;

	@BeforeClass
	public static void readPoiTypes() {
		MapPoiTypes javaTypes = new MapPoiTypes(POI_TYPES);
		MapPoiTypes.setDefault(javaTypes);
		net.osmand.shared.osm.MapPoiTypes copyTypes = new net.osmand.shared.osm.MapPoiTypes(POI_TYPES);
		net.osmand.shared.osm.MapPoiTypes.setDefault(copyTypes);
	}

	public PoiSearchCompatTest(String name, File file) throws IOException {
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@AfterClass
	public static void amenitiesWereCompared() {
		System.out.println("PoiSearchCompatTest: " + amenitiesCompared + " amenities compared, "
				+ withTagGroups + " of them with tag groups, " + onPathCompared + " on a path, "
				+ topIndexFiltersTried + " top index filters tried");
		assertTrue("amenities compared: " + amenitiesCompared, amenitiesCompared > 5000);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() {
		List<Object[]> data = new ArrayList<>();
		for (File file : TestObf.files()) {
			data.add(new Object[] {file.getName(), file});
		}
		return data;
	}

	/** What the constructor alone learned: the sections and their boxes, no tables read. */
	@Test
	public void sectionsAreTheSame() {
		assertEquals("contains poi data", java.containsPoiData(), copy.containsPoiData());
		assertEquals("poi sections", java.getPoiIndexes().size(), copy.getPoiIndexes().size());
		for (int i = 0; i < java.getPoiIndexes().size(); i++) {
			PoiRegion j = java.getPoiIndexes().get(i);
			net.osmand.shared.binary.PoiRegion k = copy.getPoiIndexes().get(i);
			String m = "section " + i;
			assertEquals(m + " name", j.getName(), k.getName());
			assertEquals(m + " filePointer", j.getFilePointer(), k.getFilePointer());
			assertEquals(m + " length", j.getLength(), k.getLength());
			assertEquals(m + " partName", j.getPartName(), k.getPartName());
			assertEquals(m + " fieldNumber", j.getFieldNumber(), k.getFieldNumber());
			assertEquals(m + " left31", j.getLeft31(), k.getLeft31());
			assertEquals(m + " right31", j.getRight31(), k.getRight31());
			assertEquals(m + " top31", j.getTop31(), k.getTop31());
			assertEquals(m + " bottom31", j.getBottom31(), k.getBottom31());
			assertEquals(m + " covers", java.containsPoiData(j.getLeft31(), j.getTop31(),
							j.getRight31(), j.getBottom31()),
					copy.containsPoiData(j.getLeft31(), j.getTop31(), j.getRight31(), j.getBottom31()));
		}
	}

	/** The decoding tables, once something has asked for them. */
	@Test
	public void categoriesAreTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();
		for (int i = 0; i < java.getPoiIndexes().size(); i++) {
			PoiRegion j = java.getPoiIndexes().get(i);
			net.osmand.shared.binary.PoiRegion k = copy.getPoiIndexes().get(i);
			String m = "section " + i;
			assertEquals(m + " categories", j.getCategories(), k.getCategories());
			assertEquals(m + " subcategories", j.getSubcategories(), k.getSubcategories());
			assertArrayEquals(m + " category freqs", j.getCategoryFreqs().toArray(),
					k.getCategoryFreqs().toArray());
			assertEquals(m + " subcategory freqs", j.getSubcategoryFreqs().size(),
					k.getSubcategoryFreqs().size());
			for (int c = 0; c < j.getSubcategoryFreqs().size(); c++) {
				assertArrayEquals(m + " subcategory freqs " + c, j.getSubcategoryFreqs().get(c).toArray(),
						k.getSubcategoryFreqs().get(c).toArray());
			}
			assertEquals(m + " subtypes", j.getSubTypes().size(), k.getSubTypes().size());
			for (int s = 0; s < j.getSubTypes().size(); s++) {
				assertSubType(m + " subtype " + s, j.getSubTypes().get(s), k.getSubTypes().get(s));
			}
			assertEquals(m + " top index subtypes", j.getTopIndexSubTypes().size(),
					k.getTopIndexSubTypes().size());
			for (int s = 0; s < j.getTopIndexSubTypes().size(); s++) {
				assertSubType(m + " top index subtype " + s, j.getTopIndexSubTypes().get(s),
						k.getTopIndexSubTypes().get(s));
			}
			// the same id has to decode to the same type and value on both sides
			for (int id = 0; id < 512; id++) {
				StringBuilder js = new StringBuilder();
				StringBuilder ks = new StringBuilder();
				PoiSubType jst = j.getSubtypeFromId(id, js);
				net.osmand.shared.binary.PoiSubType kst = k.getSubtypeFromId(id, ks);
				assertEquals(m + " subtype of id " + id, jst == null, kst == null);
				if (jst != null) {
					assertEquals(m + " subtype name of id " + id, jst.name, kst.name);
					assertEquals(m + " subtype value of id " + id, js.toString(), ks.toString());
				}
				StringBuilder jsub = new StringBuilder();
				StringBuilder ksub = new StringBuilder();
				PoiCategory jcat = j.decodePoiType(id, jsub);
				net.osmand.shared.osm.PoiCategory kcat = k.decodePoiType(id, ksub);
				assertEquals(m + " category of id " + id, jcat == null ? null : jcat.getKeyName(),
						kcat == null ? null : kcat.getKeyName());
				assertEquals(m + " subcategory of id " + id, jsub.toString(), ksub.toString());
			}
		}
	}

	/** Every amenity of the file, at the zooms that change how the reader thins them out. */
	@Test
	public void amenitiesAreTheSame() throws IOException {
		for (int zoom : new int[] {-1, 15, 14, 8}) {
			List<Amenity> jamenities = searchJava(zoom, null, null);
			List<net.osmand.shared.data.Amenity> kamenities = searchCopy(zoom, null, null);
			String m = "zoom " + zoom;
			assertEquals(m + " amenities", jamenities.size(), kamenities.size());
			for (int i = 0; i < jamenities.size(); i++) {
				assertAmenity(m + " amenity " + i, jamenities.get(i), kamenities.get(i));
				amenitiesCompared++;
				if (jamenities.get(i).getTagGroups() != null
						&& !jamenities.get(i).getTagGroups().isEmpty()) {
					withTagGroups++;
				}
			}
		}
	}

	/** A box smaller than the file, which is what a search around a tap looks like. */
	@Test
	public void boxedSearchIsTheSame() throws IOException {
		for (PoiRegion region : java.getPoiIndexes()) {
			int midX = region.getLeft31() + (region.getRight31() - region.getLeft31()) / 2;
			int midY = region.getTop31() + (region.getBottom31() - region.getTop31()) / 2;
			int side = Math.max(1, (region.getRight31() - region.getLeft31()) / 8);
			SearchRequest<Amenity> jreq = BinaryMapIndexReader.buildSearchPoiRequest(
					midX - side, midX + side, midY - side, midY + side, 15, null, null);
			net.osmand.shared.binary.SearchRequest<net.osmand.shared.data.Amenity> kreq =
					net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(
							midX - side, midX + side, midY - side, midY + side, 15, null, null, null);
			List<Amenity> jamenities = java.searchPoi(jreq);
			List<net.osmand.shared.data.Amenity> kamenities = copy.searchPoi(kreq);
			String m = "box of " + region.getName();
			assertEquals(m + " amenities", jamenities.size(), kamenities.size());
			for (int i = 0; i < jamenities.size(); i++) {
				assertAmenity(m + " amenity " + i, jamenities.get(i), kamenities.get(i));
			}
			// the other counters are package private in java's request; this one says the tree
			// walk opened the same nodes, which is what the box search turns on
			assertEquals(m + " read subtrees", jreq.numberOfReadSubtrees, kreq.numberOfReadSubtrees);
		}
	}

	/** A filter over types, which decides both which boxes are opened and which amenities are kept. */
	@Test
	public void filteredSearchIsTheSame() throws IOException {
		for (int zoom : new int[] {-1, 15}) {
			for (int modulo : new int[] {2, 3}) {
				List<Amenity> jamenities = searchJava(zoom, everyNth(modulo), null);
				List<net.osmand.shared.data.Amenity> kamenities = searchCopy(zoom, copyEveryNth(modulo), null);
				String m = "zoom " + zoom + " every " + modulo;
				assertEquals(m + " amenities", jamenities.size(), kamenities.size());
				for (int i = 0; i < jamenities.size(); i++) {
					assertAmenity(m + " amenity " + i, jamenities.get(i), kamenities.get(i));
				}
			}
			// the filter that keeps everything is not the same as no filter: it still makes the
			// reader look at the types of every box
			List<Amenity> jall = searchJava(zoom, BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null);
			List<net.osmand.shared.data.Amenity> kall = searchCopy(zoom,
					net.osmand.shared.binary.SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, null);
			assertEquals("accept all at zoom " + zoom, jall.size(), kall.size());
			for (int i = 0; i < jall.size(); i++) {
				assertAmenity("accept all zoom " + zoom + " amenity " + i, jall.get(i), kall.get(i));
			}
		}
	}

	/** A filter over a top index attribute, which is written into the boxes of the tree. */
	@Test
	public void topIndexSearchIsTheSame() throws IOException {
		java.initCategories();
		copy.initCategories();
		List<String> names = new ArrayList<>();
		for (PoiRegion region : java.getPoiIndexes()) {
			for (PoiSubType st : region.getTopIndexSubTypes()) {
				if (!names.contains(st.name)) {
					names.add(st.name);
				}
			}
		}
		for (String name : names) {
			topIndexFiltersTried++;
			List<Amenity> jamenities = searchJava(-1, null, topIndex(name));
			List<net.osmand.shared.data.Amenity> kamenities = searchCopy(-1, null, copyTopIndex(name));
			String m = "top index " + name;
			assertEquals(m + " amenities", jamenities.size(), kamenities.size());
			for (int i = 0; i < jamenities.size(); i++) {
				assertAmenity(m + " amenity " + i, jamenities.get(i), kamenities.get(i));
			}
		}
	}

	/** A search along a path, which measures every amenity against the stretch of path near it. */
	@Test
	public void pathSearchIsTheSame() throws IOException {
		List<Amenity> all = searchJava(-1, null, null);
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
			net.osmand.shared.binary.SearchRequest<net.osmand.shared.data.Amenity> kreq =
					net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(kroute, radius,
							net.osmand.shared.binary.SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, null);
			assertEquals("box left", jreq.getLeft(), kreq.getLeft());
			assertEquals("box right", jreq.getRight(), kreq.getRight());
			assertEquals("box top", jreq.getTop(), kreq.getTop());
			assertEquals("box bottom", jreq.getBottom(), kreq.getBottom());

			List<Amenity> jamenities = java.searchPoi(jreq);
			List<net.osmand.shared.data.Amenity> kamenities = copy.searchPoi(kreq);
			String m = "path radius " + radius;
			assertEquals(m + " amenities", jamenities.size(), kamenities.size());
			for (int i = 0; i < jamenities.size(); i++) {
				assertAmenity(m + " amenity " + i, jamenities.get(i), kamenities.get(i));
				assertRoutePoint(m + " amenity " + i, jamenities.get(i), kamenities.get(i));
				onPathCompared++;
			}
		}
	}

	/** The tag groups of the settlements, read separately once the amenities are in hand. */
	@Test
	public void tagGroupsAreTheSame() throws IOException {
		List<Amenity> all = searchJava(-1, null, null);
		if (all.isEmpty()) {
			return;
		}
		TLongHashSet jtiles = new TLongHashSet();
		net.osmand.shared.util.collections.KTLongHashSet ktiles =
				new net.osmand.shared.util.collections.KTLongHashSet();
		for (Amenity a : all) {
			int x31 = net.osmand.util.MapUtils.get31TileNumberX(a.getLocation().getLongitude());
			int y31 = net.osmand.util.MapUtils.get31TileNumberY(a.getLocation().getLatitude());
			long tile = HashQuadTree.encodeTileId31(
					net.osmand.binary.BinaryMapPoiReaderAdapter.EVAL_TAG_GROUP_ZOOM, x31, y31);
			jtiles.add(tile);
			ktiles.add(tile);
		}
		for (int i = 0; i < java.getPoiIndexes().size(); i++) {
			PoiRegion j = java.getPoiIndexes().get(i);
			net.osmand.shared.binary.PoiRegion k = copy.getPoiIndexes().get(i);
			String m = "section " + i;
			assertEquals(m + " read bboxes", java.readAmenityBboxes(j, new TLongHashSet(jtiles)),
					copy.readAmenityBboxes(k, copyOf(ktiles)));
			for (int id = 0; id < 64; id++) {
				assertEquals(m + " tag group " + id, tagValues(j.getTagValues(id)),
						copyTagValues(k.getTagValues(id)));
			}
			// asking again finds nothing left to read, on both sides
			assertEquals(m + " read bboxes again", java.readAmenityBboxes(j, new TLongHashSet(jtiles)),
					copy.readAmenityBboxes(k, copyOf(ktiles)));
		}
	}

	private List<Amenity> searchJava(int zoom, SearchPoiTypeFilter filter,
			SearchPoiAdditionalFilter additional) throws IOException {
		return java.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, filter, additional, null));
	}

	private List<net.osmand.shared.data.Amenity> searchCopy(int zoom,
			net.osmand.shared.binary.SearchPoiTypeFilter filter,
			net.osmand.shared.binary.SearchPoiAdditionalFilter additional) {
		return copy.searchPoi(net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, filter, additional, null));
	}

	private static void assertSubType(String m, PoiSubType j, net.osmand.shared.binary.PoiSubType k) {
		assertEquals(m + " name", j.name, k.name);
		assertEquals(m + " text", j.text, k.text);
		assertEquals(m + " frequency", j.frequency, k.frequency);
		assertEquals(m + " topIndex", j.isTopIndex(), k.isTopIndex());
		assertEquals(m + " possibleValues", j.possibleValues, k.possibleValues);
		assertEquals(m + " wikidataIds", j.wikidataIds, k.wikidataIds);
		assertEquals(m + " freqs present", j.possibleValuesFreqs == null, k.possibleValuesFreqs == null);
		if (j.possibleValuesFreqs != null) {
			assertArrayEquals(m + " freqs", j.possibleValuesFreqs.toArray(),
					k.possibleValuesFreqs.toArray());
		}
	}

	private static void assertAmenity(String m, Amenity j, net.osmand.shared.data.Amenity k) {
		assertEquals(m + " id", j.getId(), k.getId());
		assertEquals(m + " name", j.getName(), k.getName());
		assertEquals(m + " enName", j.getEnName(false), k.getEnName(false));
		assertEquals(m + " names", j.getNamesMap(true), k.getNamesMap(true));
		assertEquals(m + " type", j.getType() == null ? null : j.getType().getKeyName(),
				k.getType() == null ? null : k.getType().getKeyName());
		assertEquals(m + " subType", j.getSubType(), k.getSubType());
		assertEquals(m + " regionName", j.getRegionName(), k.getRegionName());
		assertNotNull(m + " location", k.getLocation());
		assertEquals(m + " lat", j.getLocation().getLatitude(), k.getLocation().getLatitude(), 0);
		assertEquals(m + " lon", j.getLocation().getLongitude(), k.getLocation().getLongitude(), 0);
		assertEquals(m + " additional keys", new ArrayList<>(j.getAdditionalInfoKeys()),
				new ArrayList<>(k.getAdditionalInfoKeys()));
		for (String key : j.getAdditionalInfoKeys()) {
			assertEquals(m + " additional " + key, j.getAdditionalInfo(key), k.getAdditionalInfo(key));
		}
		assertEquals(m + " openingHours", j.getOpeningHours(), k.getOpeningHours());
		assertEquals(m + " tag groups", tagGroups(j), copyTagGroups(k));
		assertEquals(m + " routeTrack", j.isRouteTrack(), k.isRouteTrack());
		assertEquals(m + " routeId", j.getRouteId(), k.getRouteId());
	}

	private static void assertRoutePoint(String m, Amenity j, net.osmand.shared.data.Amenity k) {
		assertEquals(m + " route point present", j.getRoutePoint() == null, k.getRoutePoint() == null);
		if (j.getRoutePoint() == null) {
			return;
		}
		assertEquals(m + " deviate distance", j.getRoutePoint().deviateDistance,
				k.getRoutePoint().getDeviateDistance(), 1e-9);
		assertEquals(m + " deviation right", j.getRoutePoint().deviationDirectionRight,
				k.getRoutePoint().getDeviationDirectionRight());
		assertEquals(m + " point A lat", j.getRoutePoint().pointA.getLatitude(),
				k.getRoutePoint().getPointA().getLatitude(), 0);
		assertEquals(m + " point B lon", j.getRoutePoint().pointB.getLongitude(),
				k.getRoutePoint().getPointB().getLongitude(), 0);
	}

	private static Map<Integer, String> tagGroups(Amenity a) {
		Map<Integer, String> groups = new LinkedHashMap<>();
		if (a.getTagGroups() != null) {
			for (Map.Entry<Integer, List<TagValuePair>> e : a.getTagGroups().entrySet()) {
				groups.put(e.getKey(), tagValues(e.getValue()));
			}
		}
		return groups;
	}

	private static Map<Integer, String> copyTagGroups(net.osmand.shared.data.Amenity a) {
		Map<Integer, String> groups = new LinkedHashMap<>();
		if (a.getTagGroups() != null) {
			for (Map.Entry<Integer, List<net.osmand.shared.binary.TagValuePair>> e :
					a.getTagGroups().entrySet()) {
				groups.put(e.getKey(), copyTagValues(e.getValue()));
			}
		}
		return groups;
	}

	private static String tagValues(List<TagValuePair> pairs) {
		if (pairs == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (TagValuePair p : pairs) {
			sb.append(p.tag).append('=').append(p.value).append(';');
		}
		return sb.toString();
	}

	private static String copyTagValues(List<net.osmand.shared.binary.TagValuePair> pairs) {
		if (pairs == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (net.osmand.shared.binary.TagValuePair p : pairs) {
			sb.append(p.tag).append('=').append(p.value).append(';');
		}
		return sb.toString();
	}

	private static net.osmand.shared.util.collections.KTLongHashSet copyOf(
			net.osmand.shared.util.collections.KTLongHashSet set) {
		net.osmand.shared.util.collections.KTLongHashSet copy =
				new net.osmand.shared.util.collections.KTLongHashSet();
		copy.addAll(set.toArray());
		return copy;
	}

	/** Keeps the subcategories whose name hashes to a multiple of {@code modulo}. */
	private static SearchPoiTypeFilter everyNth(int modulo) {
		return new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return Math.abs(subcategory.hashCode()) % modulo == 0;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
	}

	private static net.osmand.shared.binary.SearchPoiTypeFilter copyEveryNth(int modulo) {
		return new net.osmand.shared.binary.SearchPoiTypeFilter() {
			@Override
			public boolean accept(net.osmand.shared.osm.PoiCategory type, String subcategory) {
				return Math.abs(subcategory.hashCode()) % modulo == 0;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
	}

	private static SearchPoiAdditionalFilter topIndex(String name) {
		return new SearchPoiAdditionalFilter() {
			@Override
			public boolean accept(PoiSubType poiSubType, String value) {
				return name.equals(poiSubType.name);
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getIconResource() {
				return null;
			}
		};
	}

	private static net.osmand.shared.binary.SearchPoiAdditionalFilter copyTopIndex(String name) {
		return new net.osmand.shared.binary.SearchPoiAdditionalFilter() {
			@Override
			public boolean accept(net.osmand.shared.binary.PoiSubType poiSubType, String value) {
				return name.equals(poiSubType.name);
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getIconResource() {
				return null;
			}
		};
	}
}
