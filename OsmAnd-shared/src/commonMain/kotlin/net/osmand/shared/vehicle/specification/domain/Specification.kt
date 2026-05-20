package net.osmand.shared.vehicle.specification.domain

import net.osmand.shared.vehicle.specification.data.validator.DefaultValidator
import net.osmand.shared.vehicle.specification.data.validator.SpecificationValidator
import net.osmand.shared.vehicle.specification.domain.util.Assets
import net.osmand.shared.vehicle.specification.domain.util.UnitValues

data class Specification(
    val assets: Assets,
    val metric: UnitValues,
    val imperial: UnitValues,
    val validatorFactory: (Specification) -> SpecificationValidator = { DefaultValidator }
) {

    val validator: SpecificationValidator = validatorFactory(this)

    fun getDisplayUnits(isMetric: Boolean) = getSystem(isMetric).units

    fun getPredefinedValues(isMetric: Boolean) = getSystem(isMetric).values

    private fun getSystem(isMetric: Boolean) = if (isMetric) metric else imperial
}