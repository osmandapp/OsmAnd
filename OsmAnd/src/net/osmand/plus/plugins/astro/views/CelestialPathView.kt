package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withSave
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Topocentric
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.AstroUtils
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

class CelestialPathView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : BaseChartView(context, attrs, defStyleAttr, defStyleRes) {

	private var extras: List<CelestialEntry> = emptyList()
	private var moment: ZonedDateTime? = null

	// Pre-calculated path data to avoid recalculation in onDraw
	private data class CachedPath(
		val entry: CelestialEntry,
		val segments: List<PathSegment>
	)

	private data class PathSegment(
		val path: Path,
		val color: Int,
		// Store raw points for hit testing if needed, though simplified for now
		val hitPoints: FloatArray
	)

	private data class Model(
		override val startLocal: ZonedDateTime,
		override val endLocal: ZonedDateTime,
		val observer: Observer,
		val paths: List<CachedPath> // Pre-calculated paths
	) : BaseModel(startLocal, endLocal)

	private var cachedModel: Model? = null

	enum class Projection { EQUIDISTANT, STEREOGRAPHIC }

	// Configuration specific to this view
	var viewConfig: ViewConfig = ViewConfig()
		set(value) {
			field = value
			rebuildModel()
			invalidate()
		}

	data class ViewConfig(
		val projection: Projection = Projection.EQUIDISTANT,
		val showPaths: Boolean = true,
		val showInstantLocations: Boolean = true,
		val minuteSample: Int = 10
	)

	// Store default planets as CelestialEntries
	private val defaultPlanets = AstroUtils.visibleBodies.map {
		CelestialEntry.Planet(it, AstroUtils.bodyColor(it))
	}

	sealed class CelestialEntry(val name: String, val color: Int, val drawPath: Boolean = true) {
		class Planet(val body: Body, color: Int, drawPath: Boolean = true)
			: CelestialEntry(AstroUtils.bodyName(body), color, drawPath)
		class Fixed(
			name: String,
			val raHours: Double,
			val decDeg: Double,
			val distanceLy: Double = 1000.0,
			color: Int,
			drawPath: Boolean = true
		) : CelestialEntry(name, color, drawPath)
	}

	fun setCelestialDatabase(entries: List<CelestialEntry>) { extras = entries; rebuildModel(); invalidate() }
	fun setMoment(dateTime: ZonedDateTime?) { moment = dateTime; invalidate() }

	// ---------- Interaction (pan/zoom/tap) ----------
	private val viewMatrix = Matrix()
	private val inverseMatrix = Matrix()
	private var scale = 1f
	private var translateX = 0f
	private var translateY = 0f
	private var selected: CelestialEntry? = null

	// Re-use polylines from the Model for hit testing
	private fun getHitTestPolylines(): List<Pair<CelestialEntry, FloatArray>> {
		return cachedModel?.paths?.flatMap { cp ->
			cp.segments.map { seg -> cp.entry to seg.hitPoints }
		} ?: emptyList()
	}

	private val scaleDetector = ScaleGestureDetector(context,
		object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
			override fun onScale(det: ScaleGestureDetector): Boolean {
				val prev = scale
				scale = (scale * det.scaleFactor).coerceIn(0.6f, 4f)
				val f = scale / prev
				val cx = width / 2f
				val cy = height / 2f
				translateX = translateX * f + (det.focusX - cx) * (1 - f)
				translateY = translateY * f + (det.focusY - cy) * (1 - f)

				updateMatrix(); invalidate(); return true
			}
		})

	private val gestureDetector = GestureDetector(context,
		object : GestureDetector.SimpleOnGestureListener() {
			override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
				translateX -= dx; translateY -= dy; updateMatrix(); invalidate(); return true
			}

			override fun onDoubleTap(e: MotionEvent): Boolean { resetTransform(); invalidate(); return true }

			override fun onSingleTapUp(e: MotionEvent): Boolean {
				val pts = floatArrayOf(e.x, e.y)
				inverseMatrix.mapPoints(pts)
				val cx = width / 2f
				val cy = height / 2f

				// Calculate the scale factor used to draw the paths
				val actualRadius = 0.9f * min(cx, cy)
				val pathScale = if (CANONICAL_RADIUS > 0f) actualRadius / CANONICAL_RADIUS else 1f

				// Convert touch point to "Canonical Space" to match hitPoints
				// We must divide by pathScale to reverse the scaling applied in onDraw
				val x = (pts[0] - cx) / pathScale
				val y = (pts[1] - cy) / pathScale

				// Adjust threshold for both zoom (scale) and path scaling
				val thresh = (dp(14f) / scale) / pathScale

				var best: Pair<CelestialEntry, Float>? = null

				val candidates = getHitTestPolylines()
				for ((entry, points) in candidates) {
					val d = distanceToPolyline(x, y, points)
					if (d < thresh && (best == null || d < best.second)) {
						best = entry to d
					}
				}
				selected = best?.first
				invalidate()
				return true
			}
		})

	// ... [Distance Math Helpers preserved] ...
	private fun distanceToPolyline(x: Float, y: Float, pts: FloatArray): Float {
		var best = Float.MAX_VALUE
		var i = 0
		while (i + 3 < pts.size) {
			val x1 = pts[i]; val y1 = pts[i+1]; val x2 = pts[i+2]; val y2 = pts[i+3]
			val d = pointToSegmentDistance(x, y, x1, y1, x2, y2)
			if (d < best) best = d
			i += 2
		}
		return best
	}

	private fun pointToSegmentDistance(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
		val dx = x2 - x1; val dy = y2 - y1
		val t = ((px - x1)*dx + (py - y1)*dy) / (dx*dx + dy*dy + 1e-6f)
		val u = t.coerceIn(0f, 1f)
		val cx = x1 + u*dx; val cy = y1 + u*dy
		return hypot(px - cx, py - cy)
	}

	private fun resetTransform() { scale = 1f; translateX = 0f; translateY = 0f; updateMatrix() }

	private fun updateMatrix() {
		viewMatrix.reset()
		viewMatrix.postScale(scale, scale, width/2f, height/2f)
		viewMatrix.postTranslate(translateX, translateY)
		viewMatrix.invert(inverseMatrix)
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		val a = scaleDetector.onTouchEvent(event)
		val b = gestureDetector.onTouchEvent(event)
		return a || b || super.onTouchEvent(event)
	}

	// ---------- Paints ----------
	// [Paints preserved, colors can be extracted to resources if needed]
	private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#E4424242".toColorInt() }
	private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE; color = 0x33FFFFFF; strokeWidth = dp(1f)
		pathEffect = DashPathEffect(floatArrayOf(dp(2f), dp(4f)), 0f)
	}
	private val spokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f) }
	private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sp(12f); textAlign = Paint.Align.CENTER }
	private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(10f); textAlign = Paint.Align.CENTER }
	private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(2f) }
	private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
	private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE; textSize = sp(18f); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textAlign = Paint.Align.CENTER
	}
	private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x8C101114.toInt() }
	private val panelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x55FFFFFF; strokeWidth = dp(1f) }
	private val hourPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sp(11f); textAlign = Paint.Align.LEFT }
	private val hourDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }

	// Helper methods
	private fun topocentric(body: Body, t: ZonedDateTime, obs: Observer): Topocentric {
		val utc = Time.fromMillisecondsSince1970(t.toInstant().toEpochMilli())
		val eq = equator(body, utc, obs, EquatorEpoch.OfDate, Aberration.Corrected)
		return horizon(utc, obs, eq.ra, eq.dec, Refraction.Normal)
	}

	private fun topocentricFromRaDec(raHours: Double, decDeg: Double, t: ZonedDateTime, obs: Observer): Topocentric {
		val utc = Time.fromMillisecondsSince1970(t.toInstant().toEpochMilli())
		defineStar(Body.Star1, raHours, decDeg, 1000.0)
		val eq = equator(Body.Star1, utc, obs, EquatorEpoch.OfDate, Aberration.None)
		return horizon(utc, obs, eq.ra, eq.dec, Refraction.Normal)
	}

	// The radius is fixed for path calculation context (normalized to 1.0 or similar would be better,
	// but we need actual screen coords for path. Let's use a canonical radius and scale during draw)
	private val CANONICAL_RADIUS = 1000f

	override fun rebuildModel() {
		val startLocal = config.date.atStartOfDay(config.zoneId)
		val endLocal = startLocal.plusDays(1)
		val obs = Observer(config.latitude, config.longitude, config.elevation)

		val entries = defaultPlanets + extras
		val calculatedPaths = if (viewConfig.showPaths) {
			entries.filter { it.drawPath }.map { entry ->
				computePathForEntry(entry, startLocal, endLocal, obs)
			}
		} else emptyList()

		cachedModel = Model(startLocal, endLocal, obs, calculatedPaths)
	}

	/**
	 * Heavy calculation moved here. Generates paths based on CANONICAL_RADIUS.
	 */
	private fun computePathForEntry(entry: CelestialEntry, start: ZonedDateTime, end: ZonedDateTime, obs: Observer): CachedPath {
		val stepMin = viewConfig.minuteSample.toLong().coerceAtLeast(1)
		var t = start

		// We group segments by color (hour bands)
		val segments = ArrayList<PathSegment>()

		var currentPath = Path()
		var currentPoints = ArrayList<Float>()
		var currentColor = -1
		var firstInSegment = true

		fun flush() {
			if (!currentPath.isEmpty) {
				segments.add(PathSegment(currentPath, currentColor, currentPoints.toFloatArray()))
			}
			currentPath = Path()
			currentPoints = ArrayList()
			firstInSegment = true
		}

		while (!t.isAfter(end)) {
			val topo = when (entry) {
				is CelestialEntry.Planet -> topocentric(entry.body, t, obs)
				is CelestialEntry.Fixed -> topocentricFromRaDec(entry.raHours, entry.decDeg, t, obs)
			}

			val color = colorForHour(t.hour)
			if (currentColor != -1 && currentColor != color) {
				flush()
			}
			currentColor = color

			if (topo.altitude > -1.0) {
				val p = azAltToPoint(topo.azimuth, topo.altitude, CANONICAL_RADIUS)
				if (firstInSegment) {
					currentPath.moveTo(p.x, p.y)
					firstInSegment = false
				} else {
					currentPath.lineTo(p.x, p.y)
				}
				currentPoints.add(p.x)
				currentPoints.add(p.y)
			} else {
				// Below horizon, break the line
				flush()
			}
			t = t.plusMinutes(stepMin)
		}
		flush()
		return CachedPath(entry, segments)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		// Ensure model exists (handles first draw or config changes)
		if (cachedModel == null) rebuildModel()
		val m = cachedModel!!

		canvas.drawColor(bgPaint.color)
		updateMatrix()

		val cx = width / 2f
		val cy = height / 2f
		val actualRadius = 0.9f * min(cx, cy)

		// Calculate scale factor between canonical radius used for paths and actual screen radius
		val pathScale = actualRadius / CANONICAL_RADIUS

		canvas.drawText(context.getString(R.string.ltr_or_rtl_combine_via_dash, context.getString(R.string.celestial_paths_name), config.date), cx, dp(25f), titlePaint)

		canvas.withMatrix(viewMatrix) {
			withSave {
				translate(cx, cy)
				drawGrid(this, actualRadius)

				// Draw pre-calculated paths
				if (viewConfig.showPaths) {
					// We need to scale the paths to match current view size
					scale(pathScale, pathScale)

					for (cp in m.paths) {
						val isSelected = (cp.entry == selected)
						for (seg in cp.segments) {
							pathPaint.color = seg.color
							pathPaint.strokeWidth = (if (isSelected) dp(3.2f) else dp(2f)) / pathScale // Compensate for scale
							drawPath(seg.path, pathPaint)
						}
					}
					// Restore scale
					scale(1f/pathScale, 1f/pathScale)
				}

				// Instant markers (still calculated on draw, but cheap compared to paths)
				if (viewConfig.showInstantLocations) {
					val entries = defaultPlanets + extras
					val t = (moment ?: ZonedDateTime.now(config.zoneId)).withSecond(0).withNano(0)
					for (entry in entries) {
						val topo = when (entry) {
							is CelestialEntry.Planet -> topocentric(entry.body, t, m.observer)
							is CelestialEntry.Fixed -> topocentricFromRaDec(entry.raHours, entry.decDeg, t, m.observer)
						}
						if (topo.altitude > 0) {
							val p = azAltToPoint(topo.azimuth, topo.altitude, actualRadius)
							markerPaint.color = entry.color
							drawCircle(p.x, p.y, dp(3.5f) / scale, markerPaint)
							drawText(entry.name, p.x, p.y - dp(10f) / scale, smallPaint)
						}
					}
				}
			}
		}

		drawHourlyLegend(canvas)
		selected?.let { drawInfoPanel(canvas, it, m) }
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val h = dp(300f).toInt()
		val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
		setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
	}

	// ---------- Grid & projection ----------
	private fun drawGrid(canvas: Canvas, radius: Float) {
		for (alt in 0..80 step 10) {
			val r = altToRadius(alt.toFloat(), radius)
			canvas.drawCircle(0f, 0f, r, ringPaint)
			if (scale > 1.1f) {
				drawText(canvas, "${alt}°", +r, dp(2f)/scale, smallPaint)
				drawText(canvas, "${alt}°", -r, dp(2f)/scale, smallPaint)
			}
		}
		val compass = linkedMapOf(
			"N" to 0.0, "NNE" to 22.5, "NE" to 45.0, "ENE" to 67.5,
			"E" to 90.0, "ESE" to 112.5, "SE" to 135.0, "SSE" to 157.5,
			"S" to 180.0, "SSW" to 202.5, "SW" to 225.0, "WSW" to 247.5,
			"W" to 270.0, "WNW" to 292.5, "NW" to 315.0, "NNW" to 337.5
		)
		for ((dir, az) in compass) {
			val a = Math.toRadians(az - 90.0)
			val isMajor = az % 90.0 == 0.0
			val isMinor = az % 45.0 == 0.0
			spokePaint.color = if (isMajor) 0x4CFFFFFF else 0x26FFFFFF
			canvas.drawLine(0f, 0f, radius * cos(a).toFloat(), radius * sin(a).toFloat(), spokePaint)
			if (isMajor || (scale > 1.2f && isMinor) || scale > 2.5f) {
				labelPaint.textSize = (if (isMajor) sp(14f) else sp(10f)) / scale
				val x = (radius + dp(14f)/scale) * cos(a).toFloat()
				val y = (radius + dp(14f)/scale) * sin(a).toFloat()
				drawText(canvas, dir, x, y, labelPaint)
			}
		}
	}

	private fun altToRadius(altDeg: Float, radius: Float): Float =
		when (viewConfig.projection) {
			Projection.EQUIDISTANT -> radius * (1f - (altDeg / 90f))
			Projection.STEREOGRAPHIC -> radius * tan(Math.toRadians((90 - altDeg)/2.0)).toFloat()
		}

	private fun azAltToPoint(azDeg: Double, altDeg: Double, radius: Float): PointF {
		val r = altToRadius(altDeg.toFloat(), radius)
		val theta = Math.toRadians(azDeg - 90.0)
		return PointF(r * cos(theta).toFloat(), r * sin(theta).toFloat())
	}

	// ---------- Hour bands ----------
	private val hourStops = intArrayOf(0,3,6,9,12,15,18,21)
	private val hourColors = intArrayOf(
		"#FFE46B".toColorInt(), "#B5F46B".toColorInt(), "#7CE5FF".toColorInt(), "#6EB2FF".toColorInt(),
		"#A98BFF".toColorInt(), "#FF7AD8".toColorInt(), "#FFB36E".toColorInt(), "#FF814C".toColorInt()
	)
	private fun colorForHour(h: Int): Int {
		val idx = hourStops.indexOfLast { h >= it }
		return hourColors[if (idx < 0) 0 else idx]
	}

	// ---------- Legend ----------
	private fun drawHourlyLegend(canvas: Canvas) {
		val labels = arrayOf("00","03","06","09","12","15","18","21")
		val dot = dp(8f)
		val gap = dp(10f)
		val hourWidth = hourPaint.measureText("00")
		val widthNeeded = labels.size * (dot + gap + hourWidth + gap) - gap * 2f + dp(24f)
		val left = (width - widthNeeded) / 2f
		val top = height - dp(44f)
		val r = RectF(left, top, left + widthNeeded, top + dp(32f))

		var x = r.left + dp(12f)
		val y = r.centerY()
		for (i in labels.indices) {
			canvas.drawCircle(x + dot / 2, y, dot / 2, hourDotPaint.apply { color = hourColors[i] })
			canvas.drawText(labels[i], x + dot + dp(4f), y + sp(4f), hourPaint)
			x += gap + dot + gap + hourWidth
		}
	}

	// ---------- Info panel ----------
	private fun drawInfoPanel(canvas: Canvas, entry: CelestialEntry, m: Model) {
		val panelH = dp(76f)
		val r = RectF(dp(12f), height - panelH - dp(12f), width - dp(12f), height - dp(12f))
		canvas.drawRoundRect(r, dp(12f), dp(12f), panelPaint)
		canvas.drawRoundRect(r, dp(12f), dp(12f), panelStroke)

		val title = entry.name
		val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sp(14f) }
		canvas.drawText(title, r.left + dp(12f), r.top + dp(22f), titlePaint)

		val fmt = DateTimeFormatter.ofPattern("EEE, HH:mm")
		val (rise, set) = nextRiseSet(entry, m)
		val linePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE0E0E0.toInt(); textSize = sp(12f) }

		val lineY1 = r.top + dp(44f)
		val lineY2 = r.top + dp(64f)
		canvas.drawText("Next Rise: ${rise?.format(fmt) ?: "—"}", r.left + dp(12f), lineY1, linePaint)
		canvas.drawText("Next Set : ${set ?.format(fmt) ?: "—"}", r.left + dp(12f), lineY2, linePaint)
	}

	private fun nextRiseSet(entry: CelestialEntry, m: Model): Pair<ZonedDateTime?, ZonedDateTime?> {
		val startTime = Time.fromMillisecondsSince1970(m.startLocal.toInstant().toEpochMilli())
		fun toZdt(t: Time?): ZonedDateTime? =
			t?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(config.zoneId) }

		return when (entry) {
			is CelestialEntry.Planet -> {
				val r = searchRiseSet(entry.body, m.observer, Direction.Rise, startTime, +2.0)
				val s = searchRiseSet(entry.body, m.observer, Direction.Set , startTime, +2.0)
				toZdt(r) to toZdt(s)
			}
			is CelestialEntry.Fixed -> {
				defineStar(Body.Star1, entry.raHours, entry.decDeg, 1000.0)
				val r = searchRiseSet(Body.Star1, m.observer, Direction.Rise, startTime, +2.0)
				val s = searchRiseSet(Body.Star1, m.observer, Direction.Set , startTime, +2.0)
				toZdt(r) to toZdt(s)
			}
		}
	}

	private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, tp: TextPaint) { canvas.drawText(text, x, y, tp) }
}