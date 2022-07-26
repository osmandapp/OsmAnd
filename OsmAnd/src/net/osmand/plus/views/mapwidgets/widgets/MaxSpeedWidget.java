package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.MAX_SPEED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.RouteDataObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

public class MaxSpeedWidget extends TextInfoWidget {

	private final MapViewTrackingUtilities mapViewTrackingUtilities;

	private float cachedMaxSpeed;

	public MaxSpeedWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, MAX_SPEED);
		mapViewTrackingUtilities = app.getMapViewTrackingUtilities();

		setIcons(MAX_SPEED);
		setText(null, null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		float maxSpeed = getMaxSpeed();
		if (isUpdateNeeded() || cachedMaxSpeed != maxSpeed) {
			cachedMaxSpeed = maxSpeed;
			if (cachedMaxSpeed == 0) {
				setText(null, null);
			} else if (cachedMaxSpeed == RouteDataObject.NONE_MAX_SPEED) {
				setText(getString(R.string.max_speed_none), "");
			} else {
				FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedMaxSpeed, app);
				setText(formattedSpeed.value, formattedSpeed.unit);
			}
		}
	}

	private float getMaxSpeed() {
		if ((!routingHelper.isFollowingMode()
				|| routingHelper.isDeviatedFromRoute()
				|| (routingHelper.getCurrentGPXRoute() != null && !routingHelper.isCurrentGPXRouteV2()))
				&& mapViewTrackingUtilities.isMapLinkedToLocation()) {
			RouteDataObject routeObject = locationProvider.getLastKnownRouteSegment();
			if (routeObject != null) {
				boolean direction = routeObject.bearingVsRouteDirection(locationProvider.getLastKnownLocation());
				return routeObject.getMaximumSpeed(direction);
			}
		} else {
			return routingHelper.getCurrentMaxSpeed();
		}

		return 0;
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}