package net.osmand.shared.media

import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaPreviewUris
import net.osmand.shared.media.domain.MediaType
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

object LinkMediaFactory {

	private const val FILE_SCHEME = "file://"
	private const val CONTENT_SCHEME = "content://"
	private const val HTTP_SCHEME = "http://"
	private const val HTTPS_SCHEME = "https://"
	private const val OSMAND_SCHEME = "osmand://"

	@JvmStatic
	@JvmOverloads
	fun fromLinks(links: List<Link>?, origin: MediaOrigin = MediaOrigin.UNKNOWN): List<MediaItem> {
		return links?.mapNotNull { fromLink(it, origin) }.orEmpty()
	}

	@JvmStatic
	fun createInternalUri(path: String): String {
		return OSMAND_SCHEME + path
	}

	private fun fromLink(link: Link?, origin: MediaOrigin): MediaItem? {
		val uri = link?.href?.trim()?.takeIf { it.isNotEmpty() } ?: return null
		val type = getMediaType(link.type, uri)
		if (type == MediaType.UNKNOWN) {
			return null
		}
		val title = link.text?.trim().orEmpty()
		return when {
			isGalleryUri(uri) -> MediaItem.Gallery(uri, title, type, origin)
			isRemoteUri(uri) -> createRemoteItem(uri, title, type, origin)
			else -> getInternalPath(uri)?.let { path ->
				MediaItem.Internal(path, title, type, origin)
			}
		}
	}

	private fun createRemoteItem(uri: String, title: String, type: MediaType, origin: MediaOrigin) = MediaItem.Remote(
		id = uri,
		sourceUri = uri,
		mediaUri = uri,
		title = title,
		type = type,
		origin = origin,
		previewUris = MediaPreviewUris(
			thumbnailUri = uri,
			standardSizeUri = uri,
			fullSizeUri = uri
		),
		details = null,
		externalUri = uri,
		downloadUri = uri
	)

	private fun isGalleryUri(uri: String) = uri.startsWith(CONTENT_SCHEME, ignoreCase = true)
			|| uri.startsWith(FILE_SCHEME, ignoreCase = true)

	private fun isRemoteUri(uri: String) = uri.startsWith(HTTP_SCHEME, ignoreCase = true)
			|| uri.startsWith(HTTPS_SCHEME, ignoreCase = true)

	private fun isOsmandUri(uri: String) = uri.startsWith(OSMAND_SCHEME, ignoreCase = true)

	private fun getInternalPath(uri: String): String? {
		if (!uri.startsWith(OSMAND_SCHEME, ignoreCase = true)) {
			return null
		}
		val path = uri.substring(OSMAND_SCHEME.length)
		return when {
			path.isEmpty() -> null
			path.indexOf(':') >= 0 -> null
			path.startsWith("/") -> null
			path.startsWith("?") -> null
			path.startsWith("#") -> null
			path == ".." -> null
			path.startsWith("../") -> null
			path.endsWith("/..") -> null
			path.contains("/../") -> null
			else -> path
		}
	}

	private fun getMediaType(mimeType: String?, uri: String): MediaType {
		val normalizedMimeType = mimeType?.trim()?.lowercase()
		if (!normalizedMimeType.isNullOrEmpty()) {
			val type = MediaType.fromMimeType(normalizedMimeType)
			if (type != MediaType.UNKNOWN) {
				return type
			}
			if (!isGenericMimeType(normalizedMimeType)) {
				return MediaType.UNKNOWN
			}
		}
		return getMediaTypeByExtension(uri)
	}

	private fun isGenericMimeType(mimeType: String): Boolean {
		return mimeType == "*/*"
				|| mimeType == "application/octet-stream"
				|| mimeType == "binary/octet-stream"
	}

	private fun getMediaTypeByExtension(uri: String): MediaType {
		return when (getExtension(uri)) {
			"jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg" -> MediaType.PHOTO
			"mp4", "m4v", "mov", "avi", "mkv", "webm" -> MediaType.VIDEO
			"3gp", "3gpp", "mp3", "m4a", "aac", "wav", "ogg", "oga", "opus", "amr" -> MediaType.AUDIO
			else -> MediaType.UNKNOWN
		}
	}

	private fun getExtension(uri: String): String? {
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
			uri.substring(dotIndex + 1, end).lowercase()
		} else {
			null
		}
	}
}
