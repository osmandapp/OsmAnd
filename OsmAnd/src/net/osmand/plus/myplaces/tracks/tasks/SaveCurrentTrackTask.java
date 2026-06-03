package net.osmand.plus.myplaces.tracks.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.track.helpers.save.SaveGpxListener;
import net.osmand.shared.gpx.GpxFile;

import java.io.File;
import java.util.Map;

public class SaveCurrentTrackTask extends AsyncTask<Void, Void, Exception> {

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
	protected Exception doInBackground(Void... params) {
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		Map<String, GpxFile> files = savingTrackHelper.collectRecordedData();
		File dir;
		if (gpx.getPath().isEmpty()) {
			dir = app.getCacheDir();
		} else {
			dir = app.getAppCustomization().getTracksDir();
		}
		if (!dir.exists()) {
			dir.mkdirs();
		}
		Exception lastError = null;
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
				app.getSavingTrackHelper().setLastTimeFileSaved(fout.lastModified());
				app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(gpxFile));
			}
		}
		return lastError;
	}

	@Override
	protected void onPostExecute(Exception error) {
		if (gpx != null) {
			if (saveGpxListener != null) {
				saveGpxListener.onSaveGpxFinished(error);
			}
			if (error == null && gpx.getPath().isEmpty()) {
				gpx.setPath("");
			}
		}
	}
}