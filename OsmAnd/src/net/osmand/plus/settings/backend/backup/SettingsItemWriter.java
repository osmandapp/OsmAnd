package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;

public abstract class SettingsItemWriter<T extends SettingsItem> {

	private T item;

	public SettingsItemWriter(T item) {
		this.item = item;
	}

	public T getItem() {
		return item;
	}

	public abstract boolean writeToStream(@NonNull OutputStream outputStream) throws IOException;
}
