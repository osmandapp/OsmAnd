package net.osmand.plus.plugins.astronomy.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import androidx.core.graphics.withClip
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroChartColorPalette
import net.osmand.plus.utils.AndroidUtils
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.createBitmap

class AstroScheduleDayGraphView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : android.view.View(context, attrs, defStyleAttr) {

	private var model: AstroScheduleCardModel.ScheduleDayGraphData? = null
	private var palette: AstroChartColorPalette? = null
	private var staticLayerInvalidate = true
	private var staticLayerBitmap: Bitmap? = null
	private var staticLayerCanvas: Canvas? = null

	private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
	}
	private val objectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
	}
	private val clipPath = Path()
	private val clipRect = RectF()

	init {
		refreshThemeResources()
	}

	fun submitModel(model: AstroScheduleCardModel.ScheduleDayGraphData?) {
		this.model = model
		invalidateStaticLayer()
		invalidate()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		refreshThemeResources()
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		clearStaticLayer()
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		invalidateStaticLayer()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val localModel = model ?: return
		if (width <= 0 || height <= 0) {
			return
		}
		val localPalette = palette ?: return
		ensureStaticLayer(localModel, localPalette)
		staticLayerBitmap?.let { bitmap ->
			canvas.drawBitmap(bitmap, 0f, 0f, null)
		}
	}

	private fun ensureStaticLayer(
		localModel: AstroScheduleCardModel.ScheduleDayGraphData,
		palette: AstroChartColorPalette
	) {
		if (!staticLayerInvalidate && staticLayerBitmap != null) {
			return
		}
		val bitmap = obtainStaticBitmap(width, height)
		bitmap.eraseColor(Color.TRANSPARENT)
		val staticCanvas = staticLayerCanvas ?: Canvas(bitmap).also { staticLayerCanvas = it }

		clipRect.set(0f, 0f, width.toFloat(), height.toFloat())
		clipPath.reset()
		val corner = dp(CORNER_RADIUS_DP)
		clipPath.addRoundRect(clipRect, corner, corner, Path.Direction.CW)

		staticCanvas.withClip(clipPath) {
			drawSunBackground(this, localModel, palette)
			drawObjectVisibilityOverlay(this, localModel, palette)
		}
		staticLayerInvalidate = false
	}

	private fun drawSunBackground(
		canvas: Canvas,
		localModel: AstroScheduleCardModel.ScheduleDayGraphData,
		palette: AstroChartColorPalette
	) {
		val drawStep = if (width > MAX_FINE_SAMPLES_WIDTH) 2 else 1
		var x = 0
		while (x < width) {
			val nextX = min(width, x + drawStep)
			val fraction = if (width <= 1) 0f else x.toFloat() / (width - 1).toFloat()
			sunPaint.color = palette.colorForSunAltitude(interpolate(localModel.sunAltitudes, fraction))
			canvas.drawRect(x.toFloat(), 0f, nextX.toFloat(), height.toFloat(), sunPaint)
			x = nextX
		}
	}

	private fun drawObjectVisibilityOverlay(
		canvas: Canvas,
		localModel: AstroScheduleCardModel.ScheduleDayGraphData,
		palette: AstroChartColorPalette
	) {
		val objectBandHeight = dp(OBJECT_BAND_HEIGHT_DP).coerceAtMost(height.toFloat())
		val objectBandTop = (height - objectBandHeight) / 2f
		val objectBandBottom = objectBandTop + objectBandHeight
		val sourceAltitudes = localModel.objectAltitudes
		val sourceSampleCount = sourceAltitudes.size
		if (sourceSampleCount < 2) return

		val renderSampleCount = min(sourceSampleCount, max(2, width))
		val renderAltitudes = DoubleArray(renderSampleCount) { index ->
			val fraction = index.toFloat() / (renderSampleCount - 1).toFloat()
			interpolate(sourceAltitudes, fraction)
		}

		val colors = IntArray(renderSampleCount) { index ->
			palette.colorForPositiveObjectAltitude(renderAltitudes[index])
		}
		val positions = FloatArray(renderSampleCount) { index ->
			index.toFloat() / (renderSampleCount - 1).toFloat()
		}
		objectPaint.shader = LinearGradient(
			0f,
			objectBandTop,
			width.toFloat(),
			objectBandTop,
			colors,
			positions,
			Shader.TileMode.CLAMP
		)

		var segmentStart = -1
		for (index in 0 until renderSampleCount) {
			val isVisible = renderAltitudes[index] > 0.0
			if (isVisible && segmentStart == -1) {
				segmentStart = index
			}
			val isSegmentEnd = segmentStart != -1 && (!isVisible || index == renderSampleCount - 1)
			if (!isSegmentEnd) continue

			val segmentEnd = if (isVisible && index == renderSampleCount - 1) index else index - 1
			val left = sampleToX(segmentStart, renderSampleCount)
			val rightRaw = sampleToX(segmentEnd, renderSampleCount)
			val right = if (rightRaw <= left) {
				(left + 1f).coerceAtMost(width.toFloat())
			} else {
				rightRaw
			}
			if (right > left) {
				canvas.drawRect(left, objectBandTop, right, objectBandBottom, objectPaint)
			}
			segmentStart = -1
		}
		objectPaint.shader = null
	}

	private fun sampleToX(index: Int, sampleCount: Int): Float {
		if (sampleCount <= 1 || width <= 0) return 0f
		val clampedIndex = index.coerceIn(0, sampleCount - 1)
		return clampedIndex.toFloat() / (sampleCount - 1).toFloat() * width.toFloat()
	}

	private fun interpolate(values: DoubleArray, fraction: Float): Double {
		if (values.isEmpty()) return 0.0
		if (values.size == 1) return values[0]
		val index = fraction.coerceIn(0f, 1f) * (values.size - 1).toFloat()
		val startIndex = floor(index).toInt().coerceIn(0, values.size - 1)
		val endIndex = min(values.size - 1, startIndex + 1)
		val t = (index - startIndex).toDouble()
		return values[startIndex] + (values[endIndex] - values[startIndex]) * t
	}

	private fun refreshThemeResources() {
		palette = AstroChartColorPalette.fromContext(context)
		invalidateStaticLayer()
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

	private fun dp(value: Float): Float = AndroidUtils.dpToPxF(context, value)

	private companion object {
		private const val CORNER_RADIUS_DP = 2f
		private const val MAX_FINE_SAMPLES_WIDTH = 256
		private const val OBJECT_BAND_HEIGHT_DP = 16f
	}
}
