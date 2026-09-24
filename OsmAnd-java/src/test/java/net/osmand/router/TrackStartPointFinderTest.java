package net.osmand.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.Location;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TrackStartPointFinderTest {

	private static final double LAT0 = 47.0343;
	private static final double LON0 = 8.2726;
	private static final double M_PER_DEG_LAT = 111_320;
	private static final double M_PER_DEG_LON = M_PER_DEG_LAT * Math.cos(Math.toRadians(LAT0));

	private static Location xy(double x, double y) {
		return new Location("", LAT0 + y / M_PER_DEG_LAT, LON0 + x / M_PER_DEG_LON);
	}

	// polyline through the given x,y corners (metres), a point every 10 m
	private static List<Location> track(double... corners) {
		List<Location> res = new ArrayList<>();
		res.add(xy(corners[0], corners[1]));
		for (int c = 2; c < corners.length; c += 2) {
			double x0 = corners[c - 2], y0 = corners[c - 1], x1 = corners[c], y1 = corners[c + 1];
			int steps = Math.max(1, (int) Math.round(Math.hypot(x1 - x0, y1 - y0) / 10));
			for (int s = 1; s <= steps; s++) {
				res.add(xy(x0 + (x1 - x0) * s / steps, y0 + (y1 - y0) * s / steps));
			}
		}
		return res;
	}

	private static int find(List<Location> track, double x, double y) {
		return TrackStartPointFinder.findStartIndex(track, xy(x, y), -1);
	}

	private static Location moving(double x, double y, float bearing) {
		Location l = xy(x, y);
		l.setBearing(bearing);
		l.setSpeed(5);
		return l;
	}

	private static float distanceToFinish(List<Location> track, int index) {
		float d = 0;
		for (int i = index; i < track.size() - 1; i++) {
			d += track.get(i).distanceTo(track.get(i + 1));
		}
		return d;
	}

	// round trip like the one in #25428: 1.7 km, comes back up the same street and ends 8 m from the start
	private static List<Location> roundTrip() {
		return track(0, 0, 0, 300, 400, 300, 400, -100, 8, -100, 8, -8);
	}

	@Test
	public void openTrackStartsFromNearestPoint() {
		List<Location> t = track(0, 0, 1000, 0);
		assertEquals(0, find(t, -30, 5));
		assertEquals(50, find(t, 500, 20));
		assertEquals(t.size() - 1, find(t, 1030, 0));
	}

	@Test
	public void roundTripFromTheClosureFollowsWholeLoop25428and13851() {
		List<Location> t = roundTrip();
		// 15 m south of the closure the last point is the nearest one: argmin left nothing to follow
		assertEquals(0, find(t, 3, -15));
		assertEquals(0, find(t, 8, -2));
		// out of the start's 60 m but still nearest to the tail of the loop
		assertEquals(0, find(t, 40, -60));
	}

	@Test
	public void roundTripMidwayStartsFromNearestPoint() {
		List<Location> t = roundTrip();
		assertEquals(15, find(t, -20, 150));
		assertEquals(90, find(t, 410, 100));
	}

	@Test
	public void smallLoopThatNeverLeavesTheAreaFollowsWholeLoop() {
		List<Location> t = track(0, 0, 60, 0, 60, 40, 0, 40, 0, 5);
		assertEquals(0, find(t, -5, 8));
	}

	@Test
	public void outAndBackStartsFromOutboundPass() {
		// out 1 km along one side of the street, back along the other side, 10 m apart
		List<Location> t = track(0, 0, 1000, 0, 1000, 10, 0, 10);
		assertEquals(0, find(t, -10, 12));
		// nearer to the way back, but the outbound pass comes first
		assertEquals(50, find(t, 500, 8));
	}

	@Test
	public void recalculationNearFinishOfLoopKeepsTheTail() {
		List<Location> t = roundTrip();
		// 40 m were left of the previous route: the start of the loop is out of reach
		int i = TrackStartPointFinder.findStartIndex(t, xy(10, -20), 40 + 300);
		assertTrue(distanceToFinish(t, i) < 60);
	}

	@Test
	public void recalculationAtStartOfLoopKeepsTheWholeLoop() {
		List<Location> t = roundTrip();
		float whole = distanceToFinish(t, 0);
		assertEquals(0, TrackStartPointFinder.findStartIndex(t, xy(4, -12), whole + 300));
	}

	@Test
	public void emptyTrackOrNoPosition() {
		assertEquals(0, TrackStartPointFinder.findStartIndex(new ArrayList<>(), xy(0, 0), -1));
		assertEquals(0, TrackStartPointFinder.findStartIndex(track(0, 0, 100, 0), null, -1));
	}

	// #11717: Garmin route of 10 route points, a 114 km loop that ends 68 m from its start
	@Test
	public void sparseRoutePointLoopFollowsWholeLoop11717() {
		List<Location> points = new ArrayList<>();
		double[][] xy = {{0, 0}, {30000, 25000}, {50000, 20000}, {52000, 15000}, {50000, 5000},
				{40000, -20000}, {20000, -15000}, {6000, -4000}, {4000, -3000}, {60, -30}};
		for (double[] p : xy) {
			points.add(xy(p[0], p[1]));
		}
		// 45 m from the first point, 25 m from the last one
		assertEquals(0, TrackStartPointFinder.findStartIndex(points, xy(40, -20), -1));
	}

	// #15013: a road used both ways; moving decides which pass is meant
	@Test
	public void sharedRoadPassFollowsDirectionOfMovement15013() {
		// out along y=0, a loop, back along y=10
		List<Location> t = track(0, 0, 1000, 0, 1000, 500, 1500, 500, 1500, 10, 0, 10);
		int back = TrackStartPointFinder.findStartIndex(t, moving(500, 6, 270), -1);
		assertTrue(back > 100);
		assertTrue(t.get(back).distanceTo(xy(500, 10)) < 15);
		assertEquals(50, TrackStartPointFinder.findStartIndex(t, moving(500, 6, 90), -1));
		// standing still: the earliest pass
		assertEquals(50, find(t, 500, 6));
	}

	// finishing an out-and-back: walking home next to the start must not restart the track
	@Test
	public void movingTowardsFinishOfOutAndBackKeepsTheTail() {
		List<Location> t = track(0, 0, 1000, 0, 1000, 10, 0, 10);
		int i = TrackStartPointFinder.findStartIndex(t, moving(40, 12, 270), -1);
		assertTrue(distanceToFinish(t, i) < 60);
	}

	// #4303: a figure-eight crosses itself; a recalculation on the second pass must not go back to the first
	@Test
	public void recalculationAtCrossingKeepsTheSecondPass4303() {
		// east through the crossing (500, 0), a loop, then south through the same crossing
		List<Location> t = track(0, 0, 1000, 0, 1000, 500, 500, 500, 500, -500, 0, -500, 0, -400);
		int secondPass = 250;
		float left = distanceToFinish(t, secondPass);
		assertEquals(secondPass, TrackStartPointFinder.findStartIndex(t, xy(505, 3), left + 300));
		// no previous route, but moving south
		assertEquals(secondPass, TrackStartPointFinder.findStartIndex(t, moving(505, 3, 180), -1));
	}

	// #11040: leaving a circular track at km 2 and joining it again at km 2.5 continues from km 2.5
	@Test
	public void recalculationAfterDetourOnLoopContinuesFromRejoinPoint11040() {
		List<Location> t = track(0, 0, 0, 2500, 2500, 2500, 2500, 0, 10, 0);
		float leftAtKm2 = distanceToFinish(t, 200);
		int i = TrackStartPointFinder.findStartIndex(t, xy(20, 2500), leftAtKm2 + 300);
		assertEquals(252, i);
	}
}
