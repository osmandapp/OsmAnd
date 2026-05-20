package net.osmand.plus.gallery.ui

import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.shared.media.domain.MediaItem

/**
 * Configuration for displaying gallery items in a grid row.
 *
 * Allows callers to customize item limiting and row-level UI without making
 * GalleryGridAdapter depend on specific media sources or plugins.
 */
data class GalleryGridConfig(
	/**
	 * Maximum number of media items matching [mediaItemLimitPredicate] to display.
	 *
	 * Non-media items and media items that do not match the predicate are not limited.
	 * If null, no media item limit is applied.
	 */
	val mediaItemsLimit: Int? = null,

	/**
	 * Defines which media items should be affected by [mediaItemsLimit].
	 */
	val mediaItemLimitPredicate: (MediaItem) -> Boolean = { true },

	/**
	 * Title used for the row "Show all" button.
	 */
	@StringRes val showAllButtonTitleResId: Int = R.string.shared_string_show_all,

	/**
	 * Optional action for the row button.
	 *
	 * If null, the button opens the regular gallery grid. If set, the action is
	 * delegated to plugins via PluginsHelper.
	 */
	val showAllButtonAction: GalleryAction? = null
)