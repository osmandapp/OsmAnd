package net.osmand.shared.gpx.filters

import kotlinx.serialization.json.Json
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.util.KAlgorithms


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
