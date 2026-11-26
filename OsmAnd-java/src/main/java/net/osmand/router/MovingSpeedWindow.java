package net.osmand.router;

import net.osmand.Location;

/**
 * The speed measured while moving, for "Adapt to my speed": the average over the last interval of
 * movement after the start of navigation. Stops don't use up the interval, so after a stop the
 * speed before it still counts. Neither does the speeding up after a stop or after the start:
 * the slow seconds after a red light would fill most of a short interval.
 */
public class MovingSpeedWindow {

	private static final float MIN_MOVING_SPEED = 0.5f; // m/s in every speed unit, slower counts as standing still
	private static final long MAX_POINT_DURATION_MILLIS = 5000; // a longer gap without fixes is not counted as moving
	private static final long MAX_SPEED_UP_MILLIS = 10_000; // a car gets back to city speed after a stop in about 10 s
	private static final long MAX_BLEND_MILLIS = 15 * 60_000; // the route estimate is blended in for at most 15 min of movement
	private static final long FIRST_POINT_DURATION_MILLIS = 1000;

	private final long movingInterval;
	private final long movingTime;
	private final float averageSpeed;

	/**
	 * @param locations      points in time order, with their speeds
	 * @param startTimestamp points before it are not counted
	 * @param movingInterval how much of the latest movement is averaged, ms
	 */
	public MovingSpeedWindow(Iterable<Location> locations, long startTimestamp, long movingInterval) {
		this.movingInterval = movingInterval;
		long time = 0;
		MovingTimeCounter counter = new MovingTimeCounter(startTimestamp);
		for (Location location : locations) {
			time += counter.getMovingTime(location);
		}
		movingTime = time;

		long toSkip = movingTime - movingInterval;
		float distance = 0;
		time = 0;
		counter = new MovingTimeCounter(startTimestamp);
		for (Location location : locations) {
			long dt = counter.getMovingTime(location);
			if (dt <= 0) {
				continue;
			}
			if (toSkip > 0) {
				toSkip -= dt;
			} else {
				distance += location.getSpeed() * dt;
				time += dt;
			}
		}
		averageSpeed = time > 0 ? distance / time : Float.NaN;
	}

	/**
	 * Time of movement after the start, ms.
	 */
	public long getMovingTime() {
		return movingTime;
	}

	/**
	 * Average speed over the last interval of movement, m/s; NaN before the first movement.
	 */
	public float getAverageSpeed() {
		return averageSpeed;
	}

	/**
	 * The route's time left, in seconds, adapted to the measured speed. Until the interval of
	 * movement is filled the route estimate is blended in, so the first seconds can't swing the
	 * arrival time. The route estimate stays while there is no movement. The blend lasts at most
	 * 15 min of movement: points are kept for 60 min, so with stops a longer interval of movement
	 * may never be collected.
	 */
	public int adaptTime(int routeTime, int distance) {
		if (movingInterval <= 0 || Float.isNaN(averageSpeed)) {
			return routeTime;
		}
		float filled = Math.min(1f, (float) movingTime / Math.min(movingInterval, MAX_BLEND_MILLIS));
		return Math.round(routeTime + (distance / averageSpeed - routeTime) * filled);
	}

	/**
	 * Walks the points in time order and tells how long each one counts as movement. A stop counts
	 * as zero, and so does the speeding up after it: the points while the speed is still above the
	 * one two points earlier, at most {@link #MAX_SPEED_UP_MILLIS}. Two points, because a speed is
	 * often repeated for two fixes in a row.
	 */
	private static class MovingTimeCounter {

		private final long startTimestamp;

		private long prevTime;
		private float prevSpeed;
		private float speedTwoPointsEarlier;
		private boolean stopped = true;
		private boolean speedingUp;
		private long speedUpStartTime;

		MovingTimeCounter(long startTimestamp) {
			this.startTimestamp = startTimestamp;
		}

		long getMovingTime(Location location) {
			long time = location.getTime();
			float speed = location.getSpeed();
			long dt = prevTime > 0 ? time - prevTime : FIRST_POINT_DURATION_MILLIS;
			boolean faster = speed > speedTwoPointsEarlier;
			speedTwoPointsEarlier = prevSpeed;
			prevSpeed = speed;
			prevTime = time;

			if (speed < MIN_MOVING_SPEED) {
				stopped = true;
				return 0;
			}
			if (stopped) {
				stopped = false;
				speedingUp = true;
				speedUpStartTime = time;
			} else if (speedingUp) {
				speedingUp = faster && time - speedUpStartTime < MAX_SPEED_UP_MILLIS;
			}
			if (speedingUp || time < startTimestamp) {
				return 0;
			}
			return Math.max(0, Math.min(dt, MAX_POINT_DURATION_MILLIS));
		}
	}
}
