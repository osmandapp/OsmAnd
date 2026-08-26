package net.osmand.plus.settings.mediastorage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class FileMediaTarget extends MediaTarget {

	private final MediaStorageHelper helper;
	private final File file;
	private final boolean existedBefore;

	private boolean createdByTarget;
	private boolean reservedForDirectWrite;

	FileMediaTarget(@NonNull MediaStorageHelper helper, @NonNull File file) {
		super(file.getName());
		this.helper = helper;
		this.file = file;
		this.existedBefore = file.exists();
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

	@NonNull
	@Override
	public OutputStream openOutputStream() throws IOException {
		File parent = file.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
			throw new IOException("Failed to create media target directory: " + parent);
		}
		if (reservedForDirectWrite || !file.createNewFile()) {
			throw new IOException("Media target already exists: " + file);
		}
		createdByTarget = true;
		return new FileOutputStream(file);
	}

	@Override
	public void finish(boolean success) throws IOException {
		if (success) {
			if (!file.exists()) {
				throw new IOException("Media target was not created: " + file);
			}
			helper.scanMediaFile(file);
		} else {
			delete();
		}
	}

	@Override
	public void delete() throws IOException {
		if ((createdByTarget || reservedForDirectWrite) && file.exists() && !Algorithms.removeAllFiles(file)) {
			throw new IOException("Failed to delete media target: " + file);
		}
		createdByTarget = false;
		reservedForDirectWrite = false;
	}

	@NonNull
	@Override
	public String getHref() {
		return helper.createMediaFileHref(file);
	}

	@Nullable
	@Override
	public File getFile() {
		if (createdByTarget || reservedForDirectWrite) {
			return file;
		}
		if (existedBefore) {
			return null;
		}
		File parent = file.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
			return null;
		}
		try {
			if (!file.createNewFile()) {
				return null;
			}
		} catch (IOException e) {
			return null;
		}
		reservedForDirectWrite = true;
		return file;
	}
}