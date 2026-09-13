package net.osmand.test.ui.tracks;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.appearance.DefaultAppearanceController;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.test.common.AndroidTest;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Covers the mutual solid/non-solid exclusion between the "Color" and "Line style" cards
 * on the folder Default Appearance dialog. OpenGL rendering does not support a dash pattern
 * on a multi-colored (non-solid) line, so at most one of the two may be non-solid at a time:
 * picking a non-solid value on one card must force the other one back to "Solid" and hide/
 * disable its non-solid options.
 *
 * Both directions are exercised within a single dialog session (opened and closed exactly
 * once): between the two, the line style is reset back to "Original" via its own popup rather
 * than closing and reopening the dialog. Opening this particular dialog a second time - whether
 * from a fresh {@link ActivityScenarioRule} in another {@code @Test} method, or immediately
 * again in the same Activity - is flaky independently of the feature under test: the exact same
 * failure (the freshly reopened dialog's views are simply absent from the hierarchy) reproduces
 * with the pre-existing, unrelated {@code DefaultAppearanceLineStyleTest} when two dialog-opening
 * tests run back-to-back, and even reproduced for this test's own second re-open when it briefly
 * closed and reopened the dialog itself. Root cause looks like a race between
 * {@code DialogFragment.dismiss()} and the next {@code DialogFragment.show()} for the same tag
 * ({@code BaseFullScreenDialogFragment} / {@code DefaultAppearanceFragment.TAG}), not something
 * a single test can work around beyond avoiding the reopen.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class DefaultAppearanceColorLineStyleConstraintTest extends AndroidTest {

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void colorAndLineStyleSolidConstraintIsEnforcedInBothDirections() {
		skipAppStartDialogs(app);
		showDefaultAppearanceDialog();

		// --- Direction 1: picking a non-solid line style forces color to Solid ---
		selectFromCardPopup(lineStyleCardHeader(), R.string.gpx_line_style_dashed);

		onView(cardSelectorTitle(lineStyleCardHeader())).check(matches(withText(R.string.gpx_line_style_dashed)));
		onView(cardSelectorTitle(colorCardHeader())).check(matches(withText(R.string.track_coloring_solid)));

		onView(allOf(withId(R.id.card_selector), isDescendantOfA(colorCardHeader()))).perform(click());
		Espresso.onIdle();

		onView(allOf(withId(R.id.title), withText(R.string.track_coloring_solid)))
				.inRoot(isPlatformPopup())
				.check(matches(isDisplayed()));
		onView(allOf(withId(R.id.title), withText(R.string.shared_string_speed)))
				.inRoot(isPlatformPopup())
				.check(doesNotExist());
		onView(allOf(withId(R.id.title), withText(R.string.shared_string_original)))
				.inRoot(isPlatformPopup())
				.check(doesNotExist());

		// close the popup by re-confirming the only remaining option
		onView(allOf(withId(R.id.title), withText(R.string.track_coloring_solid)))
				.inRoot(isPlatformPopup())
				.perform(click());
		Espresso.onIdle();

		// reset the line style back to "Original" (instead of closing/reopening the whole
		// dialog - see the class doc) so color is unrestricted again for Direction 2
		selectFromCardPopup(lineStyleCardHeader(), R.string.shared_string_original);
		onView(cardSelectorTitle(lineStyleCardHeader())).check(matches(withText(R.string.shared_string_original)));

		// --- Direction 2: picking a non-solid color forces line style to Solid ---
		selectFromCardPopup(colorCardHeader(), R.string.shared_string_speed);

		onView(cardSelectorTitle(colorCardHeader())).check(matches(withText(R.string.shared_string_speed)));
		onView(cardSelectorTitle(lineStyleCardHeader())).check(matches(withText(R.string.gpx_line_style_solid)));

		// the inline style toggle must disable both non-solid options and keep "Solid" enabled
		onView(withContentDescription(app.getString(R.string.gpx_line_style_dashed)))
				.check(matches(allOf(isDescendantOfA(withId(R.id.custom_radio_buttons)), not(isEnabled()))));
		onView(withContentDescription(app.getString(R.string.gpx_line_style_dotted)))
				.check(matches(allOf(isDescendantOfA(withId(R.id.custom_radio_buttons)), not(isEnabled()))));
		onView(withContentDescription(app.getString(R.string.gpx_line_style_solid)))
				.check(matches(allOf(isDescendantOfA(withId(R.id.custom_radio_buttons)), isEnabled())));

		// the explanation text under the toggle must be shown
		onView(allOf(withId(R.id.description), hasSibling(withId(R.id.segmented_button))))
				.check(matches(allOf(isDisplayed(), withText(R.string.gpx_line_style_desc_unavailable_for_color))));

		onView(allOf(withId(R.id.card_selector), isDescendantOfA(lineStyleCardHeader()))).perform(click());
		Espresso.onIdle();

		onView(allOf(withId(R.id.title), withText(R.string.gpx_line_style_solid)))
				.inRoot(isPlatformPopup())
				.check(matches(isDisplayed()));
		onView(allOf(withId(R.id.title), withText(R.string.gpx_line_style_dashed)))
				.inRoot(isPlatformPopup())
				.check(doesNotExist());
		onView(allOf(withId(R.id.title), withText(R.string.gpx_line_style_dotted)))
				.inRoot(isPlatformPopup())
				.check(doesNotExist());
		onView(allOf(withId(R.id.title), withText(R.string.shared_string_original)))
				.inRoot(isPlatformPopup())
				.check(doesNotExist());

		// close the popup by re-confirming the only remaining option
		onView(allOf(withId(R.id.title), withText(R.string.gpx_line_style_solid)))
				.inRoot(isPlatformPopup())
				.perform(click());
		Espresso.onIdle();

		closeDialog();
	}

	private void closeDialog() {
		onView(withContentDescription(app.getString(R.string.shared_string_close))).perform(click());
		Espresso.onIdle();
	}

	private void showDefaultAppearanceDialog() {
		File dir = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR),
				"color_style_constraint_test_" + System.currentTimeMillis());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		TrackFolder folder = new TrackFolder(SharedUtil.kFile(dir), null);

		scenarioRule.getScenario().onActivity(activity -> DefaultAppearanceController.showDialog(activity, folder));
		Espresso.onIdle();
	}

	private void selectFromCardPopup(@NonNull Matcher<View> cardHeader, int titleTextId) {
		onView(allOf(withId(R.id.card_selector), isDescendantOfA(cardHeader))).perform(click());
		onView(allOf(withId(R.id.title), withText(titleTextId)))
				.inRoot(isPlatformPopup())
				.perform(click());
		Espresso.onIdle();
	}

	@NonNull
	private static Matcher<View> cardSelectorTitle(@NonNull Matcher<View> cardHeader) {
		return allOf(withId(R.id.title), isDescendantOfA(allOf(withId(R.id.card_selector), isDescendantOfA(cardHeader))));
	}

	@NonNull
	private static Matcher<View> colorCardHeader() {
		return allOf(withId(R.id.header),
				hasDescendant(allOf(withId(R.id.card_title), withText(R.string.shared_string_color))));
	}

	@NonNull
	private static Matcher<View> lineStyleCardHeader() {
		return allOf(withId(R.id.header),
				hasDescendant(allOf(withId(R.id.card_title), withText(R.string.gpx_line_style))));
	}
}
