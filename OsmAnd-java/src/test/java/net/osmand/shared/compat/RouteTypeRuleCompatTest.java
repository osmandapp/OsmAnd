package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.shared.routing.RouteTypeRule;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * {@link RouteTypeRule} is a copy of {@link net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule};
 * this builds both from the same tag and value and compares everything a rule tells the router.
 *
 * The tags and values are every encoding rule of every routing region in the test obf files - the
 * real vocabulary, conditionals and all - plus a handful of shapes that are legal but rare.
 */
public class RouteTypeRuleCompatTest {

	private static final String[][] RARE = {
			{"maxspeed", "50"}, {"maxspeed", "RO:urban"}, {"maxspeed", "none"}, {"maxspeed", "30 mph"},
			{"maxspeed", "junk"}, {"maxspeed", ""},
			{"maxspeed:conditional", "50 @ (22:00-06:00)"},
			{"maxspeed:conditional", "30 @ (Mo-Fr 07:00-09:00); 50 @ (Sa,Su)"},
			{"maxspeed:conditional", "junk @ ()"},
			{"maxspeed:hgv", "60"}, {"maxspeed:forward", "70"}, {"maxspeed:backward", "40"},
			{"maxspeed:hgv:forward", "60"},
			{"lanes", "2"}, {"lanes", "2;3"}, {"lanes", "junk"}, {"lanes", ""},
			{"oneway", "yes"}, {"oneway", "-1"}, {"oneway", "no"}, {"oneway", "reversible"}, {"oneway", null},
			{"junction", "roundabout"}, {"roundabout", "yes"},
			{"highway", "motorway"}, {"highway", "junk"}, {"highway", null},
			{"access:conditional", "no @ (Mo-Fr 07:00-09:00)"},
			{"hgv:conditional", "no @ (22:00-06:00)"},
			{"traffic_signals", "signal"}, {"name", "Main street"}, {"tunnel", "yes"},
			{"layer", "-1"}, {"maxheight", "3.5"}, {"maxweight", "7.5 t"}, {"width", "2.5 m"},
			{"area", null}, {"", ""},
	};

	private static List<String[]> corpus() throws IOException {
		List<String[]> pairs = new ArrayList<>();
		for (RouteRegion region : TestObf.regions()) {
			for (net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule rule : region.routeEncodingRules) {
				if (rule != null) {
					pairs.add(new String[] {rule.getTag(), rule.getValue()});
				}
			}
		}
		for (String[] rare : RARE) {
			pairs.add(rare);
		}
		return pairs;
	}

	private static long[] moments() {
		long[] times = new long[6];
		Calendar cal = Calendar.getInstance();
		int[][] when = {{2024, Calendar.JANUARY, 15, 8, 30}, {2024, Calendar.JUNE, 15, 23, 30},
				{2024, Calendar.DECEMBER, 25, 12, 0}, {2024, Calendar.MARCH, 3, 0, 0},
				{2025, Calendar.JULY, 4, 7, 59}, {2022, Calendar.SEPTEMBER, 30, 18, 1}};
		for (int i = 0; i < when.length; i++) {
			cal.clear();
			cal.set(when[i][0], when[i][1], when[i][2], when[i][3], when[i][4], 0);
			times[i] = cal.getTimeInMillis();
		}
		return times;
	}

	@Test
	public void testEveryRuleAgrees() throws IOException {
		List<String[]> corpus = corpus();
		assertEquals(true, corpus.size() > 1000);
		long[] moments = moments();
		for (String[] pair : corpus) {
			String m = pair[0] + "=" + pair[1];
			net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule j =
					new net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule(pair[0], pair[1]);
			RouteTypeRule k = new RouteTypeRule(pair[0], pair[1]);
			assertEquals(m, j.getTag(), k.getTag());
			assertEquals(m, j.getValue(), k.getValue());
			assertEquals(m, j.getType(), k.getType());
			assertEquals(m, j.isForward(), k.isForward());
			assertEquals(m, j.roundabout(), k.roundabout());
			assertEquals(m, j.conditional(), k.conditional());
			assertEquals(m, j.getNonConditionalTag(), k.getNonConditionalTag());
			assertEquals(m, j.onewayDirection(), k.onewayDirection());
			assertEquals(m, j.getMaxIntegerConditionalValue(), k.getMaxIntegerConditionalValue());
			assertEquals(m, j.lanes(), k.lanes());
			assertEquals(m, j.highwayRoad(), k.highwayRoad());
			assertEquals(m, j.toString(), k.toString());
			for (int profile : new int[] {RouteTypeRule.PROFILE_NONE, RouteTypeRule.PROFILE_CAR, RouteTypeRule.PROFILE_TRUCK}) {
				assertEquals(m + " profile " + profile, j.maxSpeed(profile), k.maxSpeed(profile), 0f);
			}
			for (long t : moments) {
				assertEquals(m + " at " + t, j.conditionalValue(t), k.conditionalValue(t));
			}
		}
	}
}
