package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.activities.MapActivity;

public class WptPtEditor extends PointEditor {

	private OnDismissListener onDismissListener;
	private GPXFile gpxFile;
	private WptPt wpt;
	private boolean gpxSelected;
	private boolean newGpxPointProcessing;

	public static final String TAG = "WptPtEditorFragment";

	public WptPtEditor(MapActivity mapActivity) {
		super(mapActivity);
	}

	public void setNewGpxPointProcessing(boolean newGpxPointProcessing) {
		this.newGpxPointProcessing = newGpxPointProcessing;
	}

	public boolean isNewGpxPointProcessing() {
		return newGpxPointProcessing;
	}

	public interface OnDismissListener {
		void onDismiss();
	}

	public void setOnDismissListener(OnDismissListener listener) {
		onDismissListener = listener;
	}

	public OnDismissListener getOnDismissListener() {
		return onDismissListener;
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
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);
		wpt.name = title;
		showEditorFragment();
	}

	public void add(GPXFile gpxFile, LatLon latLon, String title, String categoryName, int categoryColor, boolean skipDialog) {
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

		showEditorFragment(skipDialog);
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
		showEditorFragment();
	}

	public void showEditorFragment() {
		WptPtEditorFragment.showInstance(mapActivity);
	}

	public void showEditorFragment(boolean skipDialog) {
		WptPtEditorFragment.showInstance(mapActivity, skipDialog);
	}
}
