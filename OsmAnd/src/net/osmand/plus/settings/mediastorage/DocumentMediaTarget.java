package net.osmand.plus.settings.mediastorage;

import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.OutputStream;

class DocumentMediaTarget extends MediaTarget {

	private final OsmandApplication app;
	private final DocumentFile dir;
	private final String mimeType;
	@Nullable
	private DocumentFile file;

	DocumentMediaTarget(@NonNull OsmandApplication app, @NonNull DocumentFile dir,
	                    @NonNull String fileName, @NonNull String mimeType) {
		super(fileName);
		this.app = app;
		this.dir = dir;
		this.mimeType = mimeType;
	}

	@Override
	public boolean exists() {
		DocumentFile existing = dir.findFile(getFileName());
		return existing != null && existing.isFile();
	}

	@NonNull
	@Override
	public OutputStream openOutputStream() throws IOException {
		file = dir.createFile(getCreationMimeType(), getFileName());
		if (file == null) {
			throw new IOException("Failed to create document file: " + getFileName());
		}
		OutputStream outputStream = app.getContentResolver().openOutputStream(file.getUri());
		if (outputStream == null) {
			throw new IOException("Failed to open document output stream: " + file.getUri());
		}
		return outputStream;
	}

	/**
	 * Returns a mime type whose canonical extension matches the target file's
	 * extension, so the Storage Access Framework keeps the requested file name
	 * instead of appending a second extension. For example creating
	 * {@code name.3gp} with the semantic mime {@code audio/3gpp} would otherwise
	 * produce {@code name.3gp.3ga} (3ga is the canonical extension of audio/3gpp),
	 * which breaks the generated media-file-name format and migration. Falls back
	 * to the semantic mime type when the extension is unknown.
	 */
	@NonNull
	private String getCreationMimeType() {
		String extension = Algorithms.getFileNameExtension(getFileName());
		if (!Algorithms.isEmpty(extension)) {
			String byExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (!Algorithms.isEmpty(byExtension)) {
				return byExtension;
			}
		}
		return mimeType;
	}

	@Override
	public void finish(boolean success) throws IOException {
		if (success) {
			if (file == null) {
				throw new IOException("Document media target was not created: " + getFileName());
			}
		} else {
			delete();
		}
	}

	@Override
	public void delete() throws IOException {
		if (file != null) {
			DocumentFile targetFile = file;
			try {
				if (!targetFile.delete()) {
					throw new IOException("Failed to delete document media target: " + targetFile.getUri());
				}
				file = null;
			} catch (RuntimeException e) {
				throw new IOException("Failed to delete document media target: " + targetFile.getUri(), e);
			}
		}
	}

	@NonNull
	@Override
	public String getHref() {
		if (file == null) {
			throw new IllegalStateException("Document media target was not created: " + getFileName());
		}
		return file.getUri().toString();
	}
}