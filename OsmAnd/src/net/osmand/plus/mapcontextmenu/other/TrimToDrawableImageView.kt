package net.osmand.plus.mapcontextmenu.other

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.roundToInt

/**
 * An ImageView subclass that attempts to size the view itself to perfectly match
 * the dimensions of the visible (scaled and cropped) drawable area, as determined
 * by the set 'scaleType' and the 'layout_width' or 'layout_height' constraints.
 */
class TrimToDrawableImageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

	private val mDrawableRect = RectF()

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		// 1. Get the drawable and its intrinsic dimensions
		val drawable = drawable
		if (drawable == null) {
			// If there's no drawable, fall back to default ImageView measurement
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}

		// 2. Measure the ImageView as a standard view would
		// This gives us the size requested by the layout (parent's constraints)
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		// 3. Get the calculated width and height from the initial measurement
		var measuredWidth = measuredWidth
		var measuredHeight = measuredHeight

		// 4. Determine the size of the drawable after scaling

		// Get the matrix that the ImageView uses to transform the drawable
		val matrix: Matrix = imageMatrix

		// Get the original bounds of the drawable
		mDrawableRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

		// Apply the transformation matrix to the drawable bounds
		matrix.mapRect(mDrawableRect)

		val finalDrawableWidth = mDrawableRect.width().roundToInt()
		val finalDrawableHeight = mDrawableRect.height().roundToInt()

		// 5. Adjust the measured dimensions based on the calculated drawable size

		// If the calculated drawable width is less than the requested/measured width,
		// we use the drawable width (trim the view).
		if (finalDrawableWidth < measuredWidth) {
			measuredWidth = finalDrawableWidth
		}

		// If the calculated drawable height is less than the requested/measured height,
		// we use the drawable height (trim the view).
		if (finalDrawableHeight < measuredHeight) {
			measuredHeight = finalDrawableHeight
		}

		// 6. Set the final measured dimensions
		setMeasuredDimension(measuredWidth, measuredHeight)
	}
}