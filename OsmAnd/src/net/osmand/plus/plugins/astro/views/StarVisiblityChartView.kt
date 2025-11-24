package net.osmand.plus.plugins.astro.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.AstroUtils
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

class StarVisiblityChartView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : BaseChartView(context, attrs, defStyleAttr, defStyleRes) {

	private data class Model(
		val title: String,
		override val startLocal: ZonedDateTime,
		override val endLocal: ZonedDateTime,
		val rows: List<Row>,
		val twilight: AstroUtils.Twilight
	) : BaseModel(startLocal, endLocal)

	private var cachedModel: Model? = null

	// Specific config for this chart type
	var showTwilightBands: Boolean = true
		set(value) { field = value; rebuildModel(); invalidate() }
	var sampleMinutes: Int = 5
		set(value) { field = value; rebuildModel(); invalidate() }

	// ---------- Layout metrics ----------
	private val leftLabelW get() = measureText("Mercury") + dp(16f)
	private val timesW get() = measureText("00:00") * 2 + dp(24f)
	private val headerH = dp(40f)
	private val rowH = dp(24f)
	private val topAxisH = dp(28f)
	private val rightPad = dp(20f)
	private val bottomPad = dp(16f)

	// ---------- Paints ----------
	private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#E4424242".toColorInt() }
	private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#80868B".toColorInt(); strokeWidth = dp(1f)
	}
	private val nightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#60000000".toColorInt() }
	private val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#57C7F3".toColorInt() }
	private val twiAstro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC2B4C7E".toColorInt() }
	private val twiNaut = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC3C7AA6".toColorInt() }
	private val twiCivil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC5BBBF0".toColorInt() }

	private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = "#D9D857".toColorInt(); style = Paint.Style.FILL
	}

	private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE; textSize = sp(18f); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textAlign = Paint.Align.CENTER
	}
	private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE; textSize = sp(18f); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
	}
	private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.LTGRAY; textSize = sp(14f)
	}
	private val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.LTGRAY; textSize = sp(14f)
	}

	private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

	override fun rebuildModel() {
		val zone = config.zoneId
		val startLocal = config.date.atTime(12, 0).atZone(zone)
		val endLocal = startLocal.plusDays(1)
		val obs = Observer(config.latitude, config.longitude, config.elevation)

		val rows = AstroUtils.visibleBodies.map { body ->
			val spans = computeVisibleSpans(body, startLocal, endLocal, obs)
			val (rise, set) = computeRiseSet(body, startLocal, endLocal, obs)
			Row(body, AstroUtils.bodyName(body), rise, set, spans)
		}

		val tw = AstroUtils.computeTwilight(startLocal, endLocal, obs, zone)

		val title = context.getString(R.string.ltr_or_rtl_combine_via_dash, context.getString(R.string.star_visibility_name), startLocal.toLocalDate())
		cachedModel = Model(title, startLocal, endLocal, rows, tw)
	}

	@SuppressLint("DrawAllocation")
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		if (cachedModel == null) rebuildModel()
		val m = cachedModel!!

		val height = measureHeight()

		// Background
		canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

		val chartLeft = leftLabelW + timesW
		val chartTop = headerH + topAxisH
		val chartRight = width - rightPad
		val chartBottom = height - bottomPad

		// Title
		canvas.drawText(m.title, width / 2f, dp(25f), labelPaint)

		// Time axis labels & vertical grid every 6 hours
		drawTimeAxis(canvas, height, m.startLocal, m.endLocal, chartLeft, chartRight)

		// Twilight/day bands
		drawDayNight(canvas, m, chartLeft, chartTop, chartRight, chartBottom)

		// Rows
		var y = chartTop + rowH

		m.rows.forEach { row ->
			// labels: body name + rise/set
			val baseY = y - rowH/2 + dp(6f)
			canvas.drawText(row.name, dp(12f), baseY, bodyPaint)

			val riseText = row.rise?.toLocalTime()?.format(timeFmt) ?: "—"
			val setText  = row.set?.toLocalTime()?.format(timeFmt) ?: "—"
			canvas.drawText("↑$riseText", leftLabelW, baseY, smallPaint)
			canvas.drawText("↓$setText", leftLabelW + measureText(riseText) + dp(12f), baseY, smallPaint)

			// visibility bars
			row.visibleSpans.forEach { span ->
				val x1 = timeToX(span.start, m.startLocal, m.endLocal, chartLeft, chartRight)
				val x2 = timeToX(span.end  , m.startLocal, m.endLocal, chartLeft, chartRight)
				val rect = RectF(x1, y - rowH + dp(6f), x2, y - dp(5f))
				canvas.drawRect(rect, barPaint)
			}

			y += rowH
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val h = measureHeight()
		val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
		setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
	}

	// ---------- Internal model ----------

	private data class Row(
		val body: Body,
		val name: String,
		val rise: ZonedDateTime?,     // next rise within window
		val set: ZonedDateTime?,      // next set within window
		val visibleSpans: List<Span>  // spans within [x0..x1] where alt>0
	)

	private data class Span(val start: ZonedDateTime, val end: ZonedDateTime)

	private fun measureText(s: String) = labelPaint.measureText(s)

	private fun measureHeight(): Int {
		return dp(300f).toInt()
	}

	// ---------- Rendering helpers ----------

	private fun drawTimeAxis(canvas: Canvas, height: Int, start: ZonedDateTime, end: ZonedDateTime, left: Float, right: Float) {
		val hours = listOf(12, 18, 0, 6, 12)
		val startNoon = start
		var t = startNoon
		hours.forEachIndexed { idx, h ->
			t = (if (idx == 0) t else t.plusHours(6))
			val x = timeToX(t, start, end, left, right)
			canvas.drawLine(x, headerH, x, height - bottomPad, gridPaint)
			val label = t.toLocalTime().format(timeFmt)
			val tw = timePaint.measureText(label)
			canvas.drawText(label, x - tw/2, headerH + timePaint.textSize + dp(4f), timePaint)
		}
	}

	private fun drawDayNight(
		canvas: Canvas, m: Model,
		left: Float, top: Float, right: Float, bottom: Float
	) {
		// Base: darkest night
		canvas.drawRect(left, top, right, bottom, nightPaint)

		val tw = m.twilight

		fun xOf(t: ZonedDateTime) =
			timeToX(t, m.startLocal, m.endLocal, left, right)

		fun clampX(t: ZonedDateTime) = xOf(
			t.coerceIn(m.startLocal, m.endLocal)
		)

		val sunrise = tw.sunrise
		val sunset  = tw.sunset

		fun fillDay(x1: Float, x2: Float) {
			if (x2 > x1) canvas.drawRect(x1, top, x2, bottom, dayPaint)
		}

		when {
			sunrise != null && sunset != null -> {
				if (sunrise.isAfter(sunset)) {
					fillDay(left, clampX(sunset))
					fillDay(clampX(sunrise), right)
				} else {
					fillDay(clampX(sunrise), clampX(sunset))
				}
			}
			sunrise != null -> {
				val obs = Observer(config.latitude, config.longitude, config.elevation)
				val startAlt = altitude(Body.Sun, m.startLocal, obs)
				if (startAlt > -0.833) {
					fillDay(left, right)
				} else {
					fillDay(clampX(sunrise), right)
				}
			}
			sunset != null -> {
				val obs = Observer(config.latitude, config.longitude, config.elevation)
				val startAlt = altitude(Body.Sun, m.startLocal, obs)
				if (startAlt > -0.833) {
					fillDay(left, clampX(sunset))
				}
			}
			else -> {
				val obs = Observer(config.latitude, config.longitude, config.elevation)
				val startAlt = altitude(Body.Sun, m.startLocal, obs)
				if (startAlt > -0.833) fillDay(left, right)
			}
		}

		if (showTwilightBands) {
			fun rect(a: ZonedDateTime?, b: ZonedDateTime?, paint: Paint) {
				if (a == null || b == null) return

				val x1 = timeToX(a, m.startLocal, m.endLocal, left, right, false)
				val x2 = timeToX(b, m.startLocal, m.endLocal, left, right, false)

				fun drawSeg(s: Float, e: Float) {
					val lo = max(left, s)
					val hi = min(right, e)
					if (hi > lo) canvas.drawRect(lo, top, hi, bottom, paint)
				}

				if (x2 >= x1) {
					drawSeg(x1, x2)
				} else {
					drawSeg(x1, right)
					drawSeg(left, x2)
				}
			}

			rect(tw.nauticalDusk, tw.astroDusk, twiAstro)
			rect(tw.civilDusk,    tw.nauticalDusk, twiNaut)
			rect(tw.sunset,       tw.civilDusk,    twiCivil)
			rect(tw.astroDawn,    tw.nauticalDawn, twiAstro)
			rect(tw.nauticalDawn, tw.civilDawn,    twiNaut)
			rect(tw.civilDawn,    tw.sunrise,      twiCivil)
		}
	}

	private fun timeToX(
		t: ZonedDateTime,
		start: ZonedDateTime, end: ZonedDateTime,
		left: Float, right: Float,
		coerce: Boolean = true
	): Float {
		val total = Duration.between(start, end).toMillis().toDouble()
		val pos = Duration.between(start, t).toMillis().toDouble()
		return if (coerce) {
			(left + (right - left) * (pos.coerceIn(0.0, total) / total)).toFloat()
		} else {
			(left + (right - left) * (pos / total)).toFloat()
		}
	}

	private fun computeVisibleSpans(
		body: Body,
		startLocal: ZonedDateTime,
		endLocal: ZonedDateTime,
		obs: Observer
	): List<Span> {
		fun Time.toZdt() =
			Instant.ofEpochMilli(this.toMillisecondsSince1970()).atZone(config.zoneId)

		fun nextEvent(dir: Direction, from: ZonedDateTime): ZonedDateTime? {
			val t0 = Time.fromMillisecondsSince1970(from.toInstant().toEpochMilli())
			return searchRiseSet(body, obs, dir, t0, +2.0)?.toZdt()
		}

		val startAlt = altitude(body, startLocal, obs)
		var up = startAlt > 0.0

		var cursor = startLocal
		val spans = mutableListOf<Span>()
		var openStart: ZonedDateTime? = if (up) startLocal else null

		while (cursor.isBefore(endLocal)) {
			val dir = if (up) Direction.Set else Direction.Rise
			val evt = nextEvent(dir, cursor) ?: break
			if (evt.isAfter(endLocal)) {
				break
			}
			if (up) {
				spans += Span(openStart ?: startLocal, evt)
				openStart = null
			} else {
				openStart = evt
			}
			up = !up
			cursor = evt.plusMinutes(1)
		}
		if (up) {
			spans += Span(openStart ?: startLocal, endLocal)
		}
		return spans
	}

	private fun altitude(body: Body, tLocal: ZonedDateTime, obs: Observer): Double {
		val tUtc = Time.fromMillisecondsSince1970(tLocal.toInstant().toEpochMilli())
		val eq = equator(body, tUtc, obs, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(tUtc, obs, eq.ra, eq.dec, Refraction.Normal)
		return hor.altitude
	}

	private fun computeRiseSet(
		body: Body,
		startLocal: ZonedDateTime,
		endLocal: ZonedDateTime,
		obs: Observer
	): Pair<ZonedDateTime?, ZonedDateTime?> {
		val searchStartUtc = Time.fromMillisecondsSince1970(startLocal.toInstant().toEpochMilli())
		val limitDays = 2.0

		val nextRise = searchRiseSet(body, obs, Direction.Rise, searchStartUtc, +limitDays)
		val nextSet  = searchRiseSet(body, obs, Direction.Set , searchStartUtc, +limitDays)

		fun Time?.toZoned(): ZonedDateTime? =
			this?.let { Instant.ofEpochMilli(it.toMillisecondsSince1970()).atZone(config.zoneId) }

		val riseZ = nextRise.toZoned()?.takeIf { !it.isBefore(startLocal) && !it.isAfter(endLocal) }
		val setZ  = nextSet .toZoned()?.takeIf { !it.isBefore(startLocal) && !it.isAfter(endLocal) }
		return riseZ to setZ
	}
}