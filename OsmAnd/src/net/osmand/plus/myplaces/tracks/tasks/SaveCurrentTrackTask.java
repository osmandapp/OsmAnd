package net.osmand.plus.myplaces.tracks.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.track.helpers.save.SaveGpxListener;
import net.osmand.shared.gpx.GpxFile;

import java.io.File;
import java.util.Map;

public class SaveCurrentTrackTask extends AsyncTask<Void, Void, SaveCurrentTrackTask.SaveResult> {

	private final OsmandApplication app;
	private final GpxFile gpx;

	private final SaveGpxListener saveGpxListener;

	public SaveCurrentTrackTask(@NonNull OsmandApplication app, @NonNull GpxFile gpx, @NonNull SaveGpxListener listener) {
		this.app = app;
		this.gpx = gpx;
		saveGpxListener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (saveGpxListener != null) {
			saveGpxListener.onSaveGpxStarted();
		}
	}

	@Override
	protected SaveResult doInBackground(Void... params) {
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		Map<String, GpxFile> files = savingTrackHelper.collectRecordedData();
		boolean shouldClearPath = gpx.getPath().isEmpty();
		File dir = shouldClearPath ? app.getCacheDir() : app.getAppCustomization().getTracksDir();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		Exception lastError = null;
		String lastSavedPath = null;
		for (String f : files.keySet()) {
			GpxFile gpxFile = files.get(f);
			if (gpxFile == null) {
				continue;
			}
			File fout = new File(dir, f + IndexConstants.GPX_FILE_EXT);
			Exception exception = SharedUtil.writeGpxFile(fout, gpxFile);
			if (exception != null) {
				lastError = exception;
			} else {
				lastSavedPath = fout.getAbsolutePath();
				gpxFile.setPath(lastSavedPath);
				app.getSavingTrackHelper().setLastTimeFileSaved(fout.lastModified());
				app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(gpxFile));
			}
		}
		return new SaveResult(lastError, lastSavedPath, shouldClearPath);
	}

	@Override
	protected void onPostExecute(@Nullable SaveResult result) {
		if (gpx == null || result == null) {
			return;
		}
		if (result.error == null && result.savedPath != null) {
			gpx.setPath(result.savedPath);
		}
		if (saveGpxListener != null) {
			saveGpxListener.onSaveGpxFinished(result.error);
		}
		if (result.error == null && result.shouldClearPath) {
			gpx.setPath("");
		}
	}

	static final class SaveResult {
		@Nullable
		final Exception error;
		@Nullable
		final String savedPath;
		final boolean shouldClearPath;

		SaveResult(@Nullable Exception error, @Nullable String savedPath, boolean shouldClearPath) {
			this.error = error;
			this.savedPath = savedPath;
			this.shouldClearPath = shouldClearPath;
		}
	}
}
