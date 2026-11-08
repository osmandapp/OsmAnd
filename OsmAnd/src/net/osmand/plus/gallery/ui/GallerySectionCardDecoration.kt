package net.osmand.plus.gallery.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.gallery.ui.motion.GallerySectionCardTracks
import net.osmand.plus.gallery.ui.motion.OpenEdges
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class GallerySectionCardDecoration(private val app: OsmandApplication, nightMode: Boolean) : RecyclerView.ItemDecoration() {
	val padding = app.resources.getDimensionPixelSize(R.dimen.content_padding)
	val radius = AndroidUtils.dpToPxF(app, CARD_RADIUS_DP)
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtilities.getColor(app, ColorUtilities.getListBgColorId(nightMode)) }
	private val bounds = Rect()
	private var geometry: SectionGridGeometry? = null
	private var geometryWidth = 0

	var tracks: GallerySectionCardTracks? = null

	fun geometry(span: Int, contentWidth: Int): SectionGridGeometry {
		val current = geometry
		if (current != null && current.span == span && geometryWidth == contentWidth) return current
		return SectionGridGeometry.of(app, span, contentWidth).also {
			geometry = it
			geometryWidth = contentWidth
		}
	}

	override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
		val sections = parent.adapter as? GallerySectionSource ?: return
		val pos = parent.getChildAdapterPosition(view)
		if (pos == RecyclerView.NO_POSITION) return
		val boundary = sections.getSectionBoundary(pos) ?: return
		if (sections.isGridCell(pos)) {
			val manager = parent.layoutManager as? GridLayoutManager ?: return
			geometry(manager.spanCount, parent.width - parent.paddingLeft - parent.paddingRight)
				.cellInsets(pos - boundary.firstMediaPosition, boundary.lastPosition - boundary.firstMediaPosition,
					parent.layoutDirection == View.LAYOUT_DIRECTION_RTL, outRect)
		} else if (boundary.hasGapAbove) {
			outRect.top = padding
		}
	}

	fun cardBounds(parent: RecyclerView, child: View, boundary: GallerySectionBoundary, out: RectF) {
		parent.getDecoratedBoundsWithMargins(child, bounds)
		val gap = if (boundary.hasGapAbove) padding else 0
		out.set(parent.paddingLeft.toFloat(), (bounds.top + gap).toFloat(), (parent.width - parent.paddingRight).toFloat(), bounds.bottom.toFloat())
	}

	fun openEdges(parent: RecyclerView, boundary: GallerySectionBoundary) = OpenEdges(
		top = parent.findViewHolderForAdapterPosition(boundary.firstPosition) == null,
		bottom = parent.findViewHolderForAdapterPosition(boundary.lastPosition) == null
	)

	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		tracks?.let { it.draw(canvas, paint); return }
		staticCards(parent).values.forEach { canvas.drawRoundRect(it, radius, radius, paint) }
	}

	fun staticCards(parent: RecyclerView): Map<String, RectF> {
		val source = parent.adapter as? GallerySectionSource ?: return emptyMap()
		val sections = linkedMapOf<String, RectF>()
		val card = RectF()
		for (index in 0 until parent.childCount) {
			val child = parent.getChildAt(index)
			val pos = parent.getChildAdapterPosition(child)
			val boundary = source.getSectionBoundary(pos) ?: continue
			cardBounds(parent, child, boundary, card)
			card.offset(0f, child.translationY)
			val rect = sections.getOrPut(boundary.sectionId) { RectF(card) }
			rect.union(card)
			GalleryMotion.extendOffscreen(rect, openEdges(parent, boundary), 0f, parent.height.toFloat(), radius)
		}
		return sections
	}

	companion object {
		const val CARD_RADIUS_DP = 16f
	}
}
