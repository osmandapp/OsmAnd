package net.osmand.shared.units

enum class PowerUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem = MeasurementSystem.UNIVERSAL
) : MeasurementUnit<PowerUnits> {
	WATTS(
		nameKey = "power_watts_unit",
		symbolKey = "power_watts_unit",
		conversionCoefficient = 1.0
	);
}