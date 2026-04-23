package net.osmand.shared.media.domain

import net.osmand.shared.wiki.WikiImage

enum class MediaOrigin(val iconName: String?) {
	OSM("ic_osm"),
	WIKIPEDIA("ic_logo_wikimedia"),
	MAPILLARY("ic_logo_mapillary"),
	UNKNOWN(null)
}

enum class MediaType {
	PHOTO, VIDEO, AUDIO, UNKNOWN;

	companion object {
		// Maps standard MIME types from GPX <type> tags to MediaType
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

// Encapsulates URLs for different preview sizes provided by remote servers
data class MediaContent(
	val standardUrl: String,
	val thumbnailUrl: String,
	val hiResUrl: String
)

sealed class MediaItem {
	abstract val title: String
	abstract val type: MediaType
	abstract val origin: MediaOrigin

	// 1. Internal Media
	// Represents files physically stored within the app's internal storage (e.g., A/V notes).
	// Uses relative paths (e.g., "avnotes/video.mp4").
	data class Internal(
		val relativePath: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN
	) : MediaItem()

	// 2. System Gallery Media
	// Represents files located outside the app's internal storage, chosen by the user.
	// Uses absolute URIs (e.g., "content://..." on Android or "file://..." on iOS).
	data class Gallery(
		val uri: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN
	) : MediaItem()

	// 3. Remote Web Media
	// Represents media hosted on external servers (http:// or https://).
	// Can be any media type (photo, video, audio).
	data class Remote(
		val sourceUrl: String, // Direct link to the binary file for loading pixels
		override val title: String,
		override val type: MediaType,
		val previewContent: MediaContent? = null,
		val webpageUrl: String? = null, // Optional link to the source webpage for viewing in a browser
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN
	) : MediaItem()

	// 4. Wikipedia Media
	// Legacy wrapper for the existing WikiImage data structure.
	data class Wiki(
		val wikiImage: WikiImage,
		override val origin: MediaOrigin = MediaOrigin.WIKIPEDIA
	) : MediaItem() {
		override val title: String = wikiImage.imageName
		override val type: MediaType = MediaType.PHOTO
	}
}