package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.osmand.shared.routing.GeneralRouterProfile;
import net.osmand.shared.routing.RouteDataObject;
import net.osmand.shared.routing.RouteTypeRule;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * {@link RouteDataObject} is a copy of {@link net.osmand.binary.RouteDataObject}; this hands both the
 * same roads out of the test obf files and compares everything the router and the turn preparation
 * read off a road - tags, names, geometry, restrictions, direction and distance.
 */
public class RouteDataObjectCompatTest {

	private static final String[] LANGS = {null, "", "en", "ru", "de", "ja"};
	private static final String[] TAGS = {"maxspeed", "lanes", "oneway", "highway", "name", "surface", "access",
			"junction", "turn:lanes", "turn:lanes:forward", "turn:lanes:backward", "railway", "tunnel", "layer",
			"junction:ref", "junction:name", "destination", "traffic_signals:direction", "nonexistent"};

	@Test
	public void testEveryRoadReadsTheSame() throws IOException {
		List<net.osmand.binary.RouteDataObject> roads = TestObf.roads(6000);
		assertEquals(true, roads.size() > 10000);
		JavaToShared convert = new JavaToShared();
		Random random = new Random(20260913);
		for (net.osmand.binary.RouteDataObject j : roads) {
			RouteDataObject k = convert.road(j);
			String m = "road " + j.id;
			assertEquals(m, j.getId(), k.getId());
			assertEquals(m, j.getPointsLength(), k.getPointsLength());
			assertEquals(m, j.getHighway(), k.getHighway());
			assertEquals(m, j.getRoute(), k.getRoute());
			assertEquals(m, j.getOneway(), k.getOneway());
			assertEquals(m, j.getLanes(), k.getLanes());
			assertEquals(m, j.platform(), k.platform());
			assertEquals(m, j.roundabout(), k.roundabout());
			assertEquals(m, j.tunnel(), k.tunnel());
			assertEquals(m, j.isRoadDeleted(), k.isRoadDeleted());
			assertEquals(m, j.hasMotorwayJunctionNode(), k.hasMotorwayJunctionNode());
			assertEquals(m, j.getJunctionRef(), k.getJunctionRef());
			assertEquals(m, j.getJunctionName(), k.getJunctionName());
			assertEquals(m, j.getNodeRef(), k.getNodeRef());
			assertEquals(m, j.getNodeName(), k.getNodeName());
			assertEquals(m, j.getExitRef(), k.getExitRef());
			assertEquals(m, j.getExitName(), k.getExitName());
			assertEquals(m, j.hasPointTypes(), k.hasPointTypes());
			assertEquals(m, j.hasPointNames(), k.hasPointNames());
			assertEquals(m, j.getName(), k.getName());
			assertEquals(m, j.coordinates(), k.coordinates());
			assertEquals(m, j.toString(), k.toString());
			assertEquals(m, j.hasNameTagStartsWith("name:"), k.hasNameTagStartsWith("name:"));
			assertEquals(m, j.hasNameTagStartsWith("junk"), k.hasNameTagStartsWith("junk"));
			assertArrayEquals(m, j.getTypes(), k.getTypes());
			assertArrayEquals(m, j.getNameIds(), k.getNameIds());
			assertArrayEquals(m, j.calculateHeightArray(), k.calculateHeightArray(null), 0f);
			for (boolean dir : new boolean[] {false, true}) {
				assertEquals(m, j.getMaximumSpeed(dir), k.getMaximumSpeed(dir), 0f);
				assertEquals(m, j.getMaximumSpeed(dir, RouteTypeRule.PROFILE_CAR), k.getMaximumSpeed(dir, RouteTypeRule.PROFILE_CAR), 0f);
				assertEquals(m, j.getMaximumSpeed(dir, RouteTypeRule.PROFILE_TRUCK), k.getMaximumSpeed(dir, RouteTypeRule.PROFILE_TRUCK), 0f);
				assertEquals(m, j.isClockwise(dir), k.isClockwise(dir));
				for (String lang : LANGS) {
					assertEquals(m + " " + lang, j.getName(lang, false), k.getName(lang, false));
					assertEquals(m + " " + lang, j.getRef(lang, false, dir), k.getRef(lang, false, dir));
					assertEquals(m + " " + lang, j.getDestinationRef(lang, false, dir), k.getDestinationRef(lang, false, dir));
					assertEquals(m + " " + lang, j.getDestinationName(lang, false, dir), k.getDestinationName(lang, false, dir));
				}
			}
			for (String tag : TAGS) {
				assertEquals(m + " " + tag, j.getValue(tag), k.getValue(tag));
			}
			for (GeneralRouterProfile profile : GeneralRouterProfile.values()) {
				assertEquals(m + " " + profile, j.hasPrivateAccess(profile), k.hasPrivateAccess(profile));
			}
			if (j.getTypes() != null) {
				for (int t : j.getTypes()) {
					assertEquals(m + " type " + t, j.containsType(t), k.containsType(t));
				}
				assertEquals(m, j.containsType(-1), k.containsType(-1));
			}
			assertEquals(m, j.getRestrictionLength(), k.getRestrictionLength());
			for (int r = 0; r < j.getRestrictionLength(); r++) {
				assertEquals(m + " restriction " + r, j.getRestrictionType(r), k.getRestrictionType(r));
				assertEquals(m + " restriction " + r, j.getRestrictionId(r), k.getRestrictionId(r));
				assertEquals(m + " restriction " + r, j.getRestrictionVia(r), k.getRestrictionVia(r));
			}
			int n = j.getPointsLength();
			for (int i = 0; i < n; i++) {
				String p = m + " point " + i;
				assertEquals(p, j.getPoint31XTile(i), k.getPoint31XTile(i));
				assertEquals(p, j.getPoint31YTile(i), k.getPoint31YTile(i));
				assertArrayEquals(p, j.getPointTypes(i), k.getPointTypes(i));
				assertArrayEquals(p, j.getPointNames(i), k.getPointNames(i));
				assertArrayEquals(p, j.getPointNameTypes(i), k.getPointNameTypes(i));
				assertEquals(p, j.hasTrafficLightAt(i), k.hasTrafficLightAt(i));
				for (String tag : TAGS) {
					assertEquals(p + " " + tag, j.getValue(i, tag), k.getValue(i, tag));
				}
				for (boolean plus : new boolean[] {false, true}) {
					Same.close(p, j.directionRoute(i, plus), k.directionRoute(i, plus));
					for (float dist : new float[] {5, 15, 50}) {
						Same.close(p + " dist " + dist, j.directionRoute(i, plus, dist), k.directionRoute(i, plus, dist));
					}
					final int ii = i;
					final boolean pp = plus;
					Same.outcome(p, () -> j.isDirectionApplicable(pp, ii, 0, n - 1), () -> k.isDirectionApplicable(pp, ii, 0, n - 1));
					Same.outcome(p, () -> j.isDirectionApplicable(pp, ii, n - 1, 0), () -> k.isDirectionApplicable(pp, ii, n - 1, 0));
				}
				if (i + 1 < n) {
					assertEquals(p, j.getPoint31XTile(i, i + 1), k.getPoint31XTile(i, i + 1));
					assertEquals(p, j.getPoint31YTile(i, i + 1), k.getPoint31YTile(i, i + 1));
					Same.close(p, j.distance(i, i + 1), k.distance(i, i + 1));
					Same.close(p, j.distance(i + 1, i), k.distance(i + 1, i));
				}
			}
			if (n > 1) {
				Same.close(m, j.distance(0, n - 1), k.distance(0, n - 1));
			}
			assertEquals(m, j.compareRoute(j), k.compareRoute(k));

			// what the direction points do to a road: splice a point in, on a copy
			if (n > 1 && random.nextInt(20) == 0) {
				net.osmand.binary.RouteDataObject jc = new net.osmand.binary.RouteDataObject(j);
				RouteDataObject kc = new RouteDataObject(k);
				int pos = 1 + random.nextInt(n - 1);
				int x = jc.getPoint31XTile(pos - 1) + 100;
				int y = jc.getPoint31YTile(pos - 1) - 100;
				jc.insert(pos, x, y);
				kc.insert(pos, x, y);
				assertEquals(m + " after insert", jc.getPointsLength(), kc.getPointsLength());
				for (int i = 0; i < jc.getPointsLength(); i++) {
					assertEquals(m + " after insert " + i, jc.getPoint31XTile(i), kc.getPoint31XTile(i));
					assertEquals(m + " after insert " + i, jc.getPoint31YTile(i), kc.getPoint31YTile(i));
					assertArrayEquals(m + " after insert " + i, jc.getPointTypes(i), kc.getPointTypes(i));
					assertArrayEquals(m + " after insert " + i, jc.getPointNames(i), kc.getPointNames(i));
				}
				assertEquals(m + " after insert", jc.compareRoute(j), kc.compareRoute(k));
			}
			if (j.names != null) {
				int[] keys = j.names.keys();
				Arrays.sort(keys);
				int[] kkeys = k.getNames().keys();
				Arrays.sort(kkeys);
				assertArrayEquals(m, keys, kkeys);
				for (int key : keys) {
					assertEquals(m + " name " + key, j.names.get(key), k.getNames().get(key));
				}
			}
		}
	}
}
