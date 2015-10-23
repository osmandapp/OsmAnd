package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class MenuController {

	public final static float LANDSCAPE_WIDTH_DP = 350f;

	public class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	private MapActivity mapActivity;
	private MenuBuilder builder;
	private int currentMenuState;
	private boolean portraitMode;
	private boolean largeDevice;
	private boolean light;

	public MenuController(MenuBuilder builder, MapActivity mapActivity) {
		this.builder = builder;
		this.mapActivity = mapActivity;
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
		largeDevice = AndroidUiHelper.isXLargeDevice(mapActivity);
		light = mapActivity.getMyApplication().getSettings().isLightContent();
		this.currentMenuState = getInitialMenuState();
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void build(View rootView) {
		builder.build(rootView);
	}

	public void addPlainMenuItem(int iconId, String text) {
		builder.addPlainMenuItem(iconId, text);
	}

	public void addPlainMenuItems(String typeStr, PointDescription pointDescription) {
	}

	public int getInitialMenuState() {
		if (isLandscapeLayout()) {
			return MenuState.FULL_SCREEN;
		} else {
			return getInitialMenuStatePortrait();
		}
	}

	public boolean isLandscapeLayout() {
		return !portraitMode && !largeDevice;
	}

	public float getLandscapeWidthDp() {
		return LANDSCAPE_WIDTH_DP;
	}

	public int getSupportedMenuStates() {
		if (isLandscapeLayout()) {
			return MenuState.FULL_SCREEN;
		} else {
			return getSupportedMenuStatesPortrait();
		}
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

	protected int getInitialMenuStatePortrait() {
		return MenuState.HEADER_ONLY;
	}

	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	public int getCurrentMenuState() {
		return currentMenuState;
	}

	public boolean slideUp() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v << 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public boolean slideDown() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v >> 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public void setCurrentMenuState(int currentMenuState) {
		this.currentMenuState = currentMenuState;
	}

	public float getHalfScreenMaxHeightKoef() {
		return .7f;
	}

	protected Drawable getIcon(int iconId) {
		return getIcon(iconId, R.color.icon_color, R.color.icon_color_light);
	}

	protected Drawable getIcon(int iconId, int colorLightId, int colorDarkId) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId,
				light ? colorLightId : colorDarkId);
	}

	public boolean hasTitleButton() {
		return false;
	}

	public String getTitleButtonCaption() {
		return "";
	}

	public boolean shouldShowButtons() {
		return true;
	}

	public boolean handleSingleTapOnMap() {
		return false;
	}

	public boolean needStreetName() {
		return true;
	}

	public boolean needTypeStr() {
		return false;
	}

	public int getLeftIconId() { return 0; }

	public Drawable getLeftIcon() { return null; }

	public Drawable getSecondLineIcon() { return null; }

	public int getFavActionIconId() { return R.drawable.ic_action_fav_dark; }

	public String getTypeStr() { return ""; }

	public String getNameStr() { return ""; }

	public abstract void saveEntityState(Bundle bundle, String key);
}