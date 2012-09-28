package net.osmand.router;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingContext;
import net.osmand.swing.DataExtractionSettings;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RouterTestsSuite {
	
	private static class Parameters {
		public File obfDir;
		public List<File> tests = new ArrayList<File>();
		public RoutingConfiguration.Builder configBuilder;
		
		public static Parameters init(String[] args) throws SAXException, IOException {
			Parameters p = new Parameters();
			String routingXmlFile = null;
			String obfDirectory = null;
			BinaryRoutePlanner.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
			for (String a : args) {
				if (a.startsWith("-routingXmlPath=")) {
					routingXmlFile = a.substring("-routingXmlPath=".length());
				} else if (a.startsWith("-verbose")) {
					BinaryRoutePlanner.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
				} else if (a.startsWith("-obfDir=")) {
					obfDirectory = a.substring("-obfDir=".length());
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
				obfDirectory = DataExtractionSettings.getSettings().getBinaryFilesDir();
			}
			if (obfDirectory != null && obfDirectory.length() > 0) {
				p.obfDir = new File(obfDirectory);
			}

			if (routingXmlFile == null) {
				routingXmlFile = DataExtractionSettings.getSettings().getRoutingXmlPath();
			}
			if (routingXmlFile.equals("routing.xml")) {
				p.configBuilder = RoutingConfiguration.getDefault();
			} else {
				p.configBuilder = RoutingConfiguration.parseFromInputStream(new FileInputStream(routingXmlFile));
			}

			return p;
		}
	}
	
	public static void main(String[] args) throws Exception {
		Parameters params = Parameters.init(args);
		if(params.tests.isEmpty() || params.obfDir == null) {
			println("Run router tests is console utility to test route calculation for osmand.");
			println("\nUsage for run tests : runTestsSuite [-routingXmlPath=PATH] [-verbose]  [-obfDir=PATH] [-testDir=PATH] {individualTestPath}");
			return;
		}
		List<File> files = new ArrayList<File>();
		
		
		for (File f : params.obfDir.listFiles()) {
			if (f.getName().endsWith(".obf")) {
				files.add(f);
			}
		}
		BinaryMapIndexReader[] rs = new BinaryMapIndexReader[files.size()];
		int it = 0;
		for (File f : files) {
			RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$ //$NON-NLS-2$
			rs[it++] = new BinaryMapIndexReader(raf);
		}
		
		boolean allSuccess = true;
		
		for(File f : params.tests) {
			allSuccess &= test(null, new FileInputStream(f), rs, params.configBuilder);	
		}
		if (allSuccess) {
			System.out.println("All is successfull");
		}

	}
	

	private static void println(String string) {
		System.out.println(string);
	}


	public static boolean test(NativeLibrary lib, InputStream resource, BinaryMapIndexReader[] rs, RoutingConfiguration.Builder config) throws Exception {
		Document testSuite = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(resource));
		NodeList tests = testSuite.getElementsByTagName("test");

		for (int i = 0; i < tests.getLength(); i++) {
			Element e = (Element) tests.item(i);
			testRoute(e, config, lib, rs);
		}

		return true;
	}
	
	private static float parseFloat(Element e, String attr) {
		if(e.getAttribute(attr) == null || e.getAttribute(attr).length() == 0){
			return 0;
		}
		return Float.parseFloat(e.getAttribute(attr));
	}
	private static boolean isInOrLess(float expected, float value, float percent){
		if(Math.abs(value/expected - 1) < percent / 100){
			return true;
		}
		if(value < expected) {
			System.err.println("Test could be adjusted value "  + value + " is much less then expected " + expected);
			return true;
		}
		return false;
	}

	private static void testRoute(Element testCase, Builder config, NativeLibrary lib, BinaryMapIndexReader[] rs) throws IOException, SAXException, InterruptedException {
		String vehicle = testCase.getAttribute("vehicle");
		int loadedTiles = (int) parseFloat(testCase, "loadedTiles");
		int visitedSegments = (int) parseFloat(testCase, "visitedSegments");
		int complete_time = (int) parseFloat(testCase, "complete_time");
		int complete_distance = (int) parseFloat(testCase, "complete_distance");
		float percent = parseFloat(testCase, "best_percent");
		String testDescription = testCase.getAttribute("description");
		if(percent == 0){
			System.err.println("\n\n!! Skipped test case '" + testDescription + "' because 'best_percent' attribute is not specified \n\n" );
			return;
		}
		BinaryRoutePlanner router = new BinaryRoutePlanner();
		RoutingContext ctx = new RoutingContext(config.build(vehicle, RoutingConfiguration.DEFAULT_MEMORY_LIMIT), 
				lib, rs);
		String skip = testCase.getAttribute("skip_comment");
		if (skip != null && skip.length() > 0) {
			System.err.println("\n\n!! Skipped test case '" + testDescription + "' because '" + skip + "'\n\n" );
			return;
		}
		System.out.println("Run test " + testDescription);
		
		double startLat = Double.parseDouble(testCase.getAttribute("start_lat"));
		double startLon = Double.parseDouble(testCase.getAttribute("start_lon"));
		RouteSegment startSegment = router.findRouteSegment(startLat, startLon, ctx);
		double endLat = Double.parseDouble(testCase.getAttribute("target_lat"));
		double endLon = Double.parseDouble(testCase.getAttribute("target_lon"));
		RouteSegment endSegment = router.findRouteSegment(endLat, endLon, ctx);
		if(startSegment == null){
			throw new IllegalArgumentException("Start segment is not found for test : " + testDescription);
		}
		if(endSegment == null){
			throw new IllegalArgumentException("End segment is not found for test : " + testDescription);
		}
		List<RouteSegmentResult> route = router.searchRoute(ctx, startSegment, endSegment, false);
		float completeTime = 0;
		float completeDistance = 0;
		for (int i = 0; i < route.size(); i++) {
			completeTime += route.get(i).getSegmentTime();
			completeDistance += route.get(i).getDistance();
		}
		if(complete_time > 0 && !isInOrLess(complete_time, completeTime, percent)) {
			throw new IllegalArgumentException(String.format("Complete time (expected) %s != %s (original) : %s", complete_time, completeTime, testDescription));
		}
		if(complete_distance > 0 && !isInOrLess(complete_distance, completeDistance, percent)) {
			throw new IllegalArgumentException(String.format("Complete distance (expected) %s != %s (original) : %s", complete_distance, completeDistance, testDescription));
		}
		if(visitedSegments > 0 && !isInOrLess(visitedSegments, ctx.visitedSegments, percent)) {
			throw new IllegalArgumentException(String.format("Visited segments (expected) %s != %s (original) : %s", visitedSegments, ctx.visitedSegments, testDescription));
		}
		if(loadedTiles > 0 && !isInOrLess(loadedTiles, ctx.loadedTiles, percent)) {
			throw new IllegalArgumentException(String.format("Loaded tiles (expected) %s != %s (original) : %s", loadedTiles, ctx.loadedTiles, testDescription));
		}
		
		
//		NodeList segments = compareBySegment(testCase, testDescription, route);
//		if(segments.getLength() < route.size()){
//			throw new IllegalArgumentException("Expected route is shorter than calculated for test : " + testDescription);
//		} else if(segments.getLength() > route.size()){
//			throw new IllegalArgumentException("Expected route is more lengthy than calculated for test : " + testDescription);
//		}
	}


	protected static NodeList compareBySegment(Element testCase, String testDescription, List<RouteSegmentResult> route) {
		NodeList segments = testCase.getElementsByTagName("segment");
		int i = 0;
		while (i < segments.getLength() && i < route.size()) {
			Element segment = (Element) segments.item(i);
			long expectedId = Long.parseLong(segment.getAttribute("id"));
			int expectedStart = Integer.parseInt(segment.getAttribute("start"));
			int expectedEnd = Integer.parseInt(segment.getAttribute("end"));
			RouteSegmentResult segmentResult = route.get(i);
			if (expectedId != segmentResult.getObject().getId() >> 1) {
				throw new IllegalArgumentException("Test : '" + testDescription + "' on segment " + (i + 1) + " : " + "\n"
						+ "(expected route id) " + expectedId + " != " + (segmentResult.getObject().getId() >> 1) + " (actual route id)");
			}
			if (expectedStart != segmentResult.getStartPointIndex()) {
				throw new IllegalArgumentException("Test : '" + testDescription + "' on segment " + (i + 1) + " : " + "\n"
						+ "(expected start index) " + expectedStart + " != " + segmentResult.getStartPointIndex() + " (actual start index)");
			}
			if (expectedEnd != segmentResult.getEndPointIndex()) {
				throw new IllegalArgumentException("Test : '" + testDescription + "' on segment " + (i + 1) + " : " + "\n"
						+ "(expected end index) " + expectedEnd + " != " + segmentResult.getEndPointIndex() + " (actual end index)");
			}

			i++;
		}
		return segments;
	}

}
