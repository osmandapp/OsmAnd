package net.osmand.shared.vehicle.specification.data

import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmStatic

object VehicleValueConverter {

	@JvmStatic
	fun readSavedValue(valueStr: String, displayUnit: MeasurementUnit<*>): Double {
		val baseValue = KAlgorithms.parseDoubleSilently(valueStr, 0.0)
		return if (baseValue != 0.0) {
			displayUnit.fromBase(baseValue)
		} else {
			baseValue
		}
	}

	@JvmStatic
	fun prepareValueToSave(displayValue: Double, displayUnit: MeasurementUnit<*>): Double {
		return if (displayValue != 0.0) {
			displayUnit.toBase(displayValue)
		} else {
			displayValue
		}
	}
}