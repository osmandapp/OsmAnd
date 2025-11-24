package net.osmand.plus.plugins.astro.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.AstroUtils
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

class StarAltitudeChartView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : BaseChartView(context, attrs, defStyleAttr, defStyleRes) {

	// Pre-calculated drawing data. No allocations allowed in onDraw.
	private data class Model(
		val title: String,
		override val startLocal: ZonedDateTime,
		override val endLocal: ZonedDateTime,
		val seriesPaths: List<SeriesPath>, // Paths ready to draw
		val twilight: AstroUtils.Twilight,
		val yMin: Double,
		val yMax: Double
	) : BaseModel(startLocal, endLocal)

	private data class SeriesPath(
		val body: Body,
		val path: Path, // The expensive path is calculated in background
		val name: String,
		val rise: String,
		val set: String
	)

	private var cachedModel: Model? = null

	// Config specific
	var showTwilightBands: Boolean = true
		set(value) { field = value; triggerAsyncRebuild() }
	var sampleMinutes: Int = 20
		set(value) { field = value; triggerAsyncRebuild() }
	var yMin: Double = -30.0
		set(value) { field = value; triggerAsyncRebuild() }
	var yMax: Double = +90.0
		set(value) { field = value; triggerAsyncRebuild() }

	// ---------- Layout ----------
	private val leftPad = dp(16f)
	private val legendW by lazy { measureText("Jupiter 00:00 00:00") + dp(16f) }
	private val nameW by lazy { smallPaint.measureText("Mercury") }
	private val headerH = dp(40f)
	private val topAxisH = dp(28f)
	private val rightPad = dp(20f)
	private val bottomPad = dp(20f)
	private val legendLineH = dp(22f)

	// ---------- Paints ----------
	private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#E4424242".toColorInt() }
	private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#80868B".toColorInt(); strokeWidth = dp(1f) }
	private val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE; strokeWidth = dp(1.2f); pathEffect = DashPathEffect(floatArrayOf(10f,10f), 0f)
	}
	private val nightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#60000000".toColorInt() }
	private val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#57C7F3".toColorInt() }
	private val twiAstro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC2B4C7E".toColorInt() }
	private val twiNaut  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC3C7AA6".toColorInt() }
	private val twiCivil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC5BBBF0".toColorInt() }
	private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE; textSize = sp(18f); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textAlign = Paint.Align.CENTER
	}
	private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }
	private val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }
	private val axisPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }

	private val seriesPaints: Map<Body, Paint> = visibleBodies.associateWith {
		Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = AstroUtils.bodyColor(it); strokeWidth = dp(2f); style = Paint.Style.STROKE
		}
	}

	override suspend fun computeModel(config: Config, width: Int, height: Int): Any {
		val zone = config.zoneId
		val startLocal = config.date.atTime(12, 0).atZone(zone)
		val endLocal = startLocal.plusDays(1)
		val obs = Observer(config.latitude, config.longitude, config.elevation)
		val stepMinutes = sampleMinutes.toLong()

		// Layout calculations needed for Path generation
		val chartLeft = leftPad + legendW + dp(12f)
		val chartTop = headerH + topAxisH
		val chartRight = width - rightPad
		val chartBottom = height - bottomPad

		fun computeRiseSet(body: Body): Pair<ZonedDateTime?, ZonedDateTime?> {
			val searchStartUtc = Time.fromMillisecondsSince1970(startLocal.toInstant().toEpochMilli())
			val limitDays = 2.0
			val nextRise = searchRiseSet(body, obs, Direction.Rise, searchStartUtc, +limitDays)
			val nextSet  = searchRiseSet(body, obs, Direction.Set , searchStartUtc, +limitDays)
			fun Time?.toZ() = this?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(config.zoneId) }
			val r = nextRise.toZ()?.takeIf { !it.isBefore(startLocal) && !it.isAfter(endLocal) }
			val s = nextSet .toZ()?.takeIf { !it.isBefore(startLocal) && !it.isAfter(endLocal) }
			return r to s
		}

		// Optimization: Calculate helpers once
		val totalMillis = Duration.between(startLocal, endLocal).toMillis().toDouble()
		val chartWidth = chartRight - chartLeft
		val chartHeight = chartBottom - chartTop
		val yRange = yMax - yMin

		fun getX(t: ZonedDateTime): Float {
			val pos = Duration.between(startLocal, t).toMillis().toDouble()
			return (chartLeft + chartWidth * (pos.coerceIn(0.0, totalMillis) / totalMillis)).toFloat()
		}

		fun getY(alt: Double): Float {
			val frac = (alt.coerceIn(yMin, yMax) - yMin) / yRange
			return (chartBottom - (chartHeight * frac)).toFloat()
		}

		val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

		val paths = visibleBodies.map { body ->
			currentCoroutineContext().ensureActive() // Check cancellation per body
			val path = Path()
			var t = startLocal
			var first = true
			var drawing = false
			val yMinAlt = yMin
			val yAtFloor = getY(yMinAlt)

			var prevX = 0f
			var prevY = 0f
			var prevAlt = 0.0

			// Iterate time
			while (!t.isAfter(endLocal)) {
				currentCoroutineContext().ensureActive() // Check cancellation during loop
				val alt = altitude(body, t, obs)
				val x = getX(t)
				val y = getY(alt)

				if (first) {
					first = false
					if (alt >= yMinAlt) {
						path.moveTo(x, y)
						drawing = true
					} else {
						path.moveTo(x, yAtFloor)
						drawing = false
					}
				} else {
					if (alt < yMinAlt && prevAlt < yMinAlt) {
						drawing = false // Stay below
					} else if (alt >= yMinAlt && prevAlt >= yMinAlt) {
						if (!drawing) {
							path.moveTo(prevX, prevY) // Should catch up previous point clamp
							drawing = true
						}
						path.lineTo(x, y)
					} else {
						// Crossing the floor line
						val r = (yMinAlt - prevAlt) / (alt - prevAlt)
						val xi = prevX + (x - prevX) * r.toFloat()
						val yi = yAtFloor
						if (prevAlt >= yMinAlt && alt < yMinAlt) {
							// Going down
							if (!drawing) path.moveTo(prevX, prevY)
							path.lineTo(xi, yi)
							drawing = false
						} else {
							// Going up
							path.moveTo(xi, yi)
							path.lineTo(x, y)
							drawing = true
						}
					}
				}

				prevX = x
				prevY = y
				prevAlt = alt
				t = t.plusMinutes(stepMinutes)
			}

			val (rise, set) = computeRiseSet(body)
			SeriesPath(
				body,
				path,
				AstroUtils.bodyName(body),
				rise?.toLocalTime()?.format(timeFmt) ?: "—",
				set?.toLocalTime()?.format(timeFmt) ?: "—"
			)
		}

		val tw = AstroUtils.computeTwilight(startLocal, endLocal, obs, zone)
		val title = context.getString(R.string.ltr_or_rtl_combine_via_dash, context.getString(R.string.star_altitude_name), startLocal.toLocalDate())

		return Model(title, startLocal, endLocal, paths, tw, yMin, yMax)
	}

	override fun onModelReady(model: Any?) {
		cachedModel = model as? Model
	}

	@SuppressLint("DrawAllocation")
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		val m = cachedModel ?: return // Wait for async load

		// Layout recalculation in onDraw is cheap (primitives), Path generation was the heavy part
		val height = measureHeight()
		canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

		val chartLeft = leftPad + legendW + dp(12f)
		val chartTop = headerH + topAxisH
		val chartRight = width - rightPad
		val chartBottom = height - bottomPad

		canvas.drawText(m.title, width / 2f, dp(25f), labelPaint)

		drawTimeAxis(canvas, height, m.startLocal, m.endLocal, chartLeft, chartRight)
		drawDayNight(canvas, m, chartLeft, chartTop, chartRight, chartBottom)
		drawYGrid(canvas, chartLeft, chartTop, chartRight, chartBottom, m.yMin, m.yMax)
		drawZeroLine(canvas, chartLeft, chartRight, chartTop, chartBottom, m.yMin, m.yMax)

		// Optimization: Drawing pre-calculated paths is fast (hardware accelerated)
		m.seriesPaths.forEach { s ->
			val paint = seriesPaints[s.body]!!
			canvas.drawPath(s.path, paint)
		}

		drawLegendLeft(canvas, m, chartTop, chartBottom)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val h = measureHeight()
		val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
		setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
	}

	// Apparent altitude using equator->horizon with refraction.
	private fun altitude(body: Body, tLocal: ZonedDateTime, obs: Observer): Double {
		val tUtc = Time.fromMillisecondsSince1970(tLocal.toInstant().toEpochMilli())
		val eq = equator(body, tUtc, obs, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(tUtc, obs, eq.ra, eq.dec, Refraction.Normal)
		return hor.altitude
	}

	// ---------- Drawing helpers ----------

	private fun drawLegendLeft(canvas: Canvas, m: Model, chartTop: Float, chartBottom: Float) {
		val x0 = leftPad - dp(8f)
		var y = chartTop + smallPaint.textSize
		m.seriesPaths.forEach { s ->
			val sw = dp(16f)
			val mid = y - smallPaint.textSize/3
			canvas.drawLine(x0, mid, x0 + sw, mid, seriesPaints[s.body]!!)

			canvas.drawText(s.name, x0 + sw + dp(4f), y, smallPaint)
			val riseSet = "↑${s.rise}  ↓${s.set}"
			canvas.drawText(riseSet, x0 + sw + dp(4f) + nameW + dp(4f), y, smallPaint)
			y += legendLineH
			if (y > chartBottom) return
		}
	}

	private fun drawTimeAxis(canvas: Canvas, height: Int, start: ZonedDateTime, end: ZonedDateTime, left: Float, right: Float) {
		val fmt = DateTimeFormatter.ofPattern("HH:mm")
		var t = start
		repeat(5) { idx ->
			if (idx > 0) t = t.plusHours(6)
			val x = timeToX(t, start, end, left, right)
			canvas.drawLine(x, headerH, x, height - bottomPad, gridPaint)
			val label = t.toLocalTime().format(fmt)
			val tw = timePaint.measureText(label)
			canvas.drawText(label, x - tw/2, headerH + timePaint.textSize + dp(4f), timePaint)
		}
	}

	private fun drawYGrid(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, yMin: Double, yMax: Double) {
		val marks = listOf(-30.0, -15.0, 0.0, 15.0, 30.0, 45.0, 60.0, 75.0, 90.0).filter { it in yMin..yMax }
		marks.forEach { a ->
			val y = altToY(a, top, bottom, yMin, yMax)
			canvas.drawLine(left, y, right, y, gridPaint)
			val label = (if (a >= 0) "${a.toInt()}°" else "−${(-a).toInt()}°")
			val tw = axisPaint.measureText(label)
			canvas.drawText(label, left - tw - dp(8f), y + axisPaint.textSize/3, axisPaint)
		}
	}

	private fun drawZeroLine(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float, yMin: Double, yMax: Double) {
		if (0.0 in yMin..yMax) {
			val y = altToY(0.0, top, bottom, yMin, yMax)
			canvas.drawLine(left, y, right, y, zeroPaint)
		}
	}

	private fun drawDayNight(canvas: Canvas, m: Model, left: Float, top: Float, right: Float, bottom: Float) {
		canvas.drawRect(left, top, right, bottom, nightPaint)

		val tw = m.twilight
		fun xOf(t: ZonedDateTime) = timeToX(t, m.startLocal, m.endLocal, left, right)
		fun clampX(t: ZonedDateTime) = xOf(t.coerceIn(m.startLocal, m.endLocal))
		fun fillDay(x1: Float, x2: Float) { if (x2 > x1) canvas.drawRect(x1, top, x2, bottom, dayPaint) }

		val sunrise = tw.sunrise
		val sunset  = tw.sunset
		// Simplified logic for day fill, complex logic moved to AstroUtils if needed,
		// or kept here as it's cheap (just comparisons).
		// Assuming standard behavior for simplicity in optimized view
		if (sunrise != null && sunset != null) {
			if (sunrise.isAfter(sunset)) { fillDay(left, clampX(sunset)); fillDay(clampX(sunrise), right) }
			else                           fillDay(clampX(sunrise), clampX(sunset))
		} else if (sunrise != null) {
			// Sun rose but didn't set (Polar day started) or Set invalid
			fillDay(clampX(sunrise), right)
		} else if (sunset != null) {
			fillDay(left, clampX(sunset))
		}
		// Note: Full day/Full night logic for polar regions can be checked via altitude at noon if needed,
		// but omitted here for brevity as per original.

		if (showTwilightBands) {
			fun rect(a: ZonedDateTime?, b: ZonedDateTime?, paint: Paint) {
				if (a == null || b == null) return
				val x1 = timeToX(a, m.startLocal, m.endLocal, left, right, false)
				val x2 = timeToX(b, m.startLocal, m.endLocal, left, right, false)
				fun drawSeg(s: Float, e: Float) {
					val lo = max(left, s); val hi = min(right, e)
					if (hi > lo) canvas.drawRect(lo, top, hi, bottom, paint)
				}
				if (x2 >= x1) drawSeg(x1, x2) else { drawSeg(x1, right); drawSeg(left, x2) }
			}
			rect(tw.nauticalDusk, tw.astroDusk, twiAstro)
			rect(tw.civilDusk,    tw.nauticalDusk, twiNaut)
			rect(tw.sunset,       tw.civilDusk,    twiCivil)
			rect(tw.astroDawn,    tw.nauticalDawn, twiAstro)
			rect(tw.nauticalDawn, tw.civilDawn,    twiNaut)
			rect(tw.civilDawn,    tw.sunrise,      twiCivil)
		}
	}

	private fun timeToX(t: ZonedDateTime, start: ZonedDateTime, end: ZonedDateTime, left: Float, right: Float, coerce: Boolean = true): Float {
		val total = Duration.between(start, end).toMillis().toDouble()
		val pos = Duration.between(start, t).toMillis().toDouble()
		return if (coerce) (left + (right - left) * (pos.coerceIn(0.0, total) / total)).toFloat()
		else         (left + (right - left) * (pos / total)).toFloat()
	}

	private fun altToY(alt: Double, top: Float, bottom: Float, yMin: Double, yMax: Double): Float {
		val clamped = alt.coerceIn(yMin, yMax)
		val frac = (clamped - yMin) / (yMax - yMin)
		return bottom - ((bottom - top) * frac).toFloat()
	}

	private fun measureText(s: String) = labelPaint.measureText(s)

	private fun measureHeight(): Int {
		return dp(300f).toInt()
	}
}