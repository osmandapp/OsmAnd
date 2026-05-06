package net.osmand.shared.media.domain

import net.osmand.shared.util.Localization

enum class MediaOrigin(
	val titleKey: String? = null,
	val iconName: String? = null
) {
	OSM(iconName = "ic_osm"),
	WIKIPEDIA(titleKey = "wikimedia", iconName = "ic_logo_wikimedia"),
	MAPILLARY(iconName = "ic_logo_mapillary"),
	UNKNOWN;

	// TODO: extract from domain logic
	fun getTitle() = if (titleKey != null) Localization.getString(titleKey) else ""
}

enum class MediaType {
	PHOTO, VIDEO, AUDIO, UNKNOWN;

	companion object {
		fun fromMimeType(mimeType: String?): MediaType {
			val normalized = mimeType?.trim()?.lowercase() ?: return UNKNOWN
			return when {
				normalized.startsWith("image/") -> PHOTO
				normalized.startsWith("video/") -> VIDEO
				normalized.startsWith("audio/") -> AUDIO
				else -> UNKNOWN
			}
		}
	}
}

/**
 * Resolved URIs used by UI/providers to display or consume media.
 * For local media these URIs may all point to the same source URI.
 */
data class MediaResource(
	val thumbnailUri: String?,
	val previewUri: String?,
	val fullUri: String?
)

data class MediaDetails(
	val description: String = "",
	val author: String = "",
	val date: String = "",
	val license: String = "",
	val viewUrl: String = ""
)

data class RemoteMetadata(
	val key: String = "",
	val latitude: Double? = null,
	val longitude: Double? = null,
	val cameraAngle: Double = Double.NaN,
	val timestamp: Long? = null,
	val userName: String = "",
	val externalLink: Boolean = false,
	val distance: Float = Float.NaN,
	val bearing: Float = Float.NaN,
	val is360: Boolean = false
)

sealed class MediaItem {

	abstract val id: String
	abstract val title: String
	abstract val type: MediaType
	abstract val origin: MediaOrigin
	abstract val sourceUri: String
	abstract val resource: MediaResource
	abstract val details: MediaDetails

	data class Internal(
		val relativePath: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN,
		override val details: MediaDetails = MediaDetails()
	) : MediaItem() {
		override val id: String = relativePath
		override val sourceUri: String = relativePath
		override val resource: MediaResource = MediaResource(
			thumbnailUri = relativePath,
			previewUri = relativePath,
			fullUri = relativePath
		)
	}

	data class Gallery(
		val uri: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN,
		override val details: MediaDetails = MediaDetails()
	) : MediaItem() {
		override val id: String = uri
		override val sourceUri: String = uri
		override val resource: MediaResource = MediaResource(
			thumbnailUri = uri,
			previewUri = uri,
			fullUri = uri
		)
	}

	data class Remote(
		override val id: String,
		val sourceUrl: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN,
		override val resource: MediaResource,
		override val details: MediaDetails = MediaDetails(),
		val metadata: RemoteMetadata = RemoteMetadata()
	) : MediaItem() {
		override val sourceUri: String = sourceUrl
	}
}