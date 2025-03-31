package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.hasSearchResultWithSubstring;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestTemplate.checkSearchResultsViewMatchesSearchResults;

import static org.hamcrest.Matchers.not;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

abstract class SettingsSearchWithPluginTestTemplate implements ISettingsSearchTest {

	@Override
	public void testSearchAndFind(final OsmandApplication app) {
		// Given
		// FK-TODO: refactor
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

	protected abstract String getSearchQuery(final Context context);

	protected abstract Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses();

	protected abstract Set<Class<? extends OsmandPlugin>> getDisabledPluginClasses();

	protected abstract List<String> getExpectedSearchResults(final Context context, final Set<OsmandPlugin> enabledOsmandPlugins, final Set<OsmandPlugin> disabledOsmandPlugins);

	protected abstract List<String> getForbiddenSearchResults(final Context context, final Set<OsmandPlugin> enabledOsmandPlugins, final Set<OsmandPlugin> disabledOsmandPlugins);
}
