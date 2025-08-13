package net.osmand.plus.track.helpers.save;

import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.util.Algorithms;

import java.io.File;

public class SaveDirectionsAsyncTask extends AsyncTask<File, Void, GpxFile> {

	private final OsmandApplication app;
	boolean showOnMap;

	public SaveDirectionsAsyncTask(@NonNull OsmandApplication app, boolean showOnMap) {
		this.app = app;
		this.showOnMap = showOnMap;
	}

	@Override
	protected GpxFile doInBackground(File... params) {
		if (params.length > 0) {
			File file = params[0];
			String fileName = Algorithms.getFileNameWithoutExtension(file);
			GpxFile gpx = app.getRoutingHelper().generateGPXFileWithRoute(fileName);
			Exception exception = SharedUtil.writeGpxFile(file, gpx);
			gpx.setError(exception != null ? SharedUtil.kException(exception) : null);
			app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(gpx));
			return gpx;
		}
		return null;
	}

	@Override
	protected void onPostExecute(GpxFile gpxFile) {
		if (gpxFile.getError() == null) {
			GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
			if (showOnMap) {
				params.showOnMap().selectedByUser().addToMarkers().addToHistory();
			} else {
				params.hideFromMap();
			}
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);
			String result = app.getString(R.string.route_successfully_saved_at, gpxFile.getTracks().get(0).getName());
			app.showToastMessage(result);
		} else {
			String errorMessage = SharedUtil.jException(gpxFile.getError()).getMessage();
			if (errorMessage == null) {
				errorMessage = app.getString(R.string.error_occurred_saving_gpx);
			}
			app.showToastMessage(errorMessage);
		}
	}
}
