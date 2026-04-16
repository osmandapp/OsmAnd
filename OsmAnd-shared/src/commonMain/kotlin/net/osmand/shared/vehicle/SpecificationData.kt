package net.osmand.shared.vehicle

import net.osmand.shared.units.MeasurementUnit

data class SpecificationData(
    val iconDayResName: String,
    val iconNightResName: String,
    val descriptionResName: String,
    val metricUnits: MeasurementUnit<*>,
    val metricValues: List<Float>,
    val imperialUnits: MeasurementUnit<*>,
    val imperialValues: List<Float>
) {
    fun getDisplayUnits(isMetric: Boolean) = if (isMetric) metricUnits else imperialUnits

    fun getPredefinedValues(isMetric: Boolean) = if (isMetric) metricValues else imperialValues
}