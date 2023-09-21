package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class TrackFilterDeserializer(private val filterCreator: TrackFiltersSettingsCollection) :
	JsonDeserializer<BaseTrackFilter> {
	val gson = Gson()

	override fun deserialize(
		json: JsonElement,
		typeOfT: Type,
		context: JsonDeserializationContext): BaseTrackFilter? {
		var baseFilterObject = json.asJsonObject
		var filterType = gson.fromJson(baseFilterObject.get("filterType"), FilterType::class.java)
		var realFilterObjectType = filterCreator.getFilterClass(filterType)
		return gson.fromJson(baseFilterObject, realFilterObjectType);
	}
}