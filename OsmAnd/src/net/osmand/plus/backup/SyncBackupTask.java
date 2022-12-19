package net.osmand.plus.backup;

import static net.osmand.plus.backup.NetworkSettingsHelper.BACKUP_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.BackupSyncOperationType.BACKUP_SYNC_OPERATION_DOWNLOAD;
import static net.osmand.plus.backup.NetworkSettingsHelper.BackupSyncOperationType.BACKUP_SYNC_OPERATION_SYNC;
import static net.osmand.plus.backup.NetworkSettingsHelper.BackupSyncOperationType.BACKUP_SYNC_OPERATION_UPLOAD;
import static net.osmand.plus.backup.NetworkSettingsHelper.RESTORE_ITEMS_KEY;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupSyncOperationType;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncBackupTask extends AsyncTask<Void, Void, Void> implements OnPrepareBackupListener, ImportListener, BackupExportListener {

	private static final Log LOG = PlatformUtil.getLog(SyncBackupTask.class);

	private final String key;

	private final OsmandApplication app;
	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper networkSettingsHelper;

	private final BackupSyncOperationType operation;
	private final boolean singleOperation;

	private long maxProgress;
	private long lastProgress;
	private long currentProgress;

	public SyncBackupTask(@NonNull OsmandApplication app, @NonNull String key,
	                      @NonNull BackupSyncOperationType operation) {
		this.key = key;
		this.app = app;
		this.operation = operation;
		this.singleOperation = operation != BACKUP_SYNC_OPERATION_SYNC;

		this.backupHelper = app.getBackupHelper();
		this.networkSettingsHelper = app.getNetworkSettingsHelper();
		backupHelper.addPrepareBackupListener(this);
	}

	@Override
	protected void onCancelled() {
		backupHelper.removePrepareBackupListener(this);
	}

	@Override
	protected void onPostExecute(Void unused) {
		backupHelper.removePrepareBackupListener(this);
	}

	private void startSync() {
		PrepareBackupResult backup = backupHelper.getBackup();
		BackupInfo info = backup.getBackupInfo();

		List<SettingsItem> settingsItems = BackupHelper.getItemsForRestore(info, backup.getSettingsItems());

		if (operation != BACKUP_SYNC_OPERATION_DOWNLOAD) {
			maxProgress += calculateExportMaxProgress() / 1024;
		}
		if (operation != BACKUP_SYNC_OPERATION_UPLOAD) {
			maxProgress += ImportBackupTask.calculateMaxProgress(app);
		}
		if (settingsItems.size() > 0 && operation != BACKUP_SYNC_OPERATION_UPLOAD) {
			networkSettingsHelper.importSettings(RESTORE_ITEMS_KEY, settingsItems, false, this);
		} else if (operation != BACKUP_SYNC_OPERATION_DOWNLOAD) {
			uploadNewItems();
		} else {
			onSyncFinished(null);
		}
	}

	public void uploadLocalItem(@NonNull SettingsItem item, @NonNull String fileName) {
		networkSettingsHelper.exportSettings(fileName, Collections.singletonList(item), Collections.emptyList(), this);
	}

	public void downloadRemoteVersion(@NonNull SettingsItem item, @NonNull String fileName) {
		item.setShouldReplace(true);
		networkSettingsHelper.importSettings(fileName, Collections.singletonList(item), true, this);
	}

	public void deleteItem(@NonNull SettingsItem item, @NonNull String fileName) {
		networkSettingsHelper.exportSettings(fileName, Collections.emptyList(), Collections.singletonList(item), this);
	}

	private void onSyncFinished(@Nullable String error) {
		networkSettingsHelper.syncBackupTasks.remove(key);
	}

	private void uploadNewItems() {
		if (isCancelled()) {
			return;
		}
		try {
			BackupInfo info = backupHelper.getBackup().getBackupInfo();
			List<SettingsItem> items = info.itemsToUpload;
			if (items.size() > 0 || info.filteredFilesToUpload.size() > 0) {
				networkSettingsHelper.exportSettings(BACKUP_ITEMS_KEY, items, info.itemsToDelete, this);
			} else {
				onSyncFinished(null);
			}
		} catch (Exception e) {
			LOG.error("Backup generation error: ", e);
		}
	}

	@Override
	public void onBackupPreparing() {

	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		startSync();
	}

	private long calculateExportMaxProgress() {
		BackupInfo info = backupHelper.getBackup().getBackupInfo();

		List<SettingsItem> oldItemsToDelete = new ArrayList<>();
		for (SettingsItem item : info.itemsToUpload) {
			ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForItem(item);
			if (exportType != null && backupHelper.getVersionHistoryTypePref(exportType).get()) {
				oldItemsToDelete.add(item);
			}
		}
		return ExportBackupTask.getEstimatedItemsSize(app, info.itemsToUpload, info.itemsToDelete, oldItemsToDelete);
	}

	@Override
	protected Void doInBackground(Void... voids) {
		if (!backupHelper.isBackupPreparing()) {
			startSync();
		}
		return null;
	}

	@Override
	public void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
		if (isCancelled()) {
			return;
		}
		if (succeed) {
			app.getRendererRegistry().updateExternalRenderers();
			app.getPoiFilters().loadSelectedPoiFilters();
			AppInitializer.loadRoutingFiles(app, null);
		}
		if (singleOperation) {
			onSyncFinished(null);
		}
		uploadNewItems();
	}

	@Override
	public void onBackupExportProgressUpdate(int value) {
		ExportBackupTask exportTask = networkSettingsHelper.getExportTask(BACKUP_ITEMS_KEY);
		if (exportTask != null) {
			currentProgress += exportTask.getGeneralProgress() - lastProgress;
			float progress = (float) currentProgress / maxProgress;
			progress = progress > 1 ? 1 : progress;
			lastProgress = exportTask.getGeneralProgress();
		}
	}

	@Override
	public void onBackupExportFinished(@Nullable String error) {
		onSyncFinished(error);
	}

	@Override
	public void onImportProgressUpdate(int value, int uploadedKb) {
		currentProgress = uploadedKb;
		float progress = (float) currentProgress / maxProgress;
		progress = progress > 1 ? 1 : progress;
	}

	@Override
	public void onBackupExportStarted() {

	}

	@Override
	public void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int work) {

	}

	@Override
	public void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value) {

	}

	@Override
	public void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName) {

	}


	@Override
	public void onImportItemStarted(@NonNull String type, @NonNull String fileName, int work) {

	}

	@Override
	public void onImportItemProgress(@NonNull String type, @NonNull String fileName, int value) {

	}

	@Override
	public void onImportItemFinished(@NonNull String type, @NonNull String fileName) {

	}
}
