package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabaseConfig;
import de.KnollFrank.lib.settingssearch.db.preference.db.PrepackagedPreferencesDatabase;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenTree;

public class PreferencesDatabaseConfigFactory {

	public static final String SEARCHABLE_PREFERENCES_DB = "searchable_preferences.db";

	public static PreferencesDatabaseConfig<Configuration> createPreferencesDatabaseConfigForCreationOfPrepackagedDatabaseAssetFile() {
		return new PreferencesDatabaseConfig<>(
				SEARCHABLE_PREFERENCES_DB,
				Optional.empty(),
				PreferencesDatabaseConfig.JournalMode.TRUNCATE);
	}

	public static PreferencesDatabaseConfig<Configuration> createPreferencesDatabaseConfigUsingPrepackagedDatabaseAssetFile() {
		return new PreferencesDatabaseConfig<>(
				SEARCHABLE_PREFERENCES_DB,
				Optional.of(
						new PrepackagedPreferencesDatabase<>(
								new File("database/searchable_preferences_prepackaged.db"),
								new SearchablePreferenceScreenTreeTransformer<>() {

									@Override
									public SearchablePreferenceScreenTree transformTree(final SearchablePreferenceScreenTree tree,
																						final Configuration actualConfiguration,
																						final FragmentActivity activityContext) {
										// FK-TODO: implement by computing tree for actualConfiguration, d.h. actualConfiguration.enabledPlugins haben Preferences, die zum graph dazugefügt oder aus dem tree entfernt werden müssen.
										// siehe SettingsSearch: SearchDatabaseRootedAtPrefsFragmentFirstAdapter
										return tree;
									}
								})),
				PreferencesDatabaseConfig.JournalMode.AUTOMATIC);
	}
}
