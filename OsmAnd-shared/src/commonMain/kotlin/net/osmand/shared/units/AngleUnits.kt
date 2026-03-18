package net.osmand.shared.units

import kotlin.math.PI

object AngleConstants {
	const val RADIANS_IN_DEGREE = PI / 180.0
	const val MILLIRADIANS_IN_DEGREE = 1000.0 * RADIANS_IN_DEGREE
}

enum class AngleUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem = MeasurementSystem.UNIVERSAL
) : MeasurementUnit<AngleUnits> {

	DEGREES(
		nameKey = "shared_string_degrees",
		symbolKey = "°",
		conversionCoefficient = 1.0
	),

	RADIANS(
		nameKey = "shared_string_radians",
		symbolKey = "rad",
		conversionCoefficient = AngleConstants.RADIANS_IN_DEGREE
	),

	MILLIRADIANS(
		nameKey = "shared_string_milliradians",
		symbolKey = "mrad",
		conversionCoefficient = AngleConstants.MILLIRADIANS_IN_DEGREE
	);

	override fun getSymbol() = symbolKey
}