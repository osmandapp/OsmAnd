package net.osmand.shared.media

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import net.osmand.shared.media.domain.MediaDetails
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaPreviewUris
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.domain.RemoteMetadata
import net.osmand.shared.panoramax.PanoramaxApi
import kotlin.jvm.JvmStatic

/** Converts exact OSM Panoramax picture references and catalog results into gallery media. */
object PanoramaxMediaFactory {
	private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

	/** Keep OSM order, ignoring malformed and repeated semicolon-separated IDs. */
	@JvmStatic
	fun parseIds(value: String?): List<String> = value.orEmpty()
		.split(';')
		.map(String::trim)
		.filter { UUID_PATTERN.matches(it) }
		.map(String::lowercase)
		.distinct()

	/** The catalog returns member-instance asset URLs, avoiding its HTTP 308 image redirects. */
	@JvmStatic
	fun fromSearchResponse(response: String?, requestedIds: List<String>): List<MediaItem.Remote> {
		if (response.isNullOrBlank() || requestedIds.isEmpty()) return emptyList()
		val features = try {
			Json.parseToJsonElement(response).jsonObject["features"] as? JsonArray
		} catch (_: Exception) {
			null
		} ?: return emptyList()

		val requested = requestedIds.toSet()
		val byId = linkedMapOf<String, MediaItem.Remote>()
		for (element in features) {
			val feature = element as? JsonObject ?: continue
			val id = feature.string("id")?.lowercase() ?: continue
			if (id !in requested || !UUID_PATTERN.matches(id) || id in byId) continue
			byId[id] = fromFeature(feature, id) ?: continue
		}
		return requestedIds.mapNotNull(byId::get)
	}

	private fun fromFeature(feature: JsonObject, id: String): MediaItem.Remote? {
		val assets = feature["assets"] as? JsonObject ?: return null
		val thumbnailUrl = assets.httpsAssetUrl("thumb") ?: return null
		val imageUrl = assets.httpsAssetUrl("sd") ?: return null
		val downloadUrl = assets.httpsAssetUrl("hd") ?: imageUrl
		val properties = feature["properties"] as? JsonObject
		val coordinates = (feature["geometry"] as? JsonObject)?.get("coordinates") as? JsonArray
		val longitude = (coordinates?.getOrNull(0) as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
		val latitude = (coordinates?.getOrNull(1) as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
		val dateTime = properties?.string("datetime")
		val timestamp = parseTimestamp(dateTime)
		val producer = (feature["providers"] as? JsonArray)
			?.mapNotNull { it as? JsonObject }
			?.firstOrNull { provider ->
				(provider["roles"] as? JsonArray)?.any { (it as? JsonPrimitive)?.contentOrNull == "producer" } == true
			}
			?.string("name")
		val fieldOfView = (properties?.get("pers:interior_orientation") as? JsonObject)
			?.number("field_of_view")
		val viewerUrl = PanoramaxApi.getViewerUrl(id)

		return MediaItem.Remote(
			id = id,
			sourceUri = viewerUrl,
			mediaUri = imageUrl,
			title = properties?.string("title").orEmpty(),
			type = MediaType.PHOTO,
			origin = MediaOrigin.PANORAMAX,
			previewUris = MediaPreviewUris(
				thumbnailUri = thumbnailUrl,
				standardSizeUri = thumbnailUrl,
				fullSizeUri = imageUrl
			),
			details = MediaDetails(
				author = producer,
				date = if (timestamp != null) dateTime?.substringBefore('T') else null,
				license = properties?.string("license")
			),
			externalUri = viewerUrl,
			downloadUri = downloadUrl,
			metadata = RemoteMetadata(
				key = id,
				latitude = latitude,
				longitude = longitude,
				cameraAngle = properties?.number("view:azimuth") ?: Double.NaN,
				timestamp = timestamp,
				userName = producer.orEmpty(),
				is360 = fieldOfView != null && fieldOfView >= 360.0
			)
		)
	}

	private fun JsonObject.httpsAssetUrl(name: String): String? =
		(this[name] as? JsonObject)?.string("href")?.takeIf { it.startsWith("https://", ignoreCase = true) }

	private fun parseTimestamp(dateTime: String?): Long? = try {
		dateTime?.let { Instant.parse(it).toEpochMilliseconds() }
	} catch (_: Exception) {
		null
	}

	private fun JsonObject.string(name: String): String? =
		(this[name] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

	private fun JsonObject.number(name: String): Double? = string(name)?.toDoubleOrNull()
}
