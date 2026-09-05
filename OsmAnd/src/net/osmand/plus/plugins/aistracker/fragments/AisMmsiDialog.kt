package net.osmand.plus.plugins.aistracker.fragments

import android.view.WindowManager
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin

/**
 * "Enter MMSI" dialog. An empty value is valid and clears the MMSI; anything between 1 and 8
 * digits is not a MMSI at all and is rejected.
 */
object AisMmsiDialog {

	private const val MMSI_LENGTH = 9

	@JvmStatic
	fun show(fragment: AisBaseFragment, onSaved: () -> Unit) {
		val plugin = PluginsHelper.requirePlugin(AisTrackerPlugin::class.java)
		val context = fragment.materialContext()
		val view = LayoutInflater.from(context).inflate(R.layout.dialog_ais_mmsi, null)
		val inputLayout: TextInputLayout = view.findViewById(R.id.mmsi_layout)
		val editText: TextInputEditText = view.findViewById(R.id.mmsi_edit)

		val savedMmsi = plugin.AIS_OWN_MMSI.get()
		val savedText = if (savedMmsi == 0) "" else savedMmsi.toString()
		editText.setText(savedText)
		editText.setSelection(savedText.length)

		val dialog = MaterialAlertDialogBuilder(context)
			.setTitle(R.string.ais_enter_mmsi)
			.setView(view)
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_save, null)
			.create()

		dialog.setOnShowListener {
			val saveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
			var validationRequested = false

			fun currentText() = editText.text?.toString().orEmpty()
			fun isValid() = currentText().isEmpty() || currentText().length == MMSI_LENGTH
			fun hasChanges() = currentText() != savedText

			fun updateState() {
				saveButton.isEnabled = isValid() && hasChanges()
				if (validationRequested) {
					inputLayout.error =
						if (isValid()) null else context.getString(R.string.ais_error_mmsi_length)
				}
			}

			editText.addTextChangedListener(object : android.text.TextWatcher {
				override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
				override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
				override fun afterTextChanged(s: android.text.Editable?) = updateState()
			})
			updateState()

			saveButton.setOnClickListener {
				validationRequested = true
				if (!isValid()) {
					updateState()
					return@setOnClickListener
				}
				val value = currentText().toIntOrNull() ?: 0
				plugin.AIS_OWN_MMSI.set(value)
				if (value == 0) {
					/* without a MMSI there is nothing to show on the map */
					plugin.AIS_DISPLAY_OWN_POSITION.set(false)
				}
				plugin.layer?.refreshOwnObjectVisibility()
				onSaved()
				dialog.dismiss()
			}

			editText.requestFocus()
			dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
		}
		dialog.show()
	}
}
