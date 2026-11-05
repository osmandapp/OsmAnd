package net.osmand.plus.settings.datastorage.task;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class DocumentFilesCollectTask extends AsyncTask<Void, Void, String> {

	private final OsmandApplication app;
	private final DocumentFile folderFile;
	private final List<DocumentFile> documentFiles = new ArrayList<>();
	private final DocumentFilesCollectListener listener;
	private final long[] filesSize = new long[1];
	private final long[] estimatedSize = new long[1];

	public DocumentFilesCollectTask(@NonNull OsmandApplication app, @NonNull Uri folderUri, @Nullable DocumentFilesCollectListener listener) {
		this.app = app;
		this.listener = listener;
		folderFile = DocumentFile.fromTreeUri(app, folderUri);
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onFilesCollectingStarted();
		}
	}

	@Override
	protected String doInBackground(Void... voids) {
		String folderName = IndexConstants.APP_DIR.replace("/", "");
		if (folderName.equalsIgnoreCase(folderFile.getName())) {
			collectFiles(folderFile, documentFiles, filesSize, estimatedSize);
		}
		if (Algorithms.isEmpty(documentFiles)) {
			return app.getString(R.string.storage_migration_wrong_folder_warning);
		}
		return null;
	}

	@Override
	protected void onPostExecute(String error) {
		if (listener != null) {
			Pair<Long, Long> pair = new Pair<>(filesSize[0], estimatedSize[0]);
			listener.onFilesCollectingFinished(error, folderFile, documentFiles, pair);
		}
	}

	private void collectFiles(@NonNull DocumentFile documentFile,
	                          @NonNull List<DocumentFile> documentFiles,
	                          long[] size,
	                          long[] estimatedSize) {
		if (isCancelled()) {
			return;
		}
		if (documentFile.isDirectory()) {
			DocumentFile[] files = documentFile.listFiles();
			for (DocumentFile file : files) {
				if (isCancelled()) {
					break;
				}
				collectFiles(file, documentFiles, size, estimatedSize);
			}
		} else {
			long length = documentFile.length();
			size[0] += length;
			estimatedSize[0] += length + FileUtils.APPROXIMATE_FILE_SIZE_BYTES;
			documentFiles.add(documentFile);
		}
	}

	public interface DocumentFilesCollectListener {

		void onFilesCollectingStarted();

		void onFilesCollectingFinished(@Nullable String error, @NonNull DocumentFile folder, @NonNull List<DocumentFile> files, @NonNull Pair<Long, Long> filesSize);
	}
}
