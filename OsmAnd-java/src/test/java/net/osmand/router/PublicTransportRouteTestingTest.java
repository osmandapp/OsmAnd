package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;

public class PublicTransportRouteTestingTest {

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		List<String> actualResults = calculateRoute("ferry_gullmarsleden.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 14.02 min: 164.24 m (3.3 min) to walk, 1809.95 m (10.7 min) to travel\n"
				+ " 1. 161 [7841117]: walk 87.7 m to 'Finnsbo färjeläge' and travel  to 'Skår' by Vägfärja Gullmarsleden 1 stops \n"
				+ " F. Walk 76.5 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test (reverse direction)
	@Test
	public void testPTFerryRoutingGullmarsledenBackward() throws Exception {
		LatLon start = new LatLon(58.29961, 11.53579);
		LatLon end = new LatLon(58.30263, 11.50282);

		List<String> actualResults = calculateRoute("ferry_gullmarsleden.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 14.02 min: 164.24 m (3.3 min) to walk, 1809.95 m (10.7 min) to travel\n"
				+ " 1. 161 [7841117]: walk 76.5 m to 'Skår' and travel  to 'Finnsbo färjeläge' by Vägfärja Gullmarsleden 1 stops \n"
				+ " F. Walk 87.7 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 19.22 min: 369.81 m (7.4 min) to walk, 330.13 m (11.8 min) to travel\n"
				+ " 1. SF [4624542]: walk 277.8 m to 'Sandbanks Ferry' and travel  to 'Sandbanks' by Sandbanks Ferry 1 stops \n"
				+ " F. Walk 92.0 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test (reverse direction)
	@Test
	public void testPTFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68383, -1.94831);
		LatLon end = new LatLon(50.67782, -1.95123);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 19.22 min: 369.81 m (7.4 min) to walk, 330.13 m (11.8 min) to travel\n"
				+ " 1. SF [4624542]: walk 92.0 m to 'Sandbanks' and travel  to 'Sandbanks Ferry' by Sandbanks Ferry 1 stops \n"
				+ " F. Walk 277.8 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		List<String> actualResults = calculateRoute("ferry_nordoleden.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 13.10 min: 182.27 m (3.6 min) to walk, 1489.59 m (9.5 min) to travel\n"
				+ " 1. NORD [524836]: walk 48.1 m to 'Hyppeln' and travel  to 'Rörö' by Nordöleden 1 stops \n"
				+ " F. Walk 134.2 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test (reverse direction)
	@Test
	public void testPTFerryRoutingNordoledenBackward() throws Exception {
		LatLon start = new LatLon(57.77241, 11.61966);
		LatLon end = new LatLon(57.75831, 11.61556);

		List<String> actualResults = calculateRoute("ferry_nordoleden.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 13.10 min: 182.27 m (3.6 min) to walk, 1489.59 m (9.5 min) to travel\n"
				+ " 1. NORD [524836]: walk 134.2 m to 'Rörö' and travel  to 'Hyppeln' by Nordöleden 1 stops \n"
				+ " F. Walk 48.1 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingKungshamnForward() throws Exception {
		LatLon start = new LatLon(58.36137, 11.24880);
		LatLon end = new LatLon(58.35333, 11.22505);

		List<String> actualResults = calculateRoute("ferry_kungshamn.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 11.64 min: 65.76 m (1.3 min) to walk, 1615.25 m (10.3 min) to travel\n"
				+ " 1. K-S [5924502]: walk 50.9 m to '' and travel  to '' by Smögen - Kungshamn 1 stops \n"
				+ " F. Walk 14.9 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test (reverse direction)
	@Test
	public void testPTFerryRoutingKungshamnBackward() throws Exception {
		LatLon start = new LatLon(58.35333, 11.22505);
		LatLon end = new LatLon(58.36137, 11.24880);

		List<String> actualResults = calculateRoute("ferry_kungshamn.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 11.64 min: 65.76 m (1.3 min) to walk, 1615.25 m (10.3 min) to travel\n"
				+ " 1. K-S [5924455]: walk 14.9 m to '' and travel  to '' by Kungshamn - Smögen 1 stops \n"
				+ " F. Walk 50.9 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Car ferry routing test
	@Test
	public void testCarFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "car", start, end);

		Assert.assertEquals("Route 2007 m, 7.54 min, ferry ways [4361073]", actualResult);
	}

	// Car ferry routing test (reverse direction)
	@Test
	public void testCarFerryRoutingGullmarsledenBackward() throws Exception {
		LatLon start = new LatLon(58.29961, 11.53579);
		LatLon end = new LatLon(58.30263, 11.50282);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "car", start, end);

		Assert.assertEquals("Route 2446 m, 8.26 min, ferry ways [4361073]", actualResult);
	}

	// Bicycle ferry routing test
	@Test
	public void testBicycleFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 2007 m, 8.13 min, ferry ways [4361073]", actualResult);
	}

	// Bicycle ferry routing test (reverse direction)
	@Test
	public void testBicycleFerryRoutingGullmarsledenBackward() throws Exception {
		LatLon start = new LatLon(58.29961, 11.53579);
		LatLon end = new LatLon(58.30263, 11.50282);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 2446 m, 10.40 min, ferry ways [4361073]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 2007 m, 9.85 min, ferry ways [4361073]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingGullmarsledenBackward() throws Exception {
		LatLon start = new LatLon(58.29961, 11.53579);
		LatLon end = new LatLon(58.30263, 11.50282);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 2007 m, 9.85 min, ferry ways [4361073]", actualResult);
	}

	// Car ferry routing test
	@Test
	public void testCarFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "car", start, end);

		Assert.assertEquals("Route 703 m, 1.81 min, ferry ways [147985346]", actualResult);
	}

	// Car ferry routing test (reverse direction)
	@Test
	public void testCarFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68383, -1.94831);
		LatLon end = new LatLon(50.67782, -1.95123);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "car", start, end);

		Assert.assertEquals("Route 2387 m, 4.08 min, ferry ways [147985346]", actualResult);
	}

	// Bicycle ferry routing test
	@Test
	public void testBicycleFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "bicycle", start, end);

		Assert.assertEquals("Route 703 m, 2.69 min, ferry ways [147985346]", actualResult);
	}

	// Bicycle ferry routing test (reverse direction)
	@Test
	public void testBicycleFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68383, -1.94831);
		LatLon end = new LatLon(50.67782, -1.95123);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "bicycle", start, end);

		Assert.assertEquals("Route 1610 m, 6.11 min, ferry ways [147985346]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 703 m, 6.91 min, ferry ways [147985346]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68383, -1.94831);
		LatLon end = new LatLon(50.67782, -1.95123);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 703 m, 6.91 min, ferry ways [147985346]", actualResult);
	}

	// Car ferry routing test
	@Test
	public void testCarFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "car", start, end);

		Assert.assertEquals("Route 1993 m, 7.60 min, ferry ways [16794766]", actualResult);
	}

	// Car ferry routing test (reverse direction)
	@Test
	public void testCarFerryRoutingNordoledenBackward() throws Exception {
		LatLon start = new LatLon(57.77241, 11.61966);
		LatLon end = new LatLon(57.75831, 11.61556);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "car", start, end);

		Assert.assertEquals("Route 1993 m, 7.60 min, ferry ways [16794766]", actualResult);
	}

	// Bicycle ferry routing test
	@Test
	public void testBicycleFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 1993 m, 7.71 min, ferry ways [16794766]", actualResult);
	}

	// Bicycle ferry routing test (reverse direction)
	@Test
	public void testBicycleFerryRoutingNordoledenBackward() throws Exception {
		LatLon start = new LatLon(57.77241, 11.61966);
		LatLon end = new LatLon(57.75831, 11.61556);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 1993 m, 7.71 min, ferry ways [16794766]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1993 m, 10.01 min, ferry ways [16794766]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingNordoledenBackward() throws Exception {
		LatLon start = new LatLon(57.77241, 11.61966);
		LatLon end = new LatLon(57.75831, 11.61556);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1993 m, 10.01 min, ferry ways [16794766]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingKungshamnForward() throws Exception {
		LatLon start = new LatLon(58.36137, 11.2488);
		LatLon end = new LatLon(58.35333, 11.22505);

		String actualResult = calculateRoute("ferry_kungshamn.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1933 m, 8.80 min, ferry ways [189584079, 189582586]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingKungshamnBackward() throws Exception {
		LatLon start = new LatLon(58.35333, 11.22505);
		LatLon end = new LatLon(58.36137, 11.2488);

		String actualResult = calculateRoute("ferry_kungshamn.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1933 m, 8.80 min, ferry ways [189582586, 189584079]", actualResult);
	}

	private List<String> calculateRoute(String obfFileName, LatLon start, LatLon end) throws Exception {
		String fl = "src/test/resources/routing/" + obfFileName;
		RandomAccessFile raf = new RandomAccessFile(fl, "r");
		BinaryMapIndexReader[] readers = { new BinaryMapIndexReader(raf, new File(fl)) };

		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		GeneralRouter prouter = builder.getRouter("public_transport");
		TransportRoutingConfiguration cfg = new TransportRoutingConfiguration(prouter, Collections.emptyMap());

		TransportRoutingContext ctx = new TransportRoutingContext(cfg, null, readers);

		TransportRoutePlanner planner = new TransportRoutePlanner();
		List<TransportRouteResult> results = planner.buildRoute(ctx, start, end);
		Assert.assertNotNull("Routing failed to produce a result list", results);

		List<String> actualResults = new ArrayList<>();
		for (TransportRouteResult r : results) {
			actualResults.add(r.toString());
		}
		System.out.println("Actual routes found (" + actualResults.size() + "):");
		for (String s : actualResults) {
			System.out.println(s);
		}
		return actualResults;
	}

	// car, bicycle or pedestrian route: distance, time and OSM ids of the ferry ways on the route
	private String calculateRoute(String obfFileName, String profile, LatLon start, LatLon end) throws Exception {
		String fl = "src/test/resources/routing/" + obfFileName;
		BinaryMapIndexReader[] readers = { new BinaryMapIndexReader(new RandomAccessFile(fl, "r"), new File(fl)) };

		RoutingConfiguration config = RoutingConfiguration.getDefault().build(profile,
				new RoutingConfiguration.RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
						RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT));
		RoutePlannerFrontEnd planner = new RoutePlannerFrontEnd();
		RoutingContext ctx = planner.buildRoutingContext(config, null, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		List<RouteSegmentResult> segments = planner.searchRoute(ctx, start, end, null).detailed;
		Assert.assertNotNull("Routing failed to produce a route", segments);

		double distance = 0;
		double time = 0;
		Set<Long> ferryWays = new LinkedHashSet<>();
		for (RouteSegmentResult segment : segments) {
			distance += segment.getDistance();
			time += segment.getSegmentTime();
			if ("ferry".equals(segment.getObject().getValue("route"))) {
				ferryWays.add(segment.getObject().getId() >> ObfConstants.SHIFT_ID);
			}
		}
		String result = String.format(Locale.US, "Route %.0f m, %.2f min, ferry ways %s", distance, time / 60, ferryWays);
		System.out.println(profile + ": " + result);
		return result;
	}

}
