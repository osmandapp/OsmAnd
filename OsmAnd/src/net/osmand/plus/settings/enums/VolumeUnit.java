package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum VolumeUnit {
	LITRES(R.string.litres, R.string.liter_single, R.string.liter),
	IMPERIAL_GALLONS(R.string.imperial_gallons, R.string.imperial_gallon, R.string.us_gallons_unit),
	US_GALLONS(R.string.us_gallons, R.string.us_gallon, R.string.us_gallons_unit);

	private final int key;
	public final int unit;
	public final int singleVariant;

	VolumeUnit(@StringRes int key, @StringRes int singleVariant,  @StringRes int unit) {
		this.key = key;
		this.unit = unit;
		this.singleVariant = singleVariant;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(key);
	}

	@NonNull
	public String toSingleHumanString(@NonNull Context ctx) {
		return ctx.getString(singleVariant);
	}

	@NonNull
	public String getUnitSymbol(@NonNull Context ctx) {
		return ctx.getString(unit);
	}
}