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
import io.github.cosinekitty.astronomy.Observer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.SkyObject
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.plugins.astro.utils.AstroUtils.Twilight
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class StarAltitudeChartView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : StarChartView(context, attrs, defStyleAttr, defStyleRes) {

	private data class Model(
		val title: String,
		override val startLocal: ZonedDateTime,
		override val endLocal: ZonedDateTime,
		val seriesPaths: List<SeriesPath>,
		val twilight: Twilight,
		val yMin: Double,
		val yMax: Double
	) : BaseModel(startLocal, endLocal)

	private data class SeriesPath(
		val obj: SkyObject,
		val path: Path,
		val name: String,
		val rise: String,
		val set: String,
		val color: Int
	)

	private var cachedModel: Model? = null

	var showTwilightBands: Boolean = true
		set(value) { field = value; invalidate() }
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
	private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#80868B".toColorInt(); strokeWidth = dp(1f) }
	private val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = dp(1.2f); pathEffect = DashPathEffect(floatArrayOf(10f,10f), 0f) }
	private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sp(18f); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textAlign = Paint.Align.CENTER }
	private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }
	private val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }
	private val axisPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }
	// Paint for drawing paths is configured dynamically per object

	override suspend fun computeModel(config: Config, width: Int, height: Int): Any {
		val zone = config.zoneId
		val startLocal = config.date.atTime(12, 0).atZone(zone)
		val endLocal = startLocal.plusDays(1)
		val obs = Observer(config.latitude, config.longitude, config.elevation)
		val stepMinutes = sampleMinutes.toLong()

		val chartLeft = leftPad + legendW + dp(12f)
		val chartTop = headerH + topAxisH
		val chartRight = width - rightPad
		val chartBottom = height - bottomPad

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

		val paths = skyObjects.filter { it.isFavorite }.map { obj ->
			currentCoroutineContext().ensureActive()
			val path = Path()
			var t = startLocal
			var first = true
			var drawing = false
			val yMinAlt = yMin
			val yAtFloor = getY(yMinAlt)

			var prevX = 0f
			var prevY = 0f
			var prevAlt = 0.0

			while (!t.isAfter(endLocal)) {
				currentCoroutineContext().ensureActive()

				val alt = AstroUtils.altitude(obj, t, obs)
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
						drawing = false
					} else if (alt >= yMinAlt && prevAlt >= yMinAlt) {
						if (!drawing) {
							path.moveTo(prevX, prevY)
							drawing = true
						}
						path.lineTo(x, y)
					} else {
						val r = (yMinAlt - prevAlt) / (alt - prevAlt)
						val xi = prevX + (x - prevX) * r.toFloat()
						val yi = yAtFloor
						if (prevAlt >= yMinAlt && alt < yMinAlt) {
							if (!drawing) path.moveTo(prevX, prevY)
							path.lineTo(xi, yi)
							drawing = false
						} else {
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

			val (rise, set) = AstroUtils.nextRiseSet(obj, startLocal, obs, startLocal, endLocal)
			SeriesPath(
				obj,
				path,
				obj.name,
				rise?.toLocalTime()?.format(timeFmt) ?: "—",
				set?.toLocalTime()?.format(timeFmt) ?: "—",
				obj.color
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
		val m = cachedModel ?: return

		val height = measureHeight()
		//canvas.drawColor(bgPaint.color)

		val chartLeft = leftPad + legendW + dp(12f)
		val chartTop = headerH + topAxisH
		val chartRight = width - rightPad
		val chartBottom = height - bottomPad

		canvas.drawText(m.title, width / 2f, dp(31f), labelPaint)

		drawTimeAxis(canvas, height, m.startLocal, m.endLocal, chartLeft, chartRight)

		drawDayNightBands(canvas, m.twilight, m.startLocal, m.endLocal, chartLeft, chartTop, chartRight, chartBottom, showTwilightBands)

		drawYGrid(canvas, chartLeft, chartTop, chartRight, chartBottom, m.yMin, m.yMax)
		drawZeroLine(canvas, chartLeft, chartRight, chartTop, chartBottom, m.yMin, m.yMax)

		val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(2f); style = Paint.Style.STROKE }
		m.seriesPaths.forEach { s ->
			pathPaint.color = s.color
			canvas.drawPath(s.path, pathPaint)
		}

		drawLegendLeft(canvas, m, chartTop, chartBottom)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val h = measureHeight()
		val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
		setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
	}

	// ---------- Drawing helpers ----------

	private fun drawLegendLeft(canvas: Canvas, m: Model, chartTop: Float, chartBottom: Float) {
		val x0 = leftPad - dp(8f)
		var y = chartTop + smallPaint.textSize
		val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(2f); style = Paint.Style.STROKE }

		m.seriesPaths.forEach { s ->
			val sw = dp(16f)
			val mid = y - smallPaint.textSize/3
			linePaint.color = s.color
			canvas.drawLine(x0, mid, x0 + sw, mid, linePaint)

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

	private fun altToY(alt: Double, top: Float, bottom: Float, yMin: Double, yMax: Double): Float {
		val clamped = alt.coerceIn(yMin, yMax)
		val frac = (clamped - yMin) / (yMax - yMin)
		return bottom - ((bottom - top) * frac).toFloat()
	}

	private fun measureText(s: String) = labelPaint.measureText(s)
	private fun measureHeight() = dp(300f).toInt()
}