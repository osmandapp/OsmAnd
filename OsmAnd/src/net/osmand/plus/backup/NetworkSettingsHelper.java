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

	final Map<String, ImportBackupTask> importAsyncTasks = new HashMap<>();
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
	public ImportBackupTask getImportTask(@NonNull String key) {
		return importAsyncTasks.get(key);
	}

	@Nullable
	public ExportBackupTask getExportTask(@NonNull String key) {
		return exportAsyncTasks.get(key);
	}

	@Nullable
	public ImportType getImportTaskType(@NonNull String key) {
		ImportBackupTask importTask = getImportTask(key);
		return importTask != null ? importTask.getImportType() : null;
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

	public boolean cancelImport() {
		boolean cancelled = false;
		for (ImportBackupTask importTask : importAsyncTasks.values()) {
			if (importTask != null && (importTask.getStatus() == AsyncTask.Status.RUNNING)) {
				cancelled |= importTask.cancel(true);
			}
		}
		return cancelled;
	}

	public boolean isBackupExporting() {
		return !Algorithms.isEmpty(exportAsyncTasks);
	}

	public boolean isBackupImporting() {
		return !Algorithms.isEmpty(importAsyncTasks);
	}

	public void updateExportListener(@Nullable BackupExportListener listener) {
		for (ExportBackupTask exportTask : exportAsyncTasks.values()) {
			exportTask.setListener(listener);
		}
	}

	void finishImport(@Nullable ImportListener listener, boolean success, @NonNull List<SettingsItem> items, boolean needRestart) {
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

	public void collectSettings(@NonNull String key, boolean readData,
								@Nullable BackupCollectListener listener) {
		new ImportBackupTask(key, this, listener, readData)
				.executeOnExecutor(getBackupHelper().getExecutor());
	}

	public void checkDuplicates(@NonNull String key,
								@NonNull List<SettingsItem> items,
								@NonNull List<SettingsItem> selectedItems,
								CheckDuplicatesListener listener) {
		new ImportBackupTask(key, this, items, selectedItems, listener)
				.executeOnExecutor(getBackupHelper().getExecutor());
	}

	public void importSettings(@NonNull String key,
							   @NonNull List<SettingsItem> items,
							   boolean forceReadData,
							   @Nullable ImportListener listener) {
		ImportBackupTask importTask = new ImportBackupTask(key, this, items, listener, forceReadData);
		importTask.executeOnExecutor(getBackupHelper().getExecutor());
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
