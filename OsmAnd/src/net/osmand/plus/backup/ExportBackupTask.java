package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.BackupExporter.NetworkExportProgressListener;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExportBackupTask extends AsyncTask<Void, Integer, Boolean> {

	private final NetworkSettingsHelper helper;
	private final BackupExporter exporter;
	private BackupExportListener listener;

	ExportBackupTask(@NonNull NetworkSettingsHelper helper,
					 @Nullable BackupExportListener listener,
					 @NonNull List<SettingsItem> items) {
		this.helper = helper;
		this.listener = listener;
		this.exporter = new BackupExporter(helper.getApp().getBackupHelper(), getProgressListener());
		for (SettingsItem item : items) {
			exporter.addSettingsItem(item);
		}
	}

	public BackupExportListener getListener() {
		return listener;
	}

	public void setListener(BackupExportListener listener) {
		this.listener = listener;
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		try {
			exporter.export();
			return true;
		} catch (IOException e) {
			SettingsHelper.LOG.error("Failed to backup items", e);
		}
		return false;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onBackupExportStarted(exporter.getItems().size());
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (listener != null) {
			listener.onBackupExportProgressUpdate(values[0]);
		}
	}

	@Override
	protected void onCancelled() {
		onPostExecute(false);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		helper.exportTask = null;
		if (listener != null) {
			listener.onBackupExportFinished(success);
		}
	}

	private NetworkExportProgressListener getProgressListener() {
		return new NetworkExportProgressListener() {
			@Override
			public void updateItemProgress(@NonNull String type, @NonNull String fileName, int value) {

			}

			@Override
			public void updateGeneralProgress(int uploadedItems, int uploadedKb) {
				exporter.setCancelled(isCancelled());
				publishProgress(uploadedItems);
			}

			@Override
			public void networkExportDone(@NonNull Map<String, String> errors) {

			}
		};
	}
}