package net.osmand.plus.mapcontextmenu.editors;

import android.os.AsyncTask;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.PointsCategory;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.plus.myplaces.FavouritesDbHelper;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WptPtEditor extends PointEditor {

	private OnTemplateAddedListener onTemplateAddedListener;
	private OnDismissListener onDismissListener;

	private GPXFile gpxFile;
	private WptPt wpt;
	private PointsCategory predefinedCategory;

	private boolean gpxSelected;

	private enum ProcessedObject {
		ORDINARY,
		NEW_GPX_POINT,
		WAYPOINT_TEMPLATE
	}

	@NonNull
	private ProcessedObject processedObject = ProcessedObject.ORDINARY;

	public static final String TAG = "WptPtEditorFragment";

	public WptPtEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	public void setProcessingOrdinaryPoint() {
		processedObject = ProcessedObject.ORDINARY;
	}

	public void setNewGpxPointProcessing() {
		processedObject = ProcessedObject.NEW_GPX_POINT;
	}

	public boolean isNewGpxPointProcessing() {
		return processedObject == ProcessedObject.NEW_GPX_POINT;
	}

	@Override
	public boolean isProcessingTemplate() {
		return processedObject == ProcessedObject.WAYPOINT_TEMPLATE;
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

	public void setOnWaypointTemplateAddedListener(OnTemplateAddedListener listener) {
		onTemplateAddedListener = listener;
	}

	public OnTemplateAddedListener getOnWaypointTemplateAddedListener() {
		return onTemplateAddedListener;
	}

	@Nullable
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	@NonNull Map<String, PointsCategory> getWaypointCategories() {
		if (gpxFile != null) {
			Map<String, PointsCategory> categories = new LinkedHashMap<>(gpxFile.getCategories());
			categories.remove("");
			return categories;
		}
		if (isProcessingTemplate() && predefinedCategory != null) {
			Map<String, PointsCategory> categories = new LinkedHashMap<>();
			categories.put(predefinedCategory.getName(), predefinedCategory);
			return categories;
		}
		return new HashMap<>();
	}

	public boolean isGpxSelected() {
		return gpxSelected;
	}

	public WptPt getWptPt() {
		return wpt;
	}

	@Nullable
	@Override
	public String getPreselectedIconName() {
		return isNew && wpt != null ? wpt.getIconName() : null;
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
		predefinedCategory = null;

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

	public void add(@NonNull GPXFile gpxFile, @NonNull WptPt wpt, @Nullable PointsCategory category, boolean skipDialog) {
		if (mapActivity == null) {
			return;
		}
		isNew = true;
		predefinedCategory = null;

		this.gpxFile = gpxFile;
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		boolean categoryMissing = category != null && !gpxFile.getCategories().containsKey(category.getName());
		if (categoryMissing) {
			setCategory(category, true);
		}

		this.wpt = wpt;

		showEditorFragment(skipDialog);
	}

	public void edit(@NonNull WptPt wpt) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		isNew = false;
		predefinedCategory = null;

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
		this.processedObject = ProcessedObject.WAYPOINT_TEMPLATE;
		this.predefinedCategory = null;
		this.gpxSelected = mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path) != null;
		this.gpxFile = gpxFile;
		this.wpt = from != null ? from : new WptPt();
		showEditorFragment();
	}

	public void addWaypointTemplate(@Nullable WptPt from, @Nullable PointsCategory category) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		this.isNew = true;
		this.processedObject = ProcessedObject.WAYPOINT_TEMPLATE;
		this.predefinedCategory = category;
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

	@Override
	public void setCategory(@NonNull PointsCategory category, boolean isNew) {
		if (gpxFile != null && isNew) {
			saveNewCategory(gpxFile, category);
		}
		super.setCategory(category, isNew);
	}

	private void saveNewCategory(@NonNull GPXFile gpxFile, @NonNull PointsCategory category) {
		gpxFile.addCategory(category);
		if (gpxFile.showCurrentTrack) {
			app.getSavingTrackHelper().insertCategoryData(category);
		} else {
			new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, null)
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	public interface OnTemplateAddedListener {

		void onAddWaypointTemplate(@NonNull WptPt waypoint, @Nullable PointsCategory category);
	}
}