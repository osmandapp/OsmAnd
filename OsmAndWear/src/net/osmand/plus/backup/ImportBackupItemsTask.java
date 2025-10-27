package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.Collection;
import java.util.List;

public class ImportBackupItemsTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final BackupImporter importer;
	private final ImportItemsListener listener;
	private final List<SettingsItem> items;
	private final RemoteFilesType filesType;
	private final StateChangedListener<String> localeListener;
	private final boolean forceReadData;
	private final boolean restoreDeleted;
	private boolean needRestart;

	ImportBackupItemsTask(@NonNull OsmandApplication app,
	                      @NonNull BackupImporter importer,
	                      @NonNull List<SettingsItem> items,
	                      @NonNull RemoteFilesType filesType,
	                      @Nullable ImportItemsListener listener,
	                      boolean forceReadData,
	                      boolean restoreDeleted) {
		this.app = app;
		this.importer = importer;
		this.items = items;
		this.filesType = filesType;
		this.listener = listener;
		this.forceReadData = forceReadData;
		this.restoreDeleted = restoreDeleted;
		localeListener = change -> needRestart = true;
	}

	@Override
	protected void onPreExecute() {
		app.getSettings().PREFERRED_LOCALE.addListener(localeListener);
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		try {
			PrepareBackupResult backup = app.getBackupHelper().getBackup();
			Collection<RemoteFile> remoteFiles = backup.getRemoteFiles(filesType).values();
			importer.importItems(items, remoteFiles, forceReadData, restoreDeleted);
			return importer.isCancelled();
		} catch (IllegalArgumentException e) {
			NetworkSettingsHelper.LOG.error("Failed to import items from backup", e);
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		app.getSettings().PREFERRED_LOCALE.removeListener(localeListener);
		if (listener != null) {
			listener.onImportFinished(success, needRestart);
		}
	}

	public interface ImportItemsListener {
		void onImportFinished(boolean succeed, boolean needRestart);
	}
}
