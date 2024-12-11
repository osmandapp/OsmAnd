package net.osmand.shared.obd

import net.osmand.shared.extensions.currentTimeMillis

open class OBDDataField<T>(
	val value: T) {
	var timestamp = currentTimeMillis()
	var location: OBDDataComputer.OBDLocation? = null

	companion object {
		val NO_DATA = OBDDataField<Any>(0)
	}
}