package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;

import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class BaseMenuController {

	private MapActivity mapActivity;
	private boolean portraitMode;
	private boolean nightMode;
	private int landscapeWidthPx;

	public BaseMenuController(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		init();
	}

	private void init() {
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
		landscapeWidthPx = mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
		updateNightMode();
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		init();
	}

	public boolean isLight() {
		return !nightMode;
	}

	public void updateNightMode() {
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
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

	protected Drawable getIconOrig(int iconId) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, 0);
	}

	protected Drawable getIcon(int iconId) {
		return getIcon(iconId, isLight() ? R.color.icon_color : R.color.icon_color_light);
	}

	protected Drawable getIcon(int iconId, int colorId) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, colorId);
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getPaintedIcon(iconId, color);
	}

	protected Drawable getIcon(int iconId, int colorLightId, int colorDarkId) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, isLight() ? colorLightId : colorDarkId);
	}
}
