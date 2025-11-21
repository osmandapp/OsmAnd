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
import net.osmand.plus.plugins.astro.AstroUtils
import net.osmand.plus.utils.AndroidUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

	private data class Model(
		override val startLocal: ZonedDateTime,
		override val endLocal: ZonedDateTime,
		val observer: Observer
	) : BaseModel(startLocal, endLocal)

	private var cachedModel: Model? = null
	private var config = Config()

	enum class Projection { EQUIDISTANT, STEREOGRAPHIC }

	data class Config(
		override val date: LocalDate = LocalDate.now(),
		override val zoneId: ZoneId = ZoneId.systemDefault(),
		override val latitude: Double = 0.0,
		override val longitude: Double = 0.0,
		override val elevation: Double = 0.0,
		val projection: Projection = Projection.EQUIDISTANT,
		val showPaths: Boolean = true,
		val showInstantLocations: Boolean = true,
		val minuteSample: Int = 10
	) : BaseConfig(date, zoneId, latitude, longitude, elevation) {
		fun equalsTo(other: Config): Boolean {
			if (super.equalsTo(other)) return false

			if (projection != other.projection) return false
			if (showPaths != other.showPaths) return false
			if (showInstantLocations != other.showInstantLocations) return false
			return (minuteSample != other.minuteSample)
		}
	}

	private fun updateConfig() {
		val loc = mapTileView.currentRotatedTileBox.centerLatLon
		val config = Config(
			date = LocalDate.now(),
			zoneId = ZoneId.systemDefault(),
			latitude = loc.latitude,
			longitude = loc.longitude,
			elevation = 0.0,
			projection = config.projection,
			showPaths = config.showPaths,
			showInstantLocations = config.showInstantLocations,
			minuteSample = config.minuteSample
		)
		if (!config.equalsTo(this.config)) { this.config = config; cachedModel = null }
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

	fun setCelestialDatabase(entries: List<CelestialEntry>) { extras = entries; invalidate() }
	fun setMoment(dateTime: ZonedDateTime?) { moment = dateTime; invalidate() }
	fun setConfig(newConfig: Config) { config = newConfig; cachedModel = null; invalidate() }

	private val defaultPlanets = listOf(
		CelestialEntry.Planet(Body.Sun,  "#FFE53B".toColorInt()),
		CelestialEntry.Planet(Body.Moon, "#D0D0DC".toColorInt()),
		CelestialEntry.Planet(Body.Mercury, "#FFD857".toColorInt()),
		CelestialEntry.Planet(Body.Venus,   "#C6F0FF".toColorInt()),
		CelestialEntry.Planet(Body.Mars,    "#F28B82".toColorInt()),
		CelestialEntry.Planet(Body.Jupiter, "#A3E635".toColorInt()),
		CelestialEntry.Planet(Body.Saturn,  "#FDE68A".toColorInt())
	)

	// ---------- Interaction (pan/zoom/tap) ----------
	private val viewMatrix = Matrix()
	private val inverseMatrix = Matrix()
	private var scale = 1f
	private var translateX = 0f
	private var translateY = 0f

	// store polylines per entry for hit-testing
	private data class Poly(val entry: CelestialEntry, val points: FloatArray)
	private val polylines: MutableList<Poly> = mutableListOf()
	private var selected: CelestialEntry? = null

	private val scaleDetector = ScaleGestureDetector(context,
		object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
			override fun onScale(det: ScaleGestureDetector): Boolean {
				val prev = scale
				scale = (scale * det.scaleFactor).coerceIn(0.6f, 4f)
				val f = scale / prev
				translateX = det.focusX + (translateX - det.focusX) * f
				translateY = det.focusY + (translateY - det.focusY) * f
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
				// convert tap to chart space
				val pts = floatArrayOf(e.x, e.y)
				inverseMatrix.mapPoints(pts)
				// subtract chart center
				val cx = width/2f; val cy = height/2f
				val x = pts[0] - cx; val y = pts[1] - cy
				val thresh = dp(14f) / scale
				var best: Pair<CelestialEntry, Float>? = null
				for (poly in polylines) {
					val d = distanceToPolyline(x, y, poly.points)
					if (d < thresh && (best == null || d < best.second)) {
						best = poly.entry to d
					}
				}
				selected = best?.first
				invalidate()
				return true
			}
		})

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
		viewMatrix.postTranslate(translateX, translateY)
		viewMatrix.postScale(scale, scale, width/2f, height/2f)
		viewMatrix.invert(inverseMatrix)
	}
	override fun onTouchEvent(event: MotionEvent): Boolean {
		val a = scaleDetector.onTouchEvent(event)
		val b = gestureDetector.onTouchEvent(event)
		return a || b || super.onTouchEvent(event)
	}

	// ---------- Paints ----------
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

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		updateConfig()
		val m = cachedModel ?: buildModel().also { cachedModel = it }

		canvas.drawColor(bgPaint.color)
		updateMatrix()

		val cx = width / 2f
		val cy = height / 2f
		val radius = 0.9f * min(cx, cy)

		// title
		canvas.drawText("Celestial Paths — ${config.date}", cx, dp(25f), titlePaint)

		polylines.clear()

		canvas.withMatrix(viewMatrix) {
			withSave {
				translate(cx, cy)
				drawGrid(this, radius)

				val entries = defaultPlanets + extras

				if (config.showPaths) {
					for (entry in entries) {
						if (!entry.drawPath) continue
						drawColoredPath(canvas, entry, m, radius)
					}
				}

				// instant markers
				if (config.showInstantLocations) {
					val t = (moment ?: ZonedDateTime.now(config.zoneId)).withSecond(0).withNano(0)
					for (entry in entries) {
						val topo = when (entry) {
							is CelestialEntry.Planet -> topocentric(entry.body, t, m.observer)
							is CelestialEntry.Fixed -> topocentricFromRaDec(entry.raHours, entry.decDeg, t, m.observer)
						}
						if (topo.altitude > 0) {
							val p = azAltToPoint(topo.azimuth, topo.altitude, radius)
							markerPaint.color = entry.color
							drawCircle(p.x, p.y, dp(3.5f) / scale, markerPaint)
							drawText(entry.name, p.x, p.y - dp(10f) / scale, smallPaint)
						}
					}
				}
			}
		}

		// hourly legend (1)
		drawHourlyLegend(canvas)

		// info panel (2) for selection
		selected?.let { drawInfoPanel(canvas, it, m) }
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val h = dp(300f).toInt()
		val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
		setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
	}

	private fun buildModel(): Model {
		val startLocal = config.date.atStartOfDay(config.zoneId)
		val endLocal = startLocal.plusDays(1)
		val obs = Observer(config.latitude, config.longitude, config.elevation)
		return Model(startLocal, endLocal, obs)
	}

	// ---------- Grid & projection ----------
	private fun drawGrid(canvas: Canvas, radius: Float) {
		for (alt in 0..80 step 10) {
			val r = altToRadius(alt.toFloat(), radius)
			canvas.drawCircle(0f, 0f, r, ringPaint)
			if (scale > 1.1f) {
				drawText(canvas, "${alt}°", +r, dp(2f)/scale, smallPaint)
				drawText(canvas, "${alt}°", -r, dp(2f)/scale, smallPaint)
				drawText(canvas, "${alt}°", 0f,  r + dp(6f)/scale, smallPaint)
				drawText(canvas, "${alt}°", 0f, -r + dp(6f)/scale, smallPaint)
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
		when (config.projection) {
			Projection.EQUIDISTANT -> radius * (1f - (altDeg / 90f))
			Projection.STEREOGRAPHIC -> radius * tan(Math.toRadians((90 - altDeg)/2.0)).toFloat()
		}

	private fun azAltToPoint(azDeg: Double, altDeg: Double, radius: Float): PointF {
		val r = altToRadius(altDeg.toFloat(), radius)
		val theta = Math.toRadians(azDeg - 90.0)
		return PointF(r * cos(theta).toFloat(), r * sin(theta).toFloat())
	}

	// ---------- Hour bands (1) ----------
	private val hourStops = intArrayOf(0,3,6,9,12,15,18,21) // UTC bands
	private val hourColors = intArrayOf(
		"#FFE46B".toColorInt(), // 00
		"#B5F46B".toColorInt(), // 03
		"#7CE5FF".toColorInt(), // 06
		"#6EB2FF".toColorInt(), // 09
		"#A98BFF".toColorInt(), // 12
		"#FF7AD8".toColorInt(), // 15
		"#FFB36E".toColorInt(), // 18
		"#FF814C".toColorInt()  // 21
	)
	private fun colorForHour(h: Int): Int {
		val idx = hourStops.indexOfLast { h >= it }
		return hourColors[if (idx < 0) 0 else idx]
	}

	private fun drawColoredPath(canvas: Canvas, entry: CelestialEntry, m: Model, radius: Float) {
		// build segments grouped by UTC hour band
		val stepMin = config.minuteSample.toLong().coerceAtLeast(1)
		var t = m.startLocal
		val end = m.endLocal
		val ptsForHit = ArrayList<Float>(512)

		var currentColor = -1
		val path = Path()
		var first = true

		fun flushSegment() {
			if (first) return
			pathPaint.color = currentColor
			pathPaint.strokeWidth = if (entry == selected) dp(3.2f) else dp(2f)
			canvas.drawPath(path, pathPaint)
			path.reset()
			first = true
		}

		while (!t.isAfter(end)) {
			val topo = when (entry) {
				is CelestialEntry.Planet -> topocentric(entry.body, t, m.observer)
				is CelestialEntry.Fixed -> topocentricFromRaDec(entry.raHours, entry.decDeg, t, m.observer)
			}
			val color = colorForHour(t.hour)
			if (currentColor != color) { flushSegment(); currentColor = color }
			if (topo.altitude > -1.0) {
				val p = azAltToPoint(topo.azimuth, topo.altitude, radius)
				if (first) { path.moveTo(p.x, p.y); first = false } else path.lineTo(p.x, p.y)
				ptsForHit += p.x; ptsForHit += p.y
			}
			t = t.plusMinutes(stepMin)
		}
		flushSegment()

		// label & rise/set markers
		if (ptsForHit.isNotEmpty()) {
			val mid = ptsForHit.size/4
			//drawText(canvas, entry.name, ptsForHit[mid], ptsForHit[mid+1] - dp(8f)/scale, smallPaint)
			polylines += Poly(entry, ptsForHit.toFloatArray())
		}
	}

	// ---------- Legend (1) ----------
	private fun drawHourlyLegend(canvas: Canvas) {
		val labels = arrayOf("00","03","06","09","12","15","18","21")
		// layout bottom center
		val dot = dp(8f)
		val gap = dp(10f)
		val hourWidth = hourPaint.measureText("00")
		val widthNeeded = labels.size * (dot + gap + hourWidth + gap) - gap * 2f + dp(24f)
		val left = (width - widthNeeded) / 2f
		val top = height - dp(44f)
		val r = RectF(left, top, left + widthNeeded, top + dp(32f))
		// panel bg
		//canvas.drawRoundRect(r, dp(8f), dp(8f), panelPaint)
		//canvas.drawRoundRect(r, dp(8f), dp(8f), panelStroke)

		var x = r.left + dp(12f)
		val y = r.centerY()
		for (i in labels.indices) {
			canvas.drawCircle(x + dot / 2, y, dot / 2, hourDotPaint.apply { color = hourColors[i] })
			canvas.drawText(labels[i], x + dot + dp(4f), y + sp(4f), hourPaint)
			x += gap + dot + gap + hourWidth
		}
	}

	// ---------- Info panel (2) ----------
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

	private fun dp(v: Float) = AndroidUtils.dpToPxF(context, v)
	private fun sp(v: Float) = AndroidUtils.spToPxF(context, v)
}
