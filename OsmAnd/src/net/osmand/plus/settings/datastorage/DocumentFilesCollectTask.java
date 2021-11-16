package net.osmand.plus.settings.datastorage;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

class DocumentFilesCollectTask extends AsyncTask<Void, Void, String> {

	private final DocumentFile folderFile;
	private final List<DocumentFile> documentFiles = new ArrayList<>();
	private final FilesCollectListener listener;
	private final long[] filesSize = new long[1];

	public DocumentFilesCollectTask(@NonNull OsmandApplication app, @NonNull Uri folderUri, @Nullable FilesCollectListener listener) {
		this.listener = listener;
		folderFile = DocumentFile.fromTreeUri(app, folderUri);
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onFileCopyStarted();
		}
	}

	@Override
	protected String doInBackground(Void... voids) {
		collectFiles(folderFile, documentFiles, filesSize);
		return null;
	}

	@Override
	protected void onPostExecute(String error) {
		if (listener != null) {
			listener.onFileCopyFinished(folderFile, documentFiles, filesSize[0]);
		}
	}

	private void collectFiles(DocumentFile documentFile, List<DocumentFile> documentFiles, long[] size) {
		if (isCancelled()) {
			return;
		}
		if (documentFile.isDirectory()) {
			DocumentFile[] files = documentFile.listFiles();
			for (DocumentFile file : files) {
				if (isCancelled()) {
					break;
				}
				collectFiles(file, documentFiles, size);
			}
		} else {
			size[0] += documentFile.length();
			documentFiles.add(documentFile);
		}
	}

	public interface FilesCollectListener {

		void onFileCopyStarted();

		void onFileCopyFinished(DocumentFile folder, List<DocumentFile> files, long size);

	}
}
