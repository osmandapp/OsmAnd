package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.NetworkWriter.OnUploadItemListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.AbstractWriter;
import net.osmand.plus.settings.backend.backup.Exporter;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.backup.ExportBackupTask.APPROXIMATE_FILE_SIZE_BYTES;

public class BackupExporter extends Exporter {

	private final BackupHelper backupHelper;
	private final Map<String, SettingsItem> itemsToDelete = new LinkedHashMap<>();
	private ThreadPoolTaskExecutor<ItemWriterTask> executor;
	private final NetworkExportProgressListener listener;

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

	public Map<String, SettingsItem> getItemsToDelete() {
		return itemsToDelete;
	}

	public void addItemToDelete(SettingsItem item) throws IllegalArgumentException {
		if (itemsToDelete.containsKey(item.getName())) {
			throw new IllegalArgumentException("Already has such item: " + item.getName());
		}
		itemsToDelete.put(item.getName(), item);
	}

	@Override
	public void export() throws IOException {
		exportItems();
	}

	@Override
	protected void writeItems(@NonNull AbstractWriter writer) throws IOException {
		OperationLog log = new OperationLog("writeItems", true);
		log.startOperation();

		StringBuilder orderIdUpdateError = new StringBuilder();
		backupHelper.updateOrderId((status, message, err) -> {
			if (err != null) {
				orderIdUpdateError.append(err);
			}
		});
		if (orderIdUpdateError.length() > 0) {
			throw new IOException(orderIdUpdateError.toString());
		}

		List<ItemWriterTask> tasks = new ArrayList<>();
		for (SettingsItem item : getItems().values()) {
			tasks.add(new ItemWriterTask(writer, item));
		}
		executor = new ThreadPoolTaskExecutor<>(null);
		executor.setInterruptOnError(true);
		executor.run(tasks);

		log.finishOperation();
		if (!executor.getExceptions().isEmpty()) {
			Throwable t = executor.getExceptions().values().iterator().next();
			throw new IOException(t.getMessage(), t);
		}
	}

	private void exportItems() throws IOException {
		int[] dataProgress = {0};
		Set<Object> itemsProgress = new HashSet<>();
		Map<String, String> errors = new HashMap<>();

		OnUploadItemListener uploadItemListener = getOnUploadItemListener(itemsProgress, dataProgress, errors);
		OnDeleteFilesListener deleteFilesListener = getOnDeleteFilesListener(itemsProgress, dataProgress, errors);

		NetworkWriter networkWriter = new NetworkWriter(backupHelper, uploadItemListener);
		writeItems(networkWriter);
		deleteFiles(deleteFilesListener);
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
				for (RemoteFile remoteFile : remoteFilesMap.values()) {
					for (SettingsItem item : itemsToDelete.values()) {
						if (item.equals(remoteFile.item)) {
							remoteFiles.add(remoteFile);
						}
					}
				}
				backupHelper.deleteFiles(remoteFiles, listener);
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

	private OnUploadItemListener getOnUploadItemListener(Set<Object> itemsProgress, int[] dataProgress, Map<String, String> errors) {
		return new OnUploadItemListener() {

			@Override
			public void onItemUploadStarted(@NonNull SettingsItem item, @NonNull String fileName, int work) {
				if (listener != null) {
					listener.itemExportStarted(item.getType().name(), fileName, work);
				}
			}

			@Override
			public void onItemUploadProgress(@NonNull SettingsItem item, @NonNull String fileName, int progress, int deltaWork) {
				dataProgress[0] += deltaWork;
				if (listener != null) {
					listener.updateItemProgress(item.getType().name(), fileName, progress);
					listener.updateGeneralProgress(itemsProgress.size(), dataProgress[0]);
				}
			}

			@Override
			public void onItemFileUploadDone(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime, @Nullable String error) {
				String type = item.getType().name();
				if (!Algorithms.isEmpty(error)) {
					errors.put(type + "/" + fileName, error);
				} else {
					checkAndDeleteOldFile(item, fileName, errors);
				}
				dataProgress[0] += APPROXIMATE_FILE_SIZE_BYTES / 1024;
				if (listener != null) {
					listener.updateGeneralProgress(itemsProgress.size(), dataProgress[0]);
				}
			}

			@Override
			public void onItemUploadDone(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime, @Nullable String error) {
				String type = item.getType().name();
				if (!Algorithms.isEmpty(error)) {
					errors.put(type + "/" + fileName, error);
				}
				itemsProgress.add(item);
				if (listener != null) {
					listener.itemExportDone(item.getType().name(), fileName);
					listener.updateGeneralProgress(itemsProgress.size(), dataProgress[0]);
				}
			}
		};
	}

	private OnDeleteFilesListener getOnDeleteFilesListener(Set<Object> itemsProgress, int[] dataProgress, Map<String, String> errors) {
		return new OnDeleteFilesListener() {

			@Override
			public void onFilesDeleteStarted(@NonNull List<RemoteFile> files) {

			}

			@Override
			public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
				itemsProgress.add(file);
				if (listener != null) {
					listener.itemExportDone(file.getType(), file.getName());
					listener.updateGeneralProgress(file.getZipSize(), dataProgress[0]);
				}
			}

			@Override
			public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {

			}

			@Override
			public void onFilesDeleteError(int status, @NonNull String message) {

			}
		};
	}

	private void checkAndDeleteOldFile(@NonNull SettingsItem item, @NonNull String fileName, Map<String, String> errors) {
		String type = item.getType().name();
		try {
			ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForItem(item);
			if (exportType != null && !backupHelper.getVersionHistoryTypePref(exportType).get()) {
				RemoteFile remoteFile = backupHelper.getBackup().getRemoteFile(type, fileName);
				if (remoteFile != null) {
					backupHelper.deleteFiles(Collections.singletonList(remoteFile), true, null);
				}
			}
		} catch (UserNotRegisteredException e) {
			errors.put(type + "/" + fileName, e.getMessage());
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
