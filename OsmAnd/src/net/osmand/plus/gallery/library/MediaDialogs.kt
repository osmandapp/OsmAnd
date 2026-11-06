package net.osmand.plus.gallery.library

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.mediastorage.MediaStorageHelper
import net.osmand.plus.settings.mediastorage.MediaStorageLocation
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.shared.gpx.primitives.Link

object MediaDialogs {
	private enum class Kind(val iconId: Int, val actionId: Int) {
		DELETE(R.drawable.ic_action_delete_outlined, R.string.shared_string_delete),
		DETACH(R.drawable.ic_action_attachment_remove, R.string.shared_string_detach),
		DETACH_EXTERNAL(R.drawable.ic_action_attachment_remove, R.string.shared_string_detach);

		fun title(count: Int) = when (this) {
			DELETE -> if (count == 1) R.string.delete_media_title else R.string.delete_media_title_plural
			else -> if (count == 1) R.string.detach_media_title else R.string.detach_media_title_plural
		}

		fun message(count: Int) = when (this) {
			DELETE -> if (count == 1) R.string.delete_media_descr else R.string.delete_media_descr_plural
			DETACH -> if (count == 1) R.string.detach_media_descr else R.string.detach_media_descr_plural
			DETACH_EXTERNAL -> if (count == 1) R.string.detach_external_media_descr else R.string.detach_external_media_descr_plural
		}
	}

	fun delete(context: Context, count: Int, nightMode: Boolean, onConfirm: () -> Unit) =
		confirm(context, Kind.DELETE, count, nightMode, onConfirm)

	fun detach(context: Context, links: List<Link>, nightMode: Boolean, onConfirm: () -> Unit) {
		val app = context.applicationContext as OsmandApplication
		val storage = MediaStorageHelper(app)
		val location = MediaStorageLocation.fromSettings(app)
		val staysInLibrary = links.all { storage.isInMediaFolder(location, it.href) }
		confirm(context, if (staysInLibrary) Kind.DETACH else Kind.DETACH_EXTERNAL, links.size, nightMode, onConfirm)
	}

	fun rename(context: Context, name: String, nightMode: Boolean, onRename: (String) -> Unit) {
		val materialContext = UiUtilities.getThemedContext(context, nightMode, R.style.OsmandMaterialLightTheme, R.style.OsmandMaterialDarkTheme)
		val view = LayoutInflater.from(materialContext).inflate(R.layout.dialog_media_rename, null)
		val inputLayout: TextInputLayout = view.findViewById(R.id.name_layout)
		val editText: TextInputEditText = view.findViewById(R.id.name_edit)
		editText.setText(name)
		editText.setSelection(name.length)
		editText.doAfterTextChanged { if (!it.isNullOrBlank()) inputLayout.error = null }

		val dialog = MaterialAlertDialogBuilder(materialContext)
			.setTitle(R.string.shared_string_rename)
			.setView(view)
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_apply, null)
			.create()
		dialog.setOnShowListener {
			// Set here so that an empty name keeps the dialog open with the error under the field
			dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
				val newName = editText.text?.toString()?.trim().orEmpty()
				if (newName.isEmpty()) {
					inputLayout.error = context.getString(R.string.empty_name)
				} else {
					dialog.dismiss()
					onRename(newName)
				}
			}
			editText.requestFocus()
		}
		dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
		dialog.show()
	}

	private fun confirm(context: Context, kind: Kind, count: Int, nightMode: Boolean, onConfirm: () -> Unit) {
		if (count == 0) return
		val app = context.applicationContext as OsmandApplication
		val activeColor = ColorUtilities.getActiveColor(context, nightMode)
		val data = AlertDialogData(context, nightMode)
			.setIcon(app.uiUtilities.getPaintedIcon(kind.iconId, ColorUtilities.getDefaultIconColor(context, nightMode)))
			.setTitle(context.getString(kind.title(count), count))
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setNegativeButtonTextColor(activeColor)
			.setPositiveButton(kind.actionId) { _, _ -> onConfirm() }
			.setPositiveButtonTextColor(if (kind == Kind.DELETE) ColorUtilities.getColor(context, ColorUtilities.getWarningColorId(nightMode)) else activeColor)
		CustomAlert.showSimpleMessage(data, kind.message(count))
	}
}
