package net.osmand.plus.charts

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.github.mikephil.charting.charts.GradientChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.GradientXAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.abs

/**
 * Custom renderer that highlights the selected X-axis label by drawing a rounded background ("pill").
 * Inherits from [GradientXAxisRenderer] to preserve the specific logic
 * of mapping axis labels directly to data points.
 */
class HighlightGradientXAxisRenderer(
	chart: GradientChart,
	viewPortHandler: ViewPortHandler,
	xAxis: XAxis,
	trans: Transformer
) : GradientXAxisRenderer(chart, viewPortHandler, xAxis, trans) {

	// Value to highlight (null if nothing selected)
	var highlightedValue: Float? = null

	// Configuration for the highlighted state
	var activeBackgroundColor: Int = Color.BLUE
	var activeTextColor: Int = Color.WHITE

	// Dimensions for the background
	private val paddingHorizontal = Utils.convertDpToPixel(4f)
	private val paddingVertical = Utils.convertDpToPixel(4f)
	private val cornerRadius = Utils.convertDpToPixel(12f)

	// Paint object for the background (pre-allocated for performance)
	private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
	}
	private val bgRect = RectF()

	override fun drawLabels(c: Canvas, pos: Float, anchor: MPPointF) {
		val labelRotationAngleDegrees = mXAxis.labelRotationAngle

		// Logic copied from GradientXAxisRenderer to match specific label positioning
		// -----------------------------------------------------------------
		val entryCount = mXAxis.mEntryCount
		val range = mXAxis.mAxisMaximum - mXAxis.mAxisMinimum
		val splitInterval = range / (entryCount - 1).toFloat()

		val positions = FloatArray(entryCount * 2)

		for (i in positions.indices step 2) {
			positions[i] = mXAxis.mAxisMinimum + (i / 2) * splitInterval
		}

		mTrans.pointValuesToPixel(positions)
		// -----------------------------------------------------------------

		for (i in positions.indices step 2) {
			var x = positions[i]

			if (mViewPortHandler.isInBoundsX(x)) {
				// Get value and label
				val value = mXAxis.mEntries[i / 2]
				val label = mXAxis.valueFormatter.getFormattedValue(value, mXAxis)

				// Calculate accurate text dimensions
				val textWidth = Utils.calcTextWidth(mAxisLabelPaint, label).toFloat()
				val textHeight = Utils.calcTextHeight(mAxisLabelPaint, label).toFloat()

				// --- CLIPPING LOGIC (Copied from parent) ---
				if (mXAxis.isAvoidFirstLastClippingEnabled) {
					if (i / 2 == entryCount - 1 && entryCount > 1) {
						x -= textWidth / 2.0f
					} else if (i == 0) {
						x += textWidth / 2.0f
					}
				}
				// -------------------------------------------

				// --- HIGHLIGHT LOGIC ---
				val originalColor = mAxisLabelPaint.color

				// Check if this label matches the highlighted value
				val isSelected = highlightedValue != null &&
						!highlightedValue!!.isNaN() &&
						abs(value - highlightedValue!!) < 0.00001f

				if (isSelected) {
					// 1. Draw Background
					bgPaint.color = activeBackgroundColor

					// Center horizontally: 'x' is center, expand left/right
					val rectLeft = x - (textWidth / 2f) - paddingHorizontal
					val rectRight = x + (textWidth / 2f) + paddingHorizontal

					// Center vertically
					val rectTop = pos
					val rectBottom = pos + textHeight + paddingVertical

					bgRect.set(rectLeft, rectTop, rectRight, rectBottom)

					c.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

					// 2. Change Text Color
					mAxisLabelPaint.color = activeTextColor
				} else {
					mAxisLabelPaint.color = mXAxis.textColor
				}

				// 3. Draw Text
				// We draw the text exactly at 'pos' (standard behavior).
				// Since we expanded the background AROUND 'pos', it will be centered.
				drawLabel(c, label, x, pos, anchor, labelRotationAngleDegrees)

				// Restore paint color
				mAxisLabelPaint.color = originalColor
			}
		}
	}
}