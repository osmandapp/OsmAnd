package net.osmand.shared.units

object PercentConstants {
	const val FRACTION_COEFFICIENT = 0.01
}

enum class PercentUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem = MeasurementSystem.UNIVERSAL
) : MeasurementUnit<PercentUnits> {

	/**
	 * Percentage representation.
	 * Range: 0.0 .. 100.0
	 */
	PERCENT(
		nameKey = "shared_string_percentage",
		symbolKey = "percent_unit",
		conversionCoefficient = 1.0
	),

	/**
	 * Proportional representation (Fraction/Ratio).
	 * Range: 0.0 .. 1.0 (where 1.0 equals 100%).
	 */
	FRACTION(
		nameKey = "shared_string_ratio",
		symbolKey = "",
		conversionCoefficient = PercentConstants.FRACTION_COEFFICIENT
	);
}