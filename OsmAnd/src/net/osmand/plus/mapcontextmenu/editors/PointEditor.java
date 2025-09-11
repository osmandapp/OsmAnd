package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public abstract class PointEditor {

	protected OsmandApplication app;
	@Nullable
	protected MapActivity mapActivity;

	protected boolean isNew;

	public PointEditor(@NonNull MapActivity mapActivity) {
		this.app = mapActivity.getApp();
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

	public void setPointsGroup(@NonNull PointsGroup pointsGroup) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(getFragmentTag());
			if (fragment instanceof PointEditorFragment) {
				PointEditorFragment editorFragment = (PointEditorFragment) fragment;
				editorFragment.setPointsGroup(pointsGroup);
			}
		}
	}
}
