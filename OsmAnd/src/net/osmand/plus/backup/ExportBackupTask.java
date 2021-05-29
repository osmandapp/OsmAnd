package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ExportProgressListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.IOException;
import java.util.List;

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
	protected void onProgressUpdate(Integer... values) {
		if (listener != null) {
			listener.onBackupExportProgressUpdate(values[0]);
		}
	}

	@Override
	protected void onPostExecute(Boolean success) {
		helper.exportTask = null;
		if (listener != null) {
			listener.onBackupExportFinished(success);
		}
	}

	private ExportProgressListener getProgressListener() {
		return new ExportProgressListener() {
			@Override
			public void updateProgress(int value) {
				exporter.setCancelled(isCancelled());
				publishProgress(value);
			}
		};
	}
}