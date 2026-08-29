package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.routing.details.RouteSegment;
import net.osmand.shared.routing.details.RouteTypeAttribute;

import java.util.ArrayList;
import java.util.List;

/** Eagerly copies one Java/native route segment into the platform-neutral shared contract. */
public final class RouteSegmentResultSnapshotAdapter {

	private RouteSegmentResultSnapshotAdapter() {
	}

	public static RouteSegment toSnapshot(RouteSegmentResult source,
	                                      int routePointStartIndex,
	                                      int routePointEndIndex) {
		RouteDataObject road = source.getObject();
		boolean forward = source.isForwardDirection();
		return new RouteSegment(
				routePointStartIndex,
				routePointEndIndex,
				source.getStartPointIndex(),
				source.getEndPointIndex(),
				source.getDistance(),
				source.getSegmentTime(),
				source.getSegmentSpeed(),
				road.getId(),
				forward,
				road.getName(),
				road.getRef("", false, forward),
				road.getDestinationName("", false, forward),
				road.getDestinationRef("", false, forward),
				road.getHighway(),
				road.getMaximumSpeed(forward),
				road.getLanes(),
				road.getOneway(),
				road.roundabout(),
				road.tunnel(),
				copyRouteTypes(road),
				copyHeightValues(source.getHeightValues())
		);
	}

	/** Copies only the fields consumed by the shared statistics calculator. */
	public static RouteSegment toStatisticsSnapshot(RouteSegmentResult source, int syntheticIndex) {
		RouteDataObject road = source.getObject();
		return new RouteSegment(
				syntheticIndex,
				syntheticIndex,
				source.getStartPointIndex(),
				source.getEndPointIndex(),
				source.getDistance(),
				0,
				0,
				road.getId(),
				source.isForwardDirection(),
				null,
				null,
				null,
				null,
				null,
				0,
				-1,
				0,
				false,
				false,
				copyRouteTypes(road),
				copyHeightValues(source.getHeightValues())
		);
	}

	private static List<RouteTypeAttribute> copyRouteTypes(RouteDataObject road) {
		int[] types = road.getTypes();
		List<RouteTypeAttribute> result = new ArrayList<>(types.length);
		RouteRegion region = road.region;
		int encodingRulesSize = region.quickGetEncodingRulesSize();
		for (int type : types) {
			if (type < 0 || type >= encodingRulesSize) {
				continue;
			}
			RouteTypeRule rule = region.quickGetEncodingRule(type);
			if (rule != null && rule.getTag() != null && rule.getValue() != null) {
				result.add(new RouteTypeAttribute(rule.getTag(), rule.getValue()));
			}
		}
		return result;
	}

	private static float[] copyHeightValues(float[] heightValues) {
		// RouteSegmentResult.getHeightValues() already returns a new array owned by this conversion.
		return heightValues;
	}
}
