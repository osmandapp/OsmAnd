package net.osmand.plus.myplaces.tracks.controller

import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.base.dialog.data.DisplayData
import net.osmand.plus.base.dialog.data.DisplayItem
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider
import net.osmand.plus.settings.bottomsheets.CustomizableOptionsBottomSheet
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.shared.gpx.data.OrganizedTracksGroup

class OrganizedTracksOptionsController(
	private val app: OsmandApplication,
	private val organizedTracksGroup: OrganizedTracksGroup
) : BaseDialogController(app), IDisplayDataProvider, IDialogItemClicked {

	companion object {

		const val PROCESS_ID = "organized_tracks_options"

		fun showDialog(
			app: OsmandApplication,
			fragmentManager: FragmentManager,
			organizedTracksGroup: OrganizedTracksGroup,
			listener: OrganizedTracksOptionsListener?
		) {
			val controller = OrganizedTracksOptionsController(app, organizedTracksGroup)
			controller.setOrganizedTracksOptionsListener(listener)
			val dialogManager = app.dialogManager
			dialogManager.register(PROCESS_ID, controller)
			CustomizableOptionsBottomSheet.showInstance(fragmentManager, PROCESS_ID, false)
		}
	}

	override fun getProcessId() = PROCESS_ID

	private var optionsListener: OrganizedTracksOptionsListener? = null

	fun setOrganizedTracksOptionsListener(listener: OrganizedTracksOptionsListener?) {
		optionsListener = listener
	}

	override fun getDisplayData(processId: String): DisplayData {
		val iconsCache = app.uiUtilities
		val displayData = DisplayData()
		val nightMode = app.daynightHelper.isNightMode(ThemeUsageContext.APP)

		displayData.addDisplayItem(
			DisplayItem()
				.setTitle(organizedTracksGroup.getName())
				.setDescription("${organizedTracksGroup.getTrackItems().size} ${app.getString(R.string.shared_string_tracks)}")
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_72dp)
				.setIcon(iconsCache.getActiveIcon(R.drawable.ic_action_folder_smart, nightMode))
				.setShowBottomDivider(true, 0)
		)
		val dividerPadding = calculateSubtitleDividerPadding()
		for (listOption in OrganizedTracksOption.entries) {
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
		if (item.tag !is OrganizedTracksOption) {
			return
		}
		val option = item.tag as OrganizedTracksOption
		when (option) {
			OrganizedTracksOption.DETAILS -> {
				showDetails(organizedTracksGroup)
			}

			OrganizedTracksOption.SHOW_ALL_TRACKS -> {
				showTracksOnMap(organizedTracksGroup)
			}

			OrganizedTracksOption.EXPORT -> {
				showExportDialog(organizedTracksGroup)
			}
		}
	}

	private fun showDetails(folder: OrganizedTracksGroup) {
		dialogManager.askDismissDialog(PROCESS_ID)
		optionsListener?.showOrganizedTracksDetails(folder)
	}

	private fun showTracksOnMap(folder: OrganizedTracksGroup) {
		dialogManager.askDismissDialog(PROCESS_ID)
		optionsListener?.showOrganizedTracksOnMap(folder)
	}

	private fun showExportDialog(folder: OrganizedTracksGroup) {
		dialogManager.askDismissDialog(PROCESS_ID)
		optionsListener?.showExportDialog(folder)
	}

	private fun calculateSubtitleDividerPadding(): Int {
		val contentPadding = getDimension(R.dimen.content_padding)
		val iconWidth = getDimension(R.dimen.standard_icon_size)
		return contentPadding * 3 + iconWidth
	}
}