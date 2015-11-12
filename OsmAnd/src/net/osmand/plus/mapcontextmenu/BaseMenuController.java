package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class BaseMenuController {

	public final static float LANDSCAPE_WIDTH_DP = 350f;

	private MapActivity mapActivity;
	private boolean portraitMode;
	private boolean largeDevice;
	private boolean light;

	public BaseMenuController(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		init();
	}

	private void init() {
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
		largeDevice = AndroidUiHelper.isXLargeDevice(mapActivity);
		light = mapActivity.getMyApplication().getSettings().isLightContent();
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		init();
	}

	public boolean isLight() {
		return light;
	}

	public boolean isLandscapeLayout() {
		return !portraitMode && !largeDevice;
	}

	public float getLandscapeWidthDp() {
		return LANDSCAPE_WIDTH_DP;
	}

	public float getHalfScreenMaxHeightKoef() {
		return .7f;
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
		return iconsCache.getIcon(iconId, 0, 0f);
	}

	protected Drawable getIcon(int iconId) {
		return getIcon(iconId, R.color.icon_color, R.color.icon_color_light);
	}

	protected Drawable getIcon(int iconId, int colorId) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, colorId);
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getPaintedContentIcon(iconId, color);
	}

	protected Drawable getIcon(int iconId, int colorLightId, int colorDarkId) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, isLight() ? colorLightId : colorDarkId);
	}

	protected int getResIdFromAttribute(final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedvalueattr = new TypedValue();
		getMapActivity().getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

}
