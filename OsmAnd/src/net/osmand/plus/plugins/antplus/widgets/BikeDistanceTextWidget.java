package net.osmand.plus.plugins.antplus.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_DISTANCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeDistanceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeDistanceDevice.BikeDistanceData;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class BikeDistanceTextWidget extends TextInfoWidget {

	private final AntBikeDistanceDevice device;
	private float cachedMeters;

	public BikeDistanceTextWidget(@NonNull MapActivity mapActivity, @NonNull AntBikeDistanceDevice device) {
		super(mapActivity, ANT_BICYCLE_DISTANCE);
		this.device = device;
		updateInfo(null);
		setIcons(ANT_BICYCLE_DISTANCE);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		BikeDistanceData data = device.getLastBikeDistanceData();
		float accumulatedDistance = data != null && device.isConnected() ? (float) data.getAccumulatedDistance() : 0;
		if (isUpdateNeeded() || cachedMeters != accumulatedDistance) {
			cachedMeters = accumulatedDistance;
			if (accumulatedDistance > 0) {
				MetricsConstants metricsConstants = settings.METRIC_SYSTEM.get();
				FormattedValue formattedDistance = OsmAndFormatter.getFormattedDistanceValue(accumulatedDistance,
						app, false, metricsConstants);
				setText(formattedDistance.value, formattedDistance.unit);
			} else {
				setText(NO_VALUE, null);
			}
		}
	}
}
