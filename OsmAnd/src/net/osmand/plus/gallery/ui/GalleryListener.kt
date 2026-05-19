package net.osmand.plus.gallery.ui

import net.osmand.shared.media.domain.MediaItem

interface GalleryListener {

	fun onMediaItemClicked(mediaItem: MediaItem)

	fun onReloadMediaItems() {}
}
