package net.osmand.telegram.helpers

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import net.osmand.telegram.R
import java.util.concurrent.ConcurrentHashMap

object FontCache {

	private const val TAG = "FontCache"

	private val fontsMap = ConcurrentHashMap<String, Typeface>()

	fun getRobotoMedium(context: Context): Typeface? {
		return getFont(context, context.getString(R.string.font_roboto_medium))
	}

	fun getRobotoRegular(context: Context): Typeface? {
		return getFont(context, context.getString(R.string.font_roboto_regular))
	}

	fun getRobotoMonoBold(context: Context): Typeface? {
		return getFont(context, context.getString(R.string.font_roboto_mono_bold))
	}

	fun getFont(context: Context, fontName: String): Typeface? {
		var typeface: Typeface? = fontsMap[fontName]
		if (typeface != null) {
			return typeface
		}

		try {
			typeface = Typeface.createFromAsset(context.assets, fontName)
		} catch (e: Exception) {
			Log.e(TAG, "Failed to create typeface from asset '$fontName'", e)
			return null
		}

		if (typeface == null) {
			return null
		}

		fontsMap[fontName] = typeface

		return typeface
	}
}
