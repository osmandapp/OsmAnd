package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;

// Restrictions whose via is a chain of several ways (https://github.com/osmandapp/OsmAnd/issues/12537).
// Maps are cut from OSM around the relation, routes are the examples of https://github.com/osmandapp/OsmAnd/issues/24317
public class RestrictionViaWaysTest {

	private static final String MAPS_DIR = "src/test/resources/routing/";

	// only_left_turn 8326460, Rondo Kamyczek, Rybnik. https://www.openstreetmap.org/relation/8326460
	@Test
	public void testOnlyLeftTurn8326460() throws Exception {
		checkRestriction("restriction_via_8326460.obf", new LatLon(50.087072, 18.546247), new LatLon(50.086890, 18.545311),
				true, 590977977L, 355531171L, 1426872613L, 590977976L);
	}

	// no_left_turn 18899940, Trasa Warszawska, Konin. https://www.openstreetmap.org/relation/18899940
	@Test
	public void testNoLeftTurn18899940() throws Exception {
		checkRestriction("restriction_via_18899940.obf", new LatLon(52.216373, 18.255565), new LatLon(52.217527, 18.253554),
				false, 179053517L, 1371916141L, 1371918565L, 172605804L);
	}

	// only_right_turn 19480311, Tytusa Chałubińskiego, Warsaw. https://www.openstreetmap.org/relation/19480311
	@Test
	public void testOnlyRightTurn19480311() throws Exception {
		checkRestriction("restriction_via_19480311.obf", new LatLon(52.225256, 21.004765), new LatLon(52.226115, 21.003078),
				true, 505464654L, 1423403195L, 1423403197L, 1423403196L);
	}

	// no_right_turn 11670368, Rondo Rowieńskie, Żory. https://www.openstreetmap.org/relation/11670368
	@Test
	public void testNoRightTurn11670368() throws Exception {
		checkRestriction("restriction_via_11670368.obf", new LatLon(50.061568, 18.663572), new LatLon(50.061833, 18.663046),
				false, 397716818L, 851598848L, 851598851L, 174513204L);
	}

	// no_straight_on 8099562, Rondo Grunwaldzkie, Kraków. https://www.openstreetmap.org/relation/8099562
	@Test
	public void testNoStraightOn8099562() throws Exception {
		checkRestriction("restriction_via_8099562.obf", new LatLon(50.048366, 19.933190), new LatLon(50.049909, 19.931967),
				false, 217284501L, 153263840L, 312050146L, 19844875L);
	}

	// no_u_turn 17796970, Powstańców Śląskich / Wielka, Wrocław. https://www.openstreetmap.org/relation/17796970
	@Test
	public void testNoUTurn17796970() throws Exception {
		checkRestriction("restriction_via_17796970.obf", new LatLon(51.093660, 17.020327), new LatLon(51.093544, 17.020620),
				false, 326941467L, 326941464L, 28459702L, 115490175L);
	}

	// no_u_turn 12184293, Lisowicka, Kędzierzyn-Koźle (counterexample in #24317: already respected). https://www.openstreetmap.org/relation/12184293
	@Test
	public void testNoUTurn12184293() throws Exception {
		checkRestriction("restriction_via_12184293.obf", new LatLon(50.674180, 18.649048), new LatLon(50.674298, 18.649029),
				false, 344080459L, 344080456L, 344080450L, 344080460L);
	}

	// no_u_turn 17373084, Częstochowska / Powstańców Warszawskich, Opole (counterexample in #24317: already respected). https://www.openstreetmap.org/relation/17373084
	@Test
	public void testNoUTurn17373084() throws Exception {
		checkRestriction("restriction_via_17373084.obf", new LatLon(50.672798, 17.995943), new LatLon(50.672713, 17.995327),
				false, 181269111L, 1177939840L, 1177939815L, 180860187L);
	}

	// chain = from, via..., to. A car route that enters the chain (from -> first via) must follow it to the end
	// for only_* restrictions and must not follow it to the end for no_* restrictions.
	private void checkRestriction(String map, LatLon start, LatLon end, boolean onlyRestriction, Long... chain)
			throws Exception {
		List<Long> ways = calculateRouteWays(map, start, end);
		List<Long> restriction = Arrays.asList(chain);
		for (int i = 0; i + 1 < ways.size(); i++) {
			boolean entersChain = ways.get(i).equals(chain[0]) && ways.get(i + 1).equals(chain[1]);
			if (!entersChain) {
				continue;
			}
			boolean followsChain = i + chain.length <= ways.size()
					&& ways.subList(i, i + chain.length).equals(restriction);
			Assert.assertEquals("route ways " + ways + ", restriction " + restriction, onlyRestriction, followsChain);
		}
	}

	private List<Long> calculateRouteWays(String map, LatLon start, LatLon end) throws Exception {
		File file = new File(MAPS_DIR + map);
		BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
		fe.CALCULATE_MISSING_MAPS = false;
		RoutingMemoryLimits memoryLimits = new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
				RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
		RoutingConfiguration config = RoutingConfiguration.getDefault().build("car", memoryLimits);
		RoutingContext ctx = fe.buildRoutingContext(config, null, new BinaryMapIndexReader[] {reader},
				RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		List<RouteSegmentResult> segments = fe.searchRoute(ctx, start, end, null).detailed;
		reader.close();
		Assert.assertNotNull("no route on " + map, segments);
		List<Long> ways = new ArrayList<>();
		for (RouteSegmentResult segment : segments) {
			long id = ObfConstants.getOsmObjectId(segment.getObject());
			if (ways.isEmpty() || ways.get(ways.size() - 1) != id) {
				ways.add(id);
			}
		}
		System.out.println(map + ": " + ways);
		return ways;
	}
}
