package net.osmand.plus.mapillary;

import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MapillaryMenu {

    public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
        ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity);
        adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
        createLayersItems(adapter, mapActivity);
        return adapter;
    }

    private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter, final MapActivity mapActivity) {
        final OsmandApplication app = mapActivity.getMyApplication();
        final OsmandSettings settings = app.getSettings();
        final MapillaryPlugin plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
        if (plugin == null) {
            return;
        }

        final boolean selected = settings.SHOW_MAPILLARY.get();
        final int toggleActionStringId = selected ? R.string.shared_string_enabled : R.string.shared_string_disabled;

        ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
            @Override
            public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked) {
                if (itemId == toggleActionStringId) {
                    settings.SHOW_MAPILLARY.set(!settings.SHOW_MAPILLARY.get());
                    plugin.updateLayers(mapActivity.getMapView(), mapActivity);
                    mapActivity.getDashboard().refreshContent(true);
                }
                return false;
            }
        };

        boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
        int toggleIconColorId;
        int toggleIconId;
        if (selected) {
            toggleIconId = R.drawable.ic_action_view;
            toggleIconColorId = nightMode ? R.color.color_dialog_buttons_light : R.color.color_dialog_buttons_dark;
        } else {
            toggleIconId = R.drawable.ic_action_hide;
            toggleIconColorId = nightMode ? R.color.icon_color : 0;
        }

        contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
                .setTitleId(toggleActionStringId, mapActivity)
                .setIcon(toggleIconId)
                .setColor(toggleIconColorId)
                .setListener(l)
                .setSelected(selected)
                .createItem());

        contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
                .setTitleId(R.string.search_poi_filter, mapActivity)
                .setDescription(app.getString(R.string.mapillary_menu_filter_description))
                .setCategory(true)
                .setLayout(R.layout.list_group_title_with_descr)
                .createItem());

        contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
                .setTitleId(R.string.mapillary_menu_title_username, mapActivity)
                .setDescription(app.getString(R.string.mapillary_menu_descr_username))
                .setIcon(R.drawable.ic_action_user)
                .setClickable(false)
                .setLayout(R.layout.list_item_icon_with_title_and_descr)
                .createItem());

        contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
                .setLayout(R.layout.list_item_auto_complete_text_view)
                .setClickable(false)
                .createItem());

        contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
                .setTitleId(R.string.mapillary_menu_title_dates, mapActivity)
                .setDescription(app.getString(R.string.mapillary_menu_descr_dates))
                .setIcon(R.drawable.ic_action_data)
                .setClickable(false)
                .setLayout(R.layout.list_item_icon_with_title_and_descr)
                .createItem());

        contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
                .setLayout(R.layout.list_item_date_from_and_to)
                .setClickable(false)
                .createItem());

        contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
                .setLayout(R.layout.list_item_buttons)
                .setClickable(false)
                .createItem());
    }
}
