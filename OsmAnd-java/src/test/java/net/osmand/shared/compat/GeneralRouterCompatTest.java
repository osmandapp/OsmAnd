package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.shared.routing.GeneralRouter;
import net.osmand.shared.routing.RouteDataObject;
import net.osmand.shared.routing.RoutingConfiguration;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * {@link GeneralRouter} is a copy of {@link net.osmand.router.GeneralRouter}; this is the cost model
 * of every profile in {@code routing.xml}, asked about every real road in the test obf files, with
 * the profile's parameters at their defaults, every switch on, and every choice at its first value.
 *
 * Speeds, obstacles, priorities and one-way answers are what the search ranks roads by, so a rule
 * evaluated differently here is a different route.
 */
public class GeneralRouterCompatTest {

	@Test
	public void testEveryProfileRanksEveryRoadTheSame() throws IOException {
		List<net.osmand.binary.RouteDataObject> roads = TestObf.roads(700);
		assertEquals(true, roads.size() > 2000);
		JavaToShared convert = new JavaToShared();
		net.osmand.router.RoutingConfiguration.Builder jb = RoutingConfigurationCompatTest.javaDefault();
		RoutingConfiguration.Builder kb = RoutingConfigurationCompatTest.copyDefault();
		for (String name : jb.getAllRouters().keySet()) {
			for (Map<String, String> params : RoutingConfigurationCompatTest.variants(jb.getRouter(name))) {
				net.osmand.router.GeneralRouter j = jb.build(name,
						new net.osmand.router.RoutingConfiguration.RoutingMemoryLimits(30, 256), params).router;
				GeneralRouter k = kb.build(name, new RoutingConfiguration.RoutingMemoryLimits(30, 256),
						new java.util.LinkedHashMap<>(params)).router;
				for (net.osmand.binary.RouteDataObject jr : roads) {
					RouteDataObject kr = convert.road(jr);
					String m = name + " " + params + " road " + jr.id;
					assertEquals(m, j.acceptLine(jr), k.acceptLine(kr));
					assertEquals(m, j.isOneWay(jr), k.isOneWay(kr));
					assertEquals(m, j.isArea(jr), k.isArea(kr));
					assertEquals(m, j.getPenaltyTransition(jr), k.getPenaltyTransition(kr), 0f);
					assertEquals(m, j.defineDestinationPriority(jr), k.defineDestinationPriority(kr), 0f);
					for (boolean dir : new boolean[] {false, true}) {
						assertEquals(m + " dir " + dir, j.defineRoutingSpeed(jr, dir), k.defineRoutingSpeed(kr, dir), 0f);
						assertEquals(m + " dir " + dir, j.defineVehicleSpeed(jr, dir), k.defineVehicleSpeed(kr, dir), 0f);
						assertEquals(m + " dir " + dir, j.defineSpeedPriority(jr, dir), k.defineSpeedPriority(kr, dir), 0f);
					}
					int n = jr.getPointsLength();
					for (int i = 0; i < n; i++) {
						for (boolean back : new boolean[] {false, true}) {
							assertEquals(m + " point " + i, j.defineObstacle(jr, i, back), k.defineObstacle(kr, i, back), 0f);
							assertEquals(m + " point " + i, j.defineRoutingObstacle(jr, i, back), k.defineRoutingObstacle(kr, i, back), 0f);
						}
					}
					if (n > 1) {
						assertEquals(m, j.defineHeightObstacle(jr, (short) 0, (short) (n - 1)), k.defineHeightObstacle(kr, (short) 0, (short) (n - 1)), 0d);
						assertEquals(m, j.defineHeightObstacle(jr, (short) (n - 1), (short) 0), k.defineHeightObstacle(kr, (short) (n - 1), (short) 0), 0d);
					}
				}
			}
		}
	}
}
