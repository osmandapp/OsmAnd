package net.osmand.plus.plugins.monitoring.widgets

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import net.osmand.plus.R
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingAvgSpeedWidgetState.AvgSpeedMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetInfoFragment
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert

class TripRecordingAvgSpeedWidgetInfoFragment : BaseSimpleWidgetInfoFragment() {

	companion object {
		const val AVG_SPEED_MODE = "avg_speed_mode"
	}

	private var modePreference: OsmandPreference<AvgSpeedMode>? = null
	private lateinit var iconMode: ImageView
	private lateinit var titleMode: TextView
	private lateinit var descriptionMode: TextView

	private var selectedMode: Int = 0

	override fun initParams(bundle: Bundle) {
		super.initParams(bundle)
		val widgetInfo = widgetInfo
		if (widgetInfo != null) {
			val widget = widgetInfo.widget as TripRecordingAvgSpeedWidget
			modePreference = widget.getAvgSpeedModePreference()
		}
		val defaultModeOrdinal = modePreference?.getModeValue(appMode)?.ordinal ?: AvgSpeedMode.TRIP_AVERAGE.ordinal
		selectedMode = bundle.getInt(AVG_SPEED_MODE, defaultModeOrdinal)
	}

	override fun setupMainContent(container: ViewGroup) {
		if (modePreference != null) {
			val modeButton: View = inflate(R.layout.bottom_sheet_item_with_descr_72dp, container)
			iconMode = container.findViewById(R.id.icon)
			titleMode = container.findViewById(R.id.title)
			descriptionMode = container.findViewById(R.id.description)

			titleMode.setText(R.string.shared_string_mode)

			val currentMode = modePreference!!.get()
			iconMode.setImageDrawable(app.uiUtilities.getIcon(currentMode.getIcon(nightMode)))

			modeButton.setOnClickListener { showModeDialog() }
			modeButton.background = pressedStateDrawable
		}
	}

	private fun showModeDialog() {
		val modes = AvgSpeedMode.entries.toTypedArray()
		val items = Array<CharSequence>(modes.size) { i -> getString(modes[i].titleId) }

		val dialogData = AlertDialogData(titleMode.context, nightMode)
			.setTitle(R.string.shared_string_mode)
			.setControlsColor(ColorUtilities.getActiveColor(app, nightMode))

		CustomAlert.showSingleSelection(dialogData, items, selectedMode) { v ->
			selectedMode = v.tag as Int
			updateModeSetting()
		}
	}

	private fun updateModeSetting() {
		val mode = AvgSpeedMode.entries[selectedMode]
		descriptionMode.text = getString(mode.titleId)
		iconMode.setImageDrawable(app.uiUtilities.getIcon(mode.getIcon(nightMode)))
	}

	override fun applySettings() {
		super.applySettings()
		modePreference?.setModeValue(appMode, AvgSpeedMode.entries[selectedMode])
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(AVG_SPEED_MODE, selectedMode)
	}

	override fun onResume() {
		super.onResume()
		updateModeSetting()
	}
}