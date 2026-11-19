package net.osmand.test.ui.mapcontextmenu;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.EspressoUtils.waitForView;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.osmand.data.LatLon;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WptPtCreationTimeTest extends AndroidTest {

	@Rule
	public final ActivityTestRule<MapActivity> activityRule =
			new ActivityTestRule<>(MapActivity.class, true, false);

	@Test
	public void testCreatedOnExtensionIsDisplayedInsteadOfTime() throws Throwable {
		activityRule.launchActivity(null);
		skipAppStartDialogs(app);
		MapActivity mapActivity = activityRule.getActivity();
		assertNotNull(mapActivity);

		long time = GpxUtilities.INSTANCE.parseTime("2024-02-03T04:05:06Z");
		long createdOn = GpxUtilities.INSTANCE.parseTime("2024-01-01T00:00:00Z");
		WptPt wpt = new WptPt(50.45, 30.51, time, Double.NaN, 0f, Float.NaN);
		wpt.setCreatedOn(createdOn);

		LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());
		app.runInUIThread(() -> mapActivity.getContextMenu().show(latLon,
				new WptLocationPoint(wpt).getPointDescription(mapActivity), wpt));

		waitForView(withId(R.id.context_menu_layout));
		// The menu opens header-only in portrait; expand it so the rows are on screen.
		app.runInUIThread(() -> mapActivity.getContextMenu().openMenuFullScreen());

		String expected = formatDate(mapActivity, createdOn);
		String unexpected = formatDate(mapActivity, time);
		onView(withText(expected)).check(matches(isDisplayed()));
		onView(withText(unexpected)).check(doesNotExist());
	}

	private String formatDate(Context context, long timestamp) {
		java.text.DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
		java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
		Date date = new Date(timestamp);
		return dateFormat.format(date) + " — " + timeFormat.format(date);
	}
}
