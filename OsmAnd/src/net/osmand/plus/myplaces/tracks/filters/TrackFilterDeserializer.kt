package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import net.osmand.plus.myplaces.tracks.TrackFiltersHelper
import java.lang.reflect.Type

class TrackFilterDeserializer(private val smartFolderHelper: SmartFolderHelper) :
	JsonDeserializer<BaseTrackFilter> {
	val gson = GsonBuilder()
		.excludeFieldsWithoutExposeAnnotation()
		.create()

	override fun deserialize(
		json: JsonElement,
		typeOfT: Type,
		context: JsonDeserializationContext): BaseTrackFilter? {
		var baseFilterObject = json.asJsonObject
		var filterType = gson.fromJson(baseFilterObject.get("filterType"), FilterType::class.java)
		var realFilterObjectType = TrackFiltersHelper.getFilterClass(filterType)
		return gson.fromJson(baseFilterObject, realFilterObjectType)
	}
}