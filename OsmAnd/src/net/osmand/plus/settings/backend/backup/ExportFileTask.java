package net.osmand.plus.settings.backend.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.backup.FileSettingsHelper.SettingsExportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ExportProgressListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExportFileTask extends AsyncTask<Void, Integer, Boolean> {

	private final FileSettingsHelper helper;
	private final File file;
	private final SettingsExporter exporter;
	private SettingsExportListener listener;

	ExportFileTask(@Nullable FileSettingsHelper helper,
				   @NonNull File settingsFile,
				   @Nullable SettingsExportListener listener,
				   @NonNull List<SettingsItem> items, boolean exportItemsFiles) {
		this.helper = helper;
		this.file = settingsFile;
		this.listener = listener;
		this.exporter = new SettingsExporter(file, getProgressListener(), exportItemsFiles);
		for (SettingsItem item : items) {
			exporter.addSettingsItem(item);
		}
	}

	public SettingsExportListener getListener() {
		return listener;
	}

	public void setListener(SettingsExportListener listener) {
		this.listener = listener;
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		try {
			exporter.export();
			return true;
		} catch (JSONException e) {
			SettingsHelper.LOG.error("Failed to export items to: " + file.getName(), e);
		} catch (IOException e) {
			SettingsHelper.LOG.error("Failed to export items to: " + file.getName(), e);
		}
		return false;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (listener != null) {
			listener.onSettingsExportProgressUpdate(values[0]);
		}
	}

	@Override
	protected void onPostExecute(Boolean success) {
		helper.exportAsyncTasks.remove(file);
		if (listener != null) {
			listener.onSettingsExportFinished(file, success);
		}
	}

	@Override
	protected void onCancelled() {
		Algorithms.removeAllFiles(file);
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
