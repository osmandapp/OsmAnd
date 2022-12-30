package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
	private final OnFileMoveListener onFileMoveListener;

	public FileSettingsItemReader(@NonNull FileSettingsItem item,
	                              @Nullable OnFileMoveListener onFileMoveListener) {
		super(item);
		this.onFileMoveListener = onFileMoveListener;
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
		boolean fileMovedSuccessfully;
		try {
			notifyOnFileMoveStarted(downloadFile);
			while ((count = inputStream.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			fileMovedSuccessfully = true;
			output.flush();
		} catch (Exception e) {
			fileMovedSuccessfully = false;
		} finally {
			Algorithms.closeStream(output);
		}
		if (fileMovedSuccessfully) {
			notifyOnFileMoveFinished(downloadFile);
			if (FileUtils.replaceTargetFile(downloadFile, savedFile)) {
				long lastModifiedTime = item.getLastModifiedTime();
				if (lastModifiedTime != -1) {
					savedFile.setLastModified(lastModifiedTime);
				}
			}
		}
	}

	private void notifyOnFileMoveStarted(@NonNull File file) {
		if (onFileMoveListener != null) {
			onFileMoveListener.onFileMoveStarted(file);
		}
	}

	private void notifyOnFileMoveFinished(@NonNull File file) {
		if (onFileMoveListener != null) {
			onFileMoveListener.onFileMoveFinished(file);
		}
	}

	public interface OnFileMoveListener {
		void onFileMoveStarted(@NonNull File file);
		void onFileMoveFinished(@NonNull File file);
	}
}
