package net.osmand.plus.widgets.popup;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;

import net.osmand.plus.R;

import java.util.List;

public class PopUpMenuDisplayData {

	private static final int DEFAULT_LAYOUT_ID = R.layout.popup_menu_item;

	public View anchorView;

	/**
	 * @deprecated Unused with Compose dropdown menus. Custom XML layout is not inflated.
	 */
	@Deprecated
	public @LayoutRes int layoutId = DEFAULT_LAYOUT_ID;

	public @ColorInt int bgColor;

	/**
	 * @deprecated Unused with Compose dropdown menus. Theme is resolved from the context theme.
	 */
	@Deprecated
	public boolean nightMode;

	/**
	 * @deprecated Unused with Compose dropdown menus. Width is measured intrinsically.
	 */
	@Deprecated
	public PopUpMenuWidthMode widthMode = PopUpMenuWidthMode.AS_ANCHOR_VIEW;

	public List<PopUpMenuItem> menuItems;
	public OnPopUpMenuItemClickListener onItemClickListener;
	public CustomDropDown customDropDown = CustomDropDown.AUTO_DROP_DOWN;

	/**
	 * @deprecated Unused with Compose dropdown menus. Compound controls are configured on each {@link PopUpMenuItem}.
	 */
	@Deprecated
	public boolean showCompound = true;

	/**
	 * @deprecated Unused with Compose dropdown menus. Height and scrolling are managed automatically by Compose.
	 */
	@Deprecated
	public boolean limitHeight = false;

	public Integer dropDownGravity;
	public Integer horizontalOffset;
	public Integer verticalOffset;

	public enum CustomDropDown {
		AUTO_DROP_DOWN,
		TOP_DROPDOWN,
		BOTTOM_DROPDOWN,
		NONE
	}
}
