package net.osmand.shared.settings.enums

import net.osmand.shared.units.LengthUnits
import net.osmand.shared.util.Localization

enum class AltitudeMetrics(private val nameKey: String) {
    METERS("shared_string_meters"),
    FEET("shared_string_feet");

    fun toHumanString() = Localization.getString(nameKey)

    fun shouldUseFeet() = this == FEET

    fun getUnits() = if (shouldUseFeet()) LengthUnits.FEET else LengthUnits.METERS

    companion object {
        fun fromMetricsConstant(mc: MetricsConstants) = mc.getAltitudeMetrics()
    }
}

