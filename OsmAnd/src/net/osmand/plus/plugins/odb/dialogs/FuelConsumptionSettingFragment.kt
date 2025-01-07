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
import net.osmand.plus.plugins.odb.OBDFuelConsumptionWidget
import net.osmand.plus.plugins.odb.OBDFuelConsumptionWidget.*
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetSettingsFragment
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert

class FuelConsumptionSettingFragment : BaseSimpleWidgetSettingsFragment() {
	private var selectedFuelConsumptionMode: Int = 0

	private lateinit var inflater: LayoutInflater
	private var buttonsCard: LinearLayout? = null
	private lateinit var selectedAppMode: ApplicationMode

	private lateinit var widget: OBDFuelConsumptionWidget
	private lateinit var fuelConsumptionPref: OsmandPreference<FuelConsumptionMode>

	companion object {
		private const val FUEL_CONSUMPTION_MODE = "fuel_consumption_mode"
	}

	override fun initParams(bundle: Bundle) {
		super.initParams(bundle)
		val widgetInfo = widgetRegistry.getWidgetInfoById(widgetId)
		if (widgetInfo != null && widgetInfo.widget is OBDFuelConsumptionWidget
		) {
			widget = widgetInfo.widget
			fuelConsumptionPref = widget.fuelConsumptionMode
			selectedFuelConsumptionMode = bundle.getInt(
				FUEL_CONSUMPTION_MODE,
				fuelConsumptionPref.getModeValue(appMode).ordinal
			)
		} else {
			dismiss()
		}
	}

	override fun setupContent(themedInflater: LayoutInflater, container: ViewGroup) {
		inflater = themedInflater
		themedInflater.inflate(R.layout.map_marker_side_widget_settings_fragment, container)
		buttonsCard = view.findViewById(R.id.items_container)
		selectedAppMode = settings.applicationMode

		updateToolbarIcon()
		setupConfigButtons()
		themedInflater.inflate(R.layout.divider, container)
		super.setupContent(themedInflater, container)
	}

	private fun setupConfigButtons() {
		buttonsCard?.removeAllViews()

		buttonsCard?.addView(createButtonWithDescription(
			getString(R.string.shared_string_mode),
			FuelConsumptionMode.entries[selectedFuelConsumptionMode].getTitle(app),
		) { showFuelConsumptionModeDialog() })
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

	private fun showFuelConsumptionModeDialog() {
		val items = arrayOfNulls<CharSequence>(FuelConsumptionMode.entries.size)
		for (i in FuelConsumptionMode.entries.toTypedArray().indices) {
			items[i] = FuelConsumptionMode.entries[i].getTitle(app)
		}

		val dialogData = AlertDialogData(requireMyActivity(), nightMode)
			.setTitle(R.string.shared_string_mode)
			.setControlsColor(ColorUtilities.getActiveColor(app, nightMode))

		CustomAlert.showSingleSelection(dialogData, items, selectedFuelConsumptionMode) { v: View ->
			selectedFuelConsumptionMode = v.tag as Int
			setupConfigButtons()
		}
	}

	private fun setupListItemBackground(view: View) {
		val button = view.findViewById<View>(R.id.button_container)
		val color = selectedAppMode.getProfileColor(nightMode)
		val background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f)
		AndroidUtils.setBackground(button, background)
	}

	private fun updateToolbarIcon() {
		val icon = view.findViewById<ImageView>(R.id.icon)
		val iconId = widget.widgetType?.getIconId(nightMode)
		icon.setImageDrawable(iconId?.let { getIcon(it) })
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(FUEL_CONSUMPTION_MODE, selectedFuelConsumptionMode)
	}

	override fun applySettings() {
		super.applySettings()
		val prefsChanged =
			fuelConsumptionPref.getModeValue(appMode) != FuelConsumptionMode.entries[selectedFuelConsumptionMode]

		fuelConsumptionPref.setModeValue(
			appMode,
			FuelConsumptionMode.entries[selectedFuelConsumptionMode]
		)
		widget.updatePrefs(prefsChanged)
	}
}