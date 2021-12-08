package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.plus.myplaces.FavouritesDbHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class WptPtEditor extends PointEditor {

	private OnWaypointTemplateAddedListener onWaypointTemplateAddedListener;
	private OnDismissListener onDismissListener;

	private GPXFile gpxFile;
	private WptPt wpt;
	@ColorInt
	private int categoryColor;

	private boolean gpxSelected;
	private boolean newGpxPointProcessing;
	private boolean processingWaypointTemplate;

	public static final String TAG = "WptPtEditorFragment";

	public WptPtEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	public void setNewGpxPointProcessing(boolean newGpxPointProcessing) {
		this.newGpxPointProcessing = newGpxPointProcessing;
	}

	public boolean isNewGpxPointProcessing() {
		return newGpxPointProcessing;
	}

	@Override
	public boolean isProcessingTemplate() {
		return processingWaypointTemplate;
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

	public void setOnWaypointTemplateAddedListener(OnWaypointTemplateAddedListener listener) {
		onWaypointTemplateAddedListener = listener;
	}

	public OnWaypointTemplateAddedListener getOnWaypointTemplateAddedListener() {
		return onWaypointTemplateAddedListener;
	}

	@Nullable
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	@NonNull
	public Map<String, Integer> getColoredWaypointCategories() {
		if (gpxFile != null) {
			return gpxFile.getWaypointCategoriesWithColors(false);
		}
		if (processingWaypointTemplate && !Algorithms.isEmpty(wpt.category) && categoryColor != 0) {
			Map<String, Integer> predefinedCategory = new HashMap<>();
			predefinedCategory.put(wpt.category, categoryColor);
			return predefinedCategory;
		}
		return new HashMap<>();
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
		add(gpxFile, latLon, title, null);
	}

	public void add(GPXFile gpxFile, LatLon latLon, String title, @Nullable String preselectedIconName) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}
		isNew = true;
		processingWaypointTemplate = false;
		categoryColor = 0;

		this.gpxFile = gpxFile;
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);
		wpt.name = title;
		if (!Algorithms.isEmpty(preselectedIconName)) {
			wpt.setIconName(preselectedIconName);
		}

		showEditorFragment();
	}

	public void add(GPXFile gpxFile, LatLon latLon, String title, String address, String description,
	                int color, String backgroundType, String categoryName, int categoryColor, boolean skipDialog) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}
		isNew = true;
		processingWaypointTemplate = false;
		this.categoryColor = 0;

		this.gpxFile = gpxFile;
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		WptPt wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);

		wpt.name = title;
		wpt.setAddress(address);
		wpt.desc = description;
		wpt.setColor(color);
		wpt.setBackgroundType(backgroundType);

		if (categoryName != null && !categoryName.isEmpty()) {
			FavouritesDbHelper.FavoriteGroup category = mapActivity.getMyApplication()
					.getFavorites()
					.getGroup(categoryName);

			if (category == null) {
				mapActivity.getMyApplication()
						.getFavorites()
						.addEmptyCategory(categoryName, categoryColor);
			}

		} else {
			categoryName = "";
		}

		wpt.category = categoryName;
		this.wpt = wpt;

		showEditorFragment(skipDialog);
	}

	public void edit(@NonNull WptPt wpt) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		isNew = false;
		processingWaypointTemplate = false;
		categoryColor = 0;
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedGPXFile(wpt);
		if (selectedGpxFile != null) {
			gpxSelected = true;
			gpxFile = selectedGpxFile.getGpxFile();
		}
		this.wpt = wpt;
		showEditorFragment();
	}

	public void addWaypointTemplate(@Nullable WptPt from, @NonNull GPXFile gpxFile) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		this.isNew = true;
		this.processingWaypointTemplate = true;
		this.categoryColor = 0;
		this.gpxSelected = mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path) != null;
		this.gpxFile = gpxFile;
		this.wpt = from != null ? from : new WptPt();
		showEditorFragment();
	}

	public void addWaypointTemplate(@Nullable WptPt from, @ColorInt int categoryColor) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		this.isNew = true;
		this.processingWaypointTemplate = true;
		this.categoryColor = categoryColor;
		this.gpxSelected = false;
		this.gpxFile = null;
		this.wpt = from != null ? from : new WptPt();
		showEditorFragment();
	}

	public void showEditorFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			WptPtEditorFragmentNew.showInstance(mapActivity);
		}
	}

	public void showEditorFragment(boolean skipDialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			WptPtEditorFragmentNew.showInstance(mapActivity, skipDialog);
		}
	}

	public interface OnWaypointTemplateAddedListener {

		void onAddWaypointTemplate(@NonNull WptPt waypoint, @ColorInt int categoryColor);
	}
}