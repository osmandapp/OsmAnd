package net.osmand.plus.widgets.ctxmenu.callback;

import android.widget.ArrayAdapter;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

public interface ItemClickListener {
	/**
	 * @return true if drawer should be closed
	 */
	boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
	                           int itemId, int position, boolean isChecked,
	                           int[] viewCoordinates);
}
