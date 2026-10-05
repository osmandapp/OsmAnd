package net.osmand.plus.views.layers.core

import android.graphics.Bitmap
import net.osmand.core.jni.SingleSkImage
import net.osmand.core.jni.SwigUtilities
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.NativeUtilities
import java.util.concurrent.ConcurrentHashMap

// A provider can live long while the map shows more and more icons, and an icon grows with the square of the text
// scale, so a cache whose icons would take more than this starts over.
private const val MAX_BYTES = 16L * 1024 * 1024

// The core asks a tile provider for the image of every point it draws, and every point needs its own SkImage
// (a shared one shows only the first icon), so only the pixels copied from the bitmap are kept.
class IconPixelsCache<K : Any> {

	private val icons = ConcurrentHashMap<K, IconData>()

	private class IconData(val width: Int, val height: Int, val pixels: ByteArray)

	fun getImage(key: K, createBitmap: (K) -> Bitmap?): SingleSkImage {
		val icon = icons[key] ?: createBitmap(key)?.let { bitmap ->
			IconData(bitmap.width, bitmap.height, AndroidUtils.getByteArrayFromBitmap(bitmap)).also { newIcon ->
				if (icons.values.sumOf { it.pixels.size.toLong() } + newIcon.pixels.size > MAX_BYTES) {
					icons.clear()
				}
				icons[key] = newIcon
			}
		}
		return if (icon != null) {
			NativeUtilities.createSkImage(icon.width.toLong(), icon.height.toLong(), icon.pixels)
		} else {
			SwigUtilities.nullSkImage()
		}
	}
}
