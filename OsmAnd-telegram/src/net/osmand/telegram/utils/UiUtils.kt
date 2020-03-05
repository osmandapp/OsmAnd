package net.osmand.telegram.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.AsyncTask
import android.view.Surface
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.DirectionDrawable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

const val GRAYSCALE_PHOTOS_DIR = "grayscale_photos/"

const val GRAYSCALE_PHOTOS_EXT = ".jpeg"

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
		return getDrawable(id, if (isLightContent) R.color.icon_light else 0)
	}

	fun getActiveIcon(@DrawableRes id: Int): Drawable? {
		return getDrawable(id, if (isLightContent) R.color.ctrl_active_light else 0)
	}

	fun getActiveColor():Int {
		return ContextCompat.getColor(app, if (isLightContent) R.color.ctrl_active_light else 0)
	}

	fun getIcon(@DrawableRes id: Int): Drawable? {
		return getDrawable(id, 0)
	}

	fun getIcon(@DrawableRes id: Int, light: Boolean): Drawable? {
		return getDrawable(id, if (light) R.color.icon_light else 0)
	}

	fun convertAndSaveGrayPhoto(originalPhotoPath: String, greyPhotoPath: String) {
		if (File(originalPhotoPath).exists()) {
			ConvertPhotoToGrayscale().executeOnExecutor(
				AsyncTask.THREAD_POOL_EXECUTOR,
				originalPhotoPath,
				greyPhotoPath
			)
		}
	}
	
	private fun createCircleBitmap(source: Bitmap, recycleSource: Boolean = false): Bitmap {
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

	fun updateLocationView(
		arrow: ImageView?,
		text: TextView?,
		toLoc: LatLon?,
		cache: UpdateLocationViewCache
	) {
		val fromLoc = app.locationProvider.lastKnownLocationLatLon
		val heading = app.locationProvider.heading
		val mes = FloatArray(2)
		val locPassive = fromLoc == null || toLoc == null || cache.outdatedLocation
		val colorId = if (locPassive) R.color.icon_light else R.color.ctrl_active_light

		if (fromLoc != null && toLoc != null) {
			Location.distanceBetween(
				toLoc.latitude, toLoc.longitude, fromLoc.latitude, fromLoc.longitude, mes
			)
		}

		if (arrow != null) {
			var newImage = false
			val drawable = arrow.drawable
			val dd = if (drawable is DirectionDrawable) {
				drawable
			} else {
				newImage = true
				DirectionDrawable(app)
			}
			dd.setImage(R.drawable.ic_direction_arrow, colorId)
			if (fromLoc == null || toLoc == null || heading == null) {
				dd.setAngle(0f)
			} else {
				dd.setAngle(mes[1] - heading + 180 + cache.screenOrientation)
			}
			if (newImage) {
				arrow.setImageDrawable(dd)
			}
			arrow.invalidate()
		}

		if (text != null) {
			text.setTextColor(ContextCompat.getColor(app, colorId))
			val meters = if (fromLoc == null || toLoc == null) 0f else mes[0]
			text.text = OsmandFormatter.getFormattedDistance(meters, app)
		}
	}

	fun getUpdateLocationViewCache() =
		UpdateLocationViewCache().apply { screenOrientation = getScreenOrientation() }

	private fun getScreenOrientation(): Int {
		// screenOrientation correction must not be applied for devices without compass
		val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
		if (sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
			return 0
		}

		val windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
		val rotation = windowManager?.defaultDisplay?.rotation ?: return 0

		return when (rotation) {
			Surface.ROTATION_90 -> 90
			Surface.ROTATION_180 -> 180
			Surface.ROTATION_270 -> 270
			else -> 0
		}
	}

	class UpdateLocationViewCache {
		var screenOrientation: Int = 0
		var outdatedLocation: Boolean = false
	}

	private class ConvertPhotoToGrayscale : AsyncTask<String, String, Void?>() {

		private val log = PlatformUtil.getLog(ConvertPhotoToGrayscale::class.java)

		override fun doInBackground(vararg params: String?): Void? {
			val userOriginalPhotoPath = params[0]
			val userGrayScalePhotoPath = params[1]
			if (userOriginalPhotoPath != null && userGrayScalePhotoPath != null) {
				convertToGrayscaleAndSave(userOriginalPhotoPath, userGrayScalePhotoPath)
			}
			return null
		}

		fun convertToGrayscaleAndSave(coloredImagePath: String, newFilePath: String) {
			val currentImage = BitmapFactory.decodeFile(coloredImagePath)
			val grayscaleImage = toGrayscale(currentImage)
			saveBitmap(grayscaleImage, newFilePath)
		}

		private fun toGrayscale(bmpOriginal: Bitmap): Bitmap {
			val bmpGrayscale = Bitmap.createBitmap(bmpOriginal.width, bmpOriginal.height, Bitmap.Config.ARGB_8888)
			val c = Canvas(bmpGrayscale)
			val paint = Paint()
			val cm = ColorMatrix()
			cm.setSaturation(0f)
			val f = ColorMatrixColorFilter(cm)
			paint.colorFilter = f
			c.drawBitmap(bmpOriginal, 0f, 0f, paint)
			return bmpGrayscale
		}

		private fun saveBitmap(bitmap: Bitmap, newFilePath: String) {
			var fout: FileOutputStream? = null
			try {
				val file = File(newFilePath)
				if (file.parentFile != null) {
					file.parentFile.mkdirs()
				}
				fout = FileOutputStream(file)
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout)
			} catch (e: Exception) {
				log.error(e)
			} finally {
				try {
					fout?.close()
				} catch (e: IOException) {
					log.error(e)
				}
			}
		}
	}
}
