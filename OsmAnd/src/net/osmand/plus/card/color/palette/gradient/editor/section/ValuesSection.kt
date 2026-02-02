package net.osmand.plus.card.color.palette.gradient.editor.section

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.helpers.AndroidUiHelper

class ValuesSection(
	rootView: View,
	app: OsmandApplication,
	nightMode: Boolean,
	private val onValueChanged: (String) -> Unit
): UiSection(app, nightMode) {
	private val container: View = rootView.findViewById(R.id.text_field)
	private val editText: TextInputEditText = rootView.findViewById(R.id.text_edit)
	private val unitText: TextView = rootView.findViewById(R.id.unit)
	private val summaryText: TextView = rootView.findViewById(R.id.summary)

	private var isSelfUpdate = false

	init {
		editText.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				if (isSelfUpdate) return
				onValueChanged(s.toString())
			}
		})
	}

	private fun setInteractable(enabled: Boolean) {
		editText.isEnabled = enabled
		container.alpha = if (enabled) 1.0f else 0.5f
	}

	override fun update(oldUiState: EditorUiState?, newUiState: EditorUiState) {
		val oldState = oldUiState?.valueState
		val newState = newUiState.valueState
		if (oldState?.label != newState.label) {
			unitText.text = newState.label
		}
		if (oldState?.text != newState.text && newState.text != null) {
			setTextSilently(newState.text)
		}
		if (oldState?.interactable != newState.interactable) {
			setInteractable(newState.interactable)
		}
		if (oldState?.showTextField != newState.showTextField) {
			AndroidUiHelper.updateVisibility(container, newState.showTextField)
		}
		if (oldState?.summaryText != newState.summaryText) {
			summaryText.text = newState.summaryText
		}
		if (oldState?.summaryColor != newState.summaryColor) {
			summaryText.setTextColor(newState.summaryColor)
		}
	}

	private fun setTextSilently(text: String) {
		isSelfUpdate = true
		editText.setText(text)
		isSelfUpdate = false
	}
}