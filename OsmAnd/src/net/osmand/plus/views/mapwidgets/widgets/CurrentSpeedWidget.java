package net.osmand.plus.views.mapwidgets.widgets;

import net.osmand.Location;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CurrentSpeedWidget extends TextInfoWidget {

	private static final float LOW_SPEED_THRESHOLD_MPS = 6;
	private static final float UPDATE_THRESHOLD_MPS = .1f;
	private static final float LOW_SPEED_UPDATE_THRESHOLD_MPS = .015f; // Update more often while walking/running

	private float cachedSpeed = 0;

	public CurrentSpeedWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		setIcons(WidgetParams.CURRENT_SPEED);
		setText(null, null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		Location location = locationProvider.getLastKnownLocation();
		if (location != null && location.hasSpeed()) {
			float updateThreshold = cachedSpeed < LOW_SPEED_THRESHOLD_MPS
					? LOW_SPEED_UPDATE_THRESHOLD_MPS
					: UPDATE_THRESHOLD_MPS;
			if (isUpdateNeeded() || Math.abs(location.getSpeed() - cachedSpeed) > updateThreshold) {
				cachedSpeed = location.getSpeed();
				FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(cachedSpeed, app);
				setText(formattedSpeed.value, formattedSpeed.unit);
			}
		} else if (cachedSpeed != 0) {
			cachedSpeed = 0;
			setText(null, null);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}