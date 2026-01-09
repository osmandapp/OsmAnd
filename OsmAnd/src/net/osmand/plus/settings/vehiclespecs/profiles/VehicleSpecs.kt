package net.osmand.plus.settings.vehiclespecs.profiles

import android.content.Context
import net.osmand.plus.base.containers.ThemedIconId
import net.osmand.plus.settings.enums.MeasurementUnits
import net.osmand.plus.settings.preferences.VehicleSpecificationPreference
import net.osmand.plus.settings.vehiclespecs.SpecificationData
import net.osmand.plus.settings.vehiclespecs.SpecificationType
import net.osmand.plus.settings.vehiclespecs.containers.Assets
import net.osmand.util.Algorithms

abstract class VehicleSpecs(
    private val specsMap: Map<SpecificationType, SpecificationData>
) {

    companion object {
        @JvmStatic
        protected fun createSpecification(
            iconDayId: Int,
            iconNightId: Int,
            descriptionId: Int,
            metricUnits: MeasurementUnits,
            metricValues: List<Float>,
            imperialUnits: MeasurementUnits,
            imperialValues: List<Float>
        ): SpecificationData {
            val themedIconId = ThemedIconId(iconDayId, iconNightId)
            val assets = Assets(themedIconId, descriptionId)
            return SpecificationData(assets, metricUnits, metricValues, imperialUnits, imperialValues)
        }
    }

    fun getIconId(type: SpecificationType, nightMode: Boolean): Int {
        return getSpecsData(type).assets.getIconId(nightMode)
    }

    fun getDescriptionId(type: SpecificationType): Int {
        return getSpecsData(type).assets.descriptionId
    }

    fun getPredefinedValues(type: SpecificationType, useMetricSystem: Boolean): List<Float> {
        return getSpecsData(type).getPredefinedValues(useMetricSystem)
    }

    fun getMeasurementUnits(type: SpecificationType, useMetricSystem: Boolean): MeasurementUnits {
        return getSpecsData(type).getDisplayUnits(useMetricSystem)
    }

    fun getSpecsData(type: SpecificationType): SpecificationData {
        return specsMap[type] ?: throw NoSuchElementException("Specification data for $type not found")
    }

    open fun checkValue(
        context: Context,
        type: SpecificationType,
        useMetricSystem: Boolean,
        value: Float
    ): String {
        return ""
    }

    fun readSavedValue(preference: VehicleSpecificationPreference): Float {
        var value = Algorithms.parseDoubleSilently(preference.value, 0.0).toFloat()
        if (value != 0.0f) {
            // Convert value to display units system
            val units = getMeasurementUnits(preference.specificationType, preference.isUseMetricSystem)
            return units.fromBase(value)
        }
        return value
    }

    fun prepareValueToSave(preference: VehicleSpecificationPreference, v: Float): Float {
        var value = v
        if (value != 0.0f) {
            // Convert value to default units system (meters for length, tones for weight)
            val units = getMeasurementUnits(preference.specificationType, preference.isUseMetricSystem)
            value = units.toBase(value)
        }
        return value
    }
}