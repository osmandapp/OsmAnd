package net.osmand.plus.myplaces.tracks.controller

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.containers.ScreenItem
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.DIALOG_SUMMARY
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.DIVIDER_FULL
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.DIVIDER_WITH_PADDING
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.GROUP_HEADER
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.SELECTABLE_ITEM
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByAdapter.Companion.SPACE
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByFragment
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.shared.gpx.organization.OrganizeByParams
import net.osmand.shared.gpx.organization.OrganizeByRangeParams
import net.osmand.shared.gpx.organization.enums.OrganizeByCategory
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import net.osmand.util.CollectionUtils

class OrganizeTracksByController(
	val app: OsmandApplication,
	val appMode: ApplicationMode,
	private val folderId: String
) : BaseDialogController(app) {

	companion object {
		const val PROCESS_ID = "select_organize_tracks_by_type"

		fun showDialog(
			app: OsmandApplication,
			fragmentManager: FragmentManager,
			folderId: String,
			appMode: ApplicationMode
		) {
			val controller = OrganizeTracksByController(app, appMode, folderId)
			app.dialogManager.register(PROCESS_ID, controller)
			if (!OrganizeTracksByFragment.showInstance(fragmentManager, appMode)) {
				app.dialogManager.unregister(PROCESS_ID)
			}
		}

		fun getExistedInstance(app: OsmandApplication): OrganizeTracksByController? {
			return app.dialogManager.findController(PROCESS_ID) as? OrganizeTracksByController
		}
	}

	var fragmentActivity: FragmentActivity? = null

	// Current selection state (null represents "None")
	var selectedType: OrganizeByType? = null
		private set

	init {
		val savedType = app.smartFolderHelper.getSmartFolderById(folderId)?.getOrganizeByType()
		selectedType = savedType
	}

	override fun getProcessId(): String = PROCESS_ID

	fun populateScreenItems(): List<ScreenItem> {
		val items = mutableListOf<ScreenItem>()

		items.add(ScreenItem(DIALOG_SUMMARY))
		//'None' option represented as a 'null' value
		items.add(ScreenItem(SELECTABLE_ITEM, null))

		for (category in OrganizeByCategory.entries) {
			items.add(ScreenItem(DIVIDER_FULL))
			items.add(ScreenItem(GROUP_HEADER, category))
			for (type in OrganizeByType.valuesOf(category)) {
				items.add(ScreenItem(SELECTABLE_ITEM, type))
				if (shouldAddParagraphDivider(type)) {
					items.add(ScreenItem(DIVIDER_WITH_PADDING))
				}
			}
		}
		items.add(ScreenItem(DIVIDER_FULL))
		items.add(ScreenItem(SPACE))
		return items
	}

	private fun shouldAddParagraphDivider(type: OrganizeByType): Boolean {
		return CollectionUtils.equalsToAny(
			type,
			OrganizeByType.AVG_ALTITUDE, OrganizeByType.SENSOR_SPEED_AVG,
			OrganizeByType.HEART_RATE_AVG, OrganizeByType.CADENCE_AVG, OrganizeByType.POWER_AVG
		)
	}

	fun selectType(type: OrganizeByType?) {
		if (selectedType != type) {
			selectedType = type
			dialogManager.askRefreshDialogCompletely(PROCESS_ID)
		}
	}

	fun askSaveChanges(activity: FragmentActivity?) {
		val type = selectedType
		val newParams: OrganizeByParams? = if (type != null) {
			if (type.isRangeRelated()) {
				val currentParams = app.smartFolderHelper.getOrganizeByParams(folderId)
				if (currentParams != null
					&& currentParams.type == type
					&& currentParams is OrganizeByRangeParams
					&& currentParams.stepSize > 0) {
					// Preserve existing step size if the type hasn't changed and the step is valid
					OrganizeByRangeParams(type, currentParams.stepSize)
				} else {
					// Reset to default step size if type changed or previous state was invalid
					OrganizeByRangeParams(type, type.getDefaultStepInBaseUnits())
				}
			} else {
				// Non-range types don't require additional parameters
				OrganizeByParams(type)
			}
		} else {
			// Selection cleared (None)
			null
		}

		app.smartFolderHelper.setOrganizeByParams(folderId, newParams)
		showStepSizeDialogIfNeeded(activity)
	}

	private fun showStepSizeDialogIfNeeded(activity: FragmentActivity?) {
		val type = selectedType ?: return
		if (type.isRangeRelated()) {
			val manager = activity?.supportFragmentManager ?: return
			val params = app.smartFolderHelper.getOrganizeByParams(folderId)
			if (params is OrganizeByRangeParams) {
				val stepSize = type.getDisplayUnits().fromBase(params.stepSize).toInt()
				OrganizeTracksStepController.showDialog(app, manager, appMode, folderId, type, stepSize)
			}
		}
	}
}