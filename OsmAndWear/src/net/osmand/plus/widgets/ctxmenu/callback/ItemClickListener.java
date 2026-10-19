package net.osmand.plus.widgets.ctxmenu.callback;

import android.view.View;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

public interface ItemClickListener {
	/**
	 * @return true if drawer should be closed
	 */
	boolean onContextMenuClick(OnDataChangeUiAdapter uiAdapter, View view,
	                           ContextMenuItem item, boolean isChecked);
}
