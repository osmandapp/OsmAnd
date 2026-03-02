package net.osmand.plus.settings.fragments.search;

import android.content.res.AssetManager;
import android.os.PersistableBundle;

import androidx.fragment.app.FragmentActivity;

import com.google.common.graph.ImmutableValueGraph;

import net.osmand.plus.BuildConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.common.LanguageCode;
import de.KnollFrank.lib.settingssearch.common.graph.Tree;
import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabaseConfig;
import de.KnollFrank.lib.settingssearch.db.preference.db.PrepackagedPreferencesDatabase;
import de.KnollFrank.lib.settingssearch.db.preference.db.source.AssetDatabaseSourceProvider;
import de.KnollFrank.lib.settingssearch.db.preference.db.source.DatabaseSourceProvider;
import de.KnollFrank.lib.settingssearch.db.preference.db.source.DatabaseSourceProviders;
import de.KnollFrank.lib.settingssearch.db.preference.db.source.UrlDatabaseSourceProvider;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenTree;

public class PreferencesDatabaseConfigFactory {

	public static PreferencesDatabaseConfig<Configuration> createPreferencesDatabaseConfigForCreationOfPrepackagedDatabaseAssetFile() {
		return new PreferencesDatabaseConfig<>(
				"searchable_preferences.db",
				Optional.empty(),
				PreferencesDatabaseConfig.JournalMode.TRUNCATE);
	}

	public static PreferencesDatabaseConfig<Configuration> createPreferencesDatabaseConfigUsingPrepackagedDatabaseAssetFile(
			final LanguageCode languageCode,
			final AssetManager assetManager) {
		final File databaseAssetFile = new File(String.format("database/searchable_preferences_prepackaged_%s.db", languageCode.code()));
		return new PreferencesDatabaseConfig<>(
				databaseAssetFile.getName(),
				Optional.of(
						new PrepackagedPreferencesDatabase<>(
								createDatabaseSourceProvider(databaseAssetFile, assetManager),
								new SearchablePreferenceScreenTreeTransformer<>() {

									@Override
									public PersistableBundle getParams() {
										return new PersistableBundle();
									}

									@Override
									@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
									public Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> transformSearchablePreferenceScreenTree(
											final SearchablePreferenceScreenTree<Configuration> searchablePreferenceScreenTree,
											final Configuration targetConfiguration,
											final FragmentActivity activityContext) {
										// FK-TODO: implement by computing tree for targetConfiguration, d.h. targetConfiguration.enabledPlugins haben Preferences, die zum graph dazugefügt oder aus dem tree entfernt werden müssen.
										// siehe SettingsSearch: SearchDatabaseRootedAtPrefsFragmentFirstAdapter
										return searchablePreferenceScreenTree.tree();
									}
								})),
				PreferencesDatabaseConfig.JournalMode.AUTOMATIC);
	}

	private static DatabaseSourceProvider createDatabaseSourceProvider(final File databaseAssetFile,
																	   final AssetManager assetManager) {
		return DatabaseSourceProviders.firstAvailable(
				new AssetDatabaseSourceProvider(databaseAssetFile, assetManager),
				new UrlDatabaseSourceProvider(getDatabaseSourceUrl(databaseAssetFile.getName())));
	}

	private static URL getDatabaseSourceUrl(final String name) {
		return asUrl(String.format(BuildConfig.DATABASE_URL_TEMPLATE, name));
	}

	private static URL asUrl(final String url) {
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
