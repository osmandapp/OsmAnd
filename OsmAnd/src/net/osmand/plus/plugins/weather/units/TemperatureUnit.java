package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.shared.units.TemperatureUnits;

public enum TemperatureUnit implements WeatherUnit {

	CELSIUS, FAHRENHEIT;

	@NonNull
	@Override
	public String getUnit(@NonNull OsmandApplication app) {
		return toMeasurementUnits().getSymbol();
	}

	@NonNull
	@Override
	public String getSymbol() {
		return toMeasurementUnits().getSymbol();
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context ctx) {
		TemperatureUnits units = toMeasurementUnits();
		String title = units.getName();
		String symbol = units.getSymbol();
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, title, "(" + symbol + ")");
	}

	@NonNull
	public TemperatureUnits toMeasurementUnits() {
		return switch (this) {
			case CELSIUS -> TemperatureUnits.CELSIUS;
			case FAHRENHEIT -> TemperatureUnits.FAHRENHEIT;
		};
	}
}
