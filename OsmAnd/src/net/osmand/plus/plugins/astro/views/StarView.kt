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
import io.github.cosinekitty.astronomy.Spherical
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
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation

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
	private val gridTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#888888".toColorInt()
		textSize = 24f
	}
	private val equGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#006666".toColorInt()
		style = Paint.Style.STROKE
		strokeWidth = 2f
	}
	private val eclipticPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#FFFF00".toColorInt()
		style = Paint.Style.STROKE
		strokeWidth = 4f
		pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
	}
	private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#00FFFF".toColorInt()
		style = Paint.Style.STROKE
		strokeWidth = 3f
		pathEffect = DashPathEffect(floatArrayOf(10f, 15f), 0f)
	}
	private val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#00FFFF".toColorInt()
		style = Paint.Style.STROKE
		strokeWidth = 3f
	}
	private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#00FFFF".toColorInt()
		textSize = 24f
		textAlign = Paint.Align.CENTER
	}
	private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#00FFFF".toColorInt()
		style = Paint.Style.FILL
	}
	private val celestialpath = Path()

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

	// --- Astronomy Data ---
	private val skyObjects = mutableListOf<SkyObject>()

	// Made public so Fragment can access location for Rise/Set calculations
	var observer = Observer(56.9496, 24.1052, 0.0)

	// Made public so Fragment can access current simulation time
	var currentTime = Time(System.currentTimeMillis() / 1000.0 / 86400.0 + 2440587.5)

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

		if (showEquatorialGrid) drawEquatorialGrid(canvas)
		if (showAzimuthalGrid) drawAzimuthalGrid(canvas)
		if (showEclipticLine) drawEclipticLine(canvas)

		drawHorizon(canvas)

		if (selectedObject != null && celestialPathPoints.size > 1) {
			celestialpath.reset()

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
					celestialpath.moveTo(curr.point.x, curr.point.y)
					isPenDown = true
				} else {
					celestialpath.lineTo(curr.point.x, curr.point.y)
				}
			}
			canvas.drawPath(celestialpath, pathPaint)

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
					// Text: Offset perpendicular to path
					val textDist = 30f
					val px = curr.point.x + textDist * cos(angle - PI/2).toFloat()
					val py = curr.point.y + textDist * sin(angle - PI/2).toFloat() + 8f

					canvas.drawText(curr.hourLabel, px, py, labelPaint)
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
		canvas.withTranslation(x, y) {
			rotate(Math.toDegrees(angleRad).toFloat())

			val path = Path()
			val size = 10f
			path.moveTo(size, 0f)
			path.lineTo(-size, -size * 0.6f)
			path.lineTo(-size, size * 0.6f)
			path.close()

			drawPath(path, arrowPaint)
		}
	}

	private fun drawHorizon(canvas: Canvas) {
		paint.color = "#003300".toColorInt()
		paint.style = Paint.Style.FILL

		val path = Path()
		var started = false
		for (az in 0..360 step 2) {
			val p = skyToScreen(az.toDouble(), 0.0)
			if (p != null) {
				if (!started) { path.moveTo(p.x, p.y); started = true }
				else path.lineTo(p.x, p.y)
			} else {
				// Gap in horizon (behind view)
				started = false
			}
		}
		// Do NOT close path here for stereographic projection clipping.
		// Closing it creates a chord line across the screen if the horizon is clipped.

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

	private fun drawAzimuthalGrid(canvas: Canvas) {
		paint.color = "#444444".toColorInt()
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE

		// Draw Parallels (Latitude lines)
		for (alt in -80..80 step 20) {
			val path = Path()
			var first = true
			for (az in 0..360 step 5) {
				val p = skyToScreen(az.toDouble(), alt.toDouble())
				if (p != null) {
					if (first) { path.moveTo(p.x, p.y); first = false }
					else path.lineTo(p.x, p.y)
				} else {
					first = true
				}
			}
			canvas.drawPath(path, paint)

			// Label Parallels (along central meridian)
			if (alt != 0) {
				val pLabel = skyToScreen(azimuthCenter, alt.toDouble())
				if (pLabel != null) {
					gridTextPaint.textAlign = Paint.Align.LEFT
					gridTextPaint.color = "#888888".toColorInt()
					canvas.drawText("${alt}°", pLabel.x + 5f, pLabel.y - 5f, gridTextPaint)
				}
			}
		}

		// Draw Meridians (Longitude lines)
		for (az in 0 until 360 step 45) {
			val path = Path()
			var first = true
			for (alt in -90..90 step 5) {
				val p = skyToScreen(az.toDouble(), alt.toDouble())
				if (p != null) {
					if (first) { path.moveTo(p.x, p.y); first = false }
					else path.lineTo(p.x, p.y)
				} else {
					// Important: If points are clipped (behind view), break the path.
					// Otherwise it draws a straight line across the clipped region.
					first = true
				}
			}
			canvas.drawPath(path, paint)

			// Label Meridians (at horizon)
			// Skip cardinals (0, 90, 180, 270) as they are drawn by drawHorizon
			if (az % 90 != 0) {
				val pLabel = skyToScreen(az.toDouble(), 0.0)
				if (pLabel != null) {
					gridTextPaint.textAlign = Paint.Align.CENTER
					gridTextPaint.color = "#888888".toColorInt()
					canvas.drawText("${az}°", pLabel.x, pLabel.y - 10f, gridTextPaint)
				}
			}
		}
	}

	private fun drawEquatorialGrid(canvas: Canvas) {
		// Right Ascension lines (Meridians)
		for (ra in 0 until 24 step 2) {
			val path = Path()
			var first = true
			// Step through declination from -90 to +90
			for (dec in -90..90 step 5) {
				val hor = horizon(currentTime, observer, ra.toDouble(), dec.toDouble(), Refraction.None)
				val p = skyToScreen(hor.azimuth, hor.altitude)
				if (p != null) {
					if (first) { path.moveTo(p.x, p.y); first = false }
					else path.lineTo(p.x, p.y)
				} else {
					first = true
				}
			}
			canvas.drawPath(path, equGridPaint)
		}

		// Declination lines (Parallels)
		for (dec in -80..80 step 20) {
			val path = Path()
			var first = true
			// Step through RA from 0 to 24
			for (raStep in 0..360 step 5) {
				val ra = raStep / 15.0
				val hor = horizon(currentTime, observer, ra, dec.toDouble(), Refraction.None)
				val p = skyToScreen(hor.azimuth, hor.altitude)
				if (p != null) {
					if (first) { path.moveTo(p.x, p.y); first = false }
					else path.lineTo(p.x, p.y)
				} else {
					first = true
				}
			}
			canvas.drawPath(path, equGridPaint)
		}
	}

	private fun drawEclipticLine(canvas: Canvas) {
		val path = Path()
		var first = true

		// Ecliptic longitude from 0 to 360
		for (lon in 0..360 step 10) {
			// Convert Ecliptic Longitude (with lat=0) to Equatorial (RA/Dec)
			// Since we don't have direct conversion in simple exposed API, we use a trick or assume we have access.
			// The Astronomy engine has `eclipticToEquatorial`. However, it takes a Vector.
			// Let's synthesize a vector for Ecliptic coordinates (dist=1, lat=0, lon=lon)
			// Or approximate:
			// This is simplified. Ideally use the Astronomy lib.
			// Let's assume we use a helper or simple math if lib access is tricky in draw loop.
			// BUT, since `astronomy.kt` is available, let's use it properly.
			// We need to create an Ecliptic vector.

			val eclipticSph = Spherical(0.0, lon.toDouble(), 1.0)
			val eclipticVec = eclipticSph.toVector(currentTime)

			// Convert Ecliptic Vector -> Equatorial Vector -> Equatorial Coords -> Horizon Coords
			// Wait, `eclipticToEquatorial` converts EQJ to ECT. We need ECT to EQJ?
			// Actually, `horizon` takes RA/Dec of Date.
			// The Ecliptic is defined by the Earth's orbit.
			// Let's use a simpler geometric approach for visualization if strict accuracy isn't 100% critical,
			// OR use the provided `rotationEclHor` if available? No.

			// Best approach with existing lib:
			// Create vector in Ecliptic system. Rotate to Equatorial.
			// We have `rotationEclEqd(time)`.
			// 1. Create vector for (lat=0, lon=i)
			val radLon = Math.toRadians(lon.toDouble())
			val vecEcl = io.github.cosinekitty.astronomy.Vector(cos(radLon), sin(radLon), 0.0, currentTime)

			// 2. Rotate to Equator of Date
			val rotMat = io.github.cosinekitty.astronomy.rotationEclEqd(currentTime)
			val vecEqd = rotMat.rotate(vecEcl)

			// 3. Get RA/Dec
			val equ = vecEqd.toEquatorial()

			// 4. Get Horizon
			val hor = horizon(currentTime, observer, equ.ra, equ.dec, Refraction.None)

			val p = skyToScreen(hor.azimuth, hor.altitude)
			if (p != null) {
				if (first) { path.moveTo(p.x, p.y); first = false }
				else path.lineTo(p.x, p.y)
			} else {
				first = true
			}
		}
		canvas.drawPath(path, eclipticPaint)
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

		val sinAlt = sin(altRad)
		val cosAlt = cos(altRad)
		val sinAlt0 = sin(alt0Rad)
		val cosAlt0 = cos(alt0Rad)
		val cosAz = cos(azRad)
		val sinAz = sin(azRad)

		// Cosine of angular distance from center
		val cosC = sinAlt0 * sinAlt + cosAlt0 * cosAlt * cosAz

		// Clipping for Stereographic projection
		// Allow seeing up to ~105 degrees from center (cosC > -0.3)
		// This prevents wrapping artifacts at infinity (antipode) while allowing wide FOV
		if (cosC <= -0.3) return null

		// Stereographic Projection Factor: k = 2 / (1 + cosC)
		val k = 2.0 / (1.0 + cosC)

		val x = k * cosAlt * sinAz
		val y = k * (cosAlt0 * sinAlt - sinAlt0 * cosAlt * cosAz)

		// Calculate Scale to fit viewAngle
		// R_edge = 2 * tan(FOV_rad / 4)
		// Scale * R_edge = Width / 2
		val viewAngleRad = Math.toRadians(viewAngle)
		val scale = width / (4.0 * tan(viewAngleRad / 4.0))

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