package net.osmand.router.gtfs;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.osmand.data.LatLon;

/** The "walking vs. transit" ranking: off means earliest arrival, on charges extra for walking and transfers. */
public class GtfsRankCostTest {

	private static GtfsItinerary walkOnlyIt(long departSec, long arriveSec, long walkSec, int transfers) {
		List<GtfsLeg> legs = new ArrayList<>();
		LatLon p = new LatLon(40.6, -73.9);
		legs.add(GtfsLeg.walk(departSec, departSec + walkSec, -1, 0, p, p, (int) (walkSec * 1.25)));
		return new GtfsItinerary(legs, departSec, arriveSec, transfers);
	}

	@Test
	public void fastestIgnoresWalkingAndTransfers() {
		GtfsPlanOptions o = new GtfsPlanOptions();
		GtfsItinerary busWalkHeavy = walkOnlyIt(0, 1000, 720, 0);
		GtfsItinerary trainShortWalk = walkOnlyIt(0, 1240, 360, 0);
		assertTrue(GtfsRoutePlanner.rankCost(busWalkHeavy, o) < GtfsRoutePlanner.rankCost(trainShortWalk, o));
	}

	@Test
	public void balancedPrefersTheShorterWalkWhenArrivalIsClose() {
		GtfsPlanOptions o = new GtfsPlanOptions();
		o.walkWeight = 1.0;
		o.transferRankSec = 180;
		GtfsItinerary busWalkHeavy = walkOnlyIt(0, 1000, 720, 0);
		GtfsItinerary trainShortWalk = walkOnlyIt(0, 1240, 360, 0);
		assertTrue(GtfsRoutePlanner.rankCost(trainShortWalk, o) < GtfsRoutePlanner.rankCost(busWalkHeavy, o));
	}

	@Test
	public void balancedStillPrefersMuchEarlierArrival() {
		GtfsPlanOptions o = new GtfsPlanOptions();
		o.walkWeight = 1.0;
		o.transferRankSec = 180;
		GtfsItinerary fast = walkOnlyIt(0, 900, 600, 0);
		GtfsItinerary slowShortWalk = walkOnlyIt(0, 2400, 120, 0);
		assertTrue(GtfsRoutePlanner.rankCost(fast, o) < GtfsRoutePlanner.rankCost(slowShortWalk, o));
	}
}
