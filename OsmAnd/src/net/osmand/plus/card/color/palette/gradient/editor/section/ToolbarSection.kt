package net.osmand.plus.card.color.palette.gradient.editor.section

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorStaticUiData
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.helpers.AndroidUiHelper

class ToolbarSection(
	rootView: View,
	staticUiData: EditorStaticUiData,
	app: OsmandApplication,
	nightMode: Boolean
): UiSection(app, nightMode) {

	private val undoButton = rootView.findViewById<ImageView>(R.id.action_button)

	lateinit var onBackClicked: () -> Unit
	lateinit var onUndoClicked: () -> Unit

	init {
		val toolbar = rootView.findViewById<Toolbar>(R.id.toolbar)
		ViewCompat.setElevation(rootView.findViewById(R.id.appbar), 5.0f)
		toolbar.findViewById<TextView>(R.id.toolbar_title).text = staticUiData.toolbarTitle

		val subtitle = toolbar.findViewById<TextView>(R.id.toolbar_subtitle)
		subtitle.text = staticUiData.toolbarSubtitle
		AndroidUiHelper.updateVisibility(subtitle, true)

		val closeButton = toolbar.findViewById<ImageView>(R.id.close_button)
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close))
		closeButton.setOnClickListener { onBackClicked() }
		closeButton.contentDescription = getString(R.string.shared_string_close_the_dialog)

		undoButton.setImageDrawable(getIcon(R.drawable.ic_action_undo_dark))
		undoButton.setOnClickListener { onUndoClicked() }
		undoButton.contentDescription = getString(R.string.shared_string_undo)
		AndroidUiHelper.updateVisibility(undoButton, true)
	}

	override fun update(oldUiState: EditorUiState?, newUiState: EditorUiState) {
		val oldState = oldUiState?.toolbarState
		val newState = newUiState.toolbarState
		if (oldState?.showUndoButton != newState.showUndoButton) {
			AndroidUiHelper.updateVisibility(undoButton, newState.showUndoButton)
		}
	}
}