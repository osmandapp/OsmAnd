package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import net.osmand.plus.plugins.astro.Star
import java.time.ZonedDateTime
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.format

class StarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private var azimuthCenter = 180.0
	private var altitudeCenter = 45.0
	private var viewAngle = 60.0 // Field of view in degrees

	private var lastTouchX = 0f
	private var lastTouchY = 0f
	private var isPanning = false

	private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

	private val stars = mutableListOf<Star>()
	private val sun = Star("Sun", 0.0, 0.0, -26.74f, Color.YELLOW)

	// Observer location (e.g., my location)
	private val observer = Observer(56.9496, 24.1052, 0.0)

	private var pointerX = -1f
	private var pointerY = -1f

	init {
		// Add some stars for testing (Right Ascension in hours, Declination in degrees)
		// Brighter stars
		stars.add(Star("Sirius", 6.75, -16.7, -1.46f, Color.WHITE))
		stars.add(Star("Canopus", 6.40, -52.7, -0.74f, Color.WHITE))
		stars.add(Star("Arcturus", 14.26, 19.2, -0.04f, Color.YELLOW))
		stars.add(Star("Vega", 18.61, 38.8, 0.03f, Color.CYAN))
		stars.add(Star("Capella", 5.28, 46.0, 0.08f, Color.YELLOW))
		stars.add(Star("Rigel", 5.24, -8.2, 0.12f, Color.CYAN))
		stars.add(Star("Procyon", 7.65, 5.2, 0.34f, Color.WHITE))
		stars.add(Star("Betelgeuse", 5.92, 7.4, 0.50f, Color.RED))
		stars.add(Star("Altair", 19.84, 8.9, 0.77f, Color.WHITE))
		stars.add(Star("Aldebaran", 4.60, 16.5, 0.85f, Color.RED))

		// Dimmer stars
		stars.add(Star("Spica", 13.42, -11.1, 0.98f, Color.CYAN))
		stars.add(Star("Antares", 16.49, -26.4, 1.09f, Color.RED))
		stars.add(Star("Pollux", 7.75, 28.0, 1.14f, Color.YELLOW))
		stars.add(Star("Fomalhaut", 22.96, -29.6, 1.16f, Color.WHITE))
		stars.add(Star("Deneb", 20.69, 45.3, 1.25f, Color.CYAN))
		stars.add(Star("Regulus", 10.14, 12.0, 1.35f, Color.CYAN))
		stars.add(Star("Castor", 7.58, 31.9, 1.94f, Color.WHITE))
		stars.add(Star("Polaris", 2.53, 89.3, 1.97f, Color.YELLOW))
		stars.add(Star("Mizar", 13.40, 54.9, 2.23f, Color.WHITE))
		stars.add(Star("Alcor", 13.42, 54.9, 3.99f, Color.WHITE))

		updateCelestialPositions()
	}

	private fun updateCelestialPositions() {
		val now = Time.Companion.fromMillisecondsSince1970(ZonedDateTime.now().toInstant().toEpochMilli())

		// Update Sun position
		val sunEqu = equator(Body.Sun, now, observer, EquatorEpoch.OfDate, Aberration.Corrected)
		val sunHor = horizon(now, observer, sunEqu.ra, sunEqu.dec, Refraction.Normal)
		sun.azimuth = sunHor.azimuth
		sun.altitude = sunHor.altitude

		// Update star positions
		for (star in stars) {
			// Define star in astronomy engine
			defineStar(Body.Star1, star.ra, star.dec, 1000.0)
			val starEqu =
				equator(Body.Star1, now, observer, EquatorEpoch.OfDate, Aberration.Corrected)
			val starHor = horizon(now, observer, starEqu.ra, starEqu.dec, Refraction.Normal)
			star.azimuth = starHor.azimuth
			star.altitude = starHor.altitude
		}
	}


	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		canvas.drawColor(Color.BLACK)

		drawHorizon(canvas)
		drawAzimuthalGrid(canvas)
		drawCelestialObject(canvas, sun)
		stars.forEach { drawCelestialObject(canvas, it) }
		drawInfoPanel(canvas)
	}

	private fun drawHorizon(canvas: Canvas) {
		paint.color = Color.GREEN
		paint.strokeWidth = 3f
		paint.style = Paint.Style.STROKE

		val path = Path()
		var firstPoint = true
		for (az in 0..360) {
			val point = skyToScreen(az.toDouble(), 0.0)
			if (point != null) {
				if (firstPoint) {
					path.moveTo(point.x, point.y)
					firstPoint = false
				} else {
					path.lineTo(point.x, point.y)
				}
			}
		}
		canvas.drawPath(path, paint)
	}

	private fun drawAzimuthalGrid(canvas: Canvas) {
		paint.color = Color.DKGRAY
		paint.strokeWidth = 1f

		// Altitude lines
		for (alt in -10..80 step 10) {
			val path = Path()
			var firstPoint = true
			for (az in 0..360) {
				val point = skyToScreen(az.toDouble(), alt.toDouble())
				if (point != null) {
					if (firstPoint) {
						path.moveTo(point.x, point.y)
						firstPoint = false
					} else {
						path.lineTo(point.x, point.y)
					}
				}
			}
			canvas.drawPath(path, paint)
		}

		// Azimuth lines
		for (az in 0..350 step 10) {
			val path = Path()
			var firstPoint = true
			for (alt in -20..90) {
				val point = skyToScreen(az.toDouble(), alt.toDouble())
				if (point != null) {
					if (firstPoint) {
						path.moveTo(point.x, point.y)
						firstPoint = false
					} else {
						path.lineTo(point.x, point.y)
					}
				}
			}
			canvas.drawPath(path, paint)
		}
	}

	private fun drawCelestialObject(canvas: Canvas, star: Star) {
		val point = skyToScreen(star.azimuth, star.altitude)
		if (point != null) {
			paint.color = star.color
			paint.style = Paint.Style.FILL
			val radius = max(1f, 8f - star.magnitude)
			canvas.drawCircle(point.x, point.y, radius, paint)

			paint.color = Color.WHITE
			paint.textSize = 20f
			canvas.drawText(star.name, point.x + radius, point.y, paint)
		}
	}

	private fun drawInfoPanel(canvas: Canvas) {
		if (pointerX < 0 || pointerY < 0) return

		val coords = screenToSky(pointerX, pointerY)
		if (coords != null) {
			val infoText = "Az: %.1f°, Alt: %.1f°, FOV: %.1f°".format(coords.x, coords.y, viewAngle)
			paint.color = Color.argb(150, 80, 80, 80)
			paint.style = Paint.Style.FILL
			val textWidth = paint.measureText(infoText)
			canvas.drawRect(pointerX - 10, pointerY - 40, pointerX + textWidth + 10, pointerY + 10, paint)

			paint.color = Color.WHITE
			paint.textSize = 30f
			canvas.drawText(infoText, pointerX, pointerY, paint)
		}
	}


	private fun skyToScreen(azimuth: Double, altitude: Double): PointF? {
		// Simple Gnomonic projection
		val azRad = Math.toRadians(azimuth - azimuthCenter)
		val altRad = Math.toRadians(altitude)
		val alt0Rad = Math.toRadians(altitudeCenter)

		val cosC = sin(alt0Rad) * sin(altRad) + cos(alt0Rad) * cos(altRad) * cos(azRad)

		if (cosC < 0) return null // Behind the camera

		val k = 1.0 / cosC
		val x = k * cos(altRad) * sin(azRad)
		val y = k * (cos(alt0Rad) * sin(altRad) - sin(alt0Rad) * cos(altRad) * cos(azRad))

		val scale = width / Math.toRadians(viewAngle)

		return PointF(
			(width / 2 + scale * x).toFloat(),
			(height / 2 - scale * y).toFloat()
		)
	}

	private fun screenToSky(x: Float, y: Float): PointF? {
		val scale = width / Math.toRadians(viewAngle)
		val xRel = (x - width / 2) / scale
		val yRel = -(y - height / 2) / scale

		val alt0Rad = Math.toRadians(altitudeCenter)
		val p = sqrt(xRel * xRel + yRel * yRel)
		val c = atan(p)

		val altRad = asin(cos(c) * sin(alt0Rad) + yRel * sin(c) * cos(alt0Rad) / p)
		val azRadOffset =
			atan2(xRel * sin(c), p * cos(alt0Rad) * cos(c) - yRel * sin(alt0Rad) * sin(c))

		val altitude = Math.toDegrees(altRad)
		var azimuth = Math.toDegrees(azRadOffset) + azimuthCenter

		if (azimuth < 0) azimuth += 360
		if (azimuth >= 360) azimuth -= 360

		return PointF(azimuth.toFloat(), altitude.toFloat())
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		scaleGestureDetector.onTouchEvent(event)
		if (scaleGestureDetector.isInProgress) {
			return true
		}

		pointerX = event.x
		pointerY = event.y

		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				lastTouchX = event.x
				lastTouchY = event.y
				isPanning = true
				performClick()
			}
			MotionEvent.ACTION_MOVE -> {
				if (isPanning) {
					val dx = event.x - lastTouchX
					val dy = event.y - lastTouchY

					val scale = viewAngle / width
					azimuthCenter -= dx * scale
					altitudeCenter += dy * scale

					// Clamp altitude
					altitudeCenter = max(-20.0, min(90.0, altitudeCenter))

					// Normalize azimuth
					if (azimuthCenter < 0) azimuthCenter += 360
					if (azimuthCenter >= 360) azimuthCenter -= 360

					lastTouchX = event.x
					lastTouchY = event.y
					invalidate()
				}
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				isPanning = false
				pointerX = -1f
				pointerY = -1f
				invalidate()
			}
		}
		return true
	}

	override fun performClick(): Boolean {
		super.performClick()
		// Check for star click
		val clickThreshold = 30f
		stars.forEach { star ->
			val point = skyToScreen(star.azimuth, star.altitude)
			if (point != null) {
				val dx = point.x - lastTouchX
				val dy = point.y - lastTouchY
				if (sqrt(dx * dx + dy * dy) < clickThreshold) {
					// You can add logic here to display more info about the star
					println("Clicked on ${star.name}")
				}
			}
		}
		val sunPoint = skyToScreen(sun.azimuth, sun.altitude)
		if(sunPoint != null) {
			val dx = sunPoint.x - lastTouchX
			val dy = sunPoint.y - lastTouchY
			if (sqrt(dx * dx + dy * dy) < clickThreshold) {
				println("Clicked on ${sun.name}")
			}
		}
		return true
	}

	private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
		override fun onScale(detector: ScaleGestureDetector): Boolean {
			viewAngle /= detector.scaleFactor
			viewAngle = max(1.0, min(120.0, viewAngle)) // Clamp view angle
			invalidate()
			return true
		}
	}
}