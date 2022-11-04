package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum PressureConstants {

	HECTOPASCALS(R.string.weather_pressure_hectopascals, R.string.weather_pressure_hpa),
	MILLIMETERS_OF_MERCURY(R.string.weather_pressure_millimeters_of_mercury, R.string.weather_pressure_mmhg),
	INCHES_OF_MERCURY(R.string.weather_pressure_inches_of_mercury, R.string.weather_pressure_inhg);

	@StringRes
	private final int titleId;
	@StringRes
	private final int unitId;

	PressureConstants(@StringRes int titleId, @StringRes int unitId) {
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
