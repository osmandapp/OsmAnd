package net.osmand.plus;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

import android.view.Menu;
import android.view.MenuItem;

public class OptionsMenuHelper {

	public interface OnOptionsMenuClick {
		
		public void prepareOptionsMenu(Menu menu, MenuItem item);
		
		public boolean onClick(MenuItem item);
	}
	
	final TIntArrayList items = new TIntArrayList();
	final TIntArrayList itemNames = new TIntArrayList();
	final TIntArrayList visible = new TIntArrayList();
	final ArrayList<OnOptionsMenuClick> listeners = new ArrayList<OnOptionsMenuClick>();
	final TIntArrayList iconList = new TIntArrayList();
	
	public OptionsMenuHelper() {
	}
	
	public void registerOptionsMenu(Menu menu) {
		for (int i = 0; i < items.size(); i++) {
			int resItemId = items.get(i);
			int resName = itemNames.get(i);
			int resIcon = iconList.get(i);
			boolean visibility = visible.get(i) > 0;
			if (resIcon != -1) {
				menu.add(Menu.CATEGORY_CONTAINER, resItemId, Menu.NONE, resName).setVisible(visibility).setIcon(resIcon);
			} else {
				menu.add(Menu.CATEGORY_CONTAINER, resItemId, Menu.NONE, resName).setVisible(visibility);
			}
		}
	}
	
	public boolean onClick(MenuItem mi) {
		int id = mi.getItemId();
		int ind = items.indexOf(id);
		if(ind >= 0 && ind < listeners.size() && listeners.get(ind) != null) {
			return listeners.get(ind).onClick(mi);
		}
		return false;
	}

	public void prepareOptionsMenu(Menu menu) {
		for (int i = 0; i < items.size(); i++) {
			int resItemId = items.get(i);
			OnOptionsMenuClick l = listeners.get(i);
			if (l != null) {
				l.prepareOptionsMenu(menu, menu.findItem(resItemId));
			}
		}
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName, int resIcon, boolean visibility, OnOptionsMenuClick onClick) {
		items.add(resItemId);
		itemNames.add(resName);
		visible.add(visibility ? 1 : 0);
		listeners.add(onClick);
		iconList.add(resIcon);
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName, int resIcon, OnOptionsMenuClick onClick) {
		registerOptionsMenuItem(resItemId, resName, resIcon, true, onClick);
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName, boolean visibility, OnOptionsMenuClick onClick) {
		registerOptionsMenuItem(resItemId, resName, -1, visibility, onClick);
	}
	
	public void registerOptionsMenuItem(int resItemId, int resName, OnOptionsMenuClick onClick) {
		registerOptionsMenuItem(resItemId, resName, -1, true, onClick);
	}

	
}
