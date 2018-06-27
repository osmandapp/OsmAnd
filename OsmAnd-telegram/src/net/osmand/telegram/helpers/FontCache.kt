package net.osmand.telegram.helpers

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "FontCache"
private const val ROBOTO_MEDIUM = "fonts/Roboto-Medium.ttf"
private const val ROBOTO_REGULAR = "fonts/Roboto-Regular.ttf"

object FontCache {

	private val fontsMap = ConcurrentHashMap<String, Typeface>()

	fun getRobotoMedium(context: Context): Typeface? {
		return getFont(context, ROBOTO_MEDIUM)
	}

	fun getRobotoRegular(context: Context): Typeface? {
		return getFont(context, ROBOTO_REGULAR)
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
