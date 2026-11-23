package net.osmand.plus.mapcontextmenu.editors

import android.os.Bundle
import androidx.annotation.ColorInt
import net.osmand.data.BackgroundType

/**
 * The parts of a point's appearance the user changed by hand in the point editor.
 * On a folder change the point takes the folder's icon, colour and shape,
 * except for what the user changed (#25404).
 */
class ManualAppearanceChanges {

	var isIconChanged = false
		private set
	var isColorChanged = false
		private set
	var isShapeChanged = false
		private set

	fun onIconChanged() {
		isIconChanged = true
	}

	fun onColorChanged() {
		isColorChanged = true
	}

	fun onShapeChanged() {
		isShapeChanged = true
	}

	fun iconOnFolderChange(currentIcon: String, folderIcon: String): String =
		if (isIconChanged) currentIcon else folderIcon

	@ColorInt
	fun colorOnFolderChange(@ColorInt currentColor: Int, @ColorInt folderColor: Int): Int =
		if (isColorChanged) currentColor else folderColor

	fun shapeOnFolderChange(currentShape: BackgroundType, folderShape: BackgroundType): BackgroundType =
		if (isShapeChanged) currentShape else folderShape

	fun saveState(outState: Bundle) {
		outState.putBoolean(ICON_CHANGED_KEY, isIconChanged)
		outState.putBoolean(COLOR_CHANGED_KEY, isColorChanged)
		outState.putBoolean(SHAPE_CHANGED_KEY, isShapeChanged)
	}

	fun restoreState(savedState: Bundle) {
		isIconChanged = savedState.getBoolean(ICON_CHANGED_KEY, isIconChanged)
		isColorChanged = savedState.getBoolean(COLOR_CHANGED_KEY, isColorChanged)
		isShapeChanged = savedState.getBoolean(SHAPE_CHANGED_KEY, isShapeChanged)
	}

	companion object {
		private const val ICON_CHANGED_KEY = "editor_icon_changed_by_user"
		private const val COLOR_CHANGED_KEY = "editor_color_changed_by_user"
		private const val SHAPE_CHANGED_KEY = "editor_shape_changed_by_user"
	}
}