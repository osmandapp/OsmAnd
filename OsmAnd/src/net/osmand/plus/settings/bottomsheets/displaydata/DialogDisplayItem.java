package net.osmand.plus.settings.bottomsheets.displaydata;

import android.graphics.drawable.Drawable;

import androidx.annotation.LayoutRes;

public class DialogDisplayItem {

	public String title;
	public String description;
	public Drawable normalIcon;
	public Drawable selectedIcon;
	@LayoutRes
	public int layoutId;
	public Object tag;

}
