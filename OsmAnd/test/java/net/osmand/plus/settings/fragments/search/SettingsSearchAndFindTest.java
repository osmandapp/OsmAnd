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

import android.content.Context;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

import org.hamcrest.Matcher;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SettingsSearchAndFindTest {

	private final Function<Context, String> searchQueryProvider;
	private final BiFunction<Context, Optional<OsmandPlugin>, List<String>> searchResultsProvider;
	private final Optional<Class<? extends OsmandPlugin>> pluginClass;

	public SettingsSearchAndFindTest(final Function<Context, String> searchQueryProvider,
									 final Optional<Class<? extends OsmandPlugin>> pluginClass,
									 final BiFunction<Context, Optional<OsmandPlugin>, List<String>> searchResultsProvider) {
		this.searchQueryProvider = searchQueryProvider;
		this.searchResultsProvider = searchResultsProvider;
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
		for (final String searchResult : searchResultsProvider.apply(app, osmandPlugin)) {
			onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchResult)));
		}
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
