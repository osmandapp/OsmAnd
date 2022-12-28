package net.osmand.plus.track.fragments.controller;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;

import java.io.File;

public class EditWptDescriptionController extends EditPointDescriptionController {

	public EditWptDescriptionController(@NonNull MapActivity activity) {
		super(activity);
	}

	@Override
	public void saveEditedDescriptionImpl(@NonNull String editedText) {
		WptPt wpt = (WptPt) getContextMenuObject();
		if (wpt == null) {
			return;
		}
		OsmandApplication app = activity.getMyApplication();
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
		if (selectedGpxFile != null && selectedGpxFile.getGpxFile() != null) {
			GPXFile gpx = selectedGpxFile.getGpxFile();
			if (gpx.showCurrentTrack) {
				SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
				savingTrackHelper.updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(), editedText,
						wpt.name, wpt.category, wpt.getColor(), wpt.getIconName(), wpt.getBackgroundType());
			} else {
				WptPt wptRes = wpt;
				wptRes.desc = editedText;
				gpx.updateWptPt(wpt, wptRes);
				saveGpx(gpx);
			}
			LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());
			updateContextMenu(latLon, new WptLocationPoint(wpt).getPointDescription(activity), wpt);
		}
	}

	private void saveGpx(@NonNull GPXFile gpxFile) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, null)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	@Override
	public String getTitle() {
		WptPt wpt = (WptPt) getContextMenuObject();
		return wpt != null ? wpt.name : super.getTitle();
	}
}