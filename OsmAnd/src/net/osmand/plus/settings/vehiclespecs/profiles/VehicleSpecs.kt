package net.osmand.plus.settings.vehiclespecs.profiles

import android.content.Context
import net.osmand.plus.base.containers.ThemedIconId
import net.osmand.plus.settings.enums.MeasurementUnits
import net.osmand.plus.settings.preferences.VehicleSpecificationPreference
import net.osmand.plus.settings.vehiclespecs.SpecificationData
import net.osmand.plus.settings.vehiclespecs.SpecificationType
import net.osmand.plus.settings.vehiclespecs.containers.Assets
import net.osmand.util.Algorithms

abstract class VehicleSpecs {

    private val specsMap = mutableMapOf<SpecificationType, SpecificationData>()

    init {
        collectSpecs()
    }

    protected abstract fun collectSpecs()

    protected fun add(
        type: SpecificationType,
        iconDayId: Int,
        iconNightId: Int,
        descriptionId: Int,
        metricUnits: MeasurementUnits,
        metricValues: List<Float>,
        imperialUnits: MeasurementUnits,
        imperialValues: List<Float>
    ) {
        val themedIconId = ThemedIconId(iconDayId, iconNightId)
        val assets = Assets(themedIconId, descriptionId)
        specsMap[type] = SpecificationData(assets, metricUnits, metricValues, imperialUnits, imperialValues)
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
        ctx: Context,
        type: SpecificationType,
        useMetricSystem: Boolean,
        value: Float,
        error: StringBuilder
    ): Boolean {
        return true
    }

    fun readSavedValue(preference: VehicleSpecificationPreference): Float {
        var value = Algorithms.parseDoubleSilently(preference.value, 0.0).toFloat()
        if (value != 0.0f) {
            value += ROUTING_LIMIT_OFFSET
            // Convert value to display units system
            val units = getMeasurementUnits(preference.specificationType, preference.isUseMetricSystem)
            return units.fromBase(value)
        }
        return value
    }

    fun prepareValueToSave(preference: VehicleSpecificationPreference, v: Float): Float {
        var value = v
        if (value != 0.0f) {
            // Convert value to default units system
            val units = getMeasurementUnits(preference.specificationType, preference.isUseMetricSystem)
            value = units.toBase(value)

            value -= ROUTING_LIMIT_OFFSET
        }
        return value
    }

    companion object {
        /**
         * Offset added to or subtracted from vehicle parameter values during storage.
         * This is necessary because routing.xml uses the ">=" operator for constraint checks
         * (e.g., maxweight). If a vehicle's weight is exactly 3.5t and the road limit is 3.5t,
         * the road will be blocked. Subtracting this offset makes the stored weight (e.g., 3.4999t),
         * allowing it to pass the check.
         * @see <a href="https://github.com/osmandapp/OsmAnd/issues/4736">Issue #4736</a>
         */
        private const val ROUTING_LIMIT_OFFSET = 0.0001f
    }
}