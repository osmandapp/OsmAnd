package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.List;
import java.util.function.Function;

// FK-TODO: inline methods
class SettingsSearchTestFactory {

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

	public static SettingsSearchTest searchQueryAndResult(final @StringRes int id) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context1) {
				return context1.getString(id);
			}

			@Override
			protected List<String> getExpectedSearchResults(final Context context1) {
				return List.of(context1.getString(id));
			}
		};
	}

	public static SettingsSearchTest searchQueryAndResult(final String str) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context1) {
				return str;
			}

			@Override
			protected List<String> getExpectedSearchResults(final Context context1) {
				return List.of(str);
			}
		};
	}
}
