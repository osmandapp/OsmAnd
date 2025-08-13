package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;

public enum TemperatureUnitsMode {

	SYSTEM_DEFAULT(R.string.system_default_theme, null),
	CELSIUS(R.string.weather_temperature_celsius, TemperatureUnit.CELSIUS),
	FAHRENHEIT(R.string.weather_temperature_fahrenheit, TemperatureUnit.FAHRENHEIT);

	@StringRes
	private final int titleId;
	private final TemperatureUnit temperatureUnit;

	TemperatureUnitsMode(@StringRes int titleId, @Nullable TemperatureUnit temperatureUnit) {
		this.titleId = titleId;
		this.temperatureUnit = temperatureUnit;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@Nullable
	public TemperatureUnit getTemperatureUnit() {
		return temperatureUnit;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return temperatureUnit != null ? temperatureUnit.toHumanString(ctx) : ctx.getString(titleId);
	}
}
