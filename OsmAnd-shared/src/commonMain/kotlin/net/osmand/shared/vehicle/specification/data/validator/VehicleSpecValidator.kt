package net.osmand.shared.vehicle.specification.data.validator

/**
 * Functional interface for specification value validation.
 */
fun interface SpecificationValidator {
	/**
	 * Validates the value.
	 * @return null or empty string if valid, otherwise an error message/key.
	 */
	fun validate(value: Double, isMetric: Boolean): String
}

val DefaultValidator = SpecificationValidator { _, _ -> "" }