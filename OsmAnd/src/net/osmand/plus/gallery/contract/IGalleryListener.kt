package net.osmand.plus.gallery.contract

import net.osmand.shared.media.domain.MediaItem

interface IGalleryListener {

	fun onMediaItemClicked(mediaItem: MediaItem)

	fun onReloadMediaItems() {}
}
