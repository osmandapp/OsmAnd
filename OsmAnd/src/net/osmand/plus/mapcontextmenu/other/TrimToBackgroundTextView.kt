package net.osmand.plus.mapcontextmenu.other

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
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

	// Use a member drawable instead of the standard background property
	private var mScalableDrawable: Drawable? = null
	private val mMatrix = Matrix()
	private val mDrawableRect = RectF()

	init {
		// You would typically set your drawable here or via a custom attribute
		// Example: mScalableDrawable = ContextCompat.getDrawable(context, R.drawable.my_scaled_image)

		// Ensure text is centered by default
		gravity = Gravity.CENTER
	}

	/**
	 * Set the drawable that will be used as the scalable background content.
	 */
	fun setScalableDrawable(drawable: Drawable?) {
		mScalableDrawable = drawable
		drawable?.callback = this // Allow drawable to invalidate the view
		requestLayout() // Trigger re-measurement and redraw
	}

	// Override needed to check if the view needs a redraw based on drawable state change
	override fun verifyDrawable(who: Drawable): Boolean {
		return who === mScalableDrawable || super.verifyDrawable(who)
	}

	fun setDrawable(who: NetworkRouteDrawable?) {
		if (who == null) {
			mScalableDrawable = null
			text = null
			// Resetting text appearance might be necessary if styles were applied
		} else {
			mScalableDrawable = who.backgroundDrawable
			text = who.osmcText

			// --- NEW CODE TO APPLY TEXT STYLING ---
			applyTextPaint(who.textPaint)
			// --------------------------------------
		}
		Log.d("Corwin", "setDrawable: $text")
		invalidate()
	}

	private fun applyTextPaint(p: Paint) {
		// 1. Text Size
		textSize = p.textSize / resources.displayMetrics.scaledDensity

		// 2. Text Color
		setTextColor(p.color)

		// 3. Typeface (Font)
		setTypeface(p.typeface)

		// 4. Other properties (optional, depending on requirements)
		// You might want to apply anti-aliasing, stroke width, etc. if relevant.
		paint.flags = p.flags
		paint.strokeWidth = p.strokeWidth
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val drawable = mScalableDrawable
		if (drawable == null || drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
			// If no drawable, fall back to standard TextView behavior
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}

		// 1. Get the current layout constraints (requested size)
		val specWidth = View.MeasureSpec.getSize(widthMeasureSpec)
		val specHeight = View.MeasureSpec.getSize(heightMeasureSpec)

		// 2. Determine the size needed for the TEXT CONTENT ONLY
		// We call super.onMeasure to measure the text and store the result.
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		val dWidth = drawable.intrinsicWidth
		val dHeight = drawable.intrinsicHeight

		// Calculate the uniform scale factor to FIT the drawable inside the target
		val scaleX = specWidth.toFloat() / dWidth
		val scaleY = specHeight.toFloat() / dHeight
		val scale = min(scaleX, scaleY)


		// Crucial Change: Get the calculated text size *minus* the view padding
		// Use the Layout object to get the text bounds directly if possible,
		// or subtract the padding from the measured dimensions.
		val textContentWidth = layout.getLineWidth(0).roundToInt() - paddingLeft - paddingRight
		val textContentHeight = layout.height - paddingTop - paddingBottom

		// 3. Calculate the scaled drawable size (Fit Center logic)


		// Calculate the maximum space the view can occupy for the *content*
		// This target must be constrained by the layout spec *minus* padding,
		// and the size of the text content itself.
//		val targetWidth = min(specWidth - paddingLeft - paddingRight, textContentWidth)
//		val targetHeight = min(specHeight - paddingTop - paddingBottom, textContentHeight)


		// The actual size of the scaled drawable
		val finalDrawableWidth = (dWidth * scale).roundToInt()
		val finalDrawableHeight = (dHeight * scale).roundToInt()

		// 4. Determine the final measured size (trimming the view)

		// The *content* area must be big enough for the text OR the scaled drawable, whichever is larger.
		val contentW = max(textContentWidth, finalDrawableWidth)
		val contentH = max(textContentHeight, finalDrawableHeight)

		// Add the view's padding back to the content size for the final measured dimension
		var measuredW = contentW + paddingLeft + paddingRight
		var measuredH = contentH + paddingTop + paddingBottom

		// Final trim based on AT_MOST constraints (if wrap_content is used)
		val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)

		if (widthMode == View.MeasureSpec.AT_MOST) {
			measuredW = measuredW.coerceAtMost(specWidth)
		}
		if (heightMode == View.MeasureSpec.AT_MOST) {
			measuredH = measuredH.coerceAtMost(specHeight)
		}

		// 5. Update the scaling matrix for drawing (Center logic)
		mMatrix.reset()
		mMatrix.postScale(scale, scale)

		// Calculate the necessary translation to center the scaled image within the *content area*
		// The total view size is measuredW/H. The content area is measuredW/H - padding.
		// The translation needs to account for the left/top padding.
//		val dx = (contentW - finalDrawableWidth) / 2f + paddingLeft
//		val dy = (contentH - finalDrawableHeight) / 2f + paddingTop
//		mMatrix.postTranslate(dx, dy)

		val dx = (contentW - finalDrawableWidth) / 2f + paddingLeft
		val dy = (contentH - finalDrawableHeight) / 2f + paddingTop
		mMatrix.postTranslate(dx, dy)

		// 6. Set the final measured dimensions
		setMeasuredDimension(measuredW, measuredH)
		super.onMeasure(
			MeasureSpec.makeMeasureSpec(measuredW, MeasureSpec.EXACTLY),
			MeasureSpec.makeMeasureSpec(measuredH, MeasureSpec.EXACTLY)
		)
	}

	override fun onDraw(canvas: Canvas) {
		mScalableDrawable?.let { drawable ->
			canvas.save()
			// Apply the matrix calculated in onMeasure
			canvas.concat(mMatrix)

			// Set bounds to the original intrinsic size for the drawing operation
			drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
			drawable.draw(canvas)

			canvas.restore()
		}

		// Draw the text (TextView superclass handles centering due to Gravity.CENTER)
		super.onDraw(canvas)
	}
}