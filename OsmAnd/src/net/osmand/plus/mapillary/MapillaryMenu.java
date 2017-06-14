package net.osmand.plus.mapillary;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MapillaryMenu {

    public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
        ContextMenuAdapter adapter = new ContextMenuAdapter();
        adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
        return adapter;
    }
}
