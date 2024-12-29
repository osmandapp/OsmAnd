package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static net.osmand.plus.settings.fragments.search.PluginsHelper.enablePlugin;
import static net.osmand.plus.settings.fragments.search.PluginsHelper.getPlugin;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.hasSearchResultWithSubstring;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchView;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SearchAndFindTest {

	private final Function<Context, String> searchQueryProvider;
	private final BiFunction<Context, Optional<OsmandPlugin>, String> searchResultProvider;
	private final Optional<Class<? extends OsmandPlugin>> pluginClass;

	public SearchAndFindTest(final Function<Context, String> searchQueryProvider,
							 final Optional<Class<? extends OsmandPlugin>> pluginClass,
							 final BiFunction<Context, Optional<OsmandPlugin>, String> searchResultProvider) {
		this.searchQueryProvider = searchQueryProvider;
		this.searchResultProvider = searchResultProvider;
		this.pluginClass = pluginClass;
	}

	public void testSearchAndFind(final OsmandApplication app) {
		// Given
		final Optional<OsmandPlugin> osmandPlugin = pluginClass.map(PluginsHelper::getPlugin);
		osmandPlugin.ifPresent(_osmandPlugin -> enablePlugin(_osmandPlugin, app));

		pluginClass.ifPresent(_pluginClass -> enablePlugin(getPlugin(_pluginClass), app));
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(searchQueryProvider.apply(app)), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchResultProvider.apply(app, osmandPlugin))));
	}
}
