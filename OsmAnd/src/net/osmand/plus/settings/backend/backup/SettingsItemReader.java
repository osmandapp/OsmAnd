package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class SettingsItemReader<T extends SettingsItem> {

	private T item;

	File destination;

	public SettingsItemReader(@NonNull T item) {
		this.item = item;
	}

	public void setDestination(File destination) {
		this.destination = destination;
	}

	public File getDestination() {
		return destination;
	}

	public abstract void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException;
}
