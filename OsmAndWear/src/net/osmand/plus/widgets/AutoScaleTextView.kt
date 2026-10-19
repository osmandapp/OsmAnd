package net.osmand.plus.widgets

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.Dimension
import net.osmand.plus.R
import kotlin.math.min


class AutoScaleTextView : androidx.appcompat.widget.AppCompatTextView {
	constructor(context: Context) : super(context)
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
	constructor(
		context: Context, attrs: AttributeSet?,
		defStyleAttr: Int) : super(context, attrs) {
		val typedArray = context.theme.obtainStyledAttributes(
			attrs,
			R.styleable.AutoScaleTextView,
			defStyleAttr,
			0)
		minTextSize = typedArray.getDimension(R.styleable.AutoScaleTextView_minTextSize, 1f)
		maxTextSize = typedArray.getDimension(R.styleable.AutoScaleTextView_maxTextSize, 30f)
		typedArray.recycle()
	}

	private val textRect = Rect()
	private var minTextSize = 1f
	private var maxTextSize = 30f

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		var width = MeasureSpec.getSize(widthMeasureSpec)
		var height = MeasureSpec.getSize(heightMeasureSpec)
		val modeW = MeasureSpec.getMode(widthMeasureSpec)
		val modeH = MeasureSpec.getMode(heightMeasureSpec)
		var desiredWidth = width
		var desiredHeight = height
		var curTextSize = minTextSize
		if (text != null) {
			val textScaleCoef = TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP,
				1f, resources.displayMetrics)
			paint.textSize = curTextSize
			paint.getTextBounds(text.toString(), 0, text.length, textRect)
			while (getTextWidth() < width && textRect.height() < height && curTextSize < maxTextSize) {
				curTextSize++
				paint.textSize = curTextSize
				paint.getTextBounds(text.toString(), 0, text.length, textRect)
			}
			if (getTextWidth() > width || textRect.height() > height) {
				curTextSize--
			}
			setTextSize(Dimension.SP, curTextSize / textScaleCoef)
			desiredWidth = getTextWidth()
		}

		val measuredWidth = when (modeW) {
			MeasureSpec.EXACTLY -> width
			MeasureSpec.AT_MOST -> min(desiredWidth, width)
			else -> desiredWidth
		}

		val measuredHeight = when (modeH) {
			MeasureSpec.EXACTLY -> height
			MeasureSpec.AT_MOST -> min(desiredHeight, height)
			else -> desiredHeight
		}
		setMeasuredDimension(measuredWidth, measuredHeight)
	}

	private fun getTextWidth() = (paint.measureText(text.toString()) + 0.5f).toInt()
}
