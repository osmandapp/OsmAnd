package net.osmand.plus.gallery.data

import net.osmand.shared.media.domain.MediaItem

class MediaLoadStateRegistry {
	private val failedIds = mutableSetOf<String>()

	fun markFailed(mediaItem: MediaItem) {
		failedIds.add(mediaItem.id)
	}

	fun isFailed(mediaItem: MediaItem): Boolean {
		return mediaItem.id in failedIds
	}

	fun clear() {
		failedIds.clear()
	}
}