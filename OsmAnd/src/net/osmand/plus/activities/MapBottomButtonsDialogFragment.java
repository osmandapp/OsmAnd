package net.osmand.plus.activities;

import android.Manifest;
import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.views.MapControlsLayer;

public class MapBottomButtonsDialogFragment extends BottomSheetDialogFragment {
    private MapActivity mapActivity;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean isLightTheme = getMyApplication()
                .getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
        int themeId = isLightTheme ? R.style.OsmandLightTheme_BottomSheet
                : R.style.OsmandDarkTheme_BottomSheet;
        final Dialog dialog = new Dialog(getActivity(), themeId);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().getAttributes().windowAnimations = R.style.Animations_PopUpMenu_Bottom;
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map_bottom_buttons_fragment, container, false);

        mapActivity = (MapActivity)getActivity();
        bottomSheetBehavior = mapActivity.getBottomSheetBehavior();

        IconsCache ic = mapActivity.getMyApplication().getIconsCache();

        View parentLayout = view.findViewById(R.id.map_bottom_buttons);
        parentLayout.setBackgroundColor(getActivity().getResources().getColor(R.color.bg_color_light));

        View configureMapView = view.findViewById(R.id.map_bottom_button_configure_map_view);
        configureMapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity.clearPrevActivityIntent();
                mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONFIGURE_MAP);
                dismiss();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        View searchView = view.findViewById(R.id.map_bottom_button_search_view);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.NEW_IF_EXPIRED, false);
                dismiss();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        View hideView = view.findViewById(R.id.map_bottom_button_hide_view);
        hideView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                bottomSheetBehavior.setHideable(true);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        View routeView = view.findViewById(R.id.map_bottom_button_route_view);
        routeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
                if (mapControlsLayer != null) {
                    mapControlsLayer.doRoute(false);
                }
                dismiss();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        View locationView = view.findViewById(R.id.map_bottom_button_location_view);
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
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        return view;
    }
}
