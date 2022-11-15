package net.osmand.plus.plugins.weather;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_TEMPERATURE_WIDGET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class TemperatureWidget extends WeatherWidget {

	public TemperatureWidget(@NonNull MapActivity mapActivity, @NonNull WeatherPlugin weatherPlugin) {
		super(mapActivity, weatherPlugin, WEATHER_TEMPERATURE_WIDGET, WeatherBand.WEATHER_BAND_TEMPERATURE);
	}

	@Override
	public void onValueObtained(boolean succeeded, double value, @Nullable String formattedValue) {
		app.runInUIThread(() -> {
			if (succeeded && !Algorithms.isEmpty(formattedValue)) {
				setText(formattedValue + " °C", null);
			} else {
				setText(NO_VALUE, null);
			}
		});
	}
}
