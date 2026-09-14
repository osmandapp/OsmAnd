package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

/**
 * Compares what two calls do, not only what they return: a value, or the exception they throw.
 * Java's routing code throws on some inputs (a point without types, an attribute that is not a
 * number, an estimate between two points off the route), and the copy has to do the same there.
 */
final class Same {

	interface Call {
		Object get() throws Exception;
	}

	private Same() {
	}

	/** Doubles that come out of the same formula in a different association order: equal to a few ulps. */
	static void close(String m, double java, double copy) {
		double tolerance = 8 * Math.ulp(Math.max(Math.abs(java), Math.abs(copy)));
		assertEquals(m, java, copy, tolerance);
	}

	static void close(String m, float java, float copy) {
		float tolerance = 8 * Math.ulp(Math.max(Math.abs(java), Math.abs(copy)));
		assertEquals(m, java, copy, tolerance);
	}

	static void outcome(String m, Call java, Call copy) {
		assertEquals(m, describe(java), describe(copy));
	}

	private static String describe(Call call) {
		try {
			Object value = call.get();
			if (value instanceof int[]) {
				return Arrays.toString((int[]) value);
			}
			if (value instanceof float[]) {
				return Arrays.toString((float[]) value);
			}
			if (value instanceof Object[]) {
				return Arrays.deepToString((Object[]) value);
			}
			return String.valueOf(value);
		} catch (Throwable t) {
			return "threw " + t.getClass().getName();
		}
	}

	/** A route segment and its copy: the road, the stretch of it, the times, the distance, the speed and the manoeuvre. */
	static void segment(String m, net.osmand.router.RouteSegmentResult j, net.osmand.shared.routing.RouteSegmentResult k) {
		assertEquals(m + " road", j.getObject().id, k.getObject().id);
		assertEquals(m + " start", j.getStartPointIndex(), k.getStartPointIndex());
		assertEquals(m + " end", j.getEndPointIndex(), k.getEndPointIndex());
		assertArrayEquals(m + " pointsX", j.getObject().pointsX, k.getObject().pointsX);
		assertArrayEquals(m + " pointsY", j.getObject().pointsY, k.getObject().pointsY);
		close(m + " routingTime", j.getRoutingTime(), k.getRoutingTime());
		close(m + " distance", j.getDistance(), k.getDistance());
		close(m + " segmentTime", j.getSegmentTime(), k.getSegmentTime());
		close(m + " speed", j.getSegmentSpeed(), k.getSegmentSpeed());
		turn(m, j.getTurnType(), k.getTurnType());
	}

	static void turn(String m, net.osmand.router.TurnType j, net.osmand.shared.routing.TurnType k) {
		assertEquals(m + " turn present", j == null, k == null);
		if (j == null) {
			return;
		}
		assertEquals(m + " turn", j.getValue(), k.getValue());
		assertEquals(m + " exit", j.getExitOut(), k.getExitOut());
		close(m + " angle", j.getTurnAngle(), k.getTurnAngle());
		assertEquals(m + " skipToSpeak", j.isSkipToSpeak(), k.isSkipToSpeak());
		assertEquals(m + " possibleLeft", j.isPossibleLeftTurn(), k.isPossibleLeftTurn());
		assertEquals(m + " possibleRight", j.isPossibleRightTurn(), k.isPossibleRightTurn());
		assertArrayEquals(m + " lanes", j.getLanes(), k.getLanes());
		assertEquals(m + " toString", j.toString(), k.toString());
	}
}
