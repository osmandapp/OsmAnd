package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.FileUtils;
import net.osmand.StreamWriter;
import net.osmand.plus.backup.BackupListeners.OnUploadFileListener;
import net.osmand.plus.settings.backend.backup.AbstractWriter;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

public class NetworkWriter extends AbstractWriter {

	private final BackupHelper backupHelper;
	private final OnUploadItemListener listener;

	public interface OnUploadItemListener {
		void onItemUploadStarted(@NonNull SettingsItem item, @NonNull String fileName, int work);

		void onItemUploadProgress(@NonNull SettingsItem item, @NonNull String fileName, int progress, int deltaWork);

		void onItemFileUploadDone(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime, @Nullable String error);

		void onItemUploadDone(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime, @Nullable String error);
	}

	public NetworkWriter(@NonNull BackupHelper backupHelper, @Nullable OnUploadItemListener listener) {
		this.backupHelper = backupHelper;
		this.listener = listener;
	}

	@Override
	public void write(@NonNull SettingsItem item) throws IOException {
		String error;
		long uploadTime = System.currentTimeMillis();
		String fileName = BackupHelper.getItemFileName(item);
		SettingsItemWriter<? extends SettingsItem> itemWriter = item.getWriter();
		if (itemWriter != null) {
			try {
				error = uploadEntry(itemWriter, fileName, uploadTime);
				if (error == null) {
					error = uploadItemInfo(item, fileName + BackupHelper.INFO_EXT, uploadTime);
				}
			} catch (UserNotRegisteredException e) {
				throw new IOException(e.getMessage(), e);
			}
		} else {
			error = uploadItemInfo(item, fileName + BackupHelper.INFO_EXT, uploadTime);
		}
		if (listener != null) {
			listener.onItemUploadDone(item, fileName, uploadTime, error);
		}
		if (error != null) {
			throw new IOException(error);
		}
	}

	@Nullable
	private String uploadEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
							   @NonNull String fileName, long uploadTime)
			throws UserNotRegisteredException, IOException {
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			return uploadDirWithFiles(itemWriter, fileName, uploadTime);
		} else {
			return uploadItemFile(itemWriter, fileName, getUploadFileListener(itemWriter.getItem()), uploadTime);
		}
	}

	@Nullable
	private String uploadItemInfo(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime) throws IOException {
		try {
			JSONObject json = item.toJsonObj();
			boolean hasFile = json.has("file");
			boolean hasSubtype = json.has("subtype");
			if (json.length() > (hasFile ? 2 + (hasSubtype ? 1 : 0) : 1)) {
				String itemJson = json.toString();
				InputStream inputStream = new ByteArrayInputStream(itemJson.getBytes("UTF-8"));
				StreamWriter streamWriter = (outputStream, progress) -> {
					Algorithms.streamCopy(inputStream, outputStream, progress, 1024);
					outputStream.flush();
				};
				return backupHelper.uploadFile(fileName, item.getType().name(), streamWriter, uploadTime,
						getUploadFileListener(item));
			} else {
				return null;
			}
		} catch (JSONException | UserNotRegisteredException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Nullable
	private String uploadItemFile(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
								  @NonNull String fileName, @Nullable OnUploadFileListener listener,
								  long uploadTime) throws UserNotRegisteredException, IOException {
		if (isCancelled()) {
			throw new InterruptedIOException();
		} else {
			return backupHelper.uploadFile(fileName, itemWriter.getItem().getType().name(),
					itemWriter::writeToStream, uploadTime, listener);
		}
	}

	@Nullable
	private String uploadDirWithFiles(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
									  @NonNull String fileName, long uploadTime)
			throws UserNotRegisteredException, IOException {
		FileSettingsItem item = (FileSettingsItem) itemWriter.getItem();
		List<File> filesToUpload = backupHelper.collectItemFilesForUpload(item);
		long size = 0;
		for (File file : filesToUpload) {
			size += file.length();
		}
		OnUploadFileListener uploadListener = getUploadDirListener(item, fileName, (int) (size / 1024));
		for (File file : filesToUpload) {
			item.setFileToWrite(file);
			String name = BackupHelper.getFileItemName(file, item);
			String error = uploadItemFile(itemWriter, name, uploadListener, uploadTime);
			if (error != null) {
				return error;
			}
		}
		return null;
	}

	@NonNull
	private OnUploadFileListener getUploadFileListener(final @NonNull SettingsItem item) {
		return new OnUploadFileListener() {

			@Override
			public void onFileUploadStarted(@NonNull String type, @NonNull String fileName, int work) {
				if (listener != null) {
					listener.onItemUploadStarted(item, fileName, work);
				}
			}

			@Override
			public void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				if (listener != null) {
					listener.onItemUploadProgress(item, fileName, progress, deltaWork);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error) {
				if (item instanceof FileSettingsItem) {
					String itemFileName = BackupHelper.getFileItemName((FileSettingsItem) item);
					if (backupHelper.getApp().getAppPath(itemFileName).isDirectory()) {
						backupHelper.updateFileUploadTime(item.getType().name(), itemFileName, uploadTime);
					}
				}
				if (listener != null) {
					listener.onItemFileUploadDone(item, fileName, uploadTime, error);
				}
			}

			@Override
			public boolean isUploadCancelled() {
				return isCancelled();
			}
		};
	}

	@NonNull
	private OnUploadFileListener getUploadDirListener(@NonNull SettingsItem item, @NonNull String itemFileName, int itemWork) {
		return new OnUploadFileListener() {

			private int itemProgress = 0;
			private int deltaProgress = 0;
			private boolean uploadStarted = false;

			@Override
			public void onFileUploadStarted(@NonNull String type, @NonNull String fileName, int work) {
				if (!uploadStarted && listener != null) {
					uploadStarted = true;
					listener.onItemUploadStarted(item, itemFileName, itemWork);
				}
			}

			@Override
			public void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				if (listener != null) {
					deltaProgress += deltaWork;
					if ((deltaProgress > (itemWork / 100)) || ((itemProgress + deltaProgress) >= itemWork)) {
						itemProgress += deltaProgress;
						listener.onItemUploadProgress(item, itemFileName, itemProgress, deltaProgress);
						deltaProgress = 0;
					}
				}
			}

			@Override
			public void onFileUploadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error) {
				if (item instanceof FileSettingsItem) {
					String itemFileName = BackupHelper.getFileItemName((FileSettingsItem) item);
					if (backupHelper.getApp().getAppPath(itemFileName).isDirectory()) {
						backupHelper.updateFileUploadTime(item.getType().name(), itemFileName, uploadTime);
					}
				}
				if (listener != null) {
					listener.onItemFileUploadDone(item, fileName, uploadTime, error);
				}
			}

			@Override
			public boolean isUploadCancelled() {
				return isCancelled();
			}
		};
	}
}
