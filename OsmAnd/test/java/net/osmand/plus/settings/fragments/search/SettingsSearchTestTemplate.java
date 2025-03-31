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

import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.List;

abstract class SettingsSearchTestTemplate implements ISettingsSearchTest {

	@Override
	public void testSearchAndFind(final OsmandApplication app) {
		// Given
		initializeTest(app);
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(getSearchQuery(app)), closeSoftKeyboard());

		// Then
		checkSearchResultsViewMatchesSearchResults(
				getExpectedSearchResults(app),
				SettingsSearchTestHelper::hasSearchResultWithSubstring);
		checkSearchResultsViewMatchesSearchResults(
				getForbiddenSearchResults(app),
				forbidden -> not(hasSearchResultWithSubstring(forbidden)));
	}

	protected void initializeTest(final OsmandApplication app) {
	}

	protected abstract String getSearchQuery(final Context context);

	protected List<String> getExpectedSearchResults(final Context context) {
		return Collections.emptyList();
	}

	protected List<String> getForbiddenSearchResults(final Context context) {
		return Collections.emptyList();
	}

	static void checkSearchResultsViewMatchesSearchResults(final List<String> searchResults,
																   final Function<String, Matcher<View>> getMatcherForSearchResult) {
		for (final String searchResult : searchResults) {
			onView(searchResultsView()).check(matches(getMatcherForSearchResult.apply(searchResult)));
		}
	}
}
