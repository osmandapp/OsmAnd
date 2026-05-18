package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StreamWriter;
import net.osmand.plus.backup.BackupListeners.OnUploadFileListener;
import net.osmand.plus.base.ProgressHelper;
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
import java.util.List;

public class NetworkWriter extends AbstractWriter {

	private final BackupHelper backupHelper;
	private final OnUploadItemListener listener;

	public interface OnUploadItemListener {
		void onItemUploadStarted(@NonNull SettingsItem item, @NonNull String fileName, int work);

		void onItemUploadProgress(@NonNull SettingsItem item, @NonNull String fileName, int progress, int deltaWork);

		void onItemFileUploadDone(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime, @Nullable String error);

		void onItemUploadDone(@NonNull SettingsItem item, @NonNull String fileName, @Nullable String error);
	}

	public NetworkWriter(@NonNull BackupHelper backupHelper, @Nullable OnUploadItemListener listener) {
		this.backupHelper = backupHelper;
		this.listener = listener;
	}

	@Override
	public void write(@NonNull SettingsItem item) throws IOException {
		String error;
		String fileName = BackupUtils.getItemFileName(item);
		SettingsItemWriter<? extends SettingsItem> itemWriter = item.getWriter();
		if (itemWriter != null) {
			try {
				error = uploadEntry(itemWriter, fileName);
				if (error == null) {
					error = uploadItemInfo(item, fileName + BackupHelper.INFO_EXT);
				}
			} catch (UserNotRegisteredException e) {
				throw new IOException(e.getMessage(), e);
			}
		} else {
			error = uploadItemInfo(item, fileName + BackupHelper.INFO_EXT);
		}
		if (listener != null) {
			listener.onItemUploadDone(item, fileName, error);
		}
		if (error != null) {
			throw new IOException(error);
		}
	}

	@Nullable
	private String uploadEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
	                           @NonNull String fileName)
			throws UserNotRegisteredException, IOException {
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			return uploadDirWithFiles(itemWriter, fileName);
		} else {
			return uploadItemFile(itemWriter, fileName, getUploadFileListener(itemWriter.getItem()));
		}
	}

	@Nullable
	private String uploadItemInfo(@NonNull SettingsItem item, @NonNull String fileName) throws IOException {
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
				return backupHelper.uploadFile(fileName, item.getType().name(), item.getLastModifiedTime(),
						streamWriter, getUploadFileListener(item));
			} else {
				return null;
			}
		} catch (JSONException | UserNotRegisteredException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Nullable
	private String uploadItemFile(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
	                              @NonNull String fileName, @Nullable OnUploadFileListener listener)
			throws UserNotRegisteredException, IOException {
		if (isCancelled()) {
			throw new InterruptedIOException();
		} else {
			SettingsItem item = itemWriter.getItem();
			String type = item.getType().name();
			StreamWriter streamWriter = getStreamWriter(itemWriter, fileName);
			return backupHelper.uploadFile(fileName, type, item.getLastModifiedTime(), streamWriter, listener);
		}
	}

	private StreamWriter getStreamWriter(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter, @NonNull String fileName) {
		boolean useEmptyStreamWriter = shouldUseEmptyStreamWriter(itemWriter, fileName);
		if (useEmptyStreamWriter) {
			return (outputStream, progress) -> {
				FileSettingsItem item = (FileSettingsItem) itemWriter.getItem();
				Algorithms.closeStream(item.getInputStream());

				if (progress != null) {
					int work = (int) (item.getEstimatedSize() / 1024);
					progress.startWork(work);
					progress.progress(work);
					progress.finishTask();
				}
			};
		}
		return itemWriter::writeToStream;
	}

	private boolean shouldUseEmptyStreamWriter(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter, @NonNull String fileName) {
		SettingsItem item = itemWriter.getItem();
		if (item instanceof FileSettingsItem) {
			FileSettingsItem settingsItem = (FileSettingsItem) item;
			return BackupUtils.isDefaultObfMap(backupHelper.getApp(), settingsItem, fileName);
		}
		return false;
	}

	@Nullable
	private String uploadDirWithFiles(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
	                                  @NonNull String fileName)
			throws UserNotRegisteredException, IOException {
		FileSettingsItem item = (FileSettingsItem) itemWriter.getItem();
		List<File> filesToUpload = backupHelper.collectItemFilesForUpload(item);
		if (filesToUpload.isEmpty()) {
			return "No files to upload";
		}
		long size = 0;
		for (File file : filesToUpload) {
			size += file.length();
		}
		OnUploadFileListener uploadListener = getUploadDirListener(item, fileName, (int) (size / 1024));
		for (File file : filesToUpload) {
			item.setFileToWrite(file);
			String name = BackupUtils.getFileItemName(file, item);
			String error = uploadItemFile(itemWriter, name, uploadListener);
			if (error != null) {
				return error;
			}
		}
		return null;
	}

	@NonNull
	private OnUploadFileListener getUploadFileListener(@NonNull SettingsItem item) {
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
					FileSettingsItem fileItem = (FileSettingsItem) item;
					String itemFileName = BackupUtils.getFileItemName(fileItem);
					if (backupHelper.getApp().getAppPath(itemFileName).isDirectory()) {
						backupHelper.updateFileUploadTime(item.getType().name(), itemFileName, uploadTime);
					}
					if (fileItem.needMd5Digest() && !Algorithms.isEmpty(fileItem.getMd5Digest())) {
						backupHelper.updateFileMd5Digest(item.getType().name(), fileName, fileItem.getMd5Digest());
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

			private ProgressHelper progressHelper;
			private boolean uploadStarted;

			@Override
			public void onFileUploadStarted(@NonNull String type, @NonNull String fileName, int work) {
				progressHelper = new ProgressHelper(() -> {
					if (listener != null) {
						int progress = progressHelper.getLastKnownProgress();
						int deltaProgress = progressHelper.getLastAddedDeltaProgress();
						listener.onItemUploadProgress(item, itemFileName, progress, deltaProgress);
					}
				});
				if (!uploadStarted && listener != null) {
					uploadStarted = true;
					progressHelper.onStartWork(itemWork);
					listener.onItemUploadStarted(item, itemFileName, progressHelper.getTotalWork());
				}
			}

			@Override
			public void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				if (listener != null) {
					progressHelper.onProgress(deltaWork);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error) {
				if (item instanceof FileSettingsItem) {
					FileSettingsItem fileItem = (FileSettingsItem) item;
					String itemFileName = BackupUtils.getFileItemName(fileItem);
					if (backupHelper.getApp().getAppPath(itemFileName).isDirectory()) {
						backupHelper.updateFileUploadTime(item.getType().name(), itemFileName, uploadTime);
					}
					if (fileItem.needMd5Digest() && !Algorithms.isEmpty(fileItem.getMd5Digest())) {
						backupHelper.updateFileMd5Digest(item.getType().name(), fileName, fileItem.getMd5Digest());
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
