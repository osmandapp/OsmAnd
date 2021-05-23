package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.StreamWriter;
import net.osmand.plus.backup.BackupHelper.OnUploadFileListener;
import net.osmand.plus.settings.backend.backup.AbstractWriter;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NetworkWriter implements AbstractWriter {
	private final BackupHelper backupHelper;
	private final OnUploadFileListener listener;

	public NetworkWriter(@NonNull BackupHelper backupHelper, @Nullable OnUploadFileListener listener) {
		this.backupHelper = backupHelper;
		this.listener = listener;
	}

	@Override
	public void write(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter) throws IOException {
		SettingsItem item = itemWriter.getItem();
		String fileName = item.getFileName();
		if (Algorithms.isEmpty(fileName)) {
			fileName = item.getDefaultFileName();
		}
		try {
			writeEntry(itemWriter, fileName);
		} catch (UserNotRegisteredException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private void writeEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
							@NonNull String fileName) throws UserNotRegisteredException, IOException {
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
			writeDirWithFiles(itemWriter, fileSettingsItem.getFile());
		} else {
			writeItemToStream(itemWriter, fileName);
		}
	}

	private void writeItemToStream(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
								   @NonNull String fileName) throws UserNotRegisteredException, IOException {
		StreamWriter streamWriter = new StreamWriter() {
			@Override
			public void write(OutputStream outputStream, IProgress progress) throws IOException {
				itemWriter.writeToStream(outputStream, progress);
			}
		};
		backupHelper.uploadFile(fileName, streamWriter, listener);
	}

	private void writeDirWithFiles(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
								   @NonNull File file) throws UserNotRegisteredException, IOException {
		FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File subfolderFile : files) {
					writeDirWithFiles(itemWriter, subfolderFile);
				}
			}
		} else {
			String subtypeFolder = fileSettingsItem.getSubtype().getSubtypeFolder();
			String fileName = Algorithms.isEmpty(subtypeFolder)
					? file.getName()
					: file.getPath().substring(file.getPath().indexOf(subtypeFolder) - 1);
			fileSettingsItem.setInputStream(new FileInputStream(file));
			writeItemToStream(itemWriter, fileName);
		}
	}
}
