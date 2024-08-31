package net.osmand.shared.filters

import kotlinx.serialization.json.Json
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil


object TrackFilterList {
	fun parseFilters(str: String): List<SmartFolder>? {
//		val gson: Gson = GsonBuilder()
//			.excludeFieldsWithoutExposeAnnotation()
//			.registerTypeAdapter(BaseTrackFilter::class.java, TrackFilterDeserializer())
//			.create()
//		val token: java.lang.reflect.Type = object : TypeToken<List<SmartFolder?>?>() {}.getType()
		var savedFilters: List<SmartFolder>? = null
		if (!KAlgorithms.isEmpty(str)) {
			try {
				savedFilters = Json.decodeFromString<List<SmartFolder>?>(str)
			} catch (error: Throwable) {
				error.printStackTrace()
			}
		}
//		try {
//			savedFilters = gson.fromJson(str, token)
//		} catch (error: Throwable) {
//			error.printStackTrace()
//		}
		return savedFilters
	}
}
