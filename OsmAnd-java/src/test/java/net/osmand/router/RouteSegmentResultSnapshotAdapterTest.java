package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.routing.details.RouteSegment;
import net.osmand.shared.routing.details.RouteTypeAttribute;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

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

}
