package net.osmand.plus.views.mapwidgets.configure;

import net.osmand.plus.settings.enums.ScreenLayoutMode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WidgetsSettingsHelperTest {

	@Test
	public void testGetLayoutModeForProfileCopy_keepsPortraitSeparateLayout() {
		assertEquals(
				ScreenLayoutMode.PORTRAIT,
				WidgetsSettingsHelper.getLayoutModeForProfileCopy(ScreenLayoutMode.PORTRAIT));
	}

	@Test
	public void testGetLayoutModeForProfileCopy_keepsLandscapeSeparateLayout() {
		assertEquals(
				ScreenLayoutMode.LANDSCAPE,
				WidgetsSettingsHelper.getLayoutModeForProfileCopy(ScreenLayoutMode.LANDSCAPE));
	}

	@Test
	public void testGetLayoutModeForProfileCopy_keepsSharedLayout() {
		assertNull(WidgetsSettingsHelper.getLayoutModeForProfileCopy(null));
	}
}
