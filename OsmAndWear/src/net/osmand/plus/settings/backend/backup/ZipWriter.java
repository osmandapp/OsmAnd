package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipWriter extends AbstractWriter {

	private final ZipOutputStream zos;
	private final IProgress progress;

	public ZipWriter(@NonNull ZipOutputStream zos, @Nullable IProgress progress) {
		this.zos = zos;
		this.progress = progress;
	}

	@Override
	public void write(@NonNull SettingsItem item) throws IOException {
		SettingsItemWriter<? extends SettingsItem> itemWriter = item.getWriter();
		if (itemWriter != null) {
			String fileName = item.getFileName();
			if (Algorithms.isEmpty(fileName)) {
				fileName = item.getDefaultFileName();
			}
			writeEntry(itemWriter, fileName, zos);
		}
	}

	private void writeEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
							@NonNull String fileName, @NonNull ZipOutputStream zos) throws IOException {
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
			writeDirWithFiles(itemWriter, fileSettingsItem.getFile(), zos);
		} else {
			writeItemToStream(itemWriter, fileName, zos);
		}
	}

	private void writeItemToStream(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
								   @NonNull String fileName, @NonNull ZipOutputStream zos) throws IOException {
		ZipEntry entry = createNewEntry(itemWriter, fileName);
		zos.putNextEntry(entry);
		itemWriter.writeToStream(zos, progress);
		zos.closeEntry();
	}

	protected ZipEntry createNewEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
									  @NonNull String fileName) {
		if (fileName.startsWith(File.separator)) {
			fileName = fileName.substring(1);
		}
		ZipEntry entry = new ZipEntry(fileName);
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
			entry.setTime(fileSettingsItem.getFile().lastModified());
		}
		return entry;
	}

	private void writeDirWithFiles(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
								   @NonNull File file, @NonNull ZipOutputStream zos) throws IOException {
		FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File subfolderFile : files) {
					writeDirWithFiles(itemWriter, subfolderFile, zos);
				}
			}
		} else {
			String subtypeFolder = fileSettingsItem.getSubtype().getSubtypeFolder();
			String zipEntryName = Algorithms.isEmpty(subtypeFolder)
					? file.getName()
					: file.getPath().substring(file.getPath().indexOf(subtypeFolder) - 1);
			fileSettingsItem.setFileToWrite(file);
			writeItemToStream(itemWriter, zipEntryName, zos);
		}
	}
}
