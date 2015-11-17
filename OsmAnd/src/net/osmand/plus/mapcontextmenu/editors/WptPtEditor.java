package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.activities.MapActivity;

public class WptPtEditor extends PointEditor {

	private GPXFile gpxFile;
	private WptPt wpt;
	private boolean gpxSelected;

	public static final String TAG = "WptPtEditorFragment";

	public WptPtEditor(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public boolean isGpxSelected() {
		return gpxSelected;
	}

	public WptPt getWptPt() {
		return wpt;
	}

	public void add(GPXFile gpxFile, LatLon latLon, String title) {
		if (latLon == null) {
			return;
		}
		isNew = true;

		this.gpxFile = gpxFile;
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);
		wpt.name = title;
		WptPtEditorFragment.showInstance(mapActivity);
	}

	public void edit(WptPt wpt) {
		if (wpt == null) {
			return;
		}
		isNew = false;
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFile(wpt);
		if (selectedGpxFile != null) {
			gpxSelected = true;
			gpxFile = selectedGpxFile.getGpxFile();
		}
		this.wpt = wpt;
		WptPtEditorFragment.showInstance(mapActivity);
	}
}
