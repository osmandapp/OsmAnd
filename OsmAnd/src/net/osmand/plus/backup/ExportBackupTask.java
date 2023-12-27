package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupExporter.NetworkExportProgressListener;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportBackupTask extends AsyncTask<Void, Object, String> {

	public static final int APPROXIMATE_FILE_SIZE_BYTES = 100 * 1024;

	private final NetworkSettingsHelper helper;
	private final BackupExporter exporter;
	private BackupExportListener listener;

	private final String key;
	private final Map<String, ItemProgressInfo> itemsProgress = new HashMap<>();
	private int generalProgress;
	private long maxProgress;

	ExportBackupTask(@NonNull String key,
	                 @NonNull NetworkSettingsHelper helper,
	                 @NonNull List<SettingsItem> items,
	                 @NonNull List<SettingsItem> itemsToDelete,
	                 @NonNull List<SettingsItem> itemsToLocalDelete,
	                 @Nullable BackupExportListener listener) {
		this.key = key;
		this.helper = helper;
		this.listener = listener;
		BackupHelper backupHelper = helper.getApp().getBackupHelper();
		this.exporter = new BackupExporter(backupHelper, getProgressListener());
		for (SettingsItem item : items) {
			exporter.addSettingsItem(item);

			ExportType exportType = ExportType.findBy(item);
			if (exportType != null && !backupHelper.getVersionHistoryTypePref(exportType).get()) {
				exporter.addOldItemToDelete(item);
			}
		}
		for (SettingsItem item : itemsToDelete) {
			exporter.addItemToDelete(item);
		}
		for (SettingsItem item : itemsToLocalDelete) {
			exporter.addItemToLocalDelete(item);
		}
	}

	public BackupExportListener getListener() {
		return listener;
	}

	public void setListener(BackupExportListener listener) {
		this.listener = listener;
	}

	public int getGeneralProgress() {
		return generalProgress;
	}

	public long getMaxProgress() {
		return maxProgress;
	}

	@Nullable
	public ItemProgressInfo getItemProgressInfo(@NonNull String type, @NonNull String fileName) {
		return itemsProgress.get(type + fileName);
	}

	@Override
	protected String doInBackground(Void... voids) {
		OsmandApplication app = helper.getApp();
		long itemsSize = getEstimatedItemsSize(app, exporter.getItems(),
				exporter.getItemsToDelete(), exporter.getItemsToLocalDelete(), exporter.getOldItemsToDelete());
		publishProgress(itemsSize / 1024L);

		String error = null;
		try {
			exporter.export();
		} catch (IOException e) {
			SettingsHelper.LOG.error("Failed to backup items", e);
			error = e.getMessage();
		}
		return error;
	}

	public static long getEstimatedItemsSize(@NonNull OsmandApplication app,
	                                         @NonNull List<SettingsItem> items,
	                                         @NonNull List<SettingsItem> itemsToDelete,
	                                         @NonNull List<SettingsItem> itemsToLocalDelete,
	                                         @NonNull List<SettingsItem> oldItemsToDelete) {
		long size = 0;
		BackupHelper backupHelper = app.getBackupHelper();
		for (SettingsItem item : items) {
			if (item instanceof FileSettingsItem) {
				List<File> filesToUpload = backupHelper.collectItemFilesForUpload((FileSettingsItem) item);
				for (File file : filesToUpload) {
					size += file.length() + APPROXIMATE_FILE_SIZE_BYTES;
				}
			} else {
				size += item.getEstimatedSize() + APPROXIMATE_FILE_SIZE_BYTES;
			}
		}
		Map<String, RemoteFile> remoteFilesMap = backupHelper.getBackup().getRemoteFiles(PrepareBackupResult.RemoteFilesType.UNIQUE);
		if (!Algorithms.isEmpty(remoteFilesMap)) {
			for (RemoteFile remoteFile : remoteFilesMap.values()) {
				for (SettingsItem item : itemsToDelete) {
					if (item.equals(remoteFile.item)) {
						size += APPROXIMATE_FILE_SIZE_BYTES;
					}
				}
			}
			for (RemoteFile remoteFile : remoteFilesMap.values()) {
				for (SettingsItem item : oldItemsToDelete) {
					SettingsItem remoteFileItem = remoteFile.item;
					if (remoteFileItem != null && item.getType() == remoteFileItem.getType()) {
						String itemFileName = item.getFileName();
						if (itemFileName != null && itemFileName.startsWith("/")) {
							itemFileName = itemFileName.substring(1);
						}
						if (Algorithms.stringsEqual(itemFileName, remoteFileItem.getFileName())) {
							size += APPROXIMATE_FILE_SIZE_BYTES;
						}
					}
				}
			}
		}
		Map<String, LocalFile> localFilesMap = backupHelper.getBackup().getLocalFiles();
		if (!Algorithms.isEmpty(localFilesMap)) {
			for (LocalFile localFile : localFilesMap.values()) {
				for (SettingsItem item : itemsToLocalDelete) {
					if (item.equals(localFile.item)) {
						size += APPROXIMATE_FILE_SIZE_BYTES;
					}
				}
			}
		}
		return size;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (listener != null) {
			for (Object object : values) {
				if (object instanceof Integer) {
					generalProgress = (Integer) object;
					listener.onBackupExportProgressUpdate(generalProgress);
				} else if (object instanceof Long) {
					maxProgress = (Long) object;
					listener.onBackupExportStarted();
				} else if (object instanceof ItemProgressInfo) {
					ItemProgressInfo info = (ItemProgressInfo) object;

					ItemProgressInfo prevInfo = getItemProgressInfo(info.type, info.fileName);
					if (prevInfo != null) {
						info.setWork(prevInfo.work);
					}
					itemsProgress.put(info.type + info.fileName, info);

					if (info.finished) {
						listener.onBackupExportItemFinished(info.type, info.fileName);
					} else if (info.value == 0) {
						listener.onBackupExportItemStarted(info.type, info.fileName, info.work);
					} else {
						listener.onBackupExportItemProgress(info.type, info.fileName, info.value);
					}
				}
			}
		}
	}

	@Override
	protected void onCancelled() {
		onPostExecute(null);
	}

	@Override
	protected void onPostExecute(String error) {
		helper.exportAsyncTasks.remove(key);

		BackupHelper backupHelper = helper.getApp().getBackupHelper();
		backupHelper.getBackup().setError(error);

		if (listener != null) {
			listener.onBackupExportFinished(error);
		}
	}

	private NetworkExportProgressListener getProgressListener() {
		return new NetworkExportProgressListener() {

			@Override
			public void itemExportStarted(@NonNull String type, @NonNull String fileName, int work) {
				publishProgress(new ItemProgressInfo(type, fileName, 0, work, false));
			}

			@Override
			public void updateItemProgress(@NonNull String type, @NonNull String fileName, int progress) {
				publishProgress(new ItemProgressInfo(type, fileName, progress, 0, false));
			}

			@Override
			public void itemExportDone(@NonNull String type, @NonNull String fileName) {
				publishProgress(new ItemProgressInfo(type, fileName, 0, 0, true));
			}

			@Override
			public void updateGeneralProgress(int uploadedItems, int uploadedKb) {
				if (isCancelled()) {
					exporter.cancel();
				}
				publishProgress(uploadedKb);
			}

			@Override
			public void networkExportDone(@NonNull Map<String, String> errors) {

			}
		};
	}

	public static class ItemProgressInfo {

		protected final String type;
		protected final String fileName;

		private int work;
		private final int value;
		private final boolean finished;

		public ItemProgressInfo(String type, String fileName, int progress, int work, boolean finished) {
			this.type = type;
			this.fileName = fileName;
			this.value = progress;
			this.work = work;
			this.finished = finished;
		}

		public int getWork() {
			return work;
		}

		public void setWork(int work) {
			this.work = work;
		}

		public int getValue() {
			return value;
		}

		public boolean isFinished() {
			return finished;
		}
	}
}