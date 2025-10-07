package net.osmand.shared.gpx

import net.osmand.shared.io.KFile

class GpxDirItem(file: KFile) : DataItem(file) {

	override fun isValidValue(parameter: GpxParameter, value: Any?): Boolean {
		if (parameter.isAppearanceParameter()) {
			return value == null || parameter.typeClass == value::class;
		}
		return parameter.isGpxDirParameter() && super.isValidValue(parameter, value);
	}

	override fun getParameters(): Map<GpxParameter, Any?> =
		GpxParameter.getGpxDirParameters().associateWith { parameter -> getParameter(parameter) }
}