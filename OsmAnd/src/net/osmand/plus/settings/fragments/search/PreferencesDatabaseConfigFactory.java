package net.osmand.plus.settings.fragments.search;

import android.os.PersistableBundle;

import androidx.fragment.app.FragmentActivity;

import com.google.common.graph.ImmutableValueGraph;

import java.io.File;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.common.graph.Tree;
import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabaseConfig;
import de.KnollFrank.lib.settingssearch.db.preference.db.PrepackagedPreferencesDatabase;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
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
}
