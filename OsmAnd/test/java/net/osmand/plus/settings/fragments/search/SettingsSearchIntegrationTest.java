package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onData;
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
import static org.hamcrest.Matchers.anything;

import androidx.test.espresso.DataInteraction;
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
public class SettingsSearchIntegrationTest extends AndroidTest {

	@Rule
	public NonClosingActivityScenarioRule<MapActivity> nonClosingActivityScenarioRule = new NonClosingActivityScenarioRule<>(MapActivity.class);

	// FK-TODO: add test shouldAdaptMapStyleAfterSearch(), use "winter and ski"
	// FK-TODO: add test shouldAdaptRoadStyleAfterSearch(), use "american road"
	@Test
	public void shouldAdaptMapLanguageAfterSearch() {
		clickSearchButton(app);
		// When
		onView(searchView()).perform(replaceText("Afrikaans"), closeSoftKeyboard());
		onView(searchResultsView()).perform(actionOnItemAtPosition(0, click()));

		// And
		afrikaansItem().perform(click());
		applyButton().perform(scrollTo(), click());

		// Then
		final ViewInteraction mapLanguageItem = mapLanguageItemHavingDescription("af");
		mapLanguageItem.perform(scrollTo());
		mapLanguageItem.check(matches(isDisplayed()));
	}

	private static DataInteraction afrikaansItem() {
		return onData(anything())
				.inAdapterView(
						allOf(
								withId(me.zhanghai.android.materialprogressbar.R.id.select_dialog_listview),
								childAtPosition(
										withId(me.zhanghai.android.materialprogressbar.R.id.contentPanel),
										0)))
				.atPosition(2);
	}

	private static ViewInteraction applyButton() {
		return onView(
				allOf(
						withId(android.R.id.button1),
						withText("Apply"),
						childAtPosition(
								childAtPosition(
										withId(me.zhanghai.android.materialprogressbar.R.id.buttonPanel),
										0),
								3)));
	}

	private static ViewInteraction mapLanguageItemHavingDescription(final String description) {
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
