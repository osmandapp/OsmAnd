package net.osmand.plus.gallery.ui.motion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.RectF
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator
import net.osmand.plus.gallery.ui.GallerySectionCardDecoration
import net.osmand.plus.utils.AndroidUtils
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class GridPinchController(
	private val app: OsmandApplication,
	private val recyclerView: RecyclerView,
	private val adapter: GalleryGridAdapter,
	private val controller: GalleryGridController,
	private val cards: GallerySectionCardDecoration?,
	private val itemDecorator: GalleryGridItemDecorator?,
	private val resizableViewWidth: Int?,
	private val listener: Listener
) {
	interface Listener {
		fun isPortrait(): Boolean

		fun setScrollLocked(locked: Boolean)

		fun setExtraLayoutSpace(px: Int)

		fun commitSpan(span: Int, anchorPosition: Int, anchorOffset: Int, onLaidOut: () -> Unit)

		fun switchDisplayMode(mode: GalleryDisplayMode)
	}

	private class Captured(val position: Int, val view: View, val decorated: RectF)

	private class Layout {
		val decorated = HashMap<Int, RectF>()
		val views = HashMap<Int, RectF>()
		val cards = LinkedHashMap<String, RectF>()
		var height = 0f
	}

	val isActive: Boolean get() = active || settle != null

	private var active = false
	private var listPinch = false
	private var discrete = false
	private var stepped = false
	private var captured: List<Captured> = emptyList()
	private var captureRequested = false
	private val layouts = HashMap<Int, Layout>()
	private var baseSpan = 0
	private var minSpan = 0
	private var stepLower = 0
	private var stepUpper = 0
	private var stepRange = 0f..0f
	private var scale = 1f
	private var spanF = 0f
	private var listSupported = false
	private var focalPosition = -1
	private var focalX = 0f
	private var focalY = 0f
	private var focalRatio = 0f
	private var focalRealTop = 0f
	private var settle: ValueAnimator? = null
	private val heights = HashMap<Int, Int>()
	private val scratch = Rect()

	fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
		if (stepped) return false
		val portrait = listener.isPortrait()
		val bounds = controller.getSpanBounds(portrait)
		if (bounds.first >= bounds.last) return false
		listSupported = controller.isListModeSupported()
		if (controller.getDisplayMode() == GalleryDisplayMode.LIST) {
			if (!listSupported) return false
			listPinch = true
			scale = 1f
			return true
		}
		settle?.let { animator ->
			settle = null
			animator.cancel()
			active = true
			scale = baseSpan / spanF
			return true
		}
		if (active) return true
		active = true
		scale = 1f
		focalX = detector.focusX
		focalY = detector.focusY
		baseSpan = controller.getSpanCount(portrait)
		spanF = baseSpan.toFloat()
		minSpan = minOf(bounds.first, baseSpan)
		stepLower = maxOf(minSpan, baseSpan - 1)
		stepUpper = minOf(maxOf(bounds.last, baseSpan), baseSpan + 1)
		stepRange = (if (listSupported && baseSpan == minSpan) minSpan - LIST_ZONE else stepLower.toFloat())..stepUpper.toFloat()
		listener.setScrollLocked(true)
		if (!GalleryMotion.animationsEnabled(app)) {
			discrete = true
			return true
		}
		layouts.clear()
		captured = emptyList()
		captureRequested = true
		listener.setExtraLayoutSpace(recyclerView.height)
		recyclerView.doOnPreDraw {
			if (captureRequested) capture()
		}
		return true
	}

	fun onScale(detector: ScaleGestureDetector): Boolean {
		if (listPinch) {
			scale *= detector.scaleFactor
			if (scale <= LIST_TO_GRID_SCALE) {
				listPinch = false
				stepped = true
				listener.switchDisplayMode(GalleryDisplayMode.GRID)
			}
			return true
		}
		if (!active) return true
		scale *= detector.scaleFactor
		spanF = (baseSpan / scale).coerceIn(stepRange)
		if (captured.isNotEmpty()) apply(spanF)
		return true
	}

	fun onScaleEnd() {
		if (listPinch) {
			listPinch = false
			return
		}
		if (!active) return
		active = false
		captureRequested = false
		val target = settleTarget()
		if (target == null) {
			switchToList()
			return
		}
		if (target != baseSpan) stepped = true
		if (discrete || captured.isEmpty()) {
			discrete = false
			listener.setExtraLayoutSpace(0)
			if (target == baseSpan) {
				listener.setScrollLocked(false)
				return
			}
			persist(target)
			val first = recyclerView.getChildAt(0)
			val offset = if (first == null) 0 else {
				recyclerView.getDecoratedBoundsWithMargins(first, scratch)
				scratch.top - recyclerView.paddingTop
			}
			listener.commitSpan(target, firstVisiblePosition(), offset) { listener.setScrollLocked(false) }
			return
		}
		val distance = abs(target - spanF)
		if (distance < 0.01f) {
			commit(target)
			return
		}
		val from = spanF
		settle = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = (SETTLE_DURATION_MS * (distance / 0.5f)).toLong().coerceIn(SETTLE_MIN_DURATION_MS, SETTLE_DURATION_MS)
			interpolator = GalleryMotion.CURVE
			addUpdateListener {
				spanF = from + (target - from) * it.animatedFraction
				apply(spanF)
			}
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					if (settle === animation) {
						settle = null
						commit(target)
					}
				}
			})
			start()
		}
	}

	fun onGestureFinished() {
		if (active) onScaleEnd()
		stepped = false
		listPinch = false
		if (!isActive) listener.setScrollLocked(false)
	}

	private fun settleTarget(): Int? {
		if (listSupported && spanF <= minSpan - LIST_ZONE) return null
		return spanF.roundToInt().coerceIn(stepLower, stepUpper)
	}

	private fun switchToList() {
		stepped = true
		listener.switchDisplayMode(GalleryDisplayMode.LIST)
		reset()
	}

	private fun capture() {
		captureRequested = false
		val list = mutableListOf<Captured>()
		var focal: Captured? = null
		var nearest = Float.MAX_VALUE
		for (index in 0 until recyclerView.childCount) {
			val child = recyclerView.getChildAt(index)
			val position = recyclerView.getChildAdapterPosition(child)
			if (position !in 0 until adapter.itemCount) continue
			recyclerView.getDecoratedBoundsWithMargins(child, scratch)
			val item = Captured(position, child, RectF(scratch))
			list += item
			if (adapter.getItem(position) !is GalleryItem.Media) heights[position] = child.height
			val distance = if (item.decorated.contains(focalX, focalY)) 0f
			else abs(item.decorated.centerY() - focalY)
			if (distance < nearest) {
				nearest = distance
				focal = item
			}
		}
		val anchor = focal ?: return
		captured = list
		focalPosition = anchor.position
		focalRealTop = anchor.decorated.top
		focalRatio = (focalY - anchor.decorated.top) / anchor.decorated.height().coerceAtLeast(1f)
		apply(spanF)
	}

	private fun apply(value: Float) {
		val zoom = if (value < minSpan) minSpan / value else 1f
		val clamped = value.coerceAtLeast(minSpan.toFloat())
		val lower = floor(clamped).toInt().coerceAtLeast(1)
		val upper = lower + 1
		val fraction = (clamped - lower).coerceIn(0f, 1f)
		val lowerLayout = layoutFor(lower)
		val upperLayout = layoutFor(upper)
		val lowerShift = screenShift(lowerLayout, lower)
		val upperShift = screenShift(upperLayout, upper)
		val rect = RectF()
		fun place(lowerRect: RectF, upperRect: RectF, out: RectF) {
			out.set(
				lowerRect.left + (upperRect.left - lowerRect.left) * fraction,
				lowerRect.top + lowerShift + (upperRect.top + upperShift - lowerRect.top - lowerShift) * fraction,
				lowerRect.right + (upperRect.right - lowerRect.right) * fraction,
				lowerRect.bottom + lowerShift + (upperRect.bottom + upperShift - lowerRect.bottom - lowerShift) * fraction
			)
			if (zoom != 1f) {
				out.set(focalX + (out.left - focalX) * zoom, focalY + (out.top - focalY) * zoom,
					focalX + (out.right - focalX) * zoom, focalY + (out.bottom - focalY) * zoom)
			}
		}
		for (item in captured) {
			val lowerRect = lowerLayout.views[item.position] ?: continue
			val upperRect = upperLayout.views[item.position] ?: continue
			place(lowerRect, upperRect, rect)
			val view = item.view
			view.pivotX = 0f
			view.pivotY = 0f
			view.translationX = rect.left - view.left
			view.translationY = rect.top - view.top
			view.scaleX = if (view.width > 0) rect.width() / view.width else 1f
			view.scaleY = if (view.height > 0) rect.height() / view.height else 1f
		}
		cards?.let { decoration ->
			val live = LinkedHashMap<String, RectF>()
			for ((section, lowerCard) in lowerLayout.cards) {
				val upperCard = upperLayout.cards[section] ?: continue
				live[section] = RectF().also { place(lowerCard, upperCard, it) }
			}
			decoration.liveCards = live
		}
		recyclerView.invalidate()
	}

	private fun screenShift(layout: Layout, span: Int): Float {
		val focal = layout.decorated[focalPosition] ?: return 0f
		val screenTop = if (span == baseSpan) focalRealTop else focalY - focalRatio * focal.height()
		val top = recyclerView.paddingTop.toFloat()
		val bottom = (recyclerView.height - recyclerView.paddingBottom).toFloat()
		return (screenTop - focal.top).coerceAtLeast(bottom - layout.height).coerceAtMost(top)
	}

	private fun layoutFor(span: Int): Layout = layouts.getOrPut(span) {
		val layout = Layout()
		val rtl = recyclerView.layoutDirection == View.LAYOUT_DIRECTION_RTL
		val left = recyclerView.paddingLeft.toFloat()
		val contentWidth = recyclerView.width - recyclerView.paddingLeft - recyclerView.paddingRight
		val borders = gridBorders(span, contentWidth)
		val cellSize = controller.resolveSpanResizableSize(resizableViewWidth, span)
		val insets = Rect()
		var y = 0f
		var rowTop = 0f
		for (position in 0 until adapter.itemCount) {
			val item = adapter.getItem(position)
			val boundary = adapter.getSectionBoundary(position)
			val decorated: RectF
			if (item is GalleryItem.Media && boundary != null) {
				val index = position - boundary.firstMediaPosition
				val last = boundary.lastPosition - boundary.firstMediaPosition
				var column = index % span
				if (rtl) column = span - 1 - column
				cellInsets(span, index, last, rtl, insets)
				if (index % span == 0) rowTop = y
				decorated = RectF(left + borders[column], rowTop, left + borders[column + 1], rowTop + insets.top + cellSize + insets.bottom)
				layout.views[position] = RectF(decorated.left + insets.left, decorated.top + insets.top,
					decorated.left + insets.left + cellSize, decorated.top + insets.top + cellSize)
				if (index % span == span - 1 || index == last) y = decorated.bottom
			} else {
				val gap = if (boundary?.hasGapAbove == true) cards?.sectionGap ?: 0 else 0
				val height = heights[position] ?: defaultHeight(item)
				decorated = RectF(left, y, left + contentWidth, y + gap + height)
				layout.views[position] = RectF(left, y + gap, left + contentWidth, y + gap + height)
				y = decorated.bottom
			}
			layout.decorated[position] = decorated
			if (boundary != null && cards != null) {
				val gap = if (boundary.hasGapAbove) cards.sectionGap else 0
				val card = RectF(left, decorated.top + gap, left + contentWidth, decorated.bottom)
				layout.cards.getOrPut(boundary.sectionId) { RectF(card) }.union(card)
			}
		}
		layout.height = y
		layout
	}

	private fun cellInsets(span: Int, index: Int, last: Int, rtl: Boolean, out: Rect) {
		val cards = cards
		if (cards != null) {
			cards.mediaCellOffsets(span, index, last, rtl, out)
		} else {
			val inset = itemDecorator?.spanResizableInset ?: 0
			out.set(inset, inset, inset, inset)
		}
	}

	private fun gridBorders(span: Int, totalSpace: Int): IntArray {
		val borders = IntArray(span + 1)
		val sizePerSpan = totalSpace / span
		val remainder = totalSpace % span
		var consumed = 0
		var additional = 0
		for (index in 1..span) {
			var size = sizePerSpan
			additional += remainder
			if (additional > 0 && span - additional < remainder) {
				size += 1
				additional -= span
			}
			consumed += size
			borders[index] = consumed
		}
		return borders
	}

	private fun defaultHeight(item: GalleryItem): Int = AndroidUtils.dpToPx(app, when (item) {
		is GalleryItem.Spacer -> 16f
		is GalleryItem.MediaStats -> 60f
		else -> 56f
	})

	private fun persist(span: Int) {
		val portrait = listener.isPortrait()
		if (controller.getSpanCount(portrait) != span) controller.setSpanCount(portrait, span)
	}

	private fun commit(target: Int) {
		persist(target)
		val layout = layoutFor(target)
		val shift = screenShift(layout, target)
		val anchor = layout.decorated[focalPosition]
		val offset = if (anchor != null) (anchor.top + shift - recyclerView.paddingTop).roundToInt() else 0
		val views = captured.map { it.view }
		listener.setExtraLayoutSpace(0)
		listener.commitSpan(target, if (anchor != null) focalPosition else firstVisiblePosition(), offset) {
			views.forEach(::clearTransform)
			cards?.liveCards = null
			recyclerView.invalidate()
			if (!isActive) listener.setScrollLocked(false)
		}
		captured = emptyList()
		layouts.clear()
		heights.clear()
	}

	private fun reset() {
		captured.forEach { clearTransform(it.view) }
		captured = emptyList()
		layouts.clear()
		heights.clear()
		cards?.liveCards = null
		listener.setExtraLayoutSpace(0)
		listener.setScrollLocked(false)
		recyclerView.invalidate()
	}

	private fun clearTransform(view: View) {
		view.translationX = 0f
		view.translationY = 0f
		view.scaleX = 1f
		view.scaleY = 1f
		view.pivotX = view.width / 2f
		view.pivotY = view.height / 2f
	}

	private fun firstVisiblePosition(): Int {
		for (index in 0 until recyclerView.childCount) {
			val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))
			if (position >= 0) return position
		}
		return 0
	}

	companion object {
		private const val SETTLE_DURATION_MS = 200L
		private const val SETTLE_MIN_DURATION_MS = 60L

		private const val LIST_ZONE = 0.5f

		private const val LIST_TO_GRID_SCALE = 0.75f
	}
}
