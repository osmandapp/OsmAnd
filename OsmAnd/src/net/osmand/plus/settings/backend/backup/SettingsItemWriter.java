package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class SettingsItemWriter<T extends SettingsItem> {

	private T item;

	public SettingsItemWriter(T item) {
		this.item = item;
	}

	public T getItem() {
		return item;
	}

	public abstract boolean writeToStream(@NonNull OutputStream outputStream) throws IOException;

	public void writeEntry(String fileName, @NonNull ZipOutputStream zos) throws IOException {
		ZipEntry entry = createNewEntry(fileName);
		zos.putNextEntry(entry);
		writeToStream(zos);
		zos.closeEntry();
	}

	public ZipEntry createNewEntry(String fileName) {
		return new ZipEntry(fileName);
	}
}
