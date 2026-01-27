package net.osmand.plus.myplaces.tracks.controller

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.containers.Limits
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.base.dialog.data.DialogExtra
import net.osmand.plus.base.dialog.data.DisplayData
import net.osmand.plus.card.base.headed.IHeadedContentCard
import net.osmand.plus.card.base.slider.ISliderCard
import net.osmand.plus.card.base.slider.SliderCard
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.bottomsheets.CustomizableSliderBottomSheet
import net.osmand.plus.settings.controllers.ICustomizableSliderDialogController
import net.osmand.shared.gpx.organization.OrganizeByRangeParams
import net.osmand.shared.gpx.organization.enums.OrganizeByType

class OrganizeTracksStepController(
	val app: OsmandApplication,
	private val folderId: String,
	private val organizeByType: OrganizeByType,
	private val initialValue: Int
) : BaseDialogController(app), ICustomizableSliderDialogController {

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
			CustomizableSliderBottomSheet.showInstance(fragmentManager, appMode, PROCESS_ID)
		}
	}

	private var selectedValue = organizeByType.stepRange!!.clamp(initialValue).toInt()
	private var applyChanges = false

	private var sliderCard: ISliderCard? = null
	private var headedCard: IHeadedContentCard? = null

	override fun getProcessId() = PROCESS_ID

	// ----------- IHeadedCardController implementation -----------

	override fun bindComponent(cardInstance: IHeadedContentCard) {
		this.headedCard = cardInstance
	}

	override fun getCardTitle(): String = getString(R.string.shared_string_step)

	override fun getCardSummary() = formatValueWithUnits(selectedValue)

	override fun getCardContentView(activity: FragmentActivity, nightMode: Boolean): View {
		val innerSliderCard = SliderCard(activity, this, false)
		return innerSliderCard.build()
	}

	// ----------- ISliderCardController implementation -----------

	override fun bindComponent(cardInstance: ISliderCard) {
		sliderCard = cardInstance
	}

	override fun getSliderLimits(): Limits<Int> {
		val range = organizeByType.stepRange!!
		return Limits(range.min.toInt(), range.max.toInt())
	}

	override fun getSelectedSliderValue() = selectedValue

	override fun onChangeSliderValue(newValue: Float) {
		selectedValue = newValue.toInt()
		headedCard?.updateCardSummary()
		setOrganizeByStep(selectedValue)
	}

	override fun formatValue(number: Number): String {
		return formatValueWithUnits(number.toInt())
	}

	// ----------- implement specific ICustomizableSliderDialogController methods -----------

	private fun setOrganizeByStep(value: Int) {
		val currentParams = app.smartFolderHelper.getOrganizeByParams(folderId)
		if (currentParams != null && currentParams is OrganizeByRangeParams) {
			val newParams = OrganizeByRangeParams(currentParams.type, convertToBaseUnits(value))
			app.smartFolderHelper.setOrganizeByParams(folderId, newParams)
		}
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