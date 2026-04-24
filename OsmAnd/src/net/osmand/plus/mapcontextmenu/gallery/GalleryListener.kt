package net.osmand.plus.mapcontextmenu.gallery

import net.osmand.shared.media.domain.MediaItem

interface GalleryListener {

	fun onMediaItemClicked(mediaItem: MediaItem)

	fun onReloadMediaItems() {}
}
