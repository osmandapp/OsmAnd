package net.osmand.core.samples.android.sample1.mapcontextmenu;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.IconsCache;
import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleUtils;

public abstract class BaseMenuController {

	private MainActivity mainActivity;
	private boolean portraitMode;
	private boolean nightMode;
	private int landscapeWidthPx;

	public BaseMenuController(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
		init();
	}

	private void init() {
		portraitMode = SampleUtils.isOrientationPortrait(mainActivity);
		landscapeWidthPx = mainActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
		updateNightMode();
	}

	public MainActivity getMainActivity() {
		return mainActivity;
	}

	public void setMainActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
		init();
	}

	public boolean isLight() {
		return !nightMode;
	}

	public void updateNightMode() {
		//nightMode = mainActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
	}

	public boolean isLandscapeLayout() {
		return !portraitMode;
	}

	public int getLandscapeWidthPx() {
		return landscapeWidthPx;
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

	protected Drawable getIconOrig(int osmandIconId) {
		IconsCache iconsCache = getMainActivity().getMyApplication().getIconsCache();
		return iconsCache.getOsmandIcon(osmandIconId, 0);
	}

	protected Drawable getIcon(int osmandIconId) {
		return getIcon(osmandIconId, isLight() ? R.color.icon_color : R.color.icon_color_light);
	}

	protected Drawable getIcon(int osmandIconId, int colorId) {
		IconsCache iconsCache = getMainActivity().getMyApplication().getIconsCache();
		return iconsCache.getOsmandIcon(osmandIconId, colorId);
	}

	protected Drawable getPaintedIcon(int osmandIconId, int color) {
		IconsCache iconsCache = getMainActivity().getMyApplication().getIconsCache();
		return iconsCache.getPaintedOsmandIcon(osmandIconId, color);
	}

	protected Drawable getIcon(int osmandIconId, int colorLightId, int colorDarkId) {
		IconsCache iconsCache = getMainActivity().getMyApplication().getIconsCache();
		return iconsCache.getOsmandIcon(osmandIconId, isLight() ? colorLightId : colorDarkId);
	}
}
