package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.List;
import java.util.function.Function;

// FK-TODO: inline methods
class SettingsSearchTestFactory {

	public static SettingsSearchTest searchQueryAndResult(
			final Function<Context, String> searchQueryProvider,
			final Function<Context, List<String>> searchResultsProvider) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return searchQueryProvider.apply(context);
			}

			@Override
			protected List<String> getExpectedSearchResults(final Context context) {
				return searchResultsProvider.apply(context);
			}
		};
	}

	public static SettingsSearchTest searchQueryAndResult(final Function<Context, String> searchQueryProvider) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return searchQueryProvider.apply(context);
			}

			@Override
			protected List<String> getExpectedSearchResults(final Context context) {
				return List.of(searchQueryProvider.apply(context));
			}
		};
	}

	public static SettingsSearchTest searchQueryAndResult(final @StringRes int queryId, final @StringRes int resultId) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return context.getString(queryId);
			}

			@Override
			protected List<String> getExpectedSearchResults(final Context context) {
				return List.of(context.getString(resultId));
			}
		};
	}

	public static SettingsSearchTest searchQueryAndResult(final @StringRes int id) {
		return searchQueryAndResult(context -> context.getString(id));
	}

	public static SettingsSearchTest searchQueryAndResult(final String str) {
		return searchQueryAndResult(context -> str);
	}
}
