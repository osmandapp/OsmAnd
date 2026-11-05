package net.osmand.shared.media

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.osmand.shared.media.domain.MediaDetails
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaPreviewUris
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.domain.RemoteMetadata
import kotlin.jvm.JvmStatic

/** Exact OSM-object photo lookup. No radius search or inferred identity. */
object FotoshiMediaFactory {
	@JvmStatic
	fun requestUrl(osmId: String?): String? = osmId
		?.takeIf { Regex("[nwr][1-9][0-9]*").matches(it) }
		?.let { "https://api.fotoshi.com/v1/data/photos?osm_id=$it&limit=5&photo_size=thumbnail,1280" }

	@JvmStatic
	fun fromResponse(response: String): List<MediaItem.Remote> {
		val root = try { Json.parseToJsonElement(response) as? JsonObject } catch (_: Exception) { null }
		if (root?.string("type") != "FeatureCollection") return emptyList()
		return (root["features"] as? JsonArray).orEmpty().mapNotNull { element ->
			val feature = element as? JsonObject ?: return@mapNotNull null
			val id = feature.string("id")?.takeIf { Regex("[A-Za-z0-9_-]+").matches(it) }
				?: return@mapNotNull null
			val properties = feature["properties"] as? JsonObject ?: return@mapNotNull null
			val assets = properties["assets"] as? JsonObject ?: return@mapNotNull null
			fun asset(name: String): String? = (assets[name] as? JsonObject)?.string("href")
				?.takeIf { it.startsWith("https://") && !it.contains('\n') && !it.contains('\r') }
			val thumbnail = asset("thumbnail") ?: return@mapNotNull null
			val image = asset("image-1280") ?: return@mapNotNull null
			val license = properties.string("image_license") ?: return@mapNotNull null
			val author = listOfNotNull(properties.string("creator_name"), properties.string("username"),
				properties.string("source_organization")).distinctBy { it.lowercase() }.joinToString(" · ")
				.takeIf { it.isNotBlank() } ?: return@mapNotNull null
			val page = "https://fotoshi.com/photo/$id"
			MediaItem.Remote(
				id = "fotoshi:$id", sourceUri = page, mediaUri = image,
				title = properties.string("title").orEmpty(), type = MediaType.PHOTO, origin = MediaOrigin.OTHER,
				previewUris = MediaPreviewUris(thumbnail, thumbnail, image),
				details = MediaDetails(author = author, date = properties.string("captured_at"), license = license),
				externalUri = page, downloadUri = image,
				metadata = RemoteMetadata(key = id, userName = properties.string("username").orEmpty())
			)
		}.distinctBy { it.id }.take(5)
	}

	private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)
		?.contentOrNull?.takeIf { it.isNotBlank() }
}
