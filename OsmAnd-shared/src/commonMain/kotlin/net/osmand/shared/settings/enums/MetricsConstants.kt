package net.osmand.shared.settings.enums

import net.osmand.shared.util.Localization

enum class MetricsConstants(private val key: String, private val ttsString: String) {
	KILOMETERS_AND_METERS("si_km_m", "km-m"),
	MILES_AND_FEET("si_mi_feet", "mi-f"),
	MILES_AND_METERS("si_mi_meters", "mi-m"),
	MILES_AND_YARDS("si_mi_yard", "mi-y"),
	NAUTICAL_MILES_AND_METERS("si_nm_mt", "nm-m"),
	NAUTICAL_MILES_AND_FEET("si_nm_ft", "nm-f");

	fun toHumanString() = Localization.getString(key)

	fun toTTSString() = ttsString

	fun shouldUseFeet(): Boolean {
		return this == MILES_AND_FEET || (this == MILES_AND_YARDS) || (this == NAUTICAL_MILES_AND_FEET)
	}
}