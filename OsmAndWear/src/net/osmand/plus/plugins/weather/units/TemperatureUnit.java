package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public enum TemperatureUnit implements WeatherUnit {

	CELSIUS(R.string.weather_temperature_celsius, "°C"),
	FAHRENHEIT(R.string.weather_temperature_fahrenheit, "°F");

	@StringRes
	private final int titleId;
	private final String symbol;

	TemperatureUnit(@StringRes int titleId, @NonNull String symbol) {
		this.titleId = titleId;
		this.symbol = symbol;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	@Override
	public String getUnit(@NonNull OsmandApplication app) {
		return symbol;
	}

	@NonNull
	@Override
	public String getSymbol() {
		return symbol;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context ctx) {
		String title = ctx.getString(titleId);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, title, "(" + symbol + ")");
	}
}
