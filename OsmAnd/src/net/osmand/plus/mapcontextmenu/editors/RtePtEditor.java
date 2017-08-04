package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.activities.MapActivity;

public class RtePtEditor extends GpxPointEditor {

	public static final String TAG = "RtePtEditorFragment";

	public RtePtEditor(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public void add(GPXFile gpxFile, LatLon latLon, String title) {
		if (latLon == null) {
			return;
		}
		isNew = true;

		this.gpxFile = gpxFile;
		GpxSelectionHelper.SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);
		wpt.name = title;
		RtePtEditorFragment.showInstance(mapActivity);
	}

	public void add(GPXFile gpxFile, LatLon latLon, String title, String categoryName, int categoryColor, boolean skipDialog) {
		if (latLon == null) {
			return;
		}
		isNew = true;

		this.gpxFile = gpxFile;
		GpxSelectionHelper.SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);

		wpt.name = title;

		if (categoryName != null && !categoryName.isEmpty()) {

			FavouritesDbHelper.FavoriteGroup category = mapActivity.getMyApplication()
					.getFavorites()
					.getGroup(categoryName);

			if (category == null) {

				mapActivity.getMyApplication()
						.getFavorites()
						.addEmptyCategory(categoryName, categoryColor);
			}

		} else categoryName = "";

		wpt.category = categoryName;

		RtePtEditorFragment.showInstance(mapActivity, skipDialog);
	}

	public void edit(WptPt wpt) {
		if (wpt == null) {
			return;
		}
		isNew = false;
		GpxSelectionHelper.SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFile(wpt);
		if (selectedGpxFile != null) {
			gpxSelected = true;
			gpxFile = selectedGpxFile.getGpxFile();
		}
		this.wpt = wpt;
		RtePtEditorFragment.showInstance(mapActivity);
	}
}
