package net.osmand.plus;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.osmedit.OsmBugsDbHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.io.IOException;
import java.util.List;

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

		FileBackupHelper fileBackupHelper = new FileBackupHelper(this, FavouritesDbHelper.FILE_TO_BACKUP,
				"../databases/" + MapMarkersDbHelper.DB_NAME, "../databases/" + OsmBugsDbHelper.OSMBUGS_DB_NAME);
		addHelper("osmand.files", fileBackupHelper);
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
		super.onRestore(data, appVersionCode, newState);
		getSharedPreferences(OsmandSettings.getSharedPreferencesName(null), Context.MODE_PRIVATE)
				.edit()
				.putInt(OsmandSettings.NUMBER_OF_FREE_DOWNLOADS_ID, 0)
				.apply();
	}
}
