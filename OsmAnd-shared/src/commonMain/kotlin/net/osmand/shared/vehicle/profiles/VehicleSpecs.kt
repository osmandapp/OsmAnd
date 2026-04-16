package net.osmand.shared.vehicle.profiles

import net.osmand.shared.vehicle.SpecificationData
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization
import net.osmand.shared.vehicle.SpecificationType
import kotlin.jvm.JvmStatic

abstract class VehicleSpecs(
    private val specsMap: Map<SpecificationType, SpecificationData>
) {

    companion object {
        @JvmStatic
        protected fun createSpecification(
            iconDayResName: String,
            iconNightResName: String,
            descriptionResName: String,
            metricUnits: MeasurementUnit<*>,
            metricValues: List<Float>,
            imperialUnits: MeasurementUnit<*>,
            imperialValues: List<Float>
        ): SpecificationData {
            return SpecificationData(iconDayResName, iconNightResName, descriptionResName,
                metricUnits, metricValues, imperialUnits, imperialValues)
        }
    }

    fun getIconName(type: SpecificationType, nightMode: Boolean): String {
        val data = getSpecsData(type)
        return if (nightMode) data.iconNightResName else data.iconDayResName
    }

    fun getDescription(type: SpecificationType): String {
        val data = getSpecsData(type)
        return Localization.getString(data.descriptionResName)
    }

    fun getPredefinedValues(type: SpecificationType, useMetricSystem: Boolean): List<Float> {
        return getSpecsData(type).getPredefinedValues(useMetricSystem)
    }

    fun getMeasurementUnits(type: SpecificationType, useMetricSystem: Boolean): MeasurementUnit<*> {
        return getSpecsData(type).getDisplayUnits(useMetricSystem)
    }

    fun getSpecsData(type: SpecificationType): SpecificationData {
        return specsMap[type] ?: throw NoSuchElementException("Specification data for $type not found")
    }

    open fun checkValue(
        type: SpecificationType,
        useMetricSystem: Boolean,
        value: Float
    ): String {
        return ""
    }

    fun readSavedValue(
        valueStr: String,
        specificationType: SpecificationType,
        useMetricSystem: Boolean
    ): Double {
        val value = KAlgorithms.parseDoubleSilently(valueStr, 0.0)
        if (value != 0.0) {
            // Convert value to display units system
            val units = getMeasurementUnits(specificationType, useMetricSystem)
            return units.fromBase(value)
        }
        return value
    }

    fun prepareValueToSave(
        specificationType: SpecificationType,
        useMetricSystem: Boolean,
        v: Double
    ): Double {
        var value = v
        if (value != 0.0) {
            // Convert value to default units system (meters for length, tones for weight)
            val units = getMeasurementUnits(specificationType, useMetricSystem)
            value = units.toBase(value)
        }
        return value
    }
}