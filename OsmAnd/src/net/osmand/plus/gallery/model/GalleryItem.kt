package net.osmand.plus.gallery.model

import net.osmand.shared.media.domain.MediaItem

/**
 * Represents typed presentation items displayed by GalleryGridAdapter.
 */
sealed class GalleryItem {

	data class Media(
		val mediaItem: MediaItem,
		val showLoadingProgress: Boolean = false
	) : GalleryItem()

	data class Action(
		val action: GalleryAction
	) : GalleryItem()

	data class NoMedia(
		val action: GalleryAction? = null
	) : GalleryItem()

	data object NoInternet : GalleryItem()
	data object MediaCount : GalleryItem()
}

data class GalleryAction(
	val id: String
)