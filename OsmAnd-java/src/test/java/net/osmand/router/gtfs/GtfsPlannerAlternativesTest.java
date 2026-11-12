package net.osmand.router.gtfs;

import static net.osmand.router.gtfs.GtfsFixture.TUE;
import static net.osmand.router.gtfs.GtfsFixture.at;
import static net.osmand.router.gtfs.GtfsFixture.epoch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.data.LatLon;

/** Alternatives of {@link GtfsRoutePlanner}: deduplication, dominance, generation and ranking. */
public class GtfsPlannerAlternativesTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private GtfsIndex ix;

	private GtfsRoutePlanner planner(GtfsFixture f) throws IOException {
		File out = new File(tmp.newFolder(), "index.bin");
		new GtfsFeedImporter(null).importFeed(f.write(tmp.newFolder(), "feed.zip"), out);
		ix = GtfsIndex.open(out, true);
		return new GtfsRoutePlanner(ix);
	}

	private List<GtfsItinerary> plan(GtfsFixture f, char from, char to, String time, GtfsPlanOptions o) throws IOException {
		return planner(f).plan(at(from), at(to), epoch(TUE, time), o, null);
	}

	private static List<String> routes(GtfsItinerary it) {
		List<String> res = new ArrayList<>();
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				res.add(l.routeId.substring(l.routeId.indexOf(':') + 1));
			}
		}
		return res;
	}

	// ------------------------------------------------------------------ synthetic itineraries (no index)

	private static GtfsLeg walk(long start, int meters) {
		return GtfsLeg.walk(start, start + 60, -1, 0, at('A'), at('B'), meters);
	}

	private static GtfsLeg ride(int route, long start, long end) {
		return GtfsLeg.ride(route, route, 0, 1, new int[] {0, 1}, new long[] {start, end}, new long[] {start, end},
				at('A'), at('B'), "f:t" + route, "f:r" + route);
	}

	/** One ride of the given route after a walk of the given length; extra routes add rides (transfers). */
	private static GtfsItinerary itinerary(long arrive, int walkMeters, int... routes) {
		List<GtfsLeg> legs = new ArrayList<>();
		legs.add(walk(arrive - 3600, walkMeters));
		for (int i = 0; i < routes.length; i++) {
			long end = arrive - (routes.length - 1 - i) * 700L;
			legs.add(ride(routes[i], end - 600, end));
		}
		return new GtfsItinerary(legs, legs.get(0).startSec, legs.get(legs.size() - 1).endSec, routes.length - 1);
	}

	private static List<GtfsItinerary> accept(GtfsPlanOptions o, GtfsItinerary best, GtfsItinerary... cands) {
		List<GtfsItinerary> result = new ArrayList<>();
		result.add(best);
		GtfsRoutePlanner.addAlternatives(result, Arrays.asList(cands), o);
		return result;
	}

	@Test
	public void sameRouteSequenceOnALaterVehicleIsNotAnAlternative() {
		GtfsItinerary best = itinerary(10000, 200, 1, 2);
		GtfsItinerary later = itinerary(10600, 200, 1, 2); // other vehicles: other trip ids, same routes
		GtfsItinerary other = itinerary(10900, 200, 3);
		List<GtfsItinerary> r = accept(new GtfsPlanOptions(), best, later, other);
		assertEquals(Arrays.asList(best, other), r);
	}

	@Test
	public void dominatedAlternativesAreDropped() {
		GtfsItinerary best = itinerary(10000, 200, 1, 2); // 1 transfer, 200 m walking
		GtfsItinerary dominated = itinerary(10300, 300, 3, 4); // later, same transfers, more walking
		GtfsItinerary fewerTransfers = itinerary(10300, 300, 5); // later but 0 transfers: kept
		GtfsItinerary lessWalking = itinerary(10300, 50, 6, 7); // later, same transfers, less walking: kept
		List<GtfsItinerary> r = accept(new GtfsPlanOptions(), best, dominated, fewerTransfers, lessWalking);
		assertEquals(Arrays.asList(best, fewerTransfers, lessWalking), r);
	}

	@Test
	public void identicalMetricsOnOtherLinesAreBothKept() {
		GtfsItinerary best = itinerary(10000, 200, 1);
		GtfsItinerary twin = itinerary(10000, 200, 2);
		assertEquals(Arrays.asList(best, twin), accept(new GtfsPlanOptions(), best, twin));
	}

	@Test
	public void atMostMaxItinerariesAndWithinTheDelay() {
		GtfsItinerary best = itinerary(10000, 200, 1);
		GtfsItinerary a = itinerary(10100, 100, 2), b = itinerary(10200, 50, 3), c = itinerary(10300, 10, 4),
				far = itinerary(10000 + 3601, 0, 5);
		List<GtfsItinerary> r = accept(new GtfsPlanOptions(), best, far, c, b, a);
		assertEquals(Arrays.asList(best, a, b), r.subList(0, 3));
		assertEquals(3, r.size());
		GtfsPlanOptions o = new GtfsPlanOptions();
		o.maxItineraries = 4;
		assertEquals(Arrays.asList(best, a, b, c), accept(o, best, far, c, b, a));
	}

	// ------------------------------------------------------------------ through the planner

	@Test
	public void missedTransferOnTheNextVehicleIsNotListedAgain() throws IOException {
		// best: R1 then R2 (08:13). The penalised scan misses that transfer and rides R1 + R2 (08:45) instead.
		GtfsFixture f = GtfsFixture.basic()
				.trip("t1", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("t2", "R2", "WD", "B 08:13:00", "C 08:20:00")
				.trip("t3", "R2", "WD", "B 08:45:00", "C 08:52:00");
		List<GtfsItinerary> r = plan(f, 'A', 'C', "07:55", null);
		assertEquals(1, r.size());
		assertEquals(Arrays.asList("R1", "R2"), routes(r.get(0)));
		assertEquals(epoch(TUE, "08:20"), r.get(0).arriveSec);
	}

	@Test
	public void bestItineraryIsFirstWhateverTheAlternatives() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("first", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("second", "R2", "WD", "B 08:13:00", "C 08:20:00")
				.trip("direct", "R2", "WD", "A 08:01:00", "C 08:35:00");
		List<GtfsItinerary> r = plan(f, 'A', 'C', "07:55", null);
		assertSame(r.get(0), r.stream().min((x, y) -> Long.compare(x.arriveSec, y.arriveSec)).get());
	}

	@Test
	public void laterLineWithTheSameTransfersAndWalkingIsDominated() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("fast", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("slow", "R2", "WD", "A 08:02:00", "B 08:20:00");
		assertEquals(1, plan(f, 'A', 'B', "07:55", null).size());
	}

	// ------------------------------------------------------------------ generation

	@Test
	public void slowerDirectRideAppearsBesideTheFasterTransfer() throws IOException {
		// like Q44-SBS beside 7X+E: the direct ride arrives 15 minutes later than the best, with one transfer fewer
		GtfsFixture f = GtfsFixture.basic()
				.trip("first", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("second", "R2", "WD", "B 08:13:00", "C 08:20:00")
				.trip("direct", "R1", "WD", "A 08:01:00", "C 08:35:00");
		List<GtfsItinerary> r = plan(f, 'A', 'C', "07:55", null);
		assertEquals(2, r.size());
		assertEquals(Arrays.asList("R1", "R2"), routes(r.get(0)));
		assertEquals(Arrays.asList("R1"), routes(r.get(1)));
		assertEquals(epoch(TUE, "08:35"), r.get(1).arriveSec);
	}

	@Test
	public void banningTheSecondRouteFindsAnotherConnection() throws IOException {
		// banning R1 (the first ride) leaves nothing; banning R2 gives R1 + R3, later but ending 85 m from the goal
		GtfsFixture f = GtfsFixture.basic().row("routes.txt", "R3,L3,Line Three,3,")
				.trip("a", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("b", "R2", "WD", "B 08:13:00", "C 08:20:00")
				.trip("c", "R3", "WD", "B 08:14:00", "D 08:40:00");
		LatLon dest = new LatLon(at('D').getLatitude(), at('D').getLongitude() - 0.001);
		List<GtfsItinerary> r = planner(f).plan(at('A'), dest, epoch(TUE, "07:55"), null, null);
		assertEquals(2, r.size());
		assertEquals(Arrays.asList("R1", "R2"), routes(r.get(0)));
		assertEquals(Arrays.asList("R1", "R3"), routes(r.get(1)));
	}

	@Test
	public void routeBansAreCapped() throws IOException {
		GtfsFixture f = GtfsFixture.basic().row("routes.txt", "R3,L3,Line Three,3,")
				.trip("a", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("b", "R2", "WD", "B 08:13:00", "C 08:20:00")
				.trip("c", "R3", "WD", "B 08:14:00", "D 08:40:00");
		LatLon dest = new LatLon(at('D').getLatitude(), at('D').getLongitude() - 0.001);
		GtfsPlanOptions o = new GtfsPlanOptions();
		o.maxRouteBans = 1;
		assertEquals(1, planner(f).plan(at('A'), dest, epoch(TUE, "07:55"), o, null).size());
	}

	@Test
	public void bestItineraryDoesNotDependOnTheAlternativeSearch() throws IOException {
		GtfsFixture f = GtfsFixture.basic()
				.trip("first", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("second", "R2", "WD", "B 08:13:00", "C 08:20:00")
				.trip("direct", "R1", "WD", "A 08:01:00", "C 08:35:00");
		GtfsPlanOptions one = new GtfsPlanOptions();
		one.maxItineraries = 1;
		GtfsRoutePlanner p = planner(f);
		GtfsItinerary alone = p.plan(at('A'), at('C'), epoch(TUE, "07:55"), one, null).get(0);
		GtfsItinerary first = p.plan(at('A'), at('C'), epoch(TUE, "07:55"), null, null).get(0);
		assertEquals(alone.arriveSec, first.arriveSec);
		assertEquals(alone.transfers, first.transfers);
		assertEquals(routes(alone), routes(first));
	}

	// ------------------------------------------------------------------ ranking

	@Test
	public void fewerTransfersWinWithinTheTolerance() {
		GtfsItinerary best = itinerary(10000, 200, 1, 2);
		GtfsItinerary oneTransfer = itinerary(10200, 100, 3, 4);
		GtfsItinerary directLater = itinerary(10400, 150, 5); // 200 s after the other, 1 transfer fewer
		GtfsItinerary directMuchLater = itinerary(10900, 50, 6);
		GtfsPlanOptions o = new GtfsPlanOptions(); // 300 s per transfer
		o.maxItineraries = 4;
		assertEquals(Arrays.asList(best, directLater, oneTransfer, directMuchLater),
				accept(o, best, oneTransfer, directMuchLater, directLater));
		o.fewerTransfersToleranceSec = 0; // pure arrival order
		assertEquals(Arrays.asList(best, oneTransfer, directLater, directMuchLater),
				accept(o, best, oneTransfer, directMuchLater, directLater));
	}

	@Test
	public void bestStaysFirstEvenWhenAnAlternativeScoresBetter() {
		GtfsItinerary best = itinerary(10000, 200, 1, 2);
		GtfsItinerary direct = itinerary(10100, 200, 3);
		List<GtfsItinerary> r = accept(new GtfsPlanOptions(), best, direct);
		assertSame(best, r.get(0));
		assertSame(direct, r.get(1));
	}

	@Test
	public void sameArrivalWithFewerTransfersTakesTheFirstPlace() {
		GtfsItinerary best = itinerary(10000, 100, 1, 2);
		GtfsItinerary tie = itinerary(10000, 200, 3);
		assertEquals(Arrays.asList(tie, best), accept(new GtfsPlanOptions(), best, tie));
	}

	// ------------------------------------------------------------------ walking speed

	@Test
	public void walkSpeedIsConfigurableAndDefaultsTo125() throws IOException {
		assertEquals(1.25, new GtfsPlanOptions().walkSpeedMps, 0);
		GtfsFixture f = GtfsFixture.basic().trip("t1", "R1", "WD", "A 08:00:00", "B 08:10:00");
		LatLon from = new LatLon(at('A').getLatitude(), at('A').getLongitude() - 0.002);
		GtfsRoutePlanner p = planner(f);
		long[] secs = new long[2];
		double[] speeds = {1.25, 1.35};
		for (int i = 0; i < 2; i++) {
			GtfsPlanOptions o = new GtfsPlanOptions();
			o.walkSpeedMps = speeds[i];
			GtfsLeg walk = p.plan(from, at('B'), epoch(TUE, "07:50"), o, null).get(0).legs.get(0);
			secs[i] = walk.endSec - walk.startSec;
			assertEquals(walk.walkMetersAdjusted / speeds[i], secs[i], 1);
		}
		assertEquals(true, secs[1] < secs[0]);
	}
}
