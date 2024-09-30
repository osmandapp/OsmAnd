package net.osmand.shared.gpx.filters

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TrackFilterSerializer : KSerializer<List<BaseTrackFilter>?> {
	override val descriptor: SerialDescriptor =
		ListSerializer(BaseTrackFilter.serializer()).descriptor

	@OptIn(ExperimentalSerializationApi::class)
	override fun serialize(encoder: Encoder, value: List<BaseTrackFilter>?) {
		if (value == null) {
			encoder.encodeNull()
		} else {
			val listEncoder = encoder.beginCollection(descriptor, value.size)
			value.forEach { filter ->
				listEncoder.encodeSerializableElement(
					descriptor, 0, BaseTrackFilter.serializer(), filter
				)
			}
			listEncoder.endStructure(descriptor)
		}
	}

	override fun deserialize(decoder: Decoder): List<BaseTrackFilter> {
		val jsonDecoder = decoder as? JsonDecoder
			?: throw SerializationException("This class can be loaded only by JSON")
		val jsonArray = jsonDecoder.decodeJsonElement().jsonArray
		return jsonArray.map { jsonElement ->
			val jsonObject = jsonElement.jsonObject
			val filterType =
				jsonObject["filterType"]?.jsonPrimitive?.content?.let { TrackFilterType.valueOf(it) }
					?: throw SerializationException("Missing filterType")
			when (filterType.filterType) {
				FilterType.RANGE -> jsonDecoder.json.decodeFromJsonElement<RangeTrackFilter<Comparable<Any>>>(
					jsonObject)

				FilterType.DATE_RANGE -> jsonDecoder.json.decodeFromJsonElement<DateTrackFilter>(
					jsonObject)

				FilterType.OTHER -> jsonDecoder.json.decodeFromJsonElement<OtherTrackFilter>(
					jsonObject)

				FilterType.TEXT -> jsonDecoder.json.decodeFromJsonElement<TextTrackFilter>(
					jsonObject)

				FilterType.SINGLE_FIELD_LIST -> {
					if (filterType == TrackFilterType.FOLDER) {
						jsonDecoder.json.decodeFromJsonElement<FolderTrackFilter>(jsonObject)
					} else {
						jsonDecoder.json.decodeFromJsonElement<ListTrackFilter>(jsonObject)
					}
				}
			}.apply { initFilter() }
		}
	}
}