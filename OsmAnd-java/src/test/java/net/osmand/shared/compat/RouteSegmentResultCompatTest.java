package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.osmand.shared.routing.RouteSegmentResult;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@link RouteSegmentResult} is a copy of {@link net.osmand.router.RouteSegmentResult}; this puts
 * the same stretch of the same real road into both and compares what the turn preparation reads off
 * a segment - bearings, distances, points, names - in both directions of travel.
 */
public class RouteSegmentResultCompatTest {

	private static final String[] LANGS = {null, "en", "ru"};

	@Test
	public void testEverySegmentReadsTheSame() throws IOException {
		List<net.osmand.binary.RouteDataObject> roads = new ArrayList<>();
		for (net.osmand.binary.RouteDataObject road : TestObf.roads(1500)) {
			if (road.getPointsLength() > 1) {
				roads.add(road);
			}
		}
		assertEquals(true, roads.size() > 3000);
		JavaToShared convert = new JavaToShared();
		Random random = new Random(20260913);
		for (int r = 0; r < roads.size(); r++) {
			net.osmand.binary.RouteDataObject road = roads.get(r);
			int n = road.getPointsLength();
			for (int pair = 0; pair < 3; pair++) {
				int a = random.nextInt(n);
				int b = random.nextInt(n);
				if (a == b) {
					b = (a + 1) % n;
				}
				net.osmand.router.RouteSegmentResult j = new net.osmand.router.RouteSegmentResult(road, a, b);
				RouteSegmentResult k = new RouteSegmentResult(convert.road(road), a, b);
				String m = "road " + road.id + " " + a + "->" + b;
				assertEquals(m, j.isForwardDirection(), k.isForwardDirection());
				assertEquals(m, j.getStartPointX(), k.getStartPointX());
				assertEquals(m, j.getStartPointY(), k.getStartPointY());
				assertEquals(m, j.getEndPointX(), k.getEndPointX());
				assertEquals(m, j.getEndPointY(), k.getEndPointY());
				assertEquals(m, j.getStartPoint().getLatitude(), k.getStartPoint().getLatitude(), 0d);
				assertEquals(m, j.getStartPoint().getLongitude(), k.getStartPoint().getLongitude(), 0d);
				assertEquals(m, j.getEndPoint().getLatitude(), k.getEndPoint().getLatitude(), 0d);
				assertEquals(m, j.getEndPoint().getLongitude(), k.getEndPoint().getLongitude(), 0d);
				Same.close(m, j.getBearingBegin(), k.getBearingBegin());
				Same.close(m, j.getBearingEnd(), k.getBearingEnd());
				for (int point : new int[] {a, b}) {
					for (float dist : new float[] {10, 50}) {
						Same.close(m + " bearing at " + point, j.getBearingBegin(point, dist), k.getBearingBegin(point, dist));
						Same.close(m + " bearing at " + point, j.getBearingEnd(point, dist), k.getBearingEnd(point, dist));
					}
					for (boolean plus : new boolean[] {false, true}) {
						Same.close(m + " distance at " + point, j.getDistance(point, plus), k.getDistance(point, plus));
					}
					assertEquals(m + " point " + point, j.getPoint(point).getLatitude(), k.getPoint(point).getLatitude(), 0d);
				}
				assertArrayEquals(m, j.getHeightValues(), k.getHeightValues(), 0f);
				List<net.osmand.router.RouteSegmentResult> jl = new ArrayList<>();
				List<RouteSegmentResult> kl = new ArrayList<>();
				for (int i = 0; i < 3; i++) {
					net.osmand.binary.RouteDataObject other = roads.get(random.nextInt(roads.size()));
					int s = random.nextInt(other.getPointsLength());
					int e = (s + 1) % other.getPointsLength();
					jl.add(new net.osmand.router.RouteSegmentResult(other, s, e));
					kl.add(new RouteSegmentResult(convert.road(other), s, e));
				}
				jl.add(1, j);
				kl.add(1, k);
				for (String lang : LANGS) {
					assertEquals(m + " " + lang, j.getRef(lang, false), k.getRef(lang, false));
					assertEquals(m + " " + lang, j.getStreetName(lang, false, jl, 1), k.getStreetName(lang, false, kl, 1));
					assertEquals(m + " " + lang, j.getDestinationName(lang, false, jl, 1, true), k.getDestinationName(lang, false, kl, 1, true));
					assertEquals(m + " " + lang, j.getDestinationName(lang, false, jl, 1, false), k.getDestinationName(lang, false, kl, 1, false));
				}
				net.osmand.router.RouteSegmentResult jo = jl.get(2);
				RouteSegmentResult ko = kl.get(2);
				assertEquals(m, j.continuesBeyondRouteSegment(jo), k.continuesBeyondRouteSegment(ko));
				assertEquals(m, jo.continuesBeyondRouteSegment(j), ko.continuesBeyondRouteSegment(k));
				net.osmand.router.RouteSegmentResult j2 = new net.osmand.router.RouteSegmentResult(road, b, a);
				RouteSegmentResult k2 = new RouteSegmentResult(convert.road(road), b, a);
				assertEquals(m, j.continuesBeyondRouteSegment(j2), k.continuesBeyondRouteSegment(k2));
				j.setDescription("short", "full");
				k.setDescription("short", "full");
				assertEquals(m, j.getDescription(false), k.getDescription(false));
				assertEquals(m, j.getDescription(true), k.getDescription(true));
			}
		}
	}
}
