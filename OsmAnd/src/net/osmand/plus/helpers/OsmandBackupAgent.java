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
import net.osmand.plus.myplaces.FavouritesFileHelper;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsDbHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Requires android API from android-8
 */
public class OsmandBackupAgent extends BackupAgentHelper {

	private static final String AUTO_BACKUP_ENABLED = "auto_backup_enabled";
	private static final String AUTO_BACKUP_DISABLED = "auto_backup_disabled";

	@Override
	public void onCreate() {
		boolean enabled = isAutoBackupEnabled();
		String[] preferences = enabled ? collectPreferencesForBackup() : new String[0];
		String[] files = enabled ? collectFilesForBackup() : new String[0];

		// can't cast to OsmAnd Application
		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, preferences);
		addHelper("osmand.settings", helper);

		FileBackupHelper fileBackupHelper = new FileBackupHelper(this, files);
		addHelper("osmand.files", fileBackupHelper);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
		writeAutoBackupEnabled(data);
		if (isAutoBackupEnabled()) {
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
		readAutoBackupEnabled(data);
		if (isAutoBackupEnabled()) {
			super.onRestore(data, appVersionCode, newState);
			getSharedPreferences(OsmandSettings.getSharedPreferencesName(null), Context.MODE_PRIVATE)
					.edit()
					.putInt(OsmandSettings.NUMBER_OF_FREE_DOWNLOADS_ID, 0)
					.apply();
		}
	}

	private String[] collectPreferencesForBackup() {
		List<ApplicationMode> all = ApplicationMode.allPossibleValues();
		String[] preferences = new String[all.size() + 1];
		preferences[0] = OsmandSettings.getSharedPreferencesName(null);
		int i = 1;
		for (ApplicationMode m : all) {
			preferences[i++] = OsmandSettings.getSharedPreferencesName(m);
		}
		return preferences;
	}

	private String[] collectFilesForBackup() {
		return new String[]{
				FavouritesFileHelper.FILE_TO_BACKUP,
				"../databases/" + MapMarkersDbHelper.DB_NAME,
				"../databases/" + OsmBugsDbHelper.OSMBUGS_DB_NAME
		};
	}

	private void writeAutoBackupEnabled(BackupDataOutput data) {
		ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
		byte[] buffer = bufStream.toByteArray();
		int len = buffer.length;
		try {
			data.writeEntityHeader(isAutoBackupEnabled() ? AUTO_BACKUP_ENABLED : AUTO_BACKUP_DISABLED, len);
		} catch (IOException e) {
			// ignore
		}
	}

	private void readAutoBackupEnabled(BackupDataInput data) {
		try {
			data.readNextHeader();
			String key = data.getKey();
			boolean isAutoBackupEnabled = !AUTO_BACKUP_DISABLED.equals(key);
			getSharedPreferences(OsmandSettings.getSharedPreferencesName(null), Context.MODE_PRIVATE)
					.edit()
					.putBoolean(OsmandSettings.AUTO_BACKUP_ENABLED_ID, isAutoBackupEnabled)
					.apply();
		} catch (IOException e) {
			// ignore
		}
	}

	private boolean isAutoBackupEnabled() {
		SharedPreferences preferences = getSharedPreferences(OsmandSettings.getSharedPreferencesName(null), Context.MODE_PRIVATE);
		return preferences.getBoolean(OsmandSettings.AUTO_BACKUP_ENABLED_ID, true);
	}
}
