package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class SettingsItemReader<T extends SettingsItem> {

	private final T item;

	public SettingsItemReader(@NonNull T item) {
		this.item = item;
	}

	public T getItem() {
		return item;
	}

	public abstract void readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
	                                    @Nullable String entryName) throws IOException, IllegalArgumentException;
}
