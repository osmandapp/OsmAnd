package net.osmand.test.activities;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static net.osmand.test.common.Matchers.childAtPosition;
import static net.osmand.test.common.Matchers.recyclerViewHasItem;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import android.view.View;

import androidx.test.espresso.DataInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.test.common.AndroidTest;

import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.Stream;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SettingsSearchTest extends AndroidTest {

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void shouldSearchAndFindProfileAppearanceSettings4EachApplicationMode() {
		// Given
		clickSearchButton();

		// When
		onView(searchView()).perform(replaceText("profile appearance"), closeSoftKeyboard());

		// Then
		Stream
				.of("Driving", "Moped")
				.map(applicationMode -> String.format("Path: %s > Profile appearance", applicationMode))
				.forEach(path -> onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(path))));
	}

	@Test
	public void shouldSearchAndFindSpeedCameraSettings4EachApplicationMode() {
		// Given
		clickSearchButton();

		// When
		onView(searchView()).perform(replaceText("speed cameras"), closeSoftKeyboard());

		// Then
		Stream
				.of("Driving", "Truck")
				.map(applicationMode -> String.format("Path: %s > Navigation settings > Screen alerts > Speed cameras", applicationMode))
				.forEach(path -> onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(path))));
	}

	@Test
	public void shouldSearchAndFindRecalculateRoutePreference() {
		// Given
		final String searchQuery = "Recalculate route";
		clickSearchButton();

		// When
		onView(searchView()).perform(replaceText(searchQuery), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchQuery)));
	}

	private void clickSearchButton() {
		skipAppStartDialogs(app);
		onView(mapMenuButton()).perform(click());
		settingsButton().perform(click());
		onView(searchButton()).perform(click());
	}

	private static Matcher<View> mapMenuButton() {
		return allOf(
				withId(R.id.map_menu_button),
				withContentDescription("Back to menu"),
				childAtPosition(
						childAtPosition(
								withId(R.id.bottom_controls_container),
								3),
						0),
				isDisplayed());
	}

	private static DataInteraction settingsButton() {
		return onData(anything())
				.inAdapterView(
						allOf(
								withId(R.id.menuItems),
								childAtPosition(
										withId(R.id.drawer_relative_layout),
										0)))
				.atPosition(13);
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

	private static Matcher<View> searchView() {
		return allOf(
				withClassName(is("android.widget.SearchView$SearchAutoComplete")),
				childAtPosition(
						allOf(
								withClassName(is("android.widget.LinearLayout")),
								childAtPosition(
										withClassName(is("android.widget.LinearLayout")),
										1)),
						0),
				isDisplayed());
	}

	private static Matcher<View> searchResultsView() {
		return allOf(
				withId(androidx.preference.R.id.recycler_view),
				withParent(
						allOf(
								withId(android.R.id.list_container),
								withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout.class)))),
				isDisplayed());
	}

	private static Matcher<View> hasSearchResultWithSubstring(final String substring) {
		return recyclerViewHasItem(hasDescendant(withSubstring(substring)));
	}
}
