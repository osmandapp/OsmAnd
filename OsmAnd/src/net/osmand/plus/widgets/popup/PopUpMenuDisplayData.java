package net.osmand.plus.widgets.popup;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;

import java.util.List;

public class PopUpMenuDisplayData {
	public View anchorView;
	public @LayoutRes int layoutId;
	public @ColorInt int bgColor;
	public boolean nightMode;
	public PopUpMenuWidthMode widthMode = PopUpMenuWidthMode.AS_ANCHOR_VIEW;
	public List<PopUpMenuItem> menuItems;
}
