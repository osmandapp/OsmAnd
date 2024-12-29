package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.annotation.StringRes;

import net.osmand.plus.plugins.OsmandPlugin;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

class SearchAndFindTestFactory {

	public static SettingsSearchAndFindTest searchQueryAndResult(
			final Function<Context, String> searchQueryProvider,
			final BiFunction<Context, Optional<OsmandPlugin>, List<String>> searchResultsProvider) {
		return new SettingsSearchAndFindTest(searchQueryProvider, Optional.empty(), searchResultsProvider);
	}

	public static SettingsSearchAndFindTest searchQueryAndResult(final Function<Context, String> searchQueryProvider) {
		return new SettingsSearchAndFindTest(
				searchQueryProvider,
				Optional.empty(),
				(context, osmandPlugin) -> List.of(searchQueryProvider.apply(context)));
	}

	public static SettingsSearchAndFindTest searchQueryAndResult(final @StringRes int queryId, final @StringRes int resultId) {
		return new SettingsSearchAndFindTest(
				context -> context.getString(queryId),
				Optional.empty(),
				(context, osmandPlugin) -> List.of(context.getString(resultId)));
	}

	public static SettingsSearchAndFindTest searchQueryAndResult(final @StringRes int id) {
		return searchQueryAndResult(context -> context.getString(id));
	}

	public static SettingsSearchAndFindTest searchQueryAndResult(final String str) {
		return searchQueryAndResult(context -> str);
	}
}
