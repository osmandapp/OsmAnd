package net.osmand.plus.gallery

import net.osmand.shared.media.domain.MediaItem

/**
 * Represents the UI state for items displayed in the GalleryGridAdapter.
 */
sealed class GalleryItem {

	data class Media(
		val mediaItem: MediaItem,
		var hasError: Boolean = false,
		var showProgress: Boolean = false
	) : GalleryItem()

	data object MapillaryContribute : GalleryItem()

	data object Progress : GalleryItem()

	data object NoImages : GalleryItem()

	// Represents the "No Internet" placeholder, which can show a loading spinner
	data class NoInternet(var isLoading: Boolean = false) : GalleryItem()

	// Represents the footer text showing the total number of items
	data class ImagesCount(val count: Int) : GalleryItem()
}