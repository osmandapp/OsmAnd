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

	data object NoImages : GalleryItem()
	data object MapillaryContribute : GalleryItem()

	// Represents the "No Internet" placeholder, which can show a loading spinner
	data class NoInternet(var isLoading: Boolean = false) : GalleryItem()

	// Represents the footer text showing the total number of items
	data object MediaCount : GalleryItem()
}