package net.osmand.plus.mapcontextmenu.editors;

import android.support.v4.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class PointEditor {

	protected OsmandApplication app;
	protected MapActivity mapActivity;

	protected boolean isNew;

	private boolean portraitMode;
	private boolean largeDevice;

	public PointEditor(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		this.mapActivity = mapActivity;
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
		largeDevice = AndroidUiHelper.isXLargeDevice(mapActivity);
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public boolean isNew() {
		return isNew;
	}

	public boolean isLandscapeLayout() {
		return !portraitMode && !largeDevice;
	}

	public int getSlideInAnimation() {
		if (isLandscapeLayout()) {
			return R.anim.slide_in_left;
		} else {
			return R.anim.slide_in_bottom;
		}
	}

	public int getSlideOutAnimation() {
		if (isLandscapeLayout()) {
			return R.anim.slide_out_left;
		} else {
			return R.anim.slide_out_bottom;
		}
	}

	public abstract String getFragmentTag();

	public void hide() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(getFragmentTag());
		if (fragment != null)
			((PointEditorFragment)fragment).dismiss();
	}

	public void setCategory(String name) {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(getFragmentTag());
		if (fragment != null) {
			PointEditorFragment editorFragment = (PointEditorFragment) fragment;
			editorFragment.setCategory(name);
		}
	}
}
