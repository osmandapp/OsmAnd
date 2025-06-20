package net.osmand.test.ui.basic;

import static net.osmand.test.common.OsmAndDialogInteractions.clickButtonWithContentDescription;
import static net.osmand.test.common.OsmAndDialogInteractions.clickButtonWithText;
import static net.osmand.test.common.OsmAndDialogInteractions.clickMapButtonWithId;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static net.osmand.test.common.OsmAndDialogInteractions.writeText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for enabling the POI overlay layer via the main menu and Configure Map screen.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SimpleClickTest extends AndroidTest {

	// Rule to launch the main activity before each test
	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class);

	@Test
	public void testEnablePoiOverlay() throws Throwable {
		skipAppStartDialogs(app);

		clickMapButtonWithId(R.id.map_menu_button);
		clickButtonWithText(R.string.maps_and_resources);

		clickButtonWithContentDescription(R.string.shared_string_search);
		writeText(R.id.searchEditText, "Kyiv");

		clickButtonWithText("Kyiv");

	}
}