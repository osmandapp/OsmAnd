package net.osmand.plus.widgets.cmadapter.callback;

import android.widget.ArrayAdapter;

import net.osmand.plus.widgets.cmadapter.item.ContextMenuItem;

public interface ItemClickListener {
	/**
	 * @return true if drawer should be closed
	 */
	boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
	                           int itemId, int position, boolean isChecked,
	                           int[] viewCoordinates);
}
