package net.osmand.shared.filters

import net.osmand.shared.filters.BaseTrackFilter
import net.osmand.shared.filters.TrackFilterType
import net.osmand.shared.filters.TrackFiltersHelper

class TrackFilterDeserializer
//	:JsonDeserializer<BaseTrackFilter>
{
//	val gson: Gson = GsonBuilder()
//		.excludeFieldsWithoutExposeAnnotation()
//		.create()

//	override fun deserialize(
//		json: JsonElement,
//		typeOfT: Type,
//		context: JsonDeserializationContext): BaseTrackFilter? {
//		val baseFilterObject = json.asJsonObject
//		val trackFilterType =
//			gson.fromJson(baseFilterObject.get("filterType"), TrackFilterType::class.java)
//		val realFilterObjectType = TrackFiltersHelper.getFilterClass(trackFilterType)
//		return gson.fromJson(baseFilterObject, realFilterObjectType)
//	}
}