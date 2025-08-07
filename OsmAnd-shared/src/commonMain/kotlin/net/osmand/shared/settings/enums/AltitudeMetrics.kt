package net.osmand.shared.settings.enums

import net.osmand.shared.util.Localization

enum class AltitudeMetrics(private val nameKey: String) {
    METERS("shared_string_meters"),
    FEET("shared_string_feet");

    fun toHumanString() = Localization.getString(nameKey)

    fun shouldUseFeet(): Boolean {
        return this == FEET
    }

    companion object {
        fun fromMetricsConstant(mc: MetricsConstants): AltitudeMetrics {
            return when (mc) {
                MetricsConstants.KILOMETERS_AND_METERS,
                MetricsConstants.NAUTICAL_MILES_AND_METERS,
                MetricsConstants.MILES_AND_METERS -> METERS
                else -> FEET
            }
        }
    }
}

