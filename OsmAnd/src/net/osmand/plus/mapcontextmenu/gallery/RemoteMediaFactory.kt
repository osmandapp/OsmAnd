package net.osmand.plus.mapcontextmenu.gallery

import net.osmand.shared.media.domain.MediaDetails
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaResource
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.domain.RemoteMetadata
import net.osmand.shared.wiki.WikiImage
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RemoteMediaFactory {

	private const val THUMBNAIL_WIDTH = 160
	private const val GALLERY_FULL_SIZE_WIDTH = 1280

	private val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
		override fun initialValue(): SimpleDateFormat {
			return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
		}
	}

	/**
	 * Creates a regular remote media item from OsmAnd server JSON.
	 *
	 * This follows the legacy ImageCard behavior:
	 * - thumbnailUri is generated from imageHiresUrl with a small width;
	 * - previewUri is generated from imageHiresUrl with gallery width;
	 * - fullUri uses gallery-sized imageHiresUrl when available and falls back to imageUrl.
	 *
	 * Do not use this method for OSM "image" tag items that previously used UrlImageCard,
	 * because UrlImageCard intentionally used safer preview/full URLs.
	 */
	@JvmStatic
	fun fromJson(
		imageObject: JSONObject,
		origin: MediaOrigin = MediaOrigin.UNKNOWN
	): MediaItem.Remote? {
		val imageUrl = imageObject.optStringOrNull("imageUrl")
		val imageHiresUrl = imageObject.optStringOrNull("imageHiresUrl")
		val sourceUrl = imageUrl ?: return null

		val key = imageObject.optStringOrNull("key")
		val viewUrl = imageObject.optStringOrNull("url").orEmpty()
		val galleryFullSizeUrl = createGalleryFullSizeUrl(imageHiresUrl)

		return MediaItem.Remote(
			id = key ?: imageUrl,
			sourceUrl = sourceUrl,
			title = imageObject.optStringOrNull("title").orEmpty(),
			type = MediaType.PHOTO,
			origin = origin,
			resource = MediaResource(
				thumbnailUri = createThumbnailUrl(imageHiresUrl) ?: imageUrl,
				previewUri = galleryFullSizeUrl ?: imageUrl,
				fullUri = galleryFullSizeUrl ?: imageUrl
			),
			details = MediaDetails(
				viewUrl = viewUrl
			),
			metadata = createRemoteMetadata(imageObject)
		)
	}

	@JvmStatic
	fun fromWikiImage(wikiImage: WikiImage): MediaItem.Remote {
		val metadata = wikiImage.metadata
		val imageHiresUrl = wikiImage.imageHiResUrl

		return MediaItem.Remote(
			id = wikiImage.wikiMediaTag,
			sourceUrl = wikiImage.imageStubUrl,
			title = wikiImage.imageName,
			type = MediaType.PHOTO,
			origin = MediaOrigin.WIKIPEDIA,
			resource = MediaResource(
				thumbnailUri = createThumbnailUrl(imageHiresUrl),
				previewUri = wikiImage.imageStubUrl,
				fullUri = createGalleryFullSizeUrl(imageHiresUrl) ?: imageHiresUrl
			),
			details = MediaDetails(
				// TODO: check preferred language
				description = metadata.getDescription(null) ?: "",
				author = metadata.author ?: "",
				date = metadata.date ?: "",
				license = metadata.license ?: "",
				viewUrl = wikiImage.getUrlWithCommonAttributions()
			)
		)
	}

	@JvmStatic
	fun fromUrlImageJson(imageObject: JSONObject): MediaItem.Remote? {
		val imageUrl = imageObject.optStringOrNull("imageUrl") ?: return null
		val viewUrl = resolveBrowserUrl(imageObject).orEmpty()
		val key = imageObject.optStringOrNull("key")

		return MediaItem.Remote(
			id = key ?: viewUrl.takeIf { it.isNotBlank() } ?: imageUrl,
			sourceUrl = imageUrl,
			title = imageObject.optStringOrNull("title").orEmpty(),
			type = MediaType.PHOTO,
			origin = MediaOrigin.UNKNOWN,
			resource = MediaResource(
				/*
				 * Thumbnail is `null` due to the nature of the OSM "image" tag.
				 * Since the tag can contain any URL pointing to any external service, generating a reliable
				 * thumbnail format (a highly compressed low-resolution image) is not feasible.
				 * In such cases, the best approach is to directly load the full-size image instead.
				 */
				thumbnailUri = null,

				previewUri = imageUrl,

				/*
				 * URL for displaying the image in the gallery viewer.
				 * Instead of using the "hires" image URL (if available), it uses a lower-quality
				 * "image" URL in this case to avoid potential crashes.
				 * Some high-resolution images may be too large to be properly rendered on a canvas bitmap.
				 * To prevent crashes, this implementation uses URL with lower-quality image.
				 */
				fullUri = imageUrl
			),
			details = MediaDetails(
				viewUrl = viewUrl
			),
			metadata = createRemoteMetadata(imageObject)
		)
	}

	private fun createThumbnailUrl(imageHiresUrl: String?): String? {
		return imageHiresUrl?.takeIf { it.isNotBlank() }?.let { "$it?width=$THUMBNAIL_WIDTH" }
	}

	private fun createGalleryFullSizeUrl(imageHiresUrl: String?): String? {
		return imageHiresUrl?.takeIf { it.isNotBlank() }?.let { "$it?width=$GALLERY_FULL_SIZE_WIDTH" }
	}

	private fun parseTimestamp(timestamp: String?): Date? {
		if (timestamp.isNullOrBlank()) {
			return null
		}
		return try {
			DATE_FORMAT.get()?.parse(timestamp)
		} catch (e: ParseException) {
			timestamp.toLongOrNull()?.let { Date(it) }
		}
	}

	/**
	 * Returns a suitable URL for opening in a browser.
	 *
	 * This may be a high-resolution image URL (if available) or the URL stored in the OSM "image" tag.
	 * The link can either point directly to an image file or lead to a webpage hosting the image
	 * on an external service.
	 */
	private fun resolveBrowserUrl(imageObject: JSONObject): String? {
		val hiresUrl = imageObject.optStringOrNull("imageHiresUrl")
		return hiresUrl ?: imageObject.optStringOrNull("url")
	}

	private fun JSONObject.optStringOrNull(name: String): String? {
		return if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null
	}

	private fun JSONObject.optDoubleOrNull(name: String): Double? {
		return if (has(name) && !isNull(name)) optDouble(name) else null
	}

	private fun JSONObject.optBooleanOrFalse(name: String): Boolean {
		return has(name) && !isNull(name) && optBoolean(name, false)
	}

	private fun JSONObject.optFloatOrNaN(name: String): Float {
		return if (has(name) && !isNull(name)) optDouble(name).toFloat() else Float.NaN
	}

	/**
	 * Parses remote metadata used for media-specific behavior.
	 *
	 * Legacy ImageCard also parsed topIcon/buttonIcon/buttonText/button colors, but these fields
	 * describe old card UI configuration rather than media data. They are intentionally not mapped
	 * to MediaItem. Known source icons are represented by MediaOrigin.
	 */
	private fun createRemoteMetadata(imageObject: JSONObject): RemoteMetadata {
		return RemoteMetadata(
			key = imageObject.optStringOrNull("key").orEmpty(),
			latitude = imageObject.optDoubleOrNull("lat"),
			longitude = imageObject.optDoubleOrNull("lon"),
			cameraAngle = imageObject.optDoubleOrNull("ca") ?: Double.NaN,
			timestamp = parseTimestamp(imageObject.optStringOrNull("timestamp"))?.time,
			userName = imageObject.optStringOrNull("username").orEmpty(),
			externalLink = imageObject.optBooleanOrFalse("externalLink"),
			distance = imageObject.optFloatOrNaN("distance"),
			bearing = imageObject.optFloatOrNaN("bearing"),
			is360 = imageObject.optBooleanOrFalse("is360")
		)
	}
}