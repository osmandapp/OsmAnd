package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
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
		OutputStream output;
		FileSettingsItem item = getItem();
		savedFile = item.getFile();
		if (savedFile.isDirectory()) {
			savedFile = new File(savedFile, entryName.substring(item.getFileName().length()));
		}
		if (savedFile.exists() && !item.isShouldReplace()) {
			savedFile = item.renameFile(savedFile);
		}
		if (savedFile.getParentFile() != null && !savedFile.getParentFile().exists()) {
			savedFile.getParentFile().mkdirs();
		}
		output = new FileOutputStream(savedFile);
		byte[] buffer = new byte[SettingsHelper.BUFFER];
		int count;
		try {
			while ((count = inputStream.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			output.flush();
		} finally {
			Algorithms.closeStream(output);
		}
		long lastModifiedTime = item.getLastModifiedTime();
		if (lastModifiedTime != -1) {
			savedFile.setLastModified(lastModifiedTime);
		}
	}
}
