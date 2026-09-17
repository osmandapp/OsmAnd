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
import net.osmand.osm.edit.Way;
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

	// Public transport ferry routing test
	@Test
	public void testPTFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 28.33 min: 122.88 m (2.5 min) to walk, 578.31 m (25.9 min) to travel\n"
				+ " 1. 50 [12668143]: walk 103.3 m to 'Shell Bay Ferry' and travel  to 'Sandbanks Ferry' by Breezer 50: Swanage => Bournemouth 1 stops \n"
				+ " F. Walk 19.6 m to reach your destination");
		expectedResults.add("Route 1 stops, 0 changes, 28.91 min: 369.81 m (7.4 min) to walk, 330.13 m (21.5 min) to travel\n"
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
		// the first route walks over the water: the app drops it after calculating real walks
		expectedResults.add("Route 1 stops, 0 changes, 26.53 min: 1120.10 m (22.4 min) to walk, 419.36 m (4.1 min) to travel\n"
				+ " 1. 50 [12668143]: walk 19.6 m to 'Sandbanks Ferry' and travel  to 'Royal Motor Yacht Club' by Breezer 50: Swanage => Bournemouth 1 stops \n"
				+ " F. Walk 1100.5 m to reach your destination");
		expectedResults.add("Route 1 stops, 0 changes, 28.86 min: 151.68 m (3.0 min) to walk, 549.44 m (25.8 min) to travel\n"
				+ " 1. 50 [1047294]: walk 47.6 m to 'Sandbanks Ferry' and travel  to 'Shell Bay Ferry' by Breezer 50: Bournemouth => Swanage 1 stops \n"
				+ " F. Walk 104.0 m to reach your destination");
		expectedResults.add("Route 1 stops, 0 changes, 28.91 min: 369.81 m (7.4 min) to walk, 330.13 m (21.5 min) to travel\n"
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

	// Car ferry routing test
	@Test
	public void testCarFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "car", start, end);

		Assert.assertEquals("Route 2007 m, 22.67 min, ferry ways [4361073]", actualResult);
	}

	// Car ferry routing test (reverse direction)
	@Test
	public void testCarFerryRoutingGullmarsledenBackward() throws Exception {
		LatLon start = new LatLon(58.29961, 11.53579);
		LatLon end = new LatLon(58.30263, 11.50282);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "car", start, end);

		Assert.assertEquals("Route 2446 m, 23.40 min, ferry ways [4361073]", actualResult);
	}

	// Bicycle ferry routing test
	@Test
	public void testBicycleFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 2007 m, 23.26 min, ferry ways [4361073]", actualResult);
	}

	// Bicycle ferry routing test (reverse direction)
	@Test
	public void testBicycleFerryRoutingGullmarsledenBackward() throws Exception {
		LatLon start = new LatLon(58.29961, 11.53579);
		LatLon end = new LatLon(58.30263, 11.50282);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 2446 m, 25.54 min, ferry ways [4361073]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingGullmarsledenForward() throws Exception {
		LatLon start = new LatLon(58.30263, 11.50282);
		LatLon end = new LatLon(58.29961, 11.53579);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 2007 m, 24.99 min, ferry ways [4361073]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingGullmarsledenBackward() throws Exception {
		LatLon start = new LatLon(58.29961, 11.53579);
		LatLon end = new LatLon(58.30263, 11.50282);

		String actualResult = calculateRoute("ferry_gullmarsleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 2007 m, 24.99 min, ferry ways [4361073]", actualResult);
	}

	// Car ferry routing test
	@Test
	public void testCarFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "car", start, end);

		Assert.assertEquals("Route 703 m, 21.99 min, ferry ways [147985346]", actualResult);
	}

	// Car ferry routing test (reverse direction)
	@Test
	public void testCarFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68383, -1.94831);
		LatLon end = new LatLon(50.67782, -1.95123);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "car", start, end);

		Assert.assertEquals("Route 2387 m, 24.26 min, ferry ways [147985346]", actualResult);
	}

	// Bicycle ferry routing test
	@Test
	public void testBicycleFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "bicycle", start, end);

		Assert.assertEquals("Route 703 m, 22.87 min, ferry ways [147985346]", actualResult);
	}

	// Bicycle ferry routing test (reverse direction)
	@Test
	public void testBicycleFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68383, -1.94831);
		LatLon end = new LatLon(50.67782, -1.95123);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "bicycle", start, end);

		Assert.assertEquals("Route 1610 m, 26.29 min, ferry ways [147985346]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingSandbanksForward() throws Exception {
		LatLon start = new LatLon(50.67782, -1.95123);
		LatLon end = new LatLon(50.68383, -1.94831);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 703 m, 27.10 min, ferry ways [147985346]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingSandbanksBackward() throws Exception {
		LatLon start = new LatLon(50.68383, -1.94831);
		LatLon end = new LatLon(50.67782, -1.95123);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 703 m, 27.10 min, ferry ways [147985346]", actualResult);
	}

	// Car ferry routing test
	@Test
	public void testCarFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "car", start, end);

		Assert.assertEquals("Route 1993 m, 21.19 min, ferry ways [16794766]", actualResult);
	}

	// Car ferry routing test (reverse direction)
	@Test
	public void testCarFerryRoutingNordoledenBackward() throws Exception {
		LatLon start = new LatLon(57.77241, 11.61966);
		LatLon end = new LatLon(57.75831, 11.61556);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "car", start, end);

		Assert.assertEquals("Route 1993 m, 21.19 min, ferry ways [16794766]", actualResult);
	}

	// Bicycle ferry routing test
	@Test
	public void testBicycleFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 1993 m, 21.30 min, ferry ways [16794766]", actualResult);
	}

	// Bicycle ferry routing test (reverse direction)
	@Test
	public void testBicycleFerryRoutingNordoledenBackward() throws Exception {
		LatLon start = new LatLon(57.77241, 11.61966);
		LatLon end = new LatLon(57.75831, 11.61556);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "bicycle", start, end);

		Assert.assertEquals("Route 1993 m, 21.30 min, ferry ways [16794766]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingNordoledenForward() throws Exception {
		LatLon start = new LatLon(57.75831, 11.61556);
		LatLon end = new LatLon(57.77241, 11.61966);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1993 m, 23.60 min, ferry ways [16794766]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingNordoledenBackward() throws Exception {
		LatLon start = new LatLon(57.77241, 11.61966);
		LatLon end = new LatLon(57.75831, 11.61556);

		String actualResult = calculateRoute("ferry_nordoleden.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1993 m, 23.60 min, ferry ways [16794766]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingKungshamnForward() throws Exception {
		LatLon start = new LatLon(58.36137, 11.2488);
		LatLon end = new LatLon(58.35333, 11.22505);

		String actualResult = calculateRoute("ferry_kungshamn.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1933 m, 21.30 min, ferry ways [189584079, 189582586]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingKungshamnBackward() throws Exception {
		LatLon start = new LatLon(58.35333, 11.22505);
		LatLon end = new LatLon(58.36137, 11.2488);

		String actualResult = calculateRoute("ferry_kungshamn.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1933 m, 21.30 min, ferry ways [189582586, 189584079]", actualResult);
	}

	// Public transport ferry routing test: start and end next to the ferry terminals
	@Test
	public void testPTFerryRoutingSandbanks2Forward() throws Exception {
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
	public void testPTFerryRoutingSandbanks2Backward() throws Exception {
		LatLon start = new LatLon(50.68314, -1.94872);
		LatLon end = new LatLon(50.68001, -1.95001);

		List<String> actualResults = calculateRoute("ferry_sandbanks.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 1 stops, 0 changes, 22.11 min: 29.80 m (0.6 min) to walk, 330.13 m (21.5 min) to travel\n"
				+ " 1. SF [4624542]: walk 10.1 m to 'Sandbanks' and travel  to 'Sandbanks Ferry' by Sandbanks Ferry 1 stops \n"
				+ " F. Walk 19.7 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Car ferry routing test: start and end next to the ferry terminals
	@Test
	public void testCarFerryRoutingSandbanks2Forward() throws Exception {
		LatLon start = new LatLon(50.68001, -1.95001);
		LatLon end = new LatLon(50.68314, -1.94872);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "car", start, end);

		Assert.assertEquals("Route 360 m, 21.54 min, ferry ways [147985346]", actualResult);
	}

	// Car ferry routing test: start and end next to the ferry terminals (reverse direction)
	@Test
	public void testCarFerryRoutingSandbanks2Backward() throws Exception {
		LatLon start = new LatLon(50.68314, -1.94872);
		LatLon end = new LatLon(50.68001, -1.95001);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "car", start, end);

		Assert.assertEquals("Route 360 m, 21.54 min, ferry ways [147985346]", actualResult);
	}

	// Bicycle ferry routing test: start and end next to the ferry terminals
	@Test
	public void testBicycleFerryRoutingSandbanks2Forward() throws Exception {
		LatLon start = new LatLon(50.68001, -1.95001);
		LatLon end = new LatLon(50.68314, -1.94872);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "bicycle", start, end);

		Assert.assertEquals("Route 360 m, 21.62 min, ferry ways [147985346]", actualResult);
	}

	// Bicycle ferry routing test: start and end next to the ferry terminals (reverse direction)
	@Test
	public void testBicycleFerryRoutingSandbanks2Backward() throws Exception {
		LatLon start = new LatLon(50.68314, -1.94872);
		LatLon end = new LatLon(50.68001, -1.95001);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "bicycle", start, end);

		Assert.assertEquals("Route 360 m, 21.62 min, ferry ways [147985346]", actualResult);
	}

	// Pedestrian ferry routing test: start and end next to the ferry terminals
	@Test
	public void testPedestrianFerryRoutingSandbanks2Forward() throws Exception {
		LatLon start = new LatLon(50.68001, -1.95001);
		LatLon end = new LatLon(50.68314, -1.94872);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 360 m, 21.95 min, ferry ways [147985346]", actualResult);
	}

	// Pedestrian ferry routing test: start and end next to the ferry terminals (reverse direction)
	@Test
	public void testPedestrianFerryRoutingSandbanks2Backward() throws Exception {
		LatLon start = new LatLon(50.68314, -1.94872);
		LatLon end = new LatLon(50.68001, -1.95001);

		String actualResult = calculateRoute("ferry_sandbanks.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 360 m, 21.95 min, ferry ways [147985346]", actualResult);
	}

	// Public transport ferry routing test: short straight walk over the water isn't faster than the ferry
	@Test
	public void testPTFerryRoutingSandbanks3Forward() throws Exception {
		LatLon start = new LatLon(50.679, -1.95);
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
	public void testPTFerryRoutingSandbanks3Backward() throws Exception {
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
	public void testPTFerryRoutingBijelaForward() throws Exception {
		LatLon start = new LatLon(42.466581, 18.674241);
		LatLon end = new LatLon(42.465587, 18.68559);

		List<String> actualResults = calculateRoute("ferry_bijela.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 2 stops, 0 changes, 16.92 min: 30.06 m (0.6 min) to walk, 953.67 m (16.3 min) to travel\n"
				+ " 1. TK-L [9492733]: walk 11.0 m to 'Kamenari' and travel  to 'Lepetane' by Trajekt Kamenari - Lepetane 2 stops \n"
				+ " F. Walk 19.1 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Public transport ferry routing test (reverse direction)
	@Test
	public void testPTFerryRoutingBijelaBackward() throws Exception {
		LatLon start = new LatLon(42.465587, 18.68559);
		LatLon end = new LatLon(42.466581, 18.674241);

		List<String> actualResults = calculateRoute("ferry_bijela.obf", start, end);

		List<String> expectedResults = new ArrayList<>();
		expectedResults.add("Route 2 stops, 0 changes, 16.92 min: 30.06 m (0.6 min) to walk, 953.67 m (16.3 min) to travel\n"
				+ " 1. TK-L [9492733]: walk 19.1 m to 'Lepetane' and travel  to 'Kamenari' by Lepetane - Trajekt Kamenari 2 stops \n"
				+ " F. Walk 11.0 m to reach your destination");

		Assert.assertEquals(expectedResults, actualResults);
	}

	// Car ferry routing test
	@Test
	public void testCarFerryRoutingBijelaForward() throws Exception {
		LatLon start = new LatLon(42.466581, 18.674241);
		LatLon end = new LatLon(42.465587, 18.68559);

		String actualResult = calculateRoute("ferry_bijela.obf", "car", start, end);

		Assert.assertEquals("Route 1001 m, 17.56 min, ferry ways [145625735]", actualResult);
	}

	// Car ferry routing test (reverse direction)
	@Test
	public void testCarFerryRoutingBijelaBackward() throws Exception {
		LatLon start = new LatLon(42.465587, 18.68559);
		LatLon end = new LatLon(42.466581, 18.674241);

		String actualResult = calculateRoute("ferry_bijela.obf", "car", start, end);

		Assert.assertEquals("Route 1001 m, 17.56 min, ferry ways [145625735]", actualResult);
	}

	// Pedestrian ferry routing test
	@Test
	public void testPedestrianFerryRoutingBijelaForward() throws Exception {
		LatLon start = new LatLon(42.466581, 18.674241);
		LatLon end = new LatLon(42.465587, 18.68559);

		String actualResult = calculateRoute("ferry_bijela.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1001 m, 17.94 min, ferry ways [145625735]", actualResult);
	}

	// Pedestrian ferry routing test (reverse direction)
	@Test
	public void testPedestrianFerryRoutingBijelaBackward() throws Exception {
		LatLon start = new LatLon(42.465587, 18.68559);
		LatLon end = new LatLon(42.466581, 18.674241);

		String actualResult = calculateRoute("ferry_bijela.obf", "pedestrian", start, end);

		Assert.assertEquals("Route 1001 m, 17.94 min, ferry ways [145625735]", actualResult);
	}

	// Public transport ferry geometry test: parallel ferry ways of different berths are drawn along the ferry
	@Test
	public void testPTFerryGeometryBijelaForward() throws Exception {
		LatLon start = new LatLon(42.466581, 18.674241);
		LatLon end = new LatLon(42.465587, 18.68559);

		String actualResult = calculateGeometry("ferry_bijela.obf", start, end);

		Assert.assertEquals("Way -1: 11 nodes, 971 m", actualResult);
	}

	// Public transport ferry geometry test (reverse direction)
	@Test
	public void testPTFerryGeometryBijelaBackward() throws Exception {
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
