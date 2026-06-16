package net.osmand.plus.gallery.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.ui.holders.GalleryMediaViewHolder
import net.osmand.plus.gallery.ui.holders.MediaHolderType
import net.osmand.plus.gallery.ui.holders.ActionViewHolder
import net.osmand.plus.utils.AndroidUtils

class GalleryGridItemDecorator(
	private val app: OsmandApplication
) : RecyclerView.ItemDecoration() {

	private val standardItemOffsetInPx = AndroidUtils.dpToPx(app, 6f)

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		super.getItemOffsets(outRect, view, parent, state)
		val position = parent.getChildAdapterPosition(view)
		when (val holder = parent.getChildViewHolder(view)) {
			is GalleryMediaViewHolder -> applyMediaOffsets(outRect, holder.holderType, position)
			is ActionViewHolder -> outRect.right = AndroidUtils.dpToPx(app, 16f)
		}
	}

	private fun applyMediaOffsets(outRect: Rect, type: MediaHolderType, position: Int) {
		when (type) {
			MediaHolderType.SPAN_RESIZABLE -> {
				val gridSpace = AndroidUtils.dpToPx(app, GRID_SCREEN_ITEM_SPACE_DP.toFloat())
				outRect.set(gridSpace, gridSpace, gridSpace, gridSpace)
			}
			MediaHolderType.MAIN -> {
				outRect.left = app.resources.getDimensionPixelSize(R.dimen.content_padding)
				outRect.right = standardItemOffsetInPx
			}
			MediaHolderType.STANDARD, MediaHolderType.SMALL -> {
				outRect.left = standardItemOffsetInPx
				outRect.right = standardItemOffsetInPx
				if (position % 2 == 0) {
					outRect.top = standardItemOffsetInPx
					outRect.bottom = 0
				} else {
					outRect.bottom = standardItemOffsetInPx
					outRect.top = 0
				}
			}
		}
	}

	companion object {
		const val GRID_SCREEN_ITEM_SPACE_DP = 3

		const val GRID_SIDE_PADDING_DP = 13f
	}
}