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

	private static List<RouteTypeAttribute> copyRouteTypes(RouteDataObject road) {
		List<RouteTypeAttribute> result = new ArrayList<>();
		RouteRegion region = road.region;
		int encodingRulesSize = region.quickGetEncodingRulesSize();
		for (int type : road.getTypes()) {
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

	private static List<Float> copyHeightValues(float[] heightValues) {
		List<Float> result = new ArrayList<>(heightValues.length);
		for (float heightValue : heightValues) {
			result.add(heightValue);
		}
		return result;
	}
}
