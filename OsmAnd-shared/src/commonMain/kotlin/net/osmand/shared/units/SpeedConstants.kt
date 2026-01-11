package net.osmand.shared.units

object SpeedConstants {
	const val KM_H_COEFFICIENT = 3.6
	const val MILES_PER_HOUR = 2.2369362920544
	const val KNOTS = 1.9438444924406
}

enum class SpeedUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem
) : MeasurementUnit<SpeedUnits> {

	METERS_PER_SECOND(
		nameKey = "si_m_s",
		symbolKey = "m_s",
		conversionCoefficient = 1.0,
		system = MeasurementSystem.METRIC
	),

	KILOMETERS_PER_HOUR(
		nameKey = "si_kmh",
		symbolKey = "km_h",
		conversionCoefficient = SpeedConstants.KM_H_COEFFICIENT,
		system = MeasurementSystem.METRIC
	),

	MILES_PER_HOUR(
		nameKey = "si_mph",
		symbolKey = "mile_per_hour",
		conversionCoefficient = SpeedConstants.MILES_PER_HOUR,
		system = MeasurementSystem.IMPERIAL
	),

	KNOTS(
		nameKey = "si_nm_h",
		symbolKey = "nm_h",
		conversionCoefficient = SpeedConstants.KNOTS,
		system = MeasurementSystem.IMPERIAL // Nautical miles/h
	);
}