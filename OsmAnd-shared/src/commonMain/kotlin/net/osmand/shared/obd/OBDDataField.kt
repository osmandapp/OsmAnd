package net.osmand.shared.obd

import net.osmand.shared.extensions.currentTimeMillis

open class OBDDataField<T>(
	val type: OBDDataFieldType,
	val value: T) {
	var timestamp = currentTimeMillis()
	fun getDisplayName(): String {
		return type.getDisplayName()
	}

	fun getDisplayUnit(): String {
		return type.getDisplayUnit()
	}
}