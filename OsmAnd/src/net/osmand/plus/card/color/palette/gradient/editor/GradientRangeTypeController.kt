package net.osmand.plus.card.color.palette.gradient.editor

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import androidx.fragment.app.FragmentManager
import net.osmand.OnResultCallback
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.base.dialog.data.DialogExtra
import net.osmand.plus.base.dialog.data.DisplayData
import net.osmand.plus.base.dialog.data.DisplayItem
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.palette.domain.GradientRangeType

class GradientRangeTypeController(
	app: OsmandApplication,
	private val appMode: ApplicationMode,
	private val usedOnMap: Boolean,
	private val supportedTypes: List<GradientRangeType>,
	private val callback: OnResultCallback<GradientRangeType>
) : BaseDialogController(app), IDisplayDataProvider, IDialogItemSelected {

	companion object {

		const val PROCESS_ID = "select_gradient_range_type"

		fun showDialog(
			app: OsmandApplication,
			fragmentManager: FragmentManager,
			appMode: ApplicationMode,
			usedOnMap: Boolean,
			supportedTypes: List<GradientRangeType>,
			callback: OnResultCallback<GradientRangeType>
		) {
			val controller =
				GradientRangeTypeController(app, appMode, usedOnMap, supportedTypes, callback)
			val dialogManager = app.dialogManager
			dialogManager.register(PROCESS_ID, controller)

			// TODO: pass app mode and use on map
			CustomizableSingleSelectionBottomSheet.showInstance(fragmentManager, PROCESS_ID, null, true)
		}
	}

	override fun getProcessId() = PROCESS_ID

	override fun getDisplayData(processId: String): DisplayData {
		val displayData = DisplayData()
		val iconsCache = app.uiUtilities
		val nightMode = app.daynightHelper.isNightMode(appMode, ThemeUsageContext.valueOf(usedOnMap))
		val activeColor = app.settings.applicationMode.getProfileColor(nightMode)

		displayData.putExtra(DialogExtra.TITLE, getString(R.string.add_palette))

		val summaryBuilder = SpannableStringBuilder()

		supportedTypes.forEach { type ->
			val title = type.getTitle()
			val desc = type.getSummary()
			val fullString = getString(R.string.ltr_or_rtl_combine_via_colon, title, desc)

			val spannable = UiUtilities.createSpannableString(fullString, Typeface.BOLD, title)
			summaryBuilder.append(spannable)
			summaryBuilder.append("\n")
		}

		displayData.putExtra(DialogExtra.SUBTITLE, summaryBuilder.trim())
		displayData.putExtra(DialogExtra.SUBTITLE_BOTTOM_MARGIN, getDimension(R.dimen.content_padding_small))
		displayData.putExtra(DialogExtra.SHOW_BOTTOM_BUTTONS, true)

		for (type in supportedTypes) {
			val iconName = type.getIconName()
			val iconId = AndroidUtils.getDrawableId(app, iconName, R.drawable.ic_action_info_dark)

			val item = DisplayItem()
				.setTitle(type.getTitle())
				.setNormalIcon(iconsCache.getThemedIcon(iconId))
				.setControlsColor(activeColor)
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setTag(type)

			displayData.addDisplayItem(item)
		}

		return displayData
	}

	override fun onDialogItemSelected(processId: String, selected: DisplayItem) {
		val item = selected.tag
		if (item is GradientRangeType) {
			callback.onResult(item)
		}
	}
}