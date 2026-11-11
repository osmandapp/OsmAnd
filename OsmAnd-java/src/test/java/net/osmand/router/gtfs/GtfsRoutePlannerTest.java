package net.osmand.router.gtfs;

import static net.osmand.router.gtfs.GtfsFixture.FRI;
import static net.osmand.router.gtfs.GtfsFixture.SAT;
import static net.osmand.router.gtfs.GtfsFixture.SUN;
import static net.osmand.router.gtfs.GtfsFixture.TUE;
import static net.osmand.router.gtfs.GtfsFixture.at;
import static net.osmand.router.gtfs.GtfsFixture.epoch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.data.LatLon;

public class GtfsRoutePlannerTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private GtfsIndex ix;

	private GtfsRoutePlanner planner(GtfsFixture f) throws IOException {
		File out = new File(tmp.newFolder(), "index.bin");
		new GtfsFeedImporter(null).importFeed(f.write(tmp.newFolder(), "feed.zip"), out);
		ix = GtfsIndex.open(out, true);
		return new GtfsRoutePlanner(ix);
	}

	private List<GtfsItinerary> plan(GtfsRoutePlanner p, char from, char to, LocalDate date, String time) {
		return p.plan(at(from), at(to), epoch(date, time), null, null);
	}

	private static List<GtfsLeg> rides(GtfsItinerary it) {
		List<GtfsLeg> res = new ArrayList<>();
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				res.add(l);
			}
		}
		return res;
	}

	private String trip(GtfsLeg ride) {
		return ix.getTripHeadsign(ride.tripIdx).substring(3);
	}

	private GtfsFixture twoLines() {
		return GtfsFixture.basic()
				.trip("t1", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("t2", "R2", "WD", "B 08:15:00", "C 08:25:00", "D 08:35:00")
				.trip("t3", "R2", "WD", "B 08:45:00", "C 08:55:00", "D 09:05:00");
	}

	// ------------------------------------------------------------------ basics

	@Test
	public void twoLinesAndATransfer() throws IOException {
		List<GtfsItinerary> r = plan(planner(twoLines()), 'A', 'D', TUE, "07:55");
		GtfsItinerary it = r.get(0);
		assertEquals(1, it.transfers);
		assertEquals(epoch(TUE, "08:00"), it.departSec);
		assertEquals(epoch(TUE, "08:35"), it.arriveSec);
		List<GtfsLeg> rides = rides(it);
		assertEquals(2, rides.size());
		assertEquals("t1", trip(rides.get(0)));
		assertEquals("L1", ix.getRouteShortName(rides.get(0).routeIdx));
		assertEquals("t2", trip(rides.get(1)));
		assertEquals(0, rides.get(0).fromStop);
		assertEquals(1, rides.get(0).toStop);
		assertEquals(epoch(TUE, "08:15"), rides.get(1).startSec);
		assertEquals(3, rides.get(1).stops.length); // B, C, D
		assertEquals(epoch(TUE, "08:25"), rides.get(1).arrivalSec[1]);
		assertEquals(0, rides.get(1).boardSeq);
		assertEquals(2, rides.get(1).alightSeq);
		assertEquals(2, it.legs.size()); // zero-length walks are not reported
	}

	@Test
	public void departureAfterTheLastTripFindsNothing() throws IOException {
		assertTrue(plan(planner(twoLines()), 'A', 'D', TUE, "09:30").isEmpty());
	}

	@Test
	public void missedTransferTakesTheNextDeparture() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("t1", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("t2", "R2", "WD", "B 08:11:00", "C 08:21:00") // 60 s after arrival: too tight for 120 s
				.trip("t3", "R2", "WD", "B 08:30:00", "C 08:40:00");
		GtfsRoutePlanner p = planner(f);
		assertEquals(epoch(TUE, "08:40"), plan(p, 'A', 'C', TUE, "07:55").get(0).arriveSec);
		GtfsPlanOptions o = new GtfsPlanOptions();
		o.transferBufferSec = 30;
		assertEquals(epoch(TUE, "08:21"), p.plan(at('A'), at('C'), epoch(TUE, "07:55"), o, null).get(0).arriveSec);
	}

	@Test
	public void noRoute() throws IOException {
		GtfsFixture f = GtfsFixture.basic().trip("t1", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("t2", "R2", "WD", "C 08:00:00", "D 08:10:00");
		GtfsRoutePlanner p = planner(f);
		assertTrue(plan(p, 'A', 'D', TUE, "07:55").isEmpty());
		assertTrue(plan(p, 'B', 'A', TUE, "07:55").isEmpty()); // against the direction
		assertTrue(p.plan(at('A'), new LatLon(41.5, -74.0), epoch(TUE, "07:55"), null, null).isEmpty()); // nothing near
	}

	@Test
	public void sameStop() throws IOException {
		GtfsRoutePlanner p = planner(twoLines());
		assertTrue(plan(p, 'B', 'B', TUE, "08:00").isEmpty());
		assertTrue(p.plan(at('B'), new LatLon(at('B').getLatitude() + 0.0005, at('B').getLongitude()), epoch(TUE, "08:00"),
				null, null).isEmpty());
	}

	@Test
	public void walkingToAndFromTheStops() throws IOException {
		LatLon from = new LatLon(at('A').getLatitude(), at('A').getLongitude() - 0.003); // about 250 m west of A
		LatLon to = new LatLon(at('B').getLatitude(), at('B').getLongitude() + 0.003);
		GtfsRoutePlanner p = planner(twoLines());
		GtfsItinerary it = p.plan(from, to, epoch(TUE, "07:50"), null, null).get(0);
		assertEquals(GtfsLeg.Type.WALK, it.legs.get(0).type);
		assertEquals(-1, it.legs.get(0).fromStop);
		assertEquals(0, it.legs.get(0).toStop);
		assertEquals(253, it.legs.get(0).walkMeters, 5);
		assertEquals("leaves as late as possible", it.legs.get(1).startSec, it.legs.get(0).endSec);
		assertEquals(263, it.legs.get(0).endSec - it.legs.get(0).startSec, 5);
		GtfsLeg last = it.legs.get(it.legs.size() - 1);
		assertEquals(GtfsLeg.Type.WALK, last.type);
		assertEquals(-1, last.toStop);
		assertEquals(it.arriveSec, last.endSec);
		assertEquals(epoch(TUE, "08:10") + last.endSec - last.startSec, it.arriveSec);
	}

	// ------------------------------------------------------------------ calendar

	@Test
	public void weekdayAndSundayTimetables() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("wd", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("su", "R1", "SU", "A 09:00:00", "B 09:10:00");
		GtfsRoutePlanner p = planner(f);
		assertEquals(epoch(TUE, "08:10"), plan(p, 'A', 'B', TUE, "07:00").get(0).arriveSec);
		assertEquals(epoch(SUN, "09:10"), plan(p, 'A', 'B', SUN, "07:00").get(0).arriveSec);
		assertTrue(plan(p, 'A', 'B', SAT, "07:00").isEmpty());
	}

	@Test
	public void calendarDatesAddAndRemoveService() throws IOException {
		GtfsFixture f = GtfsFixture.basic().trip("wd", "R1", "WD", "A 08:00:00", "B 08:10:00");
		f.file("calendar_dates.txt", "service_id,date,exception_type", "WD,20261126,2", "WD,20261121,1");
		GtfsRoutePlanner p = planner(f);
		assertEquals(1, plan(p, 'A', 'B', LocalDate.of(2026, 11, 25), "07:00").size()); // Wednesday as usual
		assertTrue(plan(p, 'A', 'B', LocalDate.of(2026, 11, 26), "07:00").isEmpty()); // Thanksgiving: removed
		assertEquals(1, plan(p, 'A', 'B', SAT, "07:00").size()); // a Saturday with weekday service
		assertTrue(plan(p, 'A', 'B', SUN, "07:00").isEmpty());
	}

	@Test
	public void calendarDatesWithoutCalendarTxt() throws IOException {
		GtfsFixture f = GtfsFixture.basic().trip("x", "R1", "ONLY", "A 08:00:00", "B 08:10:00");
		f.files.remove("calendar.txt");
		f.file("calendar_dates.txt", "service_id,date,exception_type", "ONLY,20261117,1");
		GtfsRoutePlanner p = planner(f);
		assertEquals(1, plan(p, 'A', 'B', TUE, "07:00").size());
		assertTrue(plan(p, 'A', 'B', LocalDate.of(2026, 11, 18), "07:00").isEmpty());
	}

	@Test
	public void timesPastMidnightAndTheThreeServiceDays() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("n1", "R1", "WD", "A 23:55:00", "B 24:20:00")
				.trip("n2", "R1", "WD", "A 24:05:00", "B 24:30:00")
				.trip("s1", "R2", "SA", "A 00:05:00", "B 00:10:00");
		GtfsRoutePlanner p = planner(f);
		// today's service day: leaves 23:55, arrives after midnight
		GtfsItinerary it = plan(p, 'A', 'B', TUE, "23:50").get(0);
		assertEquals("n1", trip(rides(it).get(0)));
		assertEquals(epoch(LocalDate.of(2026, 11, 18), "00:20"), it.arriveSec);
		// yesterday's service day: Tuesday's 24:05 is 00:05 on Wednesday
		it = plan(p, 'A', 'B', LocalDate.of(2026, 11, 18), "00:00").get(0);
		assertEquals("n2", trip(rides(it).get(0)));
		assertEquals(epoch(LocalDate.of(2026, 11, 18), "00:05"), it.departSec);
		assertEquals(epoch(LocalDate.of(2026, 11, 18), "00:30"), it.arriveSec);
		// tomorrow's service day: at 23:50 on Friday the Saturday trip of 00:05 is the quickest
		it = plan(p, 'A', 'B', FRI, "23:50").get(0);
		assertEquals("s1", trip(rides(it).get(0)));
		assertEquals(epoch(SAT, "00:10"), it.arriveSec);
	}

	// ------------------------------------------------------------------ DST (America/New_York)

	private GtfsFixture dstFeed() {
		return GtfsFixture.basic()
				.trip("sun", "R1", "SU", "A 01:30:00", "B 01:50:00") // service times of a Sunday
				.trip("late", "R2", "SA", "A 24:10:00", "B 24:30:00") // Saturday's service, after midnight
				.trip("late25", "R2", "SA", "A 25:30:00", "B 25:50:00");
	}

	private GtfsItinerary planAt(GtfsRoutePlanner p, String utc) {
		List<GtfsItinerary> r = p.plan(at('A'), at('B'), Instant.parse(utc).getEpochSecond(), null, null);
		assertFalse(utc, r.isEmpty());
		return r.get(0);
	}

	@Test
	public void fallBackDay() throws IOException {
		// 2026-11-01: clocks go back at 02:00 EDT. Service time 00:00 of Sunday is 01:00 EDT = 05:00Z.
		GtfsRoutePlanner p = planner(dstFeed());
		GtfsItinerary it = planAt(p, "2026-11-01T06:00:00Z");
		assertEquals(Instant.parse("2026-11-01T06:30:00Z").getEpochSecond(), it.departSec); // 01:30 EST
		assertEquals(Instant.parse("2026-11-01T06:50:00Z").getEpochSecond(), it.arriveSec);
		// Saturday's 25:30 is 05:30Z (01:30 EDT, the first 01:30), found from Sunday 01:20 EDT through the
		// yesterday cursor, whose start is 25 hours before Sunday's
		it = planAt(p, "2026-11-01T05:20:00Z");
		assertEquals("late25", trip(rides(it).get(0)));
		assertEquals(Instant.parse("2026-11-01T05:30:00Z").getEpochSecond(), it.departSec);
		assertEquals(Instant.parse("2026-11-01T05:50:00Z").getEpochSecond(), it.arriveSec);
	}

	@Test
	public void springForwardDay() throws IOException {
		// 2027-03-14: clocks go forward at 02:00 EST. Service time 00:00 of Sunday is 23:00 EST = 04:00Z.
		GtfsRoutePlanner p = planner(dstFeed());
		GtfsItinerary it = planAt(p, "2027-03-14T05:00:00Z"); // 00:00 EST
		assertEquals("late", trip(rides(it).get(0))); // Saturday's 24:10 is 05:10Z, as Saturday's day has 23 hours left
		assertEquals(Instant.parse("2027-03-14T05:10:00Z").getEpochSecond(), it.departSec);
		assertEquals(Instant.parse("2027-03-14T05:30:00Z").getEpochSecond(), it.arriveSec);
		// Sunday's own 01:30 is 05:30Z
		it = planAt(p, "2027-03-14T05:15:00Z");
		assertEquals("sun", trip(rides(it).get(0)));
		assertEquals(Instant.parse("2027-03-14T05:30:00Z").getEpochSecond(), it.departSec);
		assertEquals(Instant.parse("2027-03-14T05:50:00Z").getEpochSecond(), it.arriveSec);
	}

	// ------------------------------------------------------------------ pickup and drop-off

	@Test
	public void pickupAndDropOffRestrictions() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("p", "R1", "WD", "A 08:00:00", "B 08:10:00 pickup_type=1", "C 08:20:00 drop_off_type=1", "D 08:30:00")
				.trip("q", "R2", "WD", "B 08:30:00", "D 08:50:00");
		GtfsRoutePlanner p = planner(f);
		// staying on board through stops with restrictions is fine
		GtfsItinerary it = plan(p, 'A', 'D', TUE, "07:50").get(0);
		assertEquals("p", trip(rides(it).get(0)));
		assertEquals(epoch(TUE, "08:30"), it.arriveSec);
		assertEquals(4, rides(it).get(0).stops.length);
		// no boarding of p at B: the later trip q is taken
		it = plan(p, 'B', 'D', TUE, "08:00").get(0);
		assertEquals("q", trip(rides(it).get(0)));
		assertEquals(epoch(TUE, "08:50"), it.arriveSec);
		// alighting at B is allowed, at C it is not, and nothing else serves C
		assertEquals(epoch(TUE, "08:10"), plan(p, 'A', 'B', TUE, "07:50").get(0).arriveSec);
		assertTrue(plan(p, 'A', 'C', TUE, "07:50").isEmpty());
	}

	// ------------------------------------------------------------------ transfers and footpaths

	private GtfsFixture stationFeed(int minTransfer) {
		GtfsFixture f = GtfsFixture.basic();
		f.file("stops.txt", "stop_id,stop_name,stop_lat,stop_lon,location_type,parent_station",
				"O,Origin,40.70,-74.00,,", "P1,Platform 1,40.70,-73.99,,P", "P2,Platform 2,40.70,-73.98,,P",
				"Z,Zed,40.70,-73.97,,", "P,Station,40.70,-73.985,1,");
		f.file("transfers.txt", "from_stop_id,to_stop_id,transfer_type,min_transfer_time", "P,P,2," + minTransfer);
		return f.trip("t1", "R1", "WD", "O 08:00:00", "P1 08:10:00")
				.trip("late", "R2", "WD", "P2 08:14:00", "Z 08:20:00")
				.trip("t2", "R2", "WD", "P2 08:16:00", "Z 08:30:00");
	}

	@Test
	public void stationTransfersApplyToTheChildStops() throws IOException {
		GtfsRoutePlanner p = planner(stationFeed(300));
		// the two platforms are 850 m apart, only the transfers.txt row connects them
		assertEquals(4, ix.getStopParent(1)); // the parent is the station P
		assertEquals(4, ix.getStopParent(2));
		assertTrue(ix.getFootpathCount() >= 2);
		GtfsLeg[] legs = plan(p, 'A', 'D', TUE, "07:55").get(0).legs.toArray(new GtfsLeg[0]);
		// 300 s are needed: arrival 08:10 + 300 s = 08:15 misses the 08:14 trip and catches the 08:16 one
		assertEquals(epoch(TUE, "08:30"), plan(p, 'A', 'D', TUE, "07:55").get(0).arriveSec);
		assertEquals(3, legs.length);
		assertEquals(GtfsLeg.Type.WALK, legs[1].type);
		assertEquals(300, legs[1].endSec - legs[1].startSec);
		assertEquals(1, legs[1].fromStop);
		assertEquals(2, legs[1].toStop);
		assertEquals("t2", trip(legs[2]));
	}

	@Test
	public void shorterMinimumTransferTimeCatchesTheEarlierTrip() throws IOException {
		GtfsRoutePlanner p = planner(stationFeed(120));
		GtfsItinerary it = plan(p, 'A', 'D', TUE, "07:55").get(0);
		assertEquals(epoch(TUE, "08:20"), it.arriveSec);
		assertEquals("late", trip(rides(it).get(1)));
	}

	@Test
	public void nearbyStopsAreWalkable() throws IOException {
		GtfsFixture f = GtfsFixture.basic();
		f.stop("X", "Near B", 40.70, -73.9875); // 211 m east of B
		f.stop("Y", "Far from B", 40.70, -73.983); // 591 m east of B
		f.trip("t1", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("tx", "R2", "WD", "X 08:20:00", "D 08:40:00")
				.trip("ty", "R2", "WD", "Y 08:20:00", "C 08:40:00");
		GtfsRoutePlanner p = planner(f);
		GtfsItinerary it = plan(p, 'A', 'D', TUE, "07:55").get(0);
		assertEquals(epoch(TUE, "08:40"), it.arriveSec);
		GtfsLeg walk = it.legs.get(1);
		assertEquals(GtfsLeg.Type.WALK, walk.type);
		assertEquals(211, walk.walkMeters, 5);
		assertEquals(Math.round(walk.walkMeters * 1.3 / 1.25), walk.endSec - walk.startSec);
		// 591 m is beyond the 400 m footpaths: the trip from Y is out of reach (C can still be walked to from X)
		for (GtfsItinerary other : plan(p, 'A', 'C', TUE, "07:55")) {
			for (GtfsLeg ride : rides(other)) {
				assertTrue(!"ty".equals(trip(ride)));
			}
		}
	}

	// ------------------------------------------------------------------ ties and alternatives

	@Test
	public void equalArrivalPrefersFewerTransfers() throws IOException {
		// From B the quickest way to D is trip tB, boarded at B at 08:10. Hopping to A first on "hop" (arrive
		// 08:04) and boarding tB there at 08:07 would arrive at the same time, with one more ride.
		GtfsFixture f = GtfsFixture.basic()
				.trip("hop", "R1", "WD", "B 08:02:00", "A 08:04:00")
				.trip("tB", "R2", "WD", "A 08:07:00", "B 08:10:00", "D 08:20:00");
		GtfsPlanOptions o = new GtfsPlanOptions();
		o.maxItineraries = 1; // the alternatives would find the direct ride anyway
		GtfsItinerary it = planner(f).plan(at('B'), at('D'), epoch(TUE, "08:00"), o, null).get(0);
		assertEquals(epoch(TUE, "08:20"), it.arriveSec);
		assertEquals(0, it.transfers);
		assertEquals(1, rides(it).size());
		assertEquals("tB", trip(rides(it).get(0)));
		assertEquals(1, rides(it).get(0).fromStop);
	}

	@Test
	public void alternativeWithoutTheFirstRoute() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("fast", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("slow", "R2", "WD", "A 08:02:00", "B 08:20:00");
		List<GtfsItinerary> r = plan(planner(f), 'A', 'B', TUE, "07:55");
		assertEquals(2, r.size());
		assertEquals("fast", trip(rides(r.get(0)).get(0)));
		assertEquals("slow", trip(rides(r.get(1)).get(0)));
		GtfsPlanOptions o = new GtfsPlanOptions();
		o.maxItineraries = 1;
		assertEquals(1, planner(f).plan(at('A'), at('B'), epoch(TUE, "07:55"), o, null).size());
		o.maxItineraries = 3;
		o.maxAlternativeDelaySec = 300; // 10 minutes later is too much
		assertEquals(1, planner(f).plan(at('A'), at('B'), epoch(TUE, "07:55"), o, null).size());
	}

	@Test
	public void alternativeWithFewerTransfers() throws IOException {
		// quickest: A -> B on r1, change to r2 at B (2 min buffer met); alternative: one direct, slower ride
		GtfsFixture f = GtfsFixture.basic()
				.trip("first", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("second", "R2", "WD", "B 08:13:00", "C 08:20:00")
				.trip("direct", "R2", "WD", "A 08:01:00", "C 08:35:00");
		List<GtfsItinerary> r = plan(planner(f), 'A', 'C', TUE, "07:55");
		assertEquals(2, r.size());
		assertEquals(1, r.get(0).transfers);
		assertEquals(epoch(TUE, "08:20"), r.get(0).arriveSec);
		assertEquals(0, r.get(1).transfers);
		assertEquals(epoch(TUE, "08:35"), r.get(1).arriveSec);
	}

	@Test
	public void cancelledSearchReturnsNothing() throws IOException {
		GtfsRoutePlanner p = planner(twoLines());
		GtfsProgressListener cancel = new GtfsProgressListener() {
			@Override
			public void onProgress(String stage, int percent) {
			}

			@Override
			public boolean isCancelled() {
				return true;
			}
		};
		assertTrue(p.plan(at('A'), at('D'), epoch(TUE, "07:55"), null, cancel).isEmpty());
		assertNotNull(DelayProvider.NONE);
		assertEquals(0, DelayProvider.NONE.arrivalDelaySec(3, 4));
	}
}
