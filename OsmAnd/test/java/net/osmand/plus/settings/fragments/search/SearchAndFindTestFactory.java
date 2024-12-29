package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.annotation.StringRes;

import net.osmand.plus.plugins.OsmandPlugin;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

class SearchAndFindTestFactory {

	public static SearchAndFindTest searchQueryAndResult(
			final Function<Context, String> searchQueryProvider,
			final BiFunction<Context, Optional<OsmandPlugin>, String> searchResultProvider) {
		return new SearchAndFindTest(searchQueryProvider, Optional.empty(), searchResultProvider);
	}

	public static SearchAndFindTest searchQueryAndResult(final Function<Context, String> searchQueryProvider) {
		return new SearchAndFindTest(
				searchQueryProvider,
				Optional.empty(),
				(context, osmandPlugin) -> searchQueryProvider.apply(context));
	}

	public static SearchAndFindTest searchQueryAndResult(final @StringRes int queryId, final @StringRes int resultId) {
		return new SearchAndFindTest(
				context -> context.getString(queryId),
				Optional.empty(),
				(context, osmandPlugin) -> context.getString(resultId));
	}

	public static SearchAndFindTest searchQueryAndResult(final @StringRes int id) {
		return searchQueryAndResult(context -> context.getString(id));
	}

	public static SearchAndFindTest searchQueryAndResult(final String str) {
		return searchQueryAndResult(context -> str);
	}
}
