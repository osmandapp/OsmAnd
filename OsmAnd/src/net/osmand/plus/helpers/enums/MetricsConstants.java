package net.osmand.plus.helpers.enums;

import android.content.Context;

import net.osmand.plus.R;

public enum MetricsConstants {
	KILOMETERS_AND_METERS(R.string.si_km_m, "km-m"),
	MILES_AND_FEET(R.string.si_mi_feet, "mi-f"),
	MILES_AND_METERS(R.string.si_mi_meters, "mi-m"),
	MILES_AND_YARDS(R.string.si_mi_yard, "mi-y"),
	NAUTICAL_MILES(R.string.si_nm, "nm");

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
}