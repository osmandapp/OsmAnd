package net.osmand.shared.obd

open class OBDDataField(
	val type: OBDDataFieldType,
	private val stringValue: String) {
	fun getDisplayName(): String {
		return type.getDisplayName()
	}

	fun getDisplayUnit(): String {
		return type.getDisplayUnit()
	}

	open fun getValue(): String {
		return stringValue
	}
}