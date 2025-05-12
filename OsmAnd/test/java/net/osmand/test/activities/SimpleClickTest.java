package net.osmand.test.activities;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import net.osmand.plus.activities.MapActivity; // Assuming MapActivity is your main map screen
import net.osmand.plus.R; // Assuming your resources are in net.osmand.plus
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.click;
import static net.osmand.test.common.OsmAndDialogInteractions.clickButtonWithId;
import static net.osmand.test.common.OsmAndDialogInteractions.clickButtonWithText;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf; // Required for combining matchers

/**
 * Test for enabling the POI overlay layer via the main menu and Configure Map screen.
 */
@RunWith(AndroidJUnit4.class)
public class SimpleClickTest extends AndroidTest {

	// Rule to launch the main activity before each test
	@Rule
	public ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class);

	@Test
	public void testEnablePoiOverlay() throws Throwable {
		skipAppStartDialogs(app);

		clickButtonWithId(R.id.map_menu_button);
		clickButtonWithText(R.string.configure_map);
//		clickButtonWithText(R.string.layer_map_appearance);

		// wait 5 second
        Thread.sleep(5000);
	}
}