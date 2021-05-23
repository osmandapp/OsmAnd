package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;

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

	public abstract void writeToStream(@NonNull OutputStream outputStream, @Nullable IProgress progress) throws IOException;
}
