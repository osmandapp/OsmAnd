package net.osmand.plus.myplaces.tracks.controller

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.containers.Limits
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.base.dialog.data.DialogExtra
import net.osmand.plus.base.dialog.data.DisplayData
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.bottomsheets.ModernSliderBottomSheet
import net.osmand.plus.settings.controllers.IModernSliderDialogController
import net.osmand.shared.gpx.organization.OrganizeByRangeParams
import net.osmand.shared.gpx.organization.enums.OrganizeByType

class OrganizeTracksStepController(
	val app: OsmandApplication,
	private val folderId: String,
	private val organizeByType: OrganizeByType,
	private val initialValue: Int
) : BaseDialogController(app), IModernSliderDialogController {

	companion object {
		const val PROCESS_ID = "select_step_to_organize_tracks"

		fun showDialog(
			app: OsmandApplication,
			fragmentManager: FragmentManager,
			appMode: ApplicationMode,
			folderId: String,
			type: OrganizeByType,
			initialValue: Int
		) {
			val controller = OrganizeTracksStepController(app, folderId, type, initialValue)
			app.dialogManager.register(PROCESS_ID, controller)
			ModernSliderBottomSheet.showInstance(fragmentManager, appMode, PROCESS_ID)
		}
	}

	private var selectedValue = organizeByType.stepRange!!.clamp(initialValue).toInt()
	private var applyChanges = false

	override fun getProcessId() = PROCESS_ID

	// ----------- IModernSliderDialogController implementation -----------

	override fun getSliderTitle(): String = getString(R.string.shared_string_step)

	override fun getSliderSummary() = formatValueWithUnits(selectedValue)

	override fun getSliderLimits(): Limits<Int> {
		val range = organizeByType.stepRange!!
		return Limits(range.min.toInt(), range.max.toInt())
	}

	override fun getSelectedValue() = selectedValue

	override fun onChangeValue(newValue: Float) {
		selectedValue = newValue.toInt()
		setOrganizeByStep(selectedValue)
	}

	override fun formatValue(number: Number): String {
		return formatValueWithUnits(number.toInt())
	}

	override fun onApplyChanges() {
		if (initialValue != selectedValue) {
			setOrganizeByStep(selectedValue)
			applyChanges = true
		}
	}

	override fun onDestroy(activity: FragmentActivity?) {
		finishProcessIfNeeded(activity)
	}

	// ----------- Specific logic methods -----------

	private fun setOrganizeByStep(value: Int) {
		val currentParams = app.smartFolderHelper.getOrganizeByParams(folderId)
		if (currentParams != null && currentParams is OrganizeByRangeParams) {
			val newParams = OrganizeByRangeParams(currentParams.type, convertToBaseUnits(value))
			app.smartFolderHelper.setOrganizeByParams(folderId, newParams)
		}
	}

	override fun finishProcessIfNeeded(activity: FragmentActivity?): Boolean {
		if (super.finishProcessIfNeeded(activity)) {
			if (!applyChanges) {
				setOrganizeByStep(initialValue)
			}
			return true
		}
		return false
	}

	// ----------- Utilities methods -----------

	private fun formatValueWithUnits(value: Int): String {
		return "$value ${organizeByType.getDisplayUnits().getSymbol()}"
	}

	private fun convertToBaseUnits(value: Int): Double {
		return organizeByType.getDisplayUnits().toBase(value.toDouble())
	}

	override fun getDisplayData(processId: String): DisplayData {
		val displayData = DisplayData()
		displayData.putExtra(DialogExtra.TITLE, getString(R.string.set_step_size))
		displayData.putExtra(DialogExtra.SUBTITLE, getString(R.string.set_step_size_summary))
		return displayData
	}
}