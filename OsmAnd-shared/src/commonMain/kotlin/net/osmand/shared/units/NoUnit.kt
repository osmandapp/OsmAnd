package net.osmand.shared.units

object NoUnit : MeasurementUnit<Nothing> {
	override val nameKey: String = ""
	override val symbolKey: String = ""
	override val conversionCoefficient: Double = 1.0
	override val system: MeasurementSystem = MeasurementSystem.UNIVERSAL

	override fun getName(): String = ""
	override fun getSymbol(): String = ""

	// override T-based methods if necessary, but for NONE we can just ignore T
	override fun from(value: Double, sourceUnit: MeasurementUnit<*>): Double = value
}