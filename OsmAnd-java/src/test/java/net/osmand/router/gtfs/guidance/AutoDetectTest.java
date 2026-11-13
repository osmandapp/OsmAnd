package net.osmand.router.gtfs.guidance;

import static net.osmand.router.gtfs.GuidanceTestItineraries.T0;
import static net.osmand.router.gtfs.GuidanceTestItineraries.east;
import static net.osmand.router.gtfs.GuidanceTestItineraries.loc;
import static net.osmand.router.gtfs.GuidanceTestItineraries.names;
import static net.osmand.router.gtfs.GuidanceTestItineraries.north;
import static net.osmand.router.gtfs.GuidanceTestItineraries.singleRide;
import static net.osmand.router.gtfs.guidance.GuidanceState.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.guidance.GtfsGuidanceEngine.Confidence;

/** Automatic boarding and alighting from speed, position and the timetable (no taps). */
public class AutoDetectTest {

	// singleRide(T0): Q departs stop 0 at T0+300, then every 120 s; stop k arrives 30 s before it departs;
	// stops are about 843 m apart, the last (5) arrives at T0+870.
	private static final long DEP = T0 + 300, ARR = T0 + 870;

	private static GtfsGuidanceEngine engine() {
		GtfsGuidanceEngine e = new GtfsGuidanceEngine(singleRide(T0), names());
		e.start(T0);
		return e;
	}

	private static void fix(GtfsGuidanceEngine e, LatLon p, double speed, long t) {
		e.onLocation(p.getLatitude(), p.getLongitude(), 5f, speed, Double.NaN, t, true);
	}

	private static void atPlatform(GtfsGuidanceEngine e, long from, long to) {
		for (long t = from; t <= to; t += 5) {
			fix(e, loc(0), 0, t);
		}
	}

	/** Rides east at 10 m/s from loc(0) starting at the given time, fixes every 2 s until `until`. */
	private static void ride(GtfsGuidanceEngine e, long start, long until) {
		for (long t = start; t <= until; t += 2) {
			fix(e, east(loc(0), 10.0 * (t - start)), 10, t);
		}
	}

	@Test
	public void gpsOnlyBoardingWithoutTapsReachesRidingAndArrives() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP);
		assertEquals(WAIT_FOR_VEHICLE, e.getState());
		ride(e, DEP + 5, DEP + 40);
		assertEquals(RIDING, e.getState());
		assertEquals(Confidence.AUTO, e.getConfidence());
		for (int k = 1; k <= 4; k++) {
			fix(e, loc(k), 12, DEP + 120L * k - 30);
		}
		assertEquals(RIDING, e.getState());
		fix(e, loc(5), 0.3, ARR);
		assertEquals(ARRIVED, e.getState());
		assertEquals(Confidence.AUTO, e.getConfidence());
	}

	@Test
	public void derivedSpeedWorksWithoutReportedSpeed() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP);
		for (long t = DEP + 5; t <= DEP + 40; t += 2) {
			LatLon p = east(loc(0), 10.0 * (t - DEP - 5));
			e.onLocation(p.getLatitude(), p.getLongitude(), 5f, t, true);
		}
		assertEquals(RIDING, e.getState());
	}

	@Test
	public void tunnelAfterPlatformIsAssumedBoardedThenAssumedAlighted() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP - 20);
		for (long t = DEP - 15; t < DEP + 55; t += 5) {
			e.onTick(t);
		}
		assertEquals(WAIT_FOR_VEHICLE, e.getState());
		e.onTick(DEP + 61);
		assertEquals(RIDING, e.getState());
		assertEquals(Confidence.ASSUMED, e.getConfidence());
		for (long t = DEP + 70; t < ARR + 55; t += 10) {
			e.onTick(t);
		}
		assertEquals(RIDING, e.getState());
		e.onTick(ARR + 61);
		assertEquals(ARRIVED, e.getState());
		assertEquals(Confidence.ASSUMED, e.getConfidence());
	}

	@Test
	public void assumedRideShowsConfirmationInStatus() {
		GuidanceController c = new GuidanceController(singleRide(T0), names());
		c.start(T0);
		for (long t = T0 + 200; t <= DEP - 20; t += 5) {
			c.onLocation(loc(0).getLatitude(), loc(0).getLongitude(), 5f, 0, Double.NaN, t, true);
		}
		c.onTick(DEP + 61);
		GuidanceStatus s = c.status(DEP + 61);
		assertEquals(RIDING, s.state);
		assertEquals(Confidence.ASSUMED, s.confidence);
		assertTrue(s.needsConfirmation);
		assertTrue(s.canNotOnTrain);
		assertEquals("Q", s.rideRoute);
	}

	@Test
	public void fixReturningNearExitStopConfirmsAlight() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP - 20);
		e.onTick(DEP + 61);
		assertEquals(Confidence.ASSUMED, e.getConfidence());
		for (long t = DEP + 70; t < ARR - 40; t += 10) {
			e.onTick(t);
		}
		assertEquals(RIDING, e.getState());
		fix(e, east(loc(5), -200), 6, ARR - 25);
		assertEquals(ARRIVED, e.getState());
		assertEquals(Confidence.AUTO, e.getConfidence());
	}

	@Test
	public void missedTrainWhenStillAtPlatformStaysMissed() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP + 200);
		assertEquals(MISSED, e.getState());
	}

	@Test
	public void standingAtPlatformAtDepartureDoesNotBoard() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP + 100);
		assertEquals(WAIT_FOR_VEHICLE, e.getState());
	}

	@Test
	public void walkingAlongParallelStreetDoesNotBoard() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, T0 + 290);
		LatLon start = north(loc(0), 100);
		for (long t = T0 + 295; t <= DEP + 300; t += 5) {
			fix(e, east(start, 1.4 * (t - T0 - 290)), 1.4, t);
			assertFalse(e.getState() == RIDING);
		}
	}

	@Test
	public void earlyDepartureIsDetected() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 150, T0 + 190);
		ride(e, T0 + 195, T0 + 230);
		assertEquals(RIDING, e.getState());
	}

	@Test
	public void lateDepartureWithinGraceIsDetected() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP + 85);
		ride(e, DEP + 90, DEP + 112);
		assertEquals(RIDING, e.getState());
	}

	@Test
	public void wrongTrainFarFromStopsOffersReplan() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP);
		ride(e, DEP + 5, DEP + 40);
		assertEquals(RIDING, e.getState());
		fix(e, north(loc(2), 2500), 15, DEP + 100);
		fix(e, north(loc(3), 2600), 15, DEP + 130);
		assertEquals(OFF_ROUTE, e.getState());
	}

	@Test
	public void manualCorrectionsOverrideAuto() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP - 20);
		e.onTick(DEP + 61);
		assertEquals(RIDING, e.getState());
		e.onUserAction(UserAction.NOT_ON_TRAIN, DEP + 70);
		assertEquals(MISSED, e.getState());
		// auto boarding stays off for this leg; the tap still works
		e.onUserAction(UserAction.BOARDED, DEP + 80);
		assertEquals(RIDING, e.getState());
		assertEquals(Confidence.MANUAL, e.getConfidence());
		e.onUserAction(UserAction.ALIGHTED, DEP + 200);
		assertEquals(ARRIVED, e.getState());
	}

	@Test
	public void motionSignalSupportsBoarding() {
		GtfsGuidanceEngine e = engine();
		atPlatform(e, T0 + 200, DEP);
		for (long t = DEP + 5; t <= DEP + 30; t += 2) {
			e.onMotion(t, 0.9);
			fix(e, east(loc(0), 3.0 * (t - DEP - 5)), 3.0, t);
		}
		assertEquals(RIDING, e.getState());
	}
}
