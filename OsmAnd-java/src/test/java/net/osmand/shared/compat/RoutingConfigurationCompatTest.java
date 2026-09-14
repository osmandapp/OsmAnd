package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.osmand.shared.routing.GeneralRouter;
import net.osmand.shared.routing.RoutingConfiguration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RoutingConfiguration} is a copy of {@link net.osmand.router.RoutingConfiguration}, parser
 * included; this lets both read the bundled {@code routing.xml} and compares every profile, every
 * parameter and every configuration they build from it.
 */
public class RoutingConfigurationCompatTest {

	static net.osmand.router.RoutingConfiguration.Builder javaDefault() {
		return net.osmand.router.RoutingConfiguration.getDefault();
	}

	static RoutingConfiguration.Builder copyDefault() {
		return RoutingConfiguration.getDefault();
	}

	/** Parameter sets a configuration is built with: none, every switch on, every choice at its first value. */
	static List<Map<String, String>> variants(net.osmand.router.GeneralRouter router) {
		Map<String, String> switches = new LinkedHashMap<>();
		Map<String, String> choices = new LinkedHashMap<>();
		for (net.osmand.router.GeneralRouter.RoutingParameter p : router.getParameters().values()) {
			if (p.getType() == net.osmand.router.GeneralRouter.RoutingParameterType.BOOLEAN) {
				switches.put(p.getId(), "true");
			} else if (p.getPossibleValues() != null && p.getPossibleValues().length > 0) {
				choices.put(p.getId(), String.valueOf(p.getPossibleValues()[0]));
			}
		}
		List<Map<String, String>> variants = new ArrayList<>();
		variants.add(new LinkedHashMap<>());
		variants.add(switches);
		variants.add(choices);
		return variants;
	}

	@Test
	public void testProfilesAndParametersAgree() {
		net.osmand.router.RoutingConfiguration.Builder jb = javaDefault();
		RoutingConfiguration.Builder kb = copyDefault();
		assertEquals(jb.getDefaultRouter(), kb.getDefaultRouter());
		assertEquals(jb.getAllRouters().keySet(), kb.getAllRouters().keySet());
		assertEquals(jb.getAttributes(), kb.getAttributes());
		for (String name : jb.getAllRouters().keySet()) {
			net.osmand.router.GeneralRouter jr = jb.getRouter(name);
			GeneralRouter kr = kb.getRouter(name);
			assertNotNull(name, kr);
			assertRouter(name, jr, kr);
			assertEquals(name, jb.getRoutingProfileKeyByFileName(name), kb.getRoutingProfileKeyByFileName(name));
		}
	}

	@Test
	public void testBuiltConfigurationsAgree() {
		net.osmand.router.RoutingConfiguration.Builder jb = javaDefault();
		RoutingConfiguration.Builder kb = copyDefault();
		jb.addImpassableRoad(42);
		kb.addImpassableRoad(42);
		for (String name : jb.getAllRouters().keySet()) {
			for (Map<String, String> params : variants(jb.getRouter(name))) {
				String m = name + " " + params;
				net.osmand.router.RoutingConfiguration j = jb.build(name,
						new net.osmand.router.RoutingConfiguration.RoutingMemoryLimits(30, 256), params);
				RoutingConfiguration k = kb.build(name, new RoutingConfiguration.RoutingMemoryLimits(30, 256),
						new LinkedHashMap<>(params));
				assertEquals(m, j.routerName, k.routerName);
				assertEquals(m, j.attributes, k.attributes);
				assertEquals(m, j.heuristicCoefficient, k.heuristicCoefficient, 0f);
				assertEquals(m, j.ZOOM_TO_LOAD_TILES, k.ZOOM_TO_LOAD_TILES);
				assertEquals(m, j.memoryLimitation, k.memoryLimitation);
				assertEquals(m, j.memoryMaxHits, k.memoryMaxHits);
				assertEquals(m, j.nativeMemoryLimitation, k.nativeMemoryLimitation);
				assertEquals(m, j.planRoadDirection, k.planRoadDirection);
				assertEquals(m, j.initialDirection, k.initialDirection);
				assertEquals(m, j.targetDirection, k.targetDirection);
				assertEquals(m, j.penaltyForReverseDirection, k.penaltyForReverseDirection, 0d);
				assertEquals(m, j.recalculateDistance, k.recalculateDistance, 0f);
				assertEquals(m, j.routeCalculationTime, k.routeCalculationTime);
				assertEquals(m, j.ambiguousConditionalTags, k.ambiguousConditionalTags);
				assertEquals(m, j.MAX_VISITED, k.MAX_VISITED);
				assertEquals(m, j.altHorizon, k.altHorizon, 0d);
				assertEquals(m, j.directionPointsRadius, k.directionPointsRadius);
				assertEquals(m, j.minPointApproximation, k.minPointApproximation, 0f);
				assertEquals(m, j.minStepApproximation, k.minStepApproximation, 0f);
				assertEquals(m, j.maxStepApproximation, k.maxStepApproximation, 0f);
				assertEquals(m, j.smoothenPointsNoRoute, k.smoothenPointsNoRoute, 0f);
				assertEquals(m, j.showMinorTurns, k.showMinorTurns);
				assertRouter(m, j.router, k.router);
				assertEquals(m, j.router.getParameterValues(), k.router.getParameterValues());
				assertEquals(m, Arrays.toString(j.router.getImpassableRoadIds()), Arrays.toString(k.router.getImpassableRoadIds()));
				assertEquals(m, j.router.serializeParameterValues(params), k.router.serializeParameterValues(params));
			}
		}
	}

	static void assertRouter(String m, net.osmand.router.GeneralRouter j, GeneralRouter k) {
		assertEquals(m, j.getProfileName(), k.getProfileName());
		assertEquals(m, j.getProfile().name(), k.getProfile().name());
		assertEquals(m, j.getFilename(), k.getFilename());
		assertEquals(m, j.getHeightObstacles(), k.getHeightObstacles());
		assertEquals(m, j.restrictionsAware(), k.restrictionsAware());
		assertEquals(m, j.isAllowPrivate(), k.isAllowPrivate());
		assertEquals(m, j.getMinSpeed(), k.getMinSpeed(), 0f);
		assertEquals(m, j.getMaxSpeed(), k.getMaxSpeed(), 0f);
		assertEquals(m, j.getDefaultSpeed(), k.getDefaultSpeed(), 0f);
		for (Map.Entry<String, String> attribute : j.attributes.entrySet()) {
			assertEquals(m + " " + attribute.getKey(), true, k.containsAttribute(attribute.getKey()));
			assertEquals(m + " " + attribute.getKey(), attribute.getValue(), k.getAttribute(attribute.getKey()));
			String key = attribute.getKey();
			Same.outcome(m + " " + key, () -> j.getFloatAttribute(key, -1), () -> k.getFloatAttribute(key, -1));
			Same.outcome(m + " " + key, () -> j.getIntAttribute(key, -1), () -> k.getIntAttribute(key, -1));
		}
		assertEquals(m, false, k.containsAttribute("no such attribute"));
		assertEquals(m, j.getParameters().keySet(), k.getParameters().keySet());
		for (Map.Entry<String, net.osmand.router.GeneralRouter.RoutingParameter> e : j.getParameters().entrySet()) {
			net.osmand.router.GeneralRouter.RoutingParameter jp = e.getValue();
			GeneralRouter.RoutingParameter kp = k.getParameters().get(e.getKey());
			String p = m + " parameter " + e.getKey();
			assertEquals(p, jp.getId(), kp.getId());
			assertEquals(p, jp.getGroup(), kp.getGroup());
			assertEquals(p, jp.getName(), kp.getName());
			assertEquals(p, jp.getDescription(), kp.getDescription());
			assertEquals(p, jp.getType().name(), kp.getType().name());
			assertEquals(p, Arrays.toString(jp.getPossibleValues()), Arrays.toString(kp.getPossibleValues()));
			assertEquals(p, Arrays.toString(jp.getPossibleValueDescriptions()), Arrays.toString(kp.getPossibleValueDescriptions()));
			assertEquals(p, Arrays.toString(jp.getProfiles()), Arrays.toString(kp.getProfiles()));
			assertEquals(p, jp.getDefaultBoolean(), kp.getDefaultBoolean());
			assertEquals(p, jp.getDefaultNumeric(), kp.getDefaultNumeric(), 0d);
			assertEquals(p, jp.getDefaultString(), kp.getDefaultString());
		}
	}
}
