package net.osmand.plus.myplaces.tracks.tasks;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.FileUtils;

import java.io.File;

public class DeleteGpxFilesTask extends AsyncTask<File, File, String> {

	private final OsmandApplication app;
	@Nullable
	private final GpxFilesDeletionListener listener;

	public DeleteGpxFilesTask(@NonNull OsmandApplication app, @Nullable GpxFilesDeletionListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected String doInBackground(File... params) {
		int count = 0;
		int total = 0;
		for (File file : params) {
			if (!isCancelled() && (file.exists())) {
				total++;
				boolean successful = FileUtils.removeGpxFile(app, file);
				if (successful) {
					count++;
					publishProgress(file);
				}
			}
		}
		return app.getString(R.string.local_index_items_deleted, count, total);
	}

	@Override
	protected void onProgressUpdate(File... values) {
		if (listener != null) {
			listener.onGpxFilesDeleted(values);
		}
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onGpxFilesDeletionStarted();
		}
	}

	@Override
	protected void onPostExecute(String result) {
		app.showToastMessage(result);

		if (listener != null) {
			listener.onGpxFilesDeletionFinished();
		}
	}

	public interface GpxFilesDeletionListener {
		default void onGpxFilesDeletionStarted() {

		}

		default void onGpxFilesDeleted(File... values) {
		}

		default void onGpxFilesDeletionFinished() {

		}
	}
}
