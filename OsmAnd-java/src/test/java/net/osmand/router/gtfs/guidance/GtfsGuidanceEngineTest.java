package net.osmand.router.gtfs.guidance;

import static net.osmand.router.gtfs.GuidanceTestItineraries.DEST;
import static net.osmand.router.gtfs.GuidanceTestItineraries.ORIGIN;
import static net.osmand.router.gtfs.GuidanceTestItineraries.T0;
import static net.osmand.router.gtfs.GuidanceTestItineraries.T_LATE;
import static net.osmand.router.gtfs.GuidanceTestItineraries.east;
import static net.osmand.router.gtfs.GuidanceTestItineraries.loc;
import static net.osmand.router.gtfs.GuidanceTestItineraries.names;
import static net.osmand.router.gtfs.GuidanceTestItineraries.namesWithoutLocations;
import static net.osmand.router.gtfs.GuidanceTestItineraries.north;
import static net.osmand.router.gtfs.GuidanceTestItineraries.singleRide;
import static net.osmand.router.gtfs.GuidanceTestItineraries.standard;
import static net.osmand.router.gtfs.guidance.Sim.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.osmand.router.gtfs.DelayProvider;
import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.GuidanceTestItineraries;

import static net.osmand.router.gtfs.guidance.GuidanceEventType.BOARD_PROMPT;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.GET_OFF_NEXT;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.LEG_CHANGED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.LEG_SKIPPED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.NEXT_STOP;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.OFF_ROUTE_OFFER;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.REPLAN_REQUESTED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.RIDE_ENDED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.RIDE_STARTED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.ROUTE_RESUMED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.STATE_CHANGED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.TRANSFER_INSTRUCTION;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.WALK_LEG_ENDED;
import static net.osmand.router.gtfs.guidance.GuidanceEventType.WALK_LEG_STARTED;
import static net.osmand.router.gtfs.guidance.GuidanceState.*;
import static net.osmand.router.gtfs.guidance.UserAction.*;

public class GtfsGuidanceEngineTest {

	private static Sim sim(GtfsItinerary it) {
		return new Sim(new GtfsGuidanceEngine(it, names()));
	}

	private static Sim sim() {
		return sim(standard(T0));
	}

	/** Walks the whole standard itinerary with good fixes and taps; returns the sim. */
	private static Sim happyPath() {
		Sim s = sim();
		s.fix(ORIGIN, t(0));
		s.fix(north(loc(0), -80), t(100));
		s.fix(north(loc(0), -30), t(170));
		s.tick(t(300)).tick(t(599));
		s.act(BOARDED, t(602));
		s.fix(loc(1), t(790));
		s.fix(loc(2), t(965));
		s.fix(loc(3), t(1115));
		s.fix(north(loc(10), -100), t(1200));
		s.fix(north(loc(10), -20), t(1250));
		s.tick(t(1400));
		s.act(BOARDED, t(1502));
		s.fix(loc(11), t(1690));
		s.fix(loc(12), t(1870));
		s.fix(loc(13), t(2015));
		s.fix(north(DEST, -90), t(2060));
		s.fix(north(DEST, -30), t(2100));
		return s;
	}

	@Test
	public void happyPathWithGps() {
		Sim s = happyPath();
		assertEquals(Arrays.asList(WALK_TO_BOARD, WAIT_FOR_VEHICLE, RIDING, TRANSFER_WALK, WAIT_FOR_VEHICLE, RIDING,
				WALK_TO_DEST, ARRIVED), s.states());
		assertEquals(Arrays.asList("Walk to Stop 0", "Board the Q toward Uptown at Stop 0, departs 08:10",
				"Get off at Stop 3, 3 stops", "Next stop: Stop 2. 2 stops remaining", "Next stop: get off at Stop 3",
				"Transfer to the F at Stop 10, departs 08:25", "Walk to Stop 10", "Board the F at Stop 10, departs 08:25",
				"Get off at Stop 13, 3 stops", "Next stop: Stop 12. 2 stops remaining", "Next stop: get off at Stop 13",
				"Walk to your destination", "You have arrived"), s.texts());
		assertTrue(s.engine.isFinished());
		assertEquals(0, s.of(REPLAN_REQUESTED).size());
		assertEquals(0, s.of(OFF_ROUTE_OFFER).size());
	}

	@Test
	public void walkEventsCarryCoordinatesAndTimes() {
		Sim s = happyPath();
		List<GuidanceEvent> started = s.of(WALK_LEG_STARTED), ended = s.of(WALK_LEG_ENDED);
		assertEquals(3, started.size());
		assertEquals(3, ended.size());
		assertEquals(ORIGIN, started.get(0).getFrom());
		assertEquals(loc(0), started.get(0).getTo());
		assertEquals(t(180), started.get(0).getTimeSec());
		assertEquals(0, started.get(0).getLegIndex());
		assertEquals(loc(3), started.get(1).getFrom());
		assertEquals(loc(10), started.get(1).getTo());
		assertEquals(DEST, started.get(2).getTo());
		assertEquals(4, started.get(2).getLegIndex());
		assertEquals(loc(0), ended.get(0).getTo());
	}

	@Test
	public void structuredPayloadOfRidePrompts() {
		Sim s = happyPath();
		GuidanceEvent board = s.of(BOARD_PROMPT).get(0);
		assertEquals("guidance.board", board.getTextKey());
		assertEquals(1, board.getLegIndex());
		assertEquals(0, board.getStopIdx());
		assertEquals(0, board.getStopPosition());
		assertEquals(t(600), board.getTimeSec());
		assertEquals(Arrays.asList("Q", "Uptown", "Stop 0", t(600)), board.getArgs());
		GuidanceEvent next = s.of(NEXT_STOP).get(0);
		assertEquals(2, next.getStopIdx());
		assertEquals(2, next.getStopPosition());
		assertEquals(2, next.getStopsRemaining());
		assertEquals(Arrays.asList("Stop 2", 2), next.getArgs());
		GuidanceEvent off = s.of(GET_OFF_NEXT).get(0);
		assertEquals(3, off.getStopIdx());
		assertEquals(1, off.getStopsRemaining());
		GuidanceEvent transfer = s.of(TRANSFER_INSTRUCTION).get(0);
		assertEquals(3, transfer.getLegIndex());
		assertEquals(10, transfer.getStopIdx());
		assertEquals("F", transfer.getArgs().get(0));
		assertNull(transfer.getArgs().get(1));
		assertEquals(null, s.of(STATE_CHANGED).get(0).getTextKey());
	}

	@Test
	public void subwayRideWithoutAnyFixIsTickAndTapDriven() {
		Sim s = sim(singleRide(T0));
		s.tick(t(0));
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		for (long x = 10; x < 300; x += 10) {
			s.noFix(t(x));
		}
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		s.act(BOARDED, t(303));
		assertEquals(RIDING, s.state());
		for (long x = 310; x <= 1000; x += 10) {
			s.noFix(t(x));
		}
		assertEquals(ARRIVED, s.state());
		assertEquals(Arrays.asList("Board the Q toward Uptown at Stop 0, departs 08:05", "Get off at Stop 5, 5 stops",
				"Next stop: Stop 2. 4 stops remaining", "Next stop: Stop 3. 3 stops remaining",
				"Next stop: Stop 4. 2 stops remaining", "Next stop: get off at Stop 5", "You have arrived"), s.texts());
		// scheduled progress: left stop 1 at 420, stop 2 at 540, stop 3 at 660, stop 4 at 780
		assertEquals(t(420), nextStopAt(s, 2));
		assertEquals(t(780), s.of(GET_OFF_NEXT).get(0).getEpochSec());
		// ARRIVED comes 60 s after the scheduled arrival (870), on the first tick at or after that
		assertEquals(t(930), s.of(GuidanceEventType.ARRIVED).get(0).getEpochSec());
		assertEquals(0, s.of(OFF_ROUTE_OFFER).size());
	}

	private static long nextStopAt(Sim s, int stopIdx) {
		for (GuidanceEvent e : s.of(NEXT_STOP)) {
			if (e.getStopIdx() == stopIdx) {
				return e.getEpochSec();
			}
		}
		throw new AssertionError("no NEXT_STOP for " + stopIdx);
	}

	@Test
	public void subwayWithTapsOnAllLegsAndNoFix() {
		Sim s = sim();
		s.tick(t(0));
		s.ticks(t(10), t(600), 10);
		s.act(BOARDED, t(604));
		s.ticks(t(610), t(1105), 5);
		s.act(ALIGHTED, t(1112));
		assertEquals(TRANSFER_WALK, s.state());
		s.ticks(t(1120), t(1495), 5);
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		s.act(BOARDED, t(1503));
		s.ticks(t(1510), t(2005), 5);
		s.act(ALIGHTED, t(2012));
		assertEquals(WALK_TO_DEST, s.state());
		s.ticks(t(2015), t(2400), 5);
		assertEquals(ARRIVED, s.state());
		assertEquals(0, s.count(GuidanceEventType.MISSED));
		assertEquals(Arrays.asList(WALK_TO_BOARD, WAIT_FOR_VEHICLE, RIDING, TRANSFER_WALK, WAIT_FOR_VEHICLE, RIDING,
				WALK_TO_DEST, ARRIVED), s.states());
	}

	@Test
	public void userForgetsToTapAlightIsAssumedFromSchedule() {
		Sim s = sim();
		s.tick(t(0));
		s.ticks(t(10), t(600), 10);
		s.act(BOARDED, t(604));
		s.ticks(t(610), t(1100), 10);
		assertEquals(RIDING, s.state());
		s.ticks(t(1110), t(1180), 10);
		assertEquals(TRANSFER_WALK, s.state());
		assertEquals(1, s.count(TRANSFER_INSTRUCTION));
		assertEquals(t(1170), s.of(RIDE_ENDED).get(0).getEpochSec());
	}

	@Test
	public void userForgetsToTapBoardGetsMissedThenRecoversWithAlightTap() {
		Sim s = sim();
		s.tick(t(0));
		s.ticks(t(10), t(600), 10);
		s.tick(t(720));
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		s.tick(t(721));
		assertEquals(MISSED, s.state());
		GuidanceEvent replan = s.of(REPLAN_REQUESTED).get(0);
		assertEquals(t(721), replan.getTimeSec());
		assertEquals(loc(0), replan.getFrom());
		assertEquals(DEST, replan.getTo());
		s.act(ALIGHTED, t(1115));
		assertEquals(TRANSFER_WALK, s.state());
		assertEquals(1, s.count(REPLAN_REQUESTED));
	}

	@Test
	public void missedTrainWhileWalkingSlowlyWithFixes() {
		Sim s = sim();
		s.fix(north(ORIGIN, -150), t(0));
		for (long x = 30; x <= 720; x += 30) {
			s.fix(north(ORIGIN, -150), t(x));
		}
		assertEquals(WALK_TO_BOARD, s.state());
		s.fix(north(ORIGIN, -150), t(721));
		assertEquals(MISSED, s.state());
		assertEquals(Arrays.asList("Walk to Stop 0", "You missed the Q at Stop 0, which left at 08:10", "Replan from now?"),
				s.texts());
		GuidanceEvent replan = s.of(REPLAN_REQUESTED).get(0);
		assertEquals(t(721), replan.getTimeSec());
		assertEquals(north(ORIGIN, -150), replan.getFrom());
		assertEquals(1, s.of(GuidanceEventType.MISSED).get(0).getLegIndex());
		s.tick(t(800));
		s.tick(t(5000));
		assertEquals(MISSED, s.state());
		assertEquals(1, s.count(REPLAN_REQUESTED));
		s.act(REPLAN_ACK, t(5001));
		assertEquals(CANCELLED, s.state());
		assertEquals(Arrays.asList("replan"), s.of(GuidanceEventType.CANCELLED).get(0).getArgs());
		assertTrue(s.engine.isFinished());
	}

	@Test
	public void lateVehicleAfterMissedStillBoardsWhenUserTaps() {
		Sim s = sim();
		s.tick(t(0));
		s.tick(t(800));
		assertEquals(MISSED, s.state());
		s.act(BOARDED, t(810));
		assertEquals(RIDING, s.state());
		assertEquals(1, s.count(GuidanceEventType.MISSED));
	}

	@Test
	public void gpsShowingRideProgressRecoversMissedAndWait() {
		Sim s = sim();
		s.fix(loc(0), t(0));
		s.tick(t(100));
		s.fix(loc(1), t(790));
		assertEquals(RIDING, s.state());
		assertEquals(1, s.engine.getStopPosition());
		assertEquals(0, s.count(GuidanceEventType.MISSED));
		assertEquals(1, s.count(NEXT_STOP));
	}

	@Test
	public void offRouteWithFixOffersReplanButDoesNotReplan() {
		Sim s = sim();
		s.fix(ORIGIN, t(0));
		s.fix(east(ORIGIN, 600), t(10));
		assertEquals(WALK_TO_BOARD, s.state());
		s.fix(east(ORIGIN, 620), t(20));
		assertEquals(OFF_ROUTE, s.state());
		assertEquals(Arrays.asList("Walk to Stop 0", "You are off route. Replan from here?"), s.texts());
		s.ticks(t(30), t(100), 10);
		assertEquals(OFF_ROUTE, s.state());
		assertEquals(0, s.count(REPLAN_REQUESTED));
		assertEquals(1, s.count(OFF_ROUTE_OFFER));
		// hysteresis: 280 m is below the 300 m trigger but above the 250 m resume line
		s.fix(east(ORIGIN, 280), t(110));
		assertEquals(OFF_ROUTE, s.state());
		s.fix(east(ORIGIN, 240), t(120));
		assertEquals(WALK_TO_BOARD, s.state());
		assertEquals(1, s.count(ROUTE_RESUMED));
		assertEquals(1, s.count(OFF_ROUTE_OFFER));
	}

	@Test
	public void singleBadFixIsNotOffRoute() {
		Sim s = sim();
		s.fix(ORIGIN, t(0));
		s.fix(east(ORIGIN, 900), t(5));
		s.fix(north(ORIGIN, 30), t(10));
		s.fix(east(ORIGIN, 900), t(15));
		s.fix(north(ORIGIN, 40), t(20));
		assertEquals(WALK_TO_BOARD, s.state());
		assertEquals(0, s.count(OFF_ROUTE_OFFER));
	}

	@Test
	public void offRouteGoesAheadOnAckWithReplanFromLastFix() {
		Sim s = sim();
		s.fix(east(ORIGIN, 600), t(10));
		s.fix(east(ORIGIN, 650), t(20));
		assertEquals(OFF_ROUTE, s.state());
		s.act(REPLAN_ACK, t(30));
		assertEquals(CANCELLED, s.state());
		GuidanceEvent replan = s.of(REPLAN_REQUESTED).get(0);
		assertEquals(t(30), replan.getTimeSec());
		assertEquals(east(ORIGIN, 650), replan.getFrom());
		assertEquals(DEST, replan.getTo());
	}

	@Test
	public void noFixNeverGoesOffRouteNorReplansByItself() {
		Sim s = sim();
		s.tick(t(0));
		s.ticks(t(10), t(600), 10);
		s.act(BOARDED, t(602));
		for (long x = 610; x <= 1000; x += 10) {
			s.noFix(t(x));
		}
		assertEquals(0, s.count(OFF_ROUTE_OFFER));
		assertEquals(0, s.count(REPLAN_REQUESTED));
		assertTrue(s.states().stream().noneMatch(st -> st == OFF_ROUTE));
	}

	@Test
	public void poorAccuracyFixIsTreatedAsNoFix() {
		Sim s = sim();
		s.fix(ORIGIN, t(0));
		s.fix(east(ORIGIN, 900), 400f, t(5));
		s.fix(east(ORIGIN, 900), 400f, t(10));
		assertEquals(WALK_TO_BOARD, s.state());
		s.fix(loc(0), 150f, t(15));
		assertEquals(WALK_TO_BOARD, s.state());
	}

	@Test
	public void wrongDirectionWhileRidingIsOffRoute() {
		Sim s = sim();
		s.fix(loc(0), t(0));
		s.act(BOARDED, t(601));
		s.fix(north(loc(1), 1500), t(700));
		s.fix(north(loc(1), 1600), t(710));
		assertEquals(OFF_ROUTE, s.state());
		assertEquals(0, s.count(REPLAN_REQUESTED));
		s.fix(loc(1), t(790));
		assertEquals(RIDING, s.state());
		assertEquals(1, s.count(ROUTE_RESUMED));
	}

	@Test
	public void withoutStopLocationsRidingIsNeverOffRouteAndUsesSchedule() {
		Sim s = new Sim(new GtfsGuidanceEngine(standard(T0), namesWithoutLocations()));
		s.fix(loc(0), t(0));
		s.act(BOARDED, t(601));
		s.fix(north(loc(1), 3000), t(700));
		s.fix(north(loc(1), 3000), t(710));
		assertEquals(RIDING, s.state());
		s.fix(loc(1), t(790));
		assertEquals(1, s.engine.getStopPosition());
	}

	@Test
	public void skippedLegs() {
		Sim s = sim();
		s.tick(t(0));
		s.act(SKIP_LEG, t(5));
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		assertEquals(1, s.count(LEG_SKIPPED));
		s.act(SKIP_LEG, t(10));
		assertEquals(TRANSFER_WALK, s.state());
		assertEquals(0, s.count(TRANSFER_INSTRUCTION));
		s.act(SKIP_LEG, t(15));
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		s.act(SKIP_LEG, t(20));
		assertEquals(WALK_TO_DEST, s.state());
		s.act(SKIP_LEG, t(25));
		assertEquals(ARRIVED, s.state());
		assertEquals(5, s.count(LEG_SKIPPED));
		assertTrue(s.engine.isFinished());
	}

	@Test
	public void skippingWhileRidingGetsOffAndTransfers() {
		Sim s = sim();
		s.tick(t(0));
		s.act(BOARDED, t(10));
		assertEquals(RIDING, s.state());
		s.act(SKIP_LEG, t(20));
		assertEquals(TRANSFER_WALK, s.state());
		assertEquals(1, s.count(TRANSFER_INSTRUCTION));
	}

	@Test
	public void boardedTapWhileWalkingJumpsToRidingWithoutBoardPrompt() {
		Sim s = sim();
		s.tick(t(0));
		s.act(BOARDED, t(30));
		assertEquals(RIDING, s.state());
		assertEquals(1, s.engine.getLegIndex());
		assertEquals(0, s.count(BOARD_PROMPT));
		assertEquals(1, s.count(WALK_LEG_ENDED));
	}

	@Test
	public void alightedTapIsIgnoredWhileWalking() {
		Sim s = sim();
		s.tick(t(0));
		s.act(ALIGHTED, t(30));
		assertEquals(WALK_TO_BOARD, s.state());
		assertTrue(s.last.isEmpty());
	}

	@Test
	public void cancelEndsGuidanceAndIgnoresLaterInput() {
		Sim s = sim();
		s.tick(t(0));
		s.act(BOARDED, t(30));
		s.act(CANCEL, t(40));
		assertEquals(CANCELLED, s.state());
		assertEquals(Arrays.asList("cancel"), s.of(GuidanceEventType.CANCELLED).get(0).getArgs());
		int n = s.log.size();
		s.tick(t(100)).fix(loc(3), t(110)).act(BOARDED, t(120)).act(REPLAN_ACK, t(130));
		assertEquals(n, s.log.size());
		assertEquals(CANCELLED, s.state());
	}

	@Test
	public void cancelBeforeStart() {
		Sim s = sim();
		s.act(CANCEL, t(0));
		assertEquals(CANCELLED, s.state());
		assertEquals(PLANNED, s.of(STATE_CHANGED).get(0).getPreviousState());
	}

	@Test
	public void arrivalIsTerminal() {
		Sim s = happyPath();
		assertEquals(ARRIVED, s.state());
		int n = s.log.size();
		s.tick(t(9999)).fix(ORIGIN, t(10000)).act(CANCEL, t(10001)).act(BOARDED, t(10002));
		assertEquals(n, s.log.size());
		assertEquals(ARRIVED, s.state());
		assertEquals(1, s.count(GuidanceEventType.ARRIVED));
	}

	@Test
	public void rideOnlyItineraryArrivesAtAlight() {
		Sim s = sim(singleRide(T0));
		s.fix(loc(0), t(300));
		s.act(BOARDED, t(301));
		s.fix(loc(5), t(900));
		assertEquals(ARRIVED, s.state());
		assertEquals(5, s.of(GuidanceEventType.ARRIVED).get(0).getStopIdx());
	}

	@Test
	public void delaysShiftPromptsAndMissedDeadline() {
		DelayProvider late = new DelayProvider() {
			@Override
			public int departureDelaySec(int tripIdx, int stopSeq) {
				return 300;
			}

			@Override
			public int arrivalDelaySec(int tripIdx, int stopSeq) {
				return 300;
			}
		};
		Sim s = new Sim(new GtfsGuidanceEngine(standard(T0), names(), late, GuidanceConfig.defaults()));
		s.tick(t(0));
		s.ticks(t(10), t(1020), 10);
		assertEquals("Board the Q toward Uptown at Stop 0, departs 08:15", s.texts().get(1));
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		s.tick(t(1021));
		assertEquals(MISSED, s.state());
		assertEquals(t(900), s.of(GuidanceEventType.MISSED).get(0).getTimeSec());

		Sim s2 = new Sim(new GtfsGuidanceEngine(standard(T0), names(), late, GuidanceConfig.defaults()));
		s2.tick(t(0));
		s2.act(BOARDED, t(905));
		s2.tick(t(1079));
		assertEquals(0, s2.count(NEXT_STOP));
		s2.tick(t(1080));
		assertEquals(1, s2.count(NEXT_STOP));
		// delayed arrival at 1410: assumed alighted only 60 s later
		s2.tick(t(1469));
		assertEquals(RIDING, s2.state());
		s2.tick(t(1470));
		assertEquals(TRANSFER_WALK, s2.state());
		// the transfer walk lasts its scheduled 180 s from when it actually began, so it is not over at once
		s2.tick(t(1470 + 180 + 59));
		assertEquals(TRANSFER_WALK, s2.state());
		s2.tick(t(1470 + 180 + 60));
		assertEquals(WAIT_FOR_VEHICLE, s2.state());
	}

	@Test
	public void repeatedInputsProduceNoDuplicatePrompts() {
		Sim s = sim();
		s.fix(ORIGIN, t(0));
		for (int i = 0; i < 20; i++) {
			s.fix(north(loc(0), -30), t(170));
		}
		assertEquals(1, s.count(BOARD_PROMPT));
		for (int i = 0; i < 20; i++) {
			s.tick(t(200));
			assertTrue(s.last.isEmpty());
		}
		s.act(BOARDED, t(602));
		for (int i = 0; i < 5; i++) {
			s.act(BOARDED, t(602));
			assertTrue(s.last.isEmpty());
		}
		s.fix(loc(1), t(790));
		for (int i = 0; i < 10; i++) {
			s.fix(loc(1), t(790));
			s.tick(t(790));
			assertTrue(s.last.isEmpty());
		}
		assertEquals(1, s.count(NEXT_STOP));
		assertEquals(1, s.count(RIDE_STARTED));
	}

	@Test
	public void clockThatGoesBackwardsIsClamped() {
		Sim s = sim();
		s.tick(t(1000));
		assertEquals(MISSED, s.state());
		s.tick(t(0));
		assertEquals(MISSED, s.state());
		assertEquals(t(1000), s.log.get(s.log.size() - 1).getEpochSec());
	}

	@Test
	public void deterministicAndGaplessOrdering() {
		Sim a = happyPath(), b = happyPath();
		assertEquals(a.log.toString(), b.log.toString());
		for (int i = 0; i < a.log.size(); i++) {
			assertEquals(i + 1, a.log.get(i).getSeq());
			if (i > 0) {
				assertTrue(a.log.get(i).getEpochSec() >= a.log.get(i - 1).getEpochSec());
			}
		}
		// within one call: state change precedes the prompt it causes
		int board = indexOf(a, BOARD_PROMPT);
		assertEquals(STATE_CHANGED, a.log.get(board - 1).getType());
		assertEquals(WAIT_FOR_VEHICLE, a.log.get(board - 1).getState());
	}

	private static int indexOf(Sim s, GuidanceEventType type) {
		for (int i = 0; i < s.log.size(); i++) {
			if (s.log.get(i).getType() == type) {
				return i;
			}
		}
		throw new AssertionError(type);
	}

	@Test
	public void gpsJitterAtBoardingRadiusFiresOnce() {
		Sim s = sim();
		s.fix(ORIGIN, t(0));
		for (int i = 1; i <= 10; i++) {
			s.fix(north(loc(0), -55), t(i * 5));
		}
		assertEquals(WALK_TO_BOARD, s.state());
		for (int i = 11; i <= 40; i++) {
			s.fix(north(loc(0), i % 2 == 0 ? -45 : -55), t(i * 5));
		}
		assertEquals(WAIT_FOR_VEHICLE, s.state());
		assertEquals(1, s.count(BOARD_PROMPT));
		assertEquals(1, s.states().stream().filter(x -> x == WAIT_FOR_VEHICLE).count());
		assertEquals(1, s.count(WALK_LEG_ENDED));
	}

	@Test
	public void gpsJitterAtOffRouteThresholdDoesNotFlap() {
		Sim s = sim();
		s.fix(ORIGIN, t(0));
		for (int i = 1; i <= 30; i++) {
			s.fix(east(ORIGIN, i % 2 == 0 ? 290 : 320), t(i));
		}
		// alternating around 300 m never gives two consecutive off-path fixes
		assertEquals(WALK_TO_BOARD, s.state());
		s.fix(east(ORIGIN, 320), t(40));
		s.fix(east(ORIGIN, 330), t(41));
		assertEquals(OFF_ROUTE, s.state());
		for (int i = 0; i < 20; i++) {
			s.fix(east(ORIGIN, i % 2 == 0 ? 270 : 310), t(50 + i));
		}
		assertEquals(OFF_ROUTE, s.state());
		assertEquals(1, s.count(OFF_ROUTE_OFFER));
		assertEquals(0, s.count(ROUTE_RESUMED));
	}

	@Test
	public void itineraryCrossingMidnight() {
		Sim s = new Sim(new GtfsGuidanceEngine(standard(T_LATE), names()));
		long b = T_LATE - T0;
		s.tick(t(b));
		s.ticks(t(b + 10), t(b + 600), 10);
		s.act(BOARDED, t(b + 602));
		s.ticks(t(b + 610), t(b + 1500), 10);
		s.act(BOARDED, t(b + 1502));
		s.ticks(t(b + 1510), t(b + 2400), 10);
		assertEquals(ARRIVED, s.state());
		List<String> texts = s.texts();
		assertTrue(texts.toString(), texts.contains("Board the Q toward Uptown at Stop 0, departs 00:00"));
		assertTrue(texts.toString(), texts.contains("Transfer to the F at Stop 10, departs 00:15"));
		assertTrue(texts.toString(), texts.contains("Board the F at Stop 10, departs 00:15"));
		for (int i = 1; i < s.log.size(); i++) {
			assertTrue(s.log.get(i).getEpochSec() >= s.log.get(i - 1).getEpochSec());
		}
		assertEquals(0, s.count(GuidanceEventType.MISSED));
	}

	@Test
	public void formatterUsesFeedZone() {
		GuidanceEvent e = new Sim(new GtfsGuidanceEngine(standard(T_LATE), names())).tick(T_LATE).of(WALK_LEG_STARTED).get(0);
		assertEquals("Walk to Stop 0", new EnglishGuidanceFormatter(GuidanceTestItineraries.NY).format(e));
		assertFalse(e.getArgs().isEmpty());
	}
}
