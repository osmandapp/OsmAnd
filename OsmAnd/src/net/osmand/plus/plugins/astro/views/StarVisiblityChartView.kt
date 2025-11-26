package net.osmand.plus.plugins.astro.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import androidx.core.graphics.toColorInt
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.searchRiseSet
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.AstroUtils
import net.osmand.plus.plugins.astro.AstroUtils.Twilight
import net.osmand.plus.plugins.astro.AstroUtils.toAstroTime
import net.osmand.plus.plugins.astro.AstroUtils.toZoned
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
		val twilight: Twilight
	) : BaseModel(startLocal, endLocal)

	private var cachedModel: Model? = null

	var showTwilightBands: Boolean = true
		set(value) { field = value; triggerAsyncRebuild() }

	// ---------- Layout metrics ----------
	private val leftLabelW by lazy { measureText("Mercury") + dp(16f) }
	private val timesW by lazy { measureText("00:00") * 2 + dp(24f) }
	private val headerH = dp(40f)
	private val rowH = dp(24f)
	private val topAxisH = dp(28f)
	private val rightPad = dp(20f)
	private val bottomPad = dp(16f)

	// ---------- Paints ----------
	private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#80868B".toColorInt(); strokeWidth = dp(1f) }
	private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#D9D857".toColorInt(); style = Paint.Style.FILL }
	private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sp(18f); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textAlign = Paint.Align.CENTER }
	private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sp(18f); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
	private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }
	private val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = sp(14f) }

	private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

	override suspend fun computeModel(config: Config, width: Int, height: Int): Any {
		val zone = config.zoneId
		val startLocal = config.date.atTime(12, 0).atZone(zone)
		val endLocal = startLocal.plusDays(1)
		val obs = Observer(config.latitude, config.longitude, config.elevation)

		val rows = skyObjects.filter { it.isVisible }.map { obj ->
			currentCoroutineContext().ensureActive()
			val spans = computeVisibleSpans(obj, startLocal, endLocal, obs)
			val (rise, set) = AstroUtils.nextRiseSet(obj, startLocal, obs, startLocal, endLocal)
			Row(obj, obj.name, rise, set, spans)
		}

		val tw = AstroUtils.computeTwilight(startLocal, endLocal, obs, zone)
		val title = context.getString(R.string.ltr_or_rtl_combine_via_dash, context.getString(R.string.star_visibility_name), startLocal.toLocalDate())

		return Model(title, startLocal, endLocal, rows, tw)
	}

	override fun onModelReady(model: Any?) {
		cachedModel = model as? Model
	}

	@SuppressLint("DrawAllocation")
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val m = cachedModel ?: return
		val height = measureHeight()

		canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

		val chartLeft = leftLabelW + timesW
		val chartTop = headerH + topAxisH
		val chartRight = width - rightPad
		val chartBottom = height - bottomPad

		canvas.drawText(m.title, width / 2f, dp(25f), labelPaint)
		drawTimeAxis(canvas, height, m.startLocal, m.endLocal, chartLeft, chartRight)

		drawDayNightBands(canvas, m.twilight, m.startLocal, m.endLocal, chartLeft, chartTop, chartRight, chartBottom, showTwilightBands)

		var y = chartTop + rowH

		m.rows.forEach { row ->
			val baseY = y - rowH/2 + dp(6f)
			canvas.drawText(row.name, dp(12f), baseY, bodyPaint)

			val riseText = row.rise?.toLocalTime()?.format(timeFmt) ?: "—"
			val setText  = row.set?.toLocalTime()?.format(timeFmt) ?: "—"
			canvas.drawText("↑$riseText", leftLabelW, baseY, smallPaint)
			canvas.drawText("↓$setText", leftLabelW + measureText(riseText) + dp(12f), baseY, smallPaint)

			row.visibleSpans.forEach { span ->
				val x1 = timeToX(span.start, m.startLocal, m.endLocal, chartLeft, chartRight)
				val x2 = timeToX(span.end  , m.startLocal, m.endLocal, chartLeft, chartRight)
				canvas.drawRect(x1, y - rowH + dp(6f), x2, y - dp(5f), barPaint)
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
		val obj: SkyObject,
		val name: String,
		val rise: ZonedDateTime?,
		val set: ZonedDateTime?,
		val visibleSpans: List<Span>
	)

	private data class Span(val start: ZonedDateTime, val end: ZonedDateTime)

	private fun measureText(s: String) = labelPaint.measureText(s)
	private fun measureHeight() = dp(300f).toInt()

	// ---------- Rendering helpers ----------

	private fun drawTimeAxis(canvas: Canvas, height: Int, start: ZonedDateTime, end: ZonedDateTime, left: Float, right: Float) {
		val hours = listOf(12, 18, 0, 6, 12)
		var t = start
		hours.forEachIndexed { idx, _ ->
			t = (if (idx == 0) t else t.plusHours(6))
			val x = timeToX(t, start, end, left, right)
			canvas.drawLine(x, headerH, x, height - bottomPad, gridPaint)
			val label = t.toLocalTime().format(timeFmt)
			val tw = timePaint.measureText(label)
			canvas.drawText(label, x - tw/2, headerH + timePaint.textSize + dp(4f), timePaint)
		}
	}

	private suspend fun computeVisibleSpans(
		obj: SkyObject,
		startLocal: ZonedDateTime,
		endLocal: ZonedDateTime,
		obs: Observer
	): List<Span> {
		fun nextEvent(dir: Direction, from: ZonedDateTime): ZonedDateTime? {
			val t0 = from.toAstroTime()
			return if (obj.body != null) {
				searchRiseSet(obj.body, obs, dir, t0, +2.0)?.toZoned(config.zoneId)
			} else {
				AstroUtils.withCustomStar(obj.ra, obj.dec) { star ->
					searchRiseSet(star, obs, dir, t0, +2.0)?.toZoned(config.zoneId)
				}
			}
		}

		val startAlt = AstroUtils.altitude(obj, startLocal, obs)
		var up = startAlt > 0.0

		var cursor = startLocal
		val spans = mutableListOf<Span>()
		var openStart: ZonedDateTime? = if (up) startLocal else null

		var safety = 0
		while (cursor.isBefore(endLocal) && safety++ < 5) {
			currentCoroutineContext().ensureActive()
			val dir = if (up) Direction.Set else Direction.Rise
			val evt = nextEvent(dir, cursor) ?: break

			if (evt.isAfter(endLocal)) break
			if (up) {
				spans += Span(openStart ?: startLocal, evt)
				openStart = null
			} else {
				openStart = evt
			}
			up = !up
			cursor = evt.plusMinutes(1)
		}

		if (up) spans += Span(openStart ?: startLocal, endLocal)
		return spans
	}
}