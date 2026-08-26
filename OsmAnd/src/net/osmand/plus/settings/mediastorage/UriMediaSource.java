package net.osmand.plus.settings.mediastorage;

import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class UriMediaSource extends MediaSource {

	private static final Log LOG = PlatformUtil.getLog(UriMediaSource.class);

	private final OsmandApplication app;
	private final Uri uri;
	@Nullable
	private final Uri treeUri;
	private long lastModified = -1;

	UriMediaSource(@NonNull OsmandApplication app, @NonNull String href, @NonNull Uri uri,
	               @NonNull String fileName, long length, @Nullable String mimeType,
	               @NonNull MediaDirType dirType, @Nullable Uri treeUri) {
		super(href, fileName, length, mimeType, dirType);
		this.app = app;
		this.uri = uri;
		this.treeUri = treeUri;
		addHrefKey(uri.toString());
	}

	@NonNull
	@Override
	public InputStream openInputStream() throws IOException {
		InputStream inputStream = app.getContentResolver().openInputStream(uri);
		if (inputStream == null) {
			throw new IOException("Failed to open media input stream: " + uri);
		}
		return inputStream;
	}

	@Override
	public long getLastModified() {
		long lastModified = this.lastModified;
		if (lastModified < 0) {
			lastModified = queryLastModified();
			this.lastModified = lastModified;
		}
		return lastModified;
	}

	private long queryLastModified() {
		// SAF documents expose millis, MediaStore exposes seconds
		long millis = queryLong(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
		if (millis > 0) {
			return millis;
		}
		long seconds = queryLong(MediaStore.MediaColumns.DATE_MODIFIED);
		return seconds > 0 ? seconds * 1000L : 0;
	}

	private long queryLong(@NonNull String column) {
		try (Cursor cursor = app.getContentResolver().query(uri, new String[] {column}, null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int index = cursor.getColumnIndex(column);
				if (index >= 0 && !cursor.isNull(index)) {
					return cursor.getLong(index);
				}
			}
		} catch (Exception e) {
			LOG.warn(e);
		}
		return 0;
	}

	@Override
	public void delete() throws IOException {
		try {
			Uri treeUri = this.treeUri;
			if (treeUri != null) {
				deleteTreeDocument(treeUri);
			} else if (app.getContentResolver().delete(uri, null, null) <= 0) {
				// MediaStore may require user consent for media no longer owned by this app.
				throw new IOException("Failed to delete media uri: " + uri);
			}
		} catch (FileNotFoundException e) {
			// Already deleted by a previous attempt or externally by the user.
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Failed to delete media uri: " + uri, e);
		}
	}

	private void deleteTreeDocument(@NonNull Uri treeUri) throws IOException {
		Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getDocumentId(uri));
		if (!DocumentsContract.deleteDocument(app.getContentResolver(), documentUri)) {
			throw new IOException("Failed to delete media tree document: " + documentUri);
		}
	}

	@NonNull
	@Override
	public String getId() {
		return uri.toString();
	}
}