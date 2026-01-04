package net.osmand.shared.units

enum class TimeUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem
) : MeasurementUnit<TimeUnits> {

	MILLISECONDS(
		nameKey = "shared_string_ms",
		symbolKey = "ms",
		conversionCoefficient = 1.0,
		system = MeasurementSystem.UNIVERSAL
	),

	SECONDS(
		nameKey = "shared_string_sec",
		symbolKey = "s",
		conversionCoefficient = 0.001,
		system = MeasurementSystem.UNIVERSAL
	),

	MINUTES(
		nameKey = "shared_string_minute_lowercase",
		symbolKey = "shared_string_minute_lowercase",
		conversionCoefficient = 1.0 / 60000.0,
		system = MeasurementSystem.UNIVERSAL
	);
}