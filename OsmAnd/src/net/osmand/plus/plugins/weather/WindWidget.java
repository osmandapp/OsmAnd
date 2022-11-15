package net.osmand.plus.plugins.weather;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_WIND_WIDGET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class WindWidget extends WeatherWidget {

	public WindWidget(@NonNull MapActivity mapActivity, @NonNull WeatherPlugin weatherPlugin) {
		super(mapActivity, weatherPlugin, WEATHER_WIND_WIDGET, WeatherBand.WEATHER_BAND_WIND_SPEED);
	}

	@Override
	public void onValueObtained(boolean succeeded, double value, @Nullable String formattedValue) {
		app.runInUIThread(() -> {
			if (succeeded && !Algorithms.isEmpty(formattedValue)) {
				setText(formattedValue, "m/s");
			} else {
				setText(NO_VALUE, null);
			}
		});
	}
}
