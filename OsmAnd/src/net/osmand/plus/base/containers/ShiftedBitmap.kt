package net.osmand.plus.base.containers

import android.graphics.Bitmap

data class ShiftedBitmap @JvmOverloads constructor(
    val bitmap: Bitmap,
    val marginX: Float,
    val marginY: Float,
    val scale: Float? = null
)
