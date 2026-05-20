package net.osmand.shared.vehicle.specification.domain.profiles

import net.osmand.shared.vehicle.specification.domain.Specification
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.vehicle.specification.domain.SpecificationType

abstract class VehicleSpecs(
    private val specifications: Map<SpecificationType, Specification>
) {
    fun getIconName(type: SpecificationType, nightMode: Boolean): String {
        return getSpecification(type).assets.getIconName(nightMode)
    }

    fun getSummary(type: SpecificationType): String {
        return getSpecification(type).assets.getSummary()
    }

    fun getPredefinedValues(type: SpecificationType, isMetric: Boolean): List<Float> {
        return getSpecification(type).getPredefinedValues(isMetric)
    }

    fun getMeasurementUnits(type: SpecificationType, isMetric: Boolean): MeasurementUnit<*> {
        return getSpecification(type).getDisplayUnits(isMetric)
    }

    fun getSpecification(type: SpecificationType): Specification {
        return specifications[type]
            ?: throw NoSuchElementException("Specification data for $type not found")
    }
}