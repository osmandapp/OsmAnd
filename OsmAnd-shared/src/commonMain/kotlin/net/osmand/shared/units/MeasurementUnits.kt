package net.osmand.shared.units

import net.osmand.shared.util.Localization

interface MeasurementUnit<T : MeasurementUnit<T>> {

	val nameKey: String
	val symbolKey: String
	val conversionCoefficient: Double
	val system: MeasurementSystem

	fun getName(): String = Localization.getString(nameKey)

	fun getSymbol(): String = Localization.getString(symbolKey)

	fun isImperial() = system == MeasurementSystem.IMPERIAL

	fun isMetricSystem() = system == MeasurementSystem.METRIC

	fun toBase(value: Double): Double = value / conversionCoefficient

	fun fromBase(value: Double): Double = value * conversionCoefficient

	fun from(value: Double, sourceUnit: MeasurementUnit<*>): Double {
		return if (this == sourceUnit) value else fromBase(sourceUnit.toBase(value))
	}
}