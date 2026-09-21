package net.osmand.router;

import java.util.List;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.TransportRoute;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.MapUtils;

/**
 * Ferry crossing time, same for all routing profiles, route search and estimated time:
 * waiting for a ferry (half of its interval, otherwise ferryBoardingTime), getting on it (ferryTerminalTime),
 * sailing (a part of the duration tag, otherwise with the ferry speed and ferryTerminalTime at each terminal
 * on the way) and getting off it (half of ferryTerminalTime). Getting on and off is paid only for a whole crossing:
 * a route starting or ending on a ferry (at its terminal or on board) only sails.
 * The routers apply it to their passengers (see GeneralRouter), the planners don't use it directly.
 */
public class FerryRoutingHelper {

	public static final String FERRY = "ferry";
	public static final String DURATION_TAG = "duration";
	// routing.xml attributes, seconds
	public static final String BOARDING_TIME_ATTRIBUTE = "ferryBoardingTime";
	public static final String TERMINAL_TIME_ATTRIBUTE = "ferryTerminalTime";
	// speeds (km/h) that make a duration tag believable for its distance
	private static final double MIN_DURATION_SPEED = 1;
	private static final double MAX_DURATION_SPEED = 100;

	public static boolean isFerry(RouteDataObject road) {
		return road.containsType(road.region.ferry);
	}

	// waiting for a ferry (half of its interval if known) and getting on it while it stands at the terminal
	public static double getBoardingTime(int ferryBoardingTime, int ferryTerminalTime, int interval) {
		return (interval > 0 ? interval / 2.0 : ferryBoardingTime) + ferryTerminalTime;
	}

	// getting off doesn't wait until the ferry finishes its stop
	public static double getAlightingTime(int ferryTerminalTime) {
		return ferryTerminalTime / 2.0;
	}

	// time from a duration tag, 0 if the tag is absent or unrealistic for the distance (meters)
	public static int parseDuration(String duration, double distance) {
		int seconds = TransportRoute.parseIntervalTagToSeconds(duration);
		double speed = seconds > 0 ? distance / seconds * 3.6 : 0;
		return speed >= MIN_DURATION_SPEED && speed <= MAX_DURATION_SPEED ? seconds : 0;
	}

	// a ferry with a duration tag moves with the speed from it
	public static float getRoutingSpeed(RouteDataObject road, float speed) {
		double durationSpeed = speed > 0 && isFerry(road) ? getDurationSpeed(road) : 0;
		return durationSpeed > 0 ? (float) durationSpeed : speed;
	}

	// getting on or off a ferry when the route goes from one road to another
	public static double getTransitionTime(int ferryBoardingTime, int ferryTerminalTime,
	                                       RouteDataObject from, RouteDataObject to) {
		boolean toFerry = isFerry(to);
		if (isFerry(from) == toFerry) {
			return 0;
		}
		return toFerry ? getBoardingTime(ferryBoardingTime, ferryTerminalTime, getInterval(to))
				: getAlightingTime(ferryTerminalTime);
	}

	// the ferry stops at the point between the segment and its parent (both directions of search)
	public static double getStopTime(int ferryTerminalTime, RouteSegment segment) {
		RouteSegment parent = segment.getParentRoute();
		if (parent == null || !isFerry(segment.getRoad()) || !isFerry(parent.getRoad())) {
			return 0;
		}
		return getStopTime(ferryTerminalTime, segment.getRoad(), segment.getSegmentStart(),
				parent.getRoad(), parent.getSegmentEnd());
	}

	// estimated time of the ferry segments, calculated again for the whole segment
	public static void updateSegmentTimes(VehicleRouter router, int ferryBoardingTime, int ferryTerminalTime,
	                                      List<RouteSegmentResult> result) {
		for (int i = 0; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			RouteDataObject road = rr.getObject();
			if (!isFerry(road)) {
				continue;
			}
			double speed = getDurationSpeed(road);
			if (speed <= 0) {
				speed = router.defineVehicleSpeed(road, rr.isForwardDirection());
			}
			if (speed <= 0) {
				speed = router.getDefaultSpeed();
			}
			double time = rr.getDistance() / speed;
			int start = Math.min(rr.getStartPointIndex(), rr.getEndPointIndex());
			int end = Math.max(rr.getStartPointIndex(), rr.getEndPointIndex());
			for (int point = start + 1; point < end; point++) {
				time += getStopTime(ferryTerminalTime, road, point, road, point);
			}
			boolean crossing = isCrossing(result, i);
			if (crossing && !isFerry(result.get(i - 1).getObject())) {
				time += getBoardingTime(ferryBoardingTime, ferryTerminalTime, getInterval(road));
			}
			RouteSegmentResult next = i + 1 < result.size() ? result.get(i + 1) : null;
			if (next != null && isFerry(next.getObject())) {
				time += getStopTime(ferryTerminalTime, road, rr.getEndPointIndex(),
						next.getObject(), next.getStartPointIndex());
			} else if (crossing) {
				time += getAlightingTime(ferryTerminalTime);
			}
			if (time > 0) {
				rr.setSegmentTime((float) time);
				rr.setSegmentSpeed((float) (rr.getDistance() / time)); // navigation calculates time left with the speed
			}
		}
	}

	// the route crosses water by a ferry, not only starts or ends at its terminal
	public static boolean hasCrossing(List<RouteSegmentResult> result) {
		for (int i = 0; i < result.size(); i++) {
			if (isFerry(result.get(i).getObject()) && isCrossing(result, i)) {
				return true;
			}
		}
		return false;
	}

	// the ferry segments around this one have roads before and after them
	private static boolean isCrossing(List<RouteSegmentResult> result, int ferrySegment) {
		int first = ferrySegment;
		int last = ferrySegment;
		while (first > 0 && isFerry(result.get(first - 1).getObject())) {
			first--;
		}
		while (last < result.size() - 1 && isFerry(result.get(last + 1).getObject())) {
			last++;
		}
		return first > 0 && last < result.size() - 1;
	}

	private static int getInterval(RouteDataObject road) {
		return TransportRoute.parseIntervalTagToSeconds(road.getValue(TransportRoute.INTERVAL_KEY));
	}

	// the same point of two ferry ways (or of one way): the ferry stops at a terminal
	// unless a duration of a way includes this stop
	private static double getStopTime(int ferryTerminalTime, RouteDataObject road, int point,
	                                  RouteDataObject other, int otherPoint) {
		boolean terminal = "ferry_terminal".equals(road.getValue(point, "amenity"));
		return terminal && !isInDuration(road, point) && !isInDuration(other, otherPoint) ? ferryTerminalTime : 0;
	}

	private static boolean isInDuration(RouteDataObject road, int point) {
		return point > 0 && point < road.getPointsLength() - 1 && getDurationSpeed(road) > 0;
	}

	// meters per second, 0 without a duration tag
	private static double getDurationSpeed(RouteDataObject road) {
		String duration = road.getValue(DURATION_TAG);
		if (duration == null) {
			return 0;
		}
		double length = 0;
		for (int i = 1; i < road.getPointsLength(); i++) {
			length += MapUtils.measuredDist31(road.getPoint31XTile(i - 1), road.getPoint31YTile(i - 1),
					road.getPoint31XTile(i), road.getPoint31YTile(i));
		}
		int seconds = parseDuration(duration, length);
		return seconds > 0 ? length / seconds : 0;
	}
}
