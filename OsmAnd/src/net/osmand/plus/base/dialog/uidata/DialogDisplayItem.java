package net.osmand.plus.base.dialog.uidata;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;

public class DialogDisplayItem {

	@LayoutRes
	public int layoutId;
	public String title;
	public String description;
	public Drawable normalIcon;
	public Drawable selectedIcon;
	@ColorInt
	public Integer customControlsColor;
	public boolean addDividerAfter;
	public Object tag;

}
