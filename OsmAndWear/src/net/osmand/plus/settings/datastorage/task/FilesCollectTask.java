package net.osmand.plus.settings.datastorage.task;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesCollectTask extends AsyncTask<Void, Void, String> {

	private final OsmandApplication app;
	private final File folderFile;
	private final List<File> files = new ArrayList<>();
	private final FilesCollectListener listener;
	private final long[] filesSize = new long[1];
	private final long[] estimatedSize = new long[1];

	public FilesCollectTask(@NonNull OsmandApplication app, @NonNull File file, @Nullable FilesCollectListener listener) {
		this.app = app;
		this.listener = listener;
		folderFile = file;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onFilesCollectingStarted();
		}
	}

	@Override
	protected String doInBackground(Void... voids) {
		collectFiles(folderFile, files, filesSize, estimatedSize);

		if (Algorithms.isEmpty(files)) {
			return app.getString(R.string.storage_migration_wrong_folder_warning);
		}
		return null;
	}

	@Override
	protected void onPostExecute(String error) {
		if (listener != null) {
			Pair<Long, Long> pair = new Pair<>(filesSize[0], estimatedSize[0]);
			listener.onFilesCollectingFinished(error, folderFile, files, pair);
		}
	}

	private void collectFiles(@NonNull File documentFile,
	                          @NonNull List<File> documentFiles,
	                          long[] size,
	                          long[] estimatedSize) {
		if (isCancelled()) {
			return;
		}
		if (documentFile.isDirectory()) {
			File[] files = documentFile.listFiles();
			for (File file : files) {
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

	public interface FilesCollectListener {
		default void onFilesCollectingStarted() { }
		default void onFilesCollectingFinished(@Nullable String error, @NonNull File folder,
		                                       @NonNull List<File> files, @NonNull Pair<Long, Long> filesSize) { }
	}
}
