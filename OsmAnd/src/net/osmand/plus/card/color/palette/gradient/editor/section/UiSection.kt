package net.osmand.plus.card.color.palette.gradient.editor.section

import android.graphics.drawable.Drawable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState

abstract class UiSection(
	val app: OsmandApplication,
	val nightMode: Boolean
) {
	fun getIcon(iconId: Int): Drawable? = app.uiUtilities.getIcon(iconId)

	fun getString(resId: Int, vararg formatArgs: Any?) = app.getString(resId, formatArgs)

	abstract fun update(oldUiState: EditorUiState?, newUiState: EditorUiState)
}