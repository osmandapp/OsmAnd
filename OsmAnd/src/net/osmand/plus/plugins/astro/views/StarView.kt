package net.osmand.plus.plugins.astro.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Equatorial
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class StarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

	// --- Graphics ---
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE
		textSize = 30f
	}
	private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.parseColor("#00FFFF")
		style = Paint.Style.STROKE
		strokeWidth = 3f
		pathEffect = DashPathEffect(floatArrayOf(10f, 15f), 0f)
	}
	private val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.parseColor("#00FFFF")
		style = Paint.Style.STROKE
		strokeWidth = 3f
	}
	private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.parseColor("#00FFFF")
		textSize = 24f
		textAlign = Paint.Align.CENTER
	}
	private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.parseColor("#00FFFF")
		style = Paint.Style.FILL
	}

	// --- View State ---
	private var azimuthCenter = 180.0
	private var altitudeCenter = 45.0
	private var viewAngle = 60.0 // Field of view in degrees

	// --- Interaction ---
	private var lastTouchX = 0f
	private var lastTouchY = 0f
	private var isPanning = false
	private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
	private var onObjectClickListener: ((SkyObject?) -> Unit)? = null

	// --- Astronomy Data ---
	private val skyObjects = mutableListOf<SkyObject>()
	private var observer = Observer(56.9496, 24.1052, 0.0) // Default
	private var currentTime = Time(System.currentTimeMillis() / 1000.0 / 86400.0 + 2440587.5)

	// --- Selection & Trails ---
	private var selectedObject: SkyObject? = null

	private data class PathPoint(
		val point: PointF,
		val hourLabel: String?, // e.g. "22", "00"
		val timeOffsetHours: Double,
		val isValid: Boolean
	)
	private val celestialPathPoints = mutableListOf<PathPoint>()

	// --- Animation ---
	private var visualAnimator: ValueAnimator? = null

	fun setObserverLocation(lat: Double, lon: Double, alt: Double) {
		observer = Observer(lat, lon, alt)
		recalculatePositions(currentTime, updateTargets = false)
		invalidate()
	}

	fun setSkyObjects(objects: List<SkyObject>) {
		skyObjects.clear()
		skyObjects.addAll(objects)
		recalculatePositions(currentTime, updateTargets = false)
		skyObjects.forEach {
			it.azimuth = it.targetAzimuth
			it.altitude = it.targetAltitude
		}
		invalidate()
	}

	fun updateVisibility() {
		invalidate()
	}

	fun setOnObjectClickListener(listener: (SkyObject?) -> Unit) {
		this.onObjectClickListener = listener
	}

	fun setDateTime(time: Time, animate: Boolean = true) {
		visualAnimator?.cancel()

		if (animate) {
			skyObjects.forEach {
				it.startAzimuth = it.azimuth
				it.startAltitude = it.altitude
			}
			recalculatePositions(time, updateTargets = true)
			currentTime = time

			visualAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
				duration = 400
				interpolator = DecelerateInterpolator()
				addUpdateListener { animator ->
					val fraction = animator.animatedValue as Float
					skyObjects.forEach { obj ->
						obj.azimuth = interpolateAngle(obj.startAzimuth, obj.targetAzimuth, fraction)
						obj.altitude = obj.startAltitude + (obj.targetAltitude - obj.startAltitude) * fraction
					}
					if (selectedObject != null) calculateCelestialPath(selectedObject!!)
					invalidate()
				}
				start()
			}
		} else {
			currentTime = time
			recalculatePositions(time, updateTargets = true)
			skyObjects.forEach {
				it.azimuth = it.targetAzimuth
				it.altitude = it.targetAltitude
			}
			if (selectedObject != null) calculateCelestialPath(selectedObject!!)
			invalidate()
		}
	}

	private fun recalculatePositions(time: Time, updateTargets: Boolean) {
		skyObjects.forEach { obj ->
			if (!obj.isVisible && obj != selectedObject) return@forEach

			val hor: Topocentric
			val equ: Equatorial

			if (obj.type == SkyObject.Type.STAR) {
				hor = horizon(time, observer, obj.ra, obj.dec, Refraction.Normal)
			} else {
				val body = obj.body ?: throw IllegalStateException("Planet without Body enum")
				equ = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
				hor = horizon(time, observer, equ.ra, equ.dec, Refraction.Normal)
				obj.distAu = equ.dist
			}

			if (updateTargets) {
				obj.targetAzimuth = hor.azimuth
				obj.targetAltitude = hor.altitude
			} else {
				obj.azimuth = hor.azimuth
				obj.altitude = hor.altitude
				obj.targetAzimuth = hor.azimuth
				obj.targetAltitude = hor.altitude
			}
		}
	}

	private fun interpolateAngle(start: Double, end: Double, fraction: Float): Double {
		var diff = end - start
		while (diff > 180) diff -= 360
		while (diff < -180) diff += 360
		var result = start + diff * fraction
		while (result < 0) result += 360
		while (result >= 360) result -= 360
		return result
	}

	private fun calculateCelestialPath(obj: SkyObject) {
		celestialPathPoints.clear()

		val startHours = -12
		val endHours = 12
		val stepMinutes = 10

		val totalMinutes = (endHours - startHours) * 60
		val steps = totalMinutes / stepMinutes

		// Get local timezone for hour labels
		val tz = TimeZone.getDefault()
		for (i in 0..steps) {

			// Calculate start time aligned to hour
			val startCal = Calendar.getInstance(tz)
			startCal.timeInMillis = currentTime.toMillisecondsSince1970()
			startCal.add(Calendar.HOUR_OF_DAY, startHours)
			startCal.set(Calendar.MINUTE, 0)
			startCal.set(Calendar.SECOND, 0)
			startCal.set(Calendar.MILLISECOND, 0)

			val stepTimeMillis = startCal.timeInMillis + (i * stepMinutes * 60000L)
			// Limit to +12h from now roughly (24h span total)
			val maxTime = currentTime.toMillisecondsSince1970() + (endHours * 3600000L)
			if (stepTimeMillis > maxTime + 3600000L) break // Stop if we go too far

			val tStep = Time.fromMillisecondsSince1970(stepTimeMillis)

			// Now minute is guaranteed to be 0, 10, 20... in Local Time
			val calStep = Calendar.getInstance(tz)
			calStep.timeInMillis = stepTimeMillis
			val stepMinute = calStep.get(Calendar.MINUTE)
			val stepHour = calStep.get(Calendar.HOUR_OF_DAY)

			val isHourMark = (stepMinute == 0)
			val label = if (isHourMark) "%02d".format(stepHour) else null

			// Calculate position
			val altAz: Topocentric = if (obj.type == SkyObject.Type.STAR) {
				horizon(tStep, observer, obj.ra, obj.dec, Refraction.Normal)
			} else {
				val body = obj.body!!
				val eq = equator(body, tStep, observer, EquatorEpoch.OfDate, Aberration.Corrected)
				horizon(tStep, observer, eq.ra, eq.dec, Refraction.Normal)
			}

			val screenPoint = skyToScreen(altAz.azimuth, altAz.altitude)

			// Calculate relative offset hours for Arrows (offset from *current* time, not start)
			val timeOffsetHours = (stepTimeMillis - currentTime.toMillisecondsSince1970()) / 3600000.0

			if (screenPoint != null) {
				celestialPathPoints.add(
					PathPoint(screenPoint, label, timeOffsetHours, true)
				)
			} else {
				celestialPathPoints.add(
					PathPoint(PointF(0f,0f), null, timeOffsetHours, false)
				)
			}
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		canvas.drawColor(Color.BLACK)

		drawGrid(canvas)
		drawHorizon(canvas)

		if (selectedObject != null && celestialPathPoints.size > 1) {
			val path = Path()
			var isPenDown = false

			// 1. Draw Path
			for (i in celestialPathPoints.indices) {
				val curr = celestialPathPoints[i]

				if (!curr.isValid) {
					isPenDown = false
					continue
				}

				// Check jump (screen wrapping)
				if (isPenDown && i > 0) {
					val prev = celestialPathPoints[i-1]
					if (prev.isValid) {
						val dist = hypot(curr.point.x - prev.point.x, curr.point.y - prev.point.y)
						// Using a robust threshold for wrap detection
						if (dist > width * 0.8) {
							isPenDown = false
						}
					}
				}

				if (!isPenDown) {
					path.moveTo(curr.point.x, curr.point.y)
					isPenDown = true
				} else {
					path.lineTo(curr.point.x, curr.point.y)
				}
			}
			canvas.drawPath(path, pathPaint)

			// 2. Draw Notches, Labels, Arrows
			for (i in 1 until celestialPathPoints.size - 1) {
				val curr = celestialPathPoints[i]
				if (!curr.isValid) continue

				val prev = celestialPathPoints[i-1]
				val next = celestialPathPoints[i+1]

				if (!prev.isValid || !next.isValid) continue

				// Check continuity
				if (hypot(curr.point.x - prev.point.x, curr.point.y - prev.point.y) > 200) continue
				if (hypot(next.point.x - curr.point.x, next.point.y - curr.point.y) > 200) continue

				val dx = next.point.x - prev.point.x
				val dy = next.point.y - prev.point.y
				val angle = atan2(dy, dx)

				// Hour Notch & Label
				if (curr.hourLabel != null) {
					canvas.save()
					canvas.translate(curr.point.x, curr.point.y)
					canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())

					// Notch: Perpendicular to path.
					canvas.drawLine(0f, -10f, 0f, 10f, notchPaint)
					canvas.restore()

					// Text: Offset perpendicular to path
					val textDist = 30f
					val px = curr.point.x + textDist * cos(angle - PI/2).toFloat()
					val py = curr.point.y + textDist * sin(angle - PI/2).toFloat() + 8f

					canvas.drawText(curr.hourLabel, px, py, labelPaint)
				}

				// Arrow Logic
				val prevOffset = prev.timeOffsetHours
				val currOffset = curr.timeOffsetHours
				val interval = 2.0

				// Draw arrow every 2 hours
				if (floor(prevOffset / interval) != floor(currOffset / interval)) {
					drawArrow(canvas, curr.point.x, curr.point.y, angle.toDouble())
				}
			}
		}

		skyObjects.forEach { obj ->
			if (obj.isVisible) {
				drawCelestialObject(canvas, obj)
			}
		}

		selectedObject?.let {
			if (it.isVisible) {
				val p = skyToScreen(it.azimuth, it.altitude)
				if (p != null) {
					paint.style = Paint.Style.STROKE
					paint.color = Color.RED
					paint.strokeWidth = 3f
					canvas.drawCircle(p.x, p.y, 25f, paint)
				}
			}
		}
	}

	private fun drawArrow(canvas: Canvas, x: Float, y: Float, angleRad: Double) {
		canvas.save()
		canvas.translate(x, y)
		canvas.rotate(Math.toDegrees(angleRad).toFloat())

		val path = Path()
		val size = 10f
		path.moveTo(size, 0f)
		path.lineTo(-size, -size * 0.6f)
		path.lineTo(-size, size * 0.6f)
		path.close()

		canvas.drawPath(path, arrowPaint)
		canvas.restore()
	}

	private fun drawHorizon(canvas: Canvas) {
		paint.color = Color.parseColor("#003300")
		paint.style = Paint.Style.FILL

		val path = Path()
		var started = false
		for (az in 0..360 step 2) {
			val p = skyToScreen(az.toDouble(), 0.0)
			if (p != null) {
				if (!started) { path.moveTo(p.x, p.y); started = true }
				else path.lineTo(p.x, p.y)
			} else {
				started = false
			}
		}

		paint.color = Color.GREEN
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE
		canvas.drawPath(path, paint)

		val textPaintCards = Paint(textPaint).apply { textSize = 40f; color = Color.GREEN }
		val cardinals = listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)
		cardinals.forEach { (label, az) ->
			val p = skyToScreen(az, 0.0)
			if (p != null) {
				canvas.drawText(label, p.x, p.y - 10, textPaintCards)
			}
		}
	}

	private fun drawGrid(canvas: Canvas) {
		paint.color = Color.parseColor("#333333")
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE

		for (alt in -80..80 step 20) {
			val path = Path()
			var first = true
			for (az in 0..360 step 5) {
				val p = skyToScreen(az.toDouble(), alt.toDouble())
				if (p != null) {
					if (first) { path.moveTo(p.x, p.y); first = false }
					else {
						if (hypot(p.x - (skyToScreen((az-5).toDouble(), alt.toDouble())?.x ?: 0f), 0f) < width/2)
							path.lineTo(p.x, p.y)
						else
							path.moveTo(p.x, p.y)
					}
				} else {
					first = true
				}
			}
			canvas.drawPath(path, paint)
		}

		for (az in 0 until 360 step 45) {
			val path = Path()
			var first = true
			for (alt in -90..90 step 5) {
				val p = skyToScreen(az.toDouble(), alt.toDouble())
				if (p != null) {
					if (first) { path.moveTo(p.x, p.y); first = false }
					else path.lineTo(p.x, p.y)
				}
			}
			canvas.drawPath(path, paint)
		}
	}

	private fun drawCelestialObject(canvas: Canvas, obj: SkyObject) {
		val point = skyToScreen(obj.azimuth, obj.altitude) ?: return

		paint.style = Paint.Style.FILL
		paint.color = obj.color

		val baseSize = 15f
		val radius = max(3f, baseSize - (obj.magnitude * 2f))

		canvas.drawCircle(point.x, point.y, radius, paint)

		if (obj.magnitude < 2.0 || viewAngle < 40.0 || obj == selectedObject) {
			textPaint.textSize = 25f
			textPaint.color = if(obj == selectedObject) Color.YELLOW else Color.LTGRAY
			canvas.drawText(obj.name, point.x + radius + 5, point.y, textPaint)
		}
	}

	private fun skyToScreen(azimuth: Double, altitude: Double): PointF? {
		val azRad = Math.toRadians(azimuth - azimuthCenter)
		val altRad = Math.toRadians(altitude)
		val alt0Rad = Math.toRadians(altitudeCenter)

		val cosC = sin(alt0Rad) * sin(altRad) + cos(alt0Rad) * cos(altRad) * cos(azRad)

		if (cosC <= 0.01) return null

		val k = 1.0 / cosC
		val x = k * cos(altRad) * sin(azRad)
		val y = k * (cos(alt0Rad) * sin(altRad) - sin(alt0Rad) * cos(altRad) * cos(azRad))

		val scale = width / Math.toRadians(viewAngle)

		return PointF(
			(width / 2 + scale * x).toFloat(),
			(height / 2 - scale * y).toFloat()
		)
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		scaleGestureDetector.onTouchEvent(event)
		if (scaleGestureDetector.isInProgress) return true

		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				lastTouchX = event.x
				lastTouchY = event.y
				isPanning = false
			}
			MotionEvent.ACTION_MOVE -> {
				val dx = event.x - lastTouchX
				val dy = event.y - lastTouchY
				if (sqrt(dx * dx + dy * dy) > 10f) {
					isPanning = true

					val scale = viewAngle / width
					azimuthCenter -= dx * scale
					altitudeCenter += dy * scale
					altitudeCenter = max(-90.0, min(90.0, altitudeCenter))

					if (azimuthCenter < 0) azimuthCenter += 360
					if (azimuthCenter >= 360) azimuthCenter -= 360

					lastTouchX = event.x
					lastTouchY = event.y

					if (selectedObject != null) calculateCelestialPath(selectedObject!!)
					invalidate()
				}
			}
			MotionEvent.ACTION_UP -> {
				if (!isPanning) {
					performClickAt(event.x, event.y)
				}
			}
		}
		return true
	}

	private fun performClickAt(x: Float, y: Float) {
		val clickRadius = 60f
		var bestObj: SkyObject? = null
		var bestDist = Float.MAX_VALUE

		skyObjects.forEach { obj ->
			if (!obj.isVisible) return@forEach
			val p = skyToScreen(obj.azimuth, obj.altitude)
			if (p != null) {
				val dist = hypot(x - p.x, y - p.y)
				if (dist < clickRadius && dist < bestDist) {
					bestDist = dist
					bestObj = obj
				}
			}
		}

		selectedObject = bestObj
		if (selectedObject != null) {
			calculateCelestialPath(selectedObject!!)
		}
		invalidate()
		onObjectClickListener?.invoke(bestObj)
	}

	private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
		override fun onScale(detector: ScaleGestureDetector): Boolean {
			viewAngle /= detector.scaleFactor
			viewAngle = max(10.0, min(150.0, viewAngle))
			if (selectedObject != null) calculateCelestialPath(selectedObject!!)
			invalidate()
			return true
		}
	}
}