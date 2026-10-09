package net.osmand.plus.track.fragments.controller;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;

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
			GpxFile gpx = selectedGpxFile.getGpxFile();
			if (gpx.isShowCurrentTrack()) {
				SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
				savingTrackHelper.updatePointData(wpt, wpt.getLatitude(), wpt.getLongitude(), editedText,
						wpt.getName(), wpt.getCategory(), wpt.getColor(), wpt.getIconName(), wpt.getBackgroundType());
			} else {
				WptPt wptRes = wpt;
				wptRes.setDesc(editedText);
				gpx.updateWptPt(wpt, wptRes, true);
				SaveGpxHelper.saveGpx(gpx);
			}
			LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());
			updateContextMenu(latLon, new WptLocationPoint(wpt).getPointDescription(activity), wpt);
		}
	}

	@NonNull
	@Override
	public String getTitle() {
		WptPt wpt = (WptPt) getContextMenuObject();
		return wpt != null ? wpt.getName() : super.getTitle();
	}
}