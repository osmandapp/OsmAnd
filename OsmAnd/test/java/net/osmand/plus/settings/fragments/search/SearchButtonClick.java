package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.test.common.Matchers.childAtPosition;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.hamcrest.Matcher;

class SearchButtonClick {

	public static void clickSearchButton(final OsmandApplication osmandApplication) {
		skipAppStartDialogs(osmandApplication);
		onView(mapMenuButton()).perform(click());
		onView(settingsButton()).perform(click());
		onView(searchButton()).perform(click());
	}

	private static Matcher<View> mapMenuButton() {
		return allOf(
				withId(R.id.map_menu_button),
				withContentDescription("Back to menu"),
				childAtPosition(
						allOf(
								withId(R.id.map_hud_layout),
								childAtPosition(
										withId(R.id.map_hud_container),
										0)),
						10),
				isDisplayed());
	}

	private static Matcher<View> settingsButton() {
		return withText(R.string.shared_string_settings);
	}

	private static Matcher<View> searchButton() {
		return allOf(
				withId(R.id.action_button),
				childAtPosition(
						allOf(withId(R.id.actions_container),
								childAtPosition(
										withClassName(is("android.widget.LinearLayout")),
										2)),
						0),
				isDisplayed());
	}
}
