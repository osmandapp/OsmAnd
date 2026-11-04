package net.osmand.plus.gallery.ui.viewer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.osmand.plus.R
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.utils.AndroidUtils
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

interface MediaViewerPage {
	fun contentRect(): RectF?

	fun canScrollVertically(direction: Int): Boolean

	fun onPreviewSettled()
}

class MediaViewerSheetLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
	FrameLayout(context, attrs), NestedScrollingParent3 {

	interface Listener {
		fun onProgressChanged(progress: Float, dismissProgress: Float)
		fun onDismissed()
	}

	private enum class DragMode { NONE, UNDECIDED, PAGE, MEDIA_BOX, SHEET, DISMISS }

	var pageProvider: () -> MediaViewerPage? = { null }
	var listener: Listener? = null
	var animationsEnabled = true

	var progress = 0f
		private set
	var dismissProgress = 0f
		private set
	var settledState = 0
		private set
	val previewRect = RectF()

	private lateinit var backdrop: View
	private lateinit var mediaContainer: View
	private lateinit var floatingChrome: View
	private lateinit var toolbar: View
	private lateinit var sheet: View
	private lateinit var sheetList: View
	private lateinit var solidAppBar: View

	private val nestedHelper = NestedScrollingParentHelper(this)
	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
	private val appBarHeight = context.resources.getDimensionPixelSize(R.dimen.toolbar_height)
	private val previewBoxHeight = AndroidUtils.dpToPx(context, PREVIEW_BOX_HEIGHT_DP)
	private val sheetRadius = AndroidUtils.dpToPx(context, SHEET_RADIUS_DP)
	private val listInset = AndroidUtils.dpToPx(context, LIST_TOP_INSET_DP) + sheetRadius
	private val hitRect = Rect()

	private var topInset = 0
	private var pendingState: Int? = null
	private var animator: ValueAnimator? = null
	private var velocityTracker: VelocityTracker? = null
	private var dragMode = DragMode.NONE
	private var activePointerId = MotionEvent.INVALID_POINTER_ID
	private var downX = 0f
	private var downY = 0f
	private var dragStartX = 0f
	private var dragStartY = 0f
	private var dragStartTop = 0f
	private var dismissX = 0f
	private var dismissY = 0f
	private var dismissing = false
	private var mediaRect: RectF? = null

	init {
		ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
			applyTopInset(insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()).top)
			insets
		}
	}

	override fun onFinishInflate() {
		super.onFinishInflate()
		backdrop = findViewById(R.id.backdrop)
		mediaContainer = findViewById(R.id.media_container)
		floatingChrome = findViewById(R.id.floating_chrome)
		toolbar = findViewById(R.id.toolbar)
		sheet = findViewById(R.id.details_sheet)
		sheetList = findViewById(R.id.details_list)
		solidAppBar = findViewById(R.id.solid_app_bar)
		(sheetList.layoutParams as MarginLayoutParams).bottomMargin = listInset
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		ViewCompat.requestApplyInsets(this)
	}

	fun setInitialState(state: Int) {
		if (width > 0 && height > 0) snapTo(state) else pendingState = state
	}

	fun animateTo(state: Int) {
		if (dismissing) return
		val target = state.coerceIn(STATE_MEDIA, STATE_FULL)
		cancelAnimator()
		if (!animationsEnabled || width == 0 || height == 0 || target.toFloat() == progress) {
			snapTo(target)
			return
		}
		val from = progress
		animator = ValueAnimator.ofFloat(from, target.toFloat()).apply {
			duration = settleDuration(abs(sheetTop(from) - sheetTop(target.toFloat())))
			interpolator = GalleryMotion.CURVE
			addUpdateListener { setProgress(it.animatedValue as Float) }
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					if (animator === animation) {
						animator = null
						onSettled(target)
					}
				}
			})
			start()
		}
	}

	fun handleBack(): Boolean = when {
		dismissing -> true
		progress > CHROME_SWITCH_PROGRESS -> { animateTo(STATE_PREVIEW); true }
		progress > 0f -> { animateTo(STATE_MEDIA); true }
		else -> false
	}

	private fun settleDuration(distancePx: Float): Long {
		val fraction = (distancePx / (height * SETTLE_FULL_DURATION_TRAVEL)).coerceIn(0f, 1f)
		return GalleryMotion.FADE_DURATION_MS + ((GalleryMotion.MOVE_DURATION_MS - GalleryMotion.FADE_DURATION_MS) * fraction).toLong()
	}

	fun onPageContentChanged() {
		if (progress > 0f || dismissProgress > 0f) {
			mediaRect = null
			applyProgress()
		}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		val pending = pendingState
		if (pending != null) {
			pendingState = null
			snapTo(pending)
			return
		}
		if (changed) mediaRect = null
		applyProgress()
	}

	private fun applyTopInset(inset: Int) {
		if (inset == topInset) return
		topInset = inset
		floatingChrome.setPadding(floatingChrome.paddingLeft, inset, floatingChrome.paddingRight, floatingChrome.paddingBottom)
		solidAppBar.setPadding(solidAppBar.paddingLeft, inset, solidAppBar.paddingRight, solidAppBar.paddingBottom)
		(sheet.layoutParams as LayoutParams).topMargin = inset + appBarHeight - sheetRadius
		mediaRect = null
		requestLayout()
	}

	private val boxHeight: Int
		get() = if (width > height) min(previewBoxHeight, ((height - topInset) * LANDSCAPE_BOX_MAX_HEIGHT).toInt()) else previewBoxHeight

	private val fullTop: Float
		get() = (topInset + appBarHeight - sheetRadius).toFloat()

	private fun sheetTop(p: Float): Float {
		val h = height.toFloat()
		val previewTop = (topInset + boxHeight).toFloat()
		return if (p <= STATE_PREVIEW) h + (previewTop - h) * p else previewTop + (fullTop - previewTop) * (p - STATE_PREVIEW)
	}

	private fun progressForTop(top: Float): Float {
		val h = height.toFloat()
		val previewTop = (topInset + boxHeight).toFloat()
		return if (top >= previewTop) {
			if (h > previewTop) ((h - top) / (h - previewTop)).coerceIn(STATE_MEDIA.toFloat(), STATE_PREVIEW.toFloat()) else STATE_PREVIEW.toFloat()
		} else {
			if (previewTop > fullTop) (STATE_PREVIEW + (previewTop - top) / (previewTop - fullTop)).coerceIn(STATE_PREVIEW.toFloat(), STATE_FULL.toFloat())
			else STATE_FULL.toFloat()
		}
	}

	private fun setProgress(value: Float) {
		progress = value.coerceIn(STATE_MEDIA.toFloat(), STATE_FULL.toFloat())
		applyProgress()
	}

	private fun applyProgress() {
		if (width == 0 || height == 0) return
		val p = progress
		sheet.translationY = sheetTop(p) - fullTop
		sheetList.translationY = listInset * (p - STATE_PREVIEW).coerceIn(0f, 1f)
		applyMediaTransform(min(p, STATE_PREVIEW.toFloat()))
		val fade = 1f - dismissProgress
		val chromeAlpha = (1f - (p - STATE_PREVIEW) / (CHROME_SWITCH_PROGRESS - STATE_PREVIEW)).coerceIn(0f, 1f) * fade
		floatingChrome.alpha = chromeAlpha
		floatingChrome.visibility = if (chromeAlpha > 0f) VISIBLE else INVISIBLE
		val appBarAlpha = ((p - CHROME_SWITCH_PROGRESS) / (STATE_FULL - CHROME_SWITCH_PROGRESS)).coerceIn(0f, 1f)
		solidAppBar.alpha = appBarAlpha
		solidAppBar.visibility = if (appBarAlpha > 0f) VISIBLE else INVISIBLE
		backdrop.alpha = fade
		listener?.onProgressChanged(p, dismissProgress)
	}

	private fun applyMediaTransform(p: Float) {
		if (p <= 0f && dismissY == 0f && dismissX == 0f) {
			mediaRect = null
			mediaContainer.scaleX = 1f
			mediaContainer.scaleY = 1f
			mediaContainer.translationX = 0f
			mediaContainer.translationY = 0f
			return
		}
		val rect = mediaRect ?: captureMediaRect().also { mediaRect = it }
		val dismissScale = 1f - DISMISS_SHRINK * (dismissY / (height * DISMISS_SHRINK_TRAVEL)).coerceIn(0f, 1f)
		val scale = (1f + (previewRect.height() / rect.height() - 1f) * p) * dismissScale
		mediaContainer.pivotX = rect.centerX()
		mediaContainer.pivotY = rect.centerY()
		mediaContainer.scaleX = scale
		mediaContainer.scaleY = scale
		mediaContainer.translationX = (previewRect.centerX() - rect.centerX()) * p + dismissX * DISMISS_HORIZONTAL_FOLLOW
		mediaContainer.translationY = (previewRect.centerY() - rect.centerY()) * p + dismissY
	}

	private fun captureMediaRect(): RectF {
		val bounds = RectF(0f, 0f, mediaContainer.width.toFloat(), mediaContainer.height.toFloat())
		val content = pageProvider()?.contentRect()?.let { RectF(it) }
			?.takeIf { it.width() > 0f && it.height() > 0f && it.intersect(bounds) }
			?: bounds
		val box = RectF(0f, topInset.toFloat(), width.toFloat(), (topInset + boxHeight).toFloat())
		val scale = min(box.width() / content.width(), box.height() / content.height())
		val w = content.width() * scale
		val h = content.height() * scale
		previewRect.set(box.centerX() - w / 2f, box.centerY() - h / 2f, box.centerX() + w / 2f, box.centerY() + h / 2f)
		return content
	}

	private fun snapTo(state: Int) {
		val target = state.coerceIn(STATE_MEDIA, STATE_FULL)
		cancelAnimator()
		dismissX = 0f
		dismissY = 0f
		dismissProgress = 0f
		progress = target.toFloat()
		applyProgress()
		onSettled(target)
	}

	private fun onSettled(state: Int) {
		settledState = state
		if (state == STATE_MEDIA) mediaRect = null
		if (state == STATE_PREVIEW) pageProvider()?.onPreviewSettled()
	}

	private fun settle(upwardVelocity: Float) {
		val p = progress
		val nearest = p.roundToInt()
		val atState = abs(p - nearest) < AT_STATE_EPSILON
		val target = when {
			upwardVelocity > SETTLE_VELOCITY -> if (atState) nearest + 1 else ceil(p).toInt()
			upwardVelocity < -SETTLE_VELOCITY -> if (atState) nearest - 1 else floor(p).toInt()
			else -> nearest
		}
		animateTo(target)
	}

	private fun cancelAnimator() {
		animator?.let {
			animator = null
			it.cancel()
		}
	}

	override fun onDetachedFromWindow() {
		cancelAnimator()
		endTouch()
		super.onDetachedFromWindow()
	}

	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		if (dismissing) return true
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				beginTouch(ev)
				return dragMode == DragMode.MEDIA_BOX || dragMode == DragMode.DISMISS
			}
			MotionEvent.ACTION_POINTER_DOWN -> if (dragMode == DragMode.UNDECIDED) dragMode = DragMode.PAGE
			MotionEvent.ACTION_MOVE -> {
				velocityTracker?.addMovement(ev)
				if (dragMode == DragMode.UNDECIDED && ev.pointerCount == 1) decide(ev)
				return dragMode == DragMode.SHEET || dragMode == DragMode.DISMISS
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endTouch()
		}
		return false
	}

	override fun onTouchEvent(ev: MotionEvent): Boolean {
		if (dismissing) return true
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> if (velocityTracker == null) beginTouch(ev)
			MotionEvent.ACTION_POINTER_DOWN -> if (dragMode == DragMode.UNDECIDED) dragMode = DragMode.PAGE
			MotionEvent.ACTION_POINTER_UP -> switchPointer(ev)
			MotionEvent.ACTION_MOVE -> {
				velocityTracker?.addMovement(ev)
				val index = ev.findPointerIndex(activePointerId).takeIf { it >= 0 } ?: return true
				val x = ev.getX(index)
				val y = ev.getY(index)
				when (dragMode) {
					DragMode.UNDECIDED -> if (ev.pointerCount == 1) decide(ev)
					DragMode.MEDIA_BOX -> {
						val dx = x - downX
						val dy = y - downY
						if (abs(dy) >= touchSlop && abs(dy) >= abs(dx)) startDrag(DragMode.SHEET, x, y)
					}
					else -> {}
				}
				when (dragMode) {
					DragMode.SHEET -> setProgress(progressForTop(dragStartTop + (y - dragStartY)))
					DragMode.DISMISS -> {
						dismissX = x - dragStartX
						dismissY = y - dragStartY
						dismissProgress = dismissProgressOf(dismissY)
						applyProgress()
					}
					else -> {}
				}
			}
			MotionEvent.ACTION_UP -> {
				release(ev, false)
				endTouch()
			}
			MotionEvent.ACTION_CANCEL -> {
				release(ev, true)
				endTouch()
			}
		}
		return true
	}

	private fun beginTouch(ev: MotionEvent) {
		velocityTracker?.recycle()
		velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
		activePointerId = ev.getPointerId(0)
		downX = ev.x
		downY = ev.y
		if (dismissX != 0f || dismissY != 0f) {
			cancelAnimator()
			dragMode = DragMode.DISMISS
			dragStartX = ev.x - dismissX
			dragStartY = ev.y - dismissY
			dragStartTop = sheetTop(progress)
			return
		}
		val onSheet = ev.y >= sheet.top + sheet.translationY
		toolbar.getHitRect(hitRect)
		val onToolbar = floatingChrome.visibility == VISIBLE && toolbar.visibility == VISIBLE
			&& hitRect.contains(ev.x.toInt(), ev.y.toInt())
		val onSolidAppBar = solidAppBar.visibility == VISIBLE && ev.y < solidAppBar.bottom
		dragMode = when {
			onSheet || onToolbar || onSolidAppBar -> DragMode.PAGE
			progress > 0f || animator != null -> {
				cancelAnimator()
				DragMode.MEDIA_BOX
			}
			else -> DragMode.UNDECIDED
		}
	}

	private fun decide(ev: MotionEvent) {
		val dx = ev.x - downX
		val dy = ev.y - downY
		if (abs(dx) < touchSlop && abs(dy) < touchSlop) return
		if (abs(dx) > abs(dy)) {
			dragMode = DragMode.PAGE
			return
		}
		val up = dy < 0f
		if (pageProvider()?.canScrollVertically(if (up) 1 else -1) == true) {
			dragMode = DragMode.PAGE
			return
		}
		startDrag(if (up) DragMode.SHEET else DragMode.DISMISS, ev.x, ev.y)
	}

	private fun startDrag(mode: DragMode, x: Float, y: Float) {
		dragMode = mode
		dragStartX = x
		dragStartY = y
		dragStartTop = sheetTop(progress)
		if (mode == DragMode.DISMISS && mediaRect == null) mediaRect = captureMediaRect()
	}

	private fun switchPointer(ev: MotionEvent) {
		val index = ev.actionIndex
		if (ev.getPointerId(index) != activePointerId) return
		val other = if (index == 0) 1 else 0
		activePointerId = ev.getPointerId(other)
		val x = ev.getX(other)
		val y = ev.getY(other)
		downX = x
		downY = y
		dragStartX = x - dismissX
		dragStartY = if (dragMode == DragMode.DISMISS) y - dismissY else y
		dragStartTop = sheetTop(progress)
	}

	private fun release(ev: MotionEvent, cancelled: Boolean) {
		val tracker = velocityTracker
		tracker?.computeCurrentVelocity(1000)
		val velocityY = if (tracker != null && !cancelled) tracker.yVelocity else 0f
		val touchUpwardVelocity = -velocityY
		when (dragMode) {
			DragMode.SHEET -> settle(touchUpwardVelocity)
			DragMode.DISMISS -> if (!cancelled && (dismissY > height * DISMISS_THRESHOLD_TRAVEL || velocityY > DISMISS_VELOCITY)) dismiss() else returnFromDismiss()
			DragMode.MEDIA_BOX -> {
				val tap = !cancelled && abs(ev.x - downX) < touchSlop && abs(ev.y - downY) < touchSlop
				if (tap) animateTo(STATE_MEDIA) else settle(0f)
			}
			else -> {}
		}
	}

	private fun endTouch() {
		velocityTracker?.recycle()
		velocityTracker = null
		dragMode = DragMode.NONE
		activePointerId = MotionEvent.INVALID_POINTER_ID
	}

	private fun returnFromDismiss() {
		val startX = dismissX
		val startY = dismissY
		cancelAnimator()
		if (!animationsEnabled || (startX == 0f && startY == 0f)) {
			dismissX = 0f
			dismissY = 0f
			dismissProgress = 0f
			applyProgress()
			return
		}
		animator = ValueAnimator.ofFloat(1f, 0f).apply {
			duration = settleDuration(abs(startY))
			interpolator = GalleryMotion.CURVE
			addUpdateListener {
				val f = it.animatedValue as Float
				dismissX = startX * f
				dismissY = startY * f
				dismissProgress = dismissProgressOf(dismissY)
				applyProgress()
			}
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					if (animator === animation) animator = null
				}
			})
			start()
		}
	}

	private fun dismiss() {
		dismissing = true
		cancelAnimator()
		val startX = dismissX
		val startY = dismissY
		val startFade = 1f - dismissProgress
		if (!animationsEnabled) {
			listener?.onDismissed()
			return
		}
		animator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = DISMISS_DURATION_MS
			interpolator = GalleryMotion.CURVE
			addUpdateListener {
				val f = it.animatedValue as Float
				dismissX = startX
				dismissY = startY + DISMISS_EXIT_SLIDE * height * f
				dismissProgress = 1f - startFade * (1f - f)
				mediaContainer.alpha = 1f - f
				applyProgress()
			}
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					if (animator === animation) {
						animator = null
						listener?.onDismissed()
					}
				}
			})
			start()
		}
	}

	override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean =
		!dismissing && axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0 && type == ViewCompat.TYPE_TOUCH

	override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
		nestedHelper.onNestedScrollAccepted(child, target, axes, type)
		cancelAnimator()
	}

	override fun onStopNestedScroll(target: View, type: Int) {
		nestedHelper.onStopNestedScroll(target, type)
		if (type == ViewCompat.TYPE_TOUCH && animator == null) settle(0f)
	}

	private fun dismissProgressOf(dragY: Float): Float = (dragY / (height * DISMISS_FADE_TRAVEL)).coerceIn(0f, 1f)

	override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
		if (type != ViewCompat.TYPE_TOUCH) return
		if (dy > 0 && progress < STATE_FULL) {
			consumed[1] = moveSheet(dy)
		} else if (dy < 0 && progress > 0f && !target.canScrollVertically(-1)) {
			consumed[1] = moveSheet(dy)
		}
	}

	override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
		if (type == ViewCompat.TYPE_TOUCH && dyUnconsumed < 0 && progress > 0f) consumed[1] += moveSheet(dyUnconsumed)
	}

	override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
		onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, IntArray(2))
	}

	override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
		val scrollUpwardVelocity = velocityY
		if (scrollUpwardVelocity > 0 && progress < STATE_FULL) {
			settle(scrollUpwardVelocity)
			return true
		}
		if (scrollUpwardVelocity < 0 && progress > 0f && (progress < STATE_FULL || !target.canScrollVertically(-1))) {
			settle(scrollUpwardVelocity)
			return true
		}
		return false
	}

	override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean = false

	override fun getNestedScrollAxes(): Int = nestedHelper.nestedScrollAxes

	private fun moveSheet(dy: Int): Int {
		val top = sheetTop(progress)
		val newTop = (top - dy).coerceIn(sheetTop(STATE_FULL.toFloat()), sheetTop(STATE_MEDIA.toFloat()))
		setProgress(progressForTop(newTop))
		return (top - newTop).roundToInt()
	}

	companion object {
		const val STATE_MEDIA = 0
		const val STATE_PREVIEW = 1
		const val STATE_FULL = 2
		const val CHROME_SWITCH_PROGRESS = 1.5f

		const val AUDIO_VIRTUAL_CONTENT_ASPECT = 4f / 3f

		private const val PREVIEW_BOX_HEIGHT_DP = 300f
		private const val SHEET_RADIUS_DP = 16f
		private const val LIST_TOP_INSET_DP = 16f
		private const val LANDSCAPE_BOX_MAX_HEIGHT = 0.5f
		private const val AT_STATE_EPSILON = 0.001f
		private const val SETTLE_VELOCITY = 1000f
		private const val SETTLE_FULL_DURATION_TRAVEL = 0.5f
		private const val DISMISS_VELOCITY = 1500f
		private const val DISMISS_THRESHOLD_TRAVEL = 1f / 6f
		private const val DISMISS_FADE_TRAVEL = 1f / 3f
		private const val DISMISS_SHRINK = 0.3f
		private const val DISMISS_SHRINK_TRAVEL = 0.5f
		private const val DISMISS_HORIZONTAL_FOLLOW = 0.5f
		private const val DISMISS_EXIT_SLIDE = 0.3f
		private const val DISMISS_DURATION_MS = 200L
	}
}
