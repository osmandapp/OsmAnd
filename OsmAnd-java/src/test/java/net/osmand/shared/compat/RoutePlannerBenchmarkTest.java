package net.osmand.shared.compat;

import net.osmand.LocationsHolder;
import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.routing.RouteCalculationMode;
import net.osmand.util.MapUtils;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * The routes of the shared {@code RoutePlannerBenchmarkTest}, calculated three ways on this jvm:
 * by the java planner, by the C++ router over JNI, and by the shared planner. With the shared
 * planner's Kotlin/Native run that gives the four columns the port is judged by. A second table
 * takes the same routes as tracks and attaches them back to the roads, the routing-based and the
 * geometry-based way, by the same three.
 *
 * The maps are real ones and are not in the repository, see the directories below. The C++ column
 * needs the host library built, see {@code core-legacy/binaries/darwin/arm64/Release/libosmand.dylib};
 * without it the column says so.
 *
 * <b>Disabled on purpose</b>: it takes minutes and prints numbers. To measure, lift the
 * {@code @Ignore} and run {@code ./gradlew :OsmAnd-java:test --tests "*RoutePlannerBenchmarkTest" -i}.
 */
@Ignore
public class RoutePlannerBenchmarkTest {

	private static final int WARMUP_ROUNDS = 1;
	private static final int MEASURED_ROUNDS = 2;
	private static final double TRACK_SPACING_M = 15; // a point a second at 55 km/h
	private static final double TRACK_OFFSET_LAT = 0.00002, TRACK_OFFSET_LON = 0.00003; // ~2 m north and ~2 m east

	private static final List<String> OBF_DIRECTORIES = Arrays.asList(
			"/Users/crimean/tmp/maps",
			"/Users/crimean/Library/Developer/CoreSimulator/Devices/2B4A49F7-4769-4207-93AD-2DFF3B315735/data/Containers/Data/Application/1D97C071-99BF-41BC-BC02-A1CF7EEF3BCD/Documents/Resources");

	private static final String NATIVE_LIB_DIR = "../../core-legacy/binaries/darwin/arm64/Release";

	private static final String NOORD_HOLLAND = "Netherlands_noord-holland_europe.obf";
	private static final String BAVARIA = "Germany_bayern_upper-bavaria_europe.obf";
	private static final String LOWER_AUSTRIA = "Austria_lower-austria_europe.obf";

	private static class Route {
		final String name;
		final List<String> maps;
		final LatLon start;
		final LatLon end;

		Route(String name, List<String> maps, LatLon start, LatLon end) {
			this.name = name;
			this.maps = maps;
			this.start = start;
			this.end = end;
		}
	}

	/** The same list as in the shared test; keep the two together. */
	private static final List<Route> ROUTES = Arrays.asList(
			new Route("amsterdam schiphol 17 km", Arrays.asList(NOORD_HOLLAND), new LatLon(52.3791, 4.9003), new LatLon(52.3105, 4.7683)),
			new Route("amsterdam haarlem 20 km", Arrays.asList(NOORD_HOLLAND), new LatLon(52.3791, 4.9003), new LatLon(52.3874, 4.6462)),
			new Route("amsterdam den helder 80 km", Arrays.asList(NOORD_HOLLAND), new LatLon(52.3791, 4.9003), new LatLon(52.9563, 4.7606)),
			new Route("munich airport 35 km", Arrays.asList(BAVARIA), new LatLon(48.1374, 11.5755), new LatLon(48.3538, 11.7861)),
			new Route("munich rosenheim 65 km", Arrays.asList(BAVARIA), new LatLon(48.1374, 11.5755), new LatLon(47.8561, 12.1289)),
			new Route("munich garmisch 90 km", Arrays.asList(BAVARIA), new LatLon(48.1374, 11.5755), new LatLon(47.4917, 11.0954)),
			new Route("vienna schwechat 20 km", Arrays.asList(LOWER_AUSTRIA), new LatLon(48.2082, 16.3738), new LatLon(48.1103, 16.5697)),
			new Route("st poelten krems 30 km", Arrays.asList(LOWER_AUSTRIA), new LatLon(48.2047, 15.6256), new LatLon(48.4103, 15.6136)),
			new Route("st poelten wr neustadt 70 km", Arrays.asList(LOWER_AUSTRIA), new LatLon(48.2047, 15.6256), new LatLon(47.8100, 16.2450)));

	@Test
	public void benchmarkRoutes() throws IOException {
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;
		net.osmand.shared.routing.RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;
		RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false;
		NativeLibrary nativeLib = null;
		File libDir = new File(NATIVE_LIB_DIR);
		if (libDir.exists() && NativeLibrary.loadOldLib(libDir.getAbsolutePath())) {
			nativeLib = new NativeLibrary();
		}
		System.out.println();
		System.out.println("### route planner on jvm " + System.getProperty("java.version") + ": java, C++ over JNI" + (nativeLib == null ? " (library not found)" : "")
				+ ", shared copy, each A* and HH; " + WARMUP_ROUNDS + " warmup / " + MEASURED_ROUNDS + " measured, best time");
		System.out.println();
		System.out.printf(Locale.US, "  %-28s %-10s %8s %9s %8s %10s %9s %7s%n", "route", "by", "ms", "segments", "km", "routing s", "visited", "tiles");
		for (Route route : ROUTES) {
			List<File> files = mapsOf(route);
			if (files == null) {
				continue;
			}
			runJava(route, files, null, false);
			if (nativeLib != null) {
				for (File f : files) {
					nativeLib.initMapFile(f.getAbsolutePath(), true);
				}
				runJava(route, files, nativeLib, false);
			}
			runShared(route, files, false);
			// the same routes over the hub graph, as the apps calculate them by default; only HH, so a
			// fallback to A* would show as an error rather than as a slow HH
			runJava(route, files, null, true);
			if (nativeLib != null) {
				runJava(route, files, nativeLib, true);
			}
			runShared(route, files, true);
		}
		System.out.println();
		// the same routes as tracks - the A* route's road points thinned to the spacing of a recorded
		// track - attached back to the roads, the routing-based way and the geometry-based way
		System.out.println("### gpx approximation on jvm " + System.getProperty("java.version") + ": java, C++ over JNI" + (nativeLib == null ? " (library not found)" : "")
				+ ", shared copy, each routing-based and geometry-based; " + WARMUP_ROUNDS + " warmup / " + MEASURED_ROUNDS + " measured, best time");
		System.out.println();
		System.out.printf(Locale.US, "  %-28s %-14s %8s %9s %8s %8s %9s %7s%n", "route", "by", "ms", "segments", "km", "points", "visited", "tiles");
		for (Route route : ROUTES) {
			List<File> files = mapsOf(route);
			if (files == null) {
				continue;
			}
			List<LatLon> track = track(route, files);
			for (boolean geometry : new boolean[] {false, true}) {
				runJavaApproximation(route, files, track, null, geometry);
				if (nativeLib != null) {
					runJavaApproximation(route, files, track, nativeLib, geometry);
				}
				runSharedApproximation(route, files, track, geometry);
			}
		}
		System.out.println();
	}

	private static List<File> mapsOf(Route route) {
		List<File> files = new ArrayList<>();
		for (String map : route.maps) {
			File f = find(map);
			if (f == null) {
				System.out.printf(Locale.US, "  %-28s map not found: %s%n", route.name, map);
				return null;
			}
			files.add(f);
		}
		return files;
	}

	/**
	 * The route's road points thinned to the spacing of a track recorded at driving speed, a point a
	 * second, and moved a couple of metres off the road: a recording never lies on the road's own nodes,
	 * and on a node several roads are the same distance away, where the planners pick by the order they
	 * met the roads in - java and the copy iterate their hash tables differently, so the route they attach
	 * would differ by a road here and there for a reason that has nothing to do with the approximation.
	 */
	private static List<LatLon> track(Route route, List<File> files) throws IOException {
		BinaryMapIndexReader[] readers = open(files);
		try {
			RoutingConfiguration config = RoutingConfiguration.getDefault().build("car", limits(), new HashMap<>());
			RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
			RoutingContext ctx = fe.buildRoutingContext(config, null, readers);
			RouteCalcResult res;
			try {
				res = fe.searchRoute(ctx, route.start, route.end, null);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
			List<LatLon> track = new ArrayList<>();
			LatLon last = null;
			for (RouteSegmentResult s : res.getList()) {
				int step = s.getStartPointIndex() < s.getEndPointIndex() ? 1 : -1;
				for (int i = s.getStartPointIndex(); ; i += step) {
					LatLon p = s.getPoint(i);
					last = new LatLon(p.getLatitude() + TRACK_OFFSET_LAT, p.getLongitude() + TRACK_OFFSET_LON);
					if (track.isEmpty() || MapUtils.getDistance(track.get(track.size() - 1), last) >= TRACK_SPACING_M) {
						track.add(last);
					}
					if (i == s.getEndPointIndex()) {
						break;
					}
				}
			}
			if (last != null && !track.get(track.size() - 1).equals(last)) {
				track.add(last);
			}
			return track;
		} finally {
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
		}
	}

	private static BinaryMapIndexReader[] open(List<File> files) throws IOException {
		BinaryMapIndexReader[] readers = new BinaryMapIndexReader[files.size()];
		for (int i = 0; i < files.size(); i++) {
			readers[i] = new BinaryMapIndexReader(new RandomAccessFile(files.get(i), "r"), files.get(i));
		}
		return readers;
	}

	private void runJavaApproximation(Route route, List<File> files, List<LatLon> track, NativeLibrary nativeLib, boolean geometry) throws IOException {
		BinaryMapIndexReader[] readers = open(files);
		try {
			double best = Double.MAX_VALUE;
			String line = "";
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				long start = System.nanoTime();
				RoutingConfiguration config = RoutingConfiguration.getDefault().build("car", limits(), new HashMap<>());
				RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
				fe.setUseNativeApproximation(nativeLib != null);
				fe.setUseGeometryBasedApproximation(geometry);
				RoutingContext ctx = fe.buildRoutingContext(config, nativeLib, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
				ctx.calculationProgress = new RouteCalculationProgress(); // the java path makes one itself, the JNI path does not
				GpxRouteApproximation gctx = new GpxRouteApproximation(ctx);
				List<GpxPoint> points = fe.generateGpxPoints(gctx, new LocationsHolder(track));
				GpxRouteApproximation res;
				try {
					res = fe.searchGpxRoute(gctx, points, null, false);
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
				double ms = (System.nanoTime() - start) / 1.0e6;
				if (round >= WARMUP_ROUNDS && ms < best) {
					best = ms;
					double km = 0;
					for (RouteSegmentResult s : res.fullRoute) {
						km += s.getDistance();
					}
					line = String.format(Locale.US, "%9d %8.1f %8d ", res.fullRoute.size(), km / 1000, points.size())
							+ (nativeLib == null
									? String.format(Locale.US, "%9d %7d", ctx.calculationProgress.visitedSegments, ctx.calculationProgress.loadedTiles)
									: String.format(Locale.US, "%9s %7s", "-", "-")); // the JNI path reports no counters
				}
			}
			System.out.printf(Locale.US, "  %-28s %-14s %8.1f %s%n", route.name, (nativeLib == null ? "java" : "cpp") + " gpx" + (geometry ? " geo" : ""), best, line);
		} finally {
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
		}
	}

	private void runSharedApproximation(Route route, List<File> files, List<LatLon> track, boolean geometry) {
		List<net.osmand.shared.binary.BinaryMapIndexReader> readers = new ArrayList<>();
		for (File f : files) {
			readers.add(new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
		}
		List<KLatLon> locations = new ArrayList<>();
		for (LatLon l : track) {
			locations.add(new KLatLon(l.getLatitude(), l.getLongitude()));
		}
		try {
			double best = Double.MAX_VALUE;
			String line = "";
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				long start = System.nanoTime();
				net.osmand.shared.routing.RoutingConfiguration config = net.osmand.shared.routing.RoutingConfiguration.getDefault()
						.build("car", new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(
								RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT), new LinkedHashMap<>());
				net.osmand.shared.routing.RoutePlannerFrontEnd fe = new net.osmand.shared.routing.RoutePlannerFrontEnd();
				fe.setUseGeometryBasedApproximation(geometry);
				net.osmand.shared.routing.RoutingContext ctx = fe.buildRoutingContext(config, readers, RouteCalculationMode.NORMAL);
				net.osmand.shared.routing.GpxRouteApproximation gctx = new net.osmand.shared.routing.GpxRouteApproximation(ctx);
				List<net.osmand.shared.routing.GpxPoint> points = fe.generateGpxPoints(gctx, locations, null);
				net.osmand.shared.routing.GpxRouteApproximation res = fe.searchGpxRoute(gctx, points, null, false);
				double ms = (System.nanoTime() - start) / 1.0e6;
				if (round >= WARMUP_ROUNDS && ms < best) {
					best = ms;
					double km = 0;
					for (net.osmand.shared.routing.RouteSegmentResult s : res.fullRoute) {
						km += s.getDistance();
					}
					line = String.format(Locale.US, "%9d %8.1f %8d %9d %7d", res.fullRoute.size(), km / 1000, points.size(),
							ctx.calculationProgress.visitedSegments, ctx.calculationProgress.loadedTiles);
				}
			}
			System.out.printf(Locale.US, "  %-28s %-14s %8.1f %s%n", route.name, "shared gpx" + (geometry ? " geo" : ""), best, line);
		} finally {
			for (net.osmand.shared.binary.BinaryMapIndexReader reader : readers) {
				reader.close();
			}
		}
	}

	private void runJava(Route route, List<File> files, NativeLibrary nativeLib, boolean hh) throws IOException {
		BinaryMapIndexReader[] readers = new BinaryMapIndexReader[files.size()];
		for (int i = 0; i < files.size(); i++) {
			readers[i] = new BinaryMapIndexReader(new RandomAccessFile(files.get(i), "r"), files.get(i));
		}
		try {
			double best = Double.MAX_VALUE;
			String line = "";
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				long start = System.nanoTime();
				RoutingConfiguration config = RoutingConfiguration.getDefault().build("car", limits(), new HashMap<>());
				RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
				if (hh) {
					fe.setDefaultHHRoutingConfig();
					fe.setUseOnlyHHRouting(true);
					fe.setHHRouteCpp(nativeLib != null);
				}
				RoutingContext ctx = fe.buildRoutingContext(config, nativeLib, readers);
				RouteCalcResult res;
				try {
					res = fe.searchRoute(ctx, route.start, route.end, null);
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
				double ms = (System.nanoTime() - start) / 1.0e6;
				if (round >= WARMUP_ROUNDS && ms < best) {
					best = ms;
					if (res.getError() != null) {
						line = "error: " + res.getError();
					} else {
						double km = 0;
						for (RouteSegmentResult s : res.getList()) {
							km += s.getDistance();
						}
						line = String.format(Locale.US, "%9d %8.1f %10.1f %9d %7d", res.getList().size(), km / 1000, ctx.routingTime,
								ctx.calculationProgress.visitedSegments, ctx.calculationProgress.loadedTiles);
					}
				}
			}
			System.out.printf(Locale.US, "  %-28s %-10s %8.1f %s%n", route.name, (nativeLib == null ? "java" : "cpp") + (hh ? " hh" : ""), best, line);
		} finally {
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
		}
	}

	private void runShared(Route route, List<File> files, boolean hh) {
		List<net.osmand.shared.binary.BinaryMapIndexReader> readers = new ArrayList<>();
		for (File f : files) {
			readers.add(new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
		}
		try {
			double best = Double.MAX_VALUE;
			String line = "";
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				long start = System.nanoTime();
				net.osmand.shared.routing.RoutingConfiguration config = net.osmand.shared.routing.RoutingConfiguration.getDefault()
						.build("car", new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(
								RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT), new LinkedHashMap<>());
				net.osmand.shared.routing.RoutePlannerFrontEnd fe = new net.osmand.shared.routing.RoutePlannerFrontEnd();
				net.osmand.shared.routing.RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false;
				if (hh) {
					fe.setDefaultHHRoutingConfig();
					fe.setUseOnlyHHRouting(true);
				}
				net.osmand.shared.routing.RoutingContext ctx = fe.buildRoutingContext(config, readers);
				net.osmand.shared.routing.RouteCalcResult res = fe.searchRoute(ctx,
						new KLatLon(route.start.getLatitude(), route.start.getLongitude()),
						new KLatLon(route.end.getLatitude(), route.end.getLongitude()), null);
				double ms = (System.nanoTime() - start) / 1.0e6;
				if (round >= WARMUP_ROUNDS && ms < best) {
					best = ms;
					if (res.getError() != null) {
						line = "error: " + res.getError();
					} else {
						double km = 0;
						for (net.osmand.shared.routing.RouteSegmentResult s : res.getList()) {
							km += s.getDistance();
						}
						line = String.format(Locale.US, "%9d %8.1f %10.1f %9d %7d", res.getList().size(), km / 1000, ctx.routingTime,
								ctx.calculationProgress.visitedSegments, ctx.calculationProgress.loadedTiles);
					}
				}
			}
			System.out.printf(Locale.US, "  %-28s %-10s %8.1f %s%n", route.name, hh ? "shared hh" : "shared", best, line);
		} finally {
			for (net.osmand.shared.binary.BinaryMapIndexReader reader : readers) {
				reader.close();
			}
		}
	}

	/** The 256 MB the C++ router is given, for the java planner too, so the tile cache does not thrash on the long routes. */
	private static RoutingMemoryLimits limits() {
		return new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
	}

	private static File find(String name) {
		for (String directory : OBF_DIRECTORIES) {
			File f = new File(directory, name);
			if (f.exists()) {
				return f;
			}
		}
		return null;
	}
}
