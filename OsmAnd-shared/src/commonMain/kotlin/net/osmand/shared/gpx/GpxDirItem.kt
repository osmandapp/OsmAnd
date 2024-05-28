package net.osmand.shared.gpx

import net.osmand.shared.io.CommonFile

class GpxDirItem(file: CommonFile) : DataItem(file) {

	override fun isValidValue(parameter: GpxParameter, value: Any?): Boolean {
		return parameter.isAppearanceParameter() && (value == null || parameter.typeClass == value::class)
	}
}