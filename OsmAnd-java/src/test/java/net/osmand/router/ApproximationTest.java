package net.osmand.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.osmand.LocationsHolder;
import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.*;

import static net.osmand.util.RouterUtilTest.getNativeLibPath;

@RunWith(Parameterized.class)
public class ApproximationTest {
	private final ApproximationEntry entry;
	private final int ENTRY_TIMEOUT_MS = 1500000;
	private String[] defaultProfiles = { "car" };
	private Integer[] defaultMinPointApproximation = { 50 };
	private String[] defaultTypes = { "routing", "geometry" };
	private static final String RESOURCES_PATH = "/approximation/";
	private static final String FILES_PATH = "src/test/resources/approximation/";

	
	boolean isNative() {
		return false;
	}

	public ApproximationTest(String testName, ApproximationEntry entry) {
		if (entry.types == null) entry.types = Arrays.asList(defaultTypes);
		if (entry.profiles == null) entry.profiles = Arrays.asList(defaultProfiles);
		if (entry.minPointApproximation == null) entry.minPointApproximation = Arrays.asList(defaultMinPointApproximation);
		if (entry.expectedDistMin == 0) entry.expectedDistMin = Double.NEGATIVE_INFINITY;
		if (entry.expectedDistMax == 0) entry.expectedDistMax = Double.POSITIVE_INFINITY;
		this.entry = entry;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Iterable<Object[]> data() throws IOException {
		String fileName = RESOURCES_PATH + "test.json";
		Reader reader = new InputStreamReader(Objects.requireNonNull(ApproximationTest.class.getResourceAsStream(fileName)));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ApproximationEntry[] entries = gson.fromJson(reader, ApproximationEntry[].class);
		ArrayList<Object[]> array = new ArrayList<>();
		for (ApproximationEntry entry : entries) {
			if (!entry.ignore) array.add(new Object[]{entry.gpxFile, entry});
		}
		reader.close();
		return array;
	}

	@Test(timeout = ENTRY_TIMEOUT_MS)
	public void testApproximation() throws Exception {
		boolean useNative = isNative() && getNativeLibPath() != null && !entry.ignoreNative;
		NativeLibrary nativeLibrary = null;
		if (useNative) {
			boolean old = NativeLibrary.loadOldLib(getNativeLibPath());
			nativeLibrary = new NativeLibrary();
			if (!old) {
				throw new UnsupportedOperationException("NativeLibrary is not supported");
			}
		}

		String obfFilePath = FILES_PATH + entry.obfFile;
		RandomAccessFile raf = new RandomAccessFile(obfFilePath, "r");
		BinaryMapIndexReader[] binaryMapIndexReaders = { new BinaryMapIndexReader(raf, new File(obfFilePath)) };
		if (useNative) {
			Objects.requireNonNull(nativeLibrary).initMapFile(new File(obfFilePath).getAbsolutePath(), true);
		}

		for (String type : entry.types) {
			for (String profile : entry.profiles) {
				for (Integer minPointApproximation : entry.minPointApproximation) {
					testEntry(entry, type, profile, minPointApproximation, binaryMapIndexReaders, nativeLibrary);
				}
			}
		}

		raf.close();
	}

	private void testEntry(ApproximationEntry entry, String type, String profile, Integer minPointApproximation,
	                       BinaryMapIndexReader[] binaryMapIndexReaders, NativeLibrary nativeLibrary)
			throws IOException, InterruptedException {
		
		String tag = String.format("\n%s %s %s [%d] %s\n", entry.gpxFile, type, profile, minPointApproximation, entry.name);

		final int MEM_LIMIT = RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT * 8 * 2; // ~ 4 GB
		RoutingMemoryLimits memoryLimits = new RoutingMemoryLimits(MEM_LIMIT, MEM_LIMIT);

		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();
		if ("routing".equals(type)) {
			router.setUseGeometryBasedApproximation(false);
		} else if ("geometry".equals(type)) {
			GpxRouteApproximation.GPX_SEGMENT_ALGORITHM = GpxRouteApproximation.GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM;
//			GpxRouteApproximation.GPX_SEGMENT_ALGORITHM = GpxRouteApproximation.GPX_OSM_POINTS_MATCH_ALGORITHM;
			router.setUseGeometryBasedApproximation(true);
		}
		router.setUseNativeApproximation(isNative());

		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		RoutingConfiguration config = builder.build(profile, memoryLimits, new HashMap<String, String>());
		config.routeCalculationTime = System.currentTimeMillis(); // ENABLE_TIME_CONDITIONAL_ROUTING
		if (minPointApproximation > 0) config.minPointApproximation = minPointApproximation;
		RoutingContext ctx = router.buildRoutingContext(config, isNative() ? nativeLibrary : null,
				binaryMapIndexReaders, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		GpxRouteApproximation gctx = new GpxRouteApproximation(ctx);

		String gpxFilePath = FILES_PATH + entry.gpxFile;
		GPXFile gpxFile = GPXUtilities.loadGPXFile(new File(gpxFilePath));
		List<GPXUtilities.WptPt> waypoints = gpxFile.tracks.get(0).segments.get(0).points;
		List<RoutePlannerFrontEnd.GpxPoint> gpxPoints = router.generateGpxPoints(gctx, new LocationsHolder(waypoints));
		GpxRouteApproximation r = router.searchGpxRoute(gctx, gpxPoints, null, false);
		List<RouteSegmentResult> result = r.collectFinalPointsAsRoute();

		double distance = 0;
		Set<Long> waysInResult = new HashSet<>();
		Map<Long, String> turnsInResult = new HashMap<>();
		for (RouteSegmentResult segment : result) {
			long osmId = segment.getObject().getId() / 64;
			distance += calcSegmentDistance(segment);
			waysInResult.add(osmId);
			if (segment.getTurnType() != null && segment.getObject().getId() != -1) {
				String lanes = getLanesString(segment);
				String turn = segment.getTurnType().toXmlString();
				boolean skipToSpeak = segment.getTurnType().isSkipToSpeak();
				String turnLanesString = (skipToSpeak ? "[MUTE] " : "") + turn +
						(!Algorithms.isEmpty(lanes) ? ":" + lanes : "");
				turnsInResult.put(osmId, turnLanesString);
			}
		}

		String messageDist = tag + String.format("distance (%.2f) is outside of min / max (%.2f / %.2f)",
				distance, entry.expectedDistMin, entry.expectedDistMax);
		Assert.assertTrue(messageDist, distance > entry.expectedDistMin && distance < entry.expectedDistMax);

		if (entry.expectedWays != null) {
			for (long osmId : entry.expectedWays.keySet()) {
				String messageWays = tag + "expectedWays (" + entry.expectedWays.get(osmId) + ") failed for " + osmId;
				Assert.assertTrue(messageWays, waysInResult.contains(osmId) == entry.expectedWays.get(osmId));
			}
		}

		if (entry.expectedTurns != null) {
			for (long osmId : entry.expectedTurns.keySet()) {
				String got = turnsInResult.get(osmId);
				String expected = entry.expectedTurns.get(osmId);
				Assert.assertTrue(tag + String.format("expectedTurns (%s) failed for %d (%s)", expected, osmId, got),
						(expected == null && got == null) || (expected != null && got != null && expected.equals(got)));
			}
		}
	}

	private double calcSegmentDistance(RouteSegmentResult rr) {
		double distance = 0;
		RouteDataObject road = rr.getObject();
		boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
		for (int next, j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
			next = plus ? j + 1 : j - 1;
			double d = MapUtils.squareRootDist31(
					road.getPoint31XTile(j), road.getPoint31YTile(j),
					road.getPoint31XTile(next), road.getPoint31YTile(next));
			distance += d;
		}
		return distance;
	}

	private String getLanesString(RouteSegmentResult segment) {
		final int[] lns = segment.getTurnType().getLanes();
		if (lns != null) {
			return TurnType.lanesToString(lns);
		}
		return null;
	}

	public class ApproximationEntry {
		private String name;
		private String gpxFile;
		private String obfFile;
		private boolean ignore;
		private boolean ignoreNative;
		private List<String> types;
		private List<String> profiles;
		private List<Integer> minPointApproximation;
		private double expectedDistMin;
		private double expectedDistMax;
		private Map<Long, Boolean> expectedWays;
		private Map<Long, String> expectedTurns;
	}
}