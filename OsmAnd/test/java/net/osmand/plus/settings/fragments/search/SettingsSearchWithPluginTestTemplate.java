package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.hasSearchResultWithSubstring;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

import java.util.List;

abstract class SettingsSearchWithPluginTestTemplate implements ISettingsSearchTest {

	@Override
	public void testSearchAndFind(final OsmandApplication app) {
		// Given
		final OsmandPlugin osmandPlugin = PluginsHelper.enablePlugin(getPluginClass(), app);
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(getSearchQuery(app)), closeSoftKeyboard());

		// Then
		for (final String expectedSearchResult : getExpectedSearchResults(app, osmandPlugin)) {
			onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(expectedSearchResult)));
		}
	}

	protected abstract String getSearchQuery(final Context context);

	protected abstract Class<? extends OsmandPlugin> getPluginClass();

	protected abstract List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin);
}
