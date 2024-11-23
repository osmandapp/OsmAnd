package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
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

	private final List<OnBackupSyncListener> syncListeners = new ArrayList<>();

	public enum SyncOperationType {
		SYNC_OPERATION_NONE,
		SYNC_OPERATION_SYNC,
		SYNC_OPERATION_UPLOAD,
		SYNC_OPERATION_DOWNLOAD,
		SYNC_OPERATION_DELETE
	}

	public interface BackupExportListener {
		void onBackupExportStarted();

		void onBackupExportProgressUpdate(int progress);

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

	public void addBackupSyncListener(@NonNull OnBackupSyncListener listener) {
		syncListeners.add(listener);
	}

	public void removeBackupSyncListener(@NonNull OnBackupSyncListener listener) {
		syncListeners.remove(listener);
	}

	@Nullable
	public ImportType getImportTaskType(@NonNull String key) {
		ImportBackupTask importTask = getImportTask(key);
		return importTask != null ? importTask.getImportType() : null;
	}

	public boolean cancelExport() {
		boolean cancelled = false;
		for (ExportBackupTask exportTask : exportAsyncTasks.values()) {
			if (exportTask != null) {
				cancelled |= exportTask.cancel(false);
			}
		}
		return cancelled;
	}

	public boolean cancelImport() {
		boolean cancelled = false;
		for (ImportBackupTask importTask : importAsyncTasks.values()) {
			if (importTask != null) {
				cancelled |= importTask.cancel(false);
			}
		}
		return cancelled;
	}

	public boolean cancelSyncTasks() {
		boolean cancelled = false;
		for (SyncBackupTask syncTask : syncBackupTasks.values()) {
			if (syncTask != null) {
				cancelled |= syncTask.cancel(false);
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

	public boolean isSyncing(@NonNull String key) {
		SyncBackupTask syncTask = getSyncTask(key);
		if (syncTask == null) {
			syncTask = getSyncTask(SYNC_ITEMS_KEY);
		}
		return syncTask != null;
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
	                           @NonNull RemoteFilesType filesType,
	                           boolean forceReadData,
	                           @Nullable ImportListener listener) throws IllegalStateException {
		importSettings(key, items, filesType, forceReadData, true, false, listener);
	}

	public void importSettings(@NonNull String key,
	                           @NonNull List<SettingsItem> items,
	                           @NonNull RemoteFilesType filesType,
	                           boolean forceReadData,
	                           boolean shouldReplace,
	                           boolean restoreDeleted,
	                           @Nullable ImportListener listener) throws IllegalStateException {
		if (!importAsyncTasks.containsKey(key)) {
			ImportBackupTask importTask = new ImportBackupTask(key, this, items, filesType,
					listener, forceReadData, shouldReplace, restoreDeleted);
			importAsyncTasks.put(key, importTask);
			importTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already importing " + key);
		}
	}

	public void exportSettings(@NonNull String key,
	                           @NonNull List<SettingsItem> items,
	                           @NonNull List<SettingsItem> itemsToDelete,
	                           @NonNull List<SettingsItem> itemsToLocalDelete,
	                           @Nullable BackupExportListener listener) throws IllegalStateException {
		if (!exportAsyncTasks.containsKey(key)) {
			ExportBackupTask exportTask = new ExportBackupTask(key, this, items, itemsToDelete, itemsToLocalDelete, listener);
			exportAsyncTasks.put(key, exportTask);
			exportTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already exporting " + key);
		}
	}

	public void syncSettingsItems(@NonNull String key, @NonNull SyncOperationType operation) throws IllegalStateException {
		if (!syncBackupTasks.containsKey(key)) {
			SyncBackupTask syncTask = new SyncBackupTask(getApp(), key, operation, getOnBackupSyncListener());
			registerSyncBackupTask(key, syncTask);
			syncTask.executeOnExecutor(getBackupHelper().getExecutor());
		} else {
			throw new IllegalStateException("Already syncing " + key);
		}
	}

	public void syncSettingsItems(@NonNull String key,
	                              @Nullable LocalFile localFile,
	                              @Nullable RemoteFile remoteFile,
	                              @NonNull RemoteFilesType filesType,
	                              @NonNull SyncOperationType operation) {
		syncSettingsItems(key, localFile, remoteFile, filesType, operation, true, false);
	}

	public void syncSettingsItems(@NonNull String key,
	                              @Nullable LocalFile localFile,
	                              @Nullable RemoteFile remoteFile,
	                              @NonNull RemoteFilesType type,
	                              @NonNull SyncOperationType operation,
	                              boolean shouldReplace, boolean restoreDeleted) {
		if (!syncBackupTasks.containsKey(key)) {
			SyncBackupTask syncTask = new SyncBackupTask(getApp(), key, operation, getOnBackupSyncListener());
			registerSyncBackupTask(key, syncTask);
			switch (operation) {
				case SYNC_OPERATION_DELETE -> {
					if (remoteFile != null) {
						if (remoteFile.item != null) {
							syncTask.deleteItem(remoteFile.item);
						}
					} else if (localFile != null) {
						if (localFile.item != null) {
							syncTask.deleteLocalItem(localFile.item);
						}
					}
				}
				case SYNC_OPERATION_UPLOAD -> {
					if (localFile != null && localFile.item != null) {
						syncTask.uploadLocalItem(localFile.item);
					}
				}
				case SYNC_OPERATION_DOWNLOAD -> {
					if (remoteFile != null && remoteFile.item != null) {
						syncTask.downloadItem(remoteFile.item, type, shouldReplace, restoreDeleted);
					}
				}
			}
		} else {
			throw new IllegalStateException("Already syncing " + key);
		}
	}

	public void registerSyncBackupTask(@NonNull String key, @NonNull SyncBackupTask task) {
		syncBackupTasks.put(key, task);
		getOnBackupSyncListener().onBackupSyncTasksUpdated();
	}

	public void unregisterSyncBackupTask(@NonNull String key) {
		syncBackupTasks.remove(key);
		getOnBackupSyncListener().onBackupSyncTasksUpdated();
	}

	private OnBackupSyncListener getOnBackupSyncListener() {
		return new OnBackupSyncListener() {

			@Override
			public void onBackupSyncTasksUpdated() {
				for (OnBackupSyncListener listener : syncListeners) {
					listener.onBackupSyncTasksUpdated();
				}
			}

			@Override
			public void onBackupSyncStarted() {
				for (OnBackupSyncListener listener : syncListeners) {
					listener.onBackupSyncStarted();
				}
			}

			@Override
			public void onBackupProgressUpdate(int progress) {
				for (OnBackupSyncListener listener : syncListeners) {
					listener.onBackupProgressUpdate(progress);
				}
			}

			@Override
			public void onBackupSyncFinished(@Nullable String error) {
				for (OnBackupSyncListener listener : syncListeners) {
					listener.onBackupSyncFinished(error);
				}
			}

			@Override
			public void onBackupItemStarted(@NonNull String type, @NonNull String fileName, int work) {
				for (OnBackupSyncListener listener : syncListeners) {
					listener.onBackupItemStarted(type, fileName, work);
				}
			}

			@Override
			public void onBackupItemProgress(@NonNull String type, @NonNull String fileName, int value) {
				for (OnBackupSyncListener listener : syncListeners) {
					listener.onBackupItemProgress(type, fileName, value);
				}
			}

			@Override
			public void onBackupItemFinished(@NonNull String type, @NonNull String fileName) {
				for (OnBackupSyncListener listener : syncListeners) {
					listener.onBackupItemFinished(type, fileName);
				}
			}
		};
	}
}