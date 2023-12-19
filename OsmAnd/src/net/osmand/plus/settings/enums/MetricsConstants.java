package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum MetricsConstants {
	KILOMETERS_AND_METERS(R.string.si_km_m, "km-m"),
	MILES_AND_FEET(R.string.si_mi_feet, "mi-f"),
	MILES_AND_METERS(R.string.si_mi_meters, "mi-m"),
	MILES_AND_YARDS(R.string.si_mi_yard, "mi-y"),
	NAUTICAL_MILES_AND_METERS(R.string.si_nm_mt, "nm-m"),
	NAUTICAL_MILES_AND_FEET(R.string.si_nm_ft, "nm-f");

	private final int key;
	private final String ttsString;

	MetricsConstants(int key, String ttsString) {
		this.key = key;
		this.ttsString = ttsString;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	public String toTTSString() {
		return ttsString;
	}

	public boolean shouldUseFeet() {
		return this == MILES_AND_FEET || this == MILES_AND_YARDS || this == NAUTICAL_MILES_AND_FEET;
	}

	@StringRes
	public int getBaseUnit() {
		if (this == KILOMETERS_AND_METERS || this == MILES_AND_METERS) {
			return R.string.m;
		} else if (this == MILES_AND_FEET || this == NAUTICAL_MILES_AND_FEET) {
			return R.string.foot;
		} else if (this == MILES_AND_YARDS) {
			return R.string.yard;
		}
		return R.string.m;
	}

	@StringRes
	public int getBigUnit() {
		if (this == KILOMETERS_AND_METERS) {
			return R.string.km;
		} else if (this == NAUTICAL_MILES_AND_METERS || this == NAUTICAL_MILES_AND_FEET) {
			return R.string.nm;
		} else {
			return R.string.mile;
		}
	}
}