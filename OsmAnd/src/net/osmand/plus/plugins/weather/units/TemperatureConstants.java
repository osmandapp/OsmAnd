package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum TemperatureConstants {

	CELSIUS(R.string.weather_temperature_celsius, "°C"),
	FAHRENHEIT(R.string.weather_temperature_fahrenheit, "°F");

	@StringRes
	private final int titleId;
	private final String unit;

	TemperatureConstants(@StringRes int titleId, @NonNull String unit) {
		this.titleId = titleId;
		this.unit = unit;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public String getUnit() {
		return unit;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		String title = ctx.getString(titleId);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, title, "(" + unit + ")");
	}

}
