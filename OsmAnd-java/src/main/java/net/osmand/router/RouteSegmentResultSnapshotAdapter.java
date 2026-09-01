package net.osmand.router;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.routing.details.RouteSegment;
import net.osmand.shared.routing.details.RouteTypeAttribute;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/** Copies statistics inputs from one Java/native route segment into the shared contract. */
public final class RouteSegmentResultSnapshotAdapter {

	private static final Log LOG = PlatformUtil.getLog(RouteSegmentResultSnapshotAdapter.class);

	private RouteSegmentResultSnapshotAdapter() {
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
				LOG.warn("Skipping invalid route encoding rule id=" + type
						+ " for route object id=" + road.getId()
						+ ", rules=" + encodingRulesSize);
				continue;
			}
			RouteTypeRule rule = region.quickGetEncodingRule(type);
			if (rule != null && rule.getTag() != null) {
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
