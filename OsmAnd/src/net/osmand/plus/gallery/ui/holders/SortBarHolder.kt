package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class SortBarHolder(
	itemView: View,
	private val app: OsmandApplication,
	private val onActionClicked: (View, GalleryAction) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val ivIcon: ImageView = itemView.findViewById(R.id.sort_icon)
	private val tvTitle: TextView = itemView.findViewById(R.id.sort_title)

	fun bindView(item: GalleryItem.SortBar, nightMode: Boolean, gridMode: Boolean) {
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		ivIcon.setImageDrawable(app.uiUtilities.getPaintedIcon(R.drawable.ic_action_list_sort, activeColor))
		tvTitle.setText(item.sortMode.titleId)
		tvTitle.setTextColor(activeColor)
		itemView.setOnClickListener { onActionClicked(it, SORT_ACTION) }

		val basePadding = app.resources.getDimensionPixelSize(R.dimen.content_padding)
		val gridInset = if (gridMode) {
			AndroidUtils.dpToPx(app, GalleryGridItemDecorator.GRID_SIDE_PADDING_DP)
		} else {
			0
		}
		val sidePadding = (basePadding - gridInset).coerceAtLeast(0)
		itemView.setPaddingRelative(
			sidePadding, itemView.paddingTop, sidePadding, itemView.paddingBottom
		)
	}

	companion object {
		val SORT_ACTION = GalleryAction("gallery_sort")
	}
}
