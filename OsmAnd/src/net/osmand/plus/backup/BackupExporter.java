package net.osmand.plus.backup;

import static net.osmand.plus.backup.ExportBackupTask.APPROXIMATE_FILE_SIZE_BYTES;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.NetworkWriter.OnUploadItemListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.AbstractWriter;
import net.osmand.plus.settings.backend.backup.Exporter;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupExporter extends Exporter {

	private static final int MAX_LIGHT_ITEM_SIZE = 10 * 1024 * 1024;

	private final BackupHelper backupHelper;
	private final List<SettingsItem> itemsToDelete = new ArrayList<>();
	private final List<SettingsItem> itemsToLocalDelete = new ArrayList<>();
	private final List<SettingsItem> oldItemsToDelete = new ArrayList<>();
	private ThreadPoolTaskExecutor<ItemWriterTask> executor;
	private final NetworkExportProgressListener listener;
	private List<RemoteFile> oldFilesToDelete = new ArrayList<>();

	public interface NetworkExportProgressListener {
		void itemExportStarted(@NonNull String type, @NonNull String fileName, int work);

		void updateItemProgress(@NonNull String type, @NonNull String fileName, int progress);

		void itemExportDone(@NonNull String type, @NonNull String fileName);

		void updateGeneralProgress(int uploadedItems, int uploadedKb);

		void networkExportDone(@NonNull Map<String, String> errors);
	}

	BackupExporter(@NonNull BackupHelper backupHelper, @Nullable NetworkExportProgressListener listener) {
		super(null);
		this.backupHelper = backupHelper;
		this.listener = listener;
	}

	@NonNull
	public List<SettingsItem> getItemsToDelete() {
		return itemsToDelete;
	}

	public List<SettingsItem> getItemsToLocalDelete() {
		return itemsToLocalDelete;
	}

	@NonNull
	public List<SettingsItem> getOldItemsToDelete() {
		return oldItemsToDelete;
	}

	public void addItemToDelete(@NonNull SettingsItem item) throws IllegalArgumentException {
		itemsToDelete.add(item);
	}

	public void addItemToLocalDelete(@NonNull SettingsItem item) throws IllegalArgumentException {
		itemsToLocalDelete.add(item);
	}

	public void addOldItemToDelete(@NonNull SettingsItem item) throws IllegalArgumentException {
		oldItemsToDelete.add(item);
	}

	@Override
	public void export() throws IOException {
		exportItems();
	}

	@Override
	protected void writeItems(@NonNull AbstractWriter writer) throws IOException {
		OperationLog log = new OperationLog("writeItems", true);
		log.startOperation();

		StringBuilder subscriptionError = new StringBuilder();
		backupHelper.checkSubscriptions((status, message, err) -> {
			if (err != null) {
				subscriptionError.append(err);
			}
		});
		if (subscriptionError.length() > 0) {
			throw new IOException(subscriptionError.toString());
		}

		List<ItemWriterTask> lightTasks = new ArrayList<>();
		List<ItemWriterTask> heavyTasks = new ArrayList<>();
		for (SettingsItem item : getItems()) {
			if (item.getEstimatedSize() > MAX_LIGHT_ITEM_SIZE) {
				heavyTasks.add(new ItemWriterTask(writer, item));
			} else {
				lightTasks.add(new ItemWriterTask(writer, item));
			}
		}
		if (!lightTasks.isEmpty()) {
			executor = new ThreadPoolTaskExecutor<>(null);
			executor.setInterruptOnError(true);
			executor.run(lightTasks);
			if (!executor.getExceptions().isEmpty()) {
				log.finishOperation();
				Throwable t = executor.getExceptions().values().iterator().next();
				throw new IOException(t.getMessage(), t);
			}
		}
		if (!heavyTasks.isEmpty()) {
			executor = new ThreadPoolTaskExecutor<>(1, null);
			executor.setInterruptOnError(true);
			executor.run(heavyTasks);
			if (!executor.getExceptions().isEmpty()) {
				log.finishOperation();
				Throwable t = executor.getExceptions().values().iterator().next();
				throw new IOException(t.getMessage(), t);
			}
		}
		log.finishOperation();
	}

	private void exportItems() throws IOException {
		AtomicInteger dataProgress = new AtomicInteger(0);
		Set<Object> itemsProgress = Collections.synchronizedSet(new HashSet<>());
		Map<String, String> errors = new ConcurrentHashMap<>();

		OnUploadItemListener uploadItemListener = getOnUploadItemListener(itemsProgress, dataProgress, errors);
		OnDeleteFilesListener deleteFilesListener = getOnDeleteFilesListener(itemsProgress, dataProgress);

		NetworkWriter networkWriter = new NetworkWriter(backupHelper, uploadItemListener);
		writeItems(networkWriter);
		deleteFiles(deleteFilesListener);
		deleteOldFiles(deleteFilesListener);
		deleteLocalFiles(itemsProgress, dataProgress);
		if (!isCancelled()) {
			backupHelper.updateBackupUploadTime();
		}
		if (listener != null) {
			listener.networkExportDone(errors);
		}
	}

	protected void deleteFiles(OnDeleteFilesListener listener) throws IOException {
		try {
			List<RemoteFile> remoteFiles = new ArrayList<>();
			Map<String, RemoteFile> remoteFilesMap = backupHelper.getBackup().getRemoteFiles(RemoteFilesType.UNIQUE);
			if (remoteFilesMap != null) {
				List<SettingsItem> itemsToDelete = this.itemsToDelete;
				for (RemoteFile remoteFile : remoteFilesMap.values()) {
					for (SettingsItem item : itemsToDelete) {
						if (item.equals(remoteFile.item)) {
							remoteFiles.add(remoteFile);
						}
					}
				}
				if (!Algorithms.isEmpty(remoteFiles)) {
					backupHelper.deleteFilesSync(remoteFiles, false, AsyncTask.THREAD_POOL_EXECUTOR, listener);
				}
			}
		} catch (UserNotRegisteredException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	protected void deleteLocalFiles(Set<Object> itemsProgress, AtomicInteger dataProgress) {
		List<SettingsItem> itemsToLocalDelete = this.itemsToLocalDelete;
		for (SettingsItem item : itemsToLocalDelete) {
			item.delete();
			itemsProgress.add(item);
			if (listener != null) {
				int p = dataProgress.addAndGet(APPROXIMATE_FILE_SIZE_BYTES / 1024);
				String fileName = BackupHelper.getItemFileName(item);
				listener.itemExportDone(item.getType().name(), fileName);
				listener.updateGeneralProgress(itemsProgress.size(), p);
			}
		}
	}

	protected void deleteOldFiles(OnDeleteFilesListener listener) throws IOException {
		try {
			if (!Algorithms.isEmpty(oldFilesToDelete)) {
				backupHelper.deleteFilesSync(oldFilesToDelete, true, AsyncTask.THREAD_POOL_EXECUTOR, listener);
			}
		} catch (UserNotRegisteredException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		if (executor != null) {
			executor.cancel();
		}
	}

	private OnUploadItemListener getOnUploadItemListener(Set<Object> itemsProgress, AtomicInteger dataProgress, Map<String, String> errors) {
		return new OnUploadItemListener() {

			@Override
			public void onItemUploadStarted(@NonNull SettingsItem item, @NonNull String fileName, int work) {
				if (listener != null) {
					listener.itemExportStarted(item.getType().name(), fileName, work);
				}
			}

			@Override
			public void onItemUploadProgress(@NonNull SettingsItem item, @NonNull String fileName, int progress, int deltaWork) {
				int p = dataProgress.addAndGet(deltaWork);
				if (listener != null) {
					listener.updateItemProgress(item.getType().name(), fileName, progress);
					listener.updateGeneralProgress(itemsProgress.size(), p);
				}
			}

			@Override
			public void onItemFileUploadDone(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime, @Nullable String error) {
				String type = item.getType().name();
				if (!Algorithms.isEmpty(error)) {
					errors.put(type + "/" + fileName, error);
				} else {
					markOldFileForDeletion(item, fileName);
				}
				int p = dataProgress.addAndGet(APPROXIMATE_FILE_SIZE_BYTES / 1024);
				if (listener != null) {
					listener.updateGeneralProgress(itemsProgress.size(), p);
				}
			}

			@Override
			public void onItemUploadDone(@NonNull SettingsItem item, @NonNull String fileName, @Nullable String error) {
				String type = item.getType().name();
				if (!Algorithms.isEmpty(error)) {
					errors.put(type + "/" + fileName, error);
				}
				itemsProgress.add(item);
				if (listener != null) {
					listener.itemExportDone(item.getType().name(), fileName);
					listener.updateGeneralProgress(itemsProgress.size(), dataProgress.get());
				}
			}
		};
	}

	private OnDeleteFilesListener getOnDeleteFilesListener(Set<Object> itemsProgress, AtomicInteger dataProgress) {
		return new OnDeleteFilesListener() {
			@Override
			public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
				int p = dataProgress.addAndGet(APPROXIMATE_FILE_SIZE_BYTES / 1024);
				itemsProgress.add(file);
				if (listener != null) {
					listener.itemExportDone(file.getType(), file.getName());
					listener.updateGeneralProgress(itemsProgress.size(), p);
				}
			}
		};
	}

	private void markOldFileForDeletion(@NonNull SettingsItem item, @NonNull String fileName) {
		String type = item.getType().name();
		ExportType exportType = ExportType.findBy(item);
		if (exportType != null && !backupHelper.getVersionHistoryTypePref(exportType).get()) {
			RemoteFile remoteFile = backupHelper.getBackup().getRemoteFile(type, fileName);
			if (remoteFile != null) {
				oldFilesToDelete = CollectionUtils.addToList(oldFilesToDelete, remoteFile);
			}
		}
	}

	private static class ItemWriterTask extends ThreadPoolTaskExecutor.Task {

		private final AbstractWriter writer;
		private final SettingsItem item;

		public ItemWriterTask(@NonNull AbstractWriter writer, @NonNull SettingsItem item) {
			this.writer = writer;
			this.item = item;
		}

		@Override
		public Void call() throws Exception {
			writer.write(item);
			return null;
		}

		@Override
		public void cancel() {
			super.cancel();
			writer.cancel();
		}
	}
}
