package net.osmand.plus.myplaces.tracks.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.track.helpers.save.SaveGpxListener;
import net.osmand.shared.gpx.GpxFile;

import java.io.File;
import java.util.Map;

public class SaveCurrentTrackTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final GpxFile gpx;

	private final SaveGpxListener saveGpxListener;

	private File savedFile;

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
	protected Boolean doInBackground(Void... params) {
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		Map<String, GpxFile> files = savingTrackHelper.collectRecordedData();
		File dir;
		boolean shouldClearPath = false;
		if (gpx.getPath().isEmpty()) {
			// Not the cache dir: it is not exposed by the file provider, so the saved track
			// could not be shared from there.
			dir = FileUtils.getTempDir(app);
			shouldClearPath = true;
		} else {
			dir = app.getAppCustomization().getTracksDir();
		}
		if (!dir.exists()) {
			dir.mkdir();
		}
		for (String f : files.keySet()) {
			File fout = new File(dir, f + IndexConstants.GPX_FILE_EXT);
			Exception exception = SharedUtil.writeGpxFile(fout, gpx);
			if (exception == null) {
				savedFile = fout;
				app.getSavingTrackHelper().setLastTimeFileSaved(fout.lastModified());
				app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(gpx));
			}
		}
		return shouldClearPath;
	}

	@Override
	protected void onPostExecute(Boolean shouldClearPath) {
		if (gpx != null) {
			if (shouldClearPath && savedFile != null) {
				// Let the listener reach the file that was just written.
				gpx.setPath(savedFile.getAbsolutePath());
			}
			if (saveGpxListener != null) {
				saveGpxListener.onSaveGpxFinished(null);
			}
			if (shouldClearPath) {
				gpx.setPath("");
			}
		}
	}
}