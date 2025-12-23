package net.osmand.plus.settings.vehiclespecs

import net.osmand.plus.settings.enums.MeasurementUnits
import net.osmand.plus.settings.vehiclespecs.containers.Assets

data class SpecificationData(
    val assets: Assets,
    val metricUnits: MeasurementUnits,
    val metricValues: List<Float>,
    val imperialUnits: MeasurementUnits,
    val imperialValues: List<Float>
) {
    fun getDisplayUnits(isMetric: Boolean) = if (isMetric) metricUnits else imperialUnits

    fun getPredefinedValues(isMetric: Boolean) = if (isMetric) metricValues else imperialValues
}