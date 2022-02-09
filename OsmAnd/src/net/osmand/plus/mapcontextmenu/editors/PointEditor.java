package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.GPXUtilities.PointsCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class PointEditor {

	protected OsmandApplication app;
	@Nullable
	protected MapActivity mapActivity;

	protected boolean isNew;

	public PointEditor(@NonNull MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		this.mapActivity = mapActivity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public boolean isNew() {
		return isNew;
	}

	@Nullable
	public String getPreselectedIconName() {
		return null;
	}

	public abstract boolean isProcessingTemplate();

	public abstract String getFragmentTag();

	public void setCategory(@NonNull PointsCategory category, boolean isNew) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(getFragmentTag());
			if (fragment instanceof PointEditorFragmentNew) {
				PointEditorFragmentNew editorFragment = (PointEditorFragmentNew) fragment;
				editorFragment.setCategory(category, isNew);
			}
		}
	}
}
