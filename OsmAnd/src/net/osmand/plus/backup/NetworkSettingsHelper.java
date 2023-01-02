package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkSettingsHelper extends SettingsHelper {

	public static final String BACKUP_ITEMS_KEY = "backup_items_key";
	public static final String RESTORE_ITEMS_KEY = "restore_items_key";
	public static final String SYNC_ITEMS_KEY = "sync_items_key";
	public static final String PREPARE_BACKUP_KEY = "prepare_backup_key";

	final Map<String, ImportBackupTask> importAsyncTasks = new HashMap<>();
	final Map<String, ExportBackupTask> exportAsyncTasks = new HashMap<>();
	final Map<String, SyncBackupTask> syncBackupTasks = new HashMap<>();

	public enum SyncOperationType {
		SYNC_OPERATION_NONE,
		SYNC_OPERATION_SYNC,
		SYNC_OPERATION_UPLOAD,
		SYNC_OPERATION_DOWNLOAD,
		SYNC_OPERATION_DELETE
	}

	public interface BackupExportListener {
		void onBackupExportStarted();

		void onBackupExportProgressUpdate(int value);

		void onBackupExportFinished(@Nullable String error);

		void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int work);

		void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value);

		void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName);
	}

	public interface BackupCollectListener {
		void onBackupCollectFinished(boolean succeed, boolean empty,
		                             @NonNull List<SettingsItem> items,
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
	public SyncBackupTask getSyncTask(@NonNull String key) {
		return syncBackupTasks.get(key);
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

	public boolean cancelSyncTasks() {
		boolean cancelled = false;
		for (SyncBackupTask syncTask : syncBackupTasks.values()) {
			if (syncTask != null && (syncTask.getStatus() == AsyncTask.Status.RUNNING)) {
				cancelled |= syncTask.cancel(true);
			}
		}
		return cancelled;
	}

	public void cancelSync() {
		cancelImport();
		cancelExport();
		cancelSyncTasks();
	}

	public boolean isBackupExporting() {
		return !Algorithms.isEmpty(exportAsyncTasks);
	}

	public boolean isBackupImporting() {
		return !Algorithms.isEmpty(importAsyncTasks);
	}

	public boolean isBackupSyncing() {
		return !Algorithms.isEmpty(syncBackupTasks);
	}

	public void updateExportListener(@Nullable BackupExportListener listener) {
		for (ExportBackupTask exportTask : exportAsyncTasks.values()) {
			exportTask.setListener(listener);
		}
	}

	public void updateImportListener(@Nullable ImportListener listener) {
		for (ImportBackupTask importTask : importAsyncTasks.values()) {
			importTask.setImportListener(listener);
		}
	}

	void finishImport(@Nullable ImportListener listener, boolean success, @NonNull List<SettingsItem> items, boolean needRestart) {
		String error = collectFormattedWarnings(items);
		if (!Algorithms.isEmpty(error)) {
			getApp().showToastMessage(error);
		}
		if (listener != null) {
			listener.onImportFinished(success, needRestart, items);
		}
	}

	private String collectFormattedWarnings(@NonNull List<SettingsItem> items) {
		List<String> warnings = new ArrayList<>();
		for (SettingsItem item : items) {
			warnings.addAll(item.getWarnings());
		}
		String error = null;
		if (!Algorithms.isEmpty(warnings)) {
			error = AndroidUtils.formatWarnings(warnings).toString();
		}
		return error;
	}

	public void collectSettings(@NonNull String key, boolean readData,
	                            @Nullable BackupCollectListener listener) throws IllegalStateException {
		if (!importAsyncTasks.containsKey(key)) {
			ImportBackupTask importTask = new ImportBackupTask(key, this, listener, readData);
			importAsyncTasks.put(key, importTask);
			importTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already importing " + key);
		}
	}

	public void checkDuplicates(@NonNull String key,
	                            @NonNull List<SettingsItem> items,
	                            @NonNull List<SettingsItem> selectedItems,
	                            CheckDuplicatesListener listener) throws IllegalStateException {
		if (!importAsyncTasks.containsKey(key)) {
			ImportBackupTask importTask = new ImportBackupTask(key, this, items, selectedItems, listener);
			importAsyncTasks.put(key, importTask);
			importTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already importing " + key);
		}
	}

	public void importSettings(@NonNull String key,
	                           @NonNull List<SettingsItem> items,
	                           boolean forceReadData,
	                           @Nullable ImportListener listener) throws IllegalStateException {
		if (!importAsyncTasks.containsKey(key)) {
			ImportBackupTask importTask = new ImportBackupTask(key, this, items, listener, forceReadData);
			importAsyncTasks.put(key, importTask);
			importTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already importing " + key);
		}
	}

	public void exportSettings(@NonNull String key,
	                           @NonNull List<SettingsItem> items,
	                           @NonNull List<SettingsItem> itemsToDelete,
	                           @Nullable BackupExportListener listener) throws IllegalStateException {
		if (!exportAsyncTasks.containsKey(key)) {
			ExportBackupTask exportTask = new ExportBackupTask(key, this, items, itemsToDelete, listener);
			exportAsyncTasks.put(key, exportTask);
			exportTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already exporting " + key);
		}
	}

	public void syncSettingsItems(@NonNull String key, @NonNull SyncOperationType operation,
	                              @Nullable OnBackupSyncListener listener) {
		if (!syncBackupTasks.containsKey(key)) {
			SyncBackupTask syncTask = new SyncBackupTask(getApp(), key, operation, listener);
			syncBackupTasks.put(key, syncTask);
			syncTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already syncing " + key);
		}
	}


	public void syncSettingsItems(@NonNull String key,
	                              @NonNull LocalFile localFile,
	                              @NonNull RemoteFile remoteFile,
	                              @NonNull SyncOperationType operation,
	                              @Nullable OnBackupSyncListener listener) {
		if (!syncBackupTasks.containsKey(key)) {
			SyncBackupTask syncTask = new SyncBackupTask(getApp(), key, operation, listener);
			syncBackupTasks.put(key, syncTask);
			SettingsItem settingsItem = localFile == null || localFile.item == null ? remoteFile.item : localFile.item;
			switch (operation) {
				case SYNC_OPERATION_DELETE:
					syncTask.deleteItem(remoteFile.item);
					break;
				case SYNC_OPERATION_UPLOAD:
					syncTask.uploadLocalItem(localFile.item);
					break;
				case SYNC_OPERATION_DOWNLOAD:
					syncTask.downloadRemoteVersion(remoteFile.item);
					break;
			}
		} else {
			throw new IllegalStateException("Already syncing " + key);
		}
	}

	public void exportSettings(@NonNull String key, @Nullable BackupExportListener listener,
	                           @NonNull SettingsItem... items) throws IllegalStateException {
		exportSettings(key, new ArrayList<>(Arrays.asList(items)), Collections.emptyList(), listener);
	}
}
