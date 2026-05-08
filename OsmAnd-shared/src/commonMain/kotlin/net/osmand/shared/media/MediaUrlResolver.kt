package net.osmand.shared.media

import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import kotlin.jvm.JvmStatic

object MediaUrlResolver {

	@JvmStatic
	fun getDisplayLink(mediaItem: MediaItem?): String? {
		return if (mediaItem?.origin == MediaOrigin.WIKIPEDIA) {
			mediaItem.details.viewUrl
		} else {
			val fullUri = mediaItem?.resource?.fullUri
			if (fullUri.isNullOrEmpty()) {
				mediaItem?.sourceUri
			} else {
				fullUri
			}
		}
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
}