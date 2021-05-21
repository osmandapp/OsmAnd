package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class SettingsItemWriter<T extends SettingsItem> {

	private final T item;

	public SettingsItemWriter(T item) {
		this.item = item;
	}

	public T getItem() {
		return item;
	}

	public abstract boolean writeToStream(@NonNull OutputStream outputStream) throws IOException;
}
