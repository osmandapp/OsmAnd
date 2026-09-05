package net.osmand.plus.views.controls;

import java.util.List;

public interface DynamicListViewCallbacks {

	void onItemsSwapped(List<Object> items);
	void onItemSwapping(int position);

	void onWindowVisibilityChanged(int visibility);

}
