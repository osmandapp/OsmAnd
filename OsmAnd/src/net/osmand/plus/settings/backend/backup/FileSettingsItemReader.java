package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSettingsItemReader extends SettingsItemReader<FileSettingsItem> {

	private static final Log LOG = PlatformUtil.getLog(FileSettingsItemReader.class);

	private File savedFile;

	public FileSettingsItemReader(@NonNull FileSettingsItem item) {
		super(item);
	}

	public File getSavedFile() {
		return savedFile;
	}

	@Override
	public void readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
	                           @Nullable String entryName) throws IOException, IllegalArgumentException {
		FileSettingsItem item = getItem();
		String fileName = item.getFileName();
		if (fileName == null || entryName == null) {
			throw new IllegalArgumentException("Item fileName or entryName is null");
		}

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

		if (inputFile != null && inputStream instanceof FileInputStream) {
			writeTargetFile(inputFile, savedFile, item);
			return;
		}

		File tempFile = FileUtils.getFileWithDownloadExtension(savedFile);
		OutputStream output = new FileOutputStream(tempFile);
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
			if (!success && tempFile.exists()) {
				tempFile.delete();
			}
		}
		writeTargetFile(tempFile, savedFile, item);
	}

	private void writeTargetFile(@NonNull File tempFile, @NonNull File targetFile, @NonNull FileSettingsItem item) {
		long lastModifiedTime = item.getLastModifiedTime();
		boolean replaced = FileUtils.replaceTargetFile(tempFile, targetFile);
		if (replaced) {
			if (lastModifiedTime > 0) {
				targetFile.setLastModified(lastModifiedTime);
			}
		}
		if (PluginsHelper.isDevelopment()) {
			LOG.debug(" target " + targetFile.getAbsolutePath() + " replaced " + replaced + " temp "
					+ tempFile.getAbsolutePath() + " itemTime " + lastModifiedTime + " fileTime " + targetFile.lastModified());
		}
	}
}
