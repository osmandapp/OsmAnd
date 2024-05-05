package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import net.osmand.plus.myplaces.tracks.TrackFiltersHelper
import java.lang.reflect.Type

class TrackFilterDeserializer :
	JsonDeserializer<BaseTrackFilter> {
	val gson: Gson = GsonBuilder()
		.excludeFieldsWithoutExposeAnnotation()
		.create()

	override fun deserialize(
		json: JsonElement,
		typeOfT: Type,
		context: JsonDeserializationContext): BaseTrackFilter? {
		val baseFilterObject = json.asJsonObject
		val trackFilterType =
			gson.fromJson(baseFilterObject.get("filterType"), TrackFilterType::class.java)
		val realFilterObjectType = TrackFiltersHelper.getFilterClass(trackFilterType)
		return gson.fromJson(baseFilterObject, realFilterObjectType)
	}
}