package net.osmand.plus.plugins.weather;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_AIR_PRESSURE_WIDGET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class AirPressureWidget extends WeatherWidget {

	public AirPressureWidget(@NonNull MapActivity mapActivity, @NonNull WeatherPlugin weatherPlugin) {
		super(mapActivity, weatherPlugin, WEATHER_AIR_PRESSURE_WIDGET, WeatherBand.WEATHER_BAND_PRESSURE);
	}

	@Override
	public void onValueObtained(boolean succeeded, double value, @Nullable String formattedValue) {
		app.runInUIThread(() -> {
			if (succeeded && !Algorithms.isEmpty(formattedValue)) {
				setText(formattedValue, "mmHg");
			} else {
				setText(NO_VALUE, null);
			}
		});
	}
}
