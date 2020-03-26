package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class PointEditor {

	protected OsmandApplication app;
	@Nullable
	protected MapActivity mapActivity;

	protected boolean isNew;

	private boolean portraitMode;
	private boolean nightMode;

	public PointEditor(@NonNull MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		this.mapActivity = mapActivity;
		updateLandscapePortrait(mapActivity);
		updateNightMode();
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

	public boolean isLandscapeLayout() {
		return !portraitMode;
	}

	public boolean isLight() {
		return !nightMode;
	}

	public void updateLandscapePortrait(@NonNull Activity activity) {
		portraitMode = AndroidUiHelper.isOrientationPortrait(activity);
	}

	public void updateNightMode() {
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(getFragmentTag());
			if (fragment != null)
				((PointEditorFragment) fragment).dismiss();
		}
	}

	public void setCategory(String name, int color) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(getFragmentTag());
			if (fragment != null) {
				if (fragment instanceof PointEditorFragment) {
					PointEditorFragment editorFragment = (PointEditorFragment) fragment;
					editorFragment.setCategory(name, color);
				} else if (fragment instanceof PointEditorFragmentNew) {
					PointEditorFragmentNew editorFragment = (PointEditorFragmentNew) fragment;
					editorFragment.setCategory(name, color);
				}
			}
		}
	}
}
