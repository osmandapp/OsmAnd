package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class BaseMenuController {

	@Nullable
	private MapActivity mapActivity;
	private boolean portraitMode;
	private int landscapeWidthPx;
	protected boolean nightMode;

	public BaseMenuController(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		init();
	}

	private void init() {
		if (mapActivity != null) {
			portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
			landscapeWidthPx = mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
			updateNightMode();
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		if (mapActivity != null) {
			init();
		}
	}

	public boolean isLight() {
		return !nightMode;
	}

	public void updateNightMode() {
		if (mapActivity != null) {
			nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		}
	}

	public boolean isLandscapeLayout() {
		return !portraitMode;
	}

	public int getLandscapeWidthPx() {
		return landscapeWidthPx;
	}

	public float getHalfScreenMaxHeightKoef() {
		return .75f;
	}

	public int getSlideInAnimation() {
		if (isLandscapeLayout()) {
			return AndroidUtils.isLayoutRtl(getMapActivity())
					? R.anim.slide_in_right : R.anim.slide_in_left;
		} else {
			return R.anim.slide_in_bottom;
		}
	}

	public int getSlideOutAnimation() {
		if (isLandscapeLayout()) {
			return AndroidUtils.isLayoutRtl(getMapActivity())
					? R.anim.slide_out_right : R.anim.slide_out_left;
		} else {
			return R.anim.slide_out_bottom;
		}
	}

	protected Drawable getIconOrig(int iconId) {
		if (mapActivity != null) {
			UiUtilities iconsCache = mapActivity.getMyApplication().getUIUtilities();
			return iconsCache.getIcon(iconId, 0);
		} else {
			return null;
		}
	}

	protected Drawable getIcon(int iconId) {
		return getIcon(iconId, ColorUtilities.getDefaultIconColorId(!isLight()));
	}

	protected Drawable getIcon(int iconId, int colorId) {
		if (mapActivity != null) {
			UiUtilities iconsCache = mapActivity.getMyApplication().getUIUtilities();
			return iconsCache.getIcon(iconId, colorId);
		} else {
			return null;
		}
	}

	@NonNull
	protected String getString(@StringRes int resId, Object... formatArgs) {
		return mapActivity != null ? mapActivity.getString(resId, formatArgs) : "";
	}
}
