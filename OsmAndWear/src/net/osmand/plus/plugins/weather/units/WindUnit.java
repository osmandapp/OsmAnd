package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public enum WindUnit implements WeatherUnit {

	METERS_PER_SECOND(R.string.weather_wind_meters_per_second, R.string.weather_wind_ms, "m/s"),
	KILOMETERS_PER_HOUR(R.string.weather_wind_kilimeters_per_hour, R.string.weather_wind_kmh, "km/h"),
	MILES_PER_HOUR(R.string.weather_wind_miles_per_hour, R.string.weather_wind_mph, "mph"),
	KNOTS(R.string.weather_wind_knots, R.string.weather_wind_kn, "kn");

	@StringRes
	private final int titleId;
	@StringRes
	private final int unitId;
	private final String symbol;

	WindUnit(@StringRes int titleId, @StringRes int unitId, @NonNull String symbol) {
		this.titleId = titleId;
		this.unitId = unitId;
		this.symbol = symbol;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@StringRes
	public int getUnitId() {
		return unitId;
	}

	@NonNull
	@Override
	public String getUnit(@NonNull OsmandApplication app) {
		return app.getString(unitId);
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
		String unit = ctx.getString(unitId);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, title, "(" + unit + ")");
	}
}
