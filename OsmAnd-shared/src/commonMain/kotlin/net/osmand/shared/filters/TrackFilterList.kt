package net.osmand.shared.filters

import kotlinx.serialization.json.Json
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil


object TrackFilterList {
	fun parseFilters(str: String): List<SmartFolder>? {
		var savedFilters: List<SmartFolder>? = null
		if (!KAlgorithms.isEmpty(str)) {
			try {
				savedFilters = Json.decodeFromString<List<SmartFolder>?>(str)
			} catch (error: Throwable) {
				error.printStackTrace()
			}
		}
		return savedFilters
	}
}
