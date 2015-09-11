package net.osmand.plus.mapcontextmenu.sections;

import android.view.View;

public abstract class MenuController {

	public class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	private MenuBuilder builder;
	private int currentMenuState;

	public MenuController(MenuBuilder builder) {
		this.builder = builder;
		this.currentMenuState = getInitialMenuState();
	}

	public void build(View rootView) {
		builder.build(rootView);
	}

	public int getInitialMenuState() {
		return MenuState.HEADER_ONLY;
	}

	public int getSupportedMenuStates() {
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
