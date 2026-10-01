package net.osmand.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.osmand.shared.routing.GeneralRouter;
import net.osmand.shared.routing.GeneralRouterProfile;
import net.osmand.shared.routing.RoutingConfiguration;

import org.junit.Test;

/**
 * The one thing about {@link RoutingConfiguration} that only this module can check.
 *
 * The class lives in OsmAnd-shared now, but the `routing.xml` it parses by default does not: this
 * module's `collectRoutingResources` task copies it out of the `resources` repository into
 * `net/osmand/router/`, and the shared class looks it up at that path. Nothing fails to compile if
 * the two drift apart - routing just stops having a profile.
 */
public class RoutingConfigurationDefaultTest {

	@Test
	public void testDefaultConfigurationIsParsedFromTheBundledRoutingXml() {
		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		assertNotNull(builder);
		assertEquals("car", builder.getDefaultRouter());
		assertFalse(builder.getAllRouters().isEmpty());

		GeneralRouter car = builder.getRouter("car");
		assertNotNull("routing.xml must declare a car profile", car);
		assertEquals(GeneralRouterProfile.CAR, car.getProfile());
		assertTrue("the car profile must carry rules", car.getMaxSpeed() > 0);
	}

	@Test
	public void testDefaultConfigurationIsParsedOnce() {
		assertSame(RoutingConfiguration.getDefault(), RoutingConfiguration.getDefault());
	}
}
