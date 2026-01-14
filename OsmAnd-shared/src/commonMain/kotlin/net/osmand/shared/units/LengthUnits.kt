package net.osmand.shared.units

object LengthConstants {
	const val CENTIMETERS_IN_METER = 100.0
	const val INCHES_IN_METER = 39.37007874015748
	const val FEET_IN_METER = 3.2808398950131235
	const val YARDS_IN_METER = 1.0936132983377078
	const val KILOMETERS_IN_METER = 0.001
	const val MILES_IN_METER = 0.0006213711922373339
	const val NAUTICAL_MILES_IN_METER = 0.0005399568034557235
}

enum class LengthUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem
) : MeasurementUnit<LengthUnits> {

	METERS(
		nameKey = "shared_string_meters",
		symbolKey = "m",
		conversionCoefficient = 1.0,
		system = MeasurementSystem.METRIC
	),

	KILOMETERS(
		nameKey = "shared_string_kilometers",
		symbolKey = "km",
		conversionCoefficient = LengthConstants.KILOMETERS_IN_METER,
		system = MeasurementSystem.METRIC
	),

	CENTIMETERS(
		nameKey = "shared_string_centimeters",
		symbolKey = "centimeter",
		conversionCoefficient = LengthConstants.CENTIMETERS_IN_METER,
		system = MeasurementSystem.METRIC
	),

	MILES(
		nameKey = "miles",
		symbolKey = "mile",
		conversionCoefficient = LengthConstants.MILES_IN_METER,
		system = MeasurementSystem.IMPERIAL
	),

	NAUTICAL_MILES(
		nameKey = "si_nm",
		symbolKey = "nm",
		conversionCoefficient = LengthConstants.NAUTICAL_MILES_IN_METER,
		system = MeasurementSystem.IMPERIAL
	),

	INCHES(
		nameKey = "shared_string_inches",
		symbolKey = "inch",
		conversionCoefficient = LengthConstants.INCHES_IN_METER,
		system = MeasurementSystem.IMPERIAL
	),

	FEET(
		nameKey = "shared_string_feet",
		symbolKey = "foot",
		conversionCoefficient = LengthConstants.FEET_IN_METER,
		system = MeasurementSystem.IMPERIAL
	),

	YARDS(
		nameKey = "shared_string_yards",
		symbolKey = "yard",
		conversionCoefficient = LengthConstants.YARDS_IN_METER,
		system = MeasurementSystem.IMPERIAL
	);
}