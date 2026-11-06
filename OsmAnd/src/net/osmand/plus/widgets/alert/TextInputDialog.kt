package net.osmand.plus.widgets.alert

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.plus.R
import net.osmand.plus.utils.UiUtilities

/**
 * Material 3 dialog with one outlined text field: the Material 3 counterpart of
 * [CustomAlert.showInput].
 *
 * The positive button passes the text to `onSave` only when [validator] returns no error;
 * otherwise the error is shown under the field and the dialog stays open. The error is cleared
 * as soon as the text is valid again.
 */
class TextInputDialog(
	private val context: Context,
	private val nightMode: Boolean,
	@StringRes private val titleId: Int,
	@StringRes private val hintId: Int
) {
	var initialText = ""

	@StringRes
	var positiveButtonId = R.string.shared_string_apply

	/** Returns the error to show under the field, or null when the text can be saved. */
	var validator: (String) -> String? = { null }

	fun show(onSave: (String) -> Unit) {
		val materialContext = UiUtilities.getThemedContext(context, nightMode,
			R.style.OsmandMaterialLightTheme, R.style.OsmandMaterialDarkTheme)
		val view = LayoutInflater.from(materialContext).inflate(R.layout.dialog_text_input, null)
		val inputLayout: TextInputLayout = view.findViewById(R.id.text_input)
		val editText: TextInputEditText = view.findViewById(R.id.text_input_edit)
		inputLayout.setHint(hintId)
		editText.setText(initialText)
		editText.setSelection(initialText.length)
		editText.doAfterTextChanged {
			if (inputLayout.error != null && validator(it?.toString().orEmpty()) == null) inputLayout.error = null
		}

		val dialog = MaterialAlertDialogBuilder(materialContext)
			.setTitle(titleId)
			.setView(view)
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(positiveButtonId, null)
			.create()
		dialog.setOnShowListener {
			// Set here so that an invalid text keeps the dialog open with the error under the field
			dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
				val text = editText.text?.toString().orEmpty()
				val error = validator(text)
				inputLayout.error = error
				if (error == null) {
					dialog.dismiss()
					onSave(text)
				}
			}
			editText.requestFocus()
		}
		dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
		dialog.show()
	}
}
