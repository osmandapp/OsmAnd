package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.osmand.data.LatLon;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.routing.PrecalculatedRouteDirection;
import net.osmand.shared.routing.RouteConditionalHelper;
import net.osmand.shared.routing.RouteDataObject;
import net.osmand.shared.routing.RouteSegmentResult;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * {@link PrecalculatedRouteDirection} and {@link RouteConditionalHelper} are copies of the java
 * classes of the same names; this feeds both sides the same roads and compares what they compute.
 *
 * The precalculated direction is built from chains of real road segments and asked for its time
 * estimate, deviation and index at the points of those roads and around them. The conditional helper
 * is run over every road with the moments the routing tests use, and the road's types afterwards
 * are compared - that is what the router sees of a conditional tag.
 */
public class PrecalculatedRouteCompatTest {

	@Test
	public void testDirectionsEstimateTheSame() throws IOException {
		List<net.osmand.binary.RouteDataObject> roads = new ArrayList<>();
		for (net.osmand.binary.RouteDataObject road : TestObf.roads(400)) {
			if (road.getPointsLength() > 2) {
				roads.add(road);
			}
		}
		assertEquals(true, roads.size() > 1000);
		JavaToShared convert = new JavaToShared();
		Random random = new Random(20260913);
		net.osmand.router.RoutingConfiguration.RoutingMemoryLimits jlimits = new net.osmand.router.RoutingConfiguration.RoutingMemoryLimits(30, 256);
		net.osmand.router.RoutingContext ctx = new net.osmand.router.RoutePlannerFrontEnd().buildRoutingContext(
				net.osmand.router.RoutingConfiguration.getDefault().build("car", jlimits),
				null, TestObf.readers().toArray(new net.osmand.binary.BinaryMapIndexReader[0]));
		net.osmand.shared.routing.VehicleRouter router = net.osmand.shared.routing.RoutingConfiguration.getDefault()
				.build("car", new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(30, 256)).router;
		for (int chain = 0; chain < 300; chain++) {
			List<net.osmand.router.RouteSegmentResult> js = new ArrayList<>();
			List<RouteSegmentResult> ks = new ArrayList<>();
			int start = random.nextInt(roads.size() - 6);
			for (int i = 0; i < 1 + random.nextInt(5); i++) {
				net.osmand.binary.RouteDataObject road = roads.get(start + i);
				int n = road.getPointsLength();
				int a = random.nextInt(n);
				int b = random.nextInt(n);
				if (a == b) {
					b = (a + 1) % n;
				}
				net.osmand.router.RouteSegmentResult j = new net.osmand.router.RouteSegmentResult(road, a, b);
				RouteSegmentResult k = new RouteSegmentResult(convert.road(road), a, b);
				float speed = 5 + random.nextInt(30);
				j.setSegmentSpeed(speed);
				k.setSegmentSpeed(speed);
				js.add(j);
				ks.add(k);
			}
			for (float cutoff : new float[] {0, 500, 5000}) {
				for (float maxSpeed : new float[] {15, 40}) {
					String m = "chain " + chain + " cutoff " + cutoff + " maxSpeed " + maxSpeed;
					net.osmand.router.PrecalculatedRouteDirection j = net.osmand.router.PrecalculatedRouteDirection.build(js, cutoff, maxSpeed);
					PrecalculatedRouteDirection k = PrecalculatedRouteDirection.build(ks, cutoff, maxSpeed);
					assertEquals(m, j == null, k == null);
					if (j == null) {
						continue;
					}
					assertSame(m, j, k, js, random, ctx, router);
				}
			}
			LatLon[] jl = new LatLon[js.size() * 2];
			KLatLon[] kl = new KLatLon[js.size() * 2];
			for (int i = 0; i < js.size(); i++) {
				jl[2 * i] = js.get(i).getStartPoint();
				jl[2 * i + 1] = js.get(i).getEndPoint();
				kl[2 * i] = new KLatLon(jl[2 * i].getLatitude(), jl[2 * i].getLongitude());
				kl[2 * i + 1] = new KLatLon(jl[2 * i + 1].getLatitude(), jl[2 * i + 1].getLongitude());
			}
			net.osmand.router.PrecalculatedRouteDirection j = net.osmand.router.PrecalculatedRouteDirection.build(jl, 25);
			PrecalculatedRouteDirection k = PrecalculatedRouteDirection.build(kl, 25);
			assertSame("chain " + chain + " from points", j, k, js, random, ctx, router);
		}
	}

	/**
	 * What a calculation does with a direction: adopts it for its start and target, which also sets the
	 * speeds it estimates with, and then asks how long from a point to the end or from the start to a point.
	 * The deviation truncates a projection to a tile unit, so a last-bit difference in the projection is
	 * one unit - two centimetres - of deviation, and that much over the default speed of time.
	 */
	private static void assertSame(String m, net.osmand.router.PrecalculatedRouteDirection j, PrecalculatedRouteDirection k,
	                               List<net.osmand.router.RouteSegmentResult> segments, Random random,
	                               net.osmand.router.RoutingContext ctx, net.osmand.shared.routing.VehicleRouter router) {
		assertEquals(m, j.isFollowNext(), k.isFollowNext());
		List<int[]> points = new ArrayList<>();
		for (net.osmand.router.RouteSegmentResult s : segments) {
			points.add(new int[] {s.getStartPointX(), s.getStartPointY()});
			points.add(new int[] {s.getEndPointX(), s.getEndPointY()});
			points.add(new int[] {s.getStartPointX() + random.nextInt(20000) - 10000, s.getStartPointY() + random.nextInt(20000) - 10000});
		}
		for (int[] p : points) {
			String pm = m + " at " + p[0] + "," + p[1];
			assertEquals(pm, j.getIndex(p[0], p[1]), k.getIndex(p[0], p[1]));
			assertEquals(pm, j.getDeviationDistance(p[0], p[1]), k.getDeviationDistance(p[0], p[1]), 0.05f);
		}
		int[] start = points.get(0);
		int[] target = points.get(points.size() - 2);
		ctx.startX = start[0];
		ctx.startY = start[1];
		ctx.targetX = target[0];
		ctx.targetY = target[1];
		net.osmand.router.PrecalculatedRouteDirection ja = j.adopt(ctx);
		PrecalculatedRouteDirection ka = k.adopt(start[0], start[1], target[0], target[1], router);
		assertEquals(m + " adopted", ja == null, ka == null);
		if (ja == null) {
			return;
		}
		for (int[] p : points) {
			String pm = m + " adopted, at " + p[0] + "," + p[1];
			assertEquals(pm, ja.getIndex(p[0], p[1]), ka.getIndex(p[0], p[1]));
			assertEquals(pm + " to target", ja.timeEstimate(p[0], p[1], target[0], target[1]), ka.timeEstimate(p[0], p[1], target[0], target[1]), 0.05f);
			assertEquals(pm + " from start", ja.timeEstimate(start[0], start[1], p[0], p[1]), ka.timeEstimate(start[0], start[1], p[0], p[1]), 0.05f);
		}
		int[] a = points.get(1);
		int[] b = points.get(points.size() - 1);
		ja.updatePreciseStartEnd(a[0], a[1], b[0], b[1]);
		ka.updatePreciseStartEnd(a[0], a[1], b[0], b[1]);
		assertEquals(m + " after precise ends", ja.timeEstimate(a[0], a[1], b[0], b[1]), ka.timeEstimate(a[0], a[1], b[0], b[1]), 0.05f);
		ja.setFollowNext(true);
		ka.setFollowNext(true);
		assertEquals(m + " following", ja.timeEstimate(a[0], a[1], b[0], b[1]), ka.timeEstimate(a[0], a[1], b[0], b[1]), 0.05f);
	}

	private static String decode(net.osmand.binary.RouteDataObject road) {
		StringBuilder sb = new StringBuilder();
		for (int t : road.getTypes()) {
			sb.append(t).append(':').append(road.region.quickGetEncodingRule(t)).append(' ');
		}
		return sb.toString().trim();
	}

	private static String decode(RouteDataObject road) {
		StringBuilder sb = new StringBuilder();
		for (int t : road.getTypes()) {
			sb.append(t).append(':').append(road.region.quickGetEncodingRule(t)).append(' ');
		}
		return sb.toString().trim();
	}

	@Test
	public void testConditionalTagsResolveTheSame() throws IOException {
		List<net.osmand.binary.RouteDataObject> roads = TestObf.roads(3000);
		assertEquals(true, roads.size() > 5000);
		// what the app passes: which conditional tags may override their plain tag, and how
		Map<String, String> ambiguous = new HashMap<>();
		ambiguous.put("maxspeed:conditional", net.osmand.router.RouteConditionalHelper.RULE_INT_MAX);
		ambiguous.put("access:conditional", "yes");
		ambiguous.put("oneway:conditional", "no");
		Calendar cal = Calendar.getInstance();
		long[] moments = new long[4];
		int[][] when = {{2024, Calendar.JANUARY, 15, 8, 30}, {2024, Calendar.JUNE, 15, 23, 30},
				{2024, Calendar.DECEMBER, 25, 12, 0}, {2025, Calendar.JULY, 4, 7, 59}};
		for (int i = 0; i < when.length; i++) {
			cal.clear();
			cal.set(when[i][0], when[i][1], when[i][2], when[i][3], when[i][4], 0);
			moments[i] = cal.getTimeInMillis();
		}
		JavaToShared convert = new JavaToShared();
		net.osmand.router.RouteConditionalHelper jh = new net.osmand.router.RouteConditionalHelper();
		RouteConditionalHelper kh = new RouteConditionalHelper();
		int conditional = 0;
		for (net.osmand.binary.RouteDataObject road : roads) {
			boolean hasConditional = false;
			if (road.getTypes() != null) {
				for (int t : road.getTypes()) {
					if (road.region.quickGetEncodingRule(t).conditional()) {
						hasConditional = true;
					}
				}
			}
			for (long time : moments) {
				net.osmand.binary.RouteDataObject j = new net.osmand.binary.RouteDataObject(road);
				RouteDataObject k = new RouteDataObject(convert.road(road));
				jh.resolveAmbiguousConditionalTags(j, ambiguous);
				kh.resolveAmbiguousConditionalTags(k, ambiguous);
				jh.processConditionalTags(j, time);
				kh.processConditionalTags(k, time);
				String m = "road " + road.id + " at " + time;
				assertEquals(m + " types", decode(j), decode(k));
				assertEquals(m, j.getMaximumSpeed(true), k.getMaximumSpeed(true), 0f);
				assertEquals(m, j.getValue("access"), k.getValue("access"));
				if (hasConditional) {
					conditional++;
				}
			}
		}
		assertEquals("the fixtures carry conditional tags", true, conditional > 0);
	}
}
