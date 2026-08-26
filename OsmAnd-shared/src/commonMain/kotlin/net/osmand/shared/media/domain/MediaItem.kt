package net.osmand.shared.media.domain

import kotlin.jvm.JvmStatic

enum class MediaOrigin(
	val titleKey: String? = null,
	val iconName: String? = null
) {
	OSM(iconName = "ic_osm"),
	WIKIPEDIA(titleKey = "wikimedia", iconName = "ic_logo_wikimedia"),
	MAPILLARY(iconName = "ic_logo_mapillary"),
	OTHER,
	UNKNOWN
}

enum class MediaType(val typeName: String) {

	PHOTO("Photo"),
	VIDEO("Video"),
	AUDIO("Audio"),
	UNKNOWN("Media");

	companion object {
		@JvmStatic
		fun fromMimeType(mimeType: String?): MediaType {
			val normalized = mimeType?.trim()?.lowercase() ?: return UNKNOWN
			return when {
				normalized.startsWith("image/") -> PHOTO
				normalized.startsWith("video/") -> VIDEO
				normalized.startsWith("audio/") -> AUDIO
				else -> UNKNOWN
			}
		}

		@JvmStatic
		fun fromFileName(fileName: String?): MediaType {
			return fromExtension(getExtension(fileName))
		}

		@JvmStatic
		fun fromExtension(extension: String?): MediaType {
			return when (normalizeExtension(extension)) {
				"jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg" -> PHOTO
				"mp4", "m4v", "mov", "avi", "mkv", "webm" -> VIDEO
				"3gp", "3gpp", "3ga", "mp3", "m4a", "aac", "wav", "ogg", "oga", "opus", "amr" -> AUDIO
				else -> UNKNOWN
			}
		}

		@JvmStatic
		fun isSupportedExtension(extension: String?): Boolean {
			return fromExtension(extension) != UNKNOWN
		}

		fun normalizeExtension(extension: String?): String {
			return extension?.trim()?.substringAfterLast('.')?.lowercase().orEmpty()
		}

		private fun getExtension(uri: String?): String? {
			uri ?: return null
			var end = uri.length
			val queryIndex = uri.indexOf('?')
			val fragmentIndex = uri.indexOf('#')
			if (queryIndex >= 0) {
				end = queryIndex
			}
			if (fragmentIndex in 0..<end) {
				end = fragmentIndex
			}
			if (end == 0) {
				return null
			}
			val slashIndex = uri.lastIndexOf('/', end - 1)
			val dotIndex = uri.lastIndexOf('.', end - 1)
			return if (dotIndex > slashIndex && dotIndex + 1 < end) {
				uri.substring(dotIndex + 1, end)
			} else {
				null
			}
		}
	}
}

/**
 * Resolved media URIs used by UI/providers to display or consume media.
 *
 * thumbnailUri is used for small previews, standardSizeUri for regular gallery cells,
 * and fullSizeUri for fullscreen/gallery viewer loading. For local media these may
 * all point to the same mediaUri.
 */
data class MediaPreviewUris(
	val thumbnailUri: String?,
	val standardSizeUri: String?,
	val fullSizeUri: String?
)

/**
 * Optional descriptive metadata associated with media.
 *
 * This object contains source-independent details that can be shown by UI,
 * such as localized descriptions, author, date and license.
 */
data class MediaDetails(
	val descriptions: Map<String, String>? = null,
	val author: String? = null,
	val date: String? = null,
	val license: String? = null
) {

	fun getDescription(preferredLanguage: String?): String? {
		val descriptions = descriptions ?: return null
		val normalizedLanguage = normalizeLanguageKey(preferredLanguage)

		if (normalizedLanguage != null) {
			descriptions[normalizedLanguage]
				?.takeIf { it.isNotBlank() }
				?.let { return it }
		}

		return descriptions[ENGLISH_LANGUAGE]
			?.takeIf { it.isNotBlank() }
			?: descriptions.values.firstOrNull { it.isNotBlank() }
	}

	private fun normalizeLanguageKey(language: String?): String? {
		return language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
	}

	companion object {
		const val ENGLISH_LANGUAGE = "en"
	}
}

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

	/**
	 * Source/page URI associated with this media item.
	 *
	 * For remote media this may point to a web page, attribution page,
	 * external service, or another source location related to the media.
	 * Local media usually does not have a separate source URI; use mediaUri
	 * when the media resource itself should be opened or displayed.
	 */
	abstract val sourceUri: String?

	/**
	 * Direct URI of the media resource itself.
	 *
	 * This is the base URI used to load or display the media content.
	 * For local media it may be a file/content URI or an app-relative path.
	 */
	abstract val mediaUri: String

	/**
	 * Resolved media variants used by UI/providers for loading.
	 */
	abstract val previewUris: MediaPreviewUris

	/**
	 * Optional descriptive metadata.
	 *
	 * Null means there are no additional details for this media item.
	 */
	abstract val details: MediaDetails?

	data class Internal(
		val relativePath: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN,
		override val details: MediaDetails? = null
	) : MediaItem() {
		override val id: String = relativePath
		override val sourceUri: String? = null
		override val mediaUri: String = relativePath
		override val previewUris: MediaPreviewUris = MediaPreviewUris(
			thumbnailUri = relativePath,
			standardSizeUri = relativePath,
			fullSizeUri = relativePath
		)
	}

	data class Gallery(
		val uri: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN,
		override val details: MediaDetails? = null
	) : MediaItem() {
		override val id: String = uri
		override val sourceUri: String? = null
		override val mediaUri: String = uri
		override val previewUris: MediaPreviewUris = MediaPreviewUris(
			thumbnailUri = uri,
			standardSizeUri = uri,
			fullSizeUri = uri
		)
	}

	data class Remote(
		override val id: String,
		override val sourceUri: String?,
		override val mediaUri: String,
		override val title: String,
		override val type: MediaType,
		override val origin: MediaOrigin = MediaOrigin.UNKNOWN,
		override val previewUris: MediaPreviewUris,
		override val details: MediaDetails?,
		val externalUri: String?,
		val downloadUri: String?,
		val metadata: RemoteMetadata = RemoteMetadata()
	) : MediaItem()
}