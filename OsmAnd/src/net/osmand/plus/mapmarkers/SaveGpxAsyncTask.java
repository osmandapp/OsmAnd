package net.osmand.plus.mapmarkers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.MAP_MARKERS_INDEX_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.util.Algorithms;

import java.io.File;

class SaveGpxAsyncTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;

	private final GpxFile gpx;
	private final String fileName;
	private final boolean gpxSelected;

	SaveGpxAsyncTask(@NonNull OsmandApplication app, @NonNull GpxFile gpx,
			@Nullable String fileName,
			boolean gpxSelected) {
		this.app = app;
		this.gpx = gpx;
		this.fileName = fileName;
		this.gpxSelected = gpxSelected;
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (Algorithms.isEmpty(gpx.getPath())) {
			if (!Algorithms.isEmpty(fileName)) {
				String dirName = GPX_INDEX_DIR + MAP_MARKERS_INDEX_DIR;
				File dir = app.getAppPath(dirName);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				String uniqueFileName = FileUtils.createUniqueFileName(app, fileName, dirName, GPX_FILE_EXT);
				File fout = new File(dir, uniqueFileName + GPX_FILE_EXT);
				SharedUtil.writeGpxFile(fout, gpx);
			}
		} else {
			SharedUtil.writeGpxFile(new File(gpx.getPath()), gpx);
		}
		app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(gpx));
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (!gpxSelected) {
			GpxSelectionParams params = GpxSelectionParams.newInstance()
					.showOnMap().syncGroup().selectedByUser().addToHistory().saveSelection();
			app.getSelectedGpxHelper().selectGpxFile(gpx, params);
		}
	}
}
