package net.osmand.plus.widgets.ctxmenu.callback;

import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

public abstract class OnRowItemClick implements ItemClickListener {

	//boolean return type needed to describe if drawer needed to be close or not
	public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
	                              @NonNull View view, @NonNull ContextMenuItem item) {
		CompoundButton btn = view.findViewById(R.id.toggle_item);
		if (btn != null && btn.getVisibility() == View.VISIBLE) {
			btn.setChecked(!btn.isChecked());
			return false;
		} else {
			return onContextMenuClick(uiAdapter, view, item, false);
		}
	}
}
