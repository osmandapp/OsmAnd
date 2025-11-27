package net.osmand.plus.plugins.astro.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.core.graphics.withTranslation
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.rotationEclEqd
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class StarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	// --- Graphics (Cached to avoid allocation) ---
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE
		textSize = 30f
	}
	private val gridTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF888888.toInt()
		textSize = 24f
	}
	private val equGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF006666.toInt()
		style = Paint.Style.STROKE
		strokeWidth = 2f
	}
	private val eclipticPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFFFFFF00.toInt()
		style = Paint.Style.STROKE
		strokeWidth = 4f
		pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
	}
	private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF00FFFF.toInt()
		style = Paint.Style.STROKE
		strokeWidth = 3f
		pathEffect = DashPathEffect(floatArrayOf(10f, 15f), 0f)
	}
	private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF00FFFF.toInt()
		textSize = 24f
		textAlign = Paint.Align.CENTER
	}
	private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF00FFFF.toInt()
		style = Paint.Style.FILL
	}

	// Reusable objects for drawing to prevent GC churn
	private val celestialPath = Path()
	private val tempPoint = PointF()
	private val reusableCal = Calendar.getInstance() // Reusable calendar
	private val arrowPath = Path()

	// --- View State ---
	private var azimuthCenter = 180.0
	private var altitudeCenter = 45.0
	private var viewAngle = 60.0 // Field of view in degrees

	// --- Visibility Flags ---
	var showAzimuthalGrid = true
	var showEquatorialGrid = false
	var showEclipticLine = false

	// --- Interaction ---
	private var lastTouchX = 0f
	private var lastTouchY = 0f
	private var isPanning = false
	private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
	private var onObjectClickListener: ((SkyObject?) -> Unit)? = null

	// Callback for when time animation finishes
	var onAnimationFinished: (() -> Unit)? = null

	// --- Astronomy Data ---
	private val skyObjects = mutableListOf<SkyObject>()

	var observer = Observer(56.9496, 24.1052, 0.0)
	var currentTime = Time(System.currentTimeMillis() / 1000.0 / 86400.0 + 2440587.5)

	private val eclipticStep = 10
	private val eclipticPointsCount = (360 / eclipticStep) + 1
	private val eclipticAzimuths = DoubleArray(eclipticPointsCount)
	private val eclipticAltitudes = DoubleArray(eclipticPointsCount)
	private var lastEclipticTimeT: Double = -1.0
	private var lastEclipticLat: Double = -999.0
	private var lastEclipticLon: Double = -999.0

	private var selectedObject: SkyObject? = null
	private var lastPathTime: Double = -1.0
	private var lastPathLat: Double = -999.0
	private var lastPathLon: Double = -999.0
	private var lastPathObject: SkyObject? = null

	private class PathPoint {
		val point = PointF()
		var hourLabel: String? = null
		var timeOffsetHours: Double = 0.0
		var isValid: Boolean = false
		var azimuth: Double = 0.0
		var altitude: Double = 0.0

		fun reset() {
			hourLabel = null
			isValid = false
		}
	}

	// Pre-allocate a fixed pool of points (max expected steps ~ 24h * 6 per hour = 144)
	private val pathPointPool = Array(200) { PathPoint() }
	private var activePathPointsCount = 0

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
					invalidate()
				}
				addListener(object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: Animator) {
						onAnimationFinished?.invoke()
					}
				})
				start()
			}
		} else {
			currentTime = time
			recalculatePositions(time, updateTargets = true)
			skyObjects.forEach {
				it.azimuth = it.targetAzimuth
				it.altitude = it.targetAltitude
			}
			invalidate()
			onAnimationFinished?.invoke()
		}
	}

	private fun recalculatePositions(time: Time, updateTargets: Boolean) {
		skyObjects.forEach { obj ->
			if (!obj.isVisible && obj != selectedObject) return@forEach

			val hor: Topocentric

			if (obj.type == SkyObject.Type.STAR) {
				hor = horizon(time, observer, obj.ra, obj.dec, Refraction.Normal)
			} else {
				val body = obj.body ?: throw IllegalStateException("Planet without Body enum")
				val equ = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
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

	private fun updatePathAstronomyCache(obj: SkyObject) {
		val timeUnchanged = kotlin.math.abs(currentTime.tt - lastPathTime) < 0.0000001
		val locUnchanged = observer.latitude == lastPathLat && observer.longitude == lastPathLon
		val objUnchanged = obj == lastPathObject

		if (timeUnchanged && locUnchanged && objUnchanged) return

		activePathPointsCount = 0

		val startHours = -12
		val endHours = 12
		val stepMinutes = 10

		val totalMinutes = (endHours - startHours) * 60
		val steps = totalMinutes / stepMinutes

		val tz = TimeZone.getDefault()
		val currentMillis = currentTime.toMillisecondsSince1970()
		for (i in 0..steps) {
			if (activePathPointsCount >= pathPointPool.size) break

			// Calculate time for this step
			reusableCal.timeZone = tz
			reusableCal.timeInMillis = currentMillis
			reusableCal.add(Calendar.HOUR_OF_DAY, startHours)
			reusableCal.set(Calendar.MINUTE, 0)
			reusableCal.set(Calendar.SECOND, 0)
			reusableCal.set(Calendar.MILLISECOND, 0)

			val stepTimeMillis = reusableCal.timeInMillis + (i * stepMinutes * 60000L)

			// Limit to +12h from now
			val maxTime = currentMillis + (endHours * 3600000L)
			if (stepTimeMillis > maxTime + 3600000L) break

			val tStep = Time.fromMillisecondsSince1970(stepTimeMillis)

			reusableCal.timeInMillis = stepTimeMillis
			val stepMinute = reusableCal.get(Calendar.MINUTE)
			val stepHour = reusableCal.get(Calendar.HOUR_OF_DAY)
			val isHourMark = (stepMinute == 0)

			val poolItem = pathPointPool[activePathPointsCount]
			poolItem.reset()
			poolItem.hourLabel = if (isHourMark) "%02d".format(stepHour) else null
			poolItem.timeOffsetHours = (stepTimeMillis - currentMillis) / 3600000.0

			val altAz: Topocentric = if (obj.type == SkyObject.Type.STAR) {
				horizon(tStep, observer, obj.ra, obj.dec, Refraction.Normal)
			} else {
				val body = obj.body!!
				val eq = equator(body, tStep, observer, EquatorEpoch.OfDate, Aberration.Corrected)
				horizon(tStep, observer, eq.ra, eq.dec, Refraction.Normal)
			}

			// Store Sky Coordinates ONLY
			poolItem.azimuth = altAz.azimuth
			poolItem.altitude = altAz.altitude

			activePathPointsCount++
		}

		lastPathTime = currentTime.tt
		lastPathLat = observer.latitude
		lastPathLon = observer.longitude
		lastPathObject = obj
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		canvas.drawColor(Color.BLACK)

		if (showEquatorialGrid) drawEquatorialGrid(canvas)
		if (showAzimuthalGrid) drawAzimuthalGrid(canvas)
		if (showEclipticLine) drawEclipticLine(canvas)

		drawHorizon(canvas)

		if (selectedObject != null) {
			updatePathAstronomyCache(selectedObject!!)

			for (i in 0 until activePathPointsCount) {
				val item = pathPointPool[i]
				item.isValid = skyToScreen(item.azimuth, item.altitude, item.point)
			}

			if (activePathPointsCount > 1) {
				celestialPath.reset()
				var isPenDown = false

				for (i in 0 until activePathPointsCount) {
					val curr = pathPointPool[i]

					if (!curr.isValid) {
						isPenDown = false
						continue
					}

					if (isPenDown && i > 0) {
						val prev = pathPointPool[i-1]
						if (prev.isValid) {
							val dist = hypot(curr.point.x - prev.point.x, curr.point.y - prev.point.y)
							if (dist > width * 0.8) isPenDown = false
						}
					}

					if (!isPenDown) {
						celestialPath.moveTo(curr.point.x, curr.point.y)
						isPenDown = true
					} else {
						celestialPath.lineTo(curr.point.x, curr.point.y)
					}
				}
				canvas.drawPath(celestialPath, pathPaint)

				for (i in 1 until activePathPointsCount - 1) {
					val curr = pathPointPool[i]
					if (!curr.isValid || curr.hourLabel == null) continue

					val prev = pathPointPool[i-1]
					val next = pathPointPool[i+1]
					if (!prev.isValid || !next.isValid) continue

					if (hypot(curr.point.x - prev.point.x, curr.point.y - prev.point.y) > 200) continue
					if (hypot(next.point.x - curr.point.x, next.point.y - curr.point.y) > 200) continue

					val dx = next.point.x - prev.point.x
					val dy = next.point.y - prev.point.y
					val angle = atan2(dy, dx)

					val textDist = 30f
					val px = curr.point.x + textDist * cos(angle - PI/2).toFloat()
					val py = curr.point.y + textDist * sin(angle - PI/2).toFloat() + 8f

					canvas.drawText(curr.hourLabel!!, px, py, labelPaint)
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
				if (skyToScreen(it.azimuth, it.altitude, tempPoint)) {
					paint.style = Paint.Style.STROKE
					paint.color = Color.RED
					paint.strokeWidth = 3f
					canvas.drawCircle(tempPoint.x, tempPoint.y, 25f, paint)
				}
			}
		}
	}

	private fun drawArrow(canvas: Canvas, x: Float, y: Float, angleRad: Double) {
		canvas.withTranslation(x, y) {
			rotate(Math.toDegrees(angleRad).toFloat())
			arrowPath.reset()
			val size = 10f
			arrowPath.moveTo(size, 0f)
			arrowPath.lineTo(-size, -size * 0.6f)
			arrowPath.lineTo(-size, size * 0.6f)
			arrowPath.close()
			drawPath(arrowPath, arrowPaint)
		}
	}

	private fun drawHorizon(canvas: Canvas) {
		paint.color = 0xFF003300.toInt()
		paint.style = Paint.Style.FILL

		val path = Path()
		var started = false
		for (az in 0..360 step 2) {
			if (skyToScreen(az.toDouble(), 0.0, tempPoint)) {
				if (!started) { path.moveTo(tempPoint.x, tempPoint.y); started = true }
				else path.lineTo(tempPoint.x, tempPoint.y)
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
			if (skyToScreen(az, 0.0, tempPoint)) {
				canvas.drawText(label, tempPoint.x, tempPoint.y - 10, textPaintCards)
			}
		}
	}

	private fun drawAzimuthalGrid(canvas: Canvas) {
		paint.color = 0xFF444444.toInt()
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE

		// Draw Parallels
		for (alt in -80..80 step 20) {
			val path = Path()
			var first = true
			for (az in 0..360 step 5) {
				if (skyToScreen(az.toDouble(), alt.toDouble(), tempPoint)) {
					if (first) { path.moveTo(tempPoint.x, tempPoint.y); first = false }
					else path.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}
			canvas.drawPath(path, paint)

			if (alt != 0) {
				if (skyToScreen(azimuthCenter, alt.toDouble(), tempPoint)) {
					gridTextPaint.textAlign = Paint.Align.LEFT
					gridTextPaint.color = 0xFF888888.toInt()
					canvas.drawText("${alt}°", tempPoint.x + 5f, tempPoint.y - 5f, gridTextPaint)
				}
			}
		}

		// Draw Meridians
		for (az in 0 until 360 step 45) {
			val path = Path()
			var first = true
			for (alt in -90..90 step 5) {
				if (skyToScreen(az.toDouble(), alt.toDouble(), tempPoint)) {
					if (first) { path.moveTo(tempPoint.x, tempPoint.y); first = false }
					else path.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}
			canvas.drawPath(path, paint)

			if (az % 90 != 0) {
				if (skyToScreen(az.toDouble(), 0.0, tempPoint)) {
					gridTextPaint.textAlign = Paint.Align.CENTER
					gridTextPaint.color = 0xFF888888.toInt()
					canvas.drawText("${az}°", tempPoint.x, tempPoint.y - 10f, gridTextPaint)
				}
			}
		}
	}

	private fun drawEquatorialGrid(canvas: Canvas) {
		for (ra in 0 until 24 step 2) {
			val path = Path()
			var first = true
			for (dec in -90..90 step 5) {
				val hor = horizon(currentTime, observer, ra.toDouble(), dec.toDouble(), Refraction.None)
				if (skyToScreen(hor.azimuth, hor.altitude, tempPoint)) {
					if (first) { path.moveTo(tempPoint.x, tempPoint.y); first = false }
					else path.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}
			canvas.drawPath(path, equGridPaint)
		}

		for (dec in -80..80 step 20) {
			val path = Path()
			var first = true
			for (raStep in 0..360 step 5) {
				val ra = raStep / 15.0
				val hor = horizon(currentTime, observer, ra, dec.toDouble(), Refraction.None)
				if (skyToScreen(hor.azimuth, hor.altitude, tempPoint)) {
					if (first) { path.moveTo(tempPoint.x, tempPoint.y); first = false }
					else path.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}
			canvas.drawPath(path, equGridPaint)
		}
	}

	private fun updateEclipticCache() {
		val timeUnchanged = kotlin.math.abs(currentTime.tt - lastEclipticTimeT) < 0.0000001
		val locUnchanged = observer.latitude == lastEclipticLat && observer.longitude == lastEclipticLon
		if (timeUnchanged && locUnchanged) {
			return
		}

		val rotMat = rotationEclEqd(currentTime)
		var index = 0
		for (lon in 0..360 step eclipticStep) {
			if (index >= eclipticPointsCount) break

			val radLon = Math.toRadians(lon.toDouble())
			val vecEcl = Vector(cos(radLon), sin(radLon), 0.0, currentTime)
			val vecEqd = rotMat.rotate(vecEcl)
			val equ = vecEqd.toEquatorial()
			val hor = horizon(currentTime, observer, equ.ra, equ.dec, Refraction.Normal)

			eclipticAzimuths[index] = hor.azimuth
			eclipticAltitudes[index] = hor.altitude
			index++
		}

		lastEclipticTimeT = currentTime.tt
		lastEclipticLat = observer.latitude
		lastEclipticLon = observer.longitude
	}

	private fun drawEclipticLine(canvas: Canvas) {
		updateEclipticCache()

		val path = Path()
		var first = true
		for (i in 0 until eclipticPointsCount) {
			val az = eclipticAzimuths[i]
			val alt = eclipticAltitudes[i]
			if (skyToScreen(az, alt, tempPoint)) {
				if (first) {
					path.moveTo(tempPoint.x, tempPoint.y)
					first = false
				} else {
					path.lineTo(tempPoint.x, tempPoint.y)
				}
			} else {
				first = true
			}
		}
		canvas.drawPath(path, eclipticPaint)
	}

	private fun drawCelestialObject(canvas: Canvas, obj: SkyObject) {
		if (!skyToScreen(obj.azimuth, obj.altitude, tempPoint)) return

		paint.style = Paint.Style.FILL
		paint.color = obj.color

		val baseSize = 15f
		val radius = max(3f, baseSize - (obj.magnitude * 2f))

		canvas.drawCircle(tempPoint.x, tempPoint.y, radius, paint)

		if (obj.magnitude < 2.0 || viewAngle < 40.0 || obj == selectedObject) {
			textPaint.textSize = 25f
			textPaint.color = if(obj == selectedObject) Color.YELLOW else Color.LTGRAY
			canvas.drawText(obj.name, tempPoint.x + radius + 5, tempPoint.y, textPaint)
		}
	}

	/**
	 * Projects sky coordinates (Az/Alt) to Screen (X/Y).
	 * Returns true if the point is visible (in front of camera/not clipped), false otherwise.
	 * Writes result into [outPoint] to avoid allocation.
	 */
	private fun skyToScreen(azimuth: Double, altitude: Double, outPoint: PointF): Boolean {
		val azRad = Math.toRadians(azimuth - azimuthCenter)
		val altRad = Math.toRadians(altitude)
		val alt0Rad = Math.toRadians(altitudeCenter)

		val sinAlt = sin(altRad)
		val cosAlt = cos(altRad)
		val sinAlt0 = sin(alt0Rad)
		val cosAlt0 = cos(alt0Rad)
		val cosAz = cos(azRad)
		val sinAz = sin(azRad)

		// Cosine of angular distance from center
		val cosC = sinAlt0 * sinAlt + cosAlt0 * cosAlt * cosAz

		// Clipping for Stereographic projection (~105 degrees from center)
		if (cosC <= -0.3) return false

		// Stereographic Projection Factor
		val k = 2.0 / (1.0 + cosC)

		val x = k * cosAlt * sinAz
		val y = k * (cosAlt0 * sinAlt - sinAlt0 * cosAlt * cosAz)

		val viewAngleRad = Math.toRadians(viewAngle)
		val scale = width / (4.0 * tan(viewAngleRad / 4.0))

		outPoint.x = (width / 2 + scale * x).toFloat()
		outPoint.y = (height / 2 - scale * y).toFloat()
		return true
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		scaleGestureDetector.onTouchEvent(event)
		if (scaleGestureDetector.isInProgress) {
			lastTouchX = event.x
			lastTouchY = event.y
			return true
		}

		when (event.actionMasked) {
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

					invalidate()
				}
			}
			MotionEvent.ACTION_POINTER_UP -> {
				if (event.actionIndex == 0 && event.pointerCount > 1) {
					lastTouchX = event.getX(1)
					lastTouchY = event.getY(1)
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

		// Iterate through all skyObjects to find the closest one
		for (obj in skyObjects) {
			if (!obj.isVisible) continue
			if (skyToScreen(obj.azimuth, obj.altitude, tempPoint)) {
				val dist = hypot(x - tempPoint.x, y - tempPoint.y)
				if (dist < clickRadius && dist < bestDist) {
					bestDist = dist
					bestObj = obj
				}
			}
		}

		selectedObject = bestObj
		invalidate()
		onObjectClickListener?.invoke(bestObj)
	}

	private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
		override fun onScale(detector: ScaleGestureDetector): Boolean {
			viewAngle /= detector.scaleFactor
			viewAngle = max(10.0, min(150.0, viewAngle))
			invalidate()
			return true
		}
	}
}