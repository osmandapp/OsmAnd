package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.hasSearchResultWithSubstring;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;

import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.view.View;

import com.google.common.base.Function;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

abstract class SettingsSearchTestTemplate implements ISettingsSearchTest {

	@Override
	public void testSearchAndFind(final OsmandApplication app) {
		// Given
		// FK-TODO: refactor
		initializeTest(app);
		final Set<OsmandPlugin> enabledOsmandPlugins =
				getEnabledPluginClasses()
						.stream()
						.map(enabledPluginClass -> PluginsHelper.enablePlugin(enabledPluginClass, app))
						.collect(Collectors.toUnmodifiableSet());
		final Set<OsmandPlugin> disabledOsmandPlugins =
				getDisabledPluginClasses()
						.stream()
						.map(disabledPluginClass -> PluginsHelper.disablePlugin(disabledPluginClass, app))
						.collect(Collectors.toUnmodifiableSet());
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(getSearchQuery(app)), closeSoftKeyboard());

		// Then
		checkSearchResultsViewMatchesSearchResults(
				getExpectedSearchResults(app, enabledOsmandPlugins, disabledOsmandPlugins),
				SettingsSearchTestHelper::hasSearchResultWithSubstring);
		checkSearchResultsViewMatchesSearchResults(
				getForbiddenSearchResults(app, enabledOsmandPlugins, disabledOsmandPlugins),
				forbidden -> not(hasSearchResultWithSubstring(forbidden)));
	}

	protected void initializeTest(final OsmandApplication app) {
	}

	protected abstract String getSearchQuery(final Context context);

	protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
		return Collections.emptySet();
	}

	protected Set<Class<? extends OsmandPlugin>> getDisabledPluginClasses() {
		return Collections.emptySet();
	}

	protected List<String> getExpectedSearchResults(final Context context, final Set<OsmandPlugin> enabledOsmandPlugins, final Set<OsmandPlugin> disabledOsmandPlugins) {
		return Collections.emptyList();
	}

	protected List<String> getForbiddenSearchResults(final Context context, final Set<OsmandPlugin> enabledOsmandPlugins, final Set<OsmandPlugin> disabledOsmandPlugins) {
		return Collections.emptyList();
	}

	private static void checkSearchResultsViewMatchesSearchResults(final List<String> searchResults,
																   final Function<String, Matcher<View>> getMatcherForSearchResult) {
		for (final String searchResult : searchResults) {
			onView(searchResultsView()).check(matches(getMatcherForSearchResult.apply(searchResult)));
		}
	}
}
