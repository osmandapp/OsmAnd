package net.osmand.telegram.ui.views

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import net.osmand.telegram.TelegramApplication

class DirectionDrawable(private val app: TelegramApplication) : Drawable() {

	private var angle: Float = 0.toFloat()
	private var arrowImage: Drawable? = null

	fun setImage(resourceId: Int, clrId: Int) {
		arrowImage = app.uiUtils.getIcon(resourceId, clrId)
		onBoundsChange(bounds)
	}

	fun setAngle(angle: Float) {
		this.angle = angle
	}

	override fun getIntrinsicWidth() = arrowImage?.intrinsicWidth ?: super.getIntrinsicWidth()

	override fun getIntrinsicHeight() = arrowImage?.intrinsicHeight ?: super.getIntrinsicHeight()

	override fun onBoundsChange(bounds: Rect) {
		super.onBoundsChange(bounds)
		if (arrowImage != null) {
			val w = arrowImage!!.intrinsicWidth
			val h = arrowImage!!.intrinsicHeight
			val dx = Math.max(0, bounds.width() - w)
			val dy = Math.max(0, bounds.height() - h)
			if (bounds.width() == 0 && bounds.height() == 0) {
				arrowImage!!.setBounds(0, 0, w, h)
			} else {
				arrowImage!!.setBounds(
					bounds.left + dx / 2,
					bounds.top + dy / 2,
					bounds.right - dx / 2,
					bounds.bottom - dy / 2
				)
			}
		}
	}

	override fun draw(canvas: Canvas) {
		canvas.save()
		if (arrowImage != null) {
			val r = bounds
			canvas.rotate(angle, r.centerX().toFloat(), r.centerY().toFloat())
			arrowImage!!.draw(canvas)
		}
		canvas.restore()
	}

	override fun getOpacity() = PixelFormat.UNKNOWN

	override fun setAlpha(alpha: Int) {}

	override fun setColorFilter(cf: ColorFilter?) {}
}
