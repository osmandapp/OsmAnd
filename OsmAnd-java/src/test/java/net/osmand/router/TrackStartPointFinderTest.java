package net.osmand.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.Location;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrackStartPointFinderTest {

	private static final double LAT0 = 47.0343;
	private static final double LON0 = 8.2726;
	private static final double M_PER_DEG_LAT = 111_320;
	private static final double M_PER_DEG_LON = M_PER_DEG_LAT * Math.cos(Math.toRadians(LAT0));

	private Location xy(double x, double y) {
		return new Location("", LAT0 + y / M_PER_DEG_LAT, LON0 + x / M_PER_DEG_LON);
	}

	// polyline through the given x,y corners (metres), a point every 10 m
	private List<Location> track(double... corners) {
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

	private int find(List<Location> track, double x, double y) {
		return new TrackStartPointFinder(track).findStartIndex(xy(x, y), -1);
	}

	private Location moving(double x, double y, float bearing) {
		Location l = xy(x, y);
		l.setBearing(bearing);
		l.setSpeed(5);
		return l;
	}

	private float distanceToFinish(List<Location> track, int index) {
		float d = 0;
		for (int i = index; i < track.size() - 1; i++) {
			d += track.get(i).distanceTo(track.get(i + 1));
		}
		return d;
	}

	// round trip like the one in #25428: 1.7 km, comes back up the same street and ends 8 m from the start
	private List<Location> roundTrip() {
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
		int i = new TrackStartPointFinder(t).findStartIndex(xy(10, -20), 40);
		assertTrue(distanceToFinish(t, i) < 60);
	}

	@Test
	public void recalculationAtStartOfLoopKeepsTheWholeLoop() {
		List<Location> t = roundTrip();
		float whole = distanceToFinish(t, 0);
		assertEquals(0, new TrackStartPointFinder(t).findStartIndex(xy(4, -12), whole));
	}

	@Test
	public void emptyTrackOrNoPosition() {
		assertEquals(0, new TrackStartPointFinder(new ArrayList<>()).findStartIndex(xy(0, 0), -1));
		assertEquals(0, new TrackStartPointFinder(track(0, 0, 100, 0)).findStartIndex(null, -1));
	}

	// the 10 route points of the #11717 loop (114 km, ends 68 m from its start), as a GPX with only <rtept> gives them
	private List<Location> routePointLoop11717() {
		List<Location> points = new ArrayList<>();
		double[][] xy = {{0, 0}, {30000, 25000}, {50000, 20000}, {52000, 15000}, {50000, 5000},
				{40000, -20000}, {20000, -15000}, {6000, -4000}, {4000, -3000}, {60, -30}};
		for (double[] p : xy) {
			points.add(xy(p[0], p[1]));
		}
		return points;
	}

	@Test
	public void sparseRoutePointLoopFollowsWholeLoop11717() {
		// 45 m from the first point, 25 m from the last one
		assertEquals(0, new TrackStartPointFinder(routePointLoop11717(), true).findStartIndex(xy(40, -20), -1));
	}

	// route points are kilometres apart, the bearing between two of them is not the road being driven
	@Test
	public void routePointsIgnoreBearing11717() {
		List<Location> points = routePointLoop11717();
		// 70 m from the first point, 39 m from the last one, moving the way the last leg points
		Location p = moving(35, -60, 300);
		assertEquals(0, new TrackStartPointFinder(points, true).findStartIndex(p, -1));
		// the same points taken as track geometry: the last pass goes that way
		assertEquals(points.size() - 1, new TrackStartPointFinder(points).findStartIndex(p, -1));
	}

	// #15013: a road used both ways; moving decides which pass is meant
	@Test
	public void sharedRoadPassFollowsDirectionOfMovement15013() {
		// out along y=0, a loop, back along y=10
		List<Location> t = track(0, 0, 1000, 0, 1000, 500, 1500, 500, 1500, 10, 0, 10);
		int back = new TrackStartPointFinder(t).findStartIndex(moving(500, 6, 270), -1);
		assertTrue(back > 100);
		assertTrue(t.get(back).distanceTo(xy(500, 10)) < 15);
		assertEquals(50, new TrackStartPointFinder(t).findStartIndex(moving(500, 6, 90), -1));
		// standing still: the earliest pass
		assertEquals(50, find(t, 500, 6));
	}

	// #15013 A-TC-04: a route has points only at turns, so near a turn the nearest point of both passes of a shared
	// road is the turn itself; the direction of a pass is that of the road the position is on, not of the next one
	@Test
	public void sharedRoadNextToTurnFollowsDirectionOfMovement15013() {
		List<Location> t = new ArrayList<>();
		// south along x=0 to the turn, WNW, a loop, back WNW to the turn, north along x=0
		double[] corners = {0, 200, 0, 0, -300, 100, -300, -400, 300, -400, 300, -100, 0, 0, 0, 250};
		for (int c = 0; c < corners.length; c += 2) {
			t.add(xy(corners[c], corners[c + 1]));
		}
		assertEquals(6, new TrackStartPointFinder(t).findStartIndex(moving(3, 30, 0), -1));
		assertEquals(1, new TrackStartPointFinder(t).findStartIndex(moving(3, 30, 180), -1));
		// at the turn itself
		assertEquals(6, new TrackStartPointFinder(t).findStartIndex(moving(0, 0, 0), -1));
		assertEquals(1, find(t, 3, 30));
	}

	// #25428 on an out-and-back and on a lollipop: the tail passes the start too, but whichever way the position
	// moves there, the whole track is followed
	@Test
	public void movingAtTrackStartFollowsWholeTrack() {
		List<Location> outAndBack = track(0, 0, 1000, 0, 1000, 10, 0, 10);
		List<Location> lollipop = track(0, 0, 0, 500, 300, 500, 300, 900, 8, 900, 8, 500, 8, 5);
		for (float bearing = 0; bearing < 360; bearing += 30) {
			assertEquals(0, new TrackStartPointFinder(outAndBack).findStartIndex(moving(40, 12, bearing), -1));
			assertEquals(0, new TrackStartPointFinder(lollipop).findStartIndex(moving(3, 10, bearing), -1));
		}
	}

	// finishing an out-and-back: walking home next to the start must not restart the track
	@Test
	public void recalculationTowardsFinishOfOutAndBackKeepsTheTail() {
		List<Location> t = track(0, 0, 1000, 0, 1000, 10, 0, 10);
		// 50 m were left of the previous route
		int i = new TrackStartPointFinder(t).findStartIndex(moving(40, 12, 270), 50);
		assertTrue(distanceToFinish(t, i) < 60);
	}

	// #11153: standing next to an earlier pass of the same street does not send the recalculation back to it
	@Test
	public void recalculationStandingNextToEarlierPassKeepsTheCurrentOne11153() {
		// east along y=0, a U-turn out of the area, back west along y=20, then away north
		List<Location> t = track(-300, 0, 140, 0, 140, 20, -300, 20, -300, 600);
		int current = 0;
		for (int i = 0; i < t.size(); i++) {
			if (t.get(i).distanceTo(xy(10, 20)) < t.get(current).distanceTo(xy(10, 20))) {
				current = i;
			}
		}
		int i = new TrackStartPointFinder(t).findStartIndex(xy(10, 18), distanceToFinish(t, current));
		assertTrue(Math.abs(i - current) <= 1);
	}

	// #4303: a recorded track jitters and neighbouring points may even go backwards, the direction is taken over 20 m
	@Test
	public void directionOfJitteryRecordedTrack4303() {
		// out and back along one street, points 2 m apart, every other one 3 m behind the previous one
		List<Location> t = new ArrayList<>();
		for (int k = 0; k <= 500; k++) {
			t.add(xy(2 * k - (k % 2) * 3, 0));
		}
		for (int k = 500; k >= 0; k--) {
			t.add(xy(2 * k + (k % 2) * 3, 10));
		}
		assertTrue(new TrackStartPointFinder(t).findStartIndex(moving(500, 5, 90), -1) <= 500);
		assertTrue(new TrackStartPointFinder(t).findStartIndex(moving(500, 5, 270), -1) > 500);
	}

	// #15013: nothing of the track is ahead of its end, which does not make the end fit every heading; the end goes
	// the way of its last segment, also when the last point is repeated
	@Test
	public void trackEndNextToEarlierPassGoesTheWayOfItsLastSegment15013() {
		// north through the crossing (0, 0), a loop, back west along y=0, ending 3 m east of the crossing
		List<Location> t = track(0, -500, 0, 500, 500, 500, 500, 0, 3, 0);
		List<Location> repeatedEnd = new ArrayList<>(t);
		repeatedEnd.add(t.get(t.size() - 1));
		for (List<Location> track : Arrays.asList(t, repeatedEnd)) {
			for (int bearing = 0; bearing < 360; bearing += 15) {
				int i = new TrackStartPointFinder(track).findStartIndex(moving(-2, 3, bearing), -1);
				int fromEnd = Math.abs(bearing - 270);
				if (Math.min(fromEnd, 360 - fromEnd) > 90) {
					assertTrue("bearing " + bearing + ": index " + i, distanceToFinish(track, i) > 1000);
				}
			}
			// 20 m before the end, moving the way the track ends: the last pass
			int i = new TrackStartPointFinder(track).findStartIndex(moving(23, 3, 270), -1);
			assertTrue("index " + i, distanceToFinish(track, i) < 30);
		}
	}

	// #15013 on a serpentine driven up and down the same road: the other leg of a hairpin is 8 m away and goes the
	// way the position moves, but it is another visit of the pass, behind the bend
	@Test
	public void hairpinUpAndDownFollowsDirectionOfMovement15013() {
		// up: east along y=0, a hairpin, west along y=10, north to the top; then the same way down
		double[] up = {-300, 0, 200, 0, 205, 2, 205, 8, 200, 10, 0, 10, 0, 600};
		double[] upAndDown = new double[up.length * 2 - 2];
		System.arraycopy(up, 0, upAndDown, 0, up.length);
		for (int c = up.length - 4; c >= 0; c -= 2) {
			upAndDown[up.length + (up.length - 4 - c)] = up[c];
			upAndDown[up.length + (up.length - 4 - c) + 1] = up[c + 1];
		}
		List<Location> t = track(upAndDown);
		int top = t.size() / 2;
		// on the upper leg, 2 m from it
		int down = new TrackStartPointFinder(t).findStartIndex(moving(100, 8, 90), -1);
		assertTrue("down: index " + down, down > top);
		int upIndex = new TrackStartPointFinder(t).findStartIndex(moving(100, 8, 270), -1);
		assertTrue("up: index " + upIndex, upIndex < top);
	}

	// #15013 with #4303: two laps of a loop recorded the same way jitter differently; moving along it starts the
	// first lap, the second one only seems to fit the heading better
	@Test
	public void lapsOfJitteryRecordedLoopStartFromTheFirstLap15013() {
		List<Location> t = new ArrayList<>();
		t.add(xy(-500, -500));
		for (int lap = 0; lap < 2; lap++) {
			// 600 x 400 m counter-clockwise, a point every 5 m, up to 3 m off the road
			List<Location> loop = track(0, 0, 600, 0, 600, 400, 0, 400, 0, 0);
			for (int k = 0; k < loop.size(); k++) {
				double off = 3 * Math.sin(k * (lap == 0 ? 2.1 : 1.3));
				Location p = loop.get(k);
				for (int h = 0; h < 2; h++) {
					t.add(new Location("", p.getLatitude() + off * (h == 0 ? 1 : -1) / M_PER_DEG_LAT,
							p.getLongitude() + off / M_PER_DEG_LON));
				}
			}
		}
		int secondLap = (t.size() - 1) / 2 + 1;
		for (int y = 60; y <= 340; y += 4) {
			for (int bearing = -15; bearing <= 15; bearing += 5) {
				int i = new TrackStartPointFinder(t).findStartIndex(moving(600, y, bearing), -1);
				assertTrue("y " + y + ", bearing " + bearing + ": index " + i, i < secondLap);
			}
		}
	}

	// #4303: a figure-eight crosses itself; a recalculation on the second pass must not go back to the first
	@Test
	public void recalculationAtCrossingKeepsTheSecondPass4303() {
		// east through the crossing (500, 0), a loop, then south through the same crossing
		List<Location> t = track(0, 0, 1000, 0, 1000, 500, 500, 500, 500, -500, 0, -500, 0, -400);
		int secondPass = 250;
		float left = distanceToFinish(t, secondPass);
		assertEquals(secondPass, new TrackStartPointFinder(t).findStartIndex(xy(505, 3), left));
		// no previous route, but moving south
		assertEquals(secondPass, new TrackStartPointFinder(t).findStartIndex(moving(505, 3, 180), -1));
	}

	// #11040: leaving a circular track at km 2 and joining it again at km 2.5 continues from km 2.5
	@Test
	public void recalculationAfterDetourOnLoopContinuesFromRejoinPoint11040() {
		List<Location> t = track(0, 0, 0, 2500, 2500, 2500, 2500, 0, 10, 0);
		float leftAtKm2 = distanceToFinish(t, 200);
		int i = new TrackStartPointFinder(t).findStartIndex(xy(20, 2500), leftAtKm2);
		assertEquals(252, i);
	}
}
