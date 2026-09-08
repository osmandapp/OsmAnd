package net.osmand.router;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.osmand.shared.data.KLocation;
import net.osmand.shared.routing.RouteDataBundle;
import net.osmand.shared.routing.RouteDataObject;
import net.osmand.shared.routing.RouteDataResources;
import net.osmand.shared.routing.RouteRegion;
import net.osmand.shared.routing.RouteTypeRule;
import net.osmand.shared.routing.TurnType;
import net.osmand.util.MapUtils;

/**
 * Route gpx files are written and read through {@link RouteSegmentResult#writeToBundle} and
 * {@link RouteSegmentResult#readFromBundle}, and nothing else covers that pair. This drives a
 * segment through both and checks it comes back the same, including the text of the numbers, which
 * is what ends up in the file.
 */
public class RouteBundleRoundTripTest {

	private static final double[][] POINTS = {
			{50.0, 30.0}, {50.001, 30.001}, {50.002, 30.003}, {50.0025, 30.005}
	};

	private RouteRegion sourceRegion() {
		RouteRegion region = new RouteRegion();
		region.initRouteEncodingRule(0, "", "");
		region.initRouteEncodingRule(1, "highway", "primary");
		region.initRouteEncodingRule(2, "oneway", "yes");
		region.initRouteEncodingRule(3, "name", null);
		region.initRouteEncodingRule(4, "ref", null);
		return region;
	}

	private RouteDataObject road(RouteRegion region) {
		RouteDataObject road = new RouteDataObject(region, new int[] {3, 4},
				new String[] {"Main street", "A1"});
		road.id = 12345 << 6;
		road.types = new int[] {1, 2};
		road.pointsX = new int[POINTS.length];
		road.pointsY = new int[POINTS.length];
		for (int i = 0; i < POINTS.length; i++) {
			road.pointsX[i] = MapUtils.get31TileNumberX(POINTS[i][1]);
			road.pointsY[i] = MapUtils.get31TileNumberY(POINTS[i][0]);
		}
		return road;
	}

	private List<KLocation> locations() {
		List<KLocation> locations = new ArrayList<>();
		for (double[] point : POINTS) {
			KLocation location = new KLocation("test", point[0], point[1]);
			location.setAltitude(100 + point[0]);
			locations.add(location);
		}
		return locations;
	}

	@Test
	public void testSegmentSurvivesTheRoundTrip() {
		RouteRegion region = sourceRegion();
		RouteDataObject road = road(region);

		RouteSegmentResult written = new RouteSegmentResult(road, 0, POINTS.length - 1);
		written.setSegmentTime(63.456f);
		written.setSegmentSpeed(13.888889f);
		written.setTurnType(TurnType.valueOf(TurnType.TL, false));

		RouteDataResources writeResources = new RouteDataResources(locations(), new ArrayList<>());
		written.collectTypes(writeResources);
		written.collectNames(writeResources);
		RouteDataBundle bundle = new RouteDataBundle(writeResources);
		written.writeToBundle(bundle);

		// the numbers are written the way DecimalFormat used to write them
		assertEquals("4", bundle.getString("length", null));
		assertEquals("63.46", bundle.getString("segmentTime", null));
		assertEquals("13.89", bundle.getString("speed", null));
		assertEquals("12345", bundle.getString("id", null));
		assertNotNull(bundle.getString("turnType", null));

		// read it back the way RouteImporter does, through a region rebuilt from the collected rules
		RouteRegion target = new RouteRegion();
		int ruleId = 0;
		for (RouteTypeRule rule : writeResources.getRules().keySet()) {
			target.initRouteEncodingRule(ruleId++, rule.getTag(), rule.getValue());
		}
		RouteDataResources readResources = new RouteDataResources(locations(), new ArrayList<>());
		RouteSegmentResult read = new RouteSegmentResult(new RouteDataObject(target), false);
		read.readFromBundle(new RouteDataBundle(readResources, bundle));
		read.fillNames(readResources);

		assertEquals(0, read.getStartPointIndex());
		assertEquals(POINTS.length - 1, read.getEndPointIndex());
		assertEquals(63.46f, read.getSegmentTime(), 1e-4f);
		assertEquals(13.89f, read.getSegmentSpeed(), 1e-4f);
		assertEquals(TurnType.TL, read.getTurnType().getValue());
		assertEquals(12345 << 6, read.getObject().id);

		RouteDataObject readRoad = read.getObject();
		assertEquals(POINTS.length, readRoad.getPointsLength());
		assertArrayEquals(road.pointsX, readRoad.pointsX);
		assertArrayEquals(road.pointsY, readRoad.pointsY);
		assertEquals("primary", readRoad.getHighway());
		assertEquals(1, readRoad.getOneway());
		assertEquals("Main street", readRoad.getName());
		assertEquals("A1", readRoad.getRef("", false, true));
	}

	@Test
	public void testRulesAreCollectedOnce() {
		RouteRegion region = sourceRegion();

		RouteDataResources once = new RouteDataResources();
		collect(once, region, 1);
		Map<RouteTypeRule, Integer> singleSegment = once.getRules();
		// highway and oneway, plus a bare and a valued rule for each of name and ref
		assertEquals(6, singleSegment.size());

		RouteDataResources thrice = new RouteDataResources();
		collect(thrice, region, 3);
		// segments that repeat a tag reuse its rule rather than adding another
		assertEquals(singleSegment.size(), thrice.getRules().size());
	}

	private void collect(RouteDataResources resources, RouteRegion region, int segments) {
		for (int i = 0; i < segments; i++) {
			RouteSegmentResult segment = new RouteSegmentResult(road(region), 0, POINTS.length - 1);
			segment.collectTypes(resources);
			segment.collectNames(resources);
		}
	}

	@Test
	public void testSegmentStartIndexAdvancesAcrossSegments() {
		RouteDataResources resources = new RouteDataResources(locations(), new ArrayList<>());
		assertEquals(0, resources.getCurrentSegmentStartLocationIndex());
		resources.updateNextSegmentStartLocation(3);
		// consecutive segments share their meeting point
		assertEquals(2, resources.getCurrentSegmentStartLocationIndex());
		assertEquals(POINTS[2][0], resources.getCurrentSegmentLocation(0).getLatitude(), 1e-9);
	}
}
