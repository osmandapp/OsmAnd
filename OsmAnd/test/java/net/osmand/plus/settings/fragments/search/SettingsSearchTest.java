package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static net.osmand.plus.settings.fragments.search.PluginsHelper.enablePlugin;
import static net.osmand.plus.settings.fragments.search.PluginsHelper.getPlugin;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.test.common.Matchers.childAtPosition;
import static net.osmand.test.common.Matchers.recyclerViewHasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.test.common.AndroidTest;

import org.hamcrest.Matcher;
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
		clickSearchButton(app);

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
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText("speed cameras"), closeSoftKeyboard());

		// Then
		Stream
				.of("Driving", "Truck")
				.map(applicationMode -> String.format("Path: %s > Navigation settings > Screen alerts > Speed cameras", applicationMode))
				.forEach(path -> onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(path))));
	}

	@Test
	public void shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_AccessibilityPlugin() {
		shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_Plugin(AccessibilityPlugin.class);
	}

	@Test
	public void shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_AudioVideoNotesPlugin() {
		shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_Plugin(AudioVideoNotesPlugin.class);
	}

	@Test
	public void shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_OsmandMonitoringPlugin() {
		shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_Plugin(OsmandMonitoringPlugin.class);
	}

	@Test
	public void shouldSearchAndFind_LocationInterpolationBottomSheet_title() {
		enablePlugin(OsmandDevelopmentPlugin.class, app);
		shouldSearchAndFind(app.getString(R.string.location_interpolation_percent));
	}

	@Test
	public void shouldSearchAndFind_LocationInterpolationBottomSheet_description() {
		enablePlugin(OsmandDevelopmentPlugin.class, app);
		shouldSearchAndFind(app.getString(R.string.location_interpolation_percent_desc));
	}

	private void shouldSearchAndFind(final String searchQuery) {
		// Given
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(searchQuery), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchQuery)));
	}

	public static Matcher<View> searchView() {
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

	public static Matcher<View> searchResultsView() {
		return allOf(
				withId(SearchResultsFragmentUI.SEARCH_RESULTS_VIEW_ID),
				isDisplayed());
	}

	public static Matcher<View> hasSearchResultWithSubstring(final String substring) {
		return recyclerViewHasItem(hasDescendant(withSubstring(substring)));
	}

	private void shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_Plugin(final Class<? extends OsmandPlugin> pluginClass) {
		// Given
		final OsmandPlugin plugin = getPlugin(pluginClass);
		enablePlugin(plugin, app);
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(app.getString(R.string.reset_all_profile_settings_descr)), closeSoftKeyboard());

		// Then
		final String pathExpected = String.format("Path: Driving > %s > Reset plugin settings to default", plugin.getName());
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(pathExpected)));
	}
}
