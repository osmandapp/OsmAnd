package net.osmand.plus.views.controls.maphudbuttons;

import android.Manifest;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MultiTouchSupport;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MyLocationButton extends MapButton {

	private final OsmandMapTileView mapView;
	private final AnimateDraggingMapThread animateDraggingMapThread;
	private final boolean contextMenuAllowed;

	private final OnClickListener backToLocationListener = v -> moveBackToLocation(false);
	private final OnLongClickListener backToLocationWithMenu = v -> moveBackToLocation(true);
	private final OnClickListener tiltMapListener;
	private final OnClickListener resetMapTiltListener;

	public MyLocationButton(@NonNull MapActivity mapActivity, @NonNull ImageView view, @NonNull String id, boolean contextMenuAllowed) {
		super(mapActivity, view, id);
		this.contextMenuAllowed = contextMenuAllowed;
		this.mapView = mapActivity.getMapView();
		this.animateDraggingMapThread = mapView.getAnimatedDraggingThread();
		this.tiltMapListener = v -> {
			animateDraggingMapThread.animateElevationAngleChange(45);
			mapView.refreshMap();
		};
		this.resetMapTiltListener = v -> {
			animateDraggingMapThread.animateElevationAngleChange(OsmandMapTileView.DEFAULT_ELEVATION_ANGLE);
			mapView.refreshMap();
		};
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setBackground(R.drawable.btn_circle_blue);
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
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
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
		float elevationAngle = mapView.getElevationAngle();

		if (app.accessibilityEnabled()) {
			boolean visible = view.getVisibility() == View.VISIBLE;
			view.setClickable(hasLocation && visible);
		}

		if (!hasLocation) {
			setNoLocationState();
		} else if (linkedToLocation) {
			if (!MultiTouchSupport.isTiltSupportEnabled(app)) {
				setMapLinkedToLocationState();
			} else if (elevationAngle == OsmandMapTileView.DEFAULT_ELEVATION_ANGLE) {
				setTiltMapState();
			} else {
				setRestoreMapTiltState();
			}
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
	}

	private void setMapLinkedToLocationState() {
		setIconId(R.drawable.ic_my_location);
		setIconColorId(R.color.color_myloc_distance);
		setBackground(R.drawable.btn_circle, R.drawable.btn_circle_night);
		setContentDesc(R.string.access_map_linked_to_location);
		if (view.isClickable()) {
			setMyLocationListeners();
		}
	}

	private void setTiltMapState() {
		setIconId(R.drawable.ic_action_2_5d_view_on);
		setIconColorId(R.color.color_myloc_distance);
		setBackground(R.drawable.btn_circle, R.drawable.btn_circle_night);
		setContentDesc(R.string.accessibility_2d_view_on);
		if (view.isClickable()) {
			setOnClickListener(tiltMapListener);
		}
		setOnLongClickListener(null);
	}

	private void setRestoreMapTiltState() {
		setIconId(R.drawable.ic_action_2_5d_view_off);
		setIconColorId(R.color.color_myloc_distance);
		setBackground(R.drawable.btn_circle, R.drawable.btn_circle_night);
		setContentDesc(R.string.accessibility_2d_view_off);
		if (view.isClickable()) {
			setOnClickListener(resetMapTiltListener);
		}
		setOnLongClickListener(null);
	}

	private void setReturnToLocationState() {
		setIconId(R.drawable.ic_my_location);
		setIconColorId(0);
		setBackground(R.drawable.btn_circle_blue);
		setContentDesc(R.string.map_widget_back_to_loc);
		if (view.isClickable()) {
			setMyLocationListeners();
		}
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