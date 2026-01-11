package net.osmand.shared.units

enum class HeartRateUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem = MeasurementSystem.UNIVERSAL
) : MeasurementUnit<HeartRateUnits> {
	BPM(
		nameKey = "beats_per_minute_short",
		symbolKey = "beats_per_minute_short",
		conversionCoefficient = 1.0
	)
}