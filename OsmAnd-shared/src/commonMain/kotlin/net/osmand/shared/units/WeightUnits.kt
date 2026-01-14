package net.osmand.shared.units

object WeightConstants {
	const val KILOGRAMS_IN_TON = 1000.0
	const val POUNDS_IN_TON = 2204.622621848776
}

enum class WeightUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem
) : MeasurementUnit<WeightUnits> {

	TONS(
		nameKey = "shared_string_tones",
		symbolKey = "metric_ton",
		conversionCoefficient = 1.0,
		system = MeasurementSystem.METRIC
	),

	KILOGRAMS(
		nameKey = "shared_string_kilograms",
		symbolKey = "kg",
		conversionCoefficient = WeightConstants.KILOGRAMS_IN_TON,
		system = MeasurementSystem.METRIC
	),

	POUNDS(
		nameKey = "shared_string_pounds",
		symbolKey = "metric_lbs",
		conversionCoefficient = WeightConstants.POUNDS_IN_TON,
		system = MeasurementSystem.IMPERIAL
	);
}