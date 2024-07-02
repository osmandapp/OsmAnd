package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class WptPtEditor extends PointEditor {

	public static final String TAG = WptPtEditor.class.getSimpleName();

	private OnTemplateAddedListener onTemplateAddedListener;
	private OnDismissListener onDismissListener;

	private GPXFile gpxFile;
	private WptPt wpt;
	@ColorInt
	private int categoryColor;

	private boolean gpxSelected;

	private enum ProcessedObject {
		ORDINARY,
		NEW_GPX_POINT,
		WAYPOINT_TEMPLATE
	}

	@NonNull
	private ProcessedObject processedObject = ProcessedObject.ORDINARY;

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

	@NonNull
	public Map<String, PointsGroup> getPointsGroups() {
		if (gpxFile != null) {
			return gpxFile.getPointsGroups();
		}
		if (isProcessingTemplate() && !Algorithms.isEmpty(wpt.category) && categoryColor != 0) {
			PointsGroup pointsGroup = new PointsGroup(wpt.category, wpt.getIconNameOrDefault(), wpt.getBackgroundType(), categoryColor);

			Map<String, PointsGroup> predefinedCategory = new HashMap<>();
			predefinedCategory.put(wpt.category, pointsGroup);
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

	@Nullable
	@Override
	public String getPreselectedIconName() {
		return isNew && wpt != null ? wpt.getIconName() : null;
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public void add(GPXFile gpxFile, LatLon latLon, String title, @Nullable Amenity amenity) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}
		isNew = true;
		categoryColor = 0;

		this.gpxFile = gpxFile;
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);
		wpt.name = title;

		if (amenity != null) {
			int preselectedIconId = RenderingIcons.getPreselectedIconId(amenity);
			String preselectedIconName = RenderingIcons.getBigIconName(preselectedIconId);
			if (!Algorithms.isEmpty(preselectedIconName)) {
				wpt.setIconName(preselectedIconName);
			}
			wpt.setAmenityOriginName(amenity.toStringEn());
			wpt.getExtensionsToWrite().putAll(amenity.getAmenityExtensions(app.getPoiTypes(), true));
		}

		showEditorFragment();
	}

	public void add(@NonNull GPXFile gpxFile, @NonNull WptPt wpt, String categoryName, int categoryColor, boolean skipDialog) {
		if (mapActivity == null) {
			return;
		}
		isNew = true;
		this.categoryColor = 0;

		this.gpxFile = gpxFile;
		SelectedGpxFile selectedGpxFile =
				mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
		gpxSelected = selectedGpxFile != null;

		if (!Algorithms.isEmpty(categoryName)) {
			FavoriteGroup category = mapActivity.getMyApplication()
					.getFavoritesHelper()
					.getGroup(categoryName);

			if (category == null) {
				mapActivity.getMyApplication()
						.getFavoritesHelper()
						.addFavoriteGroup(categoryName, categoryColor);
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
		this.processedObject = ProcessedObject.WAYPOINT_TEMPLATE;
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
		this.processedObject = ProcessedObject.WAYPOINT_TEMPLATE;
		this.categoryColor = categoryColor;
		this.gpxSelected = false;
		this.gpxFile = null;
		this.wpt = from != null ? from : new WptPt();
		showEditorFragment();
	}

	public void showEditorFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			WptPtEditorFragment.showInstance(mapActivity);
		}
	}

	public void showEditorFragment(boolean skipDialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			WptPtEditorFragment.showInstance(mapActivity, skipDialog);
		}
	}

	public interface OnTemplateAddedListener {

		void onAddWaypointTemplate(@NonNull WptPt waypoint, @ColorInt int categoryColor);
	}
}