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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MediaViewerSheetLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
	FrameLayout(context, attrs), NestedScrollingParent3 {

	interface Listener {
		fun onProgressChanged(progress: Float, dismiss: Float)
		fun onDismissed()
	}

	private enum class DragMode { NONE, UNDECIDED, PAGE, BOX, SHEET, DISMISS }

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
	private val previewBoxHeight = AndroidUtils.dpToPx(context, 300f)
	private val sheetRadius = AndroidUtils.dpToPx(context, 16f)
	private val listInset = AndroidUtils.dpToPx(context, 16f) + sheetRadius
	private val hitRect = Rect()

	private var statusBarInset = 0
	private var topInset = -1
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

	override fun onFinishInflate() {
		super.onFinishInflate()
		backdrop = findViewById(R.id.backdrop)
		mediaContainer = findViewById(R.id.media_container)
		floatingChrome = findViewById(R.id.floating_chrome)
		toolbar = findViewById(R.id.toolbar)
		sheet = findViewById(R.id.details_sheet)
		sheetList = findViewById(R.id.details_list)
		solidAppBar = findViewById(R.id.solid_app_bar)
	}

	fun setInitialState(state: Int) {
		if (width > 0 && height > 0) snapTo(state) else pendingState = state
	}

	fun animateTo(state: Int) {
		if (dismissing) return
		val target = state.coerceIn(0, 2)
		cancelAnimator()
		if (!animationsEnabled || width == 0 || height == 0 || target.toFloat() == progress) {
			snapTo(target)
			return
		}
		val from = progress
		val distance = abs(sheetTop(from) - sheetTop(target.toFloat()))
		val duration = 150L + (150f * (distance / (height / 2f)).coerceIn(0f, 1f)).toLong()
		animator = ValueAnimator.ofFloat(from, target.toFloat()).apply {
			this.duration = duration
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
		progress > 1.5f -> { animateTo(1); true }
		progress > 0f -> { animateTo(0); true }
		else -> false
	}

	fun onPageContentChanged() {
		if (progress > 0f || dismissProgress > 0f) {
			mediaRect = null
			applyProgress()
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		resolveInsets()
		val location = IntArray(2)
		(parent as? View)?.getLocationInWindow(location)
		applyTopInset(max(0, statusBarInset - (location[1] + top)))
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		val location = IntArray(2)
		getLocationInWindow(location)
		if (max(0, statusBarInset - location[1]) != topInset) post { requestLayout() }
		val pending = pendingState
		if (pending != null) {
			pendingState = null
			snapTo(pending)
			return
		}
		if (changed) mediaRect = null
		applyProgress()
	}

	private fun resolveInsets() {
		val insets = ViewCompat.getRootWindowInsets(this)
		statusBarInset = insets?.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())?.top
			?: AndroidUtils.getStatusBarHeight(context)
	}

	private fun applyTopInset(inset: Int) {
		if (inset == topInset) return
		topInset = inset
		floatingChrome.setPadding(floatingChrome.paddingLeft, inset, floatingChrome.paddingRight, floatingChrome.paddingBottom)
		solidAppBar.setPadding(solidAppBar.paddingLeft, inset, solidAppBar.paddingRight, solidAppBar.paddingBottom)
		(sheet.layoutParams as LayoutParams).topMargin = inset + appBarHeight - sheetRadius
	}

	private val boxHeight: Int
		get() = if (width > height) min(previewBoxHeight, (height - topInset) / 2) else previewBoxHeight

	private val fullTop: Float
		get() = (topInset + appBarHeight - sheetRadius).toFloat()

	private fun sheetTop(p: Float): Float {
		val h = height.toFloat()
		val previewTop = (topInset + boxHeight).toFloat()
		return if (p <= 1f) h + (previewTop - h) * p else previewTop + (fullTop - previewTop) * (p - 1f)
	}

	private fun progressForTop(top: Float): Float {
		val h = height.toFloat()
		val previewTop = (topInset + boxHeight).toFloat()
		return if (top >= previewTop) {
			if (h > previewTop) ((h - top) / (h - previewTop)).coerceIn(0f, 1f) else 1f
		} else {
			if (previewTop > fullTop) (1f + (previewTop - top) / (previewTop - fullTop)).coerceIn(1f, 2f) else 2f
		}
	}

	private fun setProgress(value: Float) {
		progress = value.coerceIn(0f, 2f)
		applyProgress()
	}

	private fun applyProgress() {
		if (width == 0 || height == 0) return
		val p = progress
		sheet.translationY = sheetTop(p) - fullTop
		sheetList.translationY = listInset * (p - 1f).coerceIn(0f, 1f)
		applyMediaTransform(min(p, 1f))
		val fade = 1f - dismissProgress
		val chromeAlpha = (1f - (p - 1f) / 0.5f).coerceIn(0f, 1f) * fade
		floatingChrome.alpha = chromeAlpha
		floatingChrome.visibility = if (chromeAlpha > 0f) VISIBLE else INVISIBLE
		val appBarAlpha = ((p - 1.5f) / 0.5f).coerceIn(0f, 1f)
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
		val dismissScale = 1f - 0.3f * (dismissY / (height / 2f)).coerceIn(0f, 1f)
		val scale = (1f + (previewRect.height() / rect.height() - 1f) * p) * dismissScale
		mediaContainer.pivotX = rect.centerX()
		mediaContainer.pivotY = rect.centerY()
		mediaContainer.scaleX = scale
		mediaContainer.scaleY = scale
		mediaContainer.translationX = (previewRect.centerX() - rect.centerX()) * p + dismissX * 0.5f
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
		val target = state.coerceIn(0, 2)
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
		if (state == 0) mediaRect = null
		if (state == 1) pageProvider()?.onPreviewSettled()
	}

	private fun settle(velocityUp: Float) {
		val p = progress
		val nearest = p.roundToInt()
		val atState = abs(p - nearest) < 0.001f
		val target = when {
			velocityUp > SETTLE_VELOCITY -> if (atState) nearest + 1 else ceil(p).toInt()
			velocityUp < -SETTLE_VELOCITY -> if (atState) nearest - 1 else floor(p).toInt()
			else -> nearest
		}
		animateTo(target.coerceIn(0, 2))
	}

	private fun cancelAnimator() {
		animator?.let {
			animator = null
			it.cancel()
		}
	}

	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		if (dismissing) return true
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				beginTouch(ev)
				return dragMode == DragMode.BOX || dragMode == DragMode.DISMISS
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
					DragMode.BOX -> {
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
						dismissProgress = (dismissY / (height / 3f)).coerceIn(0f, 1f)
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
				DragMode.BOX
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
		when (dragMode) {
			DragMode.SHEET -> settle(-velocityY)
			DragMode.DISMISS -> if (!cancelled && (dismissY > height / 6f || velocityY > DISMISS_VELOCITY)) dismiss() else returnFromDismiss()
			DragMode.BOX -> {
				val tap = !cancelled && abs(ev.x - downX) < touchSlop && abs(ev.y - downY) < touchSlop
				if (tap) animateTo(0) else settle(0f)
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
			duration = 150L + (150f * (abs(startY) / (height / 2f)).coerceIn(0f, 1f)).toLong()
			interpolator = GalleryMotion.CURVE
			addUpdateListener {
				val f = it.animatedValue as Float
				dismissX = startX * f
				dismissY = startY * f
				dismissProgress = (dismissY / (height / 3f)).coerceIn(0f, 1f)
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
				dismissY = startY + 0.3f * height * f
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

	override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
		if (type != ViewCompat.TYPE_TOUCH) return
		if (dy > 0 && progress < 2f) {
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
		if (velocityY > 0 && progress < 2f) {
			settle(velocityY)
			return true
		}
		if (velocityY < 0 && progress > 0f && (progress < 2f || !target.canScrollVertically(-1))) {
			settle(velocityY)
			return true
		}
		return false
	}

	override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean = false

	override fun getNestedScrollAxes(): Int = nestedHelper.nestedScrollAxes

	private fun moveSheet(dy: Int): Int {
		val top = sheetTop(progress)
		val newTop = (top - dy).coerceIn(sheetTop(2f), sheetTop(0f))
		setProgress(progressForTop(newTop))
		return (top - newTop).roundToInt()
	}

	companion object {
		private const val SETTLE_VELOCITY = 1000f
		private const val DISMISS_VELOCITY = 1500f
		private const val DISMISS_DURATION_MS = 200L
	}
}
