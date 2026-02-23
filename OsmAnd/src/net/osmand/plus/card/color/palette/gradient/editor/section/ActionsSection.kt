package net.osmand.plus.card.color.palette.gradient.editor.section

import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

class ActionsSection(
	rootView: View,
	app: OsmandApplication,
	nightMode: Boolean,
	private val onRemoveClicked: () -> Unit
) : UiSection(app, nightMode)  {

	private val removeButton: View = rootView.findViewById(R.id.remove_action)
	private val title: TextView = removeButton.findViewById(R.id.title)
	private val icon: ImageView = removeButton.findViewById(R.id.icon)

	init {
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		UiUtilities.setupListItemBackground(rootView.context, removeButton, activeColor)
	}

	override fun update(oldUiState: EditorUiState?, newUiState: EditorUiState) {
		val oldState = oldUiState?.removeButtonState
		val newState = newUiState.removeButtonState
		if (oldState?.enabled != newState.enabled) {
			val enabled = newState.enabled

			val iconColor = if (enabled) {
				ColorUtilities.getColor(app, R.color.design_default_color_error)
			} else {
				ColorUtilities.getDefaultIconColor(app, nightMode)
			}

			val textColor = if (enabled) {
				ColorUtilities.getPrimaryTextColor(app, nightMode)
			} else {
				ColorUtilities.getSecondaryTextColor(app, nightMode)
			}

			title.setTextColor(textColor)
			icon.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY)
			removeButton.setOnClickListener(
				if (enabled) {
					{ onRemoveClicked() }
				} else null
			)
		}
	}
}