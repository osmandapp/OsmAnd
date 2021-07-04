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
import java.util.concurrent.ConcurrentHashMap;

public class ExportBackupTask extends AsyncTask<Void, Object, String> {

	private final NetworkSettingsHelper helper;
	private final BackupExporter exporter;
	private BackupExportListener listener;

	private final Map<String, ItemProgressInfo> itemsProgress = new ConcurrentHashMap<>();
	private int generalProgress;

	ExportBackupTask(@NonNull NetworkSettingsHelper helper,
					 @NonNull List<SettingsItem> items,
					 @NonNull List<RemoteFile> filesToDelete,
					 @Nullable BackupExportListener listener) {
		this.helper = helper;
		this.listener = listener;
		this.exporter = new BackupExporter(helper.getApp().getBackupHelper(), getProgressListener());
		for (SettingsItem item : items) {
			exporter.addSettingsItem(item);
		}
		for (RemoteFile file : filesToDelete) {
			exporter.addFileToDelete(file);
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

	@Nullable
	public ItemProgressInfo getItemProgressInfo(@NonNull String type, @NonNull String fileName) {
		return itemsProgress.get(type + fileName);
	}

	@Override
	protected String doInBackground(Void... voids) {
		String error = null;
		try {
			exporter.export();
		} catch (IOException e) {
			SettingsHelper.LOG.error("Failed to backup items", e);
			error = e.getMessage();
		}
		return error;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onBackupExportStarted(exporter.getItems().size() + exporter.getFilesToDelete().size());
		}
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (listener != null) {
			for (Object object : values) {
				if (object instanceof Integer) {
					listener.onBackupExportProgressUpdate((Integer) object);
				} else if (object instanceof ItemProgressInfo) {
					ItemProgressInfo info = (ItemProgressInfo) object;
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
		helper.exportTask = null;
		if (listener != null) {
			listener.onBackupExportFinished(error);
		}
	}

	private NetworkExportProgressListener getProgressListener() {
		return new NetworkExportProgressListener() {

			@Override
			public void itemExportStarted(@NonNull String type, @NonNull String fileName, int work) {
				ItemProgressInfo progressInfo = new ItemProgressInfo(type, fileName, 0, work, false);
				itemsProgress.put(type + fileName, progressInfo);
				publishProgress(progressInfo);
			}

			@Override
			public void updateItemProgress(@NonNull String type, @NonNull String fileName, int progress) {
				int work = 0;
				ItemProgressInfo prevInfo = getItemProgressInfo(type, fileName);
				if (prevInfo != null) {
					work = prevInfo.work;
				}
				ItemProgressInfo progressInfo = new ItemProgressInfo(type, fileName, progress, work, false);
				itemsProgress.put(type + fileName, progressInfo);
				publishProgress(progressInfo);
			}

			@Override
			public void itemExportDone(@NonNull String type, @NonNull String fileName) {
				int work = 0;
				ItemProgressInfo prevInfo = getItemProgressInfo(type, fileName);
				if (prevInfo != null) {
					work = prevInfo.work;
				}
				ItemProgressInfo progressInfo = new ItemProgressInfo(type, fileName, 0, work, true);
				itemsProgress.put(type + fileName, progressInfo);
				publishProgress(progressInfo);
			}

			@Override
			public void updateGeneralProgress(int uploadedItems, int uploadedKb) {
				exporter.setCancelled(isCancelled());
				generalProgress = uploadedItems;
				publishProgress(uploadedItems);
			}

			@Override
			public void networkExportDone(@NonNull Map<String, String> errors) {

			}
		};
	}

	public static class ItemProgressInfo {

		private final String type;
		private final String fileName;

		public final int work;
		public final int value;
		public final boolean finished;

		public ItemProgressInfo(String type, String fileName, int progress, int work, boolean finished) {
			this.type = type;
			this.fileName = fileName;
			this.value = progress;
			this.work = work;
			this.finished = finished;
		}
	}
}