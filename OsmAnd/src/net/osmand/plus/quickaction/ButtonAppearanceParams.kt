package net.osmand.plus.quickaction

import android.os.Bundle

data class ButtonAppearanceParams(
	var iconName: String?,
	var size: Int,
	var opacity: Float,
	var cornerRadius: Int
) {

	fun readBundle(bundle: Bundle) {
		iconName = bundle.getString(ICON_NAME_KEY)
		size = bundle.getInt(SIZE_KEY)
		cornerRadius = bundle.getInt(CORNER_RADIUS_KEY)
		opacity = bundle.getFloat(OPACITY_KEY)
	}

	fun saveToBundle(bundle: Bundle) {
		bundle.putString(ICON_NAME_KEY, iconName)
		bundle.putInt(SIZE_KEY, size)
		bundle.putInt(CORNER_RADIUS_KEY, cornerRadius)
		bundle.putFloat(OPACITY_KEY, opacity)
	}

	companion object {
		internal const val SIZE_KEY = "size"
		internal const val ICON_NAME_KEY = "icon"
		internal const val OPACITY_KEY = "opacity"
		internal const val CORNER_RADIUS_KEY = "corner_radius"

		internal const val DEFAULT_ICON_ID = "ic_quick_action"
		internal const val DEFAULT_TOP_BUTTON_SIZE = 40
		internal const val DEFAULT_ACTION_BUTTON_SIZE = 48
		internal const val DEFAULT_BOTTOM_BUTTON_SIZE = 48
	}
}