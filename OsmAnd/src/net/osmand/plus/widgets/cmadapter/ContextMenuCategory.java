package net.osmand.plus.widgets.cmadapter;

import androidx.annotation.Nullable;

public class ContextMenuCategory extends ContextMenuItem {

	public ContextMenuCategory(@Nullable String id) {
		super(id);
	}

	@Override
	public boolean isCategory() {
		return true;
	}

}
