package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;

public class PublicTransportRouteTestingTest {

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		List<String> actualResults = calculateRoute("ferry_gullmarsleden.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 10.40 min: 164.24 m (3.3 min) to walk, 1809.95 m (7.1 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 10.40 min: 164.24 m (3.3 min) to walk, 1809.95 m (7.1 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 11.56 min: 369.81 m (7.4 min) to walk, 330.13 m (4.2 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 11.56 min: 369.81 m (7.4 min) to walk, 330.13 m (4.2 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 10.12 min: 182.27 m (3.6 min) to walk, 1489.59 m (6.5 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 10.12 min: 182.27 m (3.6 min) to walk, 1489.59 m (6.5 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 8.23 min: 65.76 m (1.3 min) to walk, 1615.25 m (6.9 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 8.23 min: 65.76 m (1.3 min) to walk, 1615.25 m (6.9 min) to travel\n"
				+ " 1. K-S [5924455]: walk 14.9 m to '' and travel  to '' by Kungshamn - Smögen 1 stops \n"
				+ " F. Walk 50.9 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
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

}
