package net.osmand.test.ui.poi;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.EspressoUtils.onDialogView;
import static net.osmand.test.common.EspressoUtils.waitForView;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.AutoCompleteTextView;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.test.common.AndroidTest;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

public class EditPoiAdvancedTagsTest extends AndroidTest {

	private static final String NAME = "Test name";

	@Rule
	public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class, true, false);

	@Rule
	public final GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(ACCESS_FINE_LOCATION);

	// The key field of a new tag loses focus while the list is laid out for the next new tag.
	// Applying a "name" key there must update the description item instead of crashing.
	@Test
	public void testNameKeyAppliedWhenNextTagAdded() throws Throwable {
		activityRule.launchActivity(null);
		skipAppStartDialogs(app);

		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		assertNotNull(osmEditingPlugin);
		MapActivity mapActivity = activityRule.getActivity();
		assertNotNull(mapActivity);
		app.runInUIThread(() -> PluginsHelper.enablePlugin(mapActivity, app, osmEditingPlugin, true));

		LatLon latLon = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		EditPoiDialogFragment.showAddPoiInstance(mapActivity, latLon.getLatitude(), latLon.getLongitude());

		onDialogView(withText(R.string.tab_title_advanced)).perform(click());
		addTagAndWaitForFocus();

		// Fill the value first and the key last, so the key is applied only when its field loses focus.
		onDialogView(withId(R.id.valueEditText)).perform(replaceText(NAME));
		View[] tagEditText = new View[1];
		onDialogView(focusedEmptyTagEditText()).check((view, error) -> {
			if (error != null) {
				throw error;
			}
			tagEditText[0] = view;
		});
		InstrumentationRegistry.getInstrumentation().runOnMainSync(
				() -> ((AutoCompleteTextView) tagEditText[0]).setText("name", false));
		onDialogView(isRoot()).perform(closeSoftKeyboard());

		addTagAndWaitForFocus();
		// the list scrolls to the new tag, which can take the description item with the name off the screen
		onDialogView(allOf(withId(R.id.content_recycler_view), isDisplayed()))
				.perform(scrollToPosition(0));
		onDialogView(allOf(withId(R.id.nameTextView), isDisplayed())).check(matches(withText(NAME)));
	}

	private void addTagAndWaitForFocus() throws Throwable {
		onDialogView(withId(R.id.addTagButton)).perform(click());
		// The new tag gets focus 300 ms later, from a delayed message Espresso does not wait for.
		// With the keyboard open the new tag stays below the visible part of the list, so it is not checked as displayed.
		waitForView(focusedEmptyTagEditText(), 3000);
	}

	private static Matcher<View> focusedEmptyTagEditText() {
		return allOf(withId(R.id.tagEditText), hasFocus(), withText(""));
	}
}
