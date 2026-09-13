package net.osmand.plus.gallery.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.motion.GallerySectionCardTracks
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import kotlin.math.roundToInt

class GallerySectionCardDecoration(app: OsmandApplication, nightMode: Boolean) : RecyclerView.ItemDecoration() {
	private val padding = app.resources.getDimensionPixelSize(R.dimen.content_padding)
	private val columnGap = AndroidUtils.dpToPxF(app, 8f).roundToInt()
	private val rowGap = AndroidUtils.dpToPxF(app, 6f).roundToInt()
	val radius = AndroidUtils.dpToPxF(app, 16f)
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtilities.getColor(app, ColorUtilities.getListBgColorId(nightMode)) }
	private val bounds = Rect()

	var tracks: GallerySectionCardTracks? = null

	var liveCards: Map<String, RectF>? = null

	val sectionGap: Int get() = padding

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
			mediaCellOffsets(manager.spanCount, pos - boundary.firstMediaPosition, boundary.lastPosition - boundary.firstMediaPosition,
				parent.layoutDirection == View.LAYOUT_DIRECTION_RTL, outRect)
		}
	}

	fun mediaCellOffsets(span: Int, index: Int, last: Int, rtl: Boolean, outRect: Rect) {
		val column = index % span
		val leading = padding + column * (columnGap - 2 * padding) / span
		val trailing = padding + (span - column - 1) * (columnGap - 2 * padding) / span
		if (rtl) {
			outRect.left = trailing; outRect.right = leading
		} else { outRect.left = leading; outRect.right = trailing }
		outRect.top = if (index < span) padding else rowGap
		outRect.bottom = if (index / span == last / span) padding else 0
	}

	fun cardBounds(parent: RecyclerView, child: View, boundary: GallerySectionBoundary, out: RectF) {
		parent.getDecoratedBoundsWithMargins(child, bounds)
		val gap = if (boundary.hasGapAbove) padding else 0
		out.set(parent.paddingLeft.toFloat(), (bounds.top + gap).toFloat(), (parent.width - parent.paddingRight).toFloat(), bounds.bottom.toFloat())
	}

	fun openEdges(parent: RecyclerView, boundary: GallerySectionBoundary): Pair<Boolean, Boolean> =
		(parent.findViewHolderForAdapterPosition(boundary.firstPosition) == null) to
			(parent.findViewHolderForAdapterPosition(boundary.lastPosition) == null)

	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		tracks?.let { it.draw(canvas, paint); return }
		(liveCards ?: staticCards(parent)).values.forEach { canvas.drawRoundRect(it, radius, radius, paint) }
	}

	fun staticCards(parent: RecyclerView): Map<String, RectF> {
		val adapter = parent.adapter as? GalleryGridAdapter ?: return emptyMap()
		val sections = linkedMapOf<String, RectF>()
		val card = RectF()
		for (index in 0 until parent.childCount) {
			val child = parent.getChildAt(index)
			val pos = parent.getChildAdapterPosition(child)
			val boundary = adapter.getSectionBoundary(pos) ?: continue
			cardBounds(parent, child, boundary, card)
			card.offset(0f, child.translationY)
			val rect = sections.getOrPut(boundary.sectionId) { RectF(card) }
			rect.union(card)
			val (topOpen, bottomOpen) = openEdges(parent, boundary)
			// Keep corners beyond the viewport when the real section edge is not laid out.
			if (topOpen) rect.top = minOf(rect.top, -radius)
			if (bottomOpen) rect.bottom = maxOf(rect.bottom, parent.height + radius)
		}
		return sections
	}
}
