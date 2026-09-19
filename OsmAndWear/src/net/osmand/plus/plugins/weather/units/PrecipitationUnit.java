package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public enum PrecipitationUnit implements WeatherUnit {

	MILIMETERS(R.string.weather_precip_milimeters, R.string.weather_precip_mm, "mm"),
	INCHES(R.string.weather_precip_inches, R.string.weather_precip_in, "in");

	@StringRes
	private final int titleId;
	@StringRes
	private final int unitId;
	private final String symbol;

	PrecipitationUnit(@StringRes int titleId, @StringRes int unitId, @NonNull String symbol) {
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
