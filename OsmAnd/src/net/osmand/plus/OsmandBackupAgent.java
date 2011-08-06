package net.osmand.plus;

import net.osmand.plus.activities.ApplicationMode;
import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Requires android API from android-8
 */
public class OsmandBackupAgent extends BackupAgentHelper {

	@Override
	public void onCreate() {
		String[] prefs = new String[ApplicationMode.values().length + 1];
		prefs[0] = OsmandSettings.getSharedPreferencesName(null);
		int i = 1;
		for (ApplicationMode m : ApplicationMode.values()) {
			prefs[i++] = OsmandSettings.getSharedPreferencesName(m);
		}

		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, prefs);
		addHelper("osmand.settings", helper);

		FileBackupHelper fileBackupHelper = new FileBackupHelper(this, FavouritesDbHelper.FAVOURITE_DB_NAME);
		addHelper("osmand.favorites", fileBackupHelper);
	}
}
