package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.List;

class SettingsSearchTestFactory {

	public static ISettingsSearchTest searchQueryAndExpectedSearchResult(final @StringRes int id) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return context.getString(id);
			}

			@Override
			protected List<String> getExpectedSearchResults(final Context context) {
				return List.of(context.getString(id));
			}
		};
	}

	public static ISettingsSearchTest searchQueryAndExpectedSearchResult(final String str) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return str;
			}

			@Override
			protected List<String> getExpectedSearchResults(final Context context) {
				return List.of(str);
			}
		};
	}
}
