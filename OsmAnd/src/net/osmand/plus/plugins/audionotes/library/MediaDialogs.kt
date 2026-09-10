package net.osmand.plus.plugins.audionotes.library

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.mediastorage.MediaStorageHelper
import net.osmand.plus.settings.mediastorage.MediaStorageLocation
import net.osmand.shared.gpx.primitives.Link
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

object MediaDialogs {
	fun delete(context: Context, count: Int, nightMode: Boolean, onConfirm: () -> Unit) =
		confirm(context, count, nightMode, true, true, onConfirm)

	fun detach(context: Context, links: List<Link>, nightMode: Boolean, onConfirm: () -> Unit) {
		val app = context.applicationContext as OsmandApplication
		val helper = MediaStorageHelper(app)
		val location = MediaStorageLocation.fromSettings(app)
		val allManaged = links.all { helper.resolveManagedMediaSource(location, it.href) != null }
		confirm(context, links.size, nightMode, false, allManaged, onConfirm)
	}

	private fun confirm(context: Context, count: Int, nightMode: Boolean, delete: Boolean,
		allManaged: Boolean, onConfirm: () -> Unit) {
		if (count == 0) return
		val app = context.applicationContext as OsmandApplication
		val title = if (delete) {
			if (count == 1) R.string.delete_media_title else R.string.delete_media_title_plural
		} else if (count == 1) R.string.detach_media_title else R.string.detach_media_title_plural
		val message = if (delete) {
			if (count == 1) R.string.delete_media_descr else R.string.delete_media_descr_plural
		} else if (!allManaged) {
			if (count == 1) R.string.detach_external_media_descr else R.string.detach_external_media_descr_plural
		} else if (count == 1) R.string.detach_media_descr else R.string.detach_media_descr_plural
		val dialog = MaterialAlertDialogBuilder(UiUtilities.getThemedContext(context, nightMode,
			R.style.OsmandMaterialLightTheme, R.style.OsmandMaterialDarkTheme),
			com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
			.setIcon(app.uiUtilities.getPaintedIcon(
				if (delete) R.drawable.ic_action_delete_outlined else R.drawable.ic_action_attachment_remove,
				ColorUtilities.getDefaultIconColor(context, nightMode)))
			.setTitle(context.getString(title, count))
			.setMessage(message)
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(if (delete) R.string.shared_string_delete else R.string.shared_string_detach) { _, _ -> onConfirm() }
			.create()
		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ColorUtilities.getActiveColor(context, nightMode))
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(if (delete)
				ColorUtilities.getColor(context, ColorUtilities.getWarningColorId(nightMode)) else ColorUtilities.getActiveColor(context, nightMode))
		}
		dialog.show()
	}
}
