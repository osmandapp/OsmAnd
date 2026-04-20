package net.osmand.shared.vehicle.specification.data.validator

import net.osmand.shared.util.Localization
import net.osmand.shared.util.SharedNumberFormatter
import net.osmand.shared.vehicle.specification.data.validator.StringConstants.CAR_PROFILE
import net.osmand.shared.vehicle.specification.data.validator.StringConstants.ERROR_PATTERN
import net.osmand.shared.vehicle.specification.domain.Specification

private object StringConstants {
	const val ERROR_PATTERN = "common_weight_limit_error"
	const val CAR_PROFILE = "app_mode_car"
}

class TruckWeightValidator(private val spec: Specification): SpecificationValidator {

	override fun validate(value: Double, isMetric: Boolean): String {
		val units = spec.getDisplayUnits(isMetric)
		val values = spec.getPredefinedValues(isMetric)

		val min = values.firstOrNull() ?: 0f
		if (value < min) {
			val minFormatted = SharedNumberFormatter.formatDecimal(min.toDouble())
			val unitsName = units.getName()
			val profileName = Localization.getString(CAR_PROFILE)
			return Localization.getString(ERROR_PATTERN, minFormatted, unitsName, profileName)
		}
		return ""
	}
}