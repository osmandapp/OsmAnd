package net.osmand.router.gtfs.selftest;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import net.osmand.router.TransportClockFormat;
import net.osmand.router.TransportRouteProviders;
import net.osmand.router.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.gtfs.GtfsIndex;
import net.osmand.router.gtfs.GtfsIndexSource;
import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.GtfsLeg;
import net.osmand.router.gtfs.GtfsTransitRouteProvider;
import net.osmand.router.gtfs.GtfsTransportResultAdapter;

/**
 * Plans fixed trips at several departure times through the provider path (TransportRouteProviders, then
 * GtfsTransitRouteProvider, then the result adapter), the same classes TransportRoutingHelper calls.
 */
final class PlanMatrixScenario {

	private static final Pattern HHMM = Pattern.compile("\\d\\d:\\d\\d");

	private PlanMatrixScenario() {
	}

	/** A departure time of the matrix. */
	static final class When {
		final String label;
		final long epochSec;
		final boolean inService;
		/** False for odd hours where a bus-only pair may have no service at all; the outcome is reported only. */
		boolean mandatory = true;

		When(String label, long epochSec, boolean inService) {
			this.label = label;
			this.epochSec = epochSec;
			this.inService = inService;
		}
	}

	/** Departure times relative to the index: now, tomorrow 08:00, tonight 23:50, a DST day, after the end. */
	static List<When> times(SelfTestEnv env, GtfsIndex index, ScenarioResult r) {
		ZoneId zone = index.getZone();
		LocalDate from = LocalDate.ofEpochDay(index.getValidFrom()), to = LocalDate.ofEpochDay(index.getValidTo());
		long now = env.nowSec.getAsLong();
		LocalDate today = Instant.ofEpochSecond(now).atZone(zone).toLocalDate();
		List<When> res = new ArrayList<>();
		res.add(new When("now", now, !today.isBefore(from) && !today.isAfter(to)));
		LocalDate tomorrow = clamp(today.plusDays(1), from, to);
		res.add(new When("tomorrow 08:00" + (tomorrow.equals(today.plusDays(1)) ? "" : " (clamped to " + tomorrow + ")"),
				SelfTestUtil.epoch(tomorrow, LocalTime.of(8, 0), zone), true));
		LocalDate night = clamp(today, from, to.minusDays(1));
		res.add(new When("23:50 " + night + " (crosses midnight)", SelfTestUtil.epoch(night, LocalTime.of(23, 50), zone), true));
		// a DST change inside the validity range, if any
		ZoneOffsetTransition tr = zone.getRules().nextTransition(Instant.ofEpochSecond(
				SelfTestUtil.epoch(from, LocalTime.MIDNIGHT, zone)).minusSeconds(1));
		if (tr != null) {
			LocalDate d = tr.getDateTimeBefore().toLocalDate();
			if (!d.isBefore(from) && !d.isAfter(to)) {
				r.note("DST change in the feed's range: " + tr);
				res.add(new When("DST day " + d + " 08:00", SelfTestUtil.epoch(d, LocalTime.of(8, 0), zone), true));
				When odd = new When("DST day " + d + " 01:30", SelfTestUtil.epoch(d, LocalTime.of(1, 30), zone), true);
				odd.mandatory = false;
				res.add(odd);
			} else {
				r.note("the next DST change (" + d + ") is outside the feed's range " + from + ".." + to + ": DST day not tested");
			}
		} else {
			r.note("zone " + zone + " has no DST change after " + from);
		}
		res.add(new When("after the feed ends (" + to.plusDays(30) + " 08:00)",
				SelfTestUtil.epoch(to.plusDays(30), LocalTime.of(8, 0), zone), false));
		return res;
	}

	private static LocalDate clamp(LocalDate d, LocalDate lo, LocalDate hi) {
		if (d.isBefore(lo)) {
			return lo;
		}
		return d.isAfter(hi) ? hi : d;
	}

	static void run(SelfTestEnv host, ScenarioResult r) throws Exception {
		File idx = host.ensureIndex();
		if (idx == null) {
			r.skip("no index and no zips");
			return;
		}
		GtfsIndexSource source = new GtfsIndexSource(idx);
		GtfsIndexSource.Loaded loaded = source.get();
		r.expect(loaded != null, "cannot open index " + idx + ": " + source.getLastError());
		if (loaded == null) {
			return;
		}
		GtfsIndex index = loaded.index;
		ZoneId zone = index.getZone();
		TransportClockFormat clock = new TransportClockFormat(true, Locale.US);
		TransportRouteProviders providers = new TransportRouteProviders(host.nowSec);
		providers.add(new GtfsTransitRouteProvider(source, () -> 800));
		List<When> whens = times(host, index, r);
		r.put("indexFile", idx.getPath());
		r.put("zone", zone.getId());
		r.put("validFrom", LocalDate.ofEpochDay(index.getValidFrom()).toString());
		r.put("validTo", LocalDate.ofEpochDay(index.getValidTo()).toString());

		List<Map<String, Object>> rows = new ArrayList<>();
		int routed = 0, nulls = 0;
		long maxMs = 0;
		for (SelfTestPlaces.Od od : SelfTestPlaces.all()) {
			for (When w : whens) {
				if (!od.expectRoute && !w.label.startsWith("tomorrow")) {
					continue; // coverage rows need one time only
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("od", od.name);
				row.put("when", w.label);
				row.put("departRequested", clock.format(w.epochSec, zone) + " " + Instant.ofEpochSecond(w.epochSec).atZone(zone).toLocalDate());
				Map<String, Long> memBefore = memory(host);
				long t0 = System.nanoTime();
				List<TransportRouteResult> res = providers.calculate(od.from, od.to, w.epochSec, new HashMap<String, String>(),
						null, null);
				long ms = (System.nanoTime() - t0) / 1_000_000;
				maxMs = Math.max(maxMs, ms);
				row.put("queryMs", ms);
				row.put("memoryBefore", memBefore);
				row.put("memoryAfter", memory(host));
				row.put("results", res == null ? 0 : res.size());
				boolean expectRow = od.expectRoute && w.inService && w.mandatory;
				if (res == null) {
					nulls++;
					row.put("outcome", "null (stock planner would run)");
					if (expectRow) {
						r.fail(od.name + " @ " + w.label + ": no route");
					}
				} else {
					routed++;
					if (!od.expectRoute) {
						r.fail(od.name + " @ " + w.label + ": a route was returned outside the coverage");
					}
					if (!w.inService) {
						r.note(od.name + " @ " + w.label + ": a route was returned after the feed ends");
					}
					List<Map<String, Object>> its = new ArrayList<>();
					for (TransportRouteResult tr : res) {
						its.add(describe(r, od, w, tr, clock, index));
					}
					row.put("itineraries", its);
				}
				rows.add(row);
			}
		}
		r.put("rows", rows);
		r.put("rowCount", rows.size());
		r.put("routed", routed);
		r.put("nullAnswers", nulls);
		r.put("maxQueryMs", maxMs);
		r.expect(routed > 0, "no route at all");
	}

	static Map<String, Long> memory(SelfTestEnv host) {
		Runtime rt = Runtime.getRuntime();
		Map<String, Long> m = new LinkedHashMap<>();
		m.put("javaUsedMb", (rt.totalMemory() - rt.freeMemory()) >> 20);
		m.put("javaMaxMb", rt.maxMemory() >> 20);
		if (host.memoryProbe != null) {
			for (Map.Entry<String, Long> e : host.memoryProbe.snapshot().entrySet()) {
				m.put(e.getKey(), e.getValue() < 0 ? e.getValue() : e.getValue() >> 20);
			}
		}
		return m;
	}

	private static Map<String, Object> describe(ScenarioResult r, SelfTestPlaces.Od od, When w, TransportRouteResult tr,
	                                            TransportClockFormat clock, GtfsIndex index) {
		Map<String, Object> m = new LinkedHashMap<>();
		String where = od.name + " @ " + w.label;
		if (!(tr instanceof GtfsTransportResultAdapter.GtfsResult)) {
			r.fail(where + ": result is " + tr.getClass().getName());
			return m;
		}
		GtfsTransportResultAdapter.GtfsResult g = (GtfsTransportResultAdapter.GtfsResult) tr;
		GtfsItinerary it = g.itinerary;
		ZoneId zone = g.getTimeZone();
		m.put("depart", clock.format(it.departSec, zone));
		m.put("arrive", clock.format(it.arriveSec, zone));
		m.put("arriveDate", Instant.ofEpochSecond(it.arriveSec).atZone(zone).toLocalDate().toString());
		m.put("durationMin", (it.arriveSec - it.departSec) / 60);
		m.put("transfers", it.transfers);
		m.put("walkMetersTotal", walkTotal(it));
		m.put("walkMetersAdjustedTotal", it.walkMetersAdjusted);
		m.put("durationFromRequestMin", it.durationFromRequestSec() / 60);
		m.put("inVehicleMin", it.inVehicleSec / 60);
		boolean crosses = !Instant.ofEpochSecond(it.arriveSec).atZone(zone).toLocalDate()
				.equals(Instant.ofEpochSecond(it.departSec).atZone(zone).toLocalDate());
		m.put("crossesMidnight", crosses);
		List<String> legs = new ArrayList<>();
		int rides = 0;
		for (GtfsLeg leg : it.legs) {
			String span = clock.format(leg.startSec, zone) + "-" + clock.format(leg.endSec, zone);
			if (leg.type == GtfsLeg.Type.WALK) {
				legs.add("WALK " + leg.walkMeters + " m " + span);
			} else {
				rides++;
				legs.add("RIDE " + index.getRouteShortName(leg.routeIdx) + " " + index.getStopName(leg.fromStop) + " -> "
						+ index.getStopName(leg.toStop) + " " + span + " (" + (leg.stops.length - 1) + " stops)");
			}
		}
		m.put("legs", legs);

		// invariants of the result and of the adapter
		r.expect(HHMM.matcher((String) m.get("depart")).matches() && HHMM.matcher((String) m.get("arrive")).matches(),
				where + ": clock text not HH:mm");
		r.expect(it.departSec >= w.epochSec - 60, where + ": departs " + (w.epochSec - it.departSec) + " s before the request");
		r.expect(it.arriveSec > it.departSec, where + ": arrival not after departure");
		r.expect(it.arriveSec - w.epochSec < 8 * 3600, where + ": arrives more than 8 h after the request");
		r.expect(rides > 0, where + ": no ride leg");
		r.expect(tr.getSegments().size() == rides, where + ": adapter segments " + tr.getSegments().size() + " vs rides " + rides);
		r.expect(g.getDepartureTimeSec() == it.departSec && g.getArrivalTimeSec() == it.arriveSec, where + ": result times differ");
		long prev = it.departSec;
		for (TransportRouteResultSegment s : tr.getSegments()) {
			if (s instanceof GtfsTransportResultAdapter.GtfsSegment) {
				GtfsTransportResultAdapter.GtfsSegment gs = (GtfsTransportResultAdapter.GtfsSegment) s;
				r.expect(gs.boardSec >= prev && gs.alightSec > gs.boardSec, where + ": segment times out of order");
				prev = gs.alightSec;
			} else {
				r.fail(where + ": segment is not a GtfsSegment");
			}
		}
		return m;
	}

	private static int walkTotal(GtfsItinerary it) {
		int d = 0;
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.WALK) {
				d += l.walkMeters;
			}
		}
		return d;
	}
}
