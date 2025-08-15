package net.osmand.plus.plugins.odb.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.odb.OBDRemainingFuelWidget
import net.osmand.plus.plugins.odb.OBDRemainingFuelWidget.*
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetInfoFragment
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert

class RemainingFuelSettingFragment : BaseSimpleWidgetInfoFragment() {
	private var selectedRemainingFuelMode: Int = 0

	private lateinit var inflater: LayoutInflater
	private var buttonsCard: LinearLayout? = null
	private lateinit var selectedAppMode: ApplicationMode

	private lateinit var widget: OBDRemainingFuelWidget
	private lateinit var remainingFuelMode: OsmandPreference<RemainingFuelMode>

	companion object {
		private const val REMAINING_FUEL_MODE = "remaining_fuel_mode"
	}

	override fun initParams(bundle: Bundle) {
		super.initParams(bundle)
		if (widgetInfo != null && widgetInfo?.widget is OBDRemainingFuelWidget
		) {
			widget = widgetInfo?.widget as OBDRemainingFuelWidget
			remainingFuelMode = widget.remainingFuelMode
			selectedRemainingFuelMode = bundle.getInt(
				REMAINING_FUEL_MODE,
				remainingFuelMode.getModeValue(appMode).ordinal
			)
		} else {
			dismiss()
		}
	}

	override fun setupMainContent(container: ViewGroup) {
		inflater = themedInflater
		themedInflater.inflate(R.layout.map_marker_side_widget_settings_fragment, container)
		buttonsCard = view.findViewById(R.id.items_container)
		selectedAppMode = settings.applicationMode

		setupConfigButtons()
	}

	private fun setupConfigButtons() {
		buttonsCard?.removeAllViews()

		buttonsCard?.addView(createButtonWithDescription(
			getString(R.string.shared_string_mode),
			RemainingFuelMode.entries[selectedRemainingFuelMode].getTitle(app),
		) { showRemainingFuelModeDialog() })
	}

	private fun createButtonWithDescription(
		title: String,
		desc: String,
		listener: View.OnClickListener
	): View {
		val view = inflater.inflate(R.layout.configure_screen_list_item, null)

		val ivIcon = view.findViewById<ImageView>(R.id.icon)
		AndroidUiHelper.updateVisibility(ivIcon, false)

		val tvTitle = view.findViewById<TextView>(R.id.title)
		tvTitle.text = title

		val description = view.findViewById<TextView>(R.id.description)
		description.text = desc
		AndroidUiHelper.updateVisibility(description, true)

		view.findViewById<View>(R.id.button_container).setOnClickListener(listener)

		setupListItemBackground(view)
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.short_divider), false)

		return view
	}

	private fun showRemainingFuelModeDialog() {
		val items = arrayOfNulls<CharSequence>(RemainingFuelMode.entries.size)
		for (i in RemainingFuelMode.entries.toTypedArray().indices) {
			items[i] = RemainingFuelMode.entries[i].getTitle(app)
		}

		val dialogData = AlertDialogData(requireMyActivity(), nightMode)
			.setTitle(R.string.shared_string_mode)
			.setControlsColor(ColorUtilities.getActiveColor(app, nightMode))

		CustomAlert.showSingleSelection(dialogData, items, selectedRemainingFuelMode) { v: View ->
			selectedRemainingFuelMode = v.tag as Int
			setupConfigButtons()
		}
	}

	private fun setupListItemBackground(view: View) {
		val button = view.findViewById<View>(R.id.button_container)
		val color = selectedAppMode.getProfileColor(nightMode)
		val background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f)
		AndroidUtils.setBackground(button, background)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(REMAINING_FUEL_MODE, selectedRemainingFuelMode)
	}

	override fun applySettings() {
		super.applySettings()
		val prefsChanged =
			remainingFuelMode.getModeValue(appMode) != RemainingFuelMode.entries[selectedRemainingFuelMode]

		remainingFuelMode.setModeValue(
			appMode,
			RemainingFuelMode.entries[selectedRemainingFuelMode]
		)
		widget.updatePrefs(prefsChanged)
	}
}