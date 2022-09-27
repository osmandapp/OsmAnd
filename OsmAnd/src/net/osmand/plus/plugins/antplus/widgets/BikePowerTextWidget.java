package net.osmand.plus.plugins.antplus.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_POWER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.antplus.antdevices.AntBikePowerDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikePowerDevice.BikePowerData;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class BikePowerTextWidget extends TextInfoWidget {

	private final AntBikePowerDevice device;
	private double cachedPower;

	public BikePowerTextWidget(@NonNull MapActivity mapActivity, @NonNull AntBikePowerDevice device) {
		super(mapActivity, ANT_BICYCLE_POWER);
		this.device = device;
		updateInfo(null);
		setIcons(ANT_BICYCLE_POWER);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		BikePowerData data = device.getLastBikePowerData();
		double calculatedPower = data != null && device.isConnected() ? data.getCalculatedPower() : 0;
		if (isUpdateNeeded() || cachedPower != calculatedPower) {
			cachedPower = calculatedPower;
			if (calculatedPower > 0) {
				setText(String.valueOf(calculatedPower), "W");
			} else {
				setText(null, null);
			}
		}
	}
}
