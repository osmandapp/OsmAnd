package net.osmand.plus.mapcontextmenu.gallery

import net.osmand.shared.media.domain.MediaItem

/**
 * Stores media loading failure state outside adapter/view holders.
 *
 * This keeps GalleryItem immutable while preserving failed-load state across UI recreation.
 */
interface GalleryMediaLoadStateProvider {
	fun isMediaLoadFailed(mediaItem: MediaItem): Boolean
	fun markMediaLoadFailed(mediaItem: MediaItem)
}