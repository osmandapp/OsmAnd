package net.osmand.plus.gallery

import net.osmand.shared.media.domain.MediaItem

/**
 * Represents the UI state for items displayed in the GalleryGridAdapter.
 */
sealed class GalleryItem {

	data class Media(
		val mediaItem: MediaItem,
		// TODO make GalleryItem immutable and update state via copy()
		var hasError: Boolean = false,
		var showProgress: Boolean = false
	) : GalleryItem()

	data class Action(
		val id: String,
	) : GalleryItem()

	data object NoMedia : GalleryItem()
	data object NoInternet : GalleryItem()
	data object MediaCount : GalleryItem()
}