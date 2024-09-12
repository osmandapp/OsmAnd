package net.osmand.shared.settings.enums

import net.osmand.shared.util.Localization

enum class SpeedConstants(val key: String, val descr: String, val imperial: Boolean) {
	KILOMETERS_PER_HOUR("km_h", "si_kmh", false),
	MILES_PER_HOUR("mile_per_hour", "si_mph", true),
	METERS_PER_SECOND("m_s", "si_m_s", false),
	MINUTES_PER_MILE("min_mile", "si_min_m", true),
	MINUTES_PER_KILOMETER("min_km", "si_min_km", false),
	NAUTICALMILES_PER_HOUR("nm_h", "si_nm_h", true);

	fun toHumanString(): String {
		return Localization.getString(descr)
	}

	fun toShortString(): String {
		return Localization.getString(key)
	}
}