package net.osmand.plus.views.layers.core

import android.graphics.Bitmap
import net.osmand.core.jni.SingleSkImage
import net.osmand.core.jni.SwigUtilities
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.NativeUtilities
import java.util.concurrent.ConcurrentHashMap

// The core asks a tile provider for the image of every point it draws, and every point needs its own SkImage
// (a shared one shows only the first icon), so only the pixels copied from the bitmap are kept
class IconPixelsCache<K : Any> {

    private val icons = ConcurrentHashMap<K, IconData>()

    private class IconData(val width: Int, val height: Int, val pixels: ByteArray)

    fun getImage(key: K, bitmapSupplier: () -> Bitmap?): SingleSkImage {
        val icon = icons[key] ?: bitmapSupplier()?.let { bitmap ->
            IconData(bitmap.width, bitmap.height, AndroidUtils.getByteArrayFromBitmap(bitmap)).also { icons[key] = it }
        }
        return if (icon != null) {
            NativeUtilities.createSkImage(icon.width.toLong(), icon.height.toLong(), icon.pixels)
        } else {
            SwigUtilities.nullSkImage()
        }
    }
}
