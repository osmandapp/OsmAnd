package net.osmand.telegram.helpers

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication

object TelegramUiHelper {

	fun setupPhoto(app: TelegramApplication, iv: ImageView?, photoPath: String?) {
		if (iv == null) {
			return
		}
		var drawable: Drawable? = null
		var bitmap: Bitmap? = null
		if (photoPath != null && photoPath.isNotEmpty()) {
			bitmap = app.uiUtils.getCircleBitmap(photoPath)
		}
		if (bitmap == null) {
			drawable = app.uiUtils.getThemedIcon(R.drawable.ic_group)
		}
		if (bitmap != null) {
			iv.setImageBitmap(bitmap)
		} else {
			iv.setImageDrawable(drawable)
		}
	}
}
