package net.osmand.plus;

import java.util.List;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Requires android API from android-8
 */
public class OsmandBackupAgent extends BackupAgentHelper {

	@Override
	public void onCreate() {
		// can't cast to OsmAnd Application
		List<ApplicationMode> all = ApplicationMode.allPossibleValues();
		String[] prefs = new String[all.size() + 1];
		prefs[0] = OsmandSettings.getSharedPreferencesName(null);
		int i = 1;
		for (ApplicationMode m : all) {
			prefs[i++] = OsmandSettings.getSharedPreferencesName(m);
		}

		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, prefs);
		addHelper("osmand.settings", helper);

		FileBackupHelper fileBackupHelper = new FileBackupHelper(this, FavouritesDbHelper.FAVOURITE_DB_NAME);
		addHelper("osmand.favorites", fileBackupHelper);
	}
}
