package net.osmand.plus.myplaces.tracks.controller

import android.content.DialogInterface
import android.widget.EditText
import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.base.dialog.data.DisplayData
import net.osmand.plus.base.dialog.data.DisplayItem
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider
import net.osmand.plus.settings.bottomsheets.CustomizableOptionsBottomSheet
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.AlertDialogExtra
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.util.Algorithms

class SmartFolderOptionsController(
	private val app: OsmandApplication,
	private val smartFolder: SmartFolder) : BaseDialogController(
	app), IDisplayDataProvider, IDialogItemClicked {
	private var optionsListener: SmartFolderOptionsListener? = null
	fun setSmartFolderOptionsListener(listener: SmartFolderOptionsListener?) {
		optionsListener = listener
	}

	override fun getProcessId(): String {
		return PROCESS_ID
	}

	override fun getDisplayData(processId: String): DisplayData? {
		val iconsCache = app.uiUtilities
		val displayData = DisplayData()
		iconsCache.getActiveIcon(R.drawable.ic_action_folder_smart, app.daynightHelper.isNightMode)
		displayData.addDisplayItem(
			DisplayItem()
				.setTitle(smartFolder.getName())
				.setDescription("${smartFolder.getTrackItems().size} ${app.getString(R.string.shared_string_tracks)}")
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_72dp)
				.setIcon(
					iconsCache.getActiveIcon(
						R.drawable.ic_action_folder_smart,
						app.daynightHelper.isNightMode))
				.setShowBottomDivider(true, 0)
		)
		val dividerPadding = calculateSubtitleDividerPadding()
		for (listOption in SmartFolderOption.availableOptions) {
			displayData.addDisplayItem(
				DisplayItem()
					.setTitle(getString(listOption.titleId))
					.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
					.setIcon(iconsCache.getThemedIcon(listOption.iconId))
					.setShowBottomDivider(listOption.shouldShowBottomDivider(), dividerPadding)
					.setTag(listOption)
			)
		}
		return displayData
	}

	override fun onDialogItemClicked(processId: String, item: DisplayItem) {
		if (item.tag !is SmartFolderOption) {
			return
		}
		val option = item.tag as SmartFolderOption
		when (option) {
			SmartFolderOption.DETAILS -> {
				showDetails(smartFolder)
			}

			SmartFolderOption.SHOW_ALL_TRACKS -> {
				showSmartFolderTracksOnMap(smartFolder)
			}

			SmartFolderOption.shared_string_rename -> {
				showRenameDialog()
			}

			SmartFolderOption.REFRESH -> {
				app.smartFolderHelper.refreshSmartFolder(smartFolder)
				dialogManager.askRefreshDialogCompletely(PROCESS_ID)
			}

			SmartFolderOption.EDIT_FILTER -> {
				showEditFiltersDialog(smartFolder)
			}

			SmartFolderOption.EXPORT -> {
				showExportDialog(smartFolder)
			}

			SmartFolderOption.DELETE_FOLDER -> {
				showDeleteDialog()
			}
		}
	}

	private fun showDetails(folder: SmartFolder) {
		dialogManager.askDismissDialog(PROCESS_ID)
		optionsListener?.showSmartFolderDetails(folder)
	}

	private fun showSmartFolderTracksOnMap(folder: SmartFolder) {
		dialogManager.askDismissDialog(PROCESS_ID)
		optionsListener?.showSmartFolderTracksOnMap(folder)
	}

	private fun showRenameDialog() {
		val activity = activity
		if (activity != null) {
			val dialogData = AlertDialogData(activity, isNightMode)
				.setTitle(R.string.shared_string_rename)
				.setNegativeButton(R.string.shared_string_cancel, null)
			dialogData.setPositiveButton(R.string.shared_string_apply) { dialog: DialogInterface?, which: Int ->
				val extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT)
				if (extra is EditText) {
					val newName = extra.text.toString()
					if (Algorithms.isBlank(newName)) {
						app.showToastMessage(R.string.empty_name)
					} else {
						if (app.smartFolderHelper.isSmartFolderPresent(newName)) {
							app.showToastMessage(R.string.smart_folder_name_present)
						} else {
							app.smartFolderHelper.renameSmartFolder(smartFolder, newName)
							onSmartFolderRenamed(smartFolder)
						}
					}
				}
			}
			val caption = activity.getString(R.string.enter_new_name)
			CustomAlert.showInput(dialogData, activity, smartFolder.getName(), caption)
		}
	}

	private fun onSmartFolderRenamed(folder: SmartFolder) {
		dialogManager.askRefreshDialogCompletely(PROCESS_ID)
	}

	private fun showEditFiltersDialog(folder: SmartFolder) {
		dialogManager.askDismissDialog(PROCESS_ID)
		optionsListener?.showEditFiltersDialog(folder)
	}

	private fun showExportDialog(folder: SmartFolder) {
		dialogManager.askDismissDialog(PROCESS_ID)
		optionsListener?.showExportDialog(folder)
	}

	private fun showDeleteDialog() {
		val ctx = context
		if (ctx != null) {
			val dialogData = AlertDialogData(ctx, isNightMode)
				.setTitle(R.string.delete_folder_question)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete) { dialog: DialogInterface?, which: Int ->
					app.smartFolderHelper.deleteSmartFolder(smartFolder)
					onSmartFolderDeleted()
				}
			val folderName = smartFolder.getName()
			val message =
				ctx.getString(R.string.delete_smart_folder_dialog_message, folderName)
			CustomAlert.showSimpleMessage(dialogData, message)
		}
	}


	private fun onSmartFolderDeleted() {
		dialogManager.askDismissDialog(PROCESS_ID)
	}

	private fun calculateSubtitleDividerPadding(): Int {
		val contentPadding = getDimension(R.dimen.content_padding)
		val iconWidth = getDimension(R.dimen.standard_icon_size)
		return contentPadding * 3 + iconWidth
	}

	companion object {
		const val PROCESS_ID = "smart_folder_options"
		fun showDialog(
			app: OsmandApplication, fragmentManager: FragmentManager, folder: SmartFolder,
			listener: SmartFolderOptionsListener?) {
			val controller = SmartFolderOptionsController(app, folder)
			controller.setSmartFolderOptionsListener(listener)
			val dialogManager = app.dialogManager
			dialogManager.register(PROCESS_ID, controller)
			CustomizableOptionsBottomSheet.showInstance(fragmentManager, PROCESS_ID, false)
		}
	}
}