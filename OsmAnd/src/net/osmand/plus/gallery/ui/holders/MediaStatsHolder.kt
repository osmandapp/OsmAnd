package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class MediaStatsHolder(
	itemView: View,
	private val app: OsmandApplication
) : RecyclerView.ViewHolder(itemView) {

	private val tvStats: TextView = itemView.findViewById(R.id.stats)
	private val divider: View = itemView.findViewById(R.id.stats_divider)

	fun bindView(stats: GalleryItem.MediaStats, nightMode: Boolean, gridMode: Boolean) {
		tvStats.text = stats.text
		tvStats.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))

		val baseMargin = app.resources.getDimensionPixelSize(R.dimen.content_padding)
		val topPadding = if (gridMode) baseMargin else 0
		itemView.setPaddingRelative(
			itemView.paddingStart,
			topPadding,
			itemView.paddingEnd,
			itemView.paddingBottom
		)

		val gridInset = if (gridMode) {
			AndroidUtils.dpToPx(app, GalleryGridItemDecorator.GRID_SIDE_PADDING_DP)
		} else {
			0
		}
		val sideMargin = (baseMargin - gridInset).coerceAtLeast(0)
		(divider.layoutParams as? MarginLayoutParams)?.let { params ->
			params.marginStart = sideMargin
			params.marginEnd = sideMargin
			divider.layoutParams = params
		}
	}
}
