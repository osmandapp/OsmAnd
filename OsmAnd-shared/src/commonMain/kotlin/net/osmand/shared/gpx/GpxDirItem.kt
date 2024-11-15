package net.osmand.shared.gpx

import net.osmand.shared.io.KFile

class GpxDirItem(file: KFile) : DataItem(file) {

	override fun isValidValue(parameter: GpxParameter, value: Any?): Boolean {
		return parameter.isAppearanceParameter() && (value == null || parameter.typeClass == value::class)
	}

	override fun getParameters(): Map<GpxParameter, Any?> =
		GpxParameter.getGpxDirParameters().associateWith { parameter -> getParameter(parameter) }
}