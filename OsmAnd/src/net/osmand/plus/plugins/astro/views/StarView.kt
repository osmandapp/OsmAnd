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
import android.graphics.Typeface
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
import net.osmand.plus.plugins.astro.AstroDataProvider
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
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
	private val cardinalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.GREEN
		textSize = 40f
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
	private val constellationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF5599FF.toInt()
		style = Paint.Style.STROKE
		strokeWidth = 2f
		alpha = 150
	}
	private val constellationTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFF5599FF.toInt()
		textSize = 32f
		textAlign = Paint.Align.CENTER
		typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
		alpha = 200
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
	private val gridPath = Path()
	private val tempPoint = PointF()
	private val tempPoint2 = PointF()
	private val reusableCal = Calendar.getInstance() // Reusable calendar
	private val arrowPath = Path()

	// --- View State ---
	private var azimuthCenter = 180.0
	private var altitudeCenter = 45.0
	private var viewAngle = 60.0 // Field of view in degrees

	// Cached values calculated once per frame/state change to avoid
	// repeated trig and division operations in skyToScreen()
	private var projSinAltCenter = 0.0
	private var projCosAltCenter = 1.0
	private var projScale = 1.0
	private var projHalfWidth = 0.0
	private var projHalfHeight = 0.0

	// --- Visibility Flags ---
	var showAzimuthalGrid = true
	var showEquatorialGrid = false
	var showEclipticLine = false
	var showConstellations = true

	// --- Interaction ---
	private var lastTouchX = 0f
	private var lastTouchY = 0f
	private var isPanning = false
	private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
	private var onObjectClickListener: ((SkyObject?) -> Unit)? = null

	var onAnimationFinished: (() -> Unit)? = null
	var onAzimuthManualChangeListener: ((Double) -> Unit)? = null
	var onViewAngleChangeListener: ((Double) -> Unit)? = null

	// --- View State ---
	var roll = 0.0 // Camera roll in degrees
		set(value) {
			if (abs(field - value) > 0.1) {
				field = value
				invalidate()
			}
		}

	// --- Astronomy Data ---
	private val skyObjects = mutableListOf<SkyObject>()
	private var constellations = listOf<AstroDataProvider.Constellation>()

	// Fast lookup map for constellation drawing
	private val skyObjectMap = mutableMapOf<String, SkyObject>()

	var observer = Observer(56.9496, 24.1052, 0.0)
	var currentTime = Time(System.currentTimeMillis() / 1000.0 / 86400.0 + 2440587.5)

	// Ecliptic Cache
	private val eclipticStep = 10
	private val eclipticPointsCount = (360 / eclipticStep) + 1
	private val eclipticAzimuths = DoubleArray(eclipticPointsCount)
	private val eclipticAltitudes = DoubleArray(eclipticPointsCount)
	private var lastEclipticTimeT: Double = -1.0
	private var lastEclipticLat: Double = -999.0
	private var lastEclipticLon: Double = -999.0

	// Equatorial Grid Cache
	private var lastEquGridTimeT: Double = -1.0
	private var lastEquGridLat: Double = -999.0
	private var lastEquGridLon: Double = -999.0

	// RA Lines (Meridians)
	private val equRaStep = 2 // Hour step for meridians (0, 2, 4...)
	private val equRaDecStep = 5 // Degree step for points along the meridian
	private val equRaLinesCount = 24 / equRaStep
	private val equRaPointsCount = (180 / equRaDecStep) + 1 // -90 to 90
	private val equRaAzimuths = Array(equRaLinesCount) { DoubleArray(equRaPointsCount) }
	private val equRaAltitudes = Array(equRaLinesCount) { DoubleArray(equRaPointsCount) }

	// Dec Lines (Parallels)
	private val equDecStep = 20 // Degree step for parallels (-80, -60... 80)
	private val equDecRaStep = 5 // Degree step for points along the parallel (0..360)
	private val equDecLinesCount = (160 / equDecStep) + 1 // -80 to 80 range is 160
	private val equDecPointsCount = (360 / equDecRaStep) + 1 // 0 to 360
	private val equDecAzimuths = Array(equDecLinesCount) { DoubleArray(equDecPointsCount) }
	private val equDecAltitudes = Array(equDecLinesCount) { DoubleArray(equDecPointsCount) }

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

	/**
	 * Sets the center of the view. Useful for AR mode or immediate updates.
	 * Azimuth: 0 = North, 90 = East, etc.
	 * Altitude: 90 = Zenith, 0 = Horizon.
	 */
	fun setCenter(azimuth: Double, altitude: Double) {
		this.azimuthCenter = azimuth
		this.altitudeCenter = max(-90.0, min(90.0, altitude))
		invalidate()
	}

	fun setViewAngle(angle: Double) {
		val newAngle = max(10.0, min(150.0, angle))
		if (abs(this.viewAngle - newAngle) > 0.001) {
			this.viewAngle = newAngle
			onViewAngleChangeListener?.invoke(newAngle)
			invalidate()
		}
	}

	fun setAzimuth(azimuth: Double, animate: Boolean = false, fps: Int? = 30) {
		if (abs(azimuthCenter - azimuth) < 0.5) return

		visualAnimator?.cancel()
		if (animate) {
			val startAz = azimuthCenter
			var lastFrameTime = 0L
			val frameInterval = if (fps != null && fps > 0) 1000L / fps else 0L

			visualAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
				duration = 400
				interpolator = DecelerateInterpolator()
				addUpdateListener { animator ->
					val currentTime = System.currentTimeMillis()
					val fraction = animator.animatedValue as Float

					if (frameInterval == 0L || currentTime - lastFrameTime >= frameInterval || fraction == 1f) {
						azimuthCenter = interpolateAngle(startAz, azimuth, fraction)
						invalidate()
						lastFrameTime = currentTime
					}
				}
				start()
			}
		} else {
			azimuthCenter = azimuth
			invalidate()
		}
	}

	fun setSkyObjects(objects: List<SkyObject>) {
		skyObjects.clear()
		skyObjects.addAll(objects)
		skyObjectMap.clear()
		objects.forEach { skyObjectMap[it.id] = it }

		recalculatePositions(currentTime, updateTargets = false)
		skyObjects.forEach {
			it.azimuth = it.targetAzimuth
			it.altitude = it.targetAltitude
		}
		invalidate()
	}

	fun setConstellations(list: List<AstroDataProvider.Constellation>) {
		constellations = list
		invalidate()
	}

	fun updateVisibility() {
		recalculatePositions(currentTime, updateTargets = false)
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

	fun zoomIn() {
		val newAngle = max(10.0, min(150.0, viewAngle / 1.5))
		if (abs(viewAngle - newAngle) > 0.001) {
			viewAngle = newAngle
			onViewAngleChangeListener?.invoke(viewAngle)
			invalidate()
		}
	}

	fun zoomOut() {
		val newAngle = max(10.0, min(150.0, viewAngle * 1.5))
		if (abs(viewAngle - newAngle) > 0.001) {
			viewAngle = newAngle
			onViewAngleChangeListener?.invoke(viewAngle)
			invalidate()
		}
	}

	private fun recalculatePositions(time: Time, updateTargets: Boolean) {
		skyObjects.forEach { obj ->
			if (!obj.isVisible && obj != selectedObject && !isShowInConstellation(obj)) return@forEach

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

	private fun isShowInConstellation(obj: SkyObject) : Boolean {
		// If constellations are shown, we must calculate positions for stars that are part of them,
		// even if the star itself is "hidden" in settings.
		// However, for optimization, we currently assume stars in constellations are generally visible.
		return showConstellations
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

		// Do this once per frame before any calls to skyToScreen
		updateProjectionCache()

		canvas.drawColor(Color.BLACK)

		if (showEquatorialGrid) drawEquatorialGrid(canvas)
		if (showAzimuthalGrid) drawAzimuthalGrid(canvas)
		if (showEclipticLine) drawEclipticLine(canvas)
		if (showConstellations) drawConstellations(canvas)

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

		gridPath.reset()
		var started = false
		for (az in 0..360 step 2) {
			if (skyToScreen(az.toDouble(), 0.0, tempPoint)) {
				if (!started) { gridPath.moveTo(tempPoint.x, tempPoint.y); started = true }
				else gridPath.lineTo(tempPoint.x, tempPoint.y)
			} else {
				started = false
			}
		}

		paint.color = Color.GREEN
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE
		canvas.drawPath(gridPath, paint)

		// Optimization: Use cached Paint object
		val cardinals = listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)
		cardinals.forEach { (label, az) ->
			if (skyToScreen(az, 0.0, tempPoint)) {
				canvas.drawText(label, tempPoint.x, tempPoint.y - 10, cardinalTextPaint)
			}
		}
	}

	private fun drawAzimuthalGrid(canvas: Canvas) {
		paint.color = 0xFF444444.toInt()
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE

		gridPath.reset()

		// Draw Parallels
		for (alt in -80..80 step 20) {
			// No new Path() allocation here
			var first = true
			for (az in 0..360 step 5) {
				if (skyToScreen(az.toDouble(), alt.toDouble(), tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}

			// Text must still be drawn individually, but we check if we should first
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
			var first = true
			for (alt in -90..90 step 5) {
				if (skyToScreen(az.toDouble(), alt.toDouble(), tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}

			if (az % 90 != 0) {
				if (skyToScreen(az.toDouble(), 0.0, tempPoint)) {
					gridTextPaint.textAlign = Paint.Align.CENTER
					gridTextPaint.color = 0xFF888888.toInt()
					canvas.drawText("${az}°", tempPoint.x, tempPoint.y - 10f, gridTextPaint)
				}
			}
		}

		canvas.drawPath(gridPath, paint)
	}

	private fun updateEquatorialGridCache() {
		val timeUnchanged = kotlin.math.abs(currentTime.tt - lastEquGridTimeT) < 0.0000001
		val locUnchanged = observer.latitude == lastEquGridLat && observer.longitude == lastEquGridLon
		if (timeUnchanged && locUnchanged) return

		// Cache RA Lines (Meridians)
		var lineIdx = 0
		for (ra in 0 until 24 step equRaStep) {
			var pointIdx = 0
			for (dec in -90..90 step equRaDecStep) {
				// Safety check to prevent index out of bounds if loops change
				if (lineIdx < equRaLinesCount && pointIdx < equRaPointsCount) {
					val hor = horizon(currentTime, observer, ra.toDouble(), dec.toDouble(), Refraction.Normal)
					equRaAzimuths[lineIdx][pointIdx] = hor.azimuth
					equRaAltitudes[lineIdx][pointIdx] = hor.altitude
				}
				pointIdx++
			}
			lineIdx++
		}

		// Cache Dec Lines (Parallels)
		lineIdx = 0
		for (dec in -80..80 step equDecStep) {
			var pointIdx = 0
			for (raStep in 0..360 step equDecRaStep) {
				if (lineIdx < equDecLinesCount && pointIdx < equDecPointsCount) {
					val ra = raStep / 15.0
					val hor = horizon(currentTime, observer, ra, dec.toDouble(), Refraction.Normal)
					equDecAzimuths[lineIdx][pointIdx] = hor.azimuth
					equDecAltitudes[lineIdx][pointIdx] = hor.altitude
				}
				pointIdx++
			}
			lineIdx++
		}

		lastEquGridTimeT = currentTime.tt
		lastEquGridLat = observer.latitude
		lastEquGridLon = observer.longitude
	}

	private fun drawEquatorialGrid(canvas: Canvas) {
		updateEquatorialGridCache()

		gridPath.reset()

		// Draw RA Lines
		for (i in 0 until equRaLinesCount) {
			var first = true
			for (j in 0 until equRaPointsCount) {
				val az = equRaAzimuths[i][j]
				val alt = equRaAltitudes[i][j]
				if (skyToScreen(az, alt, tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}
		}

		// Draw Dec Lines
		for (i in 0 until equDecLinesCount) {
			var first = true
			for (j in 0 until equDecPointsCount) {
				val az = equDecAzimuths[i][j]
				val alt = equDecAltitudes[i][j]
				if (skyToScreen(az, alt, tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)
				} else {
					first = true
				}
			}
		}

		canvas.drawPath(gridPath, equGridPaint)
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

		gridPath.reset()
		var first = true
		for (i in 0 until eclipticPointsCount) {
			val az = eclipticAzimuths[i]
			val alt = eclipticAltitudes[i]
			if (skyToScreen(az, alt, tempPoint)) {
				if (first) {
					gridPath.moveTo(tempPoint.x, tempPoint.y)
					first = false
				} else {
					gridPath.lineTo(tempPoint.x, tempPoint.y)
				}
			} else {
				first = true
			}
		}
		canvas.drawPath(gridPath, eclipticPaint)
	}

	private fun drawConstellations(canvas: Canvas) {
		gridPath.reset()

		constellations.forEach { constellation ->
			val uniqueStars = mutableSetOf<String>()

			constellation.lines.forEach { (id1, id2) ->
				uniqueStars.add(id1)
				uniqueStars.add(id2)

				val star1 = skyObjectMap[id1]
				val star2 = skyObjectMap[id2]

				if (star1 != null && star2 != null) {
					val p1Visible = skyToScreen(star1.azimuth, star1.altitude, tempPoint)
					val p2Visible = skyToScreen(star2.azimuth, star2.altitude, tempPoint2)

					if (p1Visible && p2Visible) {
						// Simple line if both visible
						// (Clipping logic for partially visible lines could be added here,
						// but simple check is usually sufficient for short constellation lines)
						val dist = hypot(tempPoint.x - tempPoint2.x, tempPoint.y - tempPoint2.y)
						// Don't draw if lines wrap around screen weirdly (simple heuristic)
						if (dist < width * 0.8) {
							gridPath.moveTo(tempPoint.x, tempPoint.y)
							gridPath.lineTo(tempPoint2.x, tempPoint2.y)
						}
					}
				}
			}
			// Draw Title
			var avgX = 0f
			var avgY = 0f
			var count = 0
			uniqueStars.forEach { id ->
				val star = skyObjectMap[id]
				if (star != null && skyToScreen(star.azimuth, star.altitude, tempPoint)) {
					avgX += tempPoint.x
					avgY += tempPoint.y
					count++
				}
			}

			if (count > 0) {
				// Only draw if a significant portion of the constellation is visible?
				// Or just if any part is visible.
				// If count > 0, at least one star is on screen.
				// Calculate centroid
				val cx = avgX / count
				val cy = avgY / count
				canvas.drawText(constellation.name, cx, cy, constellationTextPaint)
			}
		}
		canvas.drawPath(gridPath, constellationPaint)
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
	private fun updateProjectionCache() {
		// Calculate Frame Invariants
		val alt0Rad = Math.toRadians(altitudeCenter)
		projSinAltCenter = sin(alt0Rad)
		projCosAltCenter = cos(alt0Rad)

		val viewAngleRad = Math.toRadians(viewAngle)
		// Stereographic projection scale factor based on field of view
		// This replaces the repeated: width / (4.0 * tan(viewAngleRad / 4.0))
		projScale = width / (4.0 * tan(viewAngleRad / 4.0))

		projHalfWidth = width / 2.0
		projHalfHeight = height / 2.0
	}

	private fun skyToScreen(azimuth: Double, altitude: Double, outPoint: PointF): Boolean {
		// Convert to radians (required for trig)
		val azRad = Math.toRadians(azimuth - azimuthCenter)
		val altRad = Math.toRadians(altitude)

		// Compute trig values for the point
		val sinAlt = sin(altRad)
		val cosAlt = cos(altRad)
		val sinAz = sin(azRad)
		val cosAz = cos(azRad)

		// Cosine of angular distance from center (using cached center trigs)
		val cosC = projSinAltCenter * sinAlt + projCosAltCenter * cosAlt * cosAz

		// Clipping for Stereographic projection (~105 degrees from center)
		if (cosC <= -0.3) return false

		// Stereographic Projection Factor
		val k = 2.0 / (1.0 + cosC)
		val combinedScale = k * projScale

		// Raw projected coordinates (unscaled)
		val xRaw = cosAlt * sinAz
		val yRaw = projCosAltCenter * sinAlt - projSinAltCenter * cosAlt * cosAz

		// Apply scale (centered at 0,0 for now)
		val xScaled = combinedScale * xRaw
		val yScaled = -combinedScale * yRaw // Invert Y for screen coordinates (up is -, down is +)

		// Apply Roll Rotation (2D rotation of the projected plane)
		// roll is clockwise rotation of the camera.
		// If phone rotates CW (roll > 0), the world appears to rotate CCW.
		// So we want to rotate the projected points CCW?
		// User says: "When I roll the phone right, line of horizon rolls to the right, but should roll to the left"
		// This means the current logic moved the horizon WITH the phone (Right).
		// We want it to move LEFT (Opposite to phone).
		// Previous was -roll. So let's try +roll.
		val rollRad = Math.toRadians(roll) 
		val sinRoll = sin(rollRad)
		val cosRoll = cos(rollRad)

		val xRot = xScaled * cosRoll - yScaled * sinRoll
		val yRot = xScaled * sinRoll + yScaled * cosRoll

		// Translate to screen center
		outPoint.x = (projHalfWidth + xRot).toFloat()
		outPoint.y = (projHalfHeight + yRot).toFloat()
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

					// Notify listener of manual change
					onAzimuthManualChangeListener?.invoke(azimuthCenter)

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
			val newAngle = max(10.0, min(150.0, viewAngle / detector.scaleFactor))
			if (abs(viewAngle - newAngle) > 0.001) {
				viewAngle = newAngle
				onViewAngleChangeListener?.invoke(viewAngle)
				invalidate()
			}
			return true
		}
	}
}