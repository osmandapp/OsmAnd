package net.osmand.router.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.BicycleRouter;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.CarRouter;
import net.osmand.router.PedestrianRouter;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

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
		Properties properties = new Properties();
		properties.load(RouterTestsSuite.class.getResourceAsStream("sources.properties"));
		boolean allSuccess = true;
		allSuccess &= test("belarus_test.xml", properties);
		if (allSuccess) {
			System.out.println("All is successfull");
		}

	}
	
	private static BinaryMapIndexReader[] getMapRegions(String regions, Properties properties) throws FileNotFoundException, IOException {
		String[] regionsSplit = regions.split(",");
		BinaryMapIndexReader[] readers = new BinaryMapIndexReader[regionsSplit.length];
		int i = 0;
		for (String reg : regionsSplit) {
			reg = reg.toUpperCase().trim();
			if (!properties.containsKey(reg)) {
				throw new IllegalArgumentException("Region " + reg + " is not found in the source.properties file");
			}
			BinaryMapIndexReader r = new BinaryMapIndexReader(new RandomAccessFile((String) properties.get(reg), "r"), true);
			readers[i++] = r;
		}

		return readers;
	}

	private static boolean test(String file, Properties properties) throws SAXException, IOException, ParserConfigurationException {
		InputStream resource = RouterTestsSuite.class.getResourceAsStream(file);
		Document testSuite = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(resource));
		NodeList tests = testSuite.getElementsByTagName("test");
		
		for(int i=0; i < tests.getLength(); i++){
			Element e = (Element) tests.item(i);
			BinaryMapIndexReader[] regions = getMapRegions(e.getAttribute("regions"), properties);
			
			testRoute(e, regions);
		}
		
		return true;
		
	}

	private static void testRoute(Element testCase, BinaryMapIndexReader[] regions) throws IOException {
		BinaryRoutePlanner planner = new BinaryRoutePlanner(regions);
		RoutingContext ctx = new RoutingContext();
		String vehicle = testCase.getAttribute("vehicle");
		String testDescription = testCase.getAttribute("description");
		if("bicycle".equals(vehicle)){
			ctx.setRouter(new BicycleRouter());
		} else if("pedestrian".equals(vehicle)){
			ctx.setRouter(new PedestrianRouter());
		} else {
			ctx.setRouter(new CarRouter());
		}
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
		
		List<RouteSegmentResult> route = planner.searchRoute(ctx, startSegment, endSegment);
		
		
		NodeList segments = testCase.getElementsByTagName("segment");
		int i = 0;
		while (i < segments.getLength() && i < route.size()) {
			Element segment = (Element) segments.item(i);
			long expectedId = Long.parseLong(segment.getAttribute("id"));
			int expectedStart = Integer.parseInt(segment.getAttribute("start"));
			int expectedEnd = Integer.parseInt(segment.getAttribute("end"));
			RouteSegmentResult segmentResult = route.get(i);
			if (expectedId != segmentResult.object.getId() >> 1) {
				throw new IllegalArgumentException("Test : '" + testDescription + "' on segment " + (i + 1) + " : " + "\n"
						+ "(expected route id) " + expectedId + " != " + (segmentResult.object.getId() >> 1) + " (actual route id)");
			}
			if (expectedStart != segmentResult.startPointIndex) {
				throw new IllegalArgumentException("Test : '" + testDescription + "' on segment " + (i + 1) + " : " + "\n"
						+ "(expected start index) " + expectedStart + " != " + segmentResult.startPointIndex + " (actual start index)");
			}
			if (expectedEnd != segmentResult.endPointIndex) {
				throw new IllegalArgumentException("Test : '" + testDescription + "' on segment " + (i + 1) + " : " + "\n"
						+ "(expected end index) " + expectedEnd + " != " + segmentResult.endPointIndex + " (actual end index)");
			}

			i++;
		}
		
		if(segments.getLength() < route.size()){
			throw new IllegalArgumentException("Expected route is shorter than calculated for test : " + testDescription);
		} else if(segments.getLength() > route.size()){
			throw new IllegalArgumentException("Expected route is more lengthy than calculated for test : " + testDescription);
		}
	}

}
