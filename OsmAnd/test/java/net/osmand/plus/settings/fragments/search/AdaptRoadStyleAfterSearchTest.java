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
import static org.hamcrest.Matchers.allOf;

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
public class AdaptRoadStyleAfterSearchTest extends AndroidTest {

	@Rule
	public NonClosingActivityScenarioRule<MapActivity> nonClosingActivityScenarioRule = new NonClosingActivityScenarioRule<>(MapActivity.class);

	@Test
	public void shouldAdaptRoadStyleAfterSearch() {
		clickSearchButton(app);
		// When
		final String roadStyle = "American road atlas";
		onView(searchView()).perform(replaceText(roadStyle), closeSoftKeyboard());
		onView(searchResultsView()).perform(actionOnItemAtPosition(0, click()));

		// And
		roadStyleItemNamed(roadStyle).perform(click());

		// Then
		final var roadStyleSetting = roadStyleSettingHavingDescription(roadStyle);
		roadStyleSetting.perform(scrollTo());
		roadStyleSetting.check(matches(isDisplayed()));
	}

	private static ViewInteraction roadStyleItemNamed(final String roadStyle) {
		return onView(
				allOf(
						withId(R.id.text),
						withText(roadStyle),
						withParent(
								allOf(
										withId(R.id.button),
										withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout.class)))),
						isDisplayed()));
	}

	private static ViewInteraction roadStyleSettingHavingDescription(final String description) {
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
