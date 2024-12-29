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
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.test.common.Matchers.childAtPosition;
import static net.osmand.test.common.Matchers.recyclerViewHasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.content.Context;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

import org.hamcrest.Matcher;

import java.util.List;
import java.util.Optional;

// FK-TODO: rename to SettingsSearchWithPluginTestTemplate then create class SettingsSearchTestTemplate without getPluginClass() method
public abstract class SettingsSearchTestTemplate {

	public void testSearchAndFind(final OsmandApplication app) {
		// Given
		final Optional<OsmandPlugin> osmandPlugin = getOsmandPlugin();
		osmandPlugin.ifPresent(_osmandPlugin -> enablePlugin(_osmandPlugin, app));
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(getSearchQuery(app)), closeSoftKeyboard());

		// Then
		for (final String expectedSearchResult : getExpectedSearchResults(app, osmandPlugin)) {
			onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(expectedSearchResult)));
		}
	}

	protected abstract String getSearchQuery(final Context context);

	protected abstract Optional<Class<? extends OsmandPlugin>> getPluginClass();

	protected abstract List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin);

	private Optional<OsmandPlugin> getOsmandPlugin() {
		return this
				.getPluginClass()
				.map(PluginsHelper::getPlugin);
	}

	private static Matcher<View> searchView() {
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

	private static Matcher<View> searchResultsView() {
		return allOf(
				withId(SearchResultsFragmentUI.SEARCH_RESULTS_VIEW_ID),
				isDisplayed());
	}

	private static Matcher<View> hasSearchResultWithSubstring(final String substring) {
		return recyclerViewHasItem(hasDescendant(withSubstring(substring)));
	}
}
