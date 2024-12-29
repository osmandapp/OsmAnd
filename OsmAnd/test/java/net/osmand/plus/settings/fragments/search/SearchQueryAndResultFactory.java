package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.function.Function;

class SearchQueryAndResultFactory {

	public static SearchQueryAndResult searchQueryAndResult(final Function<Context, String> searchQueryProvider) {
		return new SearchQueryAndResult(searchQueryProvider, searchQueryProvider);
	}

	public static SearchQueryAndResult searchQueryAndResult(final @StringRes int queryId, final @StringRes int resultId) {
		return new SearchQueryAndResult(
				context -> context.getString(queryId),
				context -> context.getString(resultId));
	}

	public static SearchQueryAndResult searchQueryAndResult(final @StringRes int id) {
		return searchQueryAndResult(context -> context.getString(id));
	}

	public static SearchQueryAndResult searchQueryAndResult(final String str) {
		return searchQueryAndResult(context -> str);
	}
}
