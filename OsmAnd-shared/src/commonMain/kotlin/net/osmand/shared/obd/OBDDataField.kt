package net.osmand.shared.obd

import net.osmand.shared.util.Localization

open class OBDDataField(
	private val nameId: String,
	private val unitNameId: String,
	protected val stringValue: String) {
	fun getDisplayName(): String {
		return Localization.getString(nameId)
	}

	fun getDisplayUnit(): String {
		return Localization.getString(unitNameId)
	}

	open fun getValue(): String {
		return stringValue
	}
}