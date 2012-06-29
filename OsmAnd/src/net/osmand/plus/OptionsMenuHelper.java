package net.osmand.plus;

import android.view.Menu;

public class OptionsMenuHelper {

	private final Menu menu;
	
	public OptionsMenuHelper(Menu menu) {
		this.menu = menu;
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName, int resIcon, boolean visibility) {
		if (resIcon != -1) {
			menu.add(Menu.CATEGORY_CONTAINER, resItemId, Menu.NONE, resName).setVisible(visibility).setIcon(resIcon);
		} else {
			menu.add(Menu.CATEGORY_CONTAINER, resItemId, Menu.NONE, resName).setVisible(visibility);
		}
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName, int resIcon) {
		registerOptionsMenuItem(resItemId, resName, resIcon, true);
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName, boolean visibility) {
		registerOptionsMenuItem(resItemId, resName, -1, visibility);
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName) {
		registerOptionsMenuItem(resItemId, resName, -1, true);
	}
}
