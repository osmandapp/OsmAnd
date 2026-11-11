package net.osmand.router.gtfs;

import static net.osmand.router.gtfs.GtfsFixture.TUE;
import static net.osmand.router.gtfs.GtfsFixture.at;
import static net.osmand.router.gtfs.GtfsFixture.epoch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Way;
import net.osmand.router.GeneralRouter;
import net.osmand.router.TransportRoutePlanner;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRouteResult;
import net.osmand.router.TransportRoutingConfiguration;
import net.osmand.router.gtfs.GtfsTransportResultAdapter.GtfsResult;
import net.osmand.router.gtfs.GtfsTransportResultAdapter.GtfsRoute;
import net.osmand.router.gtfs.GtfsTransportResultAdapter.GtfsSegment;
import net.osmand.shared.routing.GeneralRouterProfile;

/** Port of the 48 checks of the A3 spike, plus round trips through the real planner and edge cases. */
public class GtfsTransportResultAdapterTest {

	/** Types the UI's TransportStopType.findType recognises (typeName.toUpperCase() against the enum). */
	private static final Set<String> UI_TYPES = new HashSet<>(Arrays.asList("bus", "ferry", "funicular", "light_rail",
			"monorail", "railway", "share_taxi", "train", "tram", "trolleybus", "subway"));

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private GtfsIndex ix;
	private TransportRoutingConfiguration cfg;
	private GtfsTransportResultAdapter adapter;
	private final long day = epoch(TUE, "00:00");

	/**
	 * t1: A..E on R1 (subway, with route_color); the walk E to F is about 330 m; t2: F,G,H on R2 (bus);
	 * t3 starts at D (same-stop transfer from t1) and loops D C D.
	 */
	@Before
	public void setUp() throws IOException {
		GtfsFixture f = GtfsFixture.basic();
		f.stop("E", "Stop E", 40.70, -73.96);
		f.stop("F", "Stop F", 40.703, -73.96);
		f.stop("G", "Stop G", 40.703, -73.95);
		f.stop("H", "Stop H", 40.703, -73.94);
		f.trip("t1", "R1", "WD", "A 08:10:00", "B 08:12:00", "C 08:15:00", "D 08:18:00", "E 08:20:00");
		f.trip("t2", "R2", "WD", "F 08:28:00", "G 08:30:00", "H 08:33:00");
		f.trip("t3", "R2", "WD", "D 08:22:00", "C 08:25:00", "D 08:28:00");
		File out = new File(tmp.newFolder(), "index.bin");
		new GtfsFeedImporter(null).importFeed(f.write(tmp.newFolder(), "feed.zip"), out);
		ix = GtfsIndex.open(out, true);
		GeneralRouter gr = new GeneralRouter(GeneralRouterProfile.PUBLIC_TRANSPORT, new HashMap<String, String>());
		cfg = new TransportRoutingConfiguration(gr, new HashMap<String, String>());
		adapter = new GtfsTransportResultAdapter(ix, cfg);
	}

	// ---------------------------------------------------------------- helpers

	private int stop(char c) {
		for (int i = 0; i < ix.getStopCount(); i++) {
			if (ix.getStopName(i).equals("Stop " + c)) {
				return i;
			}
		}
		throw new AssertionError("no stop " + c);
	}

	private int trip(String id) {
		for (int i = 0; i < ix.getTripCount(); i++) {
			if (ix.getTripHeadsign(i).equals("To " + id)) {
				return i;
			}
		}
		throw new AssertionError("no trip " + id);
	}

	private GtfsLeg ride(String tripId, int from, int to) {
		int t = trip(tripId);
		int n = to - from + 1;
		int[] stops = new int[n];
		long[] arr = new long[n], dep = new long[n];
		for (int i = 0; i < n; i++) {
			stops[i] = ix.getTripStop(t, from + i);
			arr[i] = day + ix.getTripArrival(t, from + i);
			dep[i] = day + ix.getTripDeparture(t, from + i);
		}
		return GtfsLeg.ride(t, ix.getTripRoute(t), from, to, stops, arr, dep, null, null, null, null);
	}

	private GtfsLeg walk(long start, long end, int meters) {
		return GtfsLeg.walk(start, end, -1, -1, at('A'), at('B'), meters);
	}

	private GtfsItinerary itin(long dep, long arr, GtfsLeg... legs) {
		int rides = 0;
		for (GtfsLeg l : legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				rides++;
			}
		}
		return new GtfsItinerary(new ArrayList<>(Arrays.asList(legs)), dep, arr, Math.max(0, rides - 1));
	}

	/** walk 300, ride t1 A..E (5 stops), walk 450, ride t2 F..H (3 stops), walk 250. */
	private GtfsItinerary twoRides() {
		long h8 = day + 8 * 3600;
		return itin(h8, h8 + 36 * 60, walk(h8, h8 + 300, 300), ride("t1", 0, 4), walk(h8 + 20 * 60, h8 + 27 * 60, 450),
				ride("t2", 0, 2), walk(h8 + 33 * 60, h8 + 36 * 60, 250));
	}

	// ---------------------------------------------------------------- ported checks: counts and times

	@Test
	public void twoRidesCountsAndWalks() {
		GtfsResult res = adapter.convert(twoRides());
		List<TransportRouteResultSegment> segs = res.getSegments();
		assertEquals(2, segs.size());
		assertEquals(1, res.getChanges());
		assertEquals((5 - 1) + (3 - 1), res.getStops());
		assertEquals(1000, res.getWalkDist(), 1e-6);
		assertEquals(250, res.getFinishWalkDist(), 0);
		assertEquals(300, segs.get(0).walkDist, 0);
		assertEquals(450, segs.get(1).walkDist, 0);
		assertEquals(2160, res.getRouteTime(), 0);
		double wt = res.getWalkTime();
		assertEquals(1000 / cfg.walkSpeed, wt, 1e-6);
		assertTrue(wt > 0 && !Double.isInfinite(wt) && !Double.isNaN(wt));
		assertEquals(300 / cfg.walkSpeed, segs.get(0).walkTime, 1e-6);
		assertEquals(res.getRouteTime() - wt, res.getTravelTime(), 1e-6);
		assertEquals(2160, res.getTravelTime() + wt, 1e-6);
		double td = res.getTravelDist();
		assertTrue("travel dist " + td, td > 4000 && td < 8000);
		assertEquals(td, res.getSegments().get(0).getTravelDist() + res.getSegments().get(1).getTravelDist(), 1);
		assertEquals(res.departSec, day + 8 * 3600);
		assertEquals(res.arriveSec, day + 8 * 3600 + 36 * 60);
	}

	@Test
	public void stockTravelTimeNeedsARouterAndIgnoresWaits() {
		GtfsResult res = adapter.convert(twoRides());
		TransportRoutingConfiguration nullRouter = new TransportRoutingConfiguration(null, new HashMap<String, String>());
		TransportRouteResult stockNull = new TransportRouteResult(nullRouter);
		for (TransportRouteResultSegment s : res.getSegments()) {
			stockNull.addSegment(s);
		}
		try {
			stockNull.getTravelTime();
			fail("the stock implementation needs a router in the config");
		} catch (NullPointerException expected) {
			// the reason GtfsResult overrides getTravelTime
		}
		TransportRouteResult stock = new TransportRouteResult(cfg);
		for (TransportRouteResultSegment s : res.getSegments()) {
			stock.addSegment(s);
		}
		assertTrue(Math.abs(stock.getTravelTime() - res.getTravelTime()) > 1);
		// the adapter works with a router-less config because it never asks the router
		GtfsResult noRouter = new GtfsTransportResultAdapter(ix, null).convert(twoRides());
		assertEquals(2160, noRouter.getTravelTime() + noRouter.getWalkTime(), 1e-6);
	}

	@Test
	public void changeTimeIsTheRealWait() {
		GtfsResult res = adapter.convert(twoRides());
		// t1 arrives 08:20, t2 departs 08:28
		assertEquals(8 * 60, res.getChangeTime(res.getSegments().get(0), res.getSegments().get(1)));
		assertEquals(0, res.getChangeTime(res.getSegments().get(0), null));
		assertEquals(0, res.getChangeTime(res.getSegments().get(1), res.getSegments().get(0)));
		assertEquals(0, res.getChangeTime(new TransportRouteResultSegment(), new TransportRouteResultSegment()));
	}

	// ---------------------------------------------------------------- ported checks: segment getters

	@Test
	public void segmentGettersTheUiReads() {
		GtfsResult res = adapter.convert(twoRides());
		GtfsSegment s0 = (GtfsSegment) res.getSegments().get(0);
		GtfsSegment s1 = (GtfsSegment) res.getSegments().get(1);
		assertEquals("Stop A", s0.getStart().getName("", false));
		assertEquals("Stop E", s0.getEnd().getName("", false));
		assertEquals("Stop A", s0.getStart().getName("en", false));
		assertEquals(0, s0.start);
		assertEquals(4, s0.end);
		assertEquals(5, s0.getTravelStops().size());
		assertEquals(3, s0.getTravelStops().subList(1, 4).size());
		assertEquals(3, s1.getTravelStops().size());
		assertEquals(1, s1.getTravelStops().subList(1, 2).size());
		assertEquals(-1, s0.getArrivalTime());
		assertNull(s0.route.getSchedule());
		assertTrue(s0.getTravelDist() > 0);
		assertEquals(day + 8 * 3600 + 10 * 60, s0.boardSec);
		assertEquals(10 * 60, s0.travelTime, 0);
		assertEquals(s0.getTravelDist(), s0.travelDistApproximate, 1);
		assertFalse(s0.route.hasInterval());
		assertNull(s0.route.getInterval());
		assertEquals("L1", s0.route.getAdjustedRouteRef(true));
		assertEquals("L2", s1.route.getAdjustedRouteRef(true));
		assertNotNull(s0.getSegmentRect());
		assertNotNull(s1.getSegmentRect());
	}

	@Test
	public void departureTimeIsTenSecondUnitsSinceLocalMidnight() {
		GtfsResult res = adapter.convert(twoRides());
		GtfsSegment s0 = (GtfsSegment) res.getSegments().get(0);
		assertEquals("08:10:00", TransportRoutePlanner.formatTransportTime(s0.depTime).trim());
		assertEquals(day + 8 * 3600 + 10 * 60, s0.boardSec);
		assertEquals(day + 8 * 3600 + 20 * 60, s0.alightSec);
		// a trip after midnight of the service day keeps counting past 24:00
		long late = 17 * 3600;
		GtfsLeg r = ride("t1", 0, 4);
		GtfsLeg shifted = GtfsLeg.ride(r.tripIdx, r.routeIdx, r.boardSeq, r.alightSeq, r.stops, plus(r.arrivalSec, late),
				plus(r.departureSec, late), null, null, null, null);
		GtfsResult lateRes = adapter.convert(itin(day + 8 * 3600, day + late + 8 * 3600 + 1200, shifted));
		assertEquals("25:10:00", TransportRoutePlanner.formatTransportTime(((GtfsSegment) lateRes.getSegments().get(0)).depTime).trim());
	}

	private static long[] plus(long[] a, long d) {
		long[] r = new long[a.length];
		for (int i = 0; i < a.length; i++) {
			r[i] = a[i] + d;
		}
		return r;
	}

	// ---------------------------------------------------------------- crash traps

	@Test
	public void routeFieldsTheUiDereferencesAreNeverNull() {
		for (TransportRouteResultSegment s : adapter.convert(twoRides()).getSegments()) {
			TransportRoute r = s.route;
			assertNotNull(r);
			assertNotNull(r.getType());
			assertTrue(UI_TYPES.contains(r.getType()));
			assertNotNull(r.getRef());
			assertFalse(r.getRef().isEmpty());
			assertTrue(r.getRef().equals(r.getRef()));
			assertNotNull(r.getName());
			assertNotNull(r.getId());
			assertNull(r.getColor());
			assertFalse(r.getForwardStops().isEmpty());
			int idx = -1;
			for (int i = 0; i < r.getForwardStops().size(); i++) {
				TransportStop st = r.getForwardStops().get(i);
				assertNotNull(st);
				assertNotNull(st.getId());
				assertNotNull(st.getLocation());
				if (idx < 0 && s.getStart().getId().equals(st.getId())) {
					idx = i;
				}
			}
			assertEquals(0, idx);
			assertEquals(1, r.getForwardWays().size());
		}
	}

	@Test
	public void routeTypeAndRefMapping() {
		GtfsResult res = adapter.convert(twoRides());
		assertEquals("subway", res.getSegments().get(0).route.getType());
		assertEquals("bus", res.getSegments().get(1).route.getType());
		assertEquals("L1", res.getSegments().get(0).route.getRef());
		assertEquals("To t1", res.getSegments().get(0).route.getName());
		GtfsRoute gr = (GtfsRoute) res.getSegments().get(0).route;
		assertEquals(0xFF0000, gr.colorRgb);
		assertEquals(GtfsIndex.NO_COLOR, ((GtfsRoute) res.getSegments().get(1).route).colorRgb);
		int[] all = { -1, 0, 1, 2, 3, 4, 5, 6, 7, 11, 12, 100, 109, 200, 400, 405, 700, 800, 900, 1000, 1100, 1700,
				Integer.MAX_VALUE, Integer.MIN_VALUE };
		for (int t : all) {
			String ty = GtfsTransportResultAdapter.mapType(t);
			assertNotNull(ty);
			assertTrue(t + " -> " + ty, UI_TYPES.contains(ty));
		}
		assertEquals("tram", GtfsTransportResultAdapter.mapType(0));
		assertEquals("subway", GtfsTransportResultAdapter.mapType(1));
		assertEquals("train", GtfsTransportResultAdapter.mapType(2));
		assertEquals("bus", GtfsTransportResultAdapter.mapType(3));
		assertEquals("ferry", GtfsTransportResultAdapter.mapType(4));
		assertEquals("train", GtfsTransportResultAdapter.mapType(101));
	}

	@Test
	public void unknownRouteAndTripStillGiveNonNullFields() {
		int t = trip("t1");
		GtfsLeg base = ride("t1", 0, 4);
		GtfsLeg orphan = GtfsLeg.ride(t, -1, 0, 4, base.stops, base.arrivalSec, base.departureSec, null, null, null, null);
		GtfsLeg noTrip = GtfsLeg.ride(-1, 9999, 0, 4, base.stops, base.arrivalSec, base.departureSec, null, null, null, null);
		for (GtfsLeg l : new GtfsLeg[] { orphan, noTrip }) {
			GtfsResult res = adapter.convert(itin(l.startSec, l.endSec, l));
			TransportRoute r = res.getSegments().get(0).route;
			assertEquals("bus", r.getType());
			assertNotNull(r.getRef());
			assertFalse(r.getRef().isEmpty());
			assertNotNull(r.getName());
		}
	}

	@Test
	public void idsAreUniqueNegativeAndStopsAreShared() {
		List<TransportRouteResult> list = adapter.convert(Arrays.asList(twoRides(), twoRides()));
		assertEquals(2, list.size());
		Set<Long> stopIds = new HashSet<>(), routeIds = new HashSet<>();
		Set<TransportStop> distinctStops = new HashSet<>();
		for (TransportRouteResult r : list) {
			for (TransportRouteResultSegment s : r.getSegments()) {
				routeIds.add(s.route.getId());
				assertTrue(s.route.getId() < 0);
				for (TransportStop st : s.route.getForwardStops()) {
					stopIds.add(st.getId());
					distinctStops.add(st);
					assertTrue(st.getId() < 0);
				}
			}
		}
		assertEquals(4, routeIds.size());
		assertEquals(8, stopIds.size());
		assertEquals(8, distinctStops.size());
		// same GTFS stop -> same object across itineraries of one conversion
		assertSame(list.get(0).getSegments().get(0).getStart(), list.get(1).getSegments().get(0).getStart());
		// distinct routes keep distinct objects (geometry style equality is route identity)
		assertTrue(list.get(0).getSegments().get(0).route != list.get(1).getSegments().get(0).route);
		TransportRoute r0 = list.get(0).getSegments().get(0).route;
		assertTrue(r0.compareRoute(r0));
		assertFalse(r0.getForwardStops().get(0).getRoutes().isEmpty());
	}

	// ---------------------------------------------------------------- geometry and stop queries

	@Test
	public void geometryIsAColouredWayNotTheGreyStopsLine() {
		GtfsResult res = adapter.convert(twoRides());
		for (TransportRouteResultSegment s : res.getSegments()) {
			List<Way> g = s.getGeometry();
			assertEquals(1, g.size());
			assertEquals(TransportRoutePlanner.GEOMETRY_WAY_ID, g.get(0).getId());
			assertTrue(g.get(0).getNodes().size() >= 2);
			// repeated calls (the drawer does this) keep working
			assertEquals(1, s.getGeometry().size());
		}
		// control: a route without any way would fall back to the grey stops way
		TransportRoute bare = new TransportRoute();
		TransportRouteResultSegment s0 = res.getSegments().get(0);
		bare.setForwardStops(new ArrayList<>(s0.route.getForwardStops()));
		bare.setType("subway");
		bare.setRef("X");
		bare.setId(-9L);
		TransportRouteResultSegment seg = new TransportRouteResultSegment();
		seg.route = bare;
		seg.start = 0;
		seg.end = 4;
		assertEquals(TransportRoutePlanner.STOPS_WAY_ID, seg.getGeometry().get(0).getId());
	}

	@Test
	public void isRouteStopFindsIntermediateStopsOnly() {
		GtfsResult res = adapter.convert(twoRides());
		TransportRouteResultSegment s0 = res.getSegments().get(0);
		TransportStop mid = s0.getTravelStops().get(2);
		assertTrue(res.isRouteStop(mid));
		assertSame(s0, res.getRouteStopSegment(mid));
		TransportStop foreign = new TransportStop();
		foreign.setId(12345L);
		foreign.setLocation(1, 1);
		foreign.setName("x");
		assertFalse(res.isRouteStop(foreign));
		assertNull(res.getRouteStopSegment(foreign));
	}

	@Test
	public void intermediateStopsCarryScheduledTimes() {
		GtfsResult res = adapter.convert(twoRides());
		GtfsRoute r = (GtfsRoute) res.getSegments().get(0).route;
		assertEquals(5, r.stopIdx.length);
		assertEquals(5, r.arrivalSec.length);
		assertEquals(5, r.departureSec.length);
		assertEquals(day + 8 * 3600 + 15 * 60, r.arrivalSec[2]);
		assertEquals(day + 8 * 3600 + 15 * 60, r.departureSec[2]);
		assertEquals(stop('C'), r.stopIdx[2]);
		assertEquals(trip("t1"), r.tripIdx);
		assertEquals(ix.getTripRoute(trip("t1")), r.routeIdx);
		assertEquals(r.tripIdx, ((GtfsSegment) res.getSegments().get(0)).tripIdx);
	}

	// ---------------------------------------------------------------- toString and UI getters do not throw

	@Test
	public void toStringAndGettersDoNotThrow() {
		Locale.setDefault(Locale.US);
		GtfsResult res = adapter.convert(twoRides());
		String str = res.toString();
		assertTrue(str, str.contains("Route 6 stops, 1 changes"));
		res.getTravelDist();
		res.getTravelTime();
		res.getWalkTime();
		res.getWalkSpeed();
		res.getAlternativeRoutes();
		for (TransportRouteResultSegment s : res.getSegments()) {
			s.getStart().getName("en", true);
			s.route.getName("en", true);
			s.route.getDistance();
			s.route.getAvgBothDistance();
			s.getNodes();
			s.getTravelStops();
			s.toString();
		}
	}

	// ---------------------------------------------------------------- transfers, loops, edge cases

	@Test
	public void sameStopTransferHasNoWalkAndSharesTheStop() {
		// t1 D (08:18) -> t3 starts at D 08:22
		GtfsLeg r1 = ride("t1", 0, 3);
		GtfsLeg r2 = ride("t3", 0, 1);
		GtfsResult res = adapter.convert(itin(r1.startSec, r2.endSec, r1, r2));
		assertEquals(0, res.getSegments().get(1).walkDist, 0);
		assertSame(res.getSegments().get(0).getEnd(), res.getSegments().get(1).getStart());
		assertEquals(4 * 60, res.getChangeTime(res.getSegments().get(0), res.getSegments().get(1)));
		assertEquals(0, res.getFinishWalkDist(), 0);
		assertEquals(r2.endSec - r1.startSec, res.getRouteTime(), 0);
	}

	@Test
	public void loopTripSharesFirstAndLastStop() {
		GtfsLeg loop = ride("t3", 0, 2); // D C D
		GtfsResult res = adapter.convert(itin(loop.startSec, loop.endSec, loop));
		TransportRouteResultSegment s = res.getSegments().get(0);
		assertSame(s.getStart(), s.getEnd());
		assertEquals(3, s.getTravelStops().size());
		assertEquals(2, res.getStops());
	}

	@Test
	public void walkOnlyItineraryIsNotConverted() {
		long h8 = day + 8 * 3600;
		GtfsItinerary walkOnly = itin(h8, h8 + 600, walk(h8, h8 + 600, 700));
		assertNull(adapter.convert(walkOnly));
		assertTrue(adapter.convert(Arrays.asList(walkOnly)).isEmpty());
		// mixed list: only the one with a ride survives
		List<TransportRouteResult> mixed = adapter.convert(Arrays.asList(walkOnly, twoRides(), null));
		assertEquals(1, mixed.size());
		assertFalse(mixed.get(0).getSegments().isEmpty());
	}

	@Test
	public void nullAndEmptyInputs() {
		assertNull(adapter.convert((GtfsItinerary) null));
		assertTrue(adapter.convert((List<GtfsItinerary>) null).isEmpty());
		assertTrue(adapter.convert(new ArrayList<GtfsItinerary>()).isEmpty());
		assertNull(adapter.convert(itin(0, 0)));
		try {
			new GtfsTransportResultAdapter(null, cfg);
			fail();
		} catch (IllegalArgumentException expected) {
			// index is required
		}
	}

	@Test
	public void malformedRideIsRejectedNotHalfBuilt() {
		GtfsLeg base = ride("t1", 0, 4);
		GtfsLeg oneStop = GtfsLeg.ride(base.tripIdx, base.routeIdx, 0, 0, new int[] { base.stops[0] },
				new long[] { base.arrivalSec[0] }, new long[] { base.departureSec[0] }, null, null, null, null);
		assertNull(adapter.convert(itin(base.startSec, base.endSec, oneStop)));
		GtfsLeg ragged = GtfsLeg.ride(base.tripIdx, base.routeIdx, 0, 4, base.stops, base.arrivalSec,
				new long[] { 1, 2 }, null, null, null, null);
		assertNull(adapter.convert(itin(base.startSec, base.endSec, ragged)));
	}

	@Test
	public void backToBackWalksAreSummed() {
		long h8 = day + 8 * 3600;
		GtfsLeg r = ride("t1", 0, 4);
		GtfsResult res = adapter.convert(itin(h8, r.endSec + 200, walk(h8, h8 + 10, 100), walk(h8 + 10, h8 + 20, 150), r,
				walk(r.endSec, r.endSec + 100, 40), walk(r.endSec + 100, r.endSec + 200, 60)));
		assertEquals(250, res.getSegments().get(0).walkDist, 0);
		assertEquals(100, res.getFinishWalkDist(), 0);
		assertEquals(350, res.getWalkDist(), 0);
	}

	// ---------------------------------------------------------------- round trip through the real planner

	@Test
	public void plannerItinerariesRoundTrip() {
		GtfsRoutePlanner planner = new GtfsRoutePlanner(ix);
		// start 170 m west of A and end 170 m east of H so access and egress walks exist
		LatLon from = new LatLon(40.70, -74.002);
		LatLon to = new LatLon(40.703, -73.938);
		List<GtfsItinerary> found = planner.plan(from, to, epoch(TUE, "08:00"), null, null);
		assertFalse(found.isEmpty());
		List<TransportRouteResult> res = adapter.convert(found);
		assertEquals(found.size(), res.size());
		for (int i = 0; i < res.size(); i++) {
			GtfsItinerary it = found.get(i);
			GtfsResult r = (GtfsResult) res.get(i);
			int rides = 0;
			double walk = 0;
			for (GtfsLeg l : it.legs) {
				if (l.type == GtfsLeg.Type.RIDE) {
					rides++;
				} else {
					walk += l.walkMeters;
				}
			}
			assertEquals(rides, r.getSegments().size());
			assertEquals(it.transfers, r.getChanges());
			assertEquals(walk, r.getWalkDist(), 1e-6);
			assertEquals(it.arriveSec - it.departSec, r.getRouteTime(), 0);
			assertEquals(it.arriveSec - it.departSec, r.getTravelTime() + r.getWalkTime(), 1e-6);
			for (TransportRouteResultSegment s : r.getSegments()) {
				assertTrue(s.route.getForwardStops().size() >= 2);
				assertNotNull(s.route.getType());
				assertNotNull(s.route.getRef());
				assertEquals(1, s.getGeometry().size());
			}
			assertNotNull(r.toString());
		}
		GtfsResult first = (GtfsResult) res.get(0);
		assertTrue("access walk before the first ride", first.getSegments().get(0).walkDist > 100);
		assertTrue("egress walk", first.getFinishWalkDist() > 100);
	}

	@Test
	public void plannerTransferItineraryHasTwoSegmentsAndRealWait() {
		GtfsRoutePlanner planner = new GtfsRoutePlanner(ix);
		// t1 ends at E; t2 starts at F ~330 m north: a walking transfer inside the footpath range
		List<GtfsItinerary> found = planner.plan(at('A'), new LatLon(40.703, -73.94), epoch(TUE, "08:05"), null, null);
		assertFalse(found.isEmpty());
		GtfsResult r = (GtfsResult) adapter.convert(found).get(0);
		assertEquals(2, r.getSegments().size());
		GtfsSegment s0 = (GtfsSegment) r.getSegments().get(0);
		GtfsSegment s1 = (GtfsSegment) r.getSegments().get(1);
		assertTrue(s1.walkDist > 0);
		assertEquals(s1.boardSec - s0.alightSec, r.getChangeTime(s0, s1));
		assertTrue(r.getChangeTime(s0, s1) >= 0);
		assertEquals("subway", s0.route.getType());
		assertEquals("bus", s1.route.getType());
	}

	@Test
	public void noItinerariesGiveAnEmptyList() {
		GtfsRoutePlanner planner = new GtfsRoutePlanner(ix);
		List<GtfsItinerary> none = planner.plan(at('A'), new LatLon(41.5, -70), epoch(TUE, "08:00"), null, null);
		assertTrue(adapter.convert(none).isEmpty());
	}
}
