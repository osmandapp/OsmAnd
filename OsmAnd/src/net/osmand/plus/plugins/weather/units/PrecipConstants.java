package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum PrecipConstants {

	MILIMETERS(R.string.weather_precip_milimeters, R.string.weather_precip_mm),
	INCHES(R.string.weather_precip_inches, R.string.weather_precip_in);

	@StringRes
	private final int titleId;
	@StringRes
	private final int unitId;

	PrecipConstants(@StringRes int titleId, @StringRes int unitId) {
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
