package net.osmand.plus.helpers;

import net.osmand.plus.settings.enums.MapPosition;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MapDisplayPositionManagerTest {

	@Test
	public void testNavigationMapPosition_usesPlanRouteProviderOverBottomPreference() {
		assertEquals(
				MapPosition.MIDDLE_TOP,
				MapDisplayPositionManager.resolveNavigationMapPosition(MapPosition.MIDDLE_TOP, MapPosition.BOTTOM));
	}

	@Test
	public void testNavigationMapPosition_fallsBackToPreferenceWithoutProvider() {
		assertEquals(
				MapPosition.BOTTOM,
				MapDisplayPositionManager.resolveNavigationMapPosition(null, MapPosition.BOTTOM));
	}
}
