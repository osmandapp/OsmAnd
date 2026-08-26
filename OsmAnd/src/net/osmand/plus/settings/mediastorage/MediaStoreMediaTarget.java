package net.osmand.plus.settings.mediastorage;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.io.IOException;
import java.io.OutputStream;

class MediaStoreMediaTarget extends MediaTarget {

	private final MediaStorageHelper helper;
	private final OsmandApplication app;
	private final Uri collectionUri;
	private final String relativePath;
	private final String mimeType;
	@Nullable
	private Uri uri;

	MediaStoreMediaTarget(@NonNull MediaStorageHelper helper, @NonNull Uri collectionUri,
	                      @NonNull String relativePath, @NonNull String fileName, @NonNull String mimeType) {
		super(fileName);
		this.helper = helper;
		this.app = helper.getApp();
		this.collectionUri = collectionUri;
		this.relativePath = relativePath;
		this.mimeType = mimeType;
	}

	@Override
	public boolean exists() {
		return helper.mediaStoreFileExists(collectionUri, relativePath, getFileName());
	}

	@NonNull
	@Override
	public OutputStream openOutputStream() throws IOException {
		ContentValues values = new ContentValues();
		values.put(MediaStore.MediaColumns.DISPLAY_NAME, getFileName());
		values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
		values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
		values.put(MediaStore.MediaColumns.IS_PENDING, 1);
		uri = app.getContentResolver().insert(collectionUri, values);
		if (uri == null) {
			throw new IOException("Failed to create media store file: " + getFileName());
		}
		OutputStream outputStream = app.getContentResolver().openOutputStream(uri);
		if (outputStream == null) {
			throw new IOException("Failed to open media store output stream: " + uri);
		}
		return outputStream;
	}

	@Override
	public void finish(boolean success) throws IOException {
		if (uri == null) {
			return;
		}
		if (success) {
			ContentValues values = new ContentValues();
			values.put(MediaStore.MediaColumns.IS_PENDING, 0);
			try {
				if (app.getContentResolver().update(uri, values, null, null) <= 0) {
					throw new IOException("Failed to publish media store file: " + uri);
				}
			} catch (RuntimeException e) {
				throw new IOException("Failed to publish media store file: " + uri, e);
			}
		} else {
			delete();
		}
	}

	@Override
	public void delete() throws IOException {
		if (uri != null) {
			Uri targetUri = uri;
			try {
				if (app.getContentResolver().delete(targetUri, null, null) <= 0) {
					throw new IOException("Failed to delete media store file: " + targetUri);
				}
				uri = null;
			} catch (RuntimeException e) {
				throw new IOException("Failed to delete media store file: " + targetUri, e);
			}
		}
	}

	@NonNull
	@Override
	public String getHref() {
		if (uri == null) {
			throw new IllegalStateException("Media store target was not created: " + getFileName());
		}
		return uri.toString();
	}
}