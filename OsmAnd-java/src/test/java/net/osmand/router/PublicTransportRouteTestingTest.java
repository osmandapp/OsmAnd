package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.osm.edit.Way;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.util.MapUtils;

public class PublicTransportRouteTestingTest {

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		List<String> actualResults = calculateRoute("ferry_gullmarsleden.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 25.78 min: 164.24 m (3.3 min) to walk, 1809.95 m (22.5 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 25.78 min: 164.24 m (3.3 min) to walk, 1809.95 m (22.5 min) to travel\n"
				+ " 1. 161 [7841117]: walk 76.5 m to 'Skår' and travel  to 'Finnsbo färjeläge' by Vägfärja Gullmarsleden 1 stops \n"
				+ " F. Walk 87.7 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test: start and end next to the ferry terminals
	@Test
	public void testPTFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.68001, -1.95001);
		LatLon end = new LatLon(50.68314, -1.94872);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 22.11 min: 29.80 m (0.6 min) to walk, 330.13 m (21.5 min) to travel\n"
				+ " 1. SF [4624542]: walk 19.7 m to 'Sandbanks Ferry' and travel  to 'Sandbanks' by Sandbanks Ferry 1 stops \n"
				+ " F. Walk 10.1 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test: start and end next to the ferry terminals (reverse direction)
	@Test
	public void testPTFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68314, -1.94872);
		LatLon end = new LatLon(50.68001, -1.95001);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 22.11 min: 29.80 m (0.6 min) to walk, 330.13 m (21.5 min) to travel\n"
				+ " 1. SF [4624542]: walk 10.1 m to 'Sandbanks' and travel  to 'Sandbanks Ferry' by Sandbanks Ferry 1 stops \n"
				+ " F. Walk 19.7 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test: short straight walk over the water isn't faster than the ferry
	@Test
	public void testPTFerryRoutingSandbanks2Forward() throws Exception {
		LatLon start = new LatLon(50.679,  -1.95);
		LatLon end = new LatLon(50.683, -1.946);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 28.05 min: 326.76 m (6.5 min) to walk, 330.13 m (21.5 min) to travel\n"
				+ " 1. SF [4624542]: walk 131.9 m to 'Sandbanks Ferry' and travel  to 'Sandbanks' by Sandbanks Ferry 1 stops \n"
				+ " F. Walk 194.9 m to reach your destination");
		expectedResults.add("Route 1 stops, 0 changes, 30.92 min: 252.57 m (5.1 min) to walk, 578.31 m (25.9 min) to travel\n"
				+ " 1. 50 [12668143]: walk 61.2 m to 'Shell Bay Ferry' and travel  to 'Sandbanks Ferry' by Breezer 50: Swanage => Bournemouth 1 stops \n"
				+ " F. Walk 191.4 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test: short straight walk over the water isn't faster than the ferry (reverse direction)
	@Test
	public void testPTFerryRoutingSandbanks2Backward() throws Exception {
		LatLon start = new LatLon(50.683, -1.946);
		LatLon end = new LatLon(50.679, -1.95);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 28.05 min: 326.76 m (6.5 min) to walk, 330.13 m (21.5 min) to travel\n"
				+ " 1. SF [4624542]: walk 194.9 m to 'Sandbanks' and travel  to 'Sandbanks Ferry' by Sandbanks Ferry 1 stops \n"
				+ " F. Walk 131.9 m to reach your destination");
		expectedResults.add("Route 1 stops, 0 changes, 30.47 min: 232.47 m (4.6 min) to walk, 549.44 m (25.8 min) to travel\n"
				+ " 1. 50 [1047294]: walk 176.5 m to 'Sandbanks Ferry' and travel  to 'Shell Bay Ferry' by Breezer 50: Bournemouth => Swanage 1 stops \n"
				+ " F. Walk 55.9 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		List<String> actualResults = calculateRoute("ferry_nordoleden.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 24.29 min: 182.27 m (3.6 min) to walk, 1489.59 m (20.6 min) to travel\n"
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
		expectedResults.add("Route 1 stops, 0 changes, 24.29 min: 182.27 m (3.6 min) to walk, 1489.59 m (20.6 min) to travel\n"
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
		expectedResults.add("Route 4 stops, 0 changes, 19.34 min: 558.84 m (11.2 min) to walk, 2107.82 m (8.2 min) to travel\n"
				+ " 1. 860 [5461816]: walk 11.8 m to 'A' and travel  to 'A' by Buss 860: Trollhättan - Smögen 4 stops \n"
				+ " F. Walk 547.0 m to reach your destination");
		expectedResults.add("Route 1 stops, 0 changes, 20.64 min: 65.76 m (1.3 min) to walk, 1615.25 m (19.3 min) to travel\n"
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
		expectedResults.add("Route 4 stops, 0 changes, 19.29 min: 558.84 m (11.2 min) to walk, 2077.87 m (8.1 min) to travel\n"
				+ " 1. 860 [2325043]: walk 547.0 m to 'A' and travel  to 'A' by Buss 860: Smögen - Trollhättan 4 stops \n"
				+ " F. Walk 11.8 m to reach your destination");
		expectedResults.add("Route 1 stops, 0 changes, 20.64 min: 65.76 m (1.3 min) to walk, 1615.25 m (19.3 min) to travel\n"
				+ " 1. K-S [5924455]: walk 14.9 m to '' and travel  to '' by Kungshamn - Smögen 1 stops \n"
				+ " F. Walk 50.9 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry geometry test: parallel ferry ways of different berths are drawn along the ferry
	@Test
	public void testPTFerryBijelaForward() throws Exception {
		LatLon start = new LatLon(42.466581, 18.674241);
		LatLon end = new LatLon(42.465587, 18.68559);

		String actualResult = calculateGeometry("ferry_bijela.obf", start, end);

		Assert.assertEquals("Way -1: 11 nodes, 971 m", actualResult);
	}

	// Public transport ferry geometry test (reverse direction)
	@Test
	public void testPTFerryBijelaBackward() throws Exception {
		LatLon start = new LatLon(42.465587, 18.68559);
		LatLon end = new LatLon(42.466581, 18.674241);

		String actualResult = calculateGeometry("ferry_bijela.obf", start, end);

		Assert.assertEquals("Way -1: 11 nodes, 971 m", actualResult);
	}

	// geometry of the first segment of the best public transport route: way id, nodes and length
	private String calculateGeometry(String obfFileName, LatLon start, LatLon end) throws Exception {
		String fl = "src/test/resources/routing/" + obfFileName;
		BinaryMapIndexReader[] readers = { new BinaryMapIndexReader(new RandomAccessFile(fl, "r"), new File(fl)) };
		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		TransportRoutingConfiguration cfg = new TransportRoutingConfiguration(builder, builder.getRouter("public_transport"),
				Collections.emptyMap());
		List<TransportRouteResult> results = new TransportRoutePlanner().buildRoute(new TransportRoutingContext(cfg, null, readers), start, end);
		Assert.assertFalse("Routing failed to produce a result", results.isEmpty());
		checkFerryRules(results);

		List<Way> geometry = results.get(0).getSegments().get(0).getGeometry();
		StringBuilder res = new StringBuilder();
		for (Way way : geometry) {
			double length = 0;
			for (int i = 1; i < way.getNodes().size(); i++) {
				length += MapUtils.getDistance(way.getNodes().get(i - 1).getLatLon(), way.getNodes().get(i).getLatLon());
			}
			res.append(String.format(Locale.US, "Way %d: %d nodes, %.0f m", way.getId(), way.getNodes().size(), length));
		}
		return res.toString();
	}

	// a ferry ride is one segment: ferry ways joined at a junction in the water are merged, so a change
	// of segments there (an extra transfer on the map) or two ferry segments in a row is an error
	private void checkFerryRules(List<TransportRouteResult> results) {
		for (TransportRouteResult result : results) {
			TransportRouteResultSegment previous = null;
			for (TransportRouteResultSegment segment : result.getSegments()) {
				Assert.assertFalse("Change of segments at a junction in the water: " + segment.route.getRef(),
						previous != null && TransportFerryHelper.isJunctionStop(previous.route, previous.end));
				Assert.assertFalse("Two ferry segments in a row: " + segment.route.getRef(),
						previous != null && TransportFerryHelper.isFerry(previous.route)
								&& TransportFerryHelper.isFerry(segment.route));
				previous = segment;
			}
		}
	}

	private List<String> calculateRoute(String obfFileName, LatLon start, LatLon end) throws Exception {
		String fl = "src/test/resources/routing/" + obfFileName;
		RandomAccessFile raf = new RandomAccessFile(fl, "r");
		BinaryMapIndexReader[] readers = { new BinaryMapIndexReader(raf, new File(fl)) };

		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		GeneralRouter prouter = builder.getRouter("public_transport");
		TransportRoutingConfiguration cfg = new TransportRoutingConfiguration(builder, prouter, Collections.emptyMap());

		TransportRoutingContext ctx = new TransportRoutingContext(cfg, null, readers);

		TransportRoutePlanner planner = new TransportRoutePlanner();
		List<TransportRouteResult> results = planner.buildRoute(ctx, start, end);
		Assert.assertNotNull("Routing failed to produce a result list", results);
		checkFerryRules(results);

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
