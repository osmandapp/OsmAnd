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
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
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
import net.osmand.plus.plugins.astro.Constellation
import net.osmand.plus.plugins.astro.SkyObject
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
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

	// --- Graphics ---
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE
		textSize = 30f
	}
	private val cardinalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.GREEN
		textSize = 40f
		textAlign = Paint.Align.CENTER
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
	private val selectedConstellationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFFFFD700.toInt() // Gold
		style = Paint.Style.STROKE
		strokeWidth = 4f
		alpha = 255
	}
	private val constellationTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFFAABBFF.toInt()
		textSize = 32f
		textAlign = Paint.Align.CENTER
		typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
		setShadowLayer(8f, 0f, 0f, Color.BLACK)
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
	private val pinnedHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = 0xFFFFD700.toInt() // Gold for pinned items
		style = Paint.Style.STROKE
		strokeWidth = 4f
	}

	private val celestialPath = Path()
	private val gridPath = Path()
	private val tempPoint = PointF()
	private val tempPoint2 = PointF()
	private val reusableCal = Calendar.getInstance()
	private val arrowPath = Path()
	private val occupiedRects = mutableListOf<RectF>()

	// --- View State ---
	private var azimuthCenter = 180.0
	private var altitudeCenter = 45.0
	private var viewAngle = 60.0

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

	var showStars = true
	var showGalaxies = true
	var showBlackHoles = true
	var showNebulae = true
	var showOpenCluster = true
	var showGlobularCluster = true
	var showGalaxyCluster = true
	var showSun = true
	var showMoon = true
	var showPlanets = true

	// --- Interaction ---
	private var lastTouchX = 0f
	private var lastTouchY = 0f
	private var isPanning = false
	private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
	private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
		override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
			performClickAt(e.x, e.y); return true;
		}
	})

	private var onObjectClickListener: ((SkyObject?) -> Unit)? = null

	var onAnimationFinished: (() -> Unit)? = null
	var onAzimuthManualChangeListener: ((Double) -> Unit)? = null
	var onViewAngleChangeListener: ((Double) -> Unit)? = null
	var magnitudeFilter: Float? = null

	var roll = 0.0
		set(value) {
			if (abs(field - value) > 0.1) {
				field = value
				invalidate()
			}
		}

	// --- Astronomy Data ---
	private val skyObjects = mutableListOf<SkyObject>()
	private var constellations = listOf<Constellation>()
	private val skyObjectMap = mutableMapOf<Int, SkyObject>()

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

	// Dynamic Grid Configuration
	private var gridDensityLevel = -1 // 0=Wide, 1=Medium, 2=Dense

	// Equatorial vars (re-calculated based on density)
	private var equRaStepMin = 120 // 2 hours in minutes
	private var equDecStep = 20
	private var equLineResStep = 5

	private var equRaLinesCount = 0
	private var equRaPointsCount = 0
	private var equRaAzimuths = Array(0) { DoubleArray(0) }
	private var equRaAltitudes = Array(0) { DoubleArray(0) }

	private var equDecLinesCount = 0
	private var equDecPointsCount = 0
	private var equDecAzimuths = Array(0) { DoubleArray(0) }
	private var equDecAltitudes = Array(0) { DoubleArray(0) }

	// --- Selection & Multiselection ---
	private var selectedObject: SkyObject? = null
	private var selectedConstellation: Constellation? = null
	private val selectedConstellationStarIds = mutableSetOf<Int>()
	private val pinnedObjects = mutableSetOf<SkyObject>()

	var onConstellationClickListener: ((Constellation?) -> Unit)? = null

	// --- Path Caching for Multiselection ---
	private data class CelestialPathData(
		val azimuths: DoubleArray,
		val altitudes: DoubleArray,
		val labels: Array<String?>,
		val count: Int,
		var lastTime: Double,
		var lastLat: Double,
		var lastLon: Double
	)

	private val pathCache = mutableMapOf<SkyObject, CelestialPathData>()

	// Constellation Centroids Cache
	private data class ConstellationCentroid(
		val ra: Double, val dec: Double,
		var azimuth: Double = 0.0, var altitude: Double = 0.0,
		var startAzimuth: Double = 0.0, var startAltitude: Double = 0.0,
		var targetAzimuth: Double = 0.0, var targetAltitude: Double = 0.0
	)
	private val constellationCenters = mutableMapOf<Constellation, ConstellationCentroid?>()

	private var visualAnimator: ValueAnimator? = null

	fun setObserverLocation(lat: Double, lon: Double, alt: Double) {
		observer = Observer(lat, lon, alt)
		recalculatePositions(currentTime, updateTargets = false)
		invalidate()
	}

	fun setCenter(azimuth: Double, altitude: Double, animate: Boolean = false) {
		if (animate) {
			animateTo(azimuth, altitude)
		} else {
			this.azimuthCenter = azimuth
			this.altitudeCenter = max(-90.0, min(90.0, altitude))
			invalidate()
		}
	}

	private fun animateTo(azimuth: Double, altitude: Double, targetViewAngle: Double? = null) {
		val startAz = azimuthCenter
		val startAlt = altitudeCenter
		val startAngle = viewAngle
		val targetAlt = max(-90.0, min(90.0, altitude))

		visualAnimator?.cancel()
		visualAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = 500
			interpolator = DecelerateInterpolator()
			addUpdateListener { animator ->
				val fraction = animator.animatedValue as Float
				azimuthCenter = interpolateAngle(startAz, azimuth, fraction)
				altitudeCenter = startAlt + (targetAlt - startAlt) * fraction
				if (targetViewAngle != null) {
					updateViewAngle(startAngle + (targetViewAngle - startAngle) * fraction)
				}
				invalidate()
			}
			start()
		}
	}

	fun getAltitude() = altitudeCenter

	fun getAzimuth() = azimuthCenter

	fun getViewAngle() = viewAngle

	private fun updateViewAngle(newAngle: Double, focusX: Float = width / 2f, focusY: Float = height / 2f) {
		val maxAngle = if (is2DMode) 220.0 else 150.0
		val finalAngle = max(10.0, min(maxAngle, newAngle))
		if (abs(this.viewAngle - finalAngle) > 0.001) {
			if (is2DMode && width > 0 && height > 0) {
				val oldTan = tan(Math.toRadians(viewAngle) / 4.0)
				val newTan = tan(Math.toRadians(finalAngle) / 4.0)
				if (oldTan > 0 && newTan > 0) {
					val ratio = oldTan / newTan
					val halfWidth = width / 2f
					val halfHeight = height / 2f
					panX = (focusX - halfWidth - (focusX - halfWidth - panX) * ratio).toFloat()
					panY = (focusY - halfHeight - (focusY - halfHeight - panY) * ratio).toFloat()
				}
			}
			this.viewAngle = finalAngle
			onViewAngleChangeListener?.invoke(finalAngle)
			invalidate()
		}
	}

	fun setViewAngle(angle: Double) {
		updateViewAngle(angle)
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
		//visualAnimator?.cancel()
		skyObjects.clear()
		skyObjects.addAll(objects)
		// Sort by magnitude (ascending): brighter objects (lower mag) come first
		skyObjects.sortBy { it.magnitude }
		skyObjectMap.clear()
		objects.forEach { skyObjectMap[it.hip] = it }

		recalculatePositions(currentTime, updateTargets = false)
		skyObjects.forEach {
			it.azimuth = it.targetAzimuth
			it.altitude = it.targetAltitude
		}

		// Clean up pinned objects that might no longer exist
		val toRemove = pinnedObjects.filter { !skyObjectMap.containsValue(it) }
		pinnedObjects.removeAll(toRemove)
		pathCache.keys.removeAll(toRemove)

		invalidate()
	}

	fun setConstellations(list: List<Constellation>) {
		//visualAnimator?.cancel()
		constellations = list
		updateConstellationCenters()

		invalidate()
	}

	private fun updateConstellationCenters() {
		constellationCenters.clear()
		constellations.forEach { constellation ->
			var sumX = 0.0
			var sumY = 0.0
			var sumZ = 0.0
			var count = 0

			val uniqueStars = mutableSetOf<Int>()
			constellation.lines.forEach { (id1, id2) -> uniqueStars.add(id1); uniqueStars.add(id2) }

			uniqueStars.forEach { id ->
				val star = skyObjectMap[id]
				if (star != null) {
					// Convert RA/Dec to Cartesian
					val raRad = Math.toRadians(star.ra * 15.0) // RA is in hours, multiply by 15 to get degrees
					val decRad = Math.toRadians(star.dec)
					val x = cos(decRad) * cos(raRad)
					val y = cos(decRad) * sin(raRad)
					val z = sin(decRad)

					sumX += x
					sumY += y
					sumZ += z
					count++
				}
			}

			if (count > 0) {
				val avgX = sumX / count
				val avgY = sumY / count
				val avgZ = sumZ / count

				// Convert back to RA/Dec
				val hyp = sqrt(avgX * avgX + avgY * avgY)
				val decRad = atan2(avgZ, hyp)
				var raRad = atan2(avgY, avgX)
				if (raRad < 0) raRad += 2 * PI

				val ra = Math.toDegrees(raRad) / 15.0
				val dec = Math.toDegrees(decRad)
				val center = ConstellationCentroid(ra, dec)
				val hor = horizon(currentTime, observer, ra, dec, Refraction.Normal)
				center.azimuth = hor.azimuth
				center.altitude = hor.altitude
				center.targetAzimuth = hor.azimuth
				center.targetAltitude = hor.altitude
				center.startAzimuth = hor.azimuth
				center.startAltitude = hor.altitude
				constellationCenters[constellation] = center
			} else {
				constellationCenters[constellation] = null
			}
		}
	}

	fun updateVisibility() {
		recalculatePositions(currentTime, updateTargets = false)
		invalidate()
	}

	fun setOnObjectClickListener(listener: (SkyObject?) -> Unit) {
		this.onObjectClickListener = listener
	}

	fun getSelectedConstellationItem(): Constellation? = selectedConstellation

	fun setSelectedObject(obj: SkyObject?, center: Boolean = false, animate: Boolean = false) {
		selectedConstellation = null
		if (obj != null) {
			if (obj.azimuth == 0.0 && obj.altitude == 0.0) calculatePosition(obj)
			selectedObject = obj
			if (center) setCenter(obj.azimuth, obj.altitude, animate)
		} else {
			selectedObject = obj
			invalidate()
		}
	}

	fun setSelectedConstellation(
		c: Constellation?,
		center: Boolean = false,
		animate: Boolean = false
	) {
		selectedObject = null
		selectedConstellation = c
		selectedConstellationStarIds.clear()
		c?.lines?.forEach {
			selectedConstellationStarIds.add(it.first)
			selectedConstellationStarIds.add(it.second)
		}

		skyObjects.filter { selectedConstellationStarIds.contains(it.hip) }.forEach {
			calculatePosition(it, currentTime, false)
			it.azimuth = it.targetAzimuth
			it.altitude = it.targetAltitude
		}

		if (c != null && center) {
			val center = constellationCenters[c]
			if (center != null) {
				var maxDist = 0.0
				val uniqueStars = mutableSetOf<Int>()
				c.lines.forEach { (id1, id2) -> uniqueStars.add(id1); uniqueStars.add(id2) }
				uniqueStars.forEach { id ->
					val star = skyObjectMap[id]
					if (star != null) {
						val d = angularDistance(center.ra, center.dec, star.ra, star.dec)
						if (d > maxDist) maxDist = d
					}
				}
				// targetAngle logic: Frame the constellation with padding
				val targetAngle = if (maxDist > 0) max(20.0, min(120.0, maxDist * 3.5)) else viewAngle
				if (animate) {
					animateTo(center.azimuth, center.altitude, targetAngle)
				} else {
					setCenter(center.azimuth, center.altitude)
					setViewAngle(targetAngle)
				}
			}
		}
		invalidate()
	}

	private fun angularDistance(ra1: Double, dec1: Double, ra2: Double, dec2: Double): Double {
		val phi1 = Math.toRadians(dec1)
		val phi2 = Math.toRadians(dec2)
		val lambda1 = Math.toRadians(ra1 * 15.0)
		val lambda2 = Math.toRadians(ra2 * 15.0)
		val cosD = sin(phi1) * sin(phi2) + cos(phi1) * cos(phi2) * cos(lambda1 - lambda2)
		return Math.toDegrees(acos(max(-1.0, min(1.0, cosD))))
	}

	// --- Pinning API ---

	fun isObjectPinned(obj: SkyObject): Boolean {
		return pinnedObjects.contains(obj)
	}

	fun setObjectPinned(obj: SkyObject, pinned: Boolean) {
		if (pinned) {
			pinnedObjects.add(obj)
		} else {
			pinnedObjects.remove(obj)
			pathCache.remove(obj) // Free up cache if deselected
		}
		invalidate()
	}

	// -------------------

	fun setDateTime(time: Time, animate: Boolean = true) {
		visualAnimator?.cancel()
		if (animate) {
			skyObjects.forEach {
				it.startAzimuth = it.azimuth
				it.startAltitude = it.altitude
			}
			constellationCenters.values.filterNotNull().forEach {
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
					constellationCenters.values.filterNotNull().forEach { center ->
						center.azimuth = interpolateAngle(center.startAzimuth, center.targetAzimuth, fraction)
						center.altitude = center.startAltitude + (center.targetAltitude - center.startAltitude) * fraction
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
			constellationCenters.values.filterNotNull().forEach { center ->
				center.azimuth = center.targetAzimuth
				center.altitude = center.targetAltitude
			}
			invalidate()
			onAnimationFinished?.invoke()
		}
	}

	fun getMinZoom() = if (is2DMode) 200.0 else 150.0

	fun getMaxZoom() = 150.0

	fun zoomIn() {
		updateViewAngle(viewAngle / 1.5)
	}

	fun zoomOut() {
		updateViewAngle(viewAngle * 1.5)
	}

	private fun recalculatePositions(time: Time, updateTargets: Boolean) {
		skyObjects.forEach { obj ->
			if (!shouldRecalculate(obj)) return@forEach

			calculatePosition(obj, time, updateTargets)
		}

		// Update Constellation Centroids positions
		constellationCenters.values.filterNotNull().forEach { center ->
			val hor = horizon(time, observer, center.ra, center.dec, Refraction.Normal)
			if (updateTargets) {
				center.targetAzimuth = hor.azimuth
				center.targetAltitude = hor.altitude
			} else {
				center.azimuth = hor.azimuth
				center.altitude = hor.altitude
				center.targetAzimuth = hor.azimuth
				center.targetAltitude = hor.altitude
			}
		}
	}

	fun calculatePosition(obj: SkyObject) = calculatePosition(obj, currentTime, false)

	private fun calculatePosition(
		obj: SkyObject,
		time: Time,
		updateTargets: Boolean
	) {
		val hor: Topocentric
		val body = obj.body
		if (obj.type.isSunSystem() && body != null) {
			val equ = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
			hor = horizon(time, observer, equ.ra, equ.dec, Refraction.Normal)
			obj.distAu = equ.dist
		} else {
			hor = horizon(time, observer, obj.ra, obj.dec, Refraction.Normal)
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

	private fun shouldRecalculate(obj: SkyObject): Boolean {
		if (obj == selectedObject) return true
		if (pinnedObjects.contains(obj)) return true
		if (showConstellations) return true
		if (selectedConstellationStarIds.contains(obj.hip)) return true
		return isObjectVisibleInSettings(obj)
	}

	private fun isObjectVisibleInSettings(obj: SkyObject): Boolean {
		val magnitudeFilter = magnitudeFilter
		if (magnitudeFilter != null && obj.type == SkyObject.Type.STAR && showStars
			&& obj.magnitude > magnitudeFilter) return false

		return when (obj.type) {
			SkyObject.Type.STAR -> showStars
			SkyObject.Type.GALAXY -> showGalaxies
			SkyObject.Type.BLACK_HOLE -> showBlackHoles
			SkyObject.Type.SUN -> showSun
			SkyObject.Type.MOON -> showMoon
			SkyObject.Type.PLANET -> showPlanets
			SkyObject.Type.NEBULA -> showNebulae
			SkyObject.Type.OPEN_CLUSTER -> showOpenCluster
			SkyObject.Type.GLOBULAR_CLUSTER -> showGlobularCluster
			SkyObject.Type.GALAXY_CLUSTER -> showGalaxyCluster
			SkyObject.Type.CONSTELLATION -> showConstellations
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

	// Calculate and cache path data for an object
	private fun getOrUpdatePathData(obj: SkyObject): CelestialPathData? {
		val cached = pathCache[obj]
		val timeUnchanged = cached != null && abs(currentTime.tt - cached.lastTime) < 0.0000001
		val locUnchanged = cached != null && observer.latitude == cached.lastLat && observer.longitude == cached.lastLon

		if (timeUnchanged && locUnchanged && cached != null) {
			return cached
		}

		// Calculate new path data
		val startHours = -12
		val endHours = 13
		val stepMinutes = 10
		val totalMinutes = (endHours - startHours) * 60
		val steps = totalMinutes / stepMinutes + 1

		// Arrays to store path data
		val azimuths = DoubleArray(steps)
		val altitudes = DoubleArray(steps)
		val labels = arrayOfNulls<String>(steps)

		val tz = TimeZone.getDefault()
		val currentMillis = currentTime.toMillisecondsSince1970()
		var validCount = 0

		for (i in 0 until steps) {
			reusableCal.timeZone = tz
			reusableCal.timeInMillis = currentMillis
			reusableCal.add(Calendar.HOUR_OF_DAY, startHours)
			reusableCal.set(Calendar.MINUTE, 0)
			reusableCal.set(Calendar.SECOND, 0)
			reusableCal.set(Calendar.MILLISECOND, 0)

			val stepTimeMillis = reusableCal.timeInMillis + (i * stepMinutes * 60000L)
			val maxTime = currentMillis + (endHours * 3600000L)

			if (stepTimeMillis > maxTime + 3600000L) break

			val tStep = Time.fromMillisecondsSince1970(stepTimeMillis)
			reusableCal.timeInMillis = stepTimeMillis
			val stepMinute = reusableCal.get(Calendar.MINUTE)
			val stepHour = reusableCal.get(Calendar.HOUR_OF_DAY)
			val isHourMark = (stepMinute == 0)

			if (isHourMark) {
				labels[validCount] = "%02d".format(stepHour)
			}

			val body = obj.body
			val altAz: Topocentric = if (obj.type.isSunSystem() && body != null) {
				val eq = equator(body, tStep, observer, EquatorEpoch.OfDate, Aberration.Corrected)
				horizon(tStep, observer, eq.ra, eq.dec, Refraction.Normal)
			} else {
				horizon(tStep, observer, obj.ra, obj.dec, Refraction.Normal)
			}

			azimuths[validCount] = altAz.azimuth
			altitudes[validCount] = altAz.altitude
			validCount++
		}

		val newData = CelestialPathData(
			azimuths, altitudes, labels, validCount,
			currentTime.tt, observer.latitude, observer.longitude
		)
		pathCache[obj] = newData
		return newData
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		updateProjectionCache()
		canvas.drawColor(Color.BLACK)
		occupiedRects.clear()

		if (showEquatorialGrid) drawEquatorialGrid(canvas)
		if (showAzimuthalGrid) drawAzimuthalGrid(canvas)
		if (showEclipticLine) drawEclipticLine(canvas)
		drawConstellationLines(canvas)

		drawHorizon(canvas)

		// Draw Celestial Paths for all Selected Objects (Current + Pinned)
		val objectsToDrawPath = mutableSetOf<SkyObject>()
		if (selectedObject != null) objectsToDrawPath.add(selectedObject!!)
		objectsToDrawPath.addAll(pinnedObjects)

		objectsToDrawPath.forEach { obj ->
			if (isObjectVisibleInSettings(obj)) {
				drawCelestialPath(canvas, obj)
			}
		}

		drawConstellationLabels(canvas)

		skyObjects.forEach { obj ->
			if (isObjectVisibleInSettings(obj) || selectedObject == obj) {
				drawSkyObject(canvas, obj)
			}
		}

		// Draw Highlights

		// 1. Draw highlights for pinned objects (Gold)
		pinnedObjects.forEach { obj ->
			if (isObjectVisibleInSettings(obj) || selectedObject == obj) {
				if (skyToScreen(obj.azimuth, obj.altitude, tempPoint)) {
					canvas.drawCircle(tempPoint.x, tempPoint.y, 25f, pinnedHighlightPaint)
				}
			}
		}

		// 2. Draw highlight for currently selected object (Red)
		selectedObject?.let {
			if (skyToScreen(it.azimuth, it.altitude, tempPoint)) {
				paint.style = Paint.Style.STROKE
				paint.color = Color.RED
				paint.strokeWidth = 3f
				canvas.drawCircle(tempPoint.x, tempPoint.y, 25f, paint)
			}
		}
	}

	private fun drawCelestialPath(canvas: Canvas, obj: SkyObject) {
		val pathData = getOrUpdatePathData(obj) ?: return

		if (pathData.count > 1) {
			val isMoon = obj.type == SkyObject.Type.MOON
			val drawCount = if (isMoon) pathData.count else min(pathData.count, 145)

			celestialPath.reset()
			var isPenDown = false
			val tempPt = PointF()
			val prevPt = PointF()

			for (i in 0 until drawCount) {
				val az = pathData.azimuths[i]
				val alt = pathData.altitudes[i]

				val isVisible = skyToScreen(az, alt, tempPt)

				if (!isVisible) {
					isPenDown = false
					continue
				}

				if (isPenDown && i > 0) {
					// Check distance to prevent drawing lines across the screen when wrapping or distant
					val dist = hypot(tempPt.x - prevPt.x, tempPt.y - prevPt.y)
					if (dist > width * 0.8) {
						isPenDown = false
					}
				}

				if (!isPenDown) {
					celestialPath.moveTo(tempPt.x, tempPt.y)
					isPenDown = true
				} else {
					celestialPath.lineTo(tempPt.x, tempPt.y)
				}
				prevPt.set(tempPt)
			}

			canvas.drawPath(celestialPath, pathPaint)

			// Draw Labels and Arrows
			val tempNext = PointF()
			val tempPrev = PointF()
			val drawnLabels = mutableSetOf<String>()

			for (i in 0 until drawCount) {
				val label = pathData.labels[i] ?: continue
				if (!drawnLabels.add(label)) continue

				val az = pathData.azimuths[i]
				val alt = pathData.altitudes[i]
				if (!skyToScreen(az, alt, tempPt)) continue

				// Need neighbors for angle
				val iPrev = if (i > 0) i - 1 else i
				val iNext = if (i < drawCount - 1) i + 1 else i
				if (iPrev == iNext) continue

				val azPrev = pathData.azimuths[iPrev]
				val altPrev = pathData.altitudes[iPrev]
				val azNext = pathData.azimuths[iNext]
				val altNext = pathData.altitudes[iNext]

				if (!skyToScreen(azPrev, altPrev, tempPrev) || !skyToScreen(azNext, altNext, tempNext)) continue

				val distP = hypot(tempPt.x - tempPrev.x, tempPt.y - tempPrev.y)
				val distN = hypot(tempNext.x - tempPt.x, tempNext.y - tempPt.y)
				if (i > 0 && distP > 200) continue
				if (i < drawCount - 1 && distN > 200) continue

				val dx = tempNext.x - tempPrev.x
				val dy = tempNext.y - tempPrev.y
				val angle = atan2(dy.toDouble(), dx.toDouble())

				val textDist = 30f
				val px = tempPt.x + textDist * cos(angle - PI/2).toFloat()
				val py = tempPt.y + textDist * sin(angle - PI/2).toFloat() + 8f

				canvas.drawText(label, px, py, labelPaint)
				drawArrow(canvas, tempPt.x, tempPt.y, angle)
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

	private fun drawOutsideLabel(canvas: Canvas, label: String, az: Double, alt: Double, textPaint: Paint, offset: Float = 25f) {
		if (!skyToScreen(az, alt, tempPoint)) return

		if (is2DMode) {
			val centerX = (projHalfWidth + panX).toFloat()
			val centerY = (projHalfHeight + panY).toFloat()
			val dx = tempPoint.x - centerX
			val dy = tempPoint.y - centerY
			val dist = hypot(dx, dy)
			if (dist > 0.1) {
				val px = centerX + dx * (dist + offset) / dist
				val py = centerY + dy * (dist + offset) / dist
				val fm = textPaint.fontMetrics
				canvas.drawText(label, px, py - (fm.ascent + fm.descent) / 2, textPaint)
			} else {
				canvas.drawText(label, tempPoint.x, tempPoint.y - offset, textPaint)
			}
		} else {
			canvas.drawText(label, tempPoint.x, tempPoint.y - offset, textPaint)
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
			} else { started = false }
		}
		paint.color = Color.GREEN
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE
		canvas.drawPath(gridPath, paint)
		val cardinals = listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)
		cardinals.forEach { (label, az) ->
			drawOutsideLabel(canvas, label, az, 0.0, cardinalTextPaint, 30f)
		}
	}

	private fun drawAzimuthalGrid(canvas: Canvas) {
		paint.color = 0xFF444444.toInt()
		paint.strokeWidth = 2f
		paint.style = Paint.Style.STROKE
		gridPath.reset()

		// Calculate dynamic density
		val (azStep, altStep, lineRes) = when {
			viewAngle < 20.0 -> Triple(10, 5, 1) // Dense
			viewAngle < 50.0 -> Triple(15, 10, 2) // Medium
			else -> Triple(45, 20, 5) // Wide
		}

		// Horizontal Lines (Altitudes)
		for (alt in -80..80 step altStep) {
			var first = true
			for (az in 0..360 step lineRes) {
				if (skyToScreen(az.toDouble(), alt.toDouble(), tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)
				} else { first = true }
			}
			if (alt != 0) {
				gridTextPaint.textAlign = Paint.Align.LEFT
				gridTextPaint.color = 0xFF888888.toInt()
				if (skyToScreen(azimuthCenter, alt.toDouble(), tempPoint)) {
					canvas.drawText("${alt}°", tempPoint.x + 5f, tempPoint.y - 5f, gridTextPaint)
				}
				if (skyToScreen(azimuthCenter + 180.0, alt.toDouble(), tempPoint)) {
					canvas.drawText("${alt}°", tempPoint.x + 5f, tempPoint.y - 5f, gridTextPaint)
				}
			}
		}

		// Vertical Lines (Azimuths)
		for (az in 0 until 360 step azStep) {
			var first = true
			for (alt in -90..90 step lineRes) {
				if (skyToScreen(az.toDouble(), alt.toDouble(), tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)
				} else { first = true }
			}
			if (az % 90 != 0) {
				gridTextPaint.textAlign = Paint.Align.CENTER
				gridTextPaint.color = 0xFF888888.toInt()
				drawOutsideLabel(canvas, "${az}°", az.toDouble(), 0.0, gridTextPaint, 25f)
			}
		}
		canvas.drawPath(gridPath, paint)
	}

	private fun updateEquatorialGridCache() {
		// Calculate desired density
		val newLevel = when {
			viewAngle < 20.0 -> 2 // Dense
			viewAngle < 50.0 -> 1 // Medium
			else -> 0 // Wide
		}

		// Re-initialize arrays if density level changed
		if (newLevel != gridDensityLevel) {
			gridDensityLevel = newLevel
			lastEquGridTimeT = -1.0 // Force refresh

			when (newLevel) {
				2 -> { // Dense: RA 20m, Dec 5 deg
					equRaStepMin = 20
					equDecStep = 5
					equLineResStep = 1
				}
				1 -> { // Medium: RA 1h, Dec 10 deg
					equRaStepMin = 60
					equDecStep = 10
					equLineResStep = 2
				}
				else -> { // Wide: RA 2h, Dec 20 deg
					equRaStepMin = 120
					equDecStep = 20
					equLineResStep = 5
				}
			}

			// Reallocate RA Arrays
			equRaLinesCount = (24 * 60) / equRaStepMin
			equRaPointsCount = (180 / equLineResStep) + 1
			equRaAzimuths = Array(equRaLinesCount) { DoubleArray(equRaPointsCount) }
			equRaAltitudes = Array(equRaLinesCount) { DoubleArray(equRaPointsCount) }

			// Reallocate Dec Arrays
			equDecLinesCount = (160 / equDecStep) + 1
			equDecPointsCount = (360 / equLineResStep) + 1
			equDecAzimuths = Array(equDecLinesCount) { DoubleArray(equDecPointsCount) }
			equDecAltitudes = Array(equDecLinesCount) { DoubleArray(equDecPointsCount) }
		}

		val timeUnchanged = abs(currentTime.tt - lastEquGridTimeT) < 0.0000001
		val locUnchanged = observer.latitude == lastEquGridLat && observer.longitude == lastEquGridLon
		if (timeUnchanged && locUnchanged && lastEquGridTimeT != -1.0) return

		// Populate RA lines
		for (i in 0 until equRaLinesCount) {
			val raVal = (i * equRaStepMin) / 60.0
			for (j in 0 until equRaPointsCount) {
				val decVal = -90.0 + (j * equLineResStep)
				val hor = horizon(currentTime, observer, raVal, decVal, Refraction.Normal)
				equRaAzimuths[i][j] = hor.azimuth
				equRaAltitudes[i][j] = hor.altitude
			}
		}

		// Populate Dec lines
		for (i in 0 until equDecLinesCount) {
			val decVal = -80.0 + (i * equDecStep)
			for (j in 0 until equDecPointsCount) {
				val raDeg = j * equLineResStep
				val raVal = raDeg / 15.0
				val hor = horizon(currentTime, observer, raVal, decVal, Refraction.Normal)
				equDecAzimuths[i][j] = hor.azimuth
				equDecAltitudes[i][j] = hor.altitude
			}
		}

		lastEquGridTimeT = currentTime.tt
		lastEquGridLat = observer.latitude
		lastEquGridLon = observer.longitude
	}

	private fun drawEquatorialGrid(canvas: Canvas) {
		updateEquatorialGridCache()
		gridPath.reset()

		// Set color for Equatorial labels
		gridTextPaint.color = 0xFF00AAAA.toInt()

		var bestRaIndex = -1
		var minCenterDistSq = Float.MAX_VALUE
		val centerX = width / 2f
		val centerY = height / 2f

		// Draw RA Lines and Labels
		for (i in 0 until equRaLinesCount) {
			var first = true
			var currentLineMinDistSq = Float.MAX_VALUE

			for (j in 0 until equRaPointsCount) {
				val az = equRaAzimuths[i][j]
				val alt = equRaAltitudes[i][j]
				if (skyToScreen(az, alt, tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)

					// Track distance to center for this line to find the "central" hour line
					val dx = tempPoint.x - centerX
					val dy = tempPoint.y - centerY
					val distSq = dx * dx + dy * dy
					if (distSq < currentLineMinDistSq) {
						currentLineMinDistSq = distSq
					}
				} else { first = true }
			}

			// Update best RA line
			if (currentLineMinDistSq < minCenterDistSq) {
				minCenterDistSq = currentLineMinDistSq
				bestRaIndex = i
			}

			// RA Label at Dec = 0
			val zeroDecIndex = 90 / equLineResStep
			if (zeroDecIndex >= 0 && zeroDecIndex < equRaPointsCount) {
				val az = equRaAzimuths[i][zeroDecIndex]
				val alt = equRaAltitudes[i][zeroDecIndex]
				if (skyToScreen(az, alt, tempPoint)) {
					val totalMinutes = i * equRaStepMin
					val h = totalMinutes / 60
					val m = totalMinutes % 60
					val label = if (m == 0) "${h}h" else "${h}h${m}"
					gridTextPaint.textAlign = Paint.Align.CENTER
					canvas.drawText(label, tempPoint.x, tempPoint.y - 10f, gridTextPaint)
				}
			}
		}

		// Draw Dec Lines and Labels
		for (i in 0 until equDecLinesCount) {
			var first = true
			for (j in 0 until equDecPointsCount) {
				val az = equDecAzimuths[i][j]
				val alt = equDecAltitudes[i][j]
				if (skyToScreen(az, alt, tempPoint)) {
					if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
					else gridPath.lineTo(tempPoint.x, tempPoint.y)
				} else { first = true }
			}

			// Draw label aligned to the central RA line
			if (bestRaIndex != -1) {
				val decDeg = -80 + (i * equDecStep)
				if (decDeg != 0) {
					// Calculate intersection of best RA line and this Dec line
					val raVal = (bestRaIndex * equRaStepMin) / 60.0
					val hor = horizon(currentTime, observer, raVal, decDeg.toDouble(), Refraction.Normal)

					if (skyToScreen(hor.azimuth, hor.altitude, tempPoint)) {
						// Ensure label is strictly on screen
						if (tempPoint.x >= 0 && tempPoint.x <= width && tempPoint.y >= 0 && tempPoint.y <= height) {
							gridTextPaint.textAlign = Paint.Align.LEFT
							canvas.drawText("${decDeg}°", tempPoint.x + 5f, tempPoint.y - 5f, gridTextPaint)
						}
					}
				}
			}
		}
		canvas.drawPath(gridPath, equGridPaint)
	}

	private fun updateEclipticCache() {
		val timeUnchanged = abs(currentTime.tt - lastEclipticTimeT) < 0.0000001
		val locUnchanged = observer.latitude == lastEclipticLat && observer.longitude == lastEclipticLon
		if (timeUnchanged && locUnchanged) return
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
				if (first) { gridPath.moveTo(tempPoint.x, tempPoint.y); first = false }
				else gridPath.lineTo(tempPoint.x, tempPoint.y)
			} else { first = true }
		}
		canvas.drawPath(gridPath, eclipticPaint)
	}

	private fun drawConstellationLines(canvas: Canvas) {
		constellations.forEach { constellation ->
			val isSelected = (constellation == selectedConstellation)
			if (!showConstellations && !isSelected) return@forEach

			gridPath.reset()
			val linePaint = if (isSelected) selectedConstellationPaint else constellationPaint

			constellation.lines.forEach { (id1, id2) ->
				val star1 = skyObjectMap[id1]
				val star2 = skyObjectMap[id2]
				if (star1 != null && star2 != null) {
					val p1Visible = skyToScreen(star1.azimuth, star1.altitude, tempPoint)
					val p2Visible = skyToScreen(star2.azimuth, star2.altitude, tempPoint2)
					if (p1Visible && p2Visible) {
						val dist = hypot(tempPoint.x - tempPoint2.x, tempPoint.y - tempPoint2.y)
						if (dist < width * 0.8) {
							gridPath.moveTo(tempPoint.x, tempPoint.y)
							gridPath.lineTo(tempPoint2.x, tempPoint2.y)
						}
					}
				}
			}
			canvas.drawPath(gridPath, linePaint)
		}
	}

	private fun drawConstellationLabels(canvas: Canvas) {
		constellations.forEach { constellation ->
			val isSelected = (constellation == selectedConstellation)
			if (!showConstellations && !isSelected) return@forEach

			val center = constellationCenters[constellation] ?: return@forEach
			
			if (skyToScreen(center.azimuth, center.altitude, tempPoint)) {
				val cx = tempPoint.x
				val cy = tempPoint.y

				if (isSelected) {
					constellationTextPaint.color = 0xFFFFD700.toInt()
					constellationTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
				} else {
					constellationTextPaint.color = 0xFFAABBFF.toInt()
					constellationTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
				}

				val text = constellation.localizedName ?: constellation.name
				val textSize = constellationTextPaint.textSize

				val textWidth = constellationTextPaint.measureText(text)
				val threshold = 10f
				val textRect = RectF(
					cx - textWidth / 2 - threshold,
					cy - threshold,
					cx + textWidth / 2 + threshold,
					cy + textSize + threshold
				)

				var overlaps = false
				for (rect in occupiedRects) {
					if (RectF.intersects(textRect, rect)) {
						overlaps = true
						break
					}
				}

				if (!overlaps || isSelected) {
					canvas.drawText(text, cx, cy + textSize, constellationTextPaint)
					occupiedRects.add(textRect)
				}
			}
		}
	}

	private fun drawSkyObject(canvas: Canvas, obj: SkyObject) {
		if (!skyToScreen(obj.azimuth, obj.altitude, tempPoint)) return

		// Heuristic to determine "Zoomed Out" factor (0.0 = Zoomed In, 1.0 = Zoomed Out)
		val zoomFactor = (viewAngle - 10.0) / (if (is2DMode) 210.0 else 140.0) // Normalize roughly 0..1
		val constrainedZoomFactor = max(0.0, min(1.0, zoomFactor))

		// 1. Dynamic Radius & Color
		var baseSize = 15f
		var color = obj.color
		
		// If significantly zoomed out and star is faint, reduce prominence
		if (obj.type == SkyObject.Type.STAR) {
			if (constrainedZoomFactor > 0.3 && obj.magnitude > 2.5) {
				baseSize = 8f  // Smaller base size for faint stars when zoomed out
				color = Color.GRAY // Fade to gray
			}
		}

		var radius = max(2f, baseSize - (obj.magnitude * 2f))
		// Further reduce radius for faint stars at high zoom
		if (obj.type == SkyObject.Type.STAR && constrainedZoomFactor > 0.5) {
			radius *= 0.7f
		}
		
		paint.style = Paint.Style.FILL
		paint.color = color
		canvas.drawCircle(tempPoint.x, tempPoint.y, radius, paint)

		val objRect = RectF(
			tempPoint.x - radius,
			tempPoint.y - radius,
			tempPoint.x + radius,
			tempPoint.y + radius
		)

		// 2. Dynamic Label Visibility
		var showLabel = true
		if (obj.type == SkyObject.Type.STAR) {
			// Don't show HIP names
			if (obj.name.startsWith("HIP", ignoreCase = true)) {
				showLabel = false
			} else {
				// Dynamic magnitude threshold for labels
				// Zoomed in (factor 0): show stars up to mag 5.0
				// Zoomed out (factor 1): show stars up to mag 1.5
				val magThreshold = 5.0 - (constrainedZoomFactor * 3.5)
				if (obj.magnitude > magThreshold) {
					showLabel = false
				}
			}
		}

		// Always show label for selected or pinned objects
		if (obj == selectedObject || pinnedObjects.contains(obj)) {
			showLabel = true
		}

		if (showLabel) {
			val text = obj.localizedName ?: obj.name
			val labelTextSize = 25f
			textPaint.textSize = labelTextSize

			val textWidth = textPaint.measureText(text)
			val xText = tempPoint.x + radius + 5
			val yText = tempPoint.y

			val threshold = 5f
			val textRect = RectF(
				xText - threshold,
				yText - labelTextSize - threshold,
				xText + textWidth + threshold,
				yText + (labelTextSize * 0.3f) + threshold
			)

			var textOverlaps = false
			for (rect in occupiedRects) {
				if (RectF.intersects(textRect, rect)) {
					textOverlaps = true
					break
				}
			}

			if (!textOverlaps || obj == selectedObject || pinnedObjects.contains(obj)) {
				textPaint.color = if (obj == selectedObject) Color.RED else if(pinnedObjects.contains(obj)) Color.YELLOW else Color.LTGRAY
				canvas.drawText(text, xText, yText, textPaint)

				occupiedRects.add(textRect)
				occupiedRects.add(objRect)
			}
		}
	}

	var isCameraMode: Boolean = false

	var is2DMode: Boolean = false
		set(value) {
			field = value
			if (!value) {
				panX = 0f; panY = 0f
			} else {
				roll = 0.0
			}
			invalidate()
		}
	private var panX: Float = 0f
	private var panY: Float = 0f

	private fun updateProjectionCache() {
		val alt0Rad = Math.toRadians(altitudeCenter)
		projSinAltCenter = sin(alt0Rad)
		projCosAltCenter = cos(alt0Rad)
		val viewAngleRad = Math.toRadians(viewAngle)
		projScale = width / (4.0 * tan(viewAngleRad / 4.0))
		projHalfWidth = width / 2.0
		projHalfHeight = height / 2.0
	}

	private fun skyToScreen(azimuth: Double, altitude: Double, outPoint: PointF): Boolean {
		val azRad = Math.toRadians(azimuth - azimuthCenter)
		val altRad = Math.toRadians(altitude)
		val sinAlt = sin(altRad)
		val cosAlt = cos(altRad)
		val sinAz = sin(azRad)
		val cosAz = cos(azRad)
		val cosC = projSinAltCenter * sinAlt + projCosAltCenter * cosAlt * cosAz
		if (cosC <= -0.3) return false
		val k = 2.0 / (1.0 + cosC)
		val combinedScale = k * projScale
		val xRaw = cosAlt * sinAz
		val yRaw = projCosAltCenter * sinAlt - projSinAltCenter * cosAlt * cosAz
		val xScaled = combinedScale * xRaw
		val yScaled = -combinedScale * yRaw

		// Flip East/West for 2D mode to match geographic map (East on Right) and fix celestial path
		val xFinal = if (is2DMode) -xScaled else xScaled
		val rollRad = Math.toRadians(roll)
		val sinRoll = sin(rollRad)
		val cosRoll = cos(rollRad)
		val xRot = xFinal * cosRoll - yScaled * sinRoll
		val yRot = xFinal * sinRoll + yScaled * cosRoll
		outPoint.x = (projHalfWidth + xRot + panX).toFloat()
		outPoint.y = (projHalfHeight + yRot + panY).toFloat()
		return true
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		scaleGestureDetector.onTouchEvent(event)
		if (scaleGestureDetector.isInProgress) {
			lastTouchX = event.x; lastTouchY = event.y; return true
		}
		gestureDetector.onTouchEvent(event)

		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> { 
				parent?.requestDisallowInterceptTouchEvent(true)
				lastTouchX = event.x; lastTouchY = event.y; isPanning = false 
			}
			MotionEvent.ACTION_MOVE -> {
				val dx = event.x - lastTouchX
				val dy = event.y - lastTouchY
				val hitThreshold = sqrt(dx * dx + dy * dy) > 10f
				if (isCameraMode && hitThreshold) {
					isPanning = true
				} else if (hitThreshold) {
					isPanning = true
					if (is2DMode) {
						panX += dx
						panY += dy
					} else {
						val scale = viewAngle / width
						azimuthCenter -= dx * scale
						altitudeCenter += dy * scale
						altitudeCenter = max(-90.0, min(90.0, altitudeCenter))
						if (azimuthCenter < 0) azimuthCenter += 360
						if (azimuthCenter >= 360) azimuthCenter -= 360
						onAzimuthManualChangeListener?.invoke(azimuthCenter)
					}
					lastTouchX = event.x; lastTouchY = event.y
					invalidate()
				}
			}
			MotionEvent.ACTION_POINTER_DOWN -> {
				lastTouchX = event.getX(event.actionIndex)
				lastTouchY = event.getY(event.actionIndex)
			}
			MotionEvent.ACTION_POINTER_UP -> {
				val pointerIndex = if (event.actionIndex == 0) 1 else 0
				lastTouchX = event.getX(pointerIndex)
				lastTouchY = event.getY(pointerIndex)
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { 
				isPanning = false
			}
		}
		return true
	}

	private fun performClickAt(x: Float, y: Float) {
		val clickRadius = 60f
		var bestObj: SkyObject? = null
		var bestDist = Float.MAX_VALUE

		// 1. Check Stars/Planets
		for (obj in skyObjects) {
			if (!isObjectVisibleInSettings(obj)) continue
			if (skyToScreen(obj.azimuth, obj.altitude, tempPoint)) {
				val dist = hypot(x - tempPoint.x, y - tempPoint.y)
				if (dist < clickRadius && dist < bestDist) {
					bestDist = dist; bestObj = obj
				}
			}
		}

		if (bestObj != null) {
			setSelectedObject(bestObj)
			invalidate()
			onObjectClickListener?.invoke(bestObj)
			onConstellationClickListener?.invoke(null)
			return
		}

		// 2. Check Constellations
		if (showConstellations || selectedConstellation != null) {
			var bestConst: Constellation? = null
			var bestConstDist = Float.MAX_VALUE

			constellations.forEach { constellation ->
				val isSelected = (constellation == selectedConstellation)
				if (!showConstellations && !isSelected) return@forEach

				// Check distance to lines
				constellation.lines.forEach { (id1, id2) ->
					val s1 = skyObjectMap[id1]
					val s2 = skyObjectMap[id2]
					if (s1 != null && s2 != null &&
						skyToScreen(s1.azimuth, s1.altitude, tempPoint) &&
						skyToScreen(s2.azimuth, s2.altitude, tempPoint2)) {

						val dist = distanceFromPointToSegment(x, y, tempPoint.x, tempPoint.y, tempPoint2.x, tempPoint2.y)
						if (dist < clickRadius && dist < bestConstDist) {
							bestConstDist = dist
							bestConst = constellation
						}
					}
				}

				// Check distance to label center
				var avgX = 0f; var avgY = 0f; var count = 0
				val uniqueStars = mutableSetOf<Int>()
				constellation.lines.forEach { (id1, id2) -> uniqueStars.add(id1); uniqueStars.add(id2) }
				uniqueStars.forEach { id ->
					val s = skyObjectMap[id]
					if (s != null && skyToScreen(s.azimuth, s.altitude, tempPoint)) {
						avgX += tempPoint.x; avgY += tempPoint.y; count++
					}
				}
				if (count > 0) {
					val cx = avgX / count
					val cy = avgY / count
					val dist = hypot(x - cx, y - cy)
					if (dist < clickRadius && dist < bestConstDist) {
						bestConstDist = dist
						bestConst = constellation
					}
				}
			}

			if (bestConst != null) {
				setSelectedConstellation(bestConst)
				selectedObject = null
				onObjectClickListener?.invoke(null)
				onConstellationClickListener?.invoke(bestConst)
				return
			}
		}

		// 3. Nothing clicked (Deselect focused object, but keep pins)
		selectedObject = null
		setSelectedConstellation(null)
		invalidate()
		onObjectClickListener?.invoke(null)
		onConstellationClickListener?.invoke(null)
	}

	private fun distanceFromPointToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
		val A = px - x1
		val B = py - y1
		val C = x2 - x1
		val D = y2 - y1

		val dot = A * C + B * D
		val len_sq = C * C + D * D
		val param = if (len_sq != 0f) dot / len_sq else -1f

		val xx: Float
		val yy: Float

		if (param < 0) {
			xx = x1
			yy = y1
		} else if (param > 1) {
			xx = x2
			yy = y2
		} else {
			xx = x1 + param * C
			yy = y1 + param * D
		}

		val dx = px - xx
		val dy = py - yy
		return hypot(dx, dy)
	}

	private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
		override fun onScale(detector: ScaleGestureDetector): Boolean {
			updateViewAngle(viewAngle / detector.scaleFactor, detector.focusX, detector.focusY)
			return true
		}
	}
}