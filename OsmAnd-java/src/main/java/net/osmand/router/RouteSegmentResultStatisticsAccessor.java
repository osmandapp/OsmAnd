package net.osmand.router;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.routing.details.IRouteStatisticsAccessor;

import org.apache.commons.logging.Log;

import java.util.List;

/** Reads shared statistics inputs directly from Java/native route segments. */
final class RouteSegmentResultStatisticsAccessor implements IRouteStatisticsAccessor {

	private static final Log LOG = PlatformUtil.getLog(RouteSegmentResultStatisticsAccessor.class);

	private final List<RouteSegmentResult> segments;

	RouteSegmentResultStatisticsAccessor(List<RouteSegmentResult> segments) {
		this.segments = segments;
	}

	@Override
	public int getSegmentsCount() {
		return segments.size();
	}

	@Override
	public float getDistanceMeters(int segmentIndex) {
		return segments.get(segmentIndex).getDistance();
	}

	@Override
	public float[] getHeightValues(int segmentIndex) {
		return segments.get(segmentIndex).getHeightValues();
	}

	@Override
	public int getRouteTypesCount(int segmentIndex) {
		return getRoad(segmentIndex).getTypes().length;
	}

	@Override
	public String getRouteTypeTag(int segmentIndex, int routeTypeIndex) {
		RouteTypeRule rule = getRouteTypeRule(segmentIndex, routeTypeIndex, true);
		return rule != null ? rule.getTag() : null;
	}

	@Override
	public String getRouteTypeValue(int segmentIndex, int routeTypeIndex) {
		RouteTypeRule rule = getRouteTypeRule(segmentIndex, routeTypeIndex, false);
		return rule != null ? rule.getValue() : null;
	}

	private RouteTypeRule getRouteTypeRule(int segmentIndex, int routeTypeIndex, boolean logInvalid) {
		RouteDataObject road = getRoad(segmentIndex);
		int ruleId = road.getTypes()[routeTypeIndex];
		RouteRegion region = road.region;
		int encodingRulesSize = region.quickGetEncodingRulesSize();
		if (ruleId < 0 || ruleId >= encodingRulesSize) {
			if (logInvalid) {
				LOG.warn("Skipping invalid route encoding rule id=" + ruleId
						+ " for route object id=" + road.getId()
						+ ", rules=" + encodingRulesSize);
			}
			return null;
		}
		return region.quickGetEncodingRule(ruleId);
	}

	private RouteDataObject getRoad(int segmentIndex) {
		return segments.get(segmentIndex).getObject();
	}
}
