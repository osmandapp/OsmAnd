package net.osmand.plus.views.controls.maphudbuttons;

import android.Manifest;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MultiTouchSupport;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;

public class MyLocationButton extends MapButton {

	private final boolean contextMenuAllowed;

	private final OnClickListener backToLocationListener = v -> moveBackToLocation(false);
	private final OnLongClickListener backToLocationWithMenu = v -> moveBackToLocation(true);

	public MyLocationButton(@NonNull MapActivity mapActivity, @NonNull ImageView view, @NonNull String id, boolean contextMenuAllowed) {
		super(mapActivity, view, id);
		this.contextMenuAllowed = contextMenuAllowed;
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setBackground(R.drawable.btn_circle_blue);
		updateState(app.getDaynightHelper().isNightModeForMapControls());
	}

	private boolean moveBackToLocation(boolean showLocationMenu) {
		if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
			if (showLocationMenu) {
				showContextMenuForMyLocation();
			} else if (!mapActivity.getContextMenu().isVisible()) {
				app.getMapViewTrackingUtilities().backToLocationImpl();
			}
		} else {
			ActivityCompat.requestPermissions(mapActivity,
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
					 Manifest.permission.ACCESS_COARSE_LOCATION},
					OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
		}
		return false;
	}

	private void showContextMenuForMyLocation() {
		ContextMenuLayer contextMenuLayer = app.getOsmandMap().getMapLayers().getContextMenuLayer();
		if (contextMenuLayer != null) {
			contextMenuLayer.showContextMenuForMyLocation();
		}
	}

	@Override
	protected void updateState(boolean nightMode) {
		boolean hasLocation = app.getLocationProvider().getLastKnownLocation() != null;
		boolean linkedToLocation = app.getMapViewTrackingUtilities().isMapLinkedToLocation();

		if (app.accessibilityEnabled()) {
			boolean visible = view.getVisibility() == View.VISIBLE;
			view.setClickable(hasLocation && visible);
		}

		if (!hasLocation) {
			setNoLocationState();
		} else if (linkedToLocation) {
			setMapLinkedToLocationState();
		} else {
			setReturnToLocationState();
		}
	}

	private void setNoLocationState() {
		setBackground(R.drawable.btn_circle, R.drawable.btn_circle_night);
		setIconId(R.drawable.ic_my_location);
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setContentDesc(R.string.unknown_location);
		if (view.isClickable()) {
			setMyLocationListeners();
		}
		updateIcon(app.getDaynightHelper().isNightModeForMapControls());
	}

	private void setMapLinkedToLocationState() {
		setIconId(R.drawable.ic_my_location);
		setIconColorId(R.color.color_myloc_distance);
		setBackground(R.drawable.btn_circle, R.drawable.btn_circle_night);
		setContentDesc(R.string.access_map_linked_to_location);
		if (view.isClickable()) {
			setMyLocationListeners();
		}
		updateIcon(app.getDaynightHelper().isNightModeForMapControls());
	}

	private void setReturnToLocationState() {
		setIconId(R.drawable.ic_my_location);
		setIconColorId(0);
		setBackground(R.drawable.btn_circle_blue);
		setContentDesc(R.string.map_widget_back_to_loc);
		if (view.isClickable()) {
			setMyLocationListeners();
		}
		updateIcon(app.getDaynightHelper().isNightModeForMapControls());
	}

	private void setMyLocationListeners() {
		setOnClickListener(backToLocationListener);
		if (contextMenuAllowed) {
			setOnLongClickListener(backToLocationWithMenu);
		}
	}

	@Override
	protected boolean shouldShow() {
		return !isRouteDialogOpened() && widgetsVisibilityHelper.shouldShowBackToLocationButton();
	}
}