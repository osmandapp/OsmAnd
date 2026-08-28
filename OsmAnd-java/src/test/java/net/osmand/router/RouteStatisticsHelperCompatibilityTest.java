package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatisticComputer;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Android compatibility fixtures frozen from the pre-shared RouteStatisticsHelper behavior. */
public class RouteStatisticsHelperCompatibilityTest {

	@Test
	public void preservesRendererFallbackGroupingUndefinedOmissionAndPartitionOrder() throws Exception {
		RenderingRulesStorage currentRenderer = renderer("current", """
				<renderingAttribute name="routeInfo_surface">
					<case additional="surface=asphalt" attrColorValue="#112233" attrStringValue="current_asphalt"/>
				</renderingAttribute>
				""");
		RenderingRulesStorage defaultRenderer = renderer("default", """
				<renderingAttribute name="routeInfo_surface">
					<case additional="surface=gravel" attrColorValue="#445566" attrStringValue="default_gravel"/>
				</renderingAttribute>
				<renderingAttribute name="routeInfo_roadClass">
					<case tag="highway" value="residential" attrColorValue="#770000" attrStringValue="street"/>
					<case tag="highway" value="primary" attrColorValue="#007700" attrStringValue="primary"/>
				</renderingAttribute>
				""");
		RouteRegion region = region();
		List<RouteSegmentResult> route = Arrays.asList(
				segment(region, 30f, new int[]{0, 1, 2}, new float[0]),
				segment(region, 20f, new int[]{1, 3}, new float[0]),
				segment(region, 40f, new int[]{1, 3}, new float[0]));

		List<RouteStatistics> result = RouteStatisticsHelper.calculateRouteStatistic(
				route,
				Arrays.asList("routeInfo_surface", "routeInfo_roadClass", "routeInfo_missing"),
				currentRenderer,
				defaultRenderer,
				new RenderingRuleSearchRequest(currentRenderer),
				new RenderingRuleSearchRequest(defaultRenderer));

		assertEquals(Arrays.asList("surface", "roadClass"),
				Arrays.asList(result.get(0).name, result.get(1).name));
		assertStatistic(
				result.get(0),
				90f,
				Arrays.asList("current_asphalt:30.0", "undefined:60.0"),
				Arrays.asList("current_asphalt:30.0", "undefined:60.0"));
		assertStatistic(
				result.get(1),
				90f,
				Arrays.asList("street:30.0", "primary:60.0"),
				Arrays.asList("primary:60.0", "street:30.0"));
		assertEquals(0xFF112233, result.get(0).elements.get(0).getColor());
		assertEquals(0, result.get(0).elements.get(1).getColor());

		RouteStatisticComputer legacyClassifier = new RouteStatisticComputer(
				currentRenderer,
				defaultRenderer,
				new RenderingRuleSearchRequest(currentRenderer),
				new RenderingRuleSearchRequest(defaultRenderer));
		assertAttributeEquals(
				legacyClassifier.classifySegment("routeInfo_surface", -1, route.get(0).getObject()),
				result.get(0).elements.get(0));
		assertAttributeEquals(
				legacyClassifier.classifySegment("routeInfo_surface", -1, route.get(1).getObject()),
				result.get(0).elements.get(1));
	}

	@Test
	public void preservesLegacyElevationInterpolationAndSlopePartitions() throws Exception {
		RenderingRulesStorage renderer = renderer("slope", """
				<renderingAttribute name="routeInfo_steepness">
					<case additional="steepness=-3_0" attrColorValue="#112233" attrStringValue="-3_0"/>
					<case additional="steepness=17_20" attrColorValue="#445566" attrStringValue="17_20"/>
				</renderingAttribute>
				""");
		RouteSegmentResult segment = segment(
				new RouteRegion(),
				110f,
				new int[0],
				new float[]{0f, 0f, 110f, 22f});

		RouteStatistics result = RouteStatisticsHelper.calculateRouteStatistic(
				Collections.singletonList(segment),
				Collections.singletonList("routeInfo_steepness"),
				renderer,
				renderer,
				new RenderingRuleSearchRequest(renderer),
				new RenderingRuleSearchRequest(renderer)).get(0);

		assertEquals("steepness", result.name);
		assertStatistic(
				result,
				110f,
				Arrays.asList("-4% .. 0%:50.0", "16% .. 20%:10.0", "-4% .. 0%:50.0"),
				Arrays.asList("-4% .. 0%:100.0", "16% .. 20%:10.0"));
	}

	private static RenderingRulesStorage renderer(String name, String attributes) throws Exception {
		String xml = "<renderingStyle name=\"" + name + "\" defaultColor=\"#ffffff\" version=\"1\">"
				+ attributes + "</renderingStyle>";
		RenderingRulesStorage renderer = new RenderingRulesStorage(name, Collections.emptyMap());
		renderer.parseRulesFromXmlInputStream(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
				null,
				false);
		return renderer;
	}

	private static RouteRegion region() {
		RouteRegion region = new RouteRegion();
		region.initRouteEncodingRule(0, "highway", "residential");
		region.initRouteEncodingRule(1, "highway", "primary");
		region.initRouteEncodingRule(2, "surface", "asphalt");
		region.initRouteEncodingRule(3, "surface", "gravel");
		return region;
	}

	private static RouteSegmentResult segment(RouteRegion region,
	                                          float distance,
	                                          int[] types,
	                                          float[] heightValues) {
		RouteDataObject road = new RouteDataObject(region, new int[0], new String[0]);
		road.types = types;
		road.pointsX = new int[]{0, 1};
		road.pointsY = new int[]{0, 1};
		road.heightDistanceArray = heightValues;
		RouteSegmentResult segment = new RouteSegmentResult(road, 0, 1);
		segment.setDistance(distance);
		return segment;
	}

	private static void assertStatistic(RouteStatistics statistic,
	                                    float totalDistance,
	                                    List<String> elements,
	                                    List<String> partition) {
		assertEquals(totalDistance, statistic.totalDistance, 0f);
		assertEquals(elements, attributes(statistic.elements));
		assertEquals(partition, attributes(statistic.partition.values()));
	}

	private static List<String> attributes(Iterable<RouteSegmentAttribute> attributes) {
		java.util.ArrayList<String> result = new java.util.ArrayList<>();
		for (RouteSegmentAttribute attribute : attributes) {
			result.add(attribute.getUserPropertyName() + ":" + attribute.getDistance());
		}
		return result;
	}

	private static void assertAttributeEquals(RouteSegmentAttribute expected, RouteSegmentAttribute actual) {
		assertEquals(expected.getPropertyName(), actual.getPropertyName());
		assertEquals(expected.getColor(), actual.getColor());
	}
}
