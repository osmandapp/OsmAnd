package net.osmand.plus.views.controls.maphudbuttons

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable
import androidx.core.graphics.withRotation

class CompassDrawable(private val original: Drawable) : Drawable() {

	var mapRotation = 0f
	var linkedToMapRotation = true

	override fun draw(canvas: Canvas) {
		canvas.withRotation(mapRotation, intrinsicWidth / 2f, intrinsicHeight / 2f) {
			original.draw(this)
		}
	}

	override fun getMinimumHeight(): Int {
		return original.minimumHeight
	}

	override fun getMinimumWidth(): Int {
		return original.minimumWidth
	}

	override fun getIntrinsicHeight(): Int {
		return original.intrinsicHeight
	}

	override fun getIntrinsicWidth(): Int {
		return original.intrinsicWidth
	}

	override fun setChangingConfigurations(configs: Int) {
		super.setChangingConfigurations(configs)
		original.changingConfigurations = configs
	}

	override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
		super.setBounds(left, top, right, bottom)
		original.setBounds(left, top, right, bottom)
	}

	override fun setAlpha(alpha: Int) {
		original.alpha = alpha
	}

	override fun setColorFilter(cf: ColorFilter?) {
		original.colorFilter = cf
	}

	@Deprecated("Deprecated in Java")
	override fun getOpacity(): Int {
		return original.opacity
	}
}
