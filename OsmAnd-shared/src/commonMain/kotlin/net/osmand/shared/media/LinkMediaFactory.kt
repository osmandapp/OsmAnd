package net.osmand.shared.media

import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaPreviewUris
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

object LinkMediaFactory {

	private const val FILE_SCHEME = "file://"
	private const val CONTENT_SCHEME = "content://"
	private const val HTTP_SCHEME = "http://"
	private const val HTTPS_SCHEME = "https://"
	private const val OSMAND_SCHEME = "osmand://"
	private const val INTERNAL_MEDIA_LINK_NAMESPACE = "media"

	@JvmStatic
	@JvmOverloads
	fun fromLinks(links: List<Link>?, origin: MediaOrigin = MediaOrigin.UNKNOWN): List<MediaItem> {
		return links?.mapNotNull { fromLink(it, origin) }.orEmpty()
	}

	@JvmStatic
	fun createInternalUri(path: String): String {
		return OSMAND_SCHEME + path
	}

	@JvmStatic
	fun createInternalMediaUri(fileName: String): String {
		return createInternalUri("$INTERNAL_MEDIA_LINK_NAMESPACE/$fileName")
	}

	/** Extracts the bare file name from an internal link path such as "media/x.1.jpg". */
	@JvmStatic
	fun getInternalMediaFileName(internalPath: String?): String? {
		return internalPath
			?.trim { it.isWhitespace() || it == '/' }
			?.let { KAlgorithms.getFileWithoutDirs(it) }
			?.takeIf { it.isNotEmpty() }
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

	@JvmStatic
	fun getInternalPath(uri: String): String? {
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
		return MediaType.fromFileName(uri)
	}

	private fun isGenericMimeType(mimeType: String): Boolean {
		return mimeType == "*/*"
				|| mimeType == "application/octet-stream"
				|| mimeType == "binary/octet-stream"
	}
}
