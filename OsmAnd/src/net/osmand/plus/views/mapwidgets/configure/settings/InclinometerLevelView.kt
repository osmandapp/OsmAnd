package net.osmand.plus.views.mapwidgets.configure.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Bubble level for the inclinometer calibration screen: a circle with a crosshair and a ball
 * that rolls towards the lowered side of the vehicle. The ball is drawn in the level colour while
 * both angles are within [LEVEL_TOLERANCE_DEGREES], otherwise in the tilted colour.
 */
class InclinometerLevelView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : View(context, attrs) {

	companion object {
		private const val FULL_SCALE_DEGREES = 30f
		private const val LEVEL_TOLERANCE_DEGREES = 2f
	}

	private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = dp(1.5f)
	}
	private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = dp(1f)
	}
	private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = dp(1f)
		pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(3f)), 0f)
	}
	private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
	}

	private val ballRadius = dp(12f)
	private var pitch = 0f
	private var roll = 0f

	@ColorInt
	private var levelColor = 0

	@ColorInt
	private var tiltedColor = 0

	fun setColors(@ColorInt outline: Int, @ColorInt target: Int, @ColorInt level: Int, @ColorInt tilted: Int) {
		outlinePaint.color = outline
		crosshairPaint.color = outline
		targetPaint.color = target
		levelColor = level
		tiltedColor = tilted
		invalidate()
	}

	fun setInclination(pitchDegrees: Float, rollDegrees: Float) {
		pitch = pitchDegrees
		roll = rollDegrees
		invalidate()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val cx = width / 2f
		val cy = height / 2f
		val radius = min(width, height) / 2f - outlinePaint.strokeWidth
		if (radius <= ballRadius) {
			return
		}
		canvas.drawCircle(cx, cy, radius, outlinePaint)
		canvas.drawLine(cx - radius, cy, cx + radius, cy, crosshairPaint)
		canvas.drawLine(cx, cy - radius, cx, cy + radius, crosshairPaint)
		canvas.drawCircle(cx, cy, ballRadius + dp(4f), targetPaint)

		// The ball rolls to the lowered side: right for positive roll, back (down) for a raised front
		val travel = radius - ballRadius
		var dx = roll / FULL_SCALE_DEGREES * travel
		var dy = pitch / FULL_SCALE_DEGREES * travel
		val distance = hypot(dx, dy)
		if (distance > travel) {
			dx *= travel / distance
			dy *= travel / distance
		}
		val level = abs(pitch) <= LEVEL_TOLERANCE_DEGREES && abs(roll) <= LEVEL_TOLERANCE_DEGREES
		ballPaint.color = if (level) levelColor else tiltedColor
		canvas.drawCircle(cx + dx, cy + dy, ballRadius, ballPaint)
	}

	private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
