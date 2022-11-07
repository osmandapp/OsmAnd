package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum WindConstants {

	METERS_PER_SECOND(R.string.weather_wind_meters_per_second, R.string.weather_wind_ms),
	KILOMETERS_PER_HOUR(R.string.weather_wind_kilimeters_per_hour, R.string.weather_wind_kmh),
	MILES_PER_HOUR(R.string.weather_wind_miles_per_hour, R.string.weather_wind_mph),
	KNOTS(R.string.weather_wind_knots, R.string.weather_wind_kt);

	@StringRes
	private final int titleId;
	@StringRes
	private final int unitId;

	WindConstants(@StringRes int titleId, @StringRes int unitId) {
		this.titleId = titleId;
		this.unitId = unitId;
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
	public String toHumanString(@NonNull Context ctx) {
		String title = ctx.getString(titleId);
		String unit = ctx.getString(unitId);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, title, "(" + unit + ")");
	}

}
