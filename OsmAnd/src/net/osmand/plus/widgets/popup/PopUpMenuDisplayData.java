package net.osmand.plus.widgets.popup;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;

import net.osmand.plus.R;

import java.util.List;

public class PopUpMenuDisplayData {

	private static final int DEFAULT_LAYOUT_ID = R.layout.popup_menu_item;

	public View anchorView;
	public @LayoutRes int layoutId = DEFAULT_LAYOUT_ID;
	public @ColorInt int bgColor;
	public boolean nightMode;
	public PopUpMenuWidthMode widthMode = PopUpMenuWidthMode.AS_ANCHOR_VIEW;
	public List<PopUpMenuItem> menuItems;
	public OnPopUpMenuItemClickListener onItemClickListener;
	public CustomDropDown customDropDown = CustomDropDown.AUTO_DROP_DOWN;
	public boolean showCompound = true;
	public boolean limitHeight = false;

	public boolean hasCustomizations() {
		if (layoutId != DEFAULT_LAYOUT_ID) {
			return true;
		}
		for (PopUpMenuItem menuItem : menuItems) {
			if (menuItem.hasCustomization()) {
				return true;
			}
		}
		return false;
	}

	public enum CustomDropDown {
		AUTO_DROP_DOWN,
		TOP_DROPDOWN,
		BOTTOM_DROPDOWN,
		NONE
	}
}
