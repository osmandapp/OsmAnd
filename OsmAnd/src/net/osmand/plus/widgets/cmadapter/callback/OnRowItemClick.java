package net.osmand.plus.widgets.cmadapter.callback;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import net.osmand.plus.R;
import net.osmand.plus.widgets.cmadapter.item.ContextMenuItem;

public abstract class OnRowItemClick implements ItemClickListener {

	//boolean return type needed to describe if drawer needed to be close or not
	public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
		CompoundButton btn = view.findViewById(R.id.toggle_item);
		if (btn != null && btn.getVisibility() == View.VISIBLE) {
			btn.setChecked(!btn.isChecked());
			return false;
		} else {
			return onContextMenuClick(adapter, itemId, position, false, null);
		}
	}
}
