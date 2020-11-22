package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public abstract class SettingsItemReader<T extends SettingsItem> {

	private T item;

	public SettingsItemReader(@NonNull T item) {
		this.item = item;
	}

	public abstract void readFromStream(@NonNull InputStream inputStream, String entryName) throws IOException, IllegalArgumentException;
}
