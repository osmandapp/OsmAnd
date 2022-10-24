package net.osmand.plus.plugins.weather;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_CLOUDS_WIDGET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.WeatherBand;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class CloudsWidget extends WeatherWidget {

	public CloudsWidget(@NonNull MapActivity mapActivity, @NonNull WeatherPlugin weatherPlugin) {
		super(mapActivity, weatherPlugin, WEATHER_CLOUDS_WIDGET, (short) WeatherBand.Cloud.swigValue());
	}

	@Override
	public void onValueObtained(boolean succeeded, double value, @Nullable String formattedValue) {
		getMyApplication().runInUIThread(() -> {
			if (succeeded && !Algorithms.isEmpty(formattedValue)) {
				setText(formattedValue + " %", null);
			} else {
				setText(null, null);
			}
		});
	}
}
