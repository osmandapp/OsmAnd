package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;

public enum TemperatureUnitMode {

	SYSTEM_DEFAULT(R.string.system_default_theme, null),
	CELSIUS(R.string.weather_temperature_celsius, TemperatureUnit.CELSIUS),
	FAHRENHEIT(R.string.weather_temperature_fahrenheit, TemperatureUnit.FAHRENHEIT);

	@StringRes
	private final int titleId;
	private final TemperatureUnit unit;

	TemperatureUnitMode(@StringRes int titleId, @Nullable TemperatureUnit unit) {
		this.titleId = titleId;
		this.unit = unit;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return unit != null ? unit.toHumanString(ctx) : ctx.getString(titleId);
	}
}
