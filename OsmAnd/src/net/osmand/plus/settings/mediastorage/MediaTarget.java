package net.osmand.plus.settings.mediastorage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public abstract class MediaTarget {

	private final String fileName;

	MediaTarget(@NonNull String fileName) {
		this.fileName = fileName;
	}

	@NonNull
	public String getFileName() {
		return fileName;
	}

	public abstract boolean exists();

	@NonNull
	public abstract OutputStream openOutputStream() throws IOException;

	public abstract void finish(boolean success) throws IOException;

	public abstract void delete() throws IOException;

	@NonNull
	public abstract String getHref();

	@Nullable
	public File getFile() {
		return null;
	}
}