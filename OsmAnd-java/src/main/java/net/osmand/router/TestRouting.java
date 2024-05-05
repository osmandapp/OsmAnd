package net.osmand.router;


import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class TestRouting {
	
	public static int MEMORY_TEST_LIMIT = 800;
	public static int NATIVE_MEMORY_TEST_LIMIT = 256;
	public static boolean TEST_WO_HEURISTIC = false;
	public static boolean TEST_BOTH_DIRECTION = false;
	public static NativeLibrary lib = null;
	private static String vehicle = "car";
	
	
	public static class Parameters {
		public File obfDir;
		public List<File> tests = new ArrayList<File>();
		public double startLat = 0;
		public double startLon = 0;
		public double endLat = 0;
		public double endLon = 0;
		public RoutingConfiguration.Builder configBuilder;
		public String vehicle = "car";
		
		public static Parameters init(String[] args) throws IOException, XmlPullParserException {
			Parameters p = new Parameters();
			String routingXmlFile = null;
			String obfDirectory = null;
			RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
			for (String a : args) {
				if (a.startsWith("-routingXmlPath=")) {
					routingXmlFile = a.substring("-routingXmlPath=".length());
				} else if (a.startsWith("-verbose")) {
					RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
				} else if (a.startsWith("-obfDir=")) {
					obfDirectory = a.substring("-obfDir=".length());
				} else if (a.startsWith("-vehicle=")) {
					p.vehicle = a.substring("-vehicle=".length());
				} else if (a.startsWith("-start=")) {
					String start = a.substring("-start=".length());
					String[] pt = start.split(";");
					p.startLat = Double.parseDouble(pt[0]);
					p.startLon = Double.parseDouble(pt[1]);
				} else if (a.startsWith("-end=")) {
					String start = a.substring("-end=".length());
					String[] pt = start.split(";");
					p.endLat = Double.parseDouble(pt[0]);
					p.endLon = Double.parseDouble(pt[1]);
				} else if (a.startsWith("-testDir=")) {
					String testDirectory = a.substring("-testDir=".length());
					for(File f : new File(testDirectory).listFiles()) {
						if(f.getName().endsWith(".test.xml")){
							p.tests.add(f);
						}
					}
				} else if(!a.startsWith("-")){
					p.tests.add(new File(a));
				}
			}

			if (obfDirectory == null) {
				throw new IllegalStateException("Directory with obf files not specified");
			}
			p.obfDir = new File(obfDirectory);

			if (routingXmlFile == null || routingXmlFile.equals("routing.xml")) {
				p.configBuilder = RoutingConfiguration.getDefault();
			} else {
				p.configBuilder = RoutingConfiguration.parseFromInputStream(new FileInputStream(routingXmlFile));
			}

			return p;
		}
	}
	
	public static void main(String[] args) throws Exception {
		if(args == null || args.length == 0) {
			info();
			return;
		}
		long time = System.currentTimeMillis();
		Parameters params = Parameters.init(args);
		if((params.tests.isEmpty() && params.startLat == 0) || params.obfDir == null) {
			info();
			return;
		}
		if (!params.tests.isEmpty()) {
			boolean allSuccess = runAllTests(params, lib);
			if (allSuccess) {
				System.out.println("All is successful " + (System.currentTimeMillis() - time) + " ms");
			}
		}
		if(params.startLat != 0) {
//			calculateRoute(params.obfDir.getAbsolutePath(), params.startLat, params.startLon,
//					params.endLat, params.endLon);
			BinaryMapIndexReader[] rs = collectFiles(params.obfDir.getAbsolutePath());
			vehicle = params.vehicle;
			calculateRoute(params.startLat, params.startLon,
					params.endLat, params.endLon, rs);
			calculateRoute(params.startLat, params.startLon,
					params.endLat, params.endLon, rs);
		}

	}


	public static boolean runAllTests(Parameters params, NativeLibrary lib) throws FileNotFoundException, IOException, Exception {
		BinaryMapIndexReader[] rs = collectFiles(params.obfDir.getAbsolutePath());
		
		boolean allSuccess = true;
		for(File f : params.tests) {
			System.out.println("Before test " + f.getAbsolutePath());
			System.out.flush();
			allSuccess &= test(lib, new FileInputStream(f), rs, params.configBuilder);	
		}
		return allSuccess;
	}


	public static void info() {
		println("Run router tests is console utility to test route calculation for osmand. It is also possible to calculate one route from -start to -end.");
		println("\nUsage for run tests : runTestsSuite [-routingXmlPath=PATH] [-verbose] [-obfDir=PATH] [-vehicle=VEHICLE_STRING] [-start=lat;lon] [-end=lat;lon]  [-testDir=PATH] {individualTestPath}");
    }
	

	private static void println(String string) {
		System.out.println(string);
	}


	public static boolean test(NativeLibrary lib, InputStream resource, BinaryMapIndexReader[] rs, RoutingConfiguration.Builder config) throws Exception {
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(resource, "UTF-8");
		int tok;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG) {
				String name = parser.getName();
				if(name.equals("test")){
					testRoute(parser, config, lib, rs);
				}
			}
		}

		return true;
	}
	
	private static float parseFloat(XmlPullParser parser, String attr) {
		String v = parser.getAttributeValue("", attr);
		if(v == null || v.length() == 0){
			return 0;
		}
		return Float.parseFloat(v);
	}
	private static boolean isInOrLess(float expected, float value, float percent){
		if(equalPercent(expected, value, percent)){
			return true;
		}
		if(value < expected) {
			System.err.println("Test could be adjusted value "  + value + " is much less then expected " + expected);
			return true;
		}
		return false;
	}
	
	private static boolean equalPercent(float expected, float value, float percent){
		if(Math.abs(value/expected - 1) < percent / 100){
			return true;
		}
		return false;
	}

	private static void testRoute(XmlPullParser parser, Builder config, NativeLibrary lib, BinaryMapIndexReader[] rs) throws IOException, InterruptedException {
		String vehicle = parser.getAttributeValue("", "vehicle");
		int loadedTiles = (int) parseFloat(parser, "loadedTiles");
		int visitedSegments = (int) parseFloat(parser, "visitedSegments");
		int complete_time = (int) parseFloat(parser, "complete_time");
		int routing_time = (int) parseFloat(parser, "routing_time");
		int complete_distance = (int) parseFloat(parser, "complete_distance");
		float percent = parseFloat(parser, "best_percent");
		String testDescription = parser.getAttributeValue("", "description");
		if(percent == 0){
			System.err.println("\n\n!! Skipped test case '" + testDescription + "' because 'best_percent' attribute is not specified \n\n" );
			return;
		}
		RoutingMemoryLimits memoryLimits = new RoutingMemoryLimits(MEMORY_TEST_LIMIT, NATIVE_MEMORY_TEST_LIMIT);
		RoutingConfiguration rconfig = config.build(vehicle, memoryLimits);
		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();
		RoutingContext ctx = router.buildRoutingContext(rconfig, 
				lib, rs);
		String skip = parser.getAttributeValue("", "skip_comment");
		if (skip != null && skip.length() > 0) {
			System.err.println("\n\n!! Skipped test case '" + testDescription + "' because '" + skip + "'\n\n" );
			return;
		}
		System.out.println("Run test " + testDescription);
		
		double startLat = Double.parseDouble(parser.getAttributeValue("", "start_lat"));
		double startLon = Double.parseDouble(parser.getAttributeValue("", "start_lon"));
		double endLat = Double.parseDouble(parser.getAttributeValue("", "target_lat"));
		double endLon = Double.parseDouble(parser.getAttributeValue("", "target_lon"));
		LatLon start = new LatLon(startLat, startLon);
		LatLon end = new LatLon(endLat, endLon);
		List<RouteSegmentResult> route = router.searchRoute(ctx, start, end, null).detailed;
		final float calcRoutingTime = ctx.routingTime;
		float completeTime = 0;
		float completeDistance = 0;
		for (int i = 0; i < route.size(); i++) {
			completeTime += route.get(i).getSegmentTime();
			completeDistance += route.get(i).getDistance();
		}
		if(complete_time > 0 && !isInOrLess(complete_time, completeTime, percent)) {
			throw new IllegalArgumentException(MessageFormat.format("Complete time (expected) {0} != {1} (original) : {2}", complete_time, completeTime, testDescription));
		}
		if(complete_distance > 0 && !isInOrLess(complete_distance, completeDistance, percent)) {
			throw new IllegalArgumentException(MessageFormat.format("Complete distance (expected) {0} != {1} (original) : {2}", complete_distance, completeDistance, testDescription));
		}
		if(routing_time > 0 && !isInOrLess(routing_time, calcRoutingTime, percent)) {
			throw new IllegalArgumentException(MessageFormat.format("Complete routing time (expected) {0} != {1} (original) : {2}", routing_time, calcRoutingTime, testDescription));
		}

		if (visitedSegments > 0 && !isInOrLess(visitedSegments, ctx.getVisitedSegments(), percent)) {
			throw new IllegalArgumentException(MessageFormat.format("Visited segments (expected) {0} != {1} (original) : {2}", visitedSegments,
					ctx.getVisitedSegments(), testDescription));
		}
		if (loadedTiles > 0 && !isInOrLess(loadedTiles, ctx.getLoadedTiles(), percent)) {
			throw new IllegalArgumentException(MessageFormat.format("Loaded tiles (expected) {0} != {1} (original) : {2}", loadedTiles,
					ctx.getLoadedTiles(), testDescription));
		}
		
		if(TEST_BOTH_DIRECTION){
			rconfig.planRoadDirection = -1;
			runTestSpecialTest(lib, rs, rconfig, router, start, end, calcRoutingTime, "Calculated routing time in both direction {0} != {1} time in -1 direction");
			
			rconfig.planRoadDirection = 1;
			runTestSpecialTest(lib, rs, rconfig, router, start, end, calcRoutingTime, "Calculated routing time in both direction {0} != {1} time in 1 direction");
			
		}
		
		if(TEST_WO_HEURISTIC) {
			rconfig.planRoadDirection = 0;
			rconfig.heuristicCoefficient = 0.5f;
			runTestSpecialTest(lib, rs, rconfig, router, start, end, calcRoutingTime, 
				"Calculated routing time with heuristic 1 {0} != {1} with heuristic 0.5");
		}
		
	}


	private static void runTestSpecialTest(NativeLibrary lib, BinaryMapIndexReader[] rs, RoutingConfiguration rconfig, RoutePlannerFrontEnd router,
			LatLon start, LatLon end, final float calcRoutingTime, String msg) throws IOException, InterruptedException {
		RoutingContext ctx = router.buildRoutingContext(rconfig, lib, rs);
		router.searchRoute(ctx, start, end, null);
		FinalRouteSegment frs = ctx.finalRouteSegment;
		if(frs == null || !equalPercent(calcRoutingTime, frs.distanceFromStart, 0.5f)){
			throw new IllegalArgumentException(MessageFormat.format(msg, calcRoutingTime+"",frs == null?"0":frs.distanceFromStart+""));
		}
	}

	
	
	public static void calculateRoute(String folderWithObf,
			double startLat, double startLon, double endLat, double endLon) throws IOException, InterruptedException {
		BinaryMapIndexReader[] rs = collectFiles(folderWithObf);
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
		calculateRoute(startLat, startLon, endLat, endLon, rs);
		calculateRoute(startLat, startLon, endLat, endLon, rs);
	}
	
	private static BinaryMapIndexReader[] collectFiles(String folderWithObf) throws FileNotFoundException, IOException {
		List<File> files = new ArrayList<File>();
		for (File f : new File(folderWithObf).listFiles()) {
			if (f.getName().endsWith(".obf")) {
				files.add(f);
			}
		}
		BinaryMapIndexReader[] rs = new BinaryMapIndexReader[files.size()];
		int it = 0;
		for (File f : files) {
			RandomAccessFile raf = new RandomAccessFile(f.getAbsolutePath(), "r"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(f.getName());
			rs[it++] = new BinaryMapIndexReader(raf, f);
		}
		return rs;
	}


	private static void calculateRoute(double startLat, double startLon, double endLat, double endLon, BinaryMapIndexReader[] rs)
			throws IOException, InterruptedException {
		long ts = System.currentTimeMillis();
		Builder config = RoutingConfiguration.getDefault();
		RoutingMemoryLimits memoryLimits = new RoutingMemoryLimits(MEMORY_TEST_LIMIT, NATIVE_MEMORY_TEST_LIMIT);
		RoutingConfiguration rconfig = config.build(vehicle, memoryLimits);
		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();
		RoutingContext ctx = router.buildRoutingContext(rconfig, lib, rs);
		RouteSegment startSegment = router.findRouteSegment(startLat, startLon, ctx, null);
		RouteSegment endSegment = router.findRouteSegment(endLat, endLon, ctx, null);
		if(startSegment == null){
			throw new IllegalArgumentException("Start segment is not found ");
		}
		if(endSegment == null){
			throw new IllegalArgumentException("End segment is not found ");
		}
		// Clear ctx
		ctx = router.buildRoutingContext(rconfig, lib, rs);
		List<RouteSegmentResult> route = router.searchRoute(ctx,
				new LatLon(startLat, startLon), new LatLon(endLat, endLon), null).detailed;
		System.out.println("Route is " + route.size() + " segments " + (System.currentTimeMillis() - ts) + " ms ");
	}

}
