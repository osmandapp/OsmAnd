package net.osmand.plus.settings.mediastorage;

import android.net.Uri;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class FileMediaSource extends MediaSource {

	private final File file;
	private final MediaStorageHelper helper;

	FileMediaSource(@NonNull MediaStorageHelper helper, @NonNull String href, @NonNull File file) {
		super(href, file.getName(), file.length(), MediaStorageUtils.getMimeType(null, file.getName(), getDirType(file)), getDirType(file));
		this.helper = helper;
		this.file = file;
		addHrefKey(helper.createMediaFileHref(file));
		addHrefKey(Uri.fromFile(file).toString());
		addHrefKey(file.getAbsolutePath());
	}

	@NonNull
	@Override
	public InputStream openInputStream() throws IOException {
		return new FileInputStream(file);
	}

	@Override
	public void delete() throws IOException {
		if (!file.exists()) {
			return;
		}
		if (!file.delete()) {
			throw new IOException("Failed to delete media file: " + file);
		}
		helper.scanMediaFile(file);
	}

	@NonNull
	@Override
	public String getId() {
		return file.getAbsolutePath();
	}

	@NonNull
	private static MediaDirType getDirType(@NonNull File file) {
		return MediaDirType.fromExtension(Algorithms.getFileNameExtension(file.getName()));
	}
}