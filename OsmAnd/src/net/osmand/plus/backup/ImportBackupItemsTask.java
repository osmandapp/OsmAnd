package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.List;

public class ImportBackupItemsTask extends AsyncTask<Void, Void, Boolean> {

	private final NetworkSettingsHelper helper;
	private final OsmandApplication app;
	private final BackupImporter importer;
	private final ImportListener listener;
	private final List<SettingsItem> items;
	private final StateChangedListener<String> localeListener;
	private final boolean forceReadData;
	private boolean needRestart = false;

	ImportBackupItemsTask(@NonNull NetworkSettingsHelper helper,
						  boolean forceReadData,
						  @Nullable ImportListener listener,
						  @NonNull List<SettingsItem> items) {
		this.helper = helper;
		this.forceReadData = forceReadData;
		this.app = helper.getApp();
		importer = new BackupImporter(app.getBackupHelper());
		this.listener = listener;
		this.items = items;
		localeListener = new StateChangedListener<String>() {
			@Override
			public void stateChanged(String change) {
				needRestart = true;
			}
		};
	}

	@Override
	protected void onPreExecute() {
		app.getSettings().PREFERRED_LOCALE.addListener(localeListener);
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		try {
			importer.importItems(items, forceReadData);
			return true;
		} catch (IllegalArgumentException e) {
			NetworkSettingsHelper.LOG.error("Failed to import items from backup", e);
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		app.getSettings().PREFERRED_LOCALE.removeListener(localeListener);
		helper.finishImport(listener, success, items, needRestart);
	}
}