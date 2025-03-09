package net.osmand.plus.settings.enums;

import android.content.Context;

import net.osmand.plus.R;

public enum AngularConstants {
	DEGREES(R.string.shared_string_degrees, "°"),
	DEGREES360(R.string.shared_string_degrees, "°"),
	MILLIRADS(R.string.shared_string_milliradians, "mil");

	private final int key;
	private final String unit;

	AngularConstants(int key, String unit) {
		this.key = key;
		this.unit = unit;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	public String getUnitSymbol() {
		return unit;
	}
}