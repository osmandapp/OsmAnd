package net.osmand.plus.views.layers

import android.graphics.PointF
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.PointI
import net.osmand.plus.utils.NativeUtilities
import kotlin.math.floor

/**
 * Screen offsets that lift ruler points without terrain data to the relief of their neighbours.
 * The offsets are sampled on one circle and depend only on the bearing, so a circle closes and
 * its labels sit on it whatever the drawing order is.
 */
internal class RulerTerrainShifts(private val samplesCount: Int) {

	private val angleStep = 360.0 / samplesCount
	private val sampled = BooleanArray(samplesCount)
	private val sampleX31 = IntArray(samplesCount)
	private val sampleY31 = IntArray(samplesCount)
	private val sampleHasHeight = BooleanArray(samplesCount)
	private val shiftKnown = BooleanArray(samplesCount)
	private val shiftX = FloatArray(samplesCount)
	private val shiftY = FloatArray(samplesCount)

	// PointI needs the native library, which loads after the layer is created
	private val location31 by lazy { PointI() }
	private val screenPoint31 by lazy { PointI() }

	private var renderer: MapRendererView? = null

	fun reset() {
		renderer = null
	}

	fun startCircle(renderer: MapRendererView) {
		this.renderer = renderer
		sampled.fill(false)
		shiftKnown.fill(false)
	}

	fun setSample(index: Int, x31: Int, y31: Int) {
		val renderer = renderer ?: return
		sampled[index] = true
		sampleX31[index] = x31
		sampleY31[index] = y31
		sampleHasHeight[index] = hasHeight(renderer, x31, y31)
	}

	/**
	 * Moves [pixel] of a point without terrain data by the offset interpolated at [bearing].
	 * Points with data and points outside a sampled circle stay where the renderer put them.
	 */
	fun applyShift(x31: Int, y31: Int, bearing: Double, pixel: PointF) {
		val renderer = renderer ?: return
		val position = normalizeBearing(bearing) / angleStep
		val index = floor(position).toInt().coerceIn(0, samplesCount - 1)
		val fraction = (position - index).toFloat()
		val hasHeight = if (fraction == 0f && sampled[index]
			&& sampleX31[index] == x31 && sampleY31[index] == y31) {
			sampleHasHeight[index]
		} else {
			hasHeight(renderer, x31, y31)
		}
		if (hasHeight) {
			return
		}
		val nextIndex = (index + 1) % samplesCount
		calculateShift(renderer, index)
		calculateShift(renderer, nextIndex)
		pixel.offset(
			shiftX[index] + (shiftX[nextIndex] - shiftX[index]) * fraction,
			shiftY[index] + (shiftY[nextIndex] - shiftY[index]) * fraction)
	}

	private fun calculateShift(renderer: MapRendererView, index: Int) {
		if (shiftKnown[index]) {
			return
		}
		shiftKnown[index] = true
		shiftX[index] = 0f
		shiftY[index] = 0f
		if (!sampled[index]) {
			return
		}
		if (sampleHasHeight[index]) {
			calculateOwnShift(renderer, index)
			return
		}
		val previous = findHeightSample(index, -1)
		val next = findHeightSample(index, 1)
		if (previous < 0 || next < 0) {
			return
		}
		calculateShift(renderer, previous)
		calculateShift(renderer, next)
		val toPrevious = cyclicDistance(previous, index)
		val gap = toPrevious + cyclicDistance(index, next)
		val fraction = if (gap > 0) toPrevious.toFloat() / gap else 0f
		shiftX[index] = shiftX[previous] + (shiftX[next] - shiftX[previous]) * fraction
		shiftY[index] = shiftY[previous] + (shiftY[next] - shiftY[previous]) * fraction
	}

	private fun calculateOwnShift(renderer: MapRendererView, index: Int) {
		location31.x = sampleX31[index]
		location31.y = sampleY31[index]
		if (!renderer.getElevatedPointFromLocation(location31, screenPoint31, true)) {
			return
		}
		val elevatedX = screenPoint31.x
		val elevatedY = screenPoint31.y
		if (renderer.getScreenPointFromLocation(location31, screenPoint31, true)) {
			shiftX[index] = (elevatedX - screenPoint31.x).toFloat()
			shiftY[index] = (elevatedY - screenPoint31.y).toFloat()
		}
	}

	private fun findHeightSample(from: Int, direction: Int): Int {
		for (step in 1 until samplesCount) {
			val index = Math.floorMod(from + direction * step, samplesCount)
			if (sampled[index] && sampleHasHeight[index]) {
				return index
			}
		}
		return -1
	}

	private fun cyclicDistance(from: Int, to: Int): Int = Math.floorMod(to - from, samplesCount)

	private fun hasHeight(renderer: MapRendererView, x31: Int, y31: Int): Boolean {
		location31.x = x31
		location31.y = y31
		return renderer.getLocationHeightInMeters(location31) > NativeUtilities.MIN_ALTITUDE_VALUE
	}

	private fun normalizeBearing(bearing: Double): Double {
		val shifted = (bearing + 180) % 360
		return if (shifted < 0) shifted + 360 else shifted
	}
}