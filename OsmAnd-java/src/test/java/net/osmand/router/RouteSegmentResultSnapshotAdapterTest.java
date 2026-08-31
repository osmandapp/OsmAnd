package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.routing.details.RouteSegment;
import net.osmand.shared.routing.details.RouteTypeAttribute;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteSegmentResultSnapshotAdapterTest {

	@Test
	public void statisticsSnapshotPreservesNullRuleValuesAndSkipsInvalidRuleIds() {
		RouteRegion region = new RouteRegion();
		region.initRouteEncodingRule(0, "seasonal", null);

		RouteDataObject road = new RouteDataObject(region, new int[0], new String[0]);
		road.id = 7L;
		road.types = new int[]{0, 99};
		road.pointsX = new int[]{0, 1};
		road.pointsY = new int[]{0, 1};

		RouteSegmentResult source = new RouteSegmentResult(road, 0, 1);
		source.setDistance(10f);

		RouteSegment snapshot = RouteSegmentResultSnapshotAdapter.toStatisticsSnapshot(source, 0);

		assertEquals(
				Collections.singletonList(new RouteTypeAttribute("seasonal", null)),
				snapshot.getRouteTypes());
	}

	@Test
	public void copiesExactSegmentValuesWithoutRetainingRouteDataObject() {
		RouteRegion region = new RouteRegion();
		region.initRouteEncodingRule(0, "name", null);
		region.initRouteEncodingRule(1, "ref", null);
		region.initRouteEncodingRule(2, "destination", null);
		region.initRouteEncodingRule(3, "destination:ref", null);
		region.initRouteEncodingRule(4, "highway", "residential");
		region.initRouteEncodingRule(5, "surface", "gravel");
		region.initRouteEncodingRule(6, "access", "yes");
		region.initRouteEncodingRule(7, "access", "destination");
		region.initRouteEncodingRule(8, "maxspeed", "50");
		region.initRouteEncodingRule(9, "lanes", "2");
		region.initRouteEncodingRule(10, "oneway", "yes");
		region.initRouteEncodingRule(11, "tunnel", "yes");

		RouteDataObject road = new RouteDataObject(
				region,
				new int[]{0, 1, 2, 3},
				new String[]{"Raw road", "A 1", "Centre", "B 2"}
		);
		road.id = 42L;
		road.types = new int[]{4, 5, 6, 7, 8, 9, 10, 11};
		road.pointsX = new int[]{0, 1};
		road.pointsY = new int[]{0, 1};
		road.heightDistanceArray = new float[]{0f, 12f, 100f, 14f};

		RouteSegmentResult source = new RouteSegmentResult(road, 0, 1);
		source.setDistance(100f);
		source.setSegmentTime(8f);
		source.setSegmentSpeed(12.5f);

		RouteSegment snapshot = RouteSegmentResultSnapshotAdapter.toSnapshot(source, 3, 7);

		assertEquals(3, snapshot.getRoutePointStartIndex());
		assertEquals(7, snapshot.getRoutePointEndIndex());
		assertEquals(0, snapshot.getNativeStartPointIndex());
		assertEquals(1, snapshot.getNativeEndPointIndex());
		assertEquals(100f, snapshot.getDistanceMeters(), 0f);
		assertEquals(8f, snapshot.getSegmentTimeSeconds(), 0f);
		assertEquals(12.5f, snapshot.getSegmentSpeedMetersPerSecond(), 0f);
		assertEquals(42L, snapshot.getRoadId());
		assertTrue(snapshot.getForward());
		assertEquals("Raw road", snapshot.getRoadName());
		assertEquals("A 1", snapshot.getRef());
		assertEquals("Centre", snapshot.getDestinationName());
		assertEquals("B 2", snapshot.getDestinationRef());
		assertEquals("residential", snapshot.getHighway());
		assertEquals(50f / 3.6f, snapshot.getMaximumSpeedMetersPerSecond(), 0.0001f);
		assertEquals(2, snapshot.getLanes());
		assertEquals(1, snapshot.getOneWayDirection());
		assertFalse(snapshot.getRoundabout());
		assertTrue(snapshot.getTunnel());
		assertEquals(
				Arrays.asList(
						new RouteTypeAttribute("highway", "residential"),
						new RouteTypeAttribute("surface", "gravel"),
						new RouteTypeAttribute("access", "yes"),
						new RouteTypeAttribute("access", "destination"),
						new RouteTypeAttribute("maxspeed", "50"),
						new RouteTypeAttribute("lanes", "2"),
						new RouteTypeAttribute("oneway", "yes"),
						new RouteTypeAttribute("tunnel", "yes")
				),
				snapshot.getRouteTypes()
		);
		assertArrayEquals(new float[]{0f, 12f, 100f, 14f}, snapshot.getHeightValues(), 0f);

		road.types = new int[]{4};
		road.heightDistanceArray[1] = 99f;
		source.setDistance(1f);
		assertEquals(100f, snapshot.getDistanceMeters(), 0f);
		assertEquals(8, snapshot.getRouteTypes().size());
		assertArrayEquals(new float[]{0f, 12f, 100f, 14f}, snapshot.getHeightValues(), 0f);
	}
}
