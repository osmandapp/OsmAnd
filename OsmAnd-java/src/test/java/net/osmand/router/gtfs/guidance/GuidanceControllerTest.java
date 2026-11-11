package net.osmand.router.gtfs.guidance;

import static net.osmand.router.gtfs.GuidanceTestItineraries.DEST;
import static net.osmand.router.gtfs.GuidanceTestItineraries.ORIGIN;
import static net.osmand.router.gtfs.GuidanceTestItineraries.T0;
import static net.osmand.router.gtfs.GuidanceTestItineraries.east;
import static net.osmand.router.gtfs.GuidanceTestItineraries.loc;
import static net.osmand.router.gtfs.GuidanceTestItineraries.names;
import static net.osmand.router.gtfs.GuidanceTestItineraries.north;
import static net.osmand.router.gtfs.GuidanceTestItineraries.singleRide;
import static net.osmand.router.gtfs.GuidanceTestItineraries.standard;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.guidance.GuidanceController.Command;
import net.osmand.router.gtfs.guidance.GuidanceController.Update;

public class GuidanceControllerTest {

	private static long t(long offset) {
		return T0 + offset;
	}

	private static GuidanceController controller(GtfsItinerary it) {
		return new GuidanceController(it, names());
	}

	private static List<String> keys(Update u) {
		List<String> out = new ArrayList<>();
		for (GuidanceEvent e : u.spoken) {
			out.add(e.getTextKey());
		}
		return out;
	}

	@Test
	public void startShowsTheFirstWalk() {
		GuidanceController c = controller(standard(T0));
		Update u = c.start(t(0));
		assertEquals(Arrays.asList("guidance.walk"), keys(u));
		assertFalse(u.finished);
		GuidanceStatus s = c.status(t(0));
		assertEquals(GuidanceState.WALK_TO_BOARD, s.state);
		assertEquals("guidance.walk", s.textKey);
		assertEquals(Arrays.<Object>asList("Stop 0"), s.args);
		assertEquals(-1, s.walkRemainingM);
		assertTrue(s.canBoard && s.canSkip && s.canReplan);
		assertFalse(s.canAlight);
	}

	@Test
	public void walkShowsDistanceRemainingFromAFreshFixOnly() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		c.onLocation(ORIGIN.getLatitude(), ORIGIN.getLongitude(), 5f, t(1), true);
		int d = c.status(t(1)).walkRemainingM;
		assertTrue("distance to stop 0 is about 111 m, was " + d, d > 90 && d < 130);
		// a minute and a bit later without any fix the number is stale and hidden
		c.onTick(t(70));
		assertEquals(-1, c.status(t(70)).walkRemainingM);
		// a poor fix does not count either
		c.onLocation(ORIGIN.getLatitude(), ORIGIN.getLongitude(), 500f, t(71), true);
		assertEquals(-1, c.status(t(71)).walkRemainingM);
	}

	@Test
	public void subwayRideNeedsNoFixAndOffersAlightedOnly() {
		GuidanceController c = controller(singleRide(T0));
		c.start(t(0));
		Update u = c.onCommand(Command.BOARDED, t(301));
		assertEquals(Arrays.asList("guidance.rideStarted"), keys(u));
		GuidanceStatus s = c.status(t(301));
		assertEquals(GuidanceState.RIDING, s.state);
		assertTrue(s.canAlight && s.canReplan);
		assertFalse(s.canBoard || s.canSkip);
		assertEquals(5, ((Number) s.args.get(1)).intValue());
		// the clock alone moves the ride on: stop 1 departs at +420, stop 2 at +540
		Update tick = c.onTick(t(545));
		assertEquals(Arrays.asList("guidance.nextStop"), keys(tick));
		GuidanceStatus next = c.status(t(545));
		assertEquals("guidance.nextStop", next.textKey);
		assertEquals(Arrays.<Object>asList("Stop 3", 3), next.args);
		Update off = c.onTick(t(660));
		assertEquals(Arrays.asList("guidance.nextStop"), keys(off));
		off = c.onTick(t(780));
		assertEquals(Arrays.asList("guidance.getOffNext"), keys(off));
		assertEquals("guidance.getOffNext", c.status(t(780)).textKey);
		Update done = c.onCommand(Command.ALIGHTED, t(900));
		assertTrue(done.finished);
		assertTrue(done.arrived);
		assertEquals(Arrays.asList("guidance.arrived"), keys(done));
		assertFalse(c.status(t(900)).canReplan);
	}

	@Test
	public void transferShowsTheWalkAfterAlighting() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		c.onCommand(Command.BOARDED, t(602));
		Update u = c.onCommand(Command.ALIGHTED, t(1115));
		assertEquals(Arrays.asList("guidance.transfer", "guidance.walk"), keys(u));
		GuidanceStatus s = c.status(t(1115));
		assertEquals(GuidanceState.TRANSFER_WALK, s.state);
		assertEquals("guidance.walk", s.textKey);
		assertEquals(Arrays.<Object>asList("Stop 10"), s.args);
		Update board = c.onCommand(Command.BOARDED, t(1200));
		assertEquals(Arrays.asList("guidance.rideStarted"), keys(board));
	}

	@Test
	public void skipEndsAWait() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		c.onLocation(loc(0).getLatitude(), loc(0).getLongitude(), 5f, t(200), true);
		assertEquals(GuidanceState.WAIT_FOR_VEHICLE, c.status(t(200)).state);
		assertEquals("guidance.board", c.status(t(200)).textKey);
		Update u = c.onCommand(Command.SKIP, t(210));
		assertFalse(u.finished);
		assertEquals(GuidanceState.TRANSFER_WALK, c.status(t(210)).state);
		assertEquals(2, c.getEngine().getLegIndex());
	}

	@Test
	public void missedOffersReplanAndTheTapStartsIt() {
		GuidanceController c = controller(singleRide(T0));
		c.start(t(0));
		c.onLocation(loc(0).getLatitude(), loc(0).getLongitude(), 5f, t(10), true);
		Update missed = c.onTick(t(300 + 121));
		assertEquals(Arrays.asList("guidance.missed", "guidance.replan"), keys(missed));
		assertNull(missed.replan);
		assertFalse(missed.finished);
		GuidanceStatus s = c.status(t(500));
		assertEquals(GuidanceState.MISSED, s.state);
		assertEquals("guidance.missed", s.textKey);
		Update again = c.onTick(t(5000));
		assertTrue(again.spoken.isEmpty());
		Update ack = c.onCommand(Command.REPLAN, t(5001));
		assertTrue(ack.finished);
		assertNotNull(ack.replan);
		assertEquals(loc(0), ack.replan.from);
		assertEquals(loc(5), ack.replan.to);
		assertEquals(Arrays.asList("guidance.cancelled"), keys(ack));
	}

	@Test
	public void offRouteReplanUsesTheLastFixAndIsNotSpokenTwice() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		c.onLocation(east(ORIGIN, 600).getLatitude(), east(ORIGIN, 600).getLongitude(), 5f, t(10), true);
		Update offer = c.onLocation(east(ORIGIN, 650).getLatitude(), east(ORIGIN, 650).getLongitude(), 5f, t(20), true);
		assertEquals(Arrays.asList("guidance.offRoute"), keys(offer));
		GuidanceStatus s = c.status(t(20));
		assertEquals(GuidanceState.OFF_ROUTE, s.state);
		assertEquals("guidance.offRoute", s.textKey);
		Update ack = c.onCommand(Command.REPLAN, t(30));
		assertTrue(ack.finished);
		assertEquals(east(ORIGIN, 650), ack.replan.from);
		assertEquals(DEST, ack.replan.to);
		assertEquals(t(30), ack.replan.departSec);
		// "Replan from now?" is not spoken: the traveller just asked for it
		assertEquals(Arrays.asList("guidance.cancelled"), keys(ack));
	}

	@Test
	public void offRouteThenBackOnRouteShowsTheOldInstruction() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		c.onLocation(east(ORIGIN, 600).getLatitude(), east(ORIGIN, 600).getLongitude(), 5f, t(10), true);
		c.onLocation(east(ORIGIN, 650).getLatitude(), east(ORIGIN, 650).getLongitude(), 5f, t(20), true);
		Update back = c.onLocation(ORIGIN.getLatitude(), ORIGIN.getLongitude(), 5f, t(40), true);
		assertEquals(Arrays.asList("guidance.routeResumed"), keys(back));
		GuidanceStatus s = c.status(t(40));
		assertEquals(GuidanceState.WALK_TO_BOARD, s.state);
		assertEquals("guidance.walk", s.textKey);
	}

	@Test
	public void manualReplanWhileRidingUsesTheScheduledStopWhenThereIsNoFix() {
		GuidanceController c = controller(singleRide(T0));
		c.start(t(0));
		c.onCommand(Command.BOARDED, t(301));
		c.onTick(t(545)); // schedule says the vehicle has left stop 2
		Update u = c.onCommand(Command.REPLAN, t(550));
		assertTrue(u.finished);
		assertFalse(u.arrived);
		assertNotNull(u.replan);
		assertEquals(loc(2), u.replan.from);
		assertEquals(loc(5), u.replan.to);
		assertEquals(t(550), u.replan.departSec);
		assertTrue(keys(u).isEmpty());
	}

	@Test
	public void manualReplanWithAFreshFixStartsThere() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		c.onLocation(north(ORIGIN, 20).getLatitude(), north(ORIGIN, 20).getLongitude(), 5f, t(5), true);
		Update u = c.onCommand(Command.REPLAN, t(8));
		assertEquals(north(ORIGIN, 20), u.replan.from);
		assertEquals(DEST, u.replan.to);
		assertTrue(u.finished);
	}

	@Test
	public void cancelEndsQuietly() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		Update u = c.onCommand(Command.CANCEL, t(5));
		assertTrue(u.finished);
		assertNull(u.replan);
		assertFalse(u.arrived);
		assertTrue(u.spoken.isEmpty());
		assertTrue(c.isFinished());
		// further input after the end does nothing
		Update after = c.onTick(t(10));
		assertTrue(after.spoken.isEmpty());
		assertTrue(c.onCommand(Command.REPLAN, t(11)).replan == null);
	}

	@Test
	public void happyPathEndsArrived() {
		GuidanceController c = controller(standard(T0));
		c.start(t(0));
		c.onLocation(loc(0).getLatitude(), loc(0).getLongitude(), 5f, t(200), true);
		c.onCommand(Command.BOARDED, t(602));
		c.onCommand(Command.ALIGHTED, t(1115));
		c.onCommand(Command.BOARDED, t(1502));
		c.onCommand(Command.ALIGHTED, t(2015));
		Update last = c.onLocation(DEST.getLatitude(), DEST.getLongitude(), 5f, t(2100), true);
		assertTrue(last.arrived);
		assertTrue(last.finished);
		assertEquals(GuidanceState.ARRIVED, c.status(t(2100)).state);
		assertEquals("guidance.arrived", c.status(t(2100)).textKey);
	}
}
