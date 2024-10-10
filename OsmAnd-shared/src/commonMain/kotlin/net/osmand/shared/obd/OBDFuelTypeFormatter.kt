package net.osmand.shared.obd

import net.osmand.shared.util.Localization

class OBDFuelTypeFormatter : OBDDataComputer.OBDComputerWidgetFormatter("%02d") {
	override fun format(v: Any?): String {
		return if (v == null) {
			"-"
		} else {
			val code = super.format(v)
			val type = FuelType.fromCode(code)
			return Localization.getString(type.getDisplayName())
		}
	}
}