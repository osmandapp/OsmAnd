package net.osmand.plus.gallery.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import kotlin.math.roundToInt

/** One card per adapter section; all media stay in the same RecyclerView coordinate space. */
class GallerySectionCardDecoration(app: OsmandApplication, nightMode: Boolean) : RecyclerView.ItemDecoration() {
	private val padding = app.resources.getDimensionPixelSize(R.dimen.content_padding)
	private val columnGap = AndroidUtils.dpToPxF(app, 8f).roundToInt()
	private val rowGap = AndroidUtils.dpToPxF(app, 6f).roundToInt()
	private val radius = AndroidUtils.dpToPxF(app, 16f)
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtilities.getColor(app, ColorUtilities.getListBgColorId(nightMode)) }

	override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
		val adapter = parent.adapter as? GalleryGridAdapter ?: return
		val pos = parent.getChildAdapterPosition(view)
		if (pos !in 0 until adapter.itemCount) return
		val item = adapter.getItem(pos)
		val boundary = adapter.getSectionBoundary(pos) ?: return
		if (item is GalleryItem.GroupHeader) {
			if (pos > 0) outRect.top = padding
		} else if (item is GalleryItem.NoMedia) {
			outRect.top = padding
		} else if (item is GalleryItem.Media && adapter.displayMode == GalleryDisplayMode.GRID) {
			val manager = parent.layoutManager as? GridLayoutManager ?: return
			val span = manager.spanCount
			val first = boundary.firstMediaPosition
			val end = boundary.lastPosition
			val column = (pos - first) % span
			val leading = padding + column * (columnGap - 2 * padding) / span
			val trailing = padding + (span - column - 1) * (columnGap - 2 * padding) / span
			if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_RTL) {
				outRect.left = trailing; outRect.right = leading
			} else { outRect.left = leading; outRect.right = trailing }
			outRect.top = if (pos - first < span) padding else rowGap
			outRect.bottom = if ((pos - first) / span == (end - first) / span) padding else 0
		}
	}

	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		val adapter = parent.adapter as? GalleryGridAdapter ?: return
		val sections = linkedMapOf<String, RectF>()
		val bounds = Rect()
		for (index in 0 until parent.childCount) {
			val child = parent.getChildAt(index)
			val pos = parent.getChildAdapterPosition(child)
			val boundary = adapter.getSectionBoundary(pos) ?: continue
			parent.getDecoratedBoundsWithMargins(child, bounds)
			val gap = if (boundary.isFirst && (boundary.firstPosition > 0 || adapter.getItem(pos) is GalleryItem.NoMedia)) padding else 0
			val top = bounds.top + gap + child.translationY
			val bottom = bounds.bottom + child.translationY
			val rect = sections.getOrPut(boundary.sectionId) { RectF(parent.paddingLeft.toFloat(), top,
				(parent.width - parent.paddingRight).toFloat(), bottom) }
			rect.top = minOf(rect.top, top)
			rect.bottom = maxOf(rect.bottom, bottom)
			// Keep corners beyond the viewport when the real section edge is not laid out.
			if (!boundary.roundTopCorners && parent.findViewHolderForAdapterPosition(boundary.firstPosition) == null) {
				rect.top = minOf(rect.top, -radius)
			}
			if (!boundary.roundBottomCorners && parent.findViewHolderForAdapterPosition(boundary.lastPosition) == null) {
				rect.bottom = maxOf(rect.bottom, parent.height + radius)
			}
		}
		sections.values.forEach { canvas.drawRoundRect(it, radius, radius, paint) }
	}
}
