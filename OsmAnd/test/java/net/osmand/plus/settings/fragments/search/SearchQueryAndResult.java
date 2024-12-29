package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import java.util.function.Function;

public class SearchQueryAndResult {

	private final Function<Context, String> searchQueryProvider;
	private final Function<Context, String> searchResultProvider;

	public SearchQueryAndResult(final Function<Context, String> searchQueryProvider,
								final Function<Context, String> searchResultProvider) {
		this.searchQueryProvider = searchQueryProvider;
		this.searchResultProvider = searchResultProvider;
	}

	public String getSearchQuery(final Context context) {
		return searchQueryProvider.apply(context);
	}

	public String getSearchResult(final Context context) {
		return searchResultProvider.apply(context);
	}
}
