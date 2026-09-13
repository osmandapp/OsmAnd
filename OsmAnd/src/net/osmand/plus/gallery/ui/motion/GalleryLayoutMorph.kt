package net.osmand.plus.gallery.ui.motion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.GallerySectionCardDecoration
import net.osmand.plus.gallery.ui.holders.MorphableMediaHolder

class GalleryLayoutMorph(
	private val recyclerView: RecyclerView,
	private val adapter: GalleryGridAdapter,
	private val cards: GallerySectionCardDecoration?
) {

	private class Snapshotted(val bounds: Rect, val bitmap: Bitmap?, val color: Int)

	private class Capture(
		val key: String,
		val view: View,
		val bounds: Rect,
		val card: RectF?,
		val sectionId: String?,
		val media: MorphableMediaHolder?,
		val breathes: Boolean,
		val previewBounds: Rect?,
		val snapshot: Bitmap?,
		val previewBitmap: Bitmap?,
		val centerIcon: Drawable?,
		val showsScrim: Boolean,
		val durationLabel: String?,
		val showsDuration: Boolean,
		val durationTextColor: Int,
		val bgColor: Int,
		val selection: List<Snapshotted>,
		val content: List<Snapshotted>
	)

	private abstract class Piece(val delay: Long, val duration: Long) {
		abstract fun apply(p: Float, elapsed: Long)
	}

	private class MorphTarget(val holder: MorphableMediaHolder) {
		var revealed = false
	}

	private val startStates = LinkedHashMap<String, Capture>()
	private val startOpenEdges = HashMap<String, Pair<Boolean, Boolean>>()
	private val pieces = mutableListOf<Piece>()
	private val overlayViews = mutableListOf<View>()
	private val transformedViews = mutableListOf<View>()
	private val hiddenViews = mutableListOf<View>()
	private val morphTargets = mutableListOf<MorphTarget>()
	private var timeline: ValueAnimator? = null
	private var elapsed = 0L
	private var onComplete: (() -> Unit)? = null
	private var crossMode = false
	private var wasAtTop = false
	private var anchorKey: String? = null
	private var anchorTop = 0
	private var finished = false

	private val touchBlocker = object : RecyclerView.SimpleOnItemTouchListener() {
		override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent) = true
	}

	val isRunning: Boolean get() = !finished

	fun run(newItems: List<GalleryItem>, crossMode: Boolean, apply: () -> Unit, onComplete: () -> Unit) {
		this.onComplete = onComplete
		this.crossMode = crossMode
		if (recyclerView.width == 0 || recyclerView.height == 0) {
			apply()
			finish()
			return
		}
		captureStart()
		apply()
		anchorScroll(newItems)
		recyclerView.addOnItemTouchListener(touchBlocker)
		recyclerView.doOnPreDraw { animate() }
	}

	fun cancel() {
		finish()
	}

	private fun captureStart() {
		val manager = recyclerView.layoutManager as? LinearLayoutManager
		wasAtTop = manager?.findFirstCompletelyVisibleItemPosition() == 0
		for (index in 0 until recyclerView.childCount) {
			val child = recyclerView.getChildAt(index)
			val position = recyclerView.getChildAdapterPosition(child)
			if (position !in 0 until adapter.itemCount) continue
			val item = adapter.getItem(position)
			val capture = capture(child, item, position, withSnapshots = true)
			startStates[capture.key] = capture
			if (anchorKey == null && item is GalleryItem.Media && child.top >= recyclerView.paddingTop) {
				anchorKey = capture.key
				anchorTop = child.top - recyclerView.paddingTop
			}
			val boundary = adapter.getSectionBoundary(position)
			if (cards != null && boundary != null && boundary.sectionId !in startOpenEdges) {
				startOpenEdges[boundary.sectionId] = cards.openEdges(recyclerView, boundary)
			}
		}
	}

	private fun anchorScroll(newItems: List<GalleryItem>) {
		val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
		if (wasAtTop) {
			manager.scrollToPositionWithOffset(0, 0)
			return
		}
		val key = anchorKey ?: return
		val position = newItems.indexOfFirst { it.morphKey() == key }
		if (position >= 0) manager.scrollToPositionWithOffset(position, anchorTop)
	}

	private fun capture(child: View, item: GalleryItem, position: Int, withSnapshots: Boolean): Capture {
		val holder = recyclerView.getChildViewHolder(child)
		val media = holder as? MorphableMediaHolder
		val boundary = adapter.getSectionBoundary(position)
		val card = if (cards != null && boundary != null) RectF().also { cards.cardBounds(recyclerView, child, boundary, it) } else null
		val previewBounds = media?.let { boundsOf(it.previewView) }
		val snapshot = if (!withSnapshots) null else when {
			media != null -> media.morphSnapshotView?.let { snapshot(it) }
			child.height <= recyclerView.height / 4 -> snapshot(child)
			else -> null
		}
		val content = if (withSnapshots && media != null && media.previewView !== child) media.getFadeableContentViews().map(::snapshotted) else emptyList()
		return Capture(
			key = item.morphKey(),
			view = child,
			bounds = boundsOf(child),
			card = card,
			sectionId = boundary?.sectionId,
			media = media,
			breathes = item is GalleryItem.Media || item is GalleryItem.GroupHeader,
			previewBounds = previewBounds,
			snapshot = snapshot,
			previewBitmap = media?.morphPreviewBitmap,
			centerIcon = media?.morphCenterIcon,
			showsScrim = media?.morphShowsScrim == true,
			durationLabel = media?.morphDurationLabel,
			showsDuration = media?.morphShowsDuration == true,
			durationTextColor = media?.morphDurationTextColor ?: Color.WHITE,
			bgColor = media?.morphBgColor ?: 0,
			selection = media?.getSelectionOverlayViews()?.map(::snapshotted) ?: emptyList(),
			content = content
		)
	}

	private fun snapshotted(view: View): Snapshotted {
		val color = (view.background as? ColorDrawable)?.color
		return if (view !is ViewGroup && color != null && view.javaClass == View::class.java) Snapshotted(boundsOf(view), null, color)
		else Snapshotted(boundsOf(view), snapshot(view), Color.TRANSPARENT)
	}

	private fun animate() {
		if (finished) return
		val endStates = LinkedHashMap<String, Capture>()
		for (index in 0 until recyclerView.childCount) {
			val child = recyclerView.getChildAt(index)
			val position = recyclerView.getChildAdapterPosition(child)
			if (position !in 0 until adapter.itemCount) continue
			endStates[adapter.getItem(position).morphKey()] = capture(child, adapter.getItem(position), position, withSnapshots = false)
		}
		val matched = endStates.values.filter { it.key in startStates }
			.sortedWith(compareBy({ startStates[it.key]!!.bounds.top }, { startStates[it.key]!!.bounds.left }))
		val appearing = endStates.values.filter { it.key !in startStates }.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
		val exiting = startStates.values.filter { it.key !in endStates }.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))

		val builder = cards?.let { GallerySectionCardTracks.Builder(it.radius, 0f, recyclerView.height.toFloat()) }
		val appearHandles = appearing.mapIndexed { index, end ->
			builder?.element(null, null, end.sectionId, end.card, GalleryMotion.stagger(index), GalleryMotion.MOVE_DURATION_MS)
		}
		val exitHandles = exiting.mapIndexed { index, start ->
			builder?.element(start.sectionId, start.card, null, null, GalleryMotion.stagger(index), GalleryMotion.MOVE_DURATION_MS)
		}
		val matchedHandles = matched.mapIndexed { index, end ->
			val start = startStates[end.key]!!
			builder?.element(start.sectionId, start.card, end.sectionId, end.card, GalleryMotion.stagger(index), GalleryMotion.MOVE_DURATION_MS)
		}
		if (builder != null && cards != null) {
			startOpenEdges.forEach { (section, open) -> builder.openEdgesBefore(section, open.first, open.second) }
			val seen = HashSet<String>()
			for (index in 0 until recyclerView.childCount) {
				val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))
				val boundary = adapter.getSectionBoundary(position) ?: continue
				if (!seen.add(boundary.sectionId)) continue
				val (top, bottom) = cards.openEdges(recyclerView, boundary)
				builder.openEdgesAfter(boundary.sectionId, top, bottom)
			}
			cards.tracks = builder.build()
		}

		matched.forEachIndexed { index, end ->
			val start = startStates[end.key]!!
			val delay = GalleryMotion.stagger(index)
			if (crossMode && start.media != null && end.media != null) {
				addMediaMorph(start, end, delay, matchedHandles[index])
			} else {
				addCellMorph(end.view, start.bounds, end.bounds, delay, end.breathes)
			}
		}
		appearing.forEachIndexed { index, end -> addAppear(end.view, end.bounds, GalleryMotion.stagger(index), appearHandles[index], end.breathes) }
		exiting.forEachIndexed { index, start -> addExit(start, GalleryMotion.stagger(index), exitHandles[index]) }

		if (pieces.isEmpty()) {
			finish()
			return
		}
		val total = pieces.maxOf { it.delay + it.duration }
		tick(0L)
		timeline = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = total
			interpolator = LinearInterpolator()
			addUpdateListener { tick((it.animatedFraction * total).toLong()) }
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					if (timeline === animation) finish()
				}
			})
			start()
		}
	}

	private fun tick(elapsed: Long) {
		this.elapsed = elapsed
		for (index in pieces.indices) pieces[index].let { it.apply(GalleryMotion.progress(elapsed, it.delay, it.duration), elapsed) }
		cards?.tracks?.update(elapsed)
		recyclerView.invalidate()
	}

	private fun addCellMorph(view: View, start: Rect, end: Rect, delay: Long, breathes: Boolean) {
		if (end.width() == 0 || end.height() == 0 || start.width() == 0 || start.height() == 0) {
			addAppear(view, end, delay, null, breathes)
			return
		}
		transformedViews += view
		pieces += BoundsPiece(view, start, end, end, delay)
	}

	private fun addAppear(view: View, bounds: Rect, delay: Long, handle: GallerySectionCardTracks.Handle?, breathes: Boolean) {
		transformedViews += view
		val real = handle?.end
		val other = handle?.start
		if (real != null && other != null && other != real) {
			pieces += TravelPiece(view, bounds.top, real, other, real, appear = true, breathes = breathes, delay = delay)
		} else {
			pieces += BreathePiece(view, appear = true, breathes = breathes, delay = delay)
		}
	}

	private fun addExit(start: Capture, delay: Long, handle: GallerySectionCardTracks.Handle?) {
		if (start.snapshot == null && start.media == null) return
		val bounds = if (start.media != null) start.previewBounds ?: start.bounds else start.bounds
		val view = overlayBase(start.snapshot, if (start.media != null) start.bgColor else Color.TRANSPARENT, bounds)
		val real = handle?.start
		val other = handle?.end
		if (real != null && other != null && other != real) {
			pieces += TravelPiece(view, bounds.top, real, real, other, appear = false, breathes = start.breathes, delay = delay)
		} else {
			pieces += BreathePiece(view, appear = false, breathes = start.breathes, delay = delay)
		}
	}

	private fun addMediaMorph(start: Capture, end: Capture, delay: Long, handle: GallerySectionCardTracks.Handle?) {
		val target = end.media!!.previewView
		val startBounds = start.previewBounds ?: start.bounds
		val endBounds = boundsOf(target)
		if (startBounds.width() == 0 || startBounds.height() == 0 || endBounds.width() == 0 || endBounds.height() == 0) {
			addAppear(end.view, end.bounds, delay, null, end.breathes)
			return
		}
		val rowStart = handle?.start
		val rowEnd = handle?.end
		if (target !== end.view && rowStart != null && rowEnd != null && rowStart.top != rowEnd.top) {
			val row = end.view
			transformedViews += row
			pieces += object : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
				override fun apply(p: Float, elapsed: Long) {
					row.translationY = rowStart.top + (rowEnd.top - rowStart.top) * p - rowEnd.top
				}
			}
		}
		val base = overlayBase(start.snapshot, start.bgColor, startBounds)
		pieces += BoundsPiece(base, startBounds, endBounds, startBounds, delay)
		val sharpView = ImageView(recyclerView.context).apply {
			scaleType = ImageView.ScaleType.CENTER_CROP
			alpha = 0f
		}
		addOverlay(sharpView, endBounds)
		pieces += BoundsPiece(sharpView, startBounds, endBounds, endBounds, delay)
		if (start.showsScrim) {
			val scrim = View(recyclerView.context).apply { setBackgroundColor(Color.argb(VIDEO_SCRIM_ALPHA, 0, 0, 0)) }
			addOverlay(scrim, startBounds)
			pieces += BoundsPiece(scrim, startBounds, endBounds, startBounds, delay)
		}
		var iconHeight = 0
		var iconView: ImageView? = null
		start.centerIcon?.let { icon ->
			val copy = icon.constantState?.newDrawable()?.mutate() ?: icon
			val width = copy.intrinsicWidth.coerceAtLeast(1)
			val height = copy.intrinsicHeight.coerceAtLeast(1)
			iconHeight = height
			val iconBounds = centered(startBounds, width, height)
			val view = ImageView(recyclerView.context).apply { setImageDrawable(copy) }
			iconView = view
			addOverlay(view, iconBounds)
			pieces += CenterPiece(view, startBounds, endBounds, iconBounds, delay)
		}
		val morphEnd = delay + GalleryMotion.MOVE_DURATION_MS
		val morphTarget = MorphTarget(end.media)
		morphTargets += morphTarget
		end.media.beginMorph(start.previewBitmap) { sharp ->
			if (finished || sharp === start.previewBitmap || morphEnd - elapsed < CROSSFADE_MIN_MS) return@beginMorph
			sharpView.setImageBitmap(sharp)
			val fadeStart = maxOf(elapsed, morphEnd - GalleryMotion.FADE_DURATION_MS)
			val placeholderIcon = if (start.snapshot == null) iconView else null
			pieces += object : Piece(fadeStart, morphEnd - fadeStart) {
				override fun apply(p: Float, elapsed: Long) {
					sharpView.alpha = p
					placeholderIcon?.alpha = 1f - p
				}
			}
			morphTarget.revealed = true
		}
		addSelectionOverlays(start.selection, startBounds, startBounds, endBounds, delay, appear = false)
		addSelectionOverlays(end.selection, endBounds, startBounds, endBounds, delay, appear = true)
		if (start.content.isNotEmpty()) {
			val slide = if (rowStart != null && rowEnd != null) rowEnd.top - rowStart.top else 0f
			for (piece in start.content) {
				val view = overlayBase(piece.bitmap, piece.color, piece.bounds)
				pieces += object : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
					override fun apply(p: Float, elapsed: Long) {
						view.translationY = slide * p
						view.alpha = 1f - GalleryMotion.progress(elapsed, delay, GalleryMotion.FADE_DURATION_MS)
					}
				}
			}
		}
		val label = start.durationLabel
		if (label != null && (start.showsDuration || end.showsDuration)) {
			val labelView = TextView(recyclerView.context).apply {
				text = label
				gravity = Gravity.CENTER
				setTextColor(start.durationTextColor)
				setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.default_sub_text_size))
				alpha = if (start.showsDuration) 1f else 0f
			}
			labelView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
			val from = durationBounds(startBounds, labelView.measuredWidth, labelView.measuredHeight, iconHeight)
			val to = durationBounds(endBounds, labelView.measuredWidth, labelView.measuredHeight, iconHeight)
			addOverlay(labelView, from)
			val fromAlpha = if (start.showsDuration) 1f else 0f
			val toAlpha = if (end.showsDuration) 1f else 0f
			pieces += object : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
				override fun apply(p: Float, elapsed: Long) {
					labelView.translationX = (to.left - from.left) * p
					labelView.translationY = (to.top - from.top) * p
					labelView.alpha = fromAlpha + (toAlpha - fromAlpha) * p
				}
			}
		}
		target.alpha = 0f
		hiddenViews += target
		val slidePx = recyclerView.resources.displayMetrics.density * CONTENT_SLIDE_DP
		end.media.getFadeableContentViews().forEach { content ->
			transformedViews += content
			val slide = (content.parent as? View)?.let { content.right < it.width } != false
			pieces += object : Piece(CONTENT_FADE_BASE_DELAY_MS + delay, GalleryMotion.FADE_DURATION_MS) {
				override fun apply(p: Float, elapsed: Long) {
					content.alpha = p
					if (slide) content.translationX = slidePx * (1f - p)
				}
			}
		}
	}

	private fun addSelectionOverlays(affordances: List<Snapshotted>, own: Rect, from: Rect, to: Rect, delay: Long, appear: Boolean) {
		for (piece in affordances) {
			val view = overlayBase(piece.bitmap, piece.color, piece.bounds)
			val fullCell = piece.bounds.width() >= own.width() - 1 && piece.bounds.height() >= own.height() - 1
			pieces += if (fullCell) BoundsPiece(view, from, to, piece.bounds, delay)
			else CornerPiece(view, own, from, to, piece.bounds, delay)
			pieces += FadePiece(view, appear, delay)
		}
	}

	private fun overlayBase(snapshot: Bitmap?, bgColor: Int, bounds: Rect): View {
		val view = if (snapshot != null) {
			ImageView(recyclerView.context).apply {
				setImageBitmap(snapshot)
				scaleType = ImageView.ScaleType.FIT_XY
			}
		} else {
			View(recyclerView.context).apply { setBackgroundColor(bgColor) }
		}
		addOverlay(view, bounds)
		return view
	}

	private fun addOverlay(view: View, bounds: Rect) {
		view.layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
		recyclerView.overlay.add(view)
		overlayViews += view
	}

	private fun finish() {
		if (finished) return
		finished = true
		timeline?.let { animator ->
			timeline = null
			animator.cancel()
		}
		recyclerView.removeOnItemTouchListener(touchBlocker)
		overlayViews.forEach { recyclerView.overlay.remove(it) }
		overlayViews.clear()
		transformedViews.forEach { view ->
			view.alpha = 1f
			view.scaleX = 1f
			view.scaleY = 1f
			view.translationX = 0f
			view.translationY = 0f
			view.pivotX = view.width / 2f
			view.pivotY = view.height / 2f
		}
		transformedViews.clear()
		hiddenViews.forEach { it.alpha = 1f; it.invalidate() }
		hiddenViews.clear()
		morphTargets.forEach { it.holder.endMorph(it.revealed) }
		morphTargets.clear()
		startStates.values.forEach { capture ->
			capture.snapshot?.recycle()
			(capture.selection + capture.content).forEach { it.bitmap?.recycle() }
		}
		startStates.clear()
		pieces.clear()
		cards?.let {
			if (it.tracks != null) {
				it.tracks = null
				recyclerView.invalidate()
			}
		}
		onComplete?.invoke()
		onComplete = null
	}

	private class BoundsPiece(val view: View, val from: Rect, val to: Rect, val laidOutAt: Rect, delay: Long) : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
		init {
			view.pivotX = 0f
			view.pivotY = 0f
		}

		override fun apply(p: Float, elapsed: Long) {
			val left = from.left + (to.left - from.left) * p
			val top = from.top + (to.top - from.top) * p
			val width = from.width() + (to.width() - from.width()) * p
			val height = from.height() + (to.height() - from.height()) * p
			view.translationX = left - laidOutAt.left
			view.translationY = top - laidOutAt.top
			view.scaleX = width / laidOutAt.width()
			view.scaleY = height / laidOutAt.height()
		}
	}

	private class CenterPiece(val view: View, val from: Rect, val to: Rect, val laidOutAt: Rect, delay: Long) : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
		override fun apply(p: Float, elapsed: Long) {
			val cx = from.exactCenterX() + (to.exactCenterX() - from.exactCenterX()) * p
			val cy = from.exactCenterY() + (to.exactCenterY() - from.exactCenterY()) * p
			view.translationX = cx - laidOutAt.exactCenterX()
			view.translationY = cy - laidOutAt.exactCenterY()
		}
	}

	private class BreathePiece(val view: View, val appear: Boolean, val breathes: Boolean, delay: Long) : Piece(delay, GalleryMotion.FADE_DURATION_MS) {
		init {
			view.pivotX = view.width / 2f
			view.pivotY = view.height / 2f
		}

		override fun apply(p: Float, elapsed: Long) {
			val visible = if (appear) p else 1f - p
			view.alpha = visible
			val scale = if (breathes) GalleryMotion.APPEAR_SCALE + (1f - GalleryMotion.APPEAR_SCALE) * visible else 1f
			view.scaleX = scale
			view.scaleY = scale
		}
	}

	private class FadePiece(val view: View, val appear: Boolean, delay: Long) : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
		override fun apply(p: Float, elapsed: Long) {
			view.alpha = if (appear) {
				GalleryMotion.progress(elapsed, delay + duration - GalleryMotion.FADE_DURATION_MS, GalleryMotion.FADE_DURATION_MS)
			} else {
				1f - GalleryMotion.progress(elapsed, delay, GalleryMotion.FADE_DURATION_MS)
			}
		}
	}

	private class CornerPiece(val view: View, own: Rect, val from: Rect, val to: Rect, val laidOutAt: Rect, delay: Long) : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
		private val anchorRight = own.right - laidOutAt.right < laidOutAt.left - own.left
		private val offsetX = if (anchorRight) own.right - laidOutAt.right else laidOutAt.left - own.left
		private val offsetY = laidOutAt.top - own.top

		override fun apply(p: Float, elapsed: Long) {
			val left = from.left + (to.left - from.left) * p
			val right = from.right + (to.right - from.right) * p
			val top = from.top + (to.top - from.top) * p
			view.translationX = (if (anchorRight) right - offsetX - laidOutAt.right else left + offsetX - laidOutAt.left)
			view.translationY = top + offsetY - laidOutAt.top
		}
	}

	private class TravelPiece(
		val view: View, val viewTop: Int, val real: RectF, val from: RectF, val to: RectF, val appear: Boolean, val breathes: Boolean, delay: Long
	) : Piece(delay, GalleryMotion.MOVE_DURATION_MS) {
		init {
			view.pivotX = view.width / 2f
			view.pivotY = real.top - viewTop
		}

		override fun apply(p: Float, elapsed: Long) {
			val top = from.top + (to.top - from.top) * p
			val height = from.height() + (to.height() - from.height()) * p
			val breathing = when {
				!breathes -> 1f
				appear -> GalleryMotion.APPEAR_SCALE + (1f - GalleryMotion.APPEAR_SCALE) * p
				else -> 1f - (1f - GalleryMotion.APPEAR_SCALE) * p
			}
			view.translationY = top - real.top
			view.scaleY = if (real.height() > 0f) height / real.height() * breathing else breathing
			view.scaleX = breathing
			view.alpha = if (appear) {
				GalleryMotion.progress(elapsed, delay + duration - GalleryMotion.FADE_DURATION_MS, GalleryMotion.FADE_DURATION_MS)
			} else {
				1f - GalleryMotion.progress(elapsed, delay, GalleryMotion.FADE_DURATION_MS)
			}
		}
	}

	private fun durationBounds(container: Rect, width: Int, height: Int, iconHeight: Int): Rect {
		val icon = if (iconHeight > 0) iconHeight else recyclerView.resources.getDimensionPixelSize(R.dimen.standard_icon_size)
		val gap = recyclerView.resources.displayMetrics.density * DURATION_GAP_DP
		val left = container.centerX() - width / 2
		val top = (container.centerY() + icon / 2f + gap).toInt()
		return Rect(left, top, left + width, top + height)
	}

	private fun centered(container: Rect, width: Int, height: Int): Rect {
		val left = container.centerX() - width / 2
		val top = container.centerY() - height / 2
		return Rect(left, top, left + width, top + height)
	}

	private fun boundsOf(view: View): Rect {
		val rect = Rect(0, 0, view.width, view.height)
		if (!view.matrix.isIdentity) {
			val visual = RectF(rect)
			view.matrix.mapRect(visual)
			visual.round(rect)
		}
		recyclerView.offsetDescendantRectToMyCoords(view, rect)
		return rect
	}

	private fun snapshot(view: View): Bitmap? {
		val width = view.width
		val height = view.height
		if (width <= 0 || height <= 0) return null
		return try {
			val bitmap = createBitmap(width, height)
			view.draw(Canvas(bitmap))
			bitmap
		} catch (e: OutOfMemoryError) {
			null
		} catch (e: RuntimeException) {
			null
		}
	}

	companion object {
		private const val CONTENT_FADE_BASE_DELAY_MS = 80L
		private const val CONTENT_SLIDE_DP = 8f

		private const val CROSSFADE_MIN_MS = 100L
		private const val VIDEO_SCRIM_ALPHA = 77
		private const val DURATION_GAP_DP = 2f

		fun GalleryItem.morphKey(): String = when (this) {
			is GalleryItem.Media -> "media:" + mediaItem.id
			is GalleryItem.GroupHeader -> "header:" + type.name
			is GalleryItem.Action -> "action:" + action.id
			else -> javaClass.simpleName
		}
	}
}
