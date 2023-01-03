package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSettingsItemReader extends SettingsItemReader<FileSettingsItem> {

	private File savedFile;

	public FileSettingsItemReader(@NonNull FileSettingsItem item) {
		super(item);
	}

	public File getSavedFile() {
		return savedFile;
	}

	@Override
	public void readFromStream(@NonNull InputStream inputStream, String entryName) throws IOException, IllegalArgumentException {
		FileSettingsItem item = getItem();
		String fileName = item.getFileName();

		savedFile = item.getFile();
		String dirName = fileName.endsWith(File.separator) ? fileName : fileName + File.separator;
		if (savedFile.isDirectory() || entryName.startsWith(dirName)) {
			savedFile = new File(savedFile, entryName.substring(fileName.length()));
		}
		if (savedFile.exists() && !item.isShouldReplace()) {
			savedFile = item.renameFile(savedFile);
		}
		if (savedFile.getParentFile() != null && !savedFile.getParentFile().exists()) {
			savedFile.getParentFile().mkdirs();
		}
		File downloadFile = FileUtils.getFileWithDownloadExtension(savedFile);
		OutputStream output = new FileOutputStream(downloadFile);
		byte[] buffer = new byte[SettingsHelper.BUFFER];
		int count;
		boolean success = false;
		try {
			while ((count = inputStream.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			output.flush();
			success = true;
		} finally {
			Algorithms.closeStream(output);
			if (!success && downloadFile.exists()) {
				downloadFile.delete();
			}
		}
		if (FileUtils.replaceTargetFile(downloadFile, savedFile)) {
			long lastModifiedTime = item.getLastModifiedTime();
			if (lastModifiedTime != -1) {
				savedFile.setLastModified(lastModifiedTime);
			}
		}
	}
}
