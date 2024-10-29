package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public enum VolumeUnit {
	LITRES(R.string.litres, R.string.liter),
	IMPERIAL_GALLONS(R.string.imperial_gallons, R.string.imperial_gallons_unit),
	US_GALLONS(R.string.us_gallons, R.string.us_gallons_unit);

	private final int key;
	private final int unit;

	VolumeUnit(int key, int unit) {
		this.key = key;
		this.unit = unit;
	}

	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(key);
	}

	public String getUnitSymbol(@NonNull Context ctx) {
		return ctx.getString(unit);
	}
}