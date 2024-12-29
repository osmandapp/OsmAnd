package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.annotation.StringRes;

import net.osmand.plus.plugins.OsmandPlugin;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

// FK-TODO: inline methods
class SettingsSearchTestFactory {

	public static SettingsSearchTestTemplate searchQueryAndResult(
			final Function<Context, String> searchQueryProvider,
			final BiFunction<Context, Optional<OsmandPlugin>, List<String>> searchResultsProvider) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return searchQueryProvider.apply(context);
			}

			@Override
			protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
				return Optional.empty();
			}

			@Override
			protected List<String> getSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
				return searchResultsProvider.apply(context, osmandPlugin);
			}
		};
	}

	public static SettingsSearchTestTemplate searchQueryAndResult(final Function<Context, String> searchQueryProvider) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return searchQueryProvider.apply(context);
			}

			@Override
			protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
				return Optional.empty();
			}

			@Override
			protected List<String> getSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
				return List.of(searchQueryProvider.apply(context));
			}
		};
	}

	public static SettingsSearchTestTemplate searchQueryAndResult(final @StringRes int queryId, final @StringRes int resultId) {
		return new SettingsSearchTestTemplate() {

			@Override
			protected String getSearchQuery(final Context context) {
				return context.getString(queryId);
			}

			@Override
			protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
				return Optional.empty();
			}

			@Override
			protected List<String> getSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
				return List.of(context.getString(resultId));
			}
		};
	}

	public static SettingsSearchTestTemplate searchQueryAndResult(final @StringRes int id) {
		return searchQueryAndResult(context -> context.getString(id));
	}

	public static SettingsSearchTestTemplate searchQueryAndResult(final String str) {
		return searchQueryAndResult(context -> str);
	}
}
