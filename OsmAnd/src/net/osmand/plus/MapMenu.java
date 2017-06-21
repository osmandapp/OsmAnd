package net.osmand.plus;

import android.content.Intent;
import android.widget.ArrayAdapter;

import net.osmand.data.LatLon;
import net.osmand.plus.activities.HelpActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.liveupdates.OsmLiveActivity;

import java.util.List;

public class MapMenu {
    public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
        ContextMenuAdapter mapMenuAdapter = new ContextMenuAdapter();

        mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.home, mapActivity)
                .setIcon(R.drawable.map_dashboard)
                .setListener(new ContextMenuAdapter.ItemClickListener() {
                    @Override
                    public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                        MapActivity.clearPrevActivityIntent();
                        mapActivity.closeDrawer();
                        mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.DASHBOARD);
                        return false;
                    }
                }).createItem());

        if (mapActivity.getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
            mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_markers, mapActivity)
                    .setIcon(R.drawable.ic_action_flag_dark)
                    .setListener(new ContextMenuAdapter.ItemClickListener() {
                        @Override
                        public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                            MapActivity.clearPrevActivityIntent();
                            mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS);
                            return false;
                        }
                    }).createItem());
        } else {
            mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.waypoints, mapActivity)
                    .setIcon(R.drawable.ic_action_intermediate)
                    .setListener(new ContextMenuAdapter.ItemClickListener() {
                        @Override
                        public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                            MapActivity.clearPrevActivityIntent();
                            mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.WAYPOINTS);
                            return false;
                        }
                    }).createItem());
        }

        mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_my_places, mapActivity)
                .setIcon(R.drawable.ic_action_fav_dark)
                .setListener(new ContextMenuAdapter.ItemClickListener() {
                    @Override
                    public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                        Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                                .getFavoritesActivity());
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        mapActivity.startActivity(newIntent);
                        return true;
                    }
                }).createItem());

        if (mapActivity.getMyApplication().getSettings().SHOW_LEGACY_SEARCH.get()) {
            mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.legacy_search, mapActivity)
                    .setIcon(R.drawable.ic_action_search_dark)
                    .setListener(new ContextMenuAdapter.ItemClickListener() {
                        @Override
                        public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                            Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                                    .getSearchActivity());
                            LatLon loc = mapActivity.getMapLocation();
                            newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
                            newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
                            if (mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
                                newIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
                            }
                            newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            mapActivity.startActivity(newIntent);
                            return true;
                        }
                    }).createItem());
        }

        String d = mapActivity.getMyApplication().getString(R.string.welmode_download_maps);
        if (mapActivity.getMyApplication().getDownloadThread().getIndexes().isDownloadedFromInternet) {
            List<IndexItem> updt = mapActivity.getMyApplication().getDownloadThread().getIndexes().getItemsToUpdate();
            if (updt != null && updt.size() > 0) {
                d += " (" + updt.size() + ")";
            }
        }
        mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.welmode_download_maps, null)
                .setTitle(d).setIcon(R.drawable.ic_type_archive)
                .setListener(new ContextMenuAdapter.ItemClickListener() {
                    @Override
                    public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                        Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                                .getDownloadActivity());
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        mapActivity.startActivity(newIntent);
                        return true;
                    }
                }).createItem());

        if (Version.isGooglePlayEnabled(mapActivity.getMyApplication()) || Version.isDeveloperVersion(mapActivity.getMyApplication())) {
            mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.osm_live, mapActivity)
                    .setIcon(R.drawable.ic_action_osm_live)
                    .setListener(new ContextMenuAdapter.ItemClickListener() {
                        @Override
                        public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                            Intent intent = new Intent(mapActivity, OsmLiveActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            mapActivity.startActivity(intent);
                            return false;
                        }
                    }).createItem());
        }

        mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.prefs_plugins, mapActivity)
                .setIcon(R.drawable.ic_extension_dark)
                .setListener(new ContextMenuAdapter.ItemClickListener() {
                    @Override
                    public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                        Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                                .getPluginsActivity());
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        mapActivity.startActivity(newIntent);
                        return true;
                    }
                }).createItem());

        mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.layer_map_appearance, mapActivity)
                .setIcon(R.drawable.ic_configure_screen_dark)
                .setListener(new ContextMenuAdapter.ItemClickListener() {
                    @Override
                    public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                        MapActivity.clearPrevActivityIntent();
                        mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONFIGURE_SCREEN);
                        return false;
                    }
                }).createItem());

        mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_settings, mapActivity)
                .setIcon(R.drawable.ic_action_settings)
                .setListener(new ContextMenuAdapter.ItemClickListener() {
                    @Override
                    public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                        final Intent settings = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                                .getSettingsActivity());
                        settings.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        mapActivity.startActivity(settings);
                        return true;
                    }
                }).createItem());

        mapMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_help, mapActivity)
                .setIcon(R.drawable.ic_action_help)
                .setListener(new ContextMenuAdapter.ItemClickListener() {
                    @Override
                    public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
                        Intent intent = new Intent(mapActivity, HelpActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        mapActivity.startActivity(intent);
                        return true;
                    }
                }).createItem());

        //////////// Others
        OsmandPlugin.registerOptionsMenu(mapActivity, mapMenuAdapter);

        int pluginsItemIndex = -1;
        for (int i = 0; i < mapMenuAdapter.length(); i++) {
            if (mapMenuAdapter.getItem(i).getTitleId() == R.string.prefs_plugins) {
                pluginsItemIndex = i;
                break;
            }
        }

        ContextMenuItem.ItemBuilder divider = new ContextMenuItem.ItemBuilder().setLayout(R.layout.drawer_divider);
        divider.setPosition(pluginsItemIndex >= 0 ? pluginsItemIndex : 7);
        mapMenuAdapter.addItem(divider.createItem());;

        return mapMenuAdapter;
    }
}
