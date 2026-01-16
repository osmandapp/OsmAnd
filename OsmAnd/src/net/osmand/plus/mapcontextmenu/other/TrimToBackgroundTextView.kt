package net.osmand.plus.mapcontextmenu.other

import android.content.Context
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import net.osmand.plus.mapcontextmenu.controllers.NetworkRouteDrawable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TrimToBackgroundTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

	private var mScalableDrawable: Drawable? = null
	private val mMatrix = Matrix()

	init {
		gravity = Gravity.CENTER
	}

	override fun verifyDrawable(who: Drawable): Boolean {
		return who === mScalableDrawable || super.verifyDrawable(who)
	}

	fun setDrawable(who: NetworkRouteDrawable?) {
		if (who == null) {
			mScalableDrawable = null
			text = null
		} else {
			mScalableDrawable = who.backgroundDrawable
			text = who.osmcText
			applyTextPaint(who.textPaint)
		}
		setBackgroundDrawable(mScalableDrawable)
		invalidate()
	}

	private fun applyTextPaint(p: Paint) {
		setTextColor(p.color)
		setTypeface(p.typeface)
		paint.flags = p.flags
		paint.strokeWidth = p.strokeWidth
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val drawable = mScalableDrawable
		if (drawable == null || drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}
		val specWidth = MeasureSpec.getSize(widthMeasureSpec)
		val specHeight = MeasureSpec.getSize(heightMeasureSpec)
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		val dWidth = drawable.intrinsicWidth
		val dHeight = drawable.intrinsicHeight
		val scaleX = specWidth.toFloat() / dWidth
		val scaleY = specHeight.toFloat() / dHeight
		val scale = min(scaleX, scaleY)

		val textContentWidth = layout.getLineWidth(0).roundToInt() - paddingLeft - paddingRight
		val textContentHeight = layout.height - paddingTop - paddingBottom
		val finalDrawableWidth = (dWidth * scale).roundToInt()
		val finalDrawableHeight = (dHeight * scale).roundToInt()
		val contentW = max(textContentWidth, finalDrawableWidth)
		val contentH = max(textContentHeight, finalDrawableHeight)
		var measuredW = contentW + paddingLeft + paddingRight
		var measuredH = contentH + paddingTop + paddingBottom
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)

		if (widthMode == MeasureSpec.AT_MOST) {
			measuredW = measuredW.coerceAtMost(specWidth)
		}
		if (heightMode == MeasureSpec.AT_MOST) {
			measuredH = measuredH.coerceAtMost(specHeight)
		}
		mMatrix.reset()
		mMatrix.postScale(scale, scale)
		val dx = (contentW - finalDrawableWidth) / 2f + paddingLeft
		val dy = (contentH - finalDrawableHeight) / 2f + paddingTop
		mMatrix.postTranslate(dx, dy)
		setMeasuredDimension(measuredW, measuredH)
		super.onMeasure(
			MeasureSpec.makeMeasureSpec(measuredW, MeasureSpec.EXACTLY),
			MeasureSpec.makeMeasureSpec(measuredH, MeasureSpec.EXACTLY)
		)
	}
}