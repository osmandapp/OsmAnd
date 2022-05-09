package net.osmand.plus.views.mapwidgets.widgets;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.util.MapUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.WidgetParams.RADIUS_RULER;

public class RadiusRulerWidget extends TextInfoWidget {

	private static final String DASH = "—";

	public RadiusRulerWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);

		setIcons(settings.SHOW_RADIUS_RULER_ON_MAP.get());
		setText(DASH, null);
		setOnClickListener(v -> switchRadiusRulerMode());
	}

	private void switchRadiusRulerMode() {
		boolean newShowRadiusRuler = !settings.SHOW_RADIUS_RULER_ON_MAP.get();
		setIcons(newShowRadiusRuler);
		settings.SHOW_RADIUS_RULER_ON_MAP.set(newShowRadiusRuler);
		mapActivity.refreshMap();	}

	private void setIcons(boolean showRadiusRuler) {
		if (showRadiusRuler) {
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