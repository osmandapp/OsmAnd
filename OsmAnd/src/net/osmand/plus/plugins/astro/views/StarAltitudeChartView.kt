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
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.AstroUtils
import net.osmand.plus.utils.AndroidUtils
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class StarAltitudeChartView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : BaseChartView(context, attrs, defStyleAttr, defStyleRes) {

	private data class Model(
		val title: String,
		override val startLocal: ZonedDateTime,
		override val endLocal: ZonedDateTime,
		val series: List<Series>,
		val twilight: Twilight,
		val yMin: Double,
		val yMax: Double
	) : BaseModel(startLocal, endLocal)

	private var cachedModel: Model? = null
	private var config = Config()

	private data class Config(
		override val date: LocalDate = LocalDate.now(),
		override val zoneId: ZoneId = ZoneId.systemDefault(),
		override val latitude: Double = 0.0,
		override val longitude: Double = 0.0,
		override val elevation: Double = 0.0,
		val showTwilightBands: Boolean = true,
		val sampleMinutes: Int = 5,
		val yMin: Double = -30.0,
		val yMax: Double = +90.0
	) : BaseConfig(date, zoneId, latitude, longitude, elevation) {
		fun equalsTo(other: Config): Boolean {
			if (super.equalsTo(other)) return false

			if (zoneId != other.zoneId) return false
			if (showTwilightBands != other.showTwilightBands) return false
			if (sampleMinutes != other.sampleMinutes) return false
			return (abs(yMin - other.yMin) > 1.0 || abs(yMax - other.yMax) > 1.0)
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
			showTwilightBands = config.showTwilightBands,
			sampleMinutes = config.sampleMinutes,
			yMin = config.yMin,
			yMax = config.yMax
		)
		if (!config.equalsTo(this.config)) { this.config = config; cachedModel = null }
	}

	private var bodies = listOf(Body.Sun, Body.Moon, Body.Mercury, Body.Venus, Body.Mars, Body.Jupiter, Body.Saturn)

	// ---------- Layout ----------
	private val leftPad = dp(16f)
	private val legendW get() = measureText("Jupiter 00:00 00:00") + dp(16f)   // vertical legend column
	private val nameW get() = smallPaint.measureText("Mercury")
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

	// Per-body colors
	private val seriesPaints: Map<Body, Paint> = mapOf(
		Body.Sun     to linePaint("#FFD54F"),
		Body.Moon    to linePaint("#BDBDBD"),
		Body.Mercury to linePaint("#F9A825"),
		Body.Venus   to linePaint("#66BB6A"),
		Body.Mars    to linePaint("#EF5350"),
		Body.Jupiter to linePaint("#8D6E63"),
		Body.Saturn  to linePaint("#D4A373")
	)
	private fun linePaint(hex: String) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = hex.toColorInt(); strokeWidth = dp(2f); style = Paint.Style.STROKE
	}

	@SuppressLint("DrawAllocation")
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		updateConfig()
		val m = cachedModel ?: buildModel().also { cachedModel = it }

		val height = measureHeight()
		canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

		val chartLeft = leftPad + legendW + dp(12f)   // reserve a left legend column
		val chartTop = headerH + topAxisH
		val chartRight = width - rightPad
		val chartBottom = height - bottomPad

		// Title
		canvas.drawText(m.title, width / 2f, dp(25f), labelPaint)

		// Time axis + vertical grid
		drawTimeAxis(canvas, height, m.startLocal, m.endLocal, chartLeft, chartRight)

		// Day/Night bands
		drawDayNight(canvas, m, chartLeft, chartTop, chartRight, chartBottom)

		// Y grid and horizon
		drawYGrid(canvas, chartLeft, chartTop, chartRight, chartBottom, m.yMin, m.yMax)
		drawZeroLine(canvas, chartLeft, chartRight, chartTop, chartBottom, m.yMin, m.yMax)

		m.series.forEach { s ->
			val paint = seriesPaints[s.body]!!
			val path = Path()

			val yMinAlt = m.yMin
			fun xAt(t: ZonedDateTime) =
				timeToX(t, m.startLocal, m.endLocal, chartLeft, chartRight)
			val yAtFloor = altToY(yMinAlt, chartTop, chartBottom, m.yMin, m.yMax)

			var prev = s.points.first()
			var drawing = false

			for (i in 1 until s.points.size) {
				val curr = s.points[i]

				val a0 = prev.alt
				val a1 = curr.alt

				val x0 = xAt(prev.time)
				val y0 = altToY(a0, chartTop, chartBottom, m.yMin, m.yMax)
				val x1 = xAt(curr.time)
				val y1 = altToY(a1, chartTop, chartBottom, m.yMin, m.yMax)

				when {
					// both below floor -> lift pen / skip
					a0 < yMinAlt && a1 < yMinAlt -> {
						drawing = false
					}

					// both above (or on) floor -> draw straight segment
					a0 >= yMinAlt && a1 >= yMinAlt -> {
						if (!drawing) {
							path.moveTo(x0, y0)
							drawing = true
						}
						path.lineTo(x1, y1)
					}

					// crosses floor: compute intersection at yMinAlt and clip
					else -> {
						val r = (yMinAlt - a0) / (a1 - a0)   // 0..1 along the segment
						val xi = x0 + (x1 - x0) * r.toFloat()
						val yi = yAtFloor

						if (a0 >= yMinAlt && a1 < yMinAlt) {
							// going down through floor: draw to intersection, then lift
							if (!drawing) path.moveTo(x0, y0)
							path.lineTo(xi, yi)
							drawing = false
						} else {
							// coming up through floor: start at intersection
							path.moveTo(xi, yi)
							path.lineTo(x1, y1)
							drawing = true
						}
					}
				}

				prev = curr
			}

			canvas.drawPath(path, paint)
		}

		// Vertical legend with rise/set
		drawLegendLeft(canvas, m, chartTop, chartBottom)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val h = measureHeight()
		val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
		setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
	}

	// ---------- Model ----------

	private data class Point(val time: ZonedDateTime, val alt: Double)

	private data class Series(
		val body: Body,
		val name: String,
		val points: List<Point>,
		val rise: ZonedDateTime?,
		val set: ZonedDateTime?
	)

	// ---------- Build model ----------

	private fun buildModel(): Model {
		val zone = config.zoneId
		val startLocal = config.date.atTime(12, 0).atZone(zone)
		val endLocal = startLocal.plusDays(1)
		val obs = Observer(config.latitude, config.longitude, config.elevation)
		val stepMinutes = config.sampleMinutes.toLong()

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

		fun computeSeries(body: Body): Series {
			val pts = ArrayList<Point>()
			var t = startLocal
			while (!t.isAfter(endLocal)) {
				pts += Point(t, altitude(body, t, obs))
				t = t.plusMinutes(stepMinutes)
			}
			pts += Point(endLocal, altitude(body, endLocal, obs))
			val (rise, set) = computeRiseSet(body)    // same method used in visibility view. :contentReference[oaicite:1]{index=1}
			return Series(body, AstroUtils.bodyName(body), pts, rise, set)
		}

		val series = bodies.map { computeSeries(it) }
		val tw = computeTwilight(startLocal, endLocal)

		val title = context.getString(R.string.ltr_or_rtl_combine_via_dash, context.getString(R.string.star_altitude_name), startLocal.toLocalDate())
		return Model(title, startLocal, endLocal, series, tw, config.yMin, config.yMax)
	}

	// Apparent altitude using equator->horizon with refraction. :contentReference[oaicite:2]{index=2}
	private fun altitude(body: Body, tLocal: ZonedDateTime, obs: Observer): Double {
		val tUtc = Time.fromMillisecondsSince1970(tLocal.toInstant().toEpochMilli())
		val eq = equator(body, tUtc, obs, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(tUtc, obs, eq.ra, eq.dec, Refraction.Normal)
		return hor.altitude
	}

	private fun computeTwilight(startLocal: ZonedDateTime, endLocal: ZonedDateTime): Twilight {
		val obs = Observer(config.latitude, config.longitude, config.elevation)
		fun findAlt(direction: Direction, deg: Double): ZonedDateTime? {
			val t0 = Time.fromMillisecondsSince1970(startLocal.toInstant().toEpochMilli())
			val t = searchAltitude(Body.Sun, obs, direction, t0, 2.0, deg)
			return t?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(config.zoneId) }
		}
		// Prefer precise sunrise/sunset via searchRiseSet. :contentReference[oaicite:3]{index=3}
		val sr = searchRiseSet(Body.Sun, obs, Direction.Rise, Time.fromMillisecondsSince1970(startLocal.toInstant().toEpochMilli()), 2.0)
		val ss = searchRiseSet(Body.Sun, obs, Direction.Set , Time.fromMillisecondsSince1970(startLocal.toInstant().toEpochMilli()), 2.0)
		val sunrise = sr?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(config.zoneId) }
		val sunset  = ss?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(config.zoneId) }
		return Twilight(
			sunrise, sunset,
			findAlt(Direction.Rise, -6.0),  findAlt(Direction.Set, -6.0),
			findAlt(Direction.Rise, -12.0), findAlt(Direction.Set, -12.0),
			findAlt(Direction.Rise, -18.0), findAlt(Direction.Set, -18.0)
		)
	}

	// ---------- Drawing helpers ----------

	private fun drawLegendLeft(canvas: Canvas, m: Model, chartTop: Float, chartBottom: Float) {
		val x0 = leftPad - dp(8f)
		var y = chartTop + smallPaint.textSize
		val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
		m.series.forEach { s ->
			// swatch
			val sw = dp(16f)
			val mid = y - smallPaint.textSize/3
			canvas.drawLine(x0, mid, x0 + sw, mid, seriesPaints[s.body]!!)
			// text: Name + rise/set
			val rise = s.rise?.toLocalTime()?.format(timeFmt) ?: "—"
			val set  = s.set ?.toLocalTime()?.format(timeFmt) ?: "—"
			val name = s.name
			canvas.drawText(name, x0 + sw + dp(4f), y, smallPaint)
			val riseSet = "↑$rise  ↓$set"
			canvas.drawText(riseSet, x0 + sw + dp(4f) + nameW + dp(4f), y, smallPaint)
			y += legendLineH
			if (y > chartBottom) return   // stop if we ever run out of space
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
		when {
			sunrise != null && sunset != null -> {
				if (sunrise.isAfter(sunset)) { fillDay(left, clampX(sunset)); fillDay(clampX(sunrise), right) }
				else                           fillDay(clampX(sunrise), clampX(sunset))
			}
			sunrise != null -> {
				val obs = Observer(config.latitude, config.longitude, config.elevation)
				val startAlt = altitude(Body.Sun, m.startLocal, obs)
				if (startAlt > -0.833) fillDay(left, right) else fillDay(clampX(sunrise), right)
			}
			sunset != null -> {
				val obs = Observer(config.latitude, config.longitude, config.elevation)
				val startAlt = altitude(Body.Sun, m.startLocal, obs)
				if (startAlt > -0.833) fillDay(left, clampX(sunset))
			}
			else -> {
				val obs = Observer(config.latitude, config.longitude, config.elevation)
				val startAlt = altitude(Body.Sun, m.startLocal, obs)
				if (startAlt > -0.833) fillDay(left, right)
			}
		}

		if (config.showTwilightBands) {
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

	// ---------- Mapping / metrics ----------

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
		// single plot; leave a bit of height for the legend column
		//return (headerH + topAxisH + dp(180f) + bottomPad).toInt()
		return dp(300f).toInt()
	}

	private fun dp(v: Float) = AndroidUtils.dpToPxF(context, v)
	private fun sp(v: Float) = AndroidUtils.spToPxF(context, v)
}
