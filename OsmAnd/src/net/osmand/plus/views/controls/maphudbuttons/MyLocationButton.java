package net.osmand.plus.views.controls.maphudbuttons;

import static android.graphics.drawable.GradientDrawable.RECTANGLE;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MyLocationButtonState;

public class MyLocationButton extends MapButton {

	private final MyLocationButtonState buttonState;

	public MyLocationButton(@NonNull Context context) {
		this(context, null);
	}

	public MyLocationButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyLocationButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getMyLocationButtonState();

		setOnClickListener(v -> moveBackToLocation(false));
		setOnLongClickListener(v -> moveBackToLocation(true));
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	public void update() {
		super.update();

		if (app.accessibilityEnabled()) {
			boolean visible = getVisibility() == View.VISIBLE;
			boolean hasLocation = app.getLocationProvider().getLastKnownLocation() != null;
			setClickable(hasLocation && visible);
		}
		if (app.getLocationProvider().getLastKnownLocation() == null) {
			setContentDescription(app.getString(R.string.unknown_location));
		} else if (app.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
			setContentDescription(app.getString(R.string.access_map_linked_to_location));
		} else {
			setContentDescription(app.getString(R.string.map_widget_back_to_loc));
		}
	}

	@Override
	protected void updateColors(boolean nightMode) {
		Context context = getContext();
		if (app.getLocationProvider().getLastKnownLocation() == null) {
			setIconColor(ColorUtilities.getMapButtonIconColor(context, nightMode));
			setBackgroundColors(ColorUtilities.getColor(context, nightMode ? R.color.map_widget_dark : R.color.map_widget_light),
					ColorUtilities.getMapButtonBackgroundPressedColor(context, nightMode));
		} else if (app.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
			setIconColor(ColorUtilities.getColor(context, R.color.color_myloc_distance));
			setBackgroundColors(ColorUtilities.getColor(app, nightMode ? R.color.map_widget_dark : R.color.map_widget_light),
					ColorUtilities.getMapButtonBackgroundPressedColor(context, nightMode));
		} else {
			setIconColor(0);
			setBackgroundColors(ColorUtilities.getColor(context, R.color.map_widget_blue),
					ColorUtilities.getColor(context, R.color.map_widget_blue_pressed));
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
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
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
	protected boolean shouldShow() {
		return !routeDialogOpened && visibilityHelper.shouldShowBackToLocationButton();
	}

	protected void updateBackground() {
		Context context = getContext();
		int size = AndroidUtils.dpToPx(context, appearanceParams.getSize());
		int cornerRadius = AndroidUtils.dpToPx(context, appearanceParams.getCornerRadius());

		GradientDrawable normal = new GradientDrawable();
		normal.setSize(size, size);
		normal.setShape(RECTANGLE);
		normal.setColor(ColorUtilities.getColorWithAlpha(backgroundColor, appearanceParams.getOpacity()));
		normal.setCornerRadius(cornerRadius);

		GradientDrawable pressed = new GradientDrawable();
		pressed.setSize(size, size);
		pressed.setShape(RECTANGLE);
		pressed.setColor(backgroundPressedColor);
		pressed.setCornerRadius(cornerRadius);

		imageView.setBackground(AndroidUtils.createPressedStateListDrawable(normal, pressed));
	}
}