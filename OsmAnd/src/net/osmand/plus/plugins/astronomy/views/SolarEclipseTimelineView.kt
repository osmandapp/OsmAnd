package net.osmand.plus.plugins.astronomy.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.roundToLong

internal object SolarEclipseTimelineMapper {
	fun fractionForTime(startMillis: Long, endMillis: Long, timeMillis: Long): Double {
		if (endMillis <= startMillis) return 0.0
		return ((timeMillis - startMillis).toDouble() / (endMillis - startMillis).toDouble())
			.coerceIn(0.0, 1.0)
	}

	fun timeForFraction(startMillis: Long, endMillis: Long, fraction: Double): Long {
		if (endMillis <= startMillis) return startMillis
		val raw = startMillis + (endMillis - startMillis) * fraction.coerceIn(0.0, 1.0)
		return ((raw / 1000.0).roundToLong() * 1000L).coerceIn(startMillis, endMillis)
	}
}

class SolarEclipseTimelineView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private val density = resources.displayMetrics.density
	private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0x556F7D8C
		strokeCap = Paint.Cap.ROUND
		strokeWidth = 4f * density
	}
	private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF287EF0.toInt()
		strokeCap = Paint.Cap.ROUND
		strokeWidth = 4f * density
	}
	private val maximumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFFFFA000.toInt()
		strokeWidth = 3f * density
	}
	private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF287EF0.toInt()
		style = Paint.Style.FILL
		setShadowLayer(3f * density, 0f, density, 0x55000000)
	}

	private var startMillis = 0L
	private var endMillis = 1L
	private var maximumMillis = 0L
	private var currentMillis = 0L
	private var listener: ((Long, Boolean) -> Unit)? = null

	init {
		setLayerType(LAYER_TYPE_SOFTWARE, null)
		isFocusable = true
		isClickable = true
	}

	fun setRange(start: Long, end: Long, maximum: Long, current: Long) {
		val normalizedEnd = end.coerceAtLeast(start + 1L)
		val normalizedMaximum = maximum.coerceIn(start, normalizedEnd)
		val normalizedCurrent = current.coerceIn(start, normalizedEnd)
		if (startMillis == start && endMillis == normalizedEnd &&
			maximumMillis == normalizedMaximum && currentMillis == normalizedCurrent
		) {
			return
		}
		startMillis = start
		endMillis = normalizedEnd
		maximumMillis = normalizedMaximum
		currentMillis = normalizedCurrent
		invalidate()
	}

	fun setOnTimeChangedListener(listener: (timeMillis: Long, fromUser: Boolean) -> Unit) {
		this.listener = listener
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val edge = 16f * density
		val left = edge
		val right = width - edge
		val centerY = height / 2f
		val currentX = timeToX(currentMillis, left, right)
		val maximumX = timeToX(maximumMillis, left, right)

		canvas.drawLine(left, centerY, right, centerY, trackPaint)
		if (layoutDirection == LAYOUT_DIRECTION_RTL) {
			canvas.drawLine(right, centerY, currentX, centerY, activePaint)
		} else {
			canvas.drawLine(left, centerY, currentX, centerY, activePaint)
		}
		canvas.drawLine(maximumX, centerY - 11f * density, maximumX, centerY + 11f * density, maximumPaint)
		canvas.drawCircle(currentX, centerY, 9f * density, thumbPaint)
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				parent?.requestDisallowInterceptTouchEvent(true)
				updateFromTouch(event.x)
				return true
			}
			MotionEvent.ACTION_MOVE -> {
				updateFromTouch(event.x)
				return true
			}
			MotionEvent.ACTION_UP -> {
				updateFromTouch(event.x)
				performClick()
				return true
			}
			MotionEvent.ACTION_CANCEL -> return true
		}
		return super.onTouchEvent(event)
	}

	override fun performClick(): Boolean {
		super.performClick()
		return true
	}

	override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
		super.onInitializeAccessibilityNodeInfo(info)
		info.className = "android.widget.SeekBar"
		info.isScrollable = true
		val durationSeconds = (endMillis - startMillis) / 1000f
		val currentSeconds = (currentMillis - startMillis) / 1000f
		info.rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
			AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
			0f,
			durationSeconds.coerceAtLeast(1f),
			currentSeconds.coerceIn(0f, durationSeconds.coerceAtLeast(1f))
		)
		info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
		info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
	}

	override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
		val delta = when (action) {
			AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> 1000L
			AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> -1000L
			else -> return super.performAccessibilityAction(action, arguments)
		}
		currentMillis = (currentMillis + delta).coerceIn(startMillis, endMillis)
		listener?.invoke(currentMillis, true)
		invalidate()
		sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
		return true
	}

	private fun updateFromTouch(x: Float) {
		val edge = 16f * density
		val fraction = ((x - edge) / (width - 2f * edge)).coerceIn(0f, 1f)
		val directedFraction = if (layoutDirection == LAYOUT_DIRECTION_RTL) 1f - fraction else fraction
		currentMillis = SolarEclipseTimelineMapper.timeForFraction(
			startMillis,
			endMillis,
			directedFraction.toDouble()
		)
		listener?.invoke(currentMillis, true)
		invalidate()
	}

	private fun timeToX(time: Long, left: Float, right: Float): Float {
		var fraction = SolarEclipseTimelineMapper.fractionForTime(startMillis, endMillis, time)
		if (layoutDirection == LAYOUT_DIRECTION_RTL) fraction = 1.0 - fraction
		return left + (right - left) * fraction.toFloat()
	}
}
