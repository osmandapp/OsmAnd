package net.osmand.plus.plugins.antplus.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_CADENCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeCadenceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeCadenceDevice.BikeCadenceData;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class BikeCadenceTextWidget extends TextInfoWidget {

	private final AntBikeCadenceDevice device;
	private int cachedCadence;

	public BikeCadenceTextWidget(@NonNull MapActivity mapActivity, @NonNull AntBikeCadenceDevice device) {
		super(mapActivity, ANT_BICYCLE_CADENCE);
		this.device = device;
		updateInfo(null);
		setIcons(ANT_BICYCLE_CADENCE);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		BikeCadenceData data = device.getLastBikeCadenceData();
		int calculatedCadence = data != null && device.isConnected() ? data.getCalculatedCadence() : 0;
		if (isUpdateNeeded() || cachedCadence != calculatedCadence) {
			cachedCadence = calculatedCadence;
			if (calculatedCadence > 0) {
				setText(String.valueOf(calculatedCadence), "rpm");
			} else {
				setText(null, null);
			}
		}
	}
}
