package net.osmand.router;

import net.osmand.Location;

/**
 * The speed measured while moving, for "Adapt to my speed": the average over the last interval of
 * movement after the start of navigation. Stops don't use up the interval, so after a stop the
 * speed before it still counts.
 */
public class MovingSpeedWindow {

	private static final float MIN_AVERAGE_SPEED = 0.5f; // m/s, slower counts as standing still
	private static final long MAX_POINT_DURATION_MILLIS = 5000; // a longer gap without fixes is not counted as moving
	private static final long FIRST_POINT_DURATION_MILLIS = 1000;

	private final long movingInterval;
	private final long movingTime;
	private final float averageSpeed;

	/**
	 * @param locations      points in time order, with their speeds
	 * @param startTimestamp points before it are not counted
	 * @param movingInterval how much of the latest movement is averaged, ms
	 * @param stopSpeed      points slower than this are stops, m/s
	 */
	public MovingSpeedWindow(Iterable<Location> locations, long startTimestamp, long movingInterval, float stopSpeed) {
		this.movingInterval = movingInterval;
		long time = 0;
		long prevTime = 0;
		for (Location location : locations) {
			long locationTime = location.getTime();
			if (locationTime >= startTimestamp && location.getSpeed() >= stopSpeed) {
				time += getPointDuration(prevTime, locationTime);
			}
			prevTime = locationTime;
		}
		movingTime = time;

		long toSkip = movingTime - movingInterval;
		float distance = 0;
		time = 0;
		prevTime = 0;
		for (Location location : locations) {
			long locationTime = location.getTime();
			if (locationTime >= startTimestamp && location.getSpeed() >= stopSpeed) {
				long dt = getPointDuration(prevTime, locationTime);
				if (toSkip > 0) {
					toSkip -= dt;
				} else {
					distance += location.getSpeed() * dt;
					time += dt;
				}
			}
			prevTime = locationTime;
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
	 * arrival time. The route estimate stays while there is no movement.
	 */
	public int adaptTime(int routeTime, int distance) {
		if (movingInterval <= 0 || Float.isNaN(averageSpeed) || averageSpeed < MIN_AVERAGE_SPEED) {
			return routeTime;
		}
		float filled = Math.min(1f, (float) movingTime / movingInterval);
		return Math.round(routeTime + (distance / averageSpeed - routeTime) * filled);
	}

	private static long getPointDuration(long prevTime, long time) {
		long dt = prevTime > 0 ? time - prevTime : FIRST_POINT_DURATION_MILLIS;
		return Math.max(0, Math.min(dt, MAX_POINT_DURATION_MILLIS));
	}
}
