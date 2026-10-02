package net.osmand.plus.views.layers.core

import android.graphics.Bitmap
import net.osmand.core.jni.SingleSkImage
import net.osmand.core.jni.SwigUtilities
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.NativeUtilities
import java.util.concurrent.ConcurrentHashMap

// The core asks a tile provider for the image of every point it draws, and every point needs its own SkImage
// (a shared one shows only the first icon), so only the pixels copied from the bitmap are kept.
// A cache that reaches maxSize starts over, so a long-lived provider with many different icons stays bounded.
class IconPixelsCache<K : Any> @JvmOverloads constructor(private val maxSize: Int = Int.MAX_VALUE) {

    private val icons = ConcurrentHashMap<K, IconData>()

    private class IconData(val width: Int, val height: Int, val pixels: ByteArray)

    fun getImage(key: K, createBitmap: (K) -> Bitmap?): SingleSkImage {
        val icon = icons[key] ?: createBitmap(key)?.let { bitmap ->
            IconData(bitmap.width, bitmap.height, AndroidUtils.getByteArrayFromBitmap(bitmap)).also {
                if (icons.size >= maxSize) {
                    icons.clear()
                }
                icons[key] = it
            }
        }
        return if (icon != null) {
            NativeUtilities.createSkImage(icon.width.toLong(), icon.height.toLong(), icon.pixels)
        } else {
            SwigUtilities.nullSkImage()
        }
    }
}
