package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupHelper.CollectType;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkSettingsHelper extends SettingsHelper {

	ImportBackupTask importTask;
	ExportBackupTask exportTask;

	public interface BackupExportListener {
		void onBackupExportStarted(int itemsCount);

		void onBackupExportProgressUpdate(int value);

		void onBackupExportFinished(boolean succeed);
	}

	public interface BackupCollectListener {
		void onBackupCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items,
									 @NonNull List<RemoteFile> remoteFiles);
	}

	public NetworkSettingsHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	@Nullable
	public ImportBackupTask getImportTask() {
		return importTask;
	}

	public void setImportTask(ImportBackupTask importTask) {
		this.importTask = importTask;
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
		ExportBackupTask exportTask = this.exportTask;
		if (exportTask != null && (exportTask.getStatus() == AsyncTask.Status.RUNNING)) {
			return exportTask.cancel(true);
		}
		return false;
	}

	public boolean isBackupExporting() {
		return exportTask != null;
	}

	public void updateExportListener(@Nullable BackupExportListener listener) {
		ExportBackupTask exportTask = this.exportTask;
		if (exportTask != null) {
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

	public void collectSettings(String latestChanges, int version, CollectType collectType,
								@Nullable BackupCollectListener listener) {
		new ImportBackupTask(this, latestChanges, version, collectType, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void checkDuplicates(@NonNull List<SettingsItem> items,
								@NonNull List<SettingsItem> selectedItems,
								CheckDuplicatesListener listener) {
		new ImportBackupTask(this, items, selectedItems, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void importSettings(@NonNull List<SettingsItem> items,
							   String latestChanges, int version,
							   @Nullable ImportListener listener) {
		new ImportBackupTask(this, items, latestChanges, version, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@Nullable BackupExportListener listener,
							   @NonNull List<SettingsItem> items) {
		ExportBackupTask exportTask = new ExportBackupTask(this, listener, items);
		this.exportTask = exportTask;
		exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@Nullable BackupExportListener listener,
							   @NonNull SettingsItem... items) {
		exportSettings(listener, new ArrayList<>(Arrays.asList(items)));
	}
}
