package net.osmand.plus.views.controls;

import java.util.List;

public interface DynamicListViewCallbacks {

	void onItemsSwapped(final List<Object> items);
	void onItemSwapping(final int position);

	void onWindowVisibilityChanged(int visibility);

}
