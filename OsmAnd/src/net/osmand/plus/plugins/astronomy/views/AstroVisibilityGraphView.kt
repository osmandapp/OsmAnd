package net.osmand.plus.plugins.astronomy.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import io.github.cosinekitty.astronomy.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroChartColorPalette
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroChartMath
import net.osmand.plus.utils.AndroidUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.graphics.withClip
import androidx.core.graphics.createBitmap
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroChartMath.VISIBILITY_SAMPLE_COUNT

class AstroVisibilityGraphView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private data class Model(
		val startMillis: Long,
		val endMillis: Long,
		val zoneId: ZoneId,
		val objectAltitudes: DoubleArray,
		val objectAzimuths: DoubleArray,
		val sunAltitudes: DoubleArray
	) {
		val size: Int
			get() = objectAltitudes.size

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Model

			if (startMillis != other.startMillis) return false
			if (endMillis != other.endMillis) return false
			if (zoneId != other.zoneId) return false
			if (!objectAltitudes.contentEquals(other.objectAltitudes)) return false
			if (!objectAzimuths.contentEquals(other.objectAzimuths)) return false
			if (!sunAltitudes.contentEquals(other.sunAltitudes)) return false
			if (size != other.size) return false

			return true
		}

		override fun hashCode(): Int {
			var result = startMillis.hashCode()
			result = 31 * result + endMillis.hashCode()
			result = 31 * result + zoneId.hashCode()
			result = 31 * result + objectAltitudes.contentHashCode()
			result = 31 * result + objectAzimuths.contentHashCode()
			result = 31 * result + sunAltitudes.contentHashCode()
			result = 31 * result + size
			return result
		}
	}

	private data class PlotArea(
		val left: Float,
		val top: Float,
		val right: Float,
		val bottom: Float
	) {
		val width: Float
			get() = right - left
		val height: Float
			get() = bottom - top
	}

	private enum class ZeroCrossingType { SUNRISE, SUNSET }

	private data class ZeroCrossing(
		val x: Float,
		val type: ZeroCrossingType
	)

	private var viewScope = createScope()
	private var computeJob: Job? = null

	private var skyObject: SkyObject? = null
	private var observer: Observer? = null
	private var date: LocalDate = LocalDate.now()
	private var zoneId: ZoneId = ZoneId.systemDefault()
	private var model: Model? = null
	private var palette: AstroChartColorPalette? = null
	private var staticLayerInvalidate = true
	private var staticLayerBitmap: Bitmap? = null
	private var staticLayerCanvas: Canvas? = null

	private var isTouchTracking = false
	private var cursorVisible = false
	private var cursorX = 0f

	private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
	private val sunriseDrawable: Drawable? by lazy {
		ContextCompat.getDrawable(context, R.drawable.ic_action_sunrise_12)
	}
	private val sunsetDrawable: Drawable? by lazy {
		ContextCompat.getDrawable(context, R.drawable.ic_action_sunset_12)
	}

	private val trajectoryPath = Path()
	private val fillPath = Path()
	private val tempRect = RectF()

	private val chartBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

	private val dashedGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		strokeWidth = dp(Y_GRID_STROKE_DP)
		pathEffect = DashPathEffect(floatArrayOf(dp(Y_GRID_DASH_DP), dp(Y_GRID_GAP_DP)), 0f)
	}
	private val zeroGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		strokeWidth = dp(Y_GRID_STROKE_DP)
		pathEffect = DashPathEffect(floatArrayOf(dp(Y_GRID_DASH_DP), dp(Y_GRID_GAP_DP)), 0f)
	}
	private val yAxisLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		textSize = sp(Y_LABEL_TEXT_SP)
		textAlign = Paint.Align.RIGHT
	}
	private val xAxisLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		textSize = sp(X_LABEL_TEXT_SP)
		textAlign = Paint.Align.CENTER
	}
	private val xTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		strokeWidth = dp(X_TICK_STROKE_DP)
	}
	private val cursorLineCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		strokeWidth = dp(CURSOR_LINE_STROKE_DP)
	}
	private val cursorLineSidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		strokeWidth = dp(CURSOR_LINE_STROKE_DP)
	}
	private val cursorDotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val cursorDotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = dp(CURSOR_DOT_STROKE_DP)
	}
	private val markerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = dp(MARKER_BORDER_STROKE_DP)
	}
	private val markerSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		strokeWidth = dp(MARKER_SEPARATOR_STROKE_DP)
	}
	private val markerTimePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		textSize = sp(MARKER_TEXT_SP)
		textAlign = Paint.Align.LEFT
	}
	private val markerAltitudePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		textSize = sp(MARKER_TEXT_SP)
		textAlign = Paint.Align.LEFT
	}
	private val markerAzimuthPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
		textSize = sp(MARKER_TEXT_SP)
		textAlign = Paint.Align.LEFT
	}

	init {
		refreshThemeResources()
	}

	fun submitObject(
		objectToRender: SkyObject?,
		observer: Observer?,
		date: LocalDate,
		zoneId: ZoneId
	) {
		skyObject = objectToRender
		this.observer = observer
		this.date = date
		this.zoneId = zoneId
		triggerAsyncRebuild()
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		invalidateStaticLayer()
		if (w > 0 && h > 0) {
			triggerAsyncRebuild()
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		refreshThemeResources()
		triggerAsyncRebuild()
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		if (model == null && width > 0 && height > 0 && skyObject != null && observer != null) {
			triggerAsyncRebuild()
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		computeJob?.cancel()
		computeJob = null
		clearStaticLayer()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val chartArea = getPlotArea() ?: return
		val currentModel = model ?: return
		if (currentModel.size < 2) return
		val localPalette = palette ?: return

		ensureStaticLayer(chartArea, currentModel, localPalette)
		staticLayerBitmap?.let { bitmap ->
			canvas.drawBitmap(bitmap, 0f, 0f, null)
		}

		if (cursorVisible) {
			drawCursor(canvas, chartArea, currentModel)
		}
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		val chartArea = getPlotArea() ?: return false
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				if (!isInPlotArea(event.x, event.y, chartArea)) {
					return false
				}
				parent?.requestDisallowInterceptTouchEvent(true)
				isTouchTracking = true
				updateCursor(event.x, chartArea)
				return true
			}

			MotionEvent.ACTION_MOVE -> {
				if (!isTouchTracking) {
					return false
				}
				parent?.requestDisallowInterceptTouchEvent(true)
				updateCursor(event.x, chartArea)
				return true
			}

			MotionEvent.ACTION_UP -> {
				if (!isTouchTracking) {
					return false
				}
				isTouchTracking = false
				updateCursor(event.x, chartArea)
				parent?.requestDisallowInterceptTouchEvent(false)
				return true
			}

			MotionEvent.ACTION_CANCEL -> {
				isTouchTracking = false
				cursorVisible = false
				invalidate()
				parent?.requestDisallowInterceptTouchEvent(false)
				return true
			}
		}
		return super.onTouchEvent(event)
	}

	private fun triggerAsyncRebuild() {
		val localObject = skyObject
		val localObserver = observer
		if (width == 0 || height == 0 || localObject == null || localObserver == null) {
			model = null
			invalidateStaticLayer()
			invalidate()
			return
		}
		val sampleCount = VISIBILITY_SAMPLE_COUNT

		computeJob?.cancel()
		ensureScope()
		computeJob = viewScope.launch {
			val result = withContext(Dispatchers.Default) {
				computeModel(localObject, localObserver, date, zoneId, sampleCount)
			}
			if (isActive) {
				if (model != result) {
					model = result
					invalidateStaticLayer()
				}
				invalidate()
			}
		}
	}

	private fun computeModel(
		objectToRender: SkyObject,
		observer: Observer,
		date: LocalDate,
		zoneId: ZoneId,
		sampleCount: Int
	): Model {
		val startLocal = date.atTime(12, 0).atZone(zoneId)
		val endLocal = startLocal.plusDays(1)
		val samples = AstroChartMath.computeDaySamples(
			objectToRender = objectToRender,
			observer = observer,
			startLocal = startLocal,
			endLocal = endLocal,
			sampleCount = sampleCount,
			includeAzimuth = true
		)

		return Model(
			startMillis = samples.startMillis,
			endMillis = samples.endMillis,
			zoneId = zoneId,
			objectAltitudes = samples.objectAltitudes,
			objectAzimuths = samples.objectAzimuths ?: DoubleArray(samples.objectAltitudes.size),
			sunAltitudes = samples.sunAltitudes
		)
	}

	private fun ensureStaticLayer(
		area: PlotArea,
		model: Model,
		palette: AstroChartColorPalette
	) {
		if (!staticLayerInvalidate && staticLayerBitmap != null) {
			return
		}
		val bitmap = obtainStaticBitmap(width, height)
		bitmap.eraseColor(Color.TRANSPARENT)
		val staticCanvas = staticLayerCanvas ?: Canvas(bitmap).also { staticLayerCanvas = it }

		drawDynamicBackground(staticCanvas, area, model, palette)
		val trajectory = buildTrajectoryPath(area, model)
		if (trajectory != null) {
			drawObjectFill(staticCanvas, area, trajectory, palette)
		}
		drawYAxisGridAndLabels(staticCanvas, area)
		drawXAxisTicksAndLabels(staticCanvas, area, model)
		drawSunriseSunsetIcons(staticCanvas, area, model)
		staticLayerInvalidate = false
	}

	private fun obtainStaticBitmap(width: Int, height: Int): Bitmap {
		val existing = staticLayerBitmap
		if (existing != null && !existing.isRecycled && existing.width == width && existing.height == height) {
			return existing
		}
		existing?.recycle()
		val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
		staticLayerBitmap = bitmap
		staticLayerCanvas = Canvas(bitmap)
		return bitmap
	}

	private fun invalidateStaticLayer() {
		staticLayerInvalidate = true
	}

	private fun clearStaticLayer() {
		staticLayerInvalidate = true
		staticLayerCanvas = null
		staticLayerBitmap?.recycle()
		staticLayerBitmap = null
	}

	private fun drawDynamicBackground(
		canvas: Canvas,
		area: PlotArea,
		model: Model,
		palette: AstroChartColorPalette
	) {
		val drawStepPx = BG_STRIPE_STEP_DEFAULT_PX
		var x = area.left
		while (x < area.right) {
			val nextX = min(area.right, x + drawStepPx)
			val centerFraction = (((x + nextX) * 0.5f - area.left) / area.width).coerceIn(0f, 1f)
			val sunAltitude = interpolate(model.sunAltitudes, centerFraction)
			chartBgPaint.color = palette.colorForSunAltitude(sunAltitude)
			canvas.drawRect(x, area.top, nextX, area.bottom, chartBgPaint)
			x = nextX
		}
	}

	private fun drawYAxisGridAndLabels(canvas: Canvas, area: PlotArea) {
		val axisEndX = area.right + dp(RIGHT_AXIS_OUTSET_DP)
		var altitudeMark = -30
		while (altitudeMark <= 90) {
			val y = altitudeToY(altitudeMark.toDouble(), area)
			val linePaint = if (altitudeMark == 0) zeroGridPaint else dashedGridPaint
			canvas.drawLine(area.left, y, axisEndX, y, linePaint)
			val baseline = y + dp(Y_LABEL_TO_LINE_GAP_DP) + yAxisLabelPaint.textSize
			canvas.drawText("$altitudeMark°", axisEndX, baseline, yAxisLabelPaint)
			altitudeMark += 15
		}
	}

	private fun drawXAxisTicksAndLabels(canvas: Canvas, area: PlotArea, model: Model) {
		val tickBottom = area.bottom
		val tickTop = area.bottom - dp(X_TICK_HEIGHT_DP)
		val fm = xAxisLabelPaint.fontMetrics
		val labelY = area.bottom + dp(X_LABEL_TO_GRAPH_GAP_DP) - fm.ascent
		val leftClip = dp(LABEL_EDGE_MIN_DP)
		val rightClip = width - dp(LABEL_EDGE_MIN_DP)
		for (step in 0..6) {
			val millis = model.startMillis + step * FOUR_HOURS_MILLIS
			val x = timeToX(millis, area, model)
			canvas.drawLine(x, tickTop, x, tickBottom, xTickPaint)
			val label = Instant.ofEpochMilli(millis)
				.atZone(model.zoneId)
				.toLocalTime()
				.format(timeFormatter)
			val half = xAxisLabelPaint.measureText(label) / 2f
			val clampedX = x.coerceIn(leftClip + half, rightClip - half)
			canvas.drawText(label, clampedX, labelY, xAxisLabelPaint)
		}
	}

	private fun drawSunriseSunsetIcons(canvas: Canvas, area: PlotArea, model: Model) {
		val crossings = findZeroCrossings(model, area)
		if (crossings.isEmpty()) {
			return
		}
		val y = altitudeToY(0.0, area) - dp(SUN_ICON_RAISE_DP)
		val iconSize = dp(SUN_ICON_SIZE_DP).roundToInt()
		val half = iconSize / 2
		crossings.forEach { crossing ->
			val drawable = when (crossing.type) {
				ZeroCrossingType.SUNRISE -> sunriseDrawable
				ZeroCrossingType.SUNSET -> sunsetDrawable
			} ?: return@forEach
			val left = (crossing.x - half).roundToInt()
			val top = (y - half).roundToInt()
			drawable.setBounds(left, top, left + iconSize, top + iconSize)
			drawable.draw(canvas)
		}
	}

	private fun findZeroCrossings(model: Model, area: PlotArea): List<ZeroCrossing> {
		val result = ArrayList<ZeroCrossing>(2)
		val altitudes = model.objectAltitudes
		for (i in 1 until altitudes.size) {
			val prev = altitudes[i - 1]
			val current = altitudes[i]
			if ((prev > 0.0 && current > 0.0) || (prev < 0.0 && current < 0.0)) {
				continue
			}
			val delta = current - prev
			if (delta == 0.0) {
				continue
			}
			val t = ((0.0 - prev) / delta).coerceIn(0.0, 1.0)
			val sampleIndex = (i - 1) + t
			val fraction = (sampleIndex / (altitudes.size - 1).toDouble()).toFloat()
			val x = area.left + area.width * fraction
			val type = if (delta > 0.0) ZeroCrossingType.SUNRISE else ZeroCrossingType.SUNSET
			result.add(ZeroCrossing(x, type))
		}
		return result
	}

	private fun drawObjectFill(
		canvas: Canvas,
		area: PlotArea,
		trajectory: Path,
		palette: AstroChartColorPalette
	) {
		val path = buildFillPath(area, trajectory) ?: return
		val fillGradient = buildObjectFillGradient(area, palette)

		canvas.withClip(path) {
			fillPaint.shader = fillGradient
			drawRect(area.left, area.top, area.right, area.bottom, fillPaint)
			fillPaint.shader = null
		}
	}

	private fun buildObjectFillGradient(
		area: PlotArea,
		palette: AstroChartColorPalette
	): LinearGradient {
		val transitionHalf = AstroChartColorPalette.OBJECT_GRADIENT_TRANSITION_DEGREES / 2.0
		val yRange = area.height.coerceAtLeast(1f)

		fun positionForAltitude(altitude: Double): Float {
			return ((altitudeToY(altitude, area) - area.top) / yRange).coerceIn(0f, 1f)
		}

		val pos45Upper = positionForAltitude(45.0 + transitionHalf)
		val pos45Lower = positionForAltitude(45.0 - transitionHalf)
		val pos15Upper = positionForAltitude(15.0 + transitionHalf)
		val pos15Lower = positionForAltitude(15.0 - transitionHalf)
		val pos0Upper = positionForAltitude(0.0 + transitionHalf)
		val pos0Lower = positionForAltitude(0.0 - transitionHalf)

		return LinearGradient(
			0f,
			area.top,
			0f,
			area.bottom,
			intArrayOf(
				palette.fillGt45,
				palette.fillGt45,
				palette.fill15To45,
				palette.fill15To45,
				palette.fill0To15,
				palette.fill0To15,
				palette.fillLt0,
				palette.fillLt0
			),
			floatArrayOf(
				0f,
				pos45Upper,
				pos45Lower,
				pos15Upper,
				pos15Lower,
				pos0Upper,
				pos0Lower,
				1f
			),
			Shader.TileMode.CLAMP
		)
	}

	private fun drawCursor(canvas: Canvas, area: PlotArea, model: Model) {
		val x = cursorX.coerceIn(area.left, area.right)
		val fraction = ((x - area.left) / area.width).coerceIn(0f, 1f)
		val altitude = interpolate(model.objectAltitudes, fraction)
		val azimuth = interpolateAzimuth(model.objectAzimuths, fraction)
		val millis = lerp(
			model.startMillis.toDouble(),
			model.endMillis.toDouble(),
			fraction.toDouble()
		).toLong()
		val y = altitudeToY(altitude, area)
		val lineTop = area.top - dp(MARKER_TO_GRAPH_GAP_DP)

		val sideOffset = dp(CURSOR_SIDE_OFFSET_DP)
		canvas.drawLine(x - sideOffset, lineTop, x - sideOffset, area.bottom, cursorLineSidePaint)
		canvas.drawLine(x + sideOffset, lineTop, x + sideOffset, area.bottom, cursorLineSidePaint)
		canvas.drawLine(x, lineTop, x, area.bottom, cursorLineCenterPaint)

		val markerRadius = dp(CURSOR_DOT_RADIUS_DP)
		canvas.drawCircle(x, y, markerRadius, cursorDotFillPaint)
		canvas.drawCircle(x, y, markerRadius, cursorDotStrokePaint)

		val timeLabel = Instant.ofEpochMilli(millis)
			.atZone(model.zoneId)
			.toLocalTime()
			.format(timeFormatter)
		val altitudeLabel =
			"${altitude.roundToInt()}° ${context.getString(R.string.astro_alt_short)}"
		val azimuthLabel = "${azimuth.roundToInt()}° ${context.getString(R.string.astro_az_short)}"
		drawCursorMarker(canvas, area, x, timeLabel, altitudeLabel, azimuthLabel, lineTop)
	}

	private fun drawCursorMarker(
		canvas: Canvas,
		area: PlotArea,
		anchorX: Float,
		timeLabel: String,
		altitudeLabel: String,
		azimuthLabel: String,
		lineTop: Float
	) {
		val horizontalPadding = dp(MARKER_H_PADDING_DP)
		val separatorPadding = dp(MARKER_SEPARATOR_INSET_DP)
		val corner = dp(MARKER_CORNER_DP)

		val timeWidth = markerTimePaint.measureText(timeLabel)
		val altitudeWidth = markerAltitudePaint.measureText(altitudeLabel)
		val azimuthWidth = markerAzimuthPaint.measureText(azimuthLabel)
		val section1 = timeWidth + horizontalPadding * 2f
		val section2 = altitudeWidth + horizontalPadding * 2f
		val section3 = azimuthWidth + horizontalPadding * 2f
		val markerHeight = markerHeightPx()
		val totalWidth = section1 + section2 + section3

		var left = anchorX - totalWidth / 2f
		val minLeft = area.left
		val maxLeft = max(area.left, area.right - totalWidth)
		left = left.coerceIn(minLeft, maxLeft)
		val right = left + totalWidth
		val bottom = lineTop
		val top = bottom - markerHeight
		val fm = markerTimePaint.fontMetrics
		val baseline = (top + bottom) / 2f - (fm.ascent + fm.descent) / 2f

		tempRect.set(left, top, right, bottom)
		canvas.drawRoundRect(tempRect, corner, corner, markerBorderPaint)

		val separator1X = left + section1
		val separator2X = separator1X + section2
		val separatorTop = top + separatorPadding
		val separatorBottom = bottom - separatorPadding
		canvas.drawLine(
			separator1X,
			separatorTop,
			separator1X,
			separatorBottom,
			markerSeparatorPaint
		)
		canvas.drawLine(
			separator2X,
			separatorTop,
			separator2X,
			separatorBottom,
			markerSeparatorPaint
		)

		var cursorX = left + horizontalPadding
		canvas.drawText(timeLabel, cursorX, baseline, markerTimePaint)
		cursorX = separator1X + horizontalPadding
		canvas.drawText(altitudeLabel, cursorX, baseline, markerAltitudePaint)
		cursorX = separator2X + horizontalPadding
		canvas.drawText(azimuthLabel, cursorX, baseline, markerAzimuthPaint)
	}

	private fun buildTrajectoryPath(area: PlotArea, model: Model): Path? {
		if (model.size < 2) return null
		val renderSampleCount = min(model.size, max(2, area.width.roundToInt()))
		if (renderSampleCount < 2) return null

		trajectoryPath.reset()
		for (i in 0 until renderSampleCount) {
			val fraction = i.toFloat() / (renderSampleCount - 1).toFloat()
			val x = area.left + fraction * area.width
			val y = altitudeToY(interpolate(model.objectAltitudes, fraction), area)
			if (i == 0) {
				trajectoryPath.moveTo(x, y)
			} else {
				trajectoryPath.lineTo(x, y)
			}
		}
		return trajectoryPath
	}

	private fun buildFillPath(area: PlotArea, trajectory: Path): Path? {
		if (trajectory.isEmpty) {
			return null
		}
		fillPath.reset()
		fillPath.addPath(trajectory)
		fillPath.lineTo(area.right, area.bottom)
		fillPath.lineTo(area.left, area.bottom)
		fillPath.close()
		return fillPath
	}

	private fun getPlotArea(): PlotArea? {
		val left = dp(OUTER_LEFT_PADDING_DP)
		val axisOutset = dp(RIGHT_AXIS_OUTSET_DP)
		val right = width - axisOutset - dp(OUTER_RIGHT_PADDING_DP)
		val top = dp(OUTER_TOP_PADDING_DP) + markerHeightPx() + dp(MARKER_TO_GRAPH_GAP_DP)
		val bottom = height - xAxisReservedHeightPx() - dp(OUTER_BOTTOM_PADDING_DP)
		if (right <= left || bottom <= top) {
			return null
		}
		return PlotArea(left, top, right, bottom)
	}

	private fun markerHeightPx(): Float {
		return dp(MARKER_HEIGHT_DP)
	}

	private fun xAxisReservedHeightPx(): Float {
		return dp(X_LABEL_TO_GRAPH_GAP_DP) + xAxisTextHeightPx()
	}

	private fun xAxisTextHeightPx(): Float {
		val fm = xAxisLabelPaint.fontMetrics
		return fm.descent - fm.ascent
	}

	private fun updateCursor(x: Float, area: PlotArea) {
		cursorX = x.coerceIn(area.left, area.right)
		cursorVisible = true
		invalidate()
	}

	private fun altitudeToY(altitude: Double, area: PlotArea): Float {
		val clamped = altitude.coerceIn(MIN_ALTITUDE_RENDER, MAX_ALTITUDE_RENDER)
		val fraction = (clamped - MIN_ALTITUDE_RENDER) / (MAX_ALTITUDE_RENDER - MIN_ALTITUDE_RENDER)
		return area.bottom - (area.height * fraction).toFloat()
	}

	private fun timeToX(millis: Long, area: PlotArea, model: Model): Float {
		val total = (model.endMillis - model.startMillis).coerceAtLeast(1L)
		val passed = (millis - model.startMillis).coerceIn(0L, total)
		val fraction = passed.toFloat() / total.toFloat()
		return area.left + area.width * fraction
	}

	private fun isInPlotArea(x: Float, y: Float, area: PlotArea): Boolean {
		return x >= area.left && x <= area.right && y >= area.top && y <= area.bottom
	}

	private fun refreshThemeResources() {
		palette = AstroChartColorPalette.fromContext(context)
		dashedGridPaint.color = color(R.color.astro_visibility_y_grid_color)
		zeroGridPaint.color = color(R.color.astro_visibility_y_zero_color)
		yAxisLabelPaint.color = color(R.color.astro_visibility_y_label_color)
		xAxisLabelPaint.color = color(R.color.astro_visibility_x_label_color)
		xTickPaint.color = color(R.color.astro_visibility_x_tick_color)

		cursorLineCenterPaint.color = color(R.color.astro_visibility_cursor_line_center)
		cursorLineSidePaint.color = color(R.color.astro_visibility_cursor_line_side)
		cursorDotFillPaint.color = color(R.color.astro_visibility_cursor_dot_fill)
		cursorDotStrokePaint.color = color(R.color.astro_visibility_cursor_dot_stroke)

		markerBorderPaint.color = color(R.color.astro_visibility_marker_border_color)
		markerSeparatorPaint.color = markerBorderPaint.color
		markerTimePaint.color = resolveColorAttr(
			android.R.attr.textColorPrimary,
			color(R.color.text_color_primary_light)
		)
		markerAltitudePaint.color = yAxisLabelPaint.color
		markerAzimuthPaint.color = color(R.color.astro_visibility_marker_az_color)
		invalidateStaticLayer()
	}

	private fun interpolate(values: DoubleArray, fraction: Float): Double {
		if (values.isEmpty()) return 0.0
		if (values.size == 1) return values[0]
		val index = fraction.coerceIn(0f, 1f) * (values.size - 1).toFloat()
		val start = floor(index).toInt().coerceIn(0, values.size - 1)
		val end = min(values.size - 1, start + 1)
		val t = (index - start).toDouble()
		return lerp(values[start], values[end], t)
	}

	private fun interpolateAzimuth(values: DoubleArray, fraction: Float): Double {
		if (values.isEmpty()) return 0.0
		if (values.size == 1) return values[0]
		val index = fraction.coerceIn(0f, 1f) * (values.size - 1).toFloat()
		val start = floor(index).toInt().coerceIn(0, values.size - 1)
		val end = min(values.size - 1, start + 1)
		val t = (index - start).toDouble()
		return lerpAzimuth(values[start], values[end], t)
	}

	private fun normalizeAzimuth(value: Double): Double {
		var az = value % 360.0
		if (az < 0) az += 360.0
		return az
	}

	private fun lerp(start: Double, end: Double, t: Double): Double {
		return start + (end - start) * t
	}

	private fun lerpAzimuth(start: Double, end: Double, t: Double): Double {
		val delta = ((end - start + 540.0) % 360.0) - 180.0
		return normalizeAzimuth(start + delta * t)
	}

	private fun color(resId: Int): Int = ContextCompat.getColor(context, resId)

	private fun resolveColorAttr(attrId: Int, fallback: Int): Int {
		val typedValue = TypedValue()
		if (!context.theme.resolveAttribute(attrId, typedValue, true)) {
			return fallback
		}
		return if (typedValue.resourceId != 0) {
			ContextCompat.getColor(context, typedValue.resourceId)
		} else {
			typedValue.data
		}
	}

	private fun dp(value: Float): Float = AndroidUtils.dpToPxF(context, value)
	private fun sp(value: Float): Float = AndroidUtils.spToPxF(context, value)

	private fun createScope(): CoroutineScope {
		return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	}

	private fun ensureScope() {
		val scopeJob = viewScope.coroutineContext[Job]
		if (scopeJob == null || !scopeJob.isActive) {
			viewScope = createScope()
		}
	}

	companion object {
		const val MIN_ALTITUDE_RENDER = -40.0
		const val MAX_ALTITUDE_RENDER = 95.0
		const val FOUR_HOURS_MILLIS = 4L * 60L * 60L * 1000L

		const val BG_STRIPE_STEP_DETAIL_PX = 1f
		const val BG_STRIPE_STEP_DEFAULT_PX = 2f

		const val OUTER_LEFT_PADDING_DP = 16f
		const val OUTER_RIGHT_PADDING_DP = 16f
		const val OUTER_TOP_PADDING_DP = 1f
		const val OUTER_BOTTOM_PADDING_DP = 1f

		const val Y_GRID_STROKE_DP = 1f
		const val Y_GRID_DASH_DP = 6f
		const val Y_GRID_GAP_DP = 6f
		const val Y_LABEL_TEXT_SP = 10f
		const val Y_LABEL_TO_LINE_GAP_DP = 2f
		const val RIGHT_AXIS_OUTSET_DP = 30f

		const val X_LABEL_TEXT_SP = 10f
		const val X_TICK_STROKE_DP = 1f
		const val X_TICK_HEIGHT_DP = 7f
		const val X_LABEL_TO_GRAPH_GAP_DP = 2f
		const val LABEL_EDGE_MIN_DP = 0f

		const val SUN_ICON_SIZE_DP = 12f
		const val SUN_ICON_RAISE_DP = 2f

		const val CURSOR_LINE_STROKE_DP = 2f
		const val CURSOR_SIDE_OFFSET_DP = 2f
		const val CURSOR_DOT_RADIUS_DP = 5f
		const val CURSOR_DOT_STROKE_DP = 2f

		const val MARKER_TEXT_SP = 14f
		const val MARKER_BORDER_STROKE_DP = 1f
		const val MARKER_SEPARATOR_STROKE_DP = 1f
		const val MARKER_CORNER_DP = 9f
		const val MARKER_H_PADDING_DP = 10f
		const val MARKER_HEIGHT_DP = 24f
		const val MARKER_SEPARATOR_INSET_DP = 2f
		const val MARKER_TO_GRAPH_GAP_DP = 3f
	}
}
