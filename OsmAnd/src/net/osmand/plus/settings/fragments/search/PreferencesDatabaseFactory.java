package net.osmand.plus.settings.fragments.search;

import android.os.PersistableBundle;

import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabaseConfig;
import de.KnollFrank.lib.settingssearch.db.preference.db.PrepackagedPreferencesDatabase;
import de.KnollFrank.lib.settingssearch.db.preference.db.SearchablePreferenceScreenGraphProcessor;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;

public class PreferencesDatabaseFactory {

	public static final String SEARCHABLE_PREFERENCES_DB = "searchable_preferences.db";

	public static PreferencesDatabaseConfig createPreferencesDatabaseConfigForCreationOfPrepackagedDatabaseAssetFile() {
		return new PreferencesDatabaseConfig(
				SEARCHABLE_PREFERENCES_DB,
				Optional.empty(),
				PreferencesDatabaseConfig.JournalMode.TRUNCATE);
	}

	public static PreferencesDatabaseConfig createPreferencesDatabaseConfigUsingPrepackagedDatabaseAssetFile() {
		return new PreferencesDatabaseConfig(
				SEARCHABLE_PREFERENCES_DB,
				Optional.of(
						new PrepackagedPreferencesDatabase(
								new File("database/searchable_preferences_prepackaged.db"),
								new SearchablePreferenceScreenGraphProcessor() {

									@Override
									public SearchablePreferenceScreenGraph processGraph(final SearchablePreferenceScreenGraph graph,
																						final PersistableBundle actualConfiguration,
																						final FragmentActivity activityContext) {
										// FK-TODO: implement by computing graph for actualConfiguration
										return graph;
									}
								})),
				PreferencesDatabaseConfig.JournalMode.AUTOMATIC);
	}
}
