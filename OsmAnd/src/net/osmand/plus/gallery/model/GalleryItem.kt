package net.osmand.plus.gallery.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.shared.media.domain.MediaItem

/**
 * Represents typed presentation items displayed by GalleryGridAdapter.
 */
sealed class GalleryItem {

	data class Media(
		val mediaItem: MediaItem,
		val showLoadingProgress: Boolean = false,
		val presentation: GalleryMediaPresentation? = null
	) : GalleryItem()

	data class Action(
		val action: GalleryAction
	) : GalleryItem()

	data class NoMedia @JvmOverloads constructor(
		val action: GalleryAction? = null,
		@StringRes val titleResId: Int = R.string.no_photos_available,
		@StringRes val descriptionResId: Int = R.string.no_photos_available_descr,
		@DrawableRes val iconResId: Int = R.drawable.ic_action_desert,
		val buttonStyle: ActionButtonStyle = ActionButtonStyle.SIMPLE
	) : GalleryItem() {
		enum class ActionButtonStyle { SIMPLE, DIALOG }
	}

	data object NoInternet : GalleryItem()
	data object MediaCount : GalleryItem()

	data class SortBar(
		val sortMode: GallerySortMode
	) : GalleryItem()

	data class MediaStats(
		val text: String
	) : GalleryItem()
}

data class GalleryMediaPresentation(
	val description: String? = null,
	val durationLabel: String? = null
)

data class GalleryAction(
	val id: String
)

data class GalleryActionButton(
	val titleId: Int,
	val action: GalleryAction
)

data class GalleryToolbarAction(
	val action: GalleryAction,
	@DrawableRes val iconId: Int,
	@StringRes val titleId: Int
)