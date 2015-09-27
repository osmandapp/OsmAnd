package net.osmand.plus.mapcontextmenu.details;

import android.app.Activity;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class MenuController {

	public class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	private MenuBuilder builder;
	private int currentMenuState;
	private boolean portraitMode;
	private boolean largeDevice;

	public MenuController(MenuBuilder builder, Activity activity) {
		this.builder = builder;
		portraitMode = AndroidUiHelper.isOrientationPortrait(activity);
		largeDevice = AndroidUiHelper.isXLargeDevice(activity);
		this.currentMenuState = getInitialMenuState();
	}

	public void build(View rootView) {
		builder.build(rootView);
	}

	public void addPlainMenuItem(int iconId, String text) {
		builder.addPlainMenuItem(iconId, text);
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
		return 350f;
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

	public boolean shouldShowButtons() {
		return true;
	}
}
