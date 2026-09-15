package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RouteSegmentResultStatisticsAccessorTest {

	@Test
	public void preservesNullRuleValuesAndSkipsInvalidRuleIds() {
		RouteRegion region = new RouteRegion();
		region.initRouteEncodingRule(0, "seasonal", null);

		RouteDataObject road = new RouteDataObject(region, new int[0], new String[0]);
		road.id = 7L;
		road.types = new int[]{0, 99};
		road.pointsX = new int[]{0, 1};
		road.pointsY = new int[]{0, 1};

		RouteSegmentResult source = new RouteSegmentResult(road, 0, 1);
		source.setDistance(10f);
		RouteSegmentResultStatisticsAccessor accessor =
				new RouteSegmentResultStatisticsAccessor(Collections.singletonList(source));

		assertEquals(1, accessor.getSegmentsCount());
		assertEquals(2, accessor.getRouteTypesCount(0));
		assertEquals("seasonal", accessor.getRouteTypeTag(0, 0));
		assertNull(accessor.getRouteTypeValue(0, 0));
		assertNull(accessor.getRouteTypeTag(0, 1));
	}
}
