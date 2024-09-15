package net.osmand.shared.obd

import net.osmand.shared.util.Localization

class OBDFuelTypeDataField(value: String) :
	OBDDataField("obd_fuel_type", "", value) {

	override fun getValue(): String {
		val type = FuelType.fromCode(stringValue)
		return Localization.getString(type.getDisplayName())
	}
}