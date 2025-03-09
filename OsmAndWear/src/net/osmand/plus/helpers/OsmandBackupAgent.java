package net.osmand.plus.helpers;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;

import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.myplaces.favorites.FavouritesFileHelper;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsDbHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.io.IOException;
import java.util.List;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

/**
 * Requires android API from android-8
 */
public class OsmandBackupAgent extends BackupAgentHelper {

	public static final String AUTO_BACKUP_ENABLED = "auto_backup_enabled";

	@Override
	public void onCreate() {
		// can't cast to OsmAnd Application
		if (isAutoBackupEnabled()) {
			String[] files = collectFiles();
			String[] preferences = collectPreferences();

			addHelper("osmand.files", new FileBackupHelper(this, files));
			addHelper("osmand.settings", new SharedPreferencesBackupHelper(this, preferences));
		}
	}

	private boolean isAutoBackupEnabled() {
		String preferencesName = OsmandSettings.getSharedPreferencesName(null);
		SharedPreferences preferences = getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
		return preferences.getBoolean(AUTO_BACKUP_ENABLED, true);
	}

	private String[] collectPreferences() {
		List<ApplicationMode> all = ApplicationMode.allPossibleValues();
		String[] preferences = new String[all.size() + 1];
		preferences[0] = OsmandSettings.getSharedPreferencesName(null);
		int i = 1;
		for (ApplicationMode m : all) {
			preferences[i++] = OsmandSettings.getSharedPreferencesName(m);
		}
		return preferences;
	}

	private String[] collectFiles() {
		return new String[] {
				FavouritesFileHelper.LEGACY_FAV_FILE_PREFIX + FavouritesFileHelper.BAK_FILE_SUFFIX + GPX_FILE_EXT,
				"../databases/" + MapMarkersDbHelper.DB_NAME,
				"../databases/" + OsmBugsDbHelper.OSMBUGS_DB_NAME
		};
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
		long size = oldState.getStatSize();
		if (size > 0 || isAutoBackupEnabled()) {
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
		if (isAutoBackupEnabled()) {
			super.onRestore(data, appVersionCode, newState);
		}
	}

	@Override
	public void onRestoreFinished() {
		getSharedPreferences(OsmandSettings.getSharedPreferencesName(null), Context.MODE_PRIVATE)
				.edit()
				.putInt(OsmandSettings.NUMBER_OF_FREE_DOWNLOADS_ID, 0)
				.apply();
	}
}
