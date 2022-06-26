package net.osmand.plus.views.controls.maphudbuttons;

import android.Manifest;
import android.view.View;
import android.widget.ImageView;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.ContextMenuLayer;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MyLocationButton extends MapButton {

	public MyLocationButton(@NonNull MapActivity mapActivity, @NonNull ImageView view, @NonNull String id, boolean contextMenuAllowed) {
		super(mapActivity, view, id);
		setIconId(R.drawable.ic_my_location);
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setBackground(R.drawable.btn_circle_blue);
		setOnClickListener(v -> moveBackToLocation(false));
		if (contextMenuAllowed) {
			setOnLongClickListener(v -> moveBackToLocation(true));
		}
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
		Location lastKnownLocation = app.getLocationProvider().getLastKnownLocation();
		boolean enabled = lastKnownLocation != null;
		boolean tracked = app.getMapViewTrackingUtilities().isMapLinkedToLocation();

		if (!enabled) {
			setBackground(R.drawable.btn_circle, R.drawable.btn_circle_night);
			setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
			setContentDesc(R.string.unknown_location);
		} else if (tracked) {
			setBackground(R.drawable.btn_circle, R.drawable.btn_circle_night);
			setIconColorId(R.color.color_myloc_distance);
			setContentDesc(R.string.access_map_linked_to_location);
		} else {
			setIconColorId(0);
			setBackground(R.drawable.btn_circle_blue);
			setContentDesc(R.string.map_widget_back_to_loc);
		}
		if (app.accessibilityEnabled()) {
			boolean visible = view.getVisibility() == View.VISIBLE;
			view.setClickable(enabled && visible);
		}
	}

	@Override
	protected boolean shouldShow() {
		return !isRouteDialogOpened() && widgetsVisibilityHelper.shouldShowBackToLocationButton();
	}
}