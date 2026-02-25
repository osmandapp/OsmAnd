package net.osmand.plus.plugins.monitoring.widgets

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import net.osmand.plus.R
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingMovingTimeWidgetState.MovingTimeMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetInfoFragment
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert

class TripRecordingMovingTimeWidgetInfoFragment : BaseSimpleWidgetInfoFragment() {

	companion object {
		const val MOVING_TIME_MODE = "moving_time_mode"
	}

	private var modeOsmandPreference: OsmandPreference<MovingTimeMode>? = null
	private lateinit var iconMode: ImageView
	private lateinit var titleMode: TextView
	private lateinit var descriptionMode: TextView

	private var selectedMode: Int = 0

	override fun initParams(bundle: Bundle) {
		super.initParams(bundle)
		if (widgetInfo != null) {
			val widget = widgetInfo!!.widget as TripRecordingMovingTimeWidget
			modeOsmandPreference = widget.getMovingTimeModeOsmandPreference()
		}
		val defaultModeOrdinal = modeOsmandPreference?.getModeValue(appMode)?.ordinal ?: MovingTimeMode.TOTAL.ordinal
		selectedMode = bundle.getInt(MOVING_TIME_MODE, defaultModeOrdinal)
	}

	override fun setupMainContent(container: ViewGroup) {
		if (modeOsmandPreference != null) {
			val modeButton: View = inflate(R.layout.bottom_sheet_item_with_descr_72dp, container)
			iconMode = container.findViewById(R.id.icon)
			titleMode = container.findViewById(R.id.title)
			descriptionMode = container.findViewById(R.id.description)

			titleMode.setText(R.string.shared_string_mode)

			val currentMode = modeOsmandPreference!!.get()
			iconMode.setImageDrawable(app.uiUtilities.getIcon(currentMode.getIcon(nightMode)))

			modeButton.setOnClickListener { showModeDialog() }
			modeButton.background = pressedStateDrawable
		}
	}

	private fun showModeDialog() {
		val modes = MovingTimeMode.entries.toTypedArray()
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
		val mode = MovingTimeMode.entries[selectedMode]
		descriptionMode.text = getString(mode.titleId)
		iconMode.setImageDrawable(app.uiUtilities.getIcon(mode.getIcon(nightMode)))
	}

	override fun applySettings() {
		super.applySettings()
		modeOsmandPreference?.setModeValue(appMode, MovingTimeMode.entries[selectedMode])
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(MOVING_TIME_MODE, selectedMode)
	}

	override fun onResume() {
		super.onResume()
		updateModeSetting()
	}
}