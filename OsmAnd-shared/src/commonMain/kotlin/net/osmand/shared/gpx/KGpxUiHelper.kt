package net.osmand.shared.gpx

import net.osmand.shared.util.KAlgorithms

object KGpxUiHelper {

	fun getGpxTitle(name: String?): String {
		return KAlgorithms.getFileNameWithoutExtension(name) ?: ""
	}

	fun getGpxDirTitle(name: String?): String? {
		return if (KAlgorithms.isEmpty(name)) {
			""
		} else KAlgorithms.capitalizeFirstLetter(
			KAlgorithms.getFileNameWithoutExtension(
				name))
	}

}