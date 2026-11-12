package net.osmand.router.gtfs.selftest;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.osmand.data.LatLon;
import net.osmand.router.TransportClockFormat;
import net.osmand.router.gtfs.GtfsIndex;
import net.osmand.router.gtfs.GtfsIndexSource;
import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.GtfsLeg;
import net.osmand.router.gtfs.GtfsPlanOptions;
import net.osmand.router.gtfs.guidance.EnglishGuidanceFormatter;
import net.osmand.router.gtfs.guidance.GtfsGuidanceEngine;
import net.osmand.router.gtfs.guidance.GuidanceConfig;
import net.osmand.router.gtfs.guidance.GuidanceEvent;
import net.osmand.router.gtfs.guidance.GuidanceEventType;
import net.osmand.router.gtfs.guidance.GuidanceNames;
import net.osmand.router.gtfs.guidance.GuidanceState;
import net.osmand.router.gtfs.guidance.UserAction;

/**
 * Drives GtfsGuidanceEngine with real itineraries from the index and a synthetic timeline on a fake clock:
 * the simulated seconds are just numbers passed to the engine, nothing sleeps and no location service is used.
 * Variants: schedule only (no fixes at all), fixes on walks only (subway rides without fixes), fixes all the
 * way, a missed first vehicle, and a detour while walking.
 */
final class GuidanceSimScenario {

	private static final int STEP_SEC = 5;

	private GuidanceSimScenario() {
	}

	private enum Variant {
		TICKS_ONLY("ticks-only (no GPS at all, taps)", false, false, true, true),
		GPS_WALKS("gps-on-walks (no fixes on rides, taps)", true, false, true, true),
		GPS_ALL("gps-everywhere (taps)", true, true, true, true),
		/** Boarding and alighting by speed and location only, no taps: auto-detection must succeed. */
		GPS_NO_TAP("gps-everywhere-no-taps (auto-detect)", true, true, false, true),
		MISSED("missed-first-vehicle", true, false, false, true),
		OFF_ROUTE("off-route-walk (700 m detour)", true, false, true, true);

		final String label;
		final boolean walkFixes, rideFixes, tap, strict;

		Variant(String label, boolean walkFixes, boolean rideFixes, boolean tap, boolean strict) {
			this.label = label;
			this.walkFixes = walkFixes;
			this.rideFixes = rideFixes;
			this.tap = tap;
			this.strict = strict;
		}
	}

	static GuidanceNames names(GtfsIndex index) {
		return new GuidanceNames() {
			@Override
			public String stopName(int stopIdx) {
				return stopIdx < 0 ? null : index.getStopName(stopIdx);
			}

			@Override
			public LatLon stopLocation(int stopIdx) {
				return stopIdx < 0 ? null : new LatLon(index.getStopLat(stopIdx), index.getStopLon(stopIdx));
			}

			@Override
			public String routeName(GtfsLeg ride) {
				String s = ride.routeIdx < 0 ? null : index.getRouteShortName(ride.routeIdx);
				return s == null || s.isEmpty() ? GuidanceNames.super.routeName(ride) : s;
			}

			@Override
			public String headsign(GtfsLeg ride) {
				String s = ride.tripIdx < 0 ? null : index.getTripHeadsign(ride.tripIdx);
				return s == null || s.isEmpty() ? null : s;
			}
		};
	}

	static void run(SelfTestEnv env, ScenarioResult r) throws Exception {
		File idx = env.ensureIndex();
		if (idx == null) {
			r.skip("no index and no zips");
			return;
		}
		GtfsIndexSource source = new GtfsIndexSource(idx);
		GtfsIndexSource.Loaded loaded = source.get();
		r.expect(loaded != null, "cannot open index " + idx);
		if (loaded == null) {
			return;
		}
		GtfsIndex index = loaded.index;
		ZoneId zone = index.getZone();
		LocalDate from = LocalDate.ofEpochDay(index.getValidFrom()), to = LocalDate.ofEpochDay(index.getValidTo());
		LocalDate today = Instant.ofEpochSecond(env.nowSec.getAsLong()).atZone(zone).toLocalDate();
		LocalDate day = today.plusDays(1);
		if (day.isBefore(from) || day.isAfter(to)) {
			day = from.plusDays(Math.min(1, Math.max(0, to.toEpochDay() - from.toEpochDay())));
		}
		long depart = SelfTestUtil.epoch(day, LocalTime.of(8, 0), zone);
		EnglishGuidanceFormatter fmt = new EnglishGuidanceFormatter(zone);
		TransportClockFormat clock = new TransportClockFormat(true, Locale.US);
		GuidanceNames names = names(index);
		List<Map<String, Object>> runs = new ArrayList<>();
		int itinerariesDriven = 0, variantsRun = 0;

		for (SelfTestPlaces.Od od : SelfTestPlaces.guidance()) {
			GtfsPlanOptions opts = new GtfsPlanOptions();
			opts.maxItineraries = 1;
			List<GtfsItinerary> plan;
			synchronized (loaded) {
				plan = loaded.planner.plan(od.from, od.to, depart, opts, null);
			}
			if (plan.isEmpty()) {
				r.fail(od.name + ": no itinerary to drive the engine with");
				continue;
			}
			GtfsItinerary it = plan.get(0);
			itinerariesDriven++;
			for (Variant v : Variant.values()) {
				Map<String, Object> run = new LinkedHashMap<>();
				run.put("od", od.name);
				run.put("variant", v.label);
				run.put("itinerary", summary(it, index, clock, zone));
				String where = od.name + " / " + v.label;
				boolean ran = simulate(r, where, it, v, names, fmt, run, zone);
				if (ran) {
					variantsRun++;
				}
				runs.add(run);
			}
		}
		r.put("itinerariesDriven", itinerariesDriven);
		r.put("variantsRun", variantsRun);
		r.put("runs", runs);
		r.expect(itinerariesDriven > 0, "no itinerary driven");
	}

	private static List<String> summary(GtfsItinerary it, GtfsIndex index, TransportClockFormat clock, ZoneId zone) {
		List<String> l = new ArrayList<>();
		for (GtfsLeg leg : it.legs) {
			String span = clock.format(leg.startSec, zone) + "-" + clock.format(leg.endSec, zone);
			l.add(leg.type == GtfsLeg.Type.WALK ? "WALK " + leg.walkMeters + " m " + span
					: "RIDE " + index.getRouteShortName(leg.routeIdx) + " " + index.getStopName(leg.fromStop) + " -> "
					+ index.getStopName(leg.toStop) + " " + span);
		}
		return l;
	}

	/** Where the traveller is at time t on the plan, or null when no fix is available there. */
	private static LatLon position(GtfsItinerary it, long t, boolean walkFixes, boolean rideFixes, GuidanceNames names) {
		List<GtfsLeg> legs = it.legs;
		LatLon last = legs.get(0).from;
		for (GtfsLeg leg : legs) {
			if (t < leg.startSec) {
				return walkFixes ? last : null;
			}
			if (t <= leg.endSec) {
				if (leg.type == GtfsLeg.Type.WALK) {
					if (!walkFixes) {
						return null;
					}
					double f = leg.endSec == leg.startSec ? 1 : (double) (t - leg.startSec) / (leg.endSec - leg.startSec);
					return new LatLon(leg.from.getLatitude() + f * (leg.to.getLatitude() - leg.from.getLatitude()),
							leg.from.getLongitude() + f * (leg.to.getLongitude() - leg.from.getLongitude()));
				}
				return rideFixes ? onRide(leg, t, names) : null;
			}
			last = leg.to;
		}
		return walkFixes ? legs.get(legs.size() - 1).to : null;
	}

	private static LatLon stopAt(GtfsLeg ride, int k, GuidanceNames names) {
		if (k == 0 && ride.from != null) {
			return ride.from;
		}
		if (k == ride.stops.length - 1 && ride.to != null) {
			return ride.to;
		}
		return names.stopLocation(ride.stops[k]);
	}

	/** On a ride the traveller moves from stop to stop between the scheduled departure and the next arrival. */
	private static LatLon onRide(GtfsLeg ride, long t, GuidanceNames names) {
		int n = ride.stops.length;
		for (int k = 0; k + 1 < n; k++) {
			long dep = ride.departureSec[k], arr = ride.arrivalSec[k + 1];
			LatLon a = stopAt(ride, k, names), b = stopAt(ride, k + 1, names);
			if (t <= dep) {
				return a;
			}
			if (t <= arr) {
				double f = arr == dep ? 1 : (double) (t - dep) / (arr - dep);
				return new LatLon(a.getLatitude() + f * (b.getLatitude() - a.getLatitude()),
						a.getLongitude() + f * (b.getLongitude() - a.getLongitude()));
			}
		}
		return ride.to;
	}

	private static LatLon north(LatLon p, double meters) {
		return new LatLon(p.getLatitude() + meters / 111195.0, p.getLongitude());
	}

	private static boolean simulate(ScenarioResult r, String where, GtfsItinerary it, Variant v, GuidanceNames names,
	                                EnglishGuidanceFormatter fmt, Map<String, Object> run, ZoneId zone) {
		GtfsGuidanceEngine engine = new GtfsGuidanceEngine(it, names, net.osmand.router.gtfs.DelayProvider.NONE,
				GuidanceConfig.defaults());
		long delay = 0;
		long detourFrom = -1, detourTo = -1;
		GtfsLeg firstRide = null, firstWalk = null, detourWalk = null;
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.RIDE && firstRide == null) {
				firstRide = l;
			}
			if (l.type == GtfsLeg.Type.WALK && firstWalk == null) {
				firstWalk = l;
			}
			if (l.type == GtfsLeg.Type.WALK && (detourWalk == null || l.endSec - l.startSec > detourWalk.endSec - detourWalk.startSec)) {
				detourWalk = l;
			}
		}
		if (v == Variant.MISSED) {
			// the traveller sets off only after the vehicle has left and the grace period is over
			long setOff = firstRide.departureSec[0] + GuidanceConfig.defaults().graceSec + 90;
			long walkStart = firstWalk != null && firstWalk.endSec <= firstRide.departureSec[0] ? firstWalk.startSec
					: firstRide.departureSec[0];
			delay = Math.max(0, setOff - walkStart);
		}
		if (v == Variant.OFF_ROUTE) {
			if (detourWalk == null || detourWalk.endSec - detourWalk.startSec < 120) {
				run.put("skipped", "no walk leg of 2 minutes or more");
				r.note(where + ": SKIP (no suitable walk leg)");
				return false;
			}
			long d = detourWalk.endSec - detourWalk.startSec;
			detourFrom = detourWalk.startSec + d * 3 / 10;
			detourTo = detourWalk.startSec + d * 6 / 10;
		}

		List<GuidanceEvent> all = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		long t0 = it.departSec - 60;
		long end = it.arriveSec + 900 + delay;
		boolean replanSent = false;
		int taps = 0;
		collect(all, lines, engine.start(t0), fmt, it.departSec);
		int guard = 0;
		for (long t = t0 + STEP_SEC; t <= end && !engine.isFinished() && guard++ < 100000; t += STEP_SEC) {
			LatLon p = position(it, t - delay, v.walkFixes, v.rideFixes, names);
			if (p != null && v == Variant.OFF_ROUTE && t >= detourFrom && t <= detourTo) {
				p = north(p, 700);
			}
			List<GuidanceEvent> ev;
			if (p != null) {
				ev = engine.onLocation(p.getLatitude(), p.getLongitude(), 5f, t, true);
			} else {
				ev = engine.onTick(t);
			}
			collect(all, lines, ev, fmt, it.departSec);
			// no fixes on the ride: the traveller taps "I'm on board" shortly after the vehicle leaves
			if (v.tap && engine.getState() == GuidanceState.WAIT_FOR_VEHICLE
					&& engine.getLegIndex() < it.legs.size()
					&& t >= it.legs.get(engine.getLegIndex()).departureSec[0] + 15) {
				collect(all, lines, engine.onUserAction(UserAction.BOARDED, t), fmt, it.departSec);
				taps++;
			}
			if (v == Variant.MISSED && !replanSent && engine.getState() == GuidanceState.MISSED) {
				replanSent = true;
				collect(all, lines, engine.onUserAction(UserAction.REPLAN_ACK, t + 1), fmt, it.departSec);
			}
		}
		run.put("boardedTaps", taps);
		run.put("finalState", engine.getState().name());
		run.put("events", all.size());
		run.put("timeline", lines);

		int boards = count(all, GuidanceEventType.BOARD_PROMPT), rideStarts = count(all, GuidanceEventType.RIDE_STARTED);
		int arrived = count(all, GuidanceEventType.ARRIVED), missed = count(all, GuidanceEventType.MISSED);
		int offs = count(all, GuidanceEventType.OFF_ROUTE_OFFER), resumed = count(all, GuidanceEventType.ROUTE_RESUMED);
		int rides = 0;
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				rides++;
			}
		}
		run.put("counts", "boardPrompts=" + boards + " rideStarted=" + rideStarts + " arrived=" + arrived + " missed=" + missed
				+ " offRoute=" + offs + " resumed=" + resumed + " rides=" + rides);

		// events are totally ordered and never speak "null"
		long seq = all.isEmpty() ? 0 : all.get(0).getSeq() - 1;
		for (GuidanceEvent e : all) {
			r.expect(e.getSeq() == seq + 1, where + ": event sequence gap at #" + e.getSeq());
			seq = e.getSeq();
			String text = fmt.format(e);
			if (e.getTextKey() != null) {
				r.expect(text != null && !text.isEmpty() && !text.contains("null"), where + ": bad text for " + e + ": " + text);
			}
		}
		if (!v.strict) {
			String verdict = engine.getState() == GuidanceState.ARRIVED ? "arrived by location alone"
					: "ended in " + engine.getState() + " (missed=" + missed + ", offRoute=" + offs + ") without an 'on board' tap";
			run.put("finding", verdict);
			r.note(where + ": " + verdict);
		} else if (v == Variant.MISSED) {
			r.expect(missed >= 1, where + ": missed vehicle not detected");
			r.expect(count(all, GuidanceEventType.REPLAN_REQUESTED) >= 1, where + ": no replan request");
			r.expect(engine.getState() == GuidanceState.CANCELLED, where + ": final state " + engine.getState());
			r.expect(arrived == 0, where + ": arrived despite missing the vehicle");
		} else {
			r.expect(engine.getState() == GuidanceState.ARRIVED && arrived == 1, where + ": final state " + engine.getState()
					+ ", ARRIVED events " + arrived);
			r.expect(missed == 0, where + ": false MISSED");
			r.expect(rideStarts == rides, where + ": RIDE_STARTED " + rideStarts + " for " + rides + " rides");
			if (v == Variant.OFF_ROUTE) {
				r.expect(offs >= 1, where + ": detour not detected");
				r.expect(resumed >= 1, where + ": return to the route not detected");
			} else {
				r.expect(offs == 0, where + ": false OFF_ROUTE");
				r.expect(boards >= rides, where + ": " + boards + " board prompts for " + rides + " rides");
			}
			for (GuidanceEvent e : all) {
				if (e.getType() == GuidanceEventType.ARRIVED) {
					r.expect(e.getEpochSec() >= it.arriveSec - 120 && e.getEpochSec() <= it.arriveSec + 600,
							where + ": ARRIVED " + (e.getEpochSec() - it.arriveSec) + " s from the planned arrival");
				}
			}
		}
		// the first board prompt names the planned departure clock time
		if (firstRide != null && v != Variant.MISSED) {
			String want = "departs " + new TransportClockFormat(true, Locale.US).format(firstRide.departureSec[0], zone);
			boolean found = false;
			for (GuidanceEvent e : all) {
				if (e.getType() == GuidanceEventType.BOARD_PROMPT) {
					String text = fmt.format(e);
					found = text != null && text.contains(want);
					break;
				}
			}
			r.expect(found, where + ": first board prompt does not contain '" + want + "'");
		}
		return true;
	}

	private static int count(List<GuidanceEvent> all, GuidanceEventType type) {
		int n = 0;
		for (GuidanceEvent e : all) {
			if (e.getType() == type) {
				n++;
			}
		}
		return n;
	}

	private static void collect(List<GuidanceEvent> all, List<String> lines, List<GuidanceEvent> events,
	                            EnglishGuidanceFormatter fmt, long base) {
		for (GuidanceEvent e : events) {
			all.add(e);
			long rel = e.getEpochSec() - base;
			String t = (rel < 0 ? "-" : "+") + String.format(Locale.ROOT, "%02d:%02d", Math.abs(rel) / 60, Math.abs(rel) % 60);
			String text = fmt.format(e);
			lines.add(t + " #" + e.getSeq() + " " + e.getType() + (e.getType() == GuidanceEventType.STATE_CHANGED
					? " " + e.getPreviousState() + "->" + e.getState() : "") + (text == null ? "" : "  | " + text));
		}
	}
}
