package net.osmand.shared.media

import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import kotlin.jvm.JvmStatic

object MediaUrlResolver {

	private const val THUMBNAIL_WIDTH = 160
	private const val GALLERY_FULL_SIZE_WIDTH = 1280

	@JvmStatic
	fun getDisplayLink(mediaItem: MediaItem?): String? {
		return if (mediaItem?.origin == MediaOrigin.WIKIPEDIA) {
			mediaItem.details.viewUrl
		} else if (!mediaItem?.resource?.fullUri.isNullOrEmpty()) {
			mediaItem?.resource?.fullUri
		} else {
			mediaItem?.sourceUri
		}
	}

	@JvmStatic
	fun getThumbnailUrl(mediaItem: MediaItem?): String? {
		return mediaItem?.resource?.thumbnailUri
	}

	@JvmStatic
	fun getShareUrl(mediaItem: MediaItem?): String? {
		return mediaItem?.resource?.fullUri
	}

	@JvmStatic
	fun getBrowserUrl(mediaItem: MediaItem?): String? {
		return mediaItem?.details?.viewUrl
	}

	@JvmStatic
	fun getDownloadUrl(mediaItem: MediaItem?): String? {
		return mediaItem?.resource?.fullUri ?: mediaItem?.sourceUri
	}

	fun getGalleryThumbnailUrl(mediaItem: MediaItem?): String? {
		val hiResUrl = mediaItem?.resource?.fullUri ?: return null
		return "$hiResUrl?width=$THUMBNAIL_WIDTH"
	}

	fun getGalleryFullSizeUrl(mediaItem: MediaItem?): String? {
		val hiResUrl = mediaItem?.resource?.fullUri ?: return null
		return "$hiResUrl?width=$GALLERY_FULL_SIZE_WIDTH"
	}
}