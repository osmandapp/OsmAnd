package net.osmand.router;

import java.util.BitSet;
import java.util.List;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.TransportRoute;
import net.osmand.router.GeneralRouter.RouteDataObjectAttribute;
import net.osmand.util.MapUtils;

/**
 * Ferry crossing time, same for all routing profiles: waiting for the ferry and the crossing duration.
 */
public class FerryRoutingHelper {

	public static final String FERRY = "ferry";
	public static final String DURATION_TAG = "duration";
	// speeds (km/h) that make a duration tag believable for its distance
	private static final double MIN_DURATION_SPEED = 1;
	private static final double MAX_DURATION_SPEED = 100;

	public static boolean isFerry(RouteDataObject road) {
		return FERRY.equals(road.getValue("route"));
	}

	// half of the ferry interval, otherwise the amenity=ferry_terminal routing obstacle of the profile
	public static double getWaitTime(GeneralRouter router, int interval) {
		if (interval > 0) {
			return interval / 2.0;
		}
		BitSet types = new BitSet();
		types.set(router.registerTagValueAttribute("amenity", "ferry_terminal"));
		return router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES).evaluateInt(types, 0);
	}

	// time from a duration tag, 0 if the tag is absent or unrealistic for the distance (meters)
	public static int parseDuration(String duration, double distance) {
		int seconds = TransportRoute.parseIntervalTagToSeconds(duration);
		double speed = seconds > 0 ? distance / seconds * 3.6 : 0;
		return speed >= MIN_DURATION_SPEED && speed <= MAX_DURATION_SPEED ? seconds : 0;
	}

	// ferry segments of a car, bicycle or pedestrian route
	public static void updateSegmentTimes(RoutingContext ctx, List<RouteSegmentResult> result) {
		for (int i = 0; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			RouteDataObject road = rr.getObject();
			if (!isFerry(road)) {
				continue;
			}
			double time = rr.getSegmentTime();
			double length = getLength(road);
			int duration = parseDuration(road.getValue(DURATION_TAG), length);
			if (duration > 0) {
				// the passed part of the ferry way takes the same part of its duration
				time = rr.getDistance() * duration / length;
			}
			if (i == 0 || !isFerry(result.get(i - 1).getObject())) {
				// waiting for the ferry once per crossing
				int interval = TransportRoute.parseIntervalTagToSeconds(road.getValue(TransportRoute.INTERVAL_KEY));
				time += getWaitTime((GeneralRouter) ctx.getRouter(), interval);
			}
			if (time > 0) {
				rr.setSegmentTime((float) time);
				rr.setSegmentSpeed((float) (rr.getDistance() / time)); // navigation calculates time left with the speed
			}
		}
	}

	private static double getLength(RouteDataObject road) {
		double length = 0;
		for (int i = 1; i < road.getPointsLength(); i++) {
			length += MapUtils.measuredDist31(road.getPoint31XTile(i - 1), road.getPoint31YTile(i - 1),
					road.getPoint31XTile(i), road.getPoint31YTile(i));
		}
		return length;
	}
}
