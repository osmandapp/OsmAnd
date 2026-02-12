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
		initializeTest(app);
		final Set<OsmandPlugin> enabledPlugins = enablePlugins(app);
		final Set<OsmandPlugin> disabledPlugins = disablePlugins(app);
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(getSearchQuery(app)), closeSoftKeyboard());

		// Then
		checkExpectedSearchResults(app, enabledPlugins, disabledPlugins);
		checkForbiddenSearchResults(app, enabledPlugins, disabledPlugins);
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

	protected List<String> getExpectedSearchResults(final Context context, final Set<OsmandPlugin> enabledPlugins, final Set<OsmandPlugin> disabledPlugins) {
		return Collections.emptyList();
	}

	protected List<String> getForbiddenSearchResults(final Context context, final Set<OsmandPlugin> enabledPlugins, final Set<OsmandPlugin> disabledPlugins) {
		return Collections.emptyList();
	}

	private Set<OsmandPlugin> enablePlugins(final OsmandApplication app) {
		return this
				.getEnabledPluginClasses()
				.stream()
				.map(enabledPluginClass -> PluginsHelper.enablePlugin(enabledPluginClass, app))
				.collect(Collectors.toUnmodifiableSet());
	}

	private Set<OsmandPlugin> disablePlugins(final OsmandApplication app) {
		return this
				.getDisabledPluginClasses()
				.stream()
				.map(disabledPluginClass -> PluginsHelper.disablePlugin(disabledPluginClass, app))
				.collect(Collectors.toUnmodifiableSet());
	}

	private void checkExpectedSearchResults(final OsmandApplication app, final Set<OsmandPlugin> enabledPlugins, final Set<OsmandPlugin> disabledPlugins) {
		checkSearchResultsViewMatchesSearchResults(
				getExpectedSearchResults(app, enabledPlugins, disabledPlugins),
				SettingsSearchTestHelper::hasSearchResultWithSubstring);
	}

	private void checkForbiddenSearchResults(final OsmandApplication app, final Set<OsmandPlugin> enabledPlugins, final Set<OsmandPlugin> disabledPlugins) {
		checkSearchResultsViewMatchesSearchResults(
				getForbiddenSearchResults(app, enabledPlugins, disabledPlugins),
				forbidden -> not(hasSearchResultWithSubstring(forbidden)));
	}

	public static void checkSearchResultsViewMatchesSearchResults(final List<String> searchResults,
																  final Function<String, Matcher<View>> getMatcherForSearchResult) {
		for (final String searchResult : searchResults) {
			onView(searchResultsView()).check(matches(getMatcherForSearchResult.apply(searchResult)));
		}
	}
}
