package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchApisCompatTest.v;
import static net.osmand.shared.compat.SearchPhraseCompatTest.bits;
import static net.osmand.shared.compat.SearchPhraseCompatTest.call;
import static net.osmand.shared.compat.SearchPhraseCompatTest.field;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.CommonWords;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.Street;
import net.osmand.osm.MapPoiTypes;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code GeocodingUtilities} in OsmAnd-shared against the original in OsmAnd-java, over the obf files
 * the tests ship with - the routing maps and the maps of the search tests - and the real maps
 * {@code OSMAND_GEOCODING_MAPS} names, separated by {@code :}.
 *
 * The points are every {@link #STEP}th building of the towns and villages of a file, of a real map
 * only those of its first town, and a little way off {@link #ROADS} roads of it. At each, in each of
 * the {@link #CONTEXTS}, both sides:
 * <ul>
 * <li>find the roads near it, {@code reverseGeocodingSearch};</li>
 * <li>for the first {@link #JUSTIFIED} of them, look for the streets of their name and the buildings
 * on them, {@code justifyReverseGeocodingSearch}: knowing no building yet, knowing one
 * {@link #KNOWN_BUILDING_DISTANCE} m away, and with a search already cancelled;</li>
 * <li>find the roads again and sort them as the app does, {@code sortGeocodingResults}, which drops
 * the results of one place found in two files.</li>
 * </ul>
 * With them, a road named as each street with a number by a dash or with a letter or two in its
 * name, and as every {@link #STEP}th other street, a little north of it, is justified on both sides.
 * Every result is compared whole: its fields, with the road, the building, the street and the
 * settlement, its distances and its text.
 *
 * It writes the points, with what java answered, to {@code build/geocoding-java.txt};
 * {@code GeocodingUtilitiesTest} in OsmAnd-shared holds the copy to it on Kotlin/Native.
 */
public class GeocodingCompatTest {

	/** Read by {@code GeocodingUtilitiesTest} in OsmAnd-shared, from {@code ../OsmAnd-java/build}. */
	private static final File DUMP = new File("build/geocoding-java.txt");
	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	/** Every how many buildings of a file one is a point. */
	static final int STEP = 7;
	/** How many roads of a file a point is taken next to. */
	static final int ROADS = 40;
	/** How far north of a vertex of a road the point is, in degrees: about 20 m. */
	static final double OFF_THE_ROAD = 0.0002;
	/** How many of the roads found at a point are justified. */
	static final int JUSTIFIED = 3;
	static final double KNOWN_BUILDING_DISTANCE = 30;
	/**
	 * The default context of {@code buildDefaultContextForPOI}, with and without roads that have no
	 * name; the context android geocodes in; and the default one over the file opened twice, which
	 * finds every road in two regions, as it would on the border of two maps.
	 */
	static final String[] CONTEXTS = {"car", "car with empty names", "geocoding", "twice"};

	private static int points;
	private static int answers;
	private static int results;
	private static int answered;
	private static int buildings;
	/** Answers that are the same only with the roads java found in the order the copy found them. */
	private static int inCopyOrder;
	private static int namedRoads;

	/** Common words with the names of the regions, as the app has them and the tests after this one need. */
	@BeforeClass
	public static void setUp() throws Exception {
		SearchApisCompatTest.setUpTypes();
		assertTrue("the regions in the common words of java", CommonWords.getInstance().getFrequentlyUsed("gelderland") >= 0);
		assertTrue("the regions in the common words of the copy",
				net.osmand.shared.binary.CommonWords.Companion.getInstance().getFrequentlyUsed("gelderland") >= 0);
	}

	@AfterClass
	public static void close() {
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
	}

	@Test
	public void geocodingIsTheSame() throws Exception {
		List<File> files = new ArrayList<>(TestObf.files());
		files.addAll(TestObf.searchFiles());
		int shipped = files.size();
		String maps = System.getenv("OSMAND_GEOCODING_MAPS");
		if (maps != null && !maps.isEmpty()) {
			for (String path : maps.split(":")) {
				files.add(new File(path));
			}
		}
		File written = new File(DUMP.getPath() + ".part");
		DUMP.getParentFile().mkdirs();
		try (PrintWriter dump = new PrintWriter(Files.newBufferedWriter(written.toPath(), StandardCharsets.UTF_8))) {
			for (int f = 0; f < files.size(); f++) {
				geocodeFile(dump, f, files.get(f), f >= shipped);
			}
		}
		Files.move(written.toPath(), DUMP.toPath(), StandardCopyOption.REPLACE_EXISTING);
		System.out.println("GeocodingCompatTest: " + files.size() + " files, " + points + " points, " + answers
				+ " answers compared, " + results + " results, " + answered + " sorted answers not empty, " + buildings
				+ " of them a building first; " + inCopyOrder + " answers the same only with the roads java found in the order of the copy; "
				+ namedRoads + " roads named as a street justified");
		assertTrue(answered > 1000);
	}

	private static void geocodeFile(PrintWriter dump, int f, File file, boolean real) throws Exception {
		Places at = places(file, real);
		if (at.points.isEmpty()) {
			return;
		}
		dump.println("F\t" + f + "\t" + hex(file.getPath()));
		BinaryMapIndexReader j = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		BinaryMapIndexReader j2 = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		net.osmand.shared.binary.BinaryMapIndexReader k = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
		net.osmand.shared.binary.BinaryMapIndexReader k2 = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
		RoutingContext[] jc = {};
		net.osmand.shared.routing.RoutingContext[] kc = {};
		try {
			jc = javaContexts(j, j2);
			kc = copyContexts(k, k2);
			GeocodingUtilities java = new GeocodingUtilities();
			net.osmand.shared.binary.GeocodingUtilities copy = new net.osmand.shared.binary.GeocodingUtilities();
			for (double[] p : at.points) {
				points++;
				for (int c = 0; c < CONTEXTS.length; c++) {
					boolean twice = c == 3;
					boolean allowEmptyNames = c == 1;
					RoutingContext jctx = jc[c];
					net.osmand.shared.routing.RoutingContext kctx = kc[c];
					List<BinaryMapIndexReader> jr = twice ? List.of(j, j2) : List.of(j);
					List<net.osmand.shared.binary.BinaryMapIndexReader> kr = twice ? List.of(k, k2) : List.of(k);
					String ja = outcome(() -> javaAnswer(java, jctx, jr, p[0], p[1], allowEmptyNames));
					String ka = outcome(() -> copyAnswer(copy, kctx, kr, p[0], p[1], allowEmptyNames));
					String here = file.getName() + " " + CONTEXTS[c] + " " + p[0] + "," + p[1];
					if (!ja.equals(ka)) {
						String replayed = outcome(() -> javaAnswerInItsOrder(java, jctx, jr, p[0], p[1], allowEmptyNames));
						assertEquals(here + ", java's search over the roads it found", ja, replayed);
						String ordered = outcome(() -> javaAnswerInCopyOrder(java, jctx, kctx, jr, p[0], p[1], allowEmptyNames));
						if (!ordered.equals(ka)) {
							mismatch(here, ordered, ka);
						}
						assertEquals(here + ", java with the roads in the order of the copy", ordered, ka);
						inCopyOrder++;
					}
					dump.println("P\t" + f + "\t" + c + "\t" + bits(p[0]) + "\t" + bits(p[1]) + "\t" + hex(ja) + "\t" + (ja.equals(ka) ? "-" : hex(ka)));
					answers++;
				}
			}
			for (Object[] street : at.streets) {
				String name = (String) street[0];
				double lat = (Double) street[1] + OFF_THE_ROAD;
				double lon = (Double) street[2];
				String ja = outcome(() -> javaNamedRoad(java, j, name, lat, lon));
				String ka = outcome(() -> copyNamedRoad(copy, k, name, lat, lon));
				if (!ja.equals(ka)) {
					mismatch(file.getName() + " road named " + name, ja, ka);
				}
				assertEquals(file.getName() + " road named " + name, ja, ka);
				dump.println("S\t" + f + "\t" + hex(name) + "\t" + bits(lat) + "\t" + bits(lon) + "\t" + hex(ja));
				namedRoads++;
			}
		} finally {
			// the default contexts open the file once more each
			for (RoutingContext c : jc) {
				for (BinaryMapIndexReader r : c.map.keySet()) {
					r.close();
				}
			}
			for (net.osmand.shared.routing.RoutingContext c : kc) {
				for (net.osmand.shared.binary.BinaryMapIndexReader r : c.map.keySet()) {
					r.close();
				}
			}
			j.close();
			j2.close();
			k.close();
			k2.close();
		}
	}

	/** The points of a file to geocode and the streets to justify a road named as each for. */
	static final class Places {
		final List<double[]> points = new ArrayList<>();
		/** {@code {name, lat, lon}}. */
		final List<Object[]> streets = new ArrayList<>();
	}

	/**
	 * The points of a file: every {@link #STEP}th building of its towns and villages, of a real map
	 * only those of its first town, then a little north of the middle of {@link #ROADS} roads. The
	 * streets: every street with a number by a dash or a number with a letter or two in its name,
	 * "NC-42" or "1-я", which are matched in ways of their own, and every {@link #STEP}th other one.
	 * None of a file without roads.
	 */
	static Places places(File file, boolean real) throws IOException {
		Places at = new Places();
		BinaryMapIndexReader r = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		try {
			if (r.getRoutingIndexes().isEmpty()) {
				return at;
			}
			int n = 0;
			int m = 0;
			for (CityBlocks type : new CityBlocks[] {CityBlocks.CITY_TOWN_TYPE, CityBlocks.VILLAGES_TYPE}) {
				List<City> cities = r.getCities(null, type);
				if (real) {
					cities = cities.isEmpty() ? cities : cities.subList(0, 1);
				}
				for (City city : cities) {
					r.preloadStreets(city, null, null);
					for (Street s : new ArrayList<>(city.getStreets())) {
						if (numbered(s.getName()) || m++ % STEP == 0) {
							at.streets.add(new Object[] {s.getName(), s.getLocation().getLatitude(), s.getLocation().getLongitude()});
						}
						r.preloadBuildings(s, null, null);
						for (Building b : s.getBuildings()) {
							if (n++ % STEP == 0) {
								at.points.add(new double[] {b.getLocation().getLatitude(), b.getLocation().getLongitude()});
							}
						}
					}
				}
				if (real) {
					break;
				}
			}
			int roads = 0;
			SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 16, null);
			for (RouteRegion region : r.getRoutingIndexes()) {
				for (RouteSubregion subregion : r.searchRouteIndexTree(req, region.getSubregions())) {
					for (RouteDataObject road : r.loadRouteIndexData(subregion)) {
						if (roads < ROADS && road != null && road.getPointsLength() > 1) {
							int i = road.getPointsLength() / 2;
							at.points.add(new double[] {MapUtils.get31LatitudeY(road.getPoint31YTile(i)) + OFF_THE_ROAD,
									MapUtils.get31LongitudeX(road.getPoint31XTile(i))});
							roads++;
						}
					}
				}
			}
		} finally {
			r.close();
		}
		return at;
	}

	private static boolean numbered(String name) {
		if (name.matches(".*(\\d-|-\\d).*")) {
			return true;
		}
		for (String word : name.toLowerCase().split(" ")) {
			if (net.osmand.util.SearchAlgorithms.isNumber2Letters(word)) {
				return true;
			}
		}
		return false;
	}

	static RoutingContext[] javaContexts(BinaryMapIndexReader j, BinaryMapIndexReader j2) throws IOException {
		RoutingContext car = GeocodingUtilities.buildDefaultContextForPOI(j);
		RoutingConfiguration geocoding = RoutingConfiguration.getDefault().build("geocoding", new RoutingMemoryLimits(10, 10), new HashMap<>());
		RoutingConfiguration twice = RoutingConfiguration.getDefault().build("car", new RoutingMemoryLimits(
				GeocodingUtilities.GEOCODING_POI_MEMORY, GeocodingUtilities.GEOCODING_POI_MEMORY));
		return new RoutingContext[] {car, GeocodingUtilities.buildDefaultContextForPOI(j),
				new RoutePlannerFrontEnd().buildRoutingContext(geocoding, null, new BinaryMapIndexReader[] {j}),
				new RoutePlannerFrontEnd().buildRoutingContext(twice, null, new BinaryMapIndexReader[] {j, j2}, RouteCalculationMode.NORMAL)};
	}

	static net.osmand.shared.routing.RoutingContext[] copyContexts(net.osmand.shared.binary.BinaryMapIndexReader k,
			net.osmand.shared.binary.BinaryMapIndexReader k2) {
		net.osmand.shared.routing.RoutingConfiguration.Builder builder = net.osmand.shared.routing.RoutingConfiguration.Companion.getDefault();
		int memory = net.osmand.shared.binary.GeocodingUtilities.GEOCODING_POI_MEMORY;
		net.osmand.shared.routing.RoutingConfiguration geocoding = builder.build("geocoding",
				new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(10, 10), new LinkedHashMap<>());
		net.osmand.shared.routing.RoutingConfiguration twice = builder.build("car",
				new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(memory, memory), new LinkedHashMap<>());
		net.osmand.shared.routing.RoutePlannerFrontEnd planner = new net.osmand.shared.routing.RoutePlannerFrontEnd();
		return new net.osmand.shared.routing.RoutingContext[] {
				net.osmand.shared.binary.GeocodingUtilities.buildDefaultContextForPOI(k),
				net.osmand.shared.binary.GeocodingUtilities.buildDefaultContextForPOI(k),
				planner.buildRoutingContext(geocoding, List.of(k), null),
				planner.buildRoutingContext(twice, List.of(k, k2), net.osmand.shared.routing.RouteCalculationMode.NORMAL)};
	}

	// the answers

	private static final ResultMatcher<GeocodingResult> JAVA_CANCELLED = new ResultMatcher<GeocodingResult>() {
		@Override
		public boolean publish(GeocodingResult object) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return true;
		}
	};

	private static final net.osmand.shared.binary.ResultMatcher<net.osmand.shared.binary.GeocodingUtilities.GeocodingResult> COPY_CANCELLED =
			new net.osmand.shared.binary.ResultMatcher<>() {
				@Override
				public boolean publish(net.osmand.shared.binary.GeocodingUtilities.GeocodingResult obj) {
					return false;
				}

				@Override
				public boolean isCancelled() {
					return true;
				}
			};

	static String javaAnswer(GeocodingUtilities utils, RoutingContext ctx, List<BinaryMapIndexReader> readers,
			double lat, double lon, boolean allowEmptyNames) throws IOException {
		return javaAnswer(utils, readers, () -> utils.reverseGeocodingSearch(ctx, lat, lon, allowEmptyNames), true);
	}

	/**
	 * What java would answer had it found the roads at the point in the order the copy found them:
	 * java's {@code reverseGeocodingSearch} after its {@code findRouteSegment}, over java's roads put
	 * in that order. Both sort the roads by how far they are, and roads as far to the last digit stay
	 * in the order the routing context read them from its tiles, which java takes from a trove hash
	 * set and the copy from its own.
	 */
	static String javaAnswerInCopyOrder(GeocodingUtilities utils, RoutingContext jctx, net.osmand.shared.routing.RoutingContext kctx,
			List<BinaryMapIndexReader> readers, double lat, double lon, boolean allowEmptyNames) throws IOException {
		List<RouteSegmentPoint> roads = inCopyOrder(jctx, kctx, lat, lon);
		return javaAnswer(utils, readers, () -> reverseGeocodingSearch(roads, lat, lon, allowEmptyNames), false);
	}

	/**
	 * {@link #javaAnswer} with {@link #reverseGeocodingSearch} of this test over the roads in java's own
	 * order, which shows that it searches as java does.
	 */
	private static String javaAnswerInItsOrder(GeocodingUtilities utils, RoutingContext jctx, List<BinaryMapIndexReader> readers,
			double lat, double lon, boolean allowEmptyNames) throws IOException {
		List<RouteSegmentPoint> roads = new ArrayList<>();
		RouteSegmentPoint best = new RoutePlannerFrontEnd().findRouteSegment(lat, lon, jctx, roads, false, true);
		if (best != null) {
			best.others = null;
			roads.add(0, best);
		}
		return javaAnswer(utils, readers, () -> reverseGeocodingSearch(roads, lat, lon, allowEmptyNames), false);
	}

	private interface Roads {
		List<GeocodingResult> find() throws IOException;
	}

	private static String javaAnswer(GeocodingUtilities utils, List<BinaryMapIndexReader> readers, Roads reverse, boolean counted) throws IOException {
		StringBuilder sb = new StringBuilder();
		List<GeocodingResult> found = reverse.find();
		lines(sb, "A", found);
		for (int i = 0; i < Math.min(JUSTIFIED, found.size()); i++) {
			GeocodingResult r = found.get(i);
			BinaryMapIndexReader reader = null;
			for (BinaryMapIndexReader b : readers) {
				for (RouteRegion rb : b.getRoutingIndexes()) {
					if (reader == null && r.regionFP == rb.getFilePointer() && r.regionLen == rb.getLength()) {
						reader = b;
					}
				}
			}
			if (reader != null) {
				lines(sb, "B" + i, utils.justifyReverseGeocodingSearch(r, reader, 0, null));
				lines(sb, "K" + i, utils.justifyReverseGeocodingSearch(r, reader, KNOWN_BUILDING_DISTANCE, null));
				lines(sb, "X" + i, utils.justifyReverseGeocodingSearch(r, reader, 0, JAVA_CANCELLED));
			}
		}
		List<GeocodingResult> sorted = utils.sortGeocodingResults(readers, reverse.find());
		lines(sb, "C", sorted);
		if (counted) {
			count(sorted, sorted.isEmpty() ? null : sorted.get(0).building);
		}
		return sb.toString();
	}

	/** The roads java finds at the point, the nearest first, in the order the copy finds them. */
	private static List<RouteSegmentPoint> inCopyOrder(RoutingContext jctx, net.osmand.shared.routing.RoutingContext kctx,
			double lat, double lon) throws IOException {
		List<RouteSegmentPoint> j = new ArrayList<>();
		RouteSegmentPoint jps = new RoutePlannerFrontEnd().findRouteSegment(lat, lon, jctx, j, false, true);
		if (jps != null) {
			jps.others = null;
			j.add(0, jps);
		}
		List<net.osmand.shared.routing.RouteSegmentPoint> k = new ArrayList<>();
		net.osmand.shared.routing.RouteSegmentPoint kps = new net.osmand.shared.routing.RoutePlannerFrontEnd()
				.findRouteSegment(lat, lon, kctx, k, false, true);
		if (kps != null) {
			k.add(0, kps);
		}
		assertEquals("roads found", j.size(), k.size());
		List<RouteSegmentPoint> ordered = new ArrayList<>();
		for (int i = 0; i < k.size(); i++) {
			net.osmand.shared.routing.RouteSegmentPoint c = k.get(i);
			assertTrue("roads in order of distance", i == 0 || k.get(i - 1).distToProj <= c.distToProj);
			String key = c.getRoad().getId() + ":" + c.getSegmentStart() + ":" + c.getSegmentEnd() + ":" + c.preciseX
					+ ":" + c.preciseY + ":" + bits(c.distToProj);
			int match = -1;
			for (int m = 0; m < j.size() && match < 0; m++) {
				RouteSegmentPoint p = j.get(m);
				String pk = p.getRoad().getId() + ":" + p.getSegmentStart() + ":" + p.getSegmentEnd() + ":" + p.preciseX
						+ ":" + p.preciseY + ":" + bits(p.distToProj);
				if (pk.equals(key)) {
					match = m;
				}
			}
			assertTrue("road " + key + " found by java", match >= 0);
			ordered.add(j.remove(match));
		}
		return ordered;
	}

	/** {@code GeocodingUtilities.reverseGeocodingSearch} of java from the roads its {@code findRouteSegment} found on. */
	private static List<GeocodingResult> reverseGeocodingSearch(List<RouteSegmentPoint> listR, double lat, double lon, boolean allowEmptyNames) {
		List<GeocodingResult> lst = new ArrayList<>();
		double distSquare = 0;
		Map<String, List<RouteRegion>> streetNames = new HashMap<>();
		for (RouteSegmentPoint p : listR) {
			RouteDataObject road = p.getRoad();
			String name = Algorithms.isEmpty(road.getName()) ? road.getRef("", false, true) : road.getName();
			if (allowEmptyNames || !Algorithms.isEmpty(name)) {
				if (distSquare == 0 || distSquare > p.distToProj) {
					distSquare = p.distToProj;
				}
				GeocodingResult sr = new GeocodingResult();
				sr.searchPoint = new LatLon(lat, lon);
				sr.streetName = name == null ? "" : name;
				sr.point = p;
				sr.connectionPoint = new LatLon(MapUtils.get31LatitudeY(p.preciseY), MapUtils.get31LongitudeX(p.preciseX));
				sr.regionFP = road.region.getFilePointer();
				sr.regionLen = road.region.getLength();
				List<RouteRegion> plst = streetNames.get(sr.streetName);
				if (plst == null) {
					plst = new ArrayList<>();
					streetNames.put(sr.streetName, plst);
				}
				if (!plst.contains(road.region)) {
					plst.add(road.region);
					lst.add(sr);
				}
			}
			if (p.distToProj > GeocodingUtilities.STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS * GeocodingUtilities.STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS
					&& distSquare != 0 && p.distToProj > GeocodingUtilities.THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER * distSquare) {
				break;
			}
			if (p.distToProj > GeocodingUtilities.STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS * GeocodingUtilities.STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS) {
				break;
			}
		}
		Collections.sort(lst, GeocodingUtilities.DISTANCE_COMPARATOR);
		return lst;
	}

	static String copyAnswer(net.osmand.shared.binary.GeocodingUtilities utils, net.osmand.shared.routing.RoutingContext ctx,
			List<net.osmand.shared.binary.BinaryMapIndexReader> readers, double lat, double lon, boolean allowEmptyNames) {
		StringBuilder sb = new StringBuilder();
		List<net.osmand.shared.binary.GeocodingUtilities.GeocodingResult> found = utils.reverseGeocodingSearch(ctx, lat, lon, allowEmptyNames);
		lines(sb, "A", found);
		for (int i = 0; i < Math.min(JUSTIFIED, found.size()); i++) {
			net.osmand.shared.binary.GeocodingUtilities.GeocodingResult r = found.get(i);
			net.osmand.shared.binary.BinaryMapIndexReader reader = null;
			for (net.osmand.shared.binary.BinaryMapIndexReader b : readers) {
				for (net.osmand.shared.routing.RouteRegion rb : b.getRoutingIndexes()) {
					if (reader == null && r.regionFP == rb.getFilePointer() && r.regionLen == rb.getLength()) {
						reader = b;
					}
				}
			}
			if (reader != null) {
				lines(sb, "B" + i, utils.justifyReverseGeocodingSearch(r, reader, 0, null));
				lines(sb, "K" + i, utils.justifyReverseGeocodingSearch(r, reader, KNOWN_BUILDING_DISTANCE, null));
				lines(sb, "X" + i, utils.justifyReverseGeocodingSearch(r, reader, 0, COPY_CANCELLED));
			}
		}
		lines(sb, "C", utils.sortGeocodingResults(readers, utils.reverseGeocodingSearch(ctx, lat, lon, allowEmptyNames)));
		return sb.toString();
	}

	/** A road named as a street, a little north of it, justified knowing no building yet and knowing one. */
	static String javaNamedRoad(GeocodingUtilities utils, BinaryMapIndexReader reader, String name, double lat, double lon) throws IOException {
		GeocodingResult road = new GeocodingResult();
		road.searchPoint = new LatLon(lat, lon);
		road.connectionPoint = new LatLon(lat, lon);
		road.streetName = name;
		StringBuilder sb = new StringBuilder();
		lines(sb, "B", utils.justifyReverseGeocodingSearch(road, reader, 0, null));
		lines(sb, "K", utils.justifyReverseGeocodingSearch(road, reader, KNOWN_BUILDING_DISTANCE, null));
		return sb.toString();
	}

	static String copyNamedRoad(net.osmand.shared.binary.GeocodingUtilities utils, net.osmand.shared.binary.BinaryMapIndexReader reader,
			String name, double lat, double lon) {
		net.osmand.shared.binary.GeocodingUtilities.GeocodingResult road = new net.osmand.shared.binary.GeocodingUtilities.GeocodingResult();
		road.searchPoint = new net.osmand.shared.data.KLatLon(lat, lon);
		road.connectionPoint = new net.osmand.shared.data.KLatLon(lat, lon);
		road.streetName = name;
		StringBuilder sb = new StringBuilder();
		lines(sb, "B", utils.justifyReverseGeocodingSearch(road, reader, 0, null));
		lines(sb, "K", utils.justifyReverseGeocodingSearch(road, reader, KNOWN_BUILDING_DISTANCE, null));
		return sb.toString();
	}

	private static void count(List<GeocodingResult> sorted, Building first) {
		results += sorted.size();
		if (!sorted.isEmpty()) {
			answered++;
			if (first != null) {
				buildings++;
			}
		}
	}

	private static void lines(StringBuilder sb, String tag, List<?> list) {
		sb.append(tag).append(' ').append(list.size()).append('\n');
		for (Object r : list) {
			sb.append(line(r)).append('\n');
		}
	}

	/** A result of either side, all of it, the doubles as their bits after a {@code #}. */
	static String line(Object r) {
		return v(field(r, "streetName")) + "|" + field(r, "regionFP") + "|" + field(r, "regionLen")
				+ "|" + loc(field(r, "searchPoint")) + "|" + loc(field(r, "connectionPoint"))
				+ "|" + point(field(r, "point"))
				+ "|" + mapObject(field(r, "building")) + "|" + v(field(r, "buildingInterpolation"))
				+ "|" + mapObject(field(r, "street")) + "|" + mapObject(field(r, "city"))
				+ "|" + d(call(r, "getDistance")) + "|" + d(call(r, "getCityDistance")) + "|" + d(call(r, "getSortDistance"))
				+ "|" + v(call(r, "getBuildingString")) + "|" + v(call(r, "toString"));
	}

	private static String point(Object p) {
		if (p == null) {
			return "null";
		}
		return call(call(p, "getRoad"), "getId") + ":" + call(p, "getSegmentStart") + ":" + call(p, "getSegmentEnd")
				+ ":" + field(p, "preciseX") + ":" + field(p, "preciseY") + ":" + d(field(p, "distToProj"));
	}

	private static String mapObject(Object o) {
		if (o == null) {
			return "-";
		}
		String s = v(call(o, "getName")) + ":" + call(o, "getId") + ":" + loc(call(o, "getLocation"));
		if (o instanceof Building || o instanceof net.osmand.shared.data.Building) {
			s += ":" + loc(call(o, "getLatLon2"));
		}
		return s;
	}

	private static String loc(Object l) {
		if (l == null) {
			return "null";
		}
		if (l instanceof LatLon) {
			return "(" + d(((LatLon) l).getLatitude()) + "," + d(((LatLon) l).getLongitude()) + ")";
		}
		net.osmand.shared.data.KLatLon k = (net.osmand.shared.data.KLatLon) l;
		return "(" + d(k.getLatitude()) + "," + d(k.getLongitude()) + ")";
	}

	private static String d(Object x) {
		return "#" + bits((Double) x);
	}

	private interface Answer {
		String get() throws IOException;
	}

	private static String outcome(Answer answer) {
		try {
			return answer.get();
		} catch (Throwable t) {
			Throwable cause = t;
			while ((cause instanceof AssertionError || cause instanceof java.lang.reflect.InvocationTargetException)
					&& cause.getCause() != null) {
				cause = cause.getCause();
			}
			return "threw:" + cause.getClass().getSimpleName();
		}
	}

	private static void mismatch(String at, String j, String k) {
		String[] jl = j.split("\n");
		String[] kl = k.split("\n");
		System.out.println("MISMATCH " + at + ": " + jl.length + " / " + kl.length + " lines");
		for (int i = 0; i < Math.max(jl.length, kl.length); i++) {
			String x = i < jl.length ? jl[i] : "-";
			String y = i < kl.length ? kl[i] : "-";
			if (!x.equals(y)) {
				System.out.println("  J " + x + "\n  K " + y);
			}
		}
	}
}
