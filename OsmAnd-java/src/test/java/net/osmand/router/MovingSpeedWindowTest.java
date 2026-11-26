package net.osmand.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.Location;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The time left with "Adapt to my speed" (#26231) after stops and at the start: a 24 km route with
 * a route estimate of 37 min, one GPS point per second, speeds in km/h.
 */
public class MovingSpeedWindowTest {

	private static final long START = 1_000_000;
	private static final double ROUTE_DISTANCE = 24_000;
	private static final double ROUTE_SPEED = ROUTE_DISTANCE / (37 * 60);

	private final List<Location> points = new ArrayList<>();
	private long interval = 15_000; // the shortest Interval in the settings
	private long time = START;
	private double distanceLeft = ROUTE_DISTANCE;

	@Test
	public void redLightLongerThanIntervalKeepsTheTime() {
		// the third QA video: 47 min jumped to 4 h 45 min after the stop, later to 1 h 12 min
		drive(30, 35);
		for (int second = 0; second < 30; second++) {
			drive(0, 1);
			assertTimeAtSpeed(30);
		}
		for (int kmh = 5; kmh <= 25; kmh += 5) {
			for (int second = 0; second < 2; second++) {
				drive(kmh, 1);
				assertTimeAtSpeed(30);
			}
		}
		for (int second = 0; second < 20; second++) {
			drive(30, 1);
			assertTimeAtSpeed(30);
		}
	}

	@Test
	public void gradualStartDoesNotJumpUp() {
		// 0 -> 30 km/h in 10 s took 38 min to 1 h 19 min
		for (int second = 0; second < 40; second++) {
			addPoint(Math.min(30, 3 * (second + 1)));
			assertTrue(minutesLeft() >= routeMinutes() - 0.5);
			assertTrue(minutesLeft() <= minutesAtSpeed(30) + 0.5);
		}
		assertTimeAtSpeed(30);
	}

	@Test
	public void slowDrivingFromStartIsFollowed() {
		// TC-14: at 11 km/h the time grows to about 2 h 10 min and not above it
		for (int second = 0; second < 30; second++) {
			drive(11, 1);
			assertTrue(minutesLeft() <= minutesAtSpeed(11) + 0.5);
		}
		assertTimeAtSpeed(11);
	}

	@Test
	public void slowerDrivingAfterStopIsFollowed() {
		drive(30, 120);
		drive(0, 40);
		speedUp(0, 15, 5);
		drive(15, 30);
		assertTimeAtSpeed(15);
	}

	@Test
	public void withoutStopsTheTimeFollowsTheLastInterval() {
		interval = 30_000;
		drive(20, 40);
		drive(40, 20);
		// the last 30 s: 10 s at 20 km/h and 20 s at 40 km/h
		assertTimeAtSpeed((10 * 20 + 20 * 40) / 30.0);
	}

	@Test
	public void longIntervalUsesTheMeasuredSpeedAfterFifteenMinutesOfMovement() {
		// Interval 60 min, 2 min driving and 1 min standing: the route estimate stayed blended in for good
		interval = 60 * 60_000L;
		for (int minute = 1; minute <= 45; minute++) {
			drive(minute % 3 == 0 ? 0 : 30, 60);
		}
		assertTimeAtSpeed(30);
	}

	@Test
	public void gapWithoutFixesCountsAtMostFiveSeconds() {
		drive(30, 30);
		gap(30, 60);
		drive(30, 10);
		// the first two points are the speeding up at the start
		assertEquals(28_000 + 5_000 + 9_000, window().getMovingTime());
		assertTimeAtSpeed(30);
	}

	@Test
	public void slowWalkingCountsAndGpsDriftDoesNot() {
		// the stop threshold is 0.5 m/s in every speed unit; with m/s it was 1 m/s (#26737)
		drive(0.8 * 3.6, 60);
		drive(0.3 * 3.6, 20);
		assertEquals(0.8, window().getAverageSpeed(), 0.01);
		assertEquals(58_000, window().getMovingTime());
	}

	private void drive(double kmh, int seconds) {
		for (int second = 0; second < seconds; second++) {
			addPoint(kmh);
		}
	}

	private void speedUp(double fromKmh, double toKmh, int seconds) {
		for (int second = 1; second <= seconds; second++) {
			addPoint(fromKmh + (toKmh - fromKmh) * second / seconds);
		}
	}

	private void gap(double kmh, int seconds) {
		time += seconds * 1000L;
		distanceLeft -= kmh / 3.6 * seconds;
	}

	private void addPoint(double kmh) {
		Location location = new Location("test");
		location.setTime(time);
		location.setSpeed((float) (kmh / 3.6));
		points.add(location);
		time += 1000;
		distanceLeft -= kmh / 3.6;
	}

	private MovingSpeedWindow window() {
		return new MovingSpeedWindow(points, START, interval);
	}

	private double minutesLeft() {
		int routeTime = (int) Math.round(distanceLeft / ROUTE_SPEED);
		return window().adaptTime(routeTime, (int) Math.round(distanceLeft)) / 60.0;
	}

	private double routeMinutes() {
		return distanceLeft / ROUTE_SPEED / 60;
	}

	private double minutesAtSpeed(double kmh) {
		return distanceLeft / (kmh / 3.6) / 60;
	}

	private void assertTimeAtSpeed(double kmh) {
		assertEquals(minutesAtSpeed(kmh), minutesLeft(), 0.5);
	}
}
