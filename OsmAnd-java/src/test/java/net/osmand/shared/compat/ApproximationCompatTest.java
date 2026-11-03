package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.LocationsHolder;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.routing.RouteCalculationMode;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * The OsmAnd-shared gpx approximation is a copy of the java one; this runs both over the same
 * track and map and compares what they attach it to.
 *
 * The tracks are the approximation tests' - the 24 cases of {@code approximation/test.json},
 * each over its own small obf, with the profiles and {@code minPointApproximation} values the
 * case lists - and each runs three ways: the routing-based approximation, the geometry-based one
 * and the older geometry-based points match. Both sides go the whole way from the waypoints: the
 * track points, the road searches, the A* searches or the graph walk, the straight lines where
 * nothing matched, the preparation of the turns. What comes out is compared point by point - which
 * track points the route is attached at and which are straight lines - and segment by segment: the
 * road, the stretch of it, the times, the distance, the speed and the manoeuvre; plus the counters
 * of the approximation and the number of segments the searches settled, which two runs taking the
 * same steps agree on. The one track with timestamps runs with the external timestamps applied, so
 * the speeds they give are compared too.
 */
@RunWith(Parameterized.class)
public class ApproximationCompatTest {

	private static final String RESOURCES = "src/test/resources/approximation/";
	private static final int MEM_LIMIT = RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT * 8 * 2; // ~ 4 GB, as ApproximationTest

	private static int cases;
	private static int compared;

	private final Entry entry;
	private final String type;
	private final String profile;
	private final int minPointApproximation;

	public ApproximationCompatTest(String name, Entry entry, String type, String profile, int minPointApproximation) {
		this.entry = entry;
		this.type = type;
		this.profile = profile;
		this.minPointApproximation = minPointApproximation;
	}

	@AfterClass
	public static void casesWereCompared() {
		System.out.println("ApproximationCompatTest: " + cases + " approximations, " + compared + " segments compared");
		assertTrue("approximations compared: " + cases, cases > 80);
		assertTrue("segments compared: " + compared, compared > 500);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() throws IOException {
		List<Object[]> data = new ArrayList<>();
		try (Reader reader = new InputStreamReader(Objects.requireNonNull(ApproximationCompatTest.class.getResourceAsStream("/approximation/test.json")))) {
			Gson gson = new GsonBuilder().create();
			for (Entry entry : gson.fromJson(reader, Entry[].class)) {
				if (entry.ignore) {
					continue;
				}
				// the defaults of ApproximationTest
				List<String> types = entry.types != null ? entry.types : Arrays.asList("routing", "geometry");
				List<String> profiles = entry.profiles != null ? entry.profiles : Arrays.asList("car");
				List<Integer> minPointApproximations = entry.minPointApproximation != null ? entry.minPointApproximation : Arrays.asList(50);
				for (String type : types) {
					for (String profile : profiles) {
						for (int mpa : minPointApproximations) {
							data.add(new Object[] {entry.gpxFile + " " + type + " " + profile + " " + mpa, entry, type, profile, mpa});
						}
					}
				}
			}
		}
		return data;
	}

	@Test(timeout = 300_000)
	public void testBothApproximationsAgree() throws Exception {
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;
		net.osmand.shared.routing.RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;
		GPXFile gpxFile = GPXUtilities.loadGPXFile(new File(RESOURCES + entry.gpxFile));
		List<GPXUtilities.WptPt> waypoints = gpxFile.tracks.get(0).segments.get(0).points;
		List<KLatLon> locations = new ArrayList<>();
		long[] times = new long[waypoints.size()];
		boolean timestamps = true;
		for (int i = 0; i < waypoints.size(); i++) {
			GPXUtilities.WptPt p = waypoints.get(i);
			locations.add(new KLatLon(p.lat, p.lon));
			times[i] = p.time;
			timestamps &= p.time != 0;
		}
		File obf = new File(RESOURCES + entry.obfFile);
		BinaryMapIndexReader[] readers = {new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf)};
		List<net.osmand.shared.binary.BinaryMapIndexReader> kreaders = new ArrayList<>();
		kreaders.add(new net.osmand.shared.binary.BinaryMapIndexReader(obf.getPath()));
		try {
			if ("routing".equals(type)) {
				compare(waypoints, locations, times, timestamps, readers, kreaders, false, GpxRouteApproximation.GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM);
			} else {
				compare(waypoints, locations, times, timestamps, readers, kreaders, true, GpxRouteApproximation.GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM);
				compare(waypoints, locations, times, timestamps, readers, kreaders, true, GpxRouteApproximation.GPX_OSM_POINTS_MATCH_ALGORITHM);
			}
		} finally {
			GpxRouteApproximation.GPX_SEGMENT_ALGORITHM = GpxRouteApproximation.GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM;
			net.osmand.shared.routing.GpxRouteApproximation.GPX_SEGMENT_ALGORITHM = net.osmand.shared.routing.GpxRouteApproximation.GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM;
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
			for (net.osmand.shared.binary.BinaryMapIndexReader reader : kreaders) {
				reader.close();
			}
		}
	}

	private void compare(List<GPXUtilities.WptPt> waypoints, List<KLatLon> locations, long[] times, boolean timestamps,
			BinaryMapIndexReader[] readers, List<net.osmand.shared.binary.BinaryMapIndexReader> kreaders,
			boolean geometry, int algorithm) throws IOException, InterruptedException {
		String m = entry.gpxFile + " " + (geometry ? (algorithm == GpxRouteApproximation.GPX_OSM_POINTS_MATCH_ALGORITHM ? "points match" : "geometry") : "routing")
				+ " " + profile + " [" + minPointApproximation + "]";
		long now = System.currentTimeMillis(); // ENABLE_TIME_CONDITIONAL_ROUTING, the same moment for both

		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
		fe.setUseNativeApproximation(false);
		fe.setUseGeometryBasedApproximation(geometry);
		GpxRouteApproximation.GPX_SEGMENT_ALGORITHM = algorithm;
		RoutingConfiguration config = RoutingConfiguration.getDefault().build(profile, new RoutingMemoryLimits(MEM_LIMIT, MEM_LIMIT), new HashMap<>());
		config.routeCalculationTime = now;
		if (minPointApproximation > 0) {
			config.minPointApproximation = minPointApproximation;
		}
		RoutingContext ctx = fe.buildRoutingContext(config, null, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		GpxRouteApproximation gctx = new GpxRouteApproximation(ctx);
		List<GpxPoint> gpxPoints = fe.generateGpxPoints(gctx, new LocationsHolder(waypoints));
		GpxRouteApproximation java = fe.searchGpxRoute(gctx, gpxPoints, null, timestamps);

		net.osmand.shared.routing.RoutePlannerFrontEnd kfe = new net.osmand.shared.routing.RoutePlannerFrontEnd();
		kfe.setUseGeometryBasedApproximation(geometry);
		net.osmand.shared.routing.GpxRouteApproximation.GPX_SEGMENT_ALGORITHM = algorithm;
		net.osmand.shared.routing.RoutingConfiguration kconfig = net.osmand.shared.routing.RoutingConfiguration.getDefault()
				.build(profile, new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(MEM_LIMIT, MEM_LIMIT), new LinkedHashMap<>());
		kconfig.routeCalculationTime = now;
		if (minPointApproximation > 0) {
			kconfig.minPointApproximation = minPointApproximation;
		}
		net.osmand.shared.routing.RoutingContext kctx = kfe.buildRoutingContext(kconfig, kreaders, RouteCalculationMode.NORMAL);
		net.osmand.shared.routing.GpxRouteApproximation kgctx = new net.osmand.shared.routing.GpxRouteApproximation(kctx);
		List<net.osmand.shared.routing.GpxPoint> kgpxPoints = kfe.generateGpxPoints(kgctx, locations, times);
		net.osmand.shared.routing.GpxRouteApproximation copy = kfe.searchGpxRoute(kgctx, kgpxPoints, null, timestamps);

		assertEquals(m + " track points", gpxPoints.size(), kgpxPoints.size());
		for (int i = 0; i < gpxPoints.size(); i++) {
			Same.close(m + " track point " + i + " cumDist", gpxPoints.get(i).cumDist, kgpxPoints.get(i).cumDist);
		}
		assertEquals(m + " approximation", java.toString(), copy.toString());
		assertEquals(m + " route points searched", java.routePointsSearched, copy.routePointsSearched);
		assertEquals(m + " route distance", java.routeDistance, copy.routeDistance);
		assertEquals(m + " visited segments", ctx.calculationProgress.visitedSegments, kctx.calculationProgress.visitedSegments);
		assertEquals(m + " final points", describe(java.finalPoints), describe(copy.finalPoints));
		List<RouteSegmentResult> route = java.collectFinalPointsAsRoute();
		List<net.osmand.shared.routing.RouteSegmentResult> kroute = copy.collectFinalPointsAsRoute();
		assertEquals(m + " segments", route.size(), kroute.size());
		assertEquals(m + " full route", java.fullRoute.size(), copy.fullRoute.size());
		for (int i = 0; i < route.size(); i++) {
			Same.segment(m + " segment " + i + " road " + route.get(i).getObject().id, route.get(i), kroute.get(i));
		}
		cases++;
		compared += route.size();
	}

	/** Which track points the route is attached at, how far each stretch reaches, and which are straight lines. */
	private static String describe(List<?> finalPoints) {
		StringBuilder b = new StringBuilder();
		for (Object o : finalPoints) {
			if (o instanceof GpxPoint) {
				GpxPoint p = (GpxPoint) o;
				b.append(p.ind).append("->").append(p.targetInd).append(p.straightLine ? " line" : " road").append(" x").append(p.routeToTarget.size()).append("\n");
			} else {
				net.osmand.shared.routing.GpxPoint p = (net.osmand.shared.routing.GpxPoint) o;
				b.append(p.ind).append("->").append(p.targetInd).append(p.straightLine ? " line" : " road").append(" x").append(p.routeToTarget.size()).append("\n");
			}
		}
		return b.toString();
	}

	/** One case of {@code approximation/test.json}; the expectations are ApproximationTest's business. */
	public static class Entry {
		String gpxFile;
		String obfFile;
		boolean ignore;
		List<String> types;
		List<String> profiles;
		List<Integer> minPointApproximation;
	}
}
