package net.osmand.plus.backup;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.backup.AbstractWriter;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class NetworkWriter implements AbstractWriter {
	private final ZipOutputStream zos;

	public NetworkWriter(@NonNull ZipOutputStream zos) {
		this.zos = zos;
	}

	@Override
	public void write(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter) throws IOException {
		SettingsItem item = itemWriter.getItem();
		String fileName = item.getFileName();
		if (Algorithms.isEmpty(fileName)) {
			fileName = item.getDefaultFileName();
		}
		writeEntry(itemWriter, fileName, zos);
	}

	private void writeEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
							@NonNull String fileName, @NonNull ZipOutputStream zos) throws IOException {
		if (itemWriter.getItem() instanceof FileSettingsItem) {
			FileSettingsItem fileSettingsItem = (FileSettingsItem) itemWriter.getItem();
			writeDirWithFiles(itemWriter, fileSettingsItem.getFile(), zos);
		} else {
			writeEntryToStream(itemWriter, fileName, zos);
		}
	}

	private void writeEntryToStream(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
									@NonNull String fileName, @NonNull ZipOutputStream zos) throws IOException {
		ZipEntry entry = createNewEntry(itemWriter, fileName);
		zos.putNextEntry(entry);
		itemWriter.writeToStream(zos);
		zos.closeEntry();
	}

	protected ZipEntry createNewEntry(@NonNull SettingsItemWriter<? extends SettingsItem> itemWriter,
									  @NonNull String fileName) {
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
			fileSettingsItem.setInputStream(new FileInputStream(file));
			writeEntryToStream(itemWriter, zipEntryName, zos);
		}
	}
}
