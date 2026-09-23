package net.osmand.plus.plugins.aistracker.fragments

import android.app.AlertDialog.BUTTON_POSITIVE
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.aistracker.AisFormatter
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
		/* the profile the screen was opened for, not necessarily the active one */
		val appMode = fragment.appMode
		val context = fragment.materialContext()
		val view = LayoutInflater.from(context).inflate(R.layout.dialog_ais_mmsi, null)
		val inputLayout: TextInputLayout = view.findViewById(R.id.mmsi_layout)
		val editText: TextInputEditText = view.findViewById(R.id.mmsi_edit)

		val savedMmsi = plugin.AIS_OWN_MMSI.getModeValue(appMode)
		val savedText = if (savedMmsi == 0) "" else AisFormatter.formatMmsi(savedMmsi)
		editText.setText(savedText)
		editText.setSelection(savedText.length)

		val dialog = MaterialAlertDialogBuilder(context)
			.setTitle(R.string.ais_enter_mmsi)
			.setView(view)
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_save, null)
			.create()

		dialog.setOnShowListener {
			val saveButton = dialog.getButton(BUTTON_POSITIVE)
			var validationRequested = false

			fun currentText() = editText.text?.toString().orEmpty()
			fun isValid() = currentText().isEmpty() || currentText().length == MMSI_LENGTH
			fun hasChanges() = currentText() != savedText

			fun updateState() {
				val valid = isValid()
				/* the error is shown on Save and, from the first failed Save on, while typing */
				if (validationRequested) {
					inputLayout.error =
						if (valid) null else context.getString(R.string.ais_error_mmsi_length)
				}
				/* Save stays enabled until the first failed attempt - otherwise the error could
				 * never be shown - and follows the validity from then on */
				saveButton.isEnabled = hasChanges() && (valid || !validationRequested)
			}

			editText.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
				override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
				override fun afterTextChanged(s: Editable?) = updateState()
			})
			updateState()

			saveButton.setOnClickListener {
				validationRequested = true
				if (!isValid()) {
					updateState()
					return@setOnClickListener
				}
				val value = currentText().toIntOrNull() ?: 0
				plugin.AIS_OWN_MMSI.setModeValue(appMode, value)
				if (value == 0) {
					/* without a MMSI there is nothing to show on the map */
					plugin.AIS_DISPLAY_OWN_POSITION.setModeValue(appMode, false)
				}
				plugin.layer?.refreshOwnObjectVisibility()
				onSaved()
				dialog.dismiss()
			}

			editText.requestFocus()
		}
		/* the keyboard has to come up with the dialog - set before show(), on the window the
		 * dialog already has after create() */
		dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
		dialog.show()
	}
}
