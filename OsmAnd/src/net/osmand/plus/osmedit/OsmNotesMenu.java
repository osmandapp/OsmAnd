package net.osmand.plus.osmedit;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class OsmNotesMenu {

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter adapter, final MapActivity mapActivity) {

	}
}
