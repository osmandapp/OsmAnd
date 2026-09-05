package net.osmand.router;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.LegacyRouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.LegacyRouteStatisticsHelper.RouteStatistics;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.shared.routing.details.RouteStatistic;
import net.osmand.shared.routing.details.RouteStatisticElement;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteStatisticsHelperCompatibilityTest {

	@Test
	public void steepnessLabelsMatchLegacyAfterLocaleChanges() throws Exception {
		RouteRegion region = new RouteRegion();
		region.initRouteEncodingRule(1, "highway", "primary");
		RouteDataObject road = new RouteDataObject(region);
		road.types = new int[]{1};
		road.pointsX = new int[]{1, 2};
		road.pointsY = new int[]{1, 2};
		road.heightDistanceArray = new float[]{0, 0, 110, 22};
		RouteSegmentResult segment = new RouteSegmentResult(road, 0, 1);
		segment.setDistance(110);
		List<RouteSegmentResult> route = Collections.singletonList(segment);
		List<String> attributes = Collections.singletonList("routeInfo_steepness");
		RenderingRulesStorage renderer = RenderingRulesStorage.initWithStylesFromResources("default.render.xml");

		Locale previousLocale = Locale.getDefault();
		try {
			for (Locale locale : Arrays.asList(Locale.US, Locale.forLanguageTag("ar"))) {
				Locale.setDefault(locale);
				List<RouteStatistics> expected = LegacyRouteStatisticsHelper.calculateRouteStatistic(
						route, attributes, null, renderer, null, new RenderingRuleSearchRequest(renderer));
				List<RouteStatistic> actual = RouteStatisticsHelper.calculateRouteStatistic(
						route, attributes, null, renderer, null, new RenderingRuleSearchRequest(renderer));

				assertEquals("The fixture must produce steepness statistics", 1, expected.size());
				assertEquals(expected.size(), actual.size());
				assertEquals(locale.getLanguage().equals("ar") ? "-٤% .. ٠%" : "-4% .. 0%",
						expected.get(0).elements.get(0).getUserPropertyName());
				assertStatisticEquals(expected.get(0), actual.get(0), 0);
			}
		} finally {
			Locale.setDefault(previousLocale);
		}
	}

	@Test
	public void sharedCalculatorMatchesLegacyOnCalculatedObfRoute() throws Exception {
		File mapFile = new File("src/test/resources/routing/Routing_test_64.obf");
		BinaryMapIndexReader mapReader = new BinaryMapIndexReader(
				new RandomAccessFile(mapFile, "r"),
				mapFile);
		try {
			List<RouteSegmentResult> route = calculateRoute(mapReader);
			assertTrue("The compatibility route must contain multiple segments", route.size() > 1);

			RenderingRulesStorage renderer = RenderingRulesStorage.initWithStylesFromResources("default.render.xml");
			List<String> attributeNames = RouteStatisticsHelper.getRouteStatisticAttrsNames(
					null, renderer, false);
			assertFalse("The default style must expose route statistics", attributeNames.isEmpty());

			List<RouteStatistics> expected = LegacyRouteStatisticsHelper.calculateRouteStatistic(
					route,
					attributeNames,
					null,
					renderer,
					null,
					new RenderingRuleSearchRequest(renderer));
			List<RouteStatistic> actual = RouteStatisticsHelper.calculateRouteStatistic(
					route,
					attributeNames,
					null,
					renderer,
					null,
					new RenderingRuleSearchRequest(renderer));

			assertFalse("The calculated route must produce statistics", expected.isEmpty());
			assertEquals("Statistic count", expected.size(), actual.size());
			for (int statisticIndex = 0; statisticIndex < expected.size(); statisticIndex++) {
				assertStatisticEquals(expected.get(statisticIndex), actual.get(statisticIndex), statisticIndex);
			}
		} finally {
			mapReader.close();
		}
	}

	private static List<RouteSegmentResult> calculateRoute(BinaryMapIndexReader mapReader) throws Exception {
		RoutePlannerFrontEnd frontEnd = new RoutePlannerFrontEnd();
		frontEnd.CALCULATE_MISSING_MAPS = false;
		Map<String, String> parameters = new HashMap<>();
		parameters.put("avoid_footways", "true");
		parameters.put("avoid_unpaved", "true");
		RoutingConfiguration configuration = RoutingConfiguration.getDefault().build(
				"bicycle",
				new RoutingMemoryLimits(
						RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
						RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT),
				parameters);
		configuration.planRoadDirection = 0;
		RoutingContext context = frontEnd.buildRoutingContext(
				configuration,
				null,
				new BinaryMapIndexReader[]{mapReader},
				RouteCalculationMode.NORMAL);
		return frontEnd.searchRoute(
				context,
				new LatLon(45.67710, 35.39404),
				new LatLon(45.67588, 35.39403),
				null).detailed;
	}

	private static void assertStatisticEquals(RouteStatistics expected,
	                                          RouteStatistic actual,
	                                          int statisticIndex) {
		String prefix = "Statistic " + statisticIndex + " (" + expected.name + ") ";
		assertEquals(prefix + "name", expected.name, actual.getName());
		assertEquals(prefix + "total distance", expected.totalDistance, actual.getTotalDistanceMeters(), 0f);
		assertElementsEqual(prefix + "elements", expected.elements, actual.getElements());
		assertElementsEqual(prefix + "partition", new ArrayList<>(expected.partition.values()), actual.getPartition());
	}

	private static void assertElementsEqual(String prefix,
	                                        List<RouteSegmentAttribute> expected,
	                                        List<RouteStatisticElement> actual) {
		assertEquals(prefix + " count", expected.size(), actual.size());
		for (int elementIndex = 0; elementIndex < expected.size(); elementIndex++) {
			RouteSegmentAttribute expectedElement = expected.get(elementIndex);
			RouteStatisticElement actualElement = actual.get(elementIndex);
			String elementPrefix = prefix + "[" + elementIndex + "] ";
			assertEquals(elementPrefix + "property", expectedElement.getPropertyName(), actualElement.getPropertyName());
			assertEquals(elementPrefix + "user property", expectedElement.getUserPropertyName(),
					actualElement.getUserPropertyName());
			assertEquals(elementPrefix + "color", expectedElement.getColor(), actualElement.getColor());
			assertEquals(elementPrefix + "distance", expectedElement.getDistance(),
					actualElement.getDistanceMeters(), 0f);
		}
	}
}
