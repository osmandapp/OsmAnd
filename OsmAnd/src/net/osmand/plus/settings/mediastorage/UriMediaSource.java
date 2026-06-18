package net.osmand.plus.settings.mediastorage;

import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class UriMediaSource extends MediaSource {

	private final OsmandApplication app;
	private final Uri uri;
	@Nullable
	private final Uri treeUri;

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