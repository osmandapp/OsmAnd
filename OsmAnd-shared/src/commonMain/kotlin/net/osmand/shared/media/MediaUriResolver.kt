package net.osmand.shared.media

import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import kotlin.jvm.JvmStatic

object MediaUriResolver {

	@JvmStatic
	fun getDetailsLink(mediaItem: MediaItem?): String? {
		mediaItem ?: return null

		return if (mediaItem is MediaItem.Remote) {
			if (shouldUseExternalUriAsDetailsLink(mediaItem)) {
				mediaItem.externalUri ?: mediaItem.mediaUri
			} else {
				mediaItem.downloadUri ?: mediaItem.mediaUri
			}
		} else {
			mediaItem.previewUris.fullSizeUri ?: mediaItem.mediaUri
		}
	}

	@JvmStatic
	fun getShareUri(mediaItem: MediaItem?): String? {
		return (mediaItem as? MediaItem.Remote)?.downloadUri ?: mediaItem?.mediaUri
	}

	@JvmStatic
	fun getBrowserUri(mediaItem: MediaItem?): String? {
		return (mediaItem as? MediaItem.Remote)?.externalUri
	}

	@JvmStatic
	fun getDownloadUri(mediaItem: MediaItem?): String? {
		return when (mediaItem) {
			is MediaItem.Remote -> mediaItem.downloadUri ?: mediaItem.mediaUri
			else -> mediaItem?.mediaUri
		}
	}

	@JvmStatic
	fun getFailedLoadDisplayUri(mediaItem: MediaItem?): String? {
		return mediaItem?.sourceUri ?: mediaItem?.mediaUri
	}

	private fun shouldUseExternalUriAsDetailsLink(mediaItem: MediaItem.Remote): Boolean {
		return mediaItem.origin == MediaOrigin.WIKIPEDIA
	}
}