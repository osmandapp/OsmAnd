package net.osmand.shared.units

enum class TemperatureUnits(
	override val nameKey: String,
	override val symbolKey: String,
	override val conversionCoefficient: Double,
	override val system: MeasurementSystem
) : MeasurementUnit<TemperatureUnits> {

	CELSIUS(
		nameKey = "degree_celsius",
		symbolKey = "°C",
		conversionCoefficient = 1.0,
		system = MeasurementSystem.METRIC
	),

	FAHRENHEIT(
		nameKey = "weather_temperature_fahrenheit",
		symbolKey = "°F",
		conversionCoefficient = 1.8,
		system = MeasurementSystem.IMPERIAL
	) {
		override fun toBase(value: Double): Double = (value - 32.0) / conversionCoefficient
		override fun fromBase(value: Double): Double = (value * conversionCoefficient) + 32.0
	};

	override fun getSymbol() = symbolKey
}