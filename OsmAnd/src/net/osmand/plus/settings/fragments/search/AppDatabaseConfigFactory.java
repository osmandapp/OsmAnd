package net.osmand.plus.settings.fragments.search;

import java.io.File;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.db.AppDatabaseConfig;

class AppDatabaseConfigFactory {

    private static final String SEARCHABLE_PREFERENCES_DB = "searchable_preferences.db";

    public static AppDatabaseConfig createDatabaseConfigForCreationOfPrepackagedDatabaseAssetFile() {
        return new AppDatabaseConfig(
                SEARCHABLE_PREFERENCES_DB,
                Optional.empty(),
                AppDatabaseConfig.JournalMode.TRUNCATE);
    }

    public static AppDatabaseConfig createAppDatabaseConfigUsingPrepackagedDatabaseAssetFile() {
        return new AppDatabaseConfig(
                SEARCHABLE_PREFERENCES_DB,
                Optional.of(new File("database/searchable_preferences_prepackaged.db")),
                AppDatabaseConfig.JournalMode.AUTOMATIC);
    }
}
