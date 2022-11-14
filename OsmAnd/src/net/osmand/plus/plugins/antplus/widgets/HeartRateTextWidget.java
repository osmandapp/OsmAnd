package net.osmand.plus.plugins.antplus.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_HEART_RATE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.antplus.antdevices.AntHeartRateDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntHeartRateDevice.HeartRateData;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class HeartRateTextWidget extends TextInfoWidget {

	private final AntHeartRateDevice device;

	public HeartRateTextWidget(@NonNull MapActivity mapActivity, @NonNull AntHeartRateDevice device) {
		super(mapActivity, ANT_HEART_RATE);
		this.device = device;
		updateInfo(null);
		setIcons(ANT_HEART_RATE);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		HeartRateData data = device.getLastHeartRateData();
		int computedHeartRate = data != null && device.isConnected() ? data.getComputedHeartRate() : 0;
		if (computedHeartRate > 0) {
			setText(computedHeartRate + (data.isComputedHeartRateInitial() ? "*" : ""), "bpm");
		} else {
			setText(NO_VALUE, null);
		}
	}
}
