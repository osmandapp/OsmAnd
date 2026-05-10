package net.osmand.plus.gallery.data

import net.osmand.shared.media.domain.MediaDetails
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaPreviewUris
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
	 * Expected JSON fields:
	 * - url: source/page URI related to the media;
	 * - imageUrl: direct media URI used as the base image;
	 * - imageHiresUrl: optional high-resolution image URI used to derive preview variants.
	 *
	 * This method is intended for regular remote image objects returned by OsmAnd services.
	 * OSM "image" tag objects should use fromUrlImageJson(), because they may point to
	 * arbitrary external services and require safer preview/fullscreen URI handling.
	 */
	@JvmStatic
	fun fromJson(
		imageObject: JSONObject,
		origin: MediaOrigin = MediaOrigin.UNKNOWN
	): MediaItem.Remote? {
		val mediaUri = imageObject.optStringOrNull("imageUrl") ?: return null
		val imageHiresUrl = imageObject.optStringOrNull("imageHiresUrl")
		val sourceUri = imageObject.optStringOrNull("url")
		val key = imageObject.optStringOrNull("key")
		val galleryFullSizeUri = createGalleryFullSizeUri(imageHiresUrl)

		return MediaItem.Remote(
			id = key ?: mediaUri,
			sourceUri = sourceUri,
			mediaUri = mediaUri,
			title = imageObject.optStringOrNull("title").orEmpty(),
			type = MediaType.PHOTO,
			origin = origin,
			previewUris = MediaPreviewUris(
				thumbnailUri = createThumbnailUri(imageHiresUrl) ?: mediaUri,
				standardSizeUri = mediaUri,
				fullSizeUri = galleryFullSizeUri ?: mediaUri
			),
			details = null,
			externalUri = null,
			downloadUri = imageHiresUrl ?: mediaUri,
			metadata = createRemoteMetadata(imageObject)
		)
	}

	@JvmStatic
	fun fromWikiImage(wikiImage: WikiImage): MediaItem.Remote {
		val metadata = wikiImage.metadata
		val imageHiresUrl = wikiImage.imageHiResUrl
		val mediaUri = wikiImage.imageStubUrl

		return MediaItem.Remote(
			id = wikiImage.wikiMediaTag,
			sourceUri = mediaUri,
			mediaUri = mediaUri,
			title = wikiImage.imageName,
			type = MediaType.PHOTO,
			origin = MediaOrigin.WIKIPEDIA,
			previewUris = MediaPreviewUris(
				thumbnailUri = createThumbnailUri(imageHiresUrl),
				standardSizeUri = mediaUri,
				fullSizeUri = createGalleryFullSizeUri(imageHiresUrl) ?: imageHiresUrl
			),
			externalUri = wikiImage.getUrlWithCommonAttributions(),
			downloadUri = imageHiresUrl.takeIf { it.isNotBlank() } ?: mediaUri,
			details = MediaDetails(
				descriptions = metadata.descriptions.toMap(),
				author = metadata.author,
				date = metadata.date,
				license = metadata.license
			)
		)
	}

	/**
	 * Creates a remote media item from an OSM "image" tag JSON object.
	 *
	 * OSM image tags may point to arbitrary external services, so generated thumbnail
	 * or hi-res preview URIs are not assumed to be safe. The regular imageUrl is used
	 * for both standard and full-size loading, while externalUri is resolved separately
	 * for opening the media outside the app.
	 */
	@JvmStatic
	fun fromUrlImageJson(imageObject: JSONObject): MediaItem.Remote? {
		val mediaUri = imageObject.optStringOrNull("imageUrl") ?: return null
		val key = imageObject.optStringOrNull("key")
		val sourceUri = imageObject.optStringOrNull("url")
		val imageHiresUrl = imageObject.optStringOrNull("imageHiresUrl")

		return MediaItem.Remote(
			id = key ?: sourceUri?.takeIf { it.isNotBlank() } ?: mediaUri,
			sourceUri = sourceUri,
			mediaUri = mediaUri,
			title = imageObject.optStringOrNull("title").orEmpty(),
			type = MediaType.PHOTO,
			origin = MediaOrigin.UNKNOWN,
			previewUris = MediaPreviewUris(
				/*
				 * Thumbnail is null because an OSM "image" tag can point to an
				 * arbitrary external service where reliable thumbnail generation
				 * is not available.
				 */
				thumbnailUri = null,

				/*
				 * Use the direct media URI for preview/fullscreen display.
				 * This avoids loading potentially very large hi-res resources
				 * into bitmap rendering.
				 */
				standardSizeUri = mediaUri,
				fullSizeUri = mediaUri
			),
			details = null,
			externalUri = imageHiresUrl ?: sourceUri,
			downloadUri = imageHiresUrl ?: mediaUri,
			metadata = createRemoteMetadata(imageObject)
		)
	}

	private fun createThumbnailUri(imageHiresUrl: String?): String? {
		return imageHiresUrl?.takeIf { it.isNotBlank() }?.let { "$it?width=$THUMBNAIL_WIDTH" }
	}

	private fun createGalleryFullSizeUri(imageHiresUrl: String?): String? {
		return imageHiresUrl?.takeIf { it.isNotBlank() }?.let { "$it?width=$GALLERY_FULL_SIZE_WIDTH" }
	}

	private fun parseTimestamp(timestamp: String?): Date? {
		if (timestamp.isNullOrBlank()) {
			return null
		}

		timestamp.toLongOrNull()?.let {
			return Date(it)
		}

		return try {
			DATE_FORMAT.get()?.parse(timestamp)
		} catch (e: ParseException) {
			null
		}
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
	 * UI styling fields from server responses, such as topIcon/buttonIcon/button colors,
	 * are intentionally not mapped here because MediaItem should only contain media data.
	 * Known source icons are represented by MediaOrigin.
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