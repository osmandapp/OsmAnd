package net.osmand.router.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.swing.DataExtractionSettings;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RouterTestsSuite {
	// TODO add one international test
	// POLAND/BELARUS (problems with way connection) The best at 100%
//	Start lat=52.115756035004786 lon=23.56539487838745
//	END lat=52.03710226357107 lon=23.47106695175171
//	id=32032589 start=8 end=9
//	id=32032656 start=0 end=18
//	id=32031919 start=1 end=0
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		List<File> files = new ArrayList<File>();
		for (File f : new File(DataExtractionSettings.getSettings().getBinaryFilesDir()).listFiles()) {
			if (f.getName().endsWith(".obf")) {
				files.add(f);
			}
		}
		BinaryMapIndexReader[] rs = new BinaryMapIndexReader[files.size()];
		int it = 0;
		for (File f : files) {
			RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$ //$NON-NLS-2$
			rs[it++] = new BinaryMapIndexReader(raf, false);
		}
		
		boolean allSuccess = true;
		for(String a : args) {
			allSuccess &= test(a, rs);	
		}
		if (allSuccess) {
			System.out.println("All is successfull");
		}

	}
	

	private static boolean test(String file, BinaryMapIndexReader[] rs) throws SAXException, IOException, ParserConfigurationException {
		InputStream resource = new FileInputStream(file);
		Document testSuite = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(resource));
		NodeList tests = testSuite.getElementsByTagName("test");
		
		for(int i=0; i < tests.getLength(); i++){
			Element e = (Element) tests.item(i);
			BinaryRoutePlanner router = new BinaryRoutePlanner(null, rs);
			testRoute(e, router);
		}
		
		return true;
	}
	
	private static float parseFloat(Element e, String attr) {
		if(e.getAttribute(attr) == null || e.getAttribute(attr).length() == 0){
			return 0;
		}
		return Float.parseFloat(e.getAttribute(attr));
	}
	private static boolean isIn(float expected, float value, float percent){
		if(Math.abs(value/expected - 1) < percent / 100){
			return true;
		}
		return false;
	}

	private static void testRoute(Element testCase, BinaryRoutePlanner planner) throws IOException {
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
		RoutingContext ctx = new RoutingContext(RoutingConfiguration.getDefault().build(vehicle, true));
		String skip = testCase.getAttribute("skip_comment");
		if (skip != null && skip.length() > 0) {
			System.err.println("\n\n!! Skipped test case '" + testDescription + "' because '" + skip + "'\n\n" );
			return;
		}
		System.out.println("Run test " + testDescription);
		
		double startLat = Double.parseDouble(testCase.getAttribute("start_lat"));
		double startLon = Double.parseDouble(testCase.getAttribute("start_lon"));
		RouteSegment startSegment = planner.findRouteSegment(startLat, startLon, ctx);
		double endLat = Double.parseDouble(testCase.getAttribute("target_lat"));
		double endLon = Double.parseDouble(testCase.getAttribute("target_lon"));
		RouteSegment endSegment = planner.findRouteSegment(endLat, endLon, ctx);
		if(startSegment == null){
			throw new IllegalArgumentException("Start segment is not found for test : " + testDescription);
		}
		if(endSegment == null){
			throw new IllegalArgumentException("End segment is not found for test : " + testDescription);
		}
		List<RouteSegmentResult> route = planner.searchRoute(ctx, startSegment, endSegment, false);
		float completeTime = 0;
		float completeDistance = 0;
		for (int i = 0; i < route.size(); i++) {
			completeTime += route.get(i).getSegmentTime();
			completeDistance += route.get(i).getDistance();
		}
		if(complete_time > 0 && !isIn(complete_time, completeTime, percent)) {
			throw new IllegalArgumentException(String.format("Complete time (expected) %s != %s (original) : %s", complete_time, completeTime, testDescription));
		}
		if(complete_distance > 0 && !isIn(complete_distance, completeDistance, percent)) {
			throw new IllegalArgumentException(String.format("Complete distance (expected) %s != %s (original) : %s", complete_distance, completeDistance, testDescription));
		}
		if(visitedSegments > 0 && !isIn(visitedSegments, ctx.visitedSegments, percent)) {
			throw new IllegalArgumentException(String.format("Visited segments (expected) %s != %s (original) : %s", visitedSegments, ctx.visitedSegments, testDescription));
		}
		if(loadedTiles > 0 && !isIn(loadedTiles, ctx.loadedTiles, percent)) {
			throw new IllegalArgumentException(String.format("Loaded tiles (expected) %s != %s (original) : %s", loadedTiles, ctx.loadedTiles, testDescription));
		}
		
		
//		NodeList segments = compareBySegment(testCase, testDescription, route);
//		if(segments.getLength() < route.size()){
//			throw new IllegalArgumentException("Expected route is shorter than calculated for test : " + testDescription);
//		} else if(segments.getLength() > route.size()){
//			throw new IllegalArgumentException("Expected route is more lengthy than calculated for test : " + testDescription);
//		}
	}


	private static NodeList compareBySegment(Element testCase, String testDescription, List<RouteSegmentResult> route) {
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
