package net.osmand.plus.views.mapwidgets.widgets;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.RadiusRulerControlLayer.RadiusRulerMode;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.util.MapUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.WidgetParams.RADIUS_RULER;

public class RadiusRulerWidget extends TextInfoWidget {

	private static final String DASH = "—";

	public RadiusRulerWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);

		setIcons(settings.RADIUS_RULER_MODE.get());
		setText(DASH, null);
		setOnClickListener(v -> switchRadiusRulerMode());
	}

	private void switchRadiusRulerMode() {
		RadiusRulerMode currentMode = settings.RADIUS_RULER_MODE.get();
		RadiusRulerMode newMode = RadiusRulerMode.FIRST;
		if (currentMode == RadiusRulerMode.FIRST) {
			newMode = RadiusRulerMode.SECOND;
		} else if (currentMode == RadiusRulerMode.SECOND) {
			newMode = RadiusRulerMode.EMPTY;
		}
		setIcons(newMode);
		settings.RADIUS_RULER_MODE.set(newMode);
		mapActivity.refreshMap();
	}

	private void setIcons(@Nullable RadiusRulerMode mode) {
		if (mode == RadiusRulerMode.FIRST || mode == RadiusRulerMode.SECOND) {
			setIcons(RADIUS_RULER);
		} else {
			setIcons(R.drawable.widget_hidden_day, R.drawable.widget_hidden_night);
		}
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		Location currentLocation = locationProvider.getLastKnownLocation();
		LatLon centerLocation = mapActivity.getMapLocation();

		if (currentLocation != null && centerLocation != null) {
			if (mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
				setDistanceText(0);
			} else {
				double currentLat = currentLocation.getLatitude();
				double currentLon = currentLocation.getLongitude();
				float distance = ((float) MapUtils.getDistance(centerLocation, currentLat, currentLon));
				setDistanceText(distance);
			}
		} else {
			setText(DASH, null);
		}
	}

	private void setDistanceText(float dist) {
		FormattedValue formattedDistance = OsmAndFormatter
				.getFormattedDistanceValue(dist, app, true, settings.METRIC_SYSTEM.get());
		setText(formattedDistance.value, formattedDistance.unit);
	}
}