package net.osmand.plus.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.liveupdates.OsmLiveActivity;
import net.osmand.plus.views.MapControlsLayer;

import java.util.List;

public class MapMenuDialogFragment extends android.support.design.widget.BottomSheetDialogFragment{

    private MapActivity mapActivity;

    private BottomSheetBehavior.BottomSheetCallback bottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    };

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.map_menu_fragment, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        if( behavior != null && behavior instanceof BottomSheetBehavior ) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(bottomSheetBehaviorCallback);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mapActivity = (MapActivity)getActivity();

        View view = inflater.inflate(R.layout.map_menu_fragment, container, false);

        IconsCache ic = mapActivity.getMyApplication().getIconsCache();

        View parentLayout = view.findViewById(R.id.map_menu_parent);
        parentLayout.setBackgroundColor(getActivity().getResources().getColor(R.color.bg_color_light));

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

        View configureMapView = view.findViewById(R.id.map_menu_configure_map_view);
        configureMapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity.clearPrevActivityIntent();
                mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONFIGURE_MAP);
                dismiss();
            }
        });

        View searchView = view.findViewById(R.id.map_menu_search_view);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.NEW_IF_EXPIRED, false);
                dismiss();
            }
        });

        View hideView = view.findViewById(R.id.map_menu_hide_view);
        hideView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        View routeView = view.findViewById(R.id.map_menu_route_view);
        routeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
                if (mapControlsLayer != null) {
                    mapControlsLayer.doRoute(false);
                }
                dismiss();
            }
        });

        View locationView = view.findViewById(R.id.map_menu_location_view);
        locationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
                    mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
                } else {
                    ActivityCompat.requestPermissions(mapActivity,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
                }
                dismiss();
            }
        });

        return view;
    }
}
