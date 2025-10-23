package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabaseConfig;
import de.KnollFrank.lib.settingssearch.db.preference.db.PrepackagedPreferencesDatabase;
import de.KnollFrank.lib.settingssearch.db.preference.db.SearchablePreferenceScreenGraphProcessor;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;

public class PreferencesDatabaseFactory {

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
								new SearchablePreferenceScreenGraphProcessor<>() {

									@Override
									public SearchablePreferenceScreenGraph processGraph(final SearchablePreferenceScreenGraph graph,
																						final Configuration actualConfiguration,
																						final FragmentActivity activityContext) {
										// FK-TODO: implement by computing graph for actualConfiguration, d.h. actualConfiguration.enabledPlugins haben Preferences, die zum graph dazugefügt oder aus dem graph entfernt werden müssen.
										return graph;
									}
								})),
				PreferencesDatabaseConfig.JournalMode.AUTOMATIC);
	}
}
