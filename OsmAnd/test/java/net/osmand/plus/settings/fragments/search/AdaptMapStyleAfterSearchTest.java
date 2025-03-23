package net.osmand.plus.settings.fragments.search;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;
import static net.osmand.test.common.Matchers.childAtPosition;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.test.common.AndroidTest;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AdaptMapStyleAfterSearchTest extends AndroidTest {

	@Rule
	public NonClosingActivityScenarioRule<MapActivity> nonClosingActivityScenarioRule = new NonClosingActivityScenarioRule<>(MapActivity.class);

	@Test
	public void shouldAdaptMapStyleAfterSearch() {
		clickSearchButton(app);
		// When
		final String mapStyle = "Desert";
		onView(searchView()).perform(replaceText(mapStyle), closeSoftKeyboard());
		onView(searchResultsView()).perform(actionOnItemAtPosition(0, click()));

		// And
		desertItem().perform(scrollTo(), click());
		applyButton().perform(click());

		// Then
		final var mapStyleSetting = mapStyleSettingHavingDescription(mapStyle);
		mapStyleSetting.perform(scrollTo());
		mapStyleSetting.check(matches(withText(mapStyle)));
	}

	private static ViewInteraction desertItem() {
		final int indexOfDesertItem = 1;
		return onView(
				childAtPosition(
						childAtPosition(
								withClassName(is("androidx.core.widget.NestedScrollView")),
								0),
						indexOfDesertItem));
	}

	private static ViewInteraction applyButton() {
		return onView(
				allOf(
						withId(R.id.button_wrapper),
						childAtPosition(
								allOf(
										withId(R.id.right_bottom_button),
										childAtPosition(
												withId(R.id.buttons_container),
												2)),
								0),
						isDisplayed()));
	}

	private static ViewInteraction mapStyleSettingHavingDescription(final String description) {
		return onView(
				allOf(
						withId(R.id.description),
						withText(description),
						withParent(
								allOf(
										withId(R.id.text_container),
										withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout.class))))));
	}
}
