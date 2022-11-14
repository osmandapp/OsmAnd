package net.osmand.plus.plugins.antplus.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_SPEED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeSpeedDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeSpeedDevice.BikeSpeedData;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class BikeSpeedTextWidget extends TextInfoWidget {

	private final AntBikeSpeedDevice device;
	private float cachedSpeed;

	public BikeSpeedTextWidget(@NonNull MapActivity mapActivity, @NonNull AntBikeSpeedDevice device) {
		super(mapActivity, ANT_BICYCLE_SPEED);
		this.device = device;
		updateInfo(null);
		setIcons(ANT_BICYCLE_SPEED);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		BikeSpeedData data = device.getLastBikeSpeedData();
		float calculatedSpeed = data != null && device.isConnected() ? (float) data.getCalculatedSpeed() : 0;
		if (isUpdateNeeded() || cachedSpeed != calculatedSpeed) {
			cachedSpeed = calculatedSpeed;
			if (calculatedSpeed > 0) {
				FormattedValue formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(calculatedSpeed, app);
				setText(formattedSpeed.value, formattedSpeed.unit);
			} else {
				setText(NO_VALUE, null);
			}
		}
	}
}
