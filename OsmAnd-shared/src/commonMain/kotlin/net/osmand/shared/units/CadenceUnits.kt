package net.osmand.shared.units

enum class CadenceUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem = MeasurementSystem.UNIVERSAL
) : MeasurementUnit<CadenceUnits> {
	RPM(
		nameKey = "revolutions_per_minute_unit",
		symbolKey = "revolutions_per_minute_unit",
		conversionCoefficient = 1.0
	)
}