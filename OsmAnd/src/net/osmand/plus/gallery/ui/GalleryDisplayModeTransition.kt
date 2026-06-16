package net.osmand.plus.gallery.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.gallery.ui.holders.GalleryMediaListViewHolder
import net.osmand.plus.gallery.ui.holders.GalleryMediaViewHolder

class GalleryDisplayModeTransition(private val recyclerView: RecyclerView) {

	private class StartState(
		val bounds: Rect,
		val snapshot: Bitmap? = null,
		val centerIcon: Drawable? = null,
		val bgColor: Int = 0,
		val showsScrim: Boolean = false,
		val durationLabel: String? = null,
		val showsDuration: Boolean = false,
		val durationTextColor: Int = Color.WHITE
	) {
		val hasOverlayVisual: Boolean
			get() = snapshot != null || centerIcon != null
	}

	private val startStates = LinkedHashMap<String, StartState>()
	private val endActions = mutableListOf<() -> Unit>()

	private var toList = false
	private var animatorSet: AnimatorSet? = null
	private var onComplete: (() -> Unit)? = null
	private var finished = false

	private val moveInterpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

	private val touchBlocker = object : RecyclerView.SimpleOnItemTouchListener() {
		override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent) = true
	}

	fun captureStart(toList: Boolean) {
		this.toList = toList
		for (i in 0 until recyclerView.childCount) {
			val child = recyclerView.getChildAt(i)
			when (val holder = recyclerView.getChildViewHolder(child)) {
				is GalleryMediaViewHolder -> if (toList) {
					val id = holder.boundItemId ?: continue
					startStates[id] = startState(holder, child, includePlainSnapshot = true)
				}
				is GalleryMediaListViewHolder -> if (!toList) {
					val id = holder.boundItemId ?: continue
					startStates[id] = startState(holder, holder.previewView, includePlainSnapshot = false)
				}
				else -> {}
			}
		}
	}

	fun start(onComplete: (() -> Unit)? = null) {
		this.onComplete = onComplete
		if (recyclerView.width == 0 || recyclerView.height == 0) {
			finish()
			return
		}
		recyclerView.addOnItemTouchListener(touchBlocker)
		recyclerView.doOnPreDraw { animate() }
	}

	fun cancel() {
		val set = animatorSet
		if (set != null) {
			set.end()
		} else {
			finish()
		}
	}

	private fun animate() {
		if (finished) return

		val animators = mutableListOf<Animator>()
		var matchedIndex = 0
		var appearIndex = 0
		for (i in 0 until recyclerView.childCount) {
			val child = recyclerView.getChildAt(i)
			when (val holder = recyclerView.getChildViewHolder(child)) {
				is GalleryMediaListViewHolder -> if (toList) {
					val state = holder.boundItemId?.let { startStates.remove(it) }
					when {
						state?.hasOverlayVisual == true -> {
							animators += createMediaMorph(
								state, holder.previewView, holder.morphShowsDuration, matchedIndex
							)
							animators += createContentFadeIn(holder.getFadeableContentViews(), matchedIndex)
							matchedIndex++
						}
						state != null -> {
							animators += createCellMorph(state.bounds, holder.previewView, matchedIndex)
							animators += createContentFadeIn(holder.getFadeableContentViews(), matchedIndex)
							matchedIndex++
						}
						else -> animators += createAppear(child, appearIndex++)
					}
				}
				is GalleryMediaViewHolder -> if (!toList) {
					val state = holder.boundItemId?.let { startStates.remove(it) }
					when {
						state?.hasOverlayVisual == true ->
							animators += createMediaMorph(
								state, child, holder.morphShowsDuration, matchedIndex++
							)
						state != null ->
							animators += createCellMorph(state.bounds, child, matchedIndex++)
						else -> animators += createAppear(child, appearIndex++)
					}
				}
				else -> {}
			}
		}

		if (animators.isEmpty()) {
			finish()
			return
		}
		animatorSet = AnimatorSet().apply {
			playTogether(animators)
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) = finish()
			})
			start()
		}
	}

	private fun startState(
		holder: GalleryMediaViewHolder,
		boundsView: View,
		includePlainSnapshot: Boolean
	): StartState {
		val hasChrome = holder.morphCenterIcon != null
				|| holder.morphShowsScrim
				|| holder.morphDurationLabel != null
		val snapshot = if (includePlainSnapshot || hasChrome) {
			holder.morphPreviewSnapshotView?.let { snapshot(it) }
		} else {
			null
		}
		return StartState(
			bounds = boundsOf(boundsView),
			snapshot = snapshot,
			centerIcon = holder.morphCenterIcon,
			bgColor = holder.morphBgColor,
			showsScrim = holder.morphShowsScrim,
			durationLabel = holder.morphDurationLabel,
			showsDuration = holder.morphShowsDuration,
			durationTextColor = holder.morphDurationTextColor
		)
	}

	private fun startState(
		holder: GalleryMediaListViewHolder,
		boundsView: View,
		includePlainSnapshot: Boolean
	): StartState {
		val hasChrome = holder.morphCenterIcon != null
				|| holder.morphShowsScrim
				|| holder.morphDurationLabel != null
		val snapshot = if (includePlainSnapshot || hasChrome) {
			holder.morphPreviewSnapshotView?.let { snapshot(it) }
		} else {
			null
		}
		return StartState(
			bounds = boundsOf(boundsView),
			snapshot = snapshot,
			centerIcon = holder.morphCenterIcon,
			bgColor = holder.morphBgColor,
			showsScrim = holder.morphShowsScrim,
			durationLabel = holder.morphDurationLabel,
			showsDuration = holder.morphShowsDuration,
			durationTextColor = holder.morphDurationTextColor
		)
	}

	private fun createMediaMorph(
		state: StartState,
		target: View,
		targetShowsDuration: Boolean,
		index: Int
	): List<Animator> {
		val start = state.bounds
		val end = boundsOf(target)
		if (start.width() == 0 || start.height() == 0 || end.width() == 0 || end.height() == 0) {
			return listOf(createAppear(target, index))
		}

		val overlayViews = mutableListOf<View>()
		val animators = mutableListOf<Animator>()
		val delay = (index * MOVE_STAGGER_MS).coerceAtMost(MAX_STAGGER_MS)
		val context = recyclerView.context

		val baseView: View = if (state.snapshot != null) {
			ImageView(context).apply {
				setImageBitmap(state.snapshot)
				scaleType = ImageView.ScaleType.FIT_XY
			}
		} else {
			View(context).apply { setBackgroundColor(state.bgColor) }
		}
		baseView.pivotX = 0f
		baseView.pivotY = 0f
		baseView.layout(start.left, start.top, start.right, start.bottom)
		recyclerView.overlay.add(baseView)
		overlayViews += baseView
		animators += createBoundsAnimator(baseView, start, end, delay)

		if (state.showsScrim) {
			val scrimView = View(context).apply {
				setBackgroundColor(Color.argb(VIDEO_SCRIM_ALPHA, 0, 0, 0))
				pivotX = 0f
				pivotY = 0f
				layout(start.left, start.top, start.right, start.bottom)
			}
			recyclerView.overlay.add(scrimView)
			overlayViews += scrimView
			animators += createBoundsAnimator(scrimView, start, end, delay)
		}

		val iconSize = state.centerIcon?.let { icon ->
			val iconCopy = icon.constantState?.newDrawable()?.mutate() ?: icon
			val iconWidth = iconCopy.intrinsicWidth.coerceAtLeast(1)
			val iconHeight = iconCopy.intrinsicHeight.coerceAtLeast(1)
			val iconLeft = start.centerX() - iconWidth / 2
			val iconTop = start.centerY() - iconHeight / 2
			val iconView = ImageView(context).apply {
				setImageDrawable(iconCopy)
				layout(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight)
			}
			recyclerView.overlay.add(iconView)
			overlayViews += iconView
			animators += createCenterMoveAnimator(iconView, start, end, delay)
			Pair(iconWidth, iconHeight)
		}

		val durationView = createDurationView(state, targetShowsDuration, iconSize)
		if (durationView != null) {
			recyclerView.overlay.add(durationView)
			overlayViews += durationView
			animators += createDurationMoveAnimator(
				durationView, state, start, end, targetShowsDuration, iconSize, delay
			)
		}

		target.alpha = 0f
		endActions += {
			overlayViews.forEach { recyclerView.overlay.remove(it) }
			target.alpha = 1f
			target.invalidate()
		}

		return animators
	}

	private fun createBoundsAnimator(view: View, start: Rect, end: Rect, delay: Long): Animator =
		ObjectAnimator.ofPropertyValuesHolder(
			view,
			PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, (end.left - start.left).toFloat()),
			PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, (end.top - start.top).toFloat()),
			PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, end.width() / start.width().toFloat()),
			PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, end.height() / start.height().toFloat())
		).apply {
			duration = MOVE_DURATION_MS
			interpolator = moveInterpolator
			startDelay = delay
		}

	private fun createCenterMoveAnimator(view: View, start: Rect, end: Rect, delay: Long): Animator =
		ObjectAnimator.ofPropertyValuesHolder(
			view,
			PropertyValuesHolder.ofFloat(
				View.TRANSLATION_X, 0f, (end.centerX() - start.centerX()).toFloat()
			),
			PropertyValuesHolder.ofFloat(
				View.TRANSLATION_Y, 0f, (end.centerY() - start.centerY()).toFloat()
			)
		).apply {
			duration = MOVE_DURATION_MS
			interpolator = moveInterpolator
			startDelay = delay
		}

	private fun createDurationView(
		state: StartState,
		targetShowsDuration: Boolean,
		iconSize: Pair<Int, Int>?
	): TextView? {
		val label = state.durationLabel ?: return null
		if (!state.showsDuration && !targetShowsDuration) return null

		val context = recyclerView.context
		val view = TextView(context).apply {
			text = label
			gravity = android.view.Gravity.CENTER
			setTextColor(state.durationTextColor)
			setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				context.resources.getDimension(R.dimen.default_sub_text_size)
			)
			alpha = if (state.showsDuration) 1f else 0f
		}
		view.measure(
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		)
		val position = durationPosition(
			state.bounds, view.measuredWidth, view.measuredHeight, iconSize
		)
		view.layout(position.left, position.top, position.right, position.bottom)
		return view
	}

	private fun createDurationMoveAnimator(
		view: TextView,
		state: StartState,
		start: Rect,
		end: Rect,
		targetShowsDuration: Boolean,
		iconSize: Pair<Int, Int>?,
		delay: Long
	): Animator {
		val startPosition = durationPosition(start, view.width, view.height, iconSize)
		val endPosition = durationPosition(end, view.width, view.height, iconSize)
		val targetAlpha = if (targetShowsDuration) 1f else 0f
		view.alpha = if (state.showsDuration) 1f else 0f
		return ObjectAnimator.ofPropertyValuesHolder(
			view,
			PropertyValuesHolder.ofFloat(
				View.TRANSLATION_X, 0f, (endPosition.left - startPosition.left).toFloat()
			),
			PropertyValuesHolder.ofFloat(
				View.TRANSLATION_Y, 0f, (endPosition.top - startPosition.top).toFloat()
			),
			PropertyValuesHolder.ofFloat(View.ALPHA, targetAlpha)
		).apply {
			duration = MOVE_DURATION_MS
			interpolator = moveInterpolator
			startDelay = delay
		}
	}

	private fun durationPosition(
		bounds: Rect,
		width: Int,
		height: Int,
		iconSize: Pair<Int, Int>?
	): Rect {
		val iconHeight = iconSize?.second
			?: recyclerView.resources.getDimensionPixelSize(R.dimen.standard_icon_size)
		val gap = recyclerView.resources.displayMetrics.density * DURATION_GAP_DP
		val left = bounds.centerX() - width / 2
		val top = (bounds.centerY() + iconHeight / 2f + gap).toInt()
		return Rect(left, top, left + width, top + height)
	}

	private fun createCellMorph(start: Rect, cell: View, index: Int): Animator {
		val end = boundsOf(cell)
		if (end.width() == 0 || end.height() == 0) {
			return createAppear(cell, index)
		}
		cell.pivotX = 0f
		cell.pivotY = 0f
		cell.translationX = (start.left - end.left).toFloat()
		cell.translationY = (start.top - end.top).toFloat()
		cell.scaleX = start.width() / end.width().toFloat()
		cell.scaleY = start.height() / end.height().toFloat()
		endActions += {
			cell.translationX = 0f
			cell.translationY = 0f
			cell.scaleX = 1f
			cell.scaleY = 1f
		}

		return ObjectAnimator.ofPropertyValuesHolder(
			cell,
			PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f),
			PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f),
			PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
			PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
		).apply {
			duration = MOVE_DURATION_MS
			interpolator = moveInterpolator
			startDelay = (index * MOVE_STAGGER_MS).coerceAtMost(MAX_STAGGER_MS)
		}
	}

	private fun createContentFadeIn(views: List<View>, index: Int): List<Animator> {
		val slidePx = recyclerView.resources.displayMetrics.density * CONTENT_SLIDE_DP
		return views.map { view ->
			view.alpha = 0f
			view.translationX = slidePx
			endActions += {
				view.alpha = 1f
				view.translationX = 0f
			}
			ObjectAnimator.ofPropertyValuesHolder(
				view,
				PropertyValuesHolder.ofFloat(View.ALPHA, 1f),
				PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f)
			).apply {
				duration = CONTENT_FADE_DURATION_MS
				interpolator = moveInterpolator
				startDelay = CONTENT_FADE_BASE_DELAY_MS +
						(index * CONTENT_FADE_STAGGER_MS).coerceAtMost(MAX_STAGGER_MS)
			}
		}
	}

	private fun createAppear(view: View, index: Int): Animator {
		view.alpha = 0f
		view.scaleX = APPEAR_SCALE
		view.scaleY = APPEAR_SCALE
		view.pivotX = view.width / 2f
		view.pivotY = view.height / 2f
		endActions += {
			view.alpha = 1f
			view.scaleX = 1f
			view.scaleY = 1f
		}
		return ObjectAnimator.ofPropertyValuesHolder(
			view,
			PropertyValuesHolder.ofFloat(View.ALPHA, 1f),
			PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
			PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
		).apply {
			duration = APPEAR_DURATION_MS
			interpolator = moveInterpolator
			startDelay = APPEAR_BASE_DELAY_MS +
					(index * APPEAR_STAGGER_MS).coerceAtMost(MAX_STAGGER_MS)
		}
	}

	private fun finish() {
		if (finished) return
		finished = true
		recyclerView.removeOnItemTouchListener(touchBlocker)
		endActions.forEach { it() }
		endActions.clear()
		startStates.values.forEach { it.snapshot?.recycle() }
		startStates.clear()
		animatorSet = null
		onComplete?.invoke()
		onComplete = null
	}

	private fun boundsOf(view: View): Rect {
		val rect = Rect(0, 0, view.width, view.height)
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
		}
	}

	companion object {
		private const val MOVE_DURATION_MS = 300L
		private const val MOVE_STAGGER_MS = 15L
		private const val MAX_STAGGER_MS = 120L

		private const val CONTENT_FADE_DURATION_MS = 150L
		private const val CONTENT_FADE_BASE_DELAY_MS = 80L
		private const val CONTENT_FADE_STAGGER_MS = 20L
		private const val CONTENT_SLIDE_DP = 8f

		private const val APPEAR_DURATION_MS = 180L
		private const val APPEAR_BASE_DELAY_MS = 120L
		private const val APPEAR_STAGGER_MS = 15L
		private const val APPEAR_SCALE = 0.92f

		private const val VIDEO_SCRIM_ALPHA = 77
		private const val DURATION_GAP_DP = 2f
	}
}
