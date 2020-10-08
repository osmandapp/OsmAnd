package net.osmand.plus.helpers.enums;

import android.content.Context;

import net.osmand.plus.R;

public enum SpeedConstants {
	KILOMETERS_PER_HOUR(R.string.km_h, R.string.si_kmh, false),
	MILES_PER_HOUR(R.string.mile_per_hour, R.string.si_mph, true),
	METERS_PER_SECOND(R.string.m_s, R.string.si_m_s, false),
	MINUTES_PER_MILE(R.string.min_mile, R.string.si_min_m, true),
	MINUTES_PER_KILOMETER(R.string.min_km, R.string.si_min_km, false),
	NAUTICALMILES_PER_HOUR(R.string.nm_h, R.string.si_nm_h, true);

	public final int key;
	public final int descr;
	public final boolean imperial;

	SpeedConstants(int key, int descr, boolean imperial) {
		this.key = key;
		this.descr = descr;
		this.imperial = imperial;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(descr);
	}

	public String toShortString(Context ctx) {
		return ctx.getString(key);
	}
}