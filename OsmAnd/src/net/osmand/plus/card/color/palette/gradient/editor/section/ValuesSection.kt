package net.osmand.plus.card.color.palette.gradient.editor.section

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.helpers.AndroidUiHelper

@SuppressLint("ClickableViewAccessibility")
class ValuesSection(
	rootView: View,
	app: OsmandApplication,
	nightMode: Boolean,
	private val onValueChanged: (String) -> Unit
): UiSection(app, nightMode) {
	private val container: View = rootView.findViewById(R.id.text_field)
	private val caption: TextInputLayout = rootView.findViewById(R.id.text_caption)
	private val editText: TextInputEditText = rootView.findViewById(R.id.text_edit)
	private val unitText: TextView = rootView.findViewById(R.id.unit)
	private val summaryText: TextView = rootView.findViewById(R.id.summary)

	private var isSelfUpdate = false

	init {
		editText.clearFocus()
		editText.setOnTouchListener { v, event ->
			if (event.action == MotionEvent.ACTION_UP) {
				v.performClick()
			}
			editText.onTouchEvent(event)
			editText.setSelection(editText.text?.length ?: 0)
			true
		}
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
		if (oldState?.summary != newState.summary) {
			summaryText.text = newState.summary
		}
		AndroidUiHelper.updateVisibility(summaryText, !newState.summary.isNullOrEmpty())
		if (oldState?.error != newState.error) {
			if (newState.error != null) {
				caption.isErrorEnabled = true
				caption.error = newState.error
			} else {
				caption.error = null
				caption.isErrorEnabled = false
			}
		}
	}

	private fun setTextSilently(text: String) {
		isSelfUpdate = true
		editText.setText(text)
		editText.setSelection(editText.text?.length ?: 0)
		isSelfUpdate = false
	}
}