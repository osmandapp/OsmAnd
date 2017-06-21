package net.osmand.plus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.liveupdates.OsmLiveActivity;

import java.util.List;

public class MapMenuDialogFragment extends BottomSheetDialogFragment{

    private MapActivity mapActivity;

    @Override
    public void onStart() {
        super.onStart();

        final Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.BOTTOM;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mapActivity = (MapActivity)getActivity();

        View view = inflater.inflate(R.layout.map_menu_fragment, container, false);

        IconsCache ic = mapActivity.getMyApplication().getIconsCache();

        View dashboardView = view.findViewById(R.id.dashboard_view);
        ((ImageView) view.findViewById(R.id.dashboard_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.map_dashboard));
        dashboardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity.clearPrevActivityIntent();
                mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.DASHBOARD);
                dismiss();
            }
        });

        View mapMarkersView = view.findViewById(R.id.map_markers_view);
        if (!mapActivity.getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
            mapMarkersView.setVisibility(View.GONE);
        }
        ((ImageView) view.findViewById(R.id.map_markers_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_flag_dark));
        mapMarkersView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity.clearPrevActivityIntent();
                mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS);
                dismiss();
            }
        });

        View waypointsView = view.findViewById(R.id.waypoints_view);
        if (mapActivity.getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
            waypointsView.setVisibility(View.GONE);
        }
        ((ImageView) view.findViewById(R.id.waypoints_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_intermediate));
        waypointsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity.clearPrevActivityIntent();
                mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.WAYPOINTS);
                dismiss();
            }
        });

        View myPlacesView = view.findViewById(R.id.my_places_view);
        ((ImageView) view.findViewById(R.id.my_places_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_fav_dark));
        myPlacesView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                        .getFavoritesActivity());
                newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                mapActivity.startActivity(newIntent);
                dismiss();
            }
        });

        View legacySearchView = view.findViewById(R.id.legacy_search_view);
        if (!mapActivity.getMyApplication().getSettings().SHOW_LEGACY_SEARCH.get()) {
            legacySearchView.setVisibility(View.GONE);
        }
        ((ImageView) view.findViewById(R.id.legacy_search_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_search_dark));
        legacySearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                dismiss();
            }
        });

        View downloadMapsView = view.findViewById(R.id.download_maps_view);
        String d = mapActivity.getMyApplication().getString(R.string.welmode_download_maps);
        if (mapActivity.getMyApplication().getDownloadThread().getIndexes().isDownloadedFromInternet) {
            List<IndexItem> updt = mapActivity.getMyApplication().getDownloadThread().getIndexes().getItemsToUpdate();
            if (updt != null && updt.size() > 0) {
                d += " (" + updt.size() + ")";
            }
        }
        ((TextView) view.findViewById(R.id.download_maps_text)).
                setText(d);
        ((ImageView) view.findViewById(R.id.download_maps_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_type_archive));
        downloadMapsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                        .getDownloadActivity());
                newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                mapActivity.startActivity(newIntent);
                dismiss();
            }
        });

        View osmLiveView = view.findViewById(R.id.osm_live_view);
        if (!(Version.isGooglePlayEnabled(mapActivity.getMyApplication()) || Version.isDeveloperVersion(mapActivity.getMyApplication()))) {
            osmLiveView.setVisibility(View.GONE);
        }
        ((ImageView) view.findViewById(R.id.osm_live_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_osm_live));
        osmLiveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mapActivity, OsmLiveActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                mapActivity.startActivity(intent);
                dismiss();
            }
        });

        View pluginsView = view.findViewById(R.id.plugins_view);
        ((ImageView) view.findViewById(R.id.plugins_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_extension_dark));
        pluginsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                        .getPluginsActivity());
                newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                mapActivity.startActivity(newIntent);
                dismiss();
            }
        });

        View configureScreenView = view.findViewById(R.id.configure_screen_view);
        ((ImageView) view.findViewById(R.id.configure_screen_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_configure_screen_dark));
        configureScreenView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity.clearPrevActivityIntent();
                mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONFIGURE_SCREEN);
                dismiss();
            }
        });

        View settingsView = view.findViewById(R.id.settings_view);
        ((ImageView) view.findViewById(R.id.settings_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_settings));
        settingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent settings = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
                        .getSettingsActivity());
                settings.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                mapActivity.startActivity(settings);
                dismiss();
            }
        });

        View helpView = view.findViewById(R.id.help_view);
        ((ImageView) view.findViewById(R.id.help_icon)).
                setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_help));
        helpView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mapActivity, HelpActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                mapActivity.startActivity(intent);
                dismiss();
            }
        });

        return view;
    }
}
