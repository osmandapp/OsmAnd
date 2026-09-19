package net.osmand.router;

import static net.osmand.util.RouterUtilTest.getNativeLibPath;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

/**
 * TODO #17773 temporary: checks that the native library (core-legacy/binaries) has the ferry changes.
 * Run it alone (the init script lets tests see the library):
 * ./gradlew -I ../native-tests.init.gradle :OsmAnd-java:cleanTest :OsmAnd-java:test --tests 'net.osmand.router.TmpFerryNativeCheckTest' -i
 * and look for "FERRYNATIVE" lines. It fails if native results differ from Java, and it's skipped if the library
 * isn't found (without the init script too).
 * Remove before the PR.
 */
public class TmpFerryNativeCheckTest {

	private static final double TIME_TOLERANCE_MIN = 0.25;
	private static final double COST_TOLERANCE = 0.02;

	// obf, profile, start, end
	private static final Object[][] ROUTES = {
			{"ferry_sandbanks", "car", 50.67782, -1.95123, 50.68383, -1.94831},
			{"ferry_gullmarsleden", "pedestrian", 58.30263, 11.50282, 58.29961, 11.53579},
			{"ferry_nordoleden", "car", 57.77241, 11.61966, 57.75831, 11.61556},
			{"ferry_kungshamn", "pedestrian", 58.36137, 11.24880, 58.35333, 11.22505},
			{"ferry_bijela", "car", 42.466581, 18.674241, 42.465587, 18.68559},
	};

	// obf, start, end
	private static final Object[][] PT_ROUTES = {
			{"ferry_sandbanks", 50.67782, -1.95123, 50.68383, -1.94831},
			{"ferry_sandbanks", 50.679, -1.95, 50.683, -1.946},
			{"ferry_kungshamn", 58.36137, 11.24880, 58.35333, 11.22505},
			{"ferry_nordoleden", 57.75831, 11.61556, 57.77241, 11.61966},
			{"ferry_bijela", 42.466581, 18.674241, 42.465587, 18.68559},
	};

	private final List<String> errors = new ArrayList<>();

	@Test
	public void compareJavaAndNative() throws Exception {
		String libPath = getNativeLibPath();
		log("native library dir: " + libPath);
		// skipped in usual builds: tests see the library only with the init script
		Assume.assumeNotNull(libPath);
		File lib = new File(libPath, System.mapLibraryName("osmand"));
		log(String.format(Locale.US, "native library file: %s, modified %tF %<tT", lib, lib.lastModified()));
		Assert.assertTrue("Native library isn't loaded: " + lib, NativeLibrary.loadOldLib(libPath));
		NativeLibrary nativeLibrary = new NativeLibrary();

		for (Object[] r : ROUTES) {
			BinaryMapIndexReader[] readers = open((String) r[0], nativeLibrary);
			LatLon start = new LatLon((Double) r[2], (Double) r[3]);
			LatLon end = new LatLon((Double) r[4], (Double) r[5]);
			compareRoute((String) r[0], (String) r[1], start, end, readers, nativeLibrary);
		}
		for (Object[] r : PT_ROUTES) {
			BinaryMapIndexReader[] readers = open((String) r[0], nativeLibrary);
			LatLon start = new LatLon((Double) r[1], (Double) r[2]);
			LatLon end = new LatLon((Double) r[3], (Double) r[4]);
			comparePTRoute((String) r[0], start, end, readers, nativeLibrary);
		}
		log(errors.isEmpty() ? "RESULT: native has the ferry changes" : "RESULT: native differs from java:\n" + String.join("\n", errors));
		Assert.assertTrue(String.join("\n", errors), errors.isEmpty());
	}

	private void compareRoute(String obf, String profile, LatLon start, LatLon end,
	                          BinaryMapIndexReader[] readers, NativeLibrary nativeLibrary) throws Exception {
		RoutePlannerFrontEnd planner = new RoutePlannerFrontEnd();
		RoutingContext javaCtx = planner.buildRoutingContext(config(profile), null, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		javaCtx.calculationProgress = new RouteCalculationProgress();
		List<RouteSegmentResult> javaRoute = planner.searchRoute(javaCtx, start, end, null).detailed;
		double javaEta = getTime(javaRoute, false);
		double javaFerry = getTime(javaRoute, true);
		double javaCost = javaCtx.routingTime;

		// raw native result: RoutePlannerFrontEnd recalculates ferry times in java after native routing
		RoutingContext nativeCtx = planner.buildRoutingContext(config(profile), nativeLibrary, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		nativeCtx.calculationProgress = new RouteCalculationProgress();
		nativeCtx.requestNativePrepareResult = true;
		nativeCtx.startX = MapUtils.get31TileNumberX(start.getLongitude());
		nativeCtx.startY = MapUtils.get31TileNumberY(start.getLatitude());
		nativeCtx.targetX = MapUtils.get31TileNumberX(end.getLongitude());
		nativeCtx.targetY = MapUtils.get31TileNumberY(end.getLatitude());
		nativeCtx.intermediatesX = new int[0];
		nativeCtx.intermediatesY = new int[0];
		RouteSegmentResult[] nativeRoute = nativeLibrary.runNativeRouting(nativeCtx, null,
				nativeCtx.reverseMap.keySet().toArray(new RouteRegion[0]), false);
		List<RouteSegmentResult> nativeList = nativeRoute == null ? Collections.emptyList() : Arrays.asList(nativeRoute);
		// raw native route has no precise start and end, so only ferry segments are compared
		double nativeEta = getTime(nativeList, false);
		double nativeFerry = getTime(nativeList, true);
		double nativeCost = nativeCtx.calculationProgress.routingCalculatedTime;

		String name = obf + " " + profile;
		log(String.format(Locale.US, "%-32s java: eta %6.2f (ferry %6.2f), search %6.2f min | native: eta %6.2f (ferry %6.2f), search %6.2f min",
				name, javaEta / 60, javaFerry / 60, javaCost / 60, nativeEta / 60, nativeFerry / 60, nativeCost / 60));
		check(name + " ferry eta", javaFerry, nativeFerry, TIME_TOLERANCE_MIN * 60);
		check(name + " search", javaCost, nativeCost, Math.max(TIME_TOLERANCE_MIN * 60, javaCost * COST_TOLERANCE));
	}

	private void comparePTRoute(String obf, LatLon start, LatLon end, BinaryMapIndexReader[] readers,
	                            NativeLibrary nativeLibrary) throws Exception {
		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		TransportRoutingConfiguration cfg = new TransportRoutingConfiguration(builder, builder.getRouter("public_transport"),
				Collections.emptyMap());
		List<TransportRouteResult> javaRes = new TransportRoutePlanner().buildRoute(new TransportRoutingContext(cfg, null, readers), start, end);

		NativeTransportRoutingResult[] nativeRaw = nativeLibrary.runNativePTRouting(
				MapUtils.get31TileNumberX(start.getLongitude()), MapUtils.get31TileNumberY(start.getLatitude()),
				MapUtils.get31TileNumberX(end.getLongitude()), MapUtils.get31TileNumberY(end.getLatitude()),
				cfg, new RouteCalculationProgress());
		List<TransportRouteResult> nativeRes = TransportRoutePlanner.convertToTransportRoutingResult(
				nativeRaw == null ? new NativeTransportRoutingResult[0] : nativeRaw, cfg);
		// java planner temporarily shows only routes with ferries
		nativeRes.removeIf(r -> r.getSegments().stream().noneMatch(s -> TransportFerryHelper.isFerry(s.route)));

		String name = obf + " pt " + start;
		log(String.format(Locale.US, "%-32s java: %s | native: %s", name, describe(javaRes), describe(nativeRes)));
		if (javaRes.isEmpty() || nativeRes.isEmpty()) {
			if (javaRes.isEmpty() != nativeRes.isEmpty()) {
				errors.add(name + ": java " + javaRes.size() + " routes, native " + nativeRes.size() + " routes");
			}
			return;
		}
		check(name + " time", javaRes.get(0).getRouteTime(), nativeRes.get(0).getRouteTime(), TIME_TOLERANCE_MIN * 60);
		String javaRefs = getRefs(javaRes.get(0));
		String nativeRefs = getRefs(nativeRes.get(0));
		if (!javaRefs.equals(nativeRefs)) {
			errors.add(name + ": java " + javaRefs + ", native " + nativeRefs);
		}
	}

	private void check(String name, double java, double nat, double tolerance) {
		if (Math.abs(java - nat) > tolerance) {
			errors.add(String.format(Locale.US, "%s: java %.2f min, native %.2f min", name, java / 60, nat / 60));
		}
	}

	private static String describe(List<TransportRouteResult> results) {
		StringBuilder sb = new StringBuilder();
		for (TransportRouteResult r : results) {
			sb.append(String.format(Locale.US, "[%.2f min %s]", r.getRouteTime() / 60, getRefs(r)));
		}
		return results.size() + " routes " + sb;
	}

	private static String getRefs(TransportRouteResult r) {
		StringBuilder sb = new StringBuilder();
		for (TransportRoutePlanner.TransportRouteResultSegment s : r.getSegments()) {
			sb.append(s.route.getRef()).append(' ').append(s.start).append('-').append(s.end).append(';');
		}
		return sb.toString();
	}

	private static double getTime(List<RouteSegmentResult> route, boolean onlyFerry) {
		double time = 0;
		for (RouteSegmentResult s : route) {
			if (!onlyFerry || FerryRoutingHelper.isFerry(s.getObject())) {
				time += s.getSegmentTime();
			}
		}
		return time;
	}

	private static RoutingConfiguration config(String profile) {
		return RoutingConfiguration.getDefault().build(profile, new RoutingConfiguration.RoutingMemoryLimits(
				RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT));
	}

	private static BinaryMapIndexReader[] open(String obf, NativeLibrary nativeLibrary) throws Exception {
		File file = new File("src/test/resources/routing/" + obf + ".obf");
		nativeLibrary.initMapFile(file.getAbsolutePath(), true);
		return new BinaryMapIndexReader[] {new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file)};
	}

	private static void log(String msg) {
		System.out.println("FERRYNATIVE " + msg);
	}
}
