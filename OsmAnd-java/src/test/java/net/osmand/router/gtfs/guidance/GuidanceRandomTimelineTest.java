package net.osmand.router.gtfs.guidance;

import static net.osmand.router.gtfs.GuidanceTestItineraries.DEST;
import static net.osmand.router.gtfs.GuidanceTestItineraries.ORIGIN;
import static net.osmand.router.gtfs.GuidanceTestItineraries.T0;
import static net.osmand.router.gtfs.GuidanceTestItineraries.east;
import static net.osmand.router.gtfs.GuidanceTestItineraries.loc;
import static net.osmand.router.gtfs.GuidanceTestItineraries.names;
import static net.osmand.router.gtfs.GuidanceTestItineraries.north;
import static net.osmand.router.gtfs.GuidanceTestItineraries.standard;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.GtfsLeg;

/** Seeded random event streams against invariants that must hold for any input order. */
public class GuidanceRandomTimelineTest {

	private static final Set<GuidanceEventType> ONCE = EnumSet.of(GuidanceEventType.BOARD_PROMPT,
			GuidanceEventType.TRANSFER_INSTRUCTION, GuidanceEventType.RIDE_STARTED, GuidanceEventType.NEXT_STOP,
			GuidanceEventType.GET_OFF_NEXT, GuidanceEventType.MISSED, GuidanceEventType.ARRIVED,
			GuidanceEventType.WALK_LEG_STARTED, GuidanceEventType.WALK_LEG_ENDED, GuidanceEventType.LEG_CHANGED,
			GuidanceEventType.RIDE_ENDED);

	private static LatLon randomPoint(Random r) {
		switch (r.nextInt(6)) {
			case 0:
				return loc(new int[] { 0, 1, 2, 3, 10, 11, 12, 13 }[r.nextInt(8)]);
			case 1:
				return r.nextBoolean() ? ORIGIN : DEST;
			case 2:
				return north(loc(r.nextInt(4)), r.nextInt(200) - 100);
			case 3:
				return east(loc(10 + r.nextInt(4)), r.nextInt(200) - 100);
			case 4:
				return north(east(ORIGIN, r.nextInt(2000) - 500), r.nextInt(1500) - 500);
			default:
				return north(loc(r.nextInt(4)), 400 + r.nextInt(3000));
		}
	}

	private static int rank(GuidanceState s) {
		switch (s) {
			case WAIT_FOR_VEHICLE:
				return 1;
			case RIDING:
				return 2;
			default:
				return 0;
		}
	}

	private void run(long seed, boolean allowFixes, GtfsItinerary it) {
		Random r = new Random(seed);
		GtfsGuidanceEngine e = new GtfsGuidanceEngine(it, names());
		UserAction[] actions = UserAction.values();
		long now = T0 - r.nextInt(300);
		long lastKey = -1;
		boolean everFix = false, sawTerminal = false;
		GuidanceState terminal = null;
		long lastSeq = 0, lastEpoch = Long.MIN_VALUE;
		Set<String> seen = new HashSet<>();
		GuidanceState chain = GuidanceState.PLANNED;
		int steps = 40 + r.nextInt(260);
		for (int i = 0; i < steps; i++) {
			now += r.nextInt(10) == 0 ? r.nextInt(900) : 1 + r.nextInt(60);
			List<GuidanceEvent> out;
			int kind = r.nextInt(20);
			if (kind < 8) {
				out = e.onTick(now);
			} else if (kind < 16) {
				LatLon p = randomPoint(r);
				boolean fix = allowFixes && r.nextInt(5) != 0;
				float acc = r.nextInt(8) == 0 ? 150 : 5 + r.nextInt(30);
				if (fix && acc <= 100) {
					everFix = true;
				}
				out = e.onLocation(p.getLatitude(), p.getLongitude(), acc, now, fix);
			} else {
				UserAction a = actions[r.nextInt(actions.length)];
				if (a == UserAction.CANCEL && r.nextInt(4) != 0) {
					a = UserAction.BOARDED;
				}
				out = e.onUserAction(a, now);
			}
			String where = "seed " + seed + " step " + i + " state " + e.getState();
			if (terminal != null) {
				assertTrue("terminal state must not emit: " + where, out.isEmpty());
				assertEquals(where, terminal, e.getState());
				continue;
			}
			for (GuidanceEvent ev : out) {
				assertEquals(where, ++lastSeq, ev.getSeq());
				assertTrue(where, ev.getEpochSec() >= lastEpoch);
				lastEpoch = ev.getEpochSec();
				if (ev.getType() == GuidanceEventType.STATE_CHANGED) {
					assertEquals("state chain: " + where, chain, ev.getPreviousState());
					chain = ev.getState();
				}
				if (ONCE.contains(ev.getType())) {
					String key = ev.getType() + ":" + ev.getLegIndex() + ":" + ev.getStopPosition();
					assertTrue("duplicate prompt " + key + " " + where, seen.add(key));
				}
				if (ev.getType() == GuidanceEventType.OFF_ROUTE_OFFER) {
					assertTrue("off-route without a fix: " + where, everFix);
				}
				if (ev.getType() == GuidanceEventType.REPLAN_REQUESTED) {
					assertEquals(where, ev.getEpochSec(), ev.getTimeSec());
				}
			}
			assertEquals("engine state matches the last STATE_CHANGED: " + where, chain, e.getState());
			GuidanceState s = e.getState();
			if (s == GuidanceState.OFF_ROUTE) {
				assertTrue(where, everFix);
			}
			if (s != GuidanceState.OFF_ROUTE && s != GuidanceState.MISSED && !s.isTerminal() && s != GuidanceState.PLANNED) {
				long key = e.getLegIndex() * 10L + rank(s);
				assertTrue("state regressed (" + key + " < " + lastKey + "): " + where, key >= lastKey);
				lastKey = key;
			}
			if (s == GuidanceState.MISSED) {
				assertTrue(where, e.getLegIndex() * 10L + 1 >= lastKey);
				assertEquals(where, GtfsLeg.Type.RIDE, it.legs.get(e.getLegIndex()).type);
			}
			if (s.isTerminal()) {
				terminal = s;
				sawTerminal = true;
			}
		}
		if (sawTerminal && terminal == GuidanceState.ARRIVED) {
			assertEquals(it.legs.size() - 1, e.getLegIndex());
		}
	}

	@Test
	public void randomTimelinesWithFixes() {
		for (long seed = 1; seed <= 600; seed++) {
			run(seed, true, standard(T0));
		}
	}

	@Test
	public void randomTimelinesWithoutAnyFix() {
		for (long seed = 1000; seed <= 1300; seed++) {
			run(seed, false, standard(T0));
		}
	}

	@Test
	public void randomTimelinesAreReproducible() {
		// same seed twice must give identical engine behaviour: compare via final state of two runs
		for (long seed = 5; seed < 25; seed++) {
			Random a = new Random(seed), b = new Random(seed);
			GtfsGuidanceEngine ea = new GtfsGuidanceEngine(standard(T0), names()),
					eb = new GtfsGuidanceEngine(standard(T0), names());
			StringBuilder la = new StringBuilder(), lb = new StringBuilder();
			long ta = T0, tb = T0;
			for (int i = 0; i < 80; i++) {
				ta += 1 + a.nextInt(100);
				tb += 1 + b.nextInt(100);
				LatLon pa = randomPoint(a), pb = randomPoint(b);
				la.append(ea.onLocation(pa.getLatitude(), pa.getLongitude(), 8f, ta, true));
				lb.append(eb.onLocation(pb.getLatitude(), pb.getLongitude(), 8f, tb, true));
			}
			assertEquals(la.toString(), lb.toString());
			assertFalse(la.length() == 0);
		}
	}

	@Test
	public void cooperativeUserReachesArrivalFromAnySchedulePhase() {
		// ticks plus correct taps always end in ARRIVED, never in MISSED, however coarse the ticks are
		for (int step = 1; step <= 55; step += 6) {
			GtfsGuidanceEngine e = new GtfsGuidanceEngine(standard(T0), names());
			boolean boarded1 = false, boarded2 = false;
			for (long t = T0; t < T0 + 3000 && !e.isFinished(); t += step) {
				e.onTick(t);
				if (!boarded1 && t >= T0 + 600 && e.getLegIndex() == 1) {
					e.onUserAction(UserAction.BOARDED, t);
					boarded1 = true;
				}
				if (!boarded2 && t >= T0 + 1500 && e.getLegIndex() == 3 && e.getState() != GuidanceState.TRANSFER_WALK) {
					e.onUserAction(UserAction.BOARDED, t);
					boarded2 = true;
				}
			}
			assertEquals("step " + step, GuidanceState.ARRIVED, e.getState());
		}
	}

}
