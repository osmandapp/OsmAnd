package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.StreamWriter;
import net.osmand.plus.backup.BackupHelper.OnUploadFileListener;
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
		if (itemWriter != null) {
			try {
				if (uploadEntry(itemWriter, fileName) == null) {
					uploadItemInfo(item, fileName + BackupHelper.INFO_EXT);
				}
			} catch (UserNotRegisteredException e) {
				throw new IOException(e.getMessage(), e);
			}
		} else {
			uploadItemInfo(item, fileName + BackupHelper.INFO_EXT);
		}
	}

	private String uploadEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
							   @NonNull String fileName) throws UserNotRegisteredException, IOException {
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
			return uploadDirWithFiles(itemWriter, fileSettingsItem.getFile());
		} else {
			return uploadItemFile(itemWriter, fileName);
		}
	}

	private String uploadItemInfo(@NonNull SettingsItem item, @NonNull String fileName) throws IOException {
		try {
			JSONObject json = item.toJsonObj();
			boolean hasFile = json.has("file");
			if (json.length() > (hasFile ? 2 : 1)) {
				String itemJson = json.toString();
				InputStream inputStream = new ByteArrayInputStream(itemJson.getBytes("UTF-8"));
				StreamWriter streamWriter = new StreamWriter() {
					@Override
					public void write(OutputStream outputStream, IProgress progress) throws IOException {
						Algorithms.streamCopy(inputStream, outputStream, progress, 1024);
						outputStream.flush();
					}
				};
				return backupHelper.uploadFileSync(fileName, item.getType().name(), streamWriter,
						getUploadFileListener(item));
			} else {
				return null;
			}
		} catch (JSONException | UserNotRegisteredException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private String uploadItemFile(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
								  @NonNull String fileName) throws UserNotRegisteredException, IOException {
		StreamWriter streamWriter = new StreamWriter() {
			@Override
			public void write(OutputStream outputStream, IProgress progress) throws IOException {
				itemWriter.writeToStream(outputStream, progress);
			}
		};
		return backupHelper.uploadFileSync(fileName, itemWriter.getItem().getType().name(), streamWriter,
				getUploadFileListener(itemWriter.getItem()));
	}

	private String uploadDirWithFiles(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
									  @NonNull File file) throws UserNotRegisteredException, IOException {
		String error = null;
		FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File subfolderFile : files) {
					String err = uploadDirWithFiles(itemWriter, subfolderFile);
					if (err != null) {
						error = err;
					}
				}
			}
		} else {
			String fileName = BackupHelper.getFileItemName(file, fileSettingsItem);
			fileSettingsItem.setInputStream(new FileInputStream(file));
			error = uploadItemFile(itemWriter, fileName);
		}
		return error;
	}

	private OnUploadFileListener getUploadFileListener(final @NonNull SettingsItem item) {
		return new OnUploadFileListener() {
			@Override
			public void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				if (listener != null) {
					listener.onItemFileUploadProgress(item, fileName, progress, deltaWork);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error) {
				if (listener != null) {
					listener.onItemFileUploadDone(item, fileName, uploadTime, error);
				}
			}
		};
	}
}
