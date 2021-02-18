package net.osmand.plus.myplaces;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;

import java.io.File;
import java.util.Map;

public class SaveCurrentTrackTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final GPXFile gpx;

	private final SaveGpxListener saveGpxListener;

	public SaveCurrentTrackTask(@NonNull OsmandApplication app, @NonNull GPXFile gpx, @NonNull SaveGpxListener listener) {
		this.app = app;
		this.gpx = gpx;
		saveGpxListener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (saveGpxListener != null) {
			saveGpxListener.gpxSavingStarted();
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		Map<String, GPXUtilities.GPXFile> files = savingTrackHelper.collectRecordedData();
		File dir;
		boolean shouldClearPath = false;
		if (gpx.path.isEmpty()) {
			dir = app.getCacheDir();
			shouldClearPath = true;
		} else {
			dir = app.getAppCustomization().getTracksDir();
		}
		if (!dir.exists()) {
			dir.mkdir();
		}
		for (final String f : files.keySet()) {
			File fout = new File(dir, f + IndexConstants.GPX_FILE_EXT);
			Exception exception = GPXUtilities.writeGpxFile(fout, gpx);
			if (exception == null) {
				app.getSavingTrackHelper().setLastTimeFileSaved(fout.lastModified());
			}
		}
		return shouldClearPath;
	}

	@Override
	protected void onPostExecute(Boolean shouldClearPath) {
		if (gpx != null) {
			if (saveGpxListener != null) {
				saveGpxListener.gpxSavingFinished(null);
			}
			if (shouldClearPath) {
				gpx.path = "";
			}
		}
	}
}