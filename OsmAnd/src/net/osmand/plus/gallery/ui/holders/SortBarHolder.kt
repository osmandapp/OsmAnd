package net.osmand.plus.gallery.ui.holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator
import net.osmand.plus.gallery.ui.GallerySortBarView
import net.osmand.plus.utils.AndroidUtils

class SortBarHolder(
	itemView: View,
	private val app: OsmandApplication,
	private val onSortModeSelected: (GallerySortMode) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val sortBarView = itemView as GallerySortBarView

	fun bindView(item: GalleryItem.SortBar, gridMode: Boolean) {
		val basePadding = app.resources.getDimensionPixelSize(R.dimen.content_padding)
		val gridInset = if (gridMode) {
			AndroidUtils.dpToPx(app, GalleryGridItemDecorator.GRID_SIDE_PADDING_DP)
		} else {
			0
		}
		val sidePadding = (basePadding - gridInset).coerceAtLeast(0)
		sortBarView.bind(
			sortMode = item.sortMode,
			sortModes = item.sortModes,
			horizontalContentPaddingPx = sidePadding,
			onSortModeSelected = onSortModeSelected
		)
	}
}
