package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NetworkWriter implements AbstractWriter {

	private final BackupHelper backupHelper;
	private final OnUploadItemListener listener;

	public interface OnUploadItemListener {
		void onItemFileUploadStarted(@NonNull SettingsItem item, @NonNull String fileName, int work);

		void onItemFileUploadProgress(@NonNull SettingsItem item, @NonNull String fileName, int progress, int deltaWork);

		void onItemFileUploadDone(@NonNull SettingsItem item, @NonNull String fileName, long uploadTime, @Nullable String error);
	}

	public NetworkWriter(@NonNull BackupHelper backupHelper, @Nullable OnUploadItemListener listener) {
		this.backupHelper = backupHelper;
		this.listener = listener;
	}

	@Override
	public void write(@NonNull SettingsItem item) throws IOException {
		String fileName = BackupHelper.getItemFileName(item);
		SettingsItemWriter<? extends SettingsItem> itemWriter = item.getWriter();
		long uploadTime = System.currentTimeMillis();
		String error;
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
		if (error != null) {
			throw new IOException(error);
		}
	}

	private String uploadEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
							   @NonNull String fileName, long uploadTime) throws UserNotRegisteredException, IOException {
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
			return uploadDirWithFiles(itemWriter, fileSettingsItem.getFile(), uploadTime);
		} else {
			return uploadItemFile(itemWriter, fileName, uploadTime);
		}
	}

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

	private String uploadItemFile(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
								  @NonNull String fileName, long uploadTime) throws UserNotRegisteredException, IOException {
		StreamWriter streamWriter = new StreamWriter() {
			@Override
			public void write(OutputStream outputStream, IProgress progress) throws IOException {
				itemWriter.writeToStream(outputStream, progress);
			}
		};
		return backupHelper.uploadFile(fileName, itemWriter.getItem().getType().name(), streamWriter, uploadTime,
				getUploadFileListener(itemWriter.getItem()));
	}

	private String uploadDirWithFiles(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
									  @NonNull File file, long uploadTime) throws UserNotRegisteredException, IOException {
		FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File subfolderFile : files) {
					String error = uploadDirWithFiles(itemWriter, subfolderFile, uploadTime);
					if (error != null) {
						return error;
					}
				}
			}
		} else {
			String fileName = BackupHelper.getFileItemName(file, fileSettingsItem);
			fileSettingsItem.setInputStream(new FileInputStream(file));
			return uploadItemFile(itemWriter, fileName, uploadTime);
		}
		return null;
	}

	private OnUploadFileListener getUploadFileListener(final @NonNull SettingsItem item) {
		return new OnUploadFileListener() {

			@Override
			public void onFileUploadStarted(@NonNull String type, @NonNull String fileName, int work) {
				if (listener != null) {
					listener.onItemFileUploadStarted(item, fileName, work);
				}
			}

			@Override
			public void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				if (listener != null) {
					listener.onItemFileUploadProgress(item, fileName, progress, deltaWork);
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
		};
	}
}
