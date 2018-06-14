package net.osmand.telegram.utils

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import java.util.*

class UiUtils(private val app: TelegramApplication) {

	private val drawableCache = LinkedHashMap<Long, Drawable>()
	private val circleBitmapCache = LinkedHashMap<String, Bitmap>()

	private val isLightContent: Boolean
		get() = true

	fun getCircleBitmap(path: String): Bitmap? {
		var bmp: Bitmap? = circleBitmapCache[path]
		if (bmp == null) {
			bmp = BitmapFactory.decodeFile(path)
			if (bmp != null) {
				bmp = app.uiUtils.createCircleBitmap(bmp, true)
				circleBitmapCache[path] = bmp
			}
		}
		return bmp
	}

	private fun getDrawable(@DrawableRes resId: Int, @ColorRes clrId: Int): Drawable? {
		val hash = (resId.toLong() shl 31) + clrId
		var d: Drawable? = drawableCache[hash]
		if (d == null) {
			d = ContextCompat.getDrawable(app, resId)
			if (d != null) {
				d = DrawableCompat.wrap(d)
				d!!.mutate()
				if (clrId != 0) {
					DrawableCompat.setTint(d, ContextCompat.getColor(app, clrId))
				}
				drawableCache[hash] = d
			}
		}
		return d
	}

	private fun getPaintedDrawable(@DrawableRes resId: Int, @ColorInt color: Int): Drawable? {
		val hash = (resId.toLong() shl 31) + color
		var d: Drawable? = drawableCache[hash]
		if (d == null) {
			d = ContextCompat.getDrawable(app, resId)
			if (d != null) {
				d = DrawableCompat.wrap(d)
				d!!.mutate()
				DrawableCompat.setTint(d, color)
				drawableCache[hash] = d
			}
		}
		return d
	}

	fun getPaintedIcon(@DrawableRes id: Int, @ColorInt color: Int): Drawable? {
		return getPaintedDrawable(id, color)
	}

	fun getIcon(@DrawableRes id: Int, @ColorRes colorId: Int): Drawable? {
		return getDrawable(id, colorId)
	}

	fun getIcon(@DrawableRes backgroundId: Int, @DrawableRes id: Int, @ColorRes colorId: Int): Drawable {
		val b = getDrawable(backgroundId, 0)
		val f = getDrawable(id, colorId)
		val layers = arrayOfNulls<Drawable>(2)
		layers[0] = b
		layers[1] = f
		return LayerDrawable(layers)
	}

	fun getThemedIcon(@DrawableRes id: Int): Drawable? {
		return getDrawable(id, if (isLightContent) R.color.icon_color_light else 0)
	}

	fun getIcon(@DrawableRes id: Int): Drawable? {
		return getDrawable(id, 0)
	}

	fun getIcon(@DrawableRes id: Int, light: Boolean): Drawable? {
		return getDrawable(id, if (light) R.color.icon_color_light else 0)
	}

	fun createCircleBitmap(source: Bitmap, recycleSource: Boolean = false): Bitmap {
		val size = Math.min(source.width, source.height)

		val width = (source.width - size) / 2
		val height = (source.height - size) / 2

		val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

		val canvas = Canvas(bitmap)
		val paint = Paint()
		val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
		if (width != 0 || height != 0) {
			// source isn't square, move viewport to center
			val matrix = Matrix()
			matrix.setTranslate((-width).toFloat(), (-height).toFloat())
			shader.setLocalMatrix(matrix)
		}
		paint.shader = shader
		paint.isAntiAlias = true

		val r = size / 2f
		canvas.drawCircle(r, r, r, paint)

		if (recycleSource) {
			source.recycle()
		}

		return bitmap
	}
}
