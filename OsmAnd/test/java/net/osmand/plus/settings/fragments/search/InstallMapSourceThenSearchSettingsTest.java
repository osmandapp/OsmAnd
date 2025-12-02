package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.mapMenuButton;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.searchButton;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.searchInsideDisabledProfilesCheckBox;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.settingsButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.hasSearchResultWithText;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;
import static net.osmand.test.common.Matchers.childAtPosition;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.View;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.collect.Sets;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.test.common.AndroidTest;

import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class InstallMapSourceThenSearchSettingsTest extends AndroidTest {

	@Rule
	public NonClosingActivityScenarioRule<MapActivity> nonClosingActivityScenarioRule = new NonClosingActivityScenarioRule<>(MapActivity.class);

	private long originalTimeout;
	private TimeUnit originalTimeUnit;

	// FK-TODO: remove increaseTimeout() and resetTimeout()
	@Before
	public void increaseTimeout() {
		originalTimeout = IdlingPolicies.getMasterIdlingPolicy().getIdleTimeout();
		originalTimeUnit = IdlingPolicies.getMasterIdlingPolicy().getIdleTimeoutUnit();
		IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES);
	}

	@After
	public void resetTimeout() {
		IdlingPolicies.setMasterPolicyTimeout(originalTimeout, originalTimeUnit);
	}

	@Test
	public void test_installMapSource_searchSettings_configureMapSearchResultFoundForEachApplicationMode() {
		// Given
		final String mapSourceName = "Microsoft Maps";
		skipAppStartDialogs(app);

		// When
		addMapSource(mapSourceName);

		// And
		onView(searchButton()).perform(click());
		onView(searchInsideDisabledProfilesCheckBox()).perform(click());
		onView(searchView()).perform(replaceText(mapSourceName), closeSoftKeyboard());

		// Then
		hasConfigureMapSearchResults(mapSourceName);
	}

	private static Matcher<View> navigateUpButton() {
		return allOf(
				withId(R.id.close_button),
				withContentDescription("Navigate up"),
				childAtPosition(
						childAtPosition(
								withClassName(is("android.widget.LinearLayout")),
								0),
						0),
				isDisplayed());
	}

	private static Matcher<View> backToMapButton() {
		return allOf(
				withId(R.id.toolbar_back),
				withContentDescription("Back to map"),
				childAtPosition(
						childAtPosition(
								withClassName(is("android.widget.LinearLayout")),
								0),
						0),
				isDisplayed());
	}

	private void addMapSource(final String mapSourceName) {
		PluginsHelper.enablePlugin(OsmandRasterMapsPlugin.class, app);
		onView(mapMenuButton()).perform(click());
		onView(settingsButton()).perform(click());
		clickDriving();
		clickConfigureMap();
		onView(mapSourceButton()).perform(click());
		onView(addMoreButton()).perform(click());
		onView(mapSourceButton(mapSourceName)).perform(click());
		onView(applyButton()).perform(scrollTo(), click());
		onView(backToMapButton()).perform(click());
		onView(navigateUpButton()).perform(click());
	}

	private static Matcher<View> applyButton() {
		return allOf(
				withId(android.R.id.button1),
				withText("Apply"),
				childAtPosition(
						childAtPosition(
								withId(me.zhanghai.android.materialprogressbar.R.id.buttonPanel),
								0),
						3));
	}

	private static Matcher<View> mapSourceButton(final String mapSourceName) {
		return allOf(
				withId(R.id.text),
				withText(mapSourceName),
				withParent(
						allOf(
								withId(R.id.button),
								withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout.class)))),
				isDisplayed());
	}

	private static void clickDriving() {
		getViewInteraction().perform(actionOnItemAtPosition(10, click()));
	}

	private static void clickConfigureMap() {
		getViewInteraction().perform(actionOnItemAtPosition(3, click()));
	}

	private static Matcher<View> mapSourceButton() {
		return allOf(
				childAtPosition(
						allOf(
								withId(R.id.items_container),
								childAtPosition(
										withClassName(is("android.widget.FrameLayout")),
										0)),
						6),
				isDisplayed());
	}

	private static Matcher<View> addMoreButton() {
		return allOf(
				withId(R.id.text),
				withText("Add more…"),
				withParent(
						allOf(
								withId(R.id.button),
								withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout.class)))),
				isDisplayed());
	}

	// FK-TODO: rename
	private static ViewInteraction getViewInteraction() {
		return onView(
				allOf(
						withId(R.id.recycler_view),
						childAtPosition(
								withId(android.R.id.list_container),
								0)));
	}

	private static void hasConfigureMapSearchResults(final String mapSourceName) {
		for (final ApplicationMode applicationMode : getApplicationModesWithoutDefault()) {
			hasConfigureMapSearchResult(applicationMode, mapSourceName);
		}
	}

	private static Set<ApplicationMode> getApplicationModesWithoutDefault() {
		return Sets.difference(
				new HashSet<>(ApplicationMode.allPossibleValues()),
				Set.of(ApplicationMode.DEFAULT));
	}

	private static void hasConfigureMapSearchResult(final ApplicationMode applicationMode, final String mapSourceName) {
		onView(searchResultsView()).check(matches(hasSearchResultWithText(getConfigureMapSearchResult(applicationMode, mapSourceName))));
	}

	private static String getConfigureMapSearchResult(final ApplicationMode applicationMode, final String mapSourceName) {
		return String.format(
				"Path: %s > Configure map > Map source… > %s",
				applicationMode.toHumanString(),
				mapSourceName);
	}
}
