package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkSettingsHelper extends SettingsHelper {

	ImportBackupTask importTask;
	final Map<String, ExportBackupTask> exportAsyncTasks = new HashMap<>();

	public interface BackupExportListener {
		void onBackupExportStarted();

		void onBackupExportProgressUpdate(int value);

		void onBackupExportFinished(@Nullable String error);

		void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int work);

		void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value);

		void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName);
	}

	public interface BackupCollectListener {
		void onBackupCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items,
									 @NonNull List<RemoteFile> remoteFiles);
	}

	public NetworkSettingsHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	private BackupHelper getBackupHelper() {
		return getApp().getBackupHelper();
	}

	@Nullable
	public ImportBackupTask getImportTask() {
		return importTask;
	}

	public void setImportTask(ImportBackupTask importTask) {
		this.importTask = importTask;
	}

	@Nullable
	public ExportBackupTask getExportTask(@NonNull String key) {
		return exportAsyncTasks.get(key);
	}

	@Nullable
	public ImportType getImportTaskType() {
		ImportBackupTask importTask = this.importTask;
		return importTask != null ? importTask.getImportType() : null;
	}

	public boolean isImportDone() {
		ImportBackupTask importTask = this.importTask;
		return importTask == null || importTask.isImportDone();
	}

	public boolean cancelExport() {
		boolean cancelled = false;
		for (ExportBackupTask exportTask : exportAsyncTasks.values()) {
			if (exportTask != null && (exportTask.getStatus() == AsyncTask.Status.RUNNING)) {
				cancelled |= exportTask.cancel(true);
			}
		}
		return cancelled;
	}

	public boolean isBackupExporting() {
		return !Algorithms.isEmpty(exportAsyncTasks);
	}

	public boolean isBackupImporting() {
		return importTask != null;
	}

	public void updateExportListener(@Nullable BackupExportListener listener) {
		for (ExportBackupTask exportTask : exportAsyncTasks.values()) {
			exportTask.setListener(listener);
		}
	}

	void finishImport(@Nullable ImportListener listener, boolean success, @NonNull List<SettingsItem> items, boolean needRestart) {
		importTask = null;
		List<String> warnings = new ArrayList<>();
		for (SettingsItem item : items) {
			warnings.addAll(item.getWarnings());
		}
		if (!warnings.isEmpty()) {
			getApp().showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
		}
		if (listener != null) {
			listener.onImportFinished(success, needRestart, items);
		}
	}

	public void collectSettings(String latestChanges, int version, boolean readData,
								@Nullable BackupCollectListener listener) {
		new ImportBackupTask(this, latestChanges, version, readData, listener)
				.executeOnExecutor(getBackupHelper().getExecutor());
	}

	public void checkDuplicates(@NonNull List<SettingsItem> items,
								@NonNull List<SettingsItem> selectedItems,
								CheckDuplicatesListener listener) {
		new ImportBackupTask(this, items, selectedItems, listener)
				.executeOnExecutor(getBackupHelper().getExecutor());
	}

	public void importSettings(@NonNull List<SettingsItem> items,
							   String latestChanges, int version,
							   boolean forceReadData,
							   @Nullable ImportListener listener) {
		new ImportBackupTask(this, forceReadData, items, latestChanges, version, listener)
				.executeOnExecutor(getBackupHelper().getExecutor());
	}

	public void exportSettings(@NonNull String key,
							   @NonNull List<SettingsItem> items,
							   @NonNull List<SettingsItem> itemsToDelete,
							   @Nullable BackupExportListener listener) {
		ExportBackupTask exportTask = new ExportBackupTask(key, this, items, itemsToDelete, listener);
		exportAsyncTasks.put(key, exportTask);
		exportTask.executeOnExecutor(getBackupHelper().getExecutor());
	}

	public void exportSettings(@NonNull String key, @Nullable BackupExportListener listener,
							   @NonNull SettingsItem... items) {
		exportSettings(key, new ArrayList<>(Arrays.asList(items)), Collections.emptyList(), listener);
	}
}
