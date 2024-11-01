package net.osmand.plus.plugins.odb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.slider.Slider
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetSettingsFragment
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert

class AverageModeSettingFragment : BaseSimpleWidgetSettingsFragment() {
	private var selectedAverageMode: Boolean = false
	private var selectedIntervalMillis: Long = 0
	private var localSeekBarIntervalMillis: Long = 0

	private lateinit var inflater: LayoutInflater
	private var buttonsCard: LinearLayout? = null
	private var selectedAppMode: ApplicationMode? = null
	private var availableIntervals: Map<Long, String>? = null

	private lateinit var widget: OBDTextWidget
	private lateinit var averageValueModePref: OsmandPreference<Boolean>
	private lateinit var averageValueIntervalPref: OsmandPreference<Long>

	companion object {
		private const val AVERAGE_MODE_KEY = "average_mode"
		private const val AVERAGE_VALUE_INTERVAL_KEY = "average_value_interval"
	}

	override fun initParams(bundle: Bundle) {
		super.initParams(bundle)
		val widgetInfo = widgetRegistry.getWidgetInfoById(widgetId)
		if (widgetInfo != null && widgetInfo.widget is OBDTextWidget &&
			widgetInfo.widget.averageModePref != null && widgetInfo.widget.measuredIntervalPref != null
		) {
			widget = widgetInfo.widget
			averageValueModePref = widget.averageModePref!!
			averageValueIntervalPref = widget.measuredIntervalPref!!
		} else {
			dismiss()
		}
	}

	override fun setupContent(themedInflater: LayoutInflater, container: ViewGroup) {
		inflater = themedInflater
		themedInflater.inflate(R.layout.map_marker_side_widget_settings_fragment, container)
		buttonsCard = view.findViewById(R.id.items_container)
		selectedAppMode = settings.applicationMode
		availableIntervals = getAvailableIntervals()

		selectedIntervalMillis = averageValueIntervalPref.getModeValue(appMode)
		selectedAverageMode = averageValueModePref.getModeValue(appMode)

		updateToolbarIcon()
		setupConfigButtons()
		themedInflater.inflate(R.layout.divider, container)
		super.setupContent(themedInflater, container)
	}

	private fun getModeName(averageMode: Boolean): String {
		return if (widget.isTemperatureWidget()) {
			app.getString(if (averageMode) R.string.average_temperature else R.string.current_temperature)
		} else {
			app.getString(if (averageMode) R.string.average else R.string.shared_string_instant)
		}
	}

	private fun setupConfigButtons() {
		buttonsCard!!.removeAllViews()

		buttonsCard!!.addView(createButtonWithDescription(
			getString(R.string.shared_string_mode),
			getModeName(averageValueModePref.getModeValue(appMode)),
			selectedAverageMode,
		) { showMarkerModeDialog() })

		if (selectedAverageMode) {
			buttonsCard!!.addView(createButtonWithDescription(
				getString(R.string.shared_string_interval),
				availableIntervals!![selectedIntervalMillis]!!,
				false,
			) { showSeekbarSettingsDialog() })
		}
	}

	private val initialIntervalIndex: Int
		get() {
			val intervals: List<Long> = ArrayList(availableIntervals!!.keys)
			for (i in intervals.indices) {
				val interval = intervals[i]
				if (selectedIntervalMillis == interval) {
					return i
				}
			}

			return 0
		}

	private fun getAvailableIntervals(): Map<Long, String> {
		val intervals: MutableMap<Long, String> = LinkedHashMap()
		for (interval in AverageSpeedComputer.MEASURED_INTERVALS) {
			val formattedInterval = OBDTextWidget.formatIntervals(app, interval)
			intervals[interval] = formattedInterval
		}
		return intervals
	}

	private fun createButtonWithDescription(
		title: String,
		desc: String,
		showShortDivider: Boolean,
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
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.short_divider), showShortDivider)

		return view
	}

	private fun showSeekbarSettingsDialog() {
		val nightMode = !app.settings.isLightContentForMode(appMode)
		localSeekBarIntervalMillis = selectedIntervalMillis
		val themedContext = UiUtilities.getThemedContext(activity, nightMode)
		val builder = AlertDialog.Builder(themedContext)
		val seekbarView = inflater.inflate(R.layout.map_marker_interval_dialog, null, false)
		builder.setView(seekbarView)
		builder.setPositiveButton(R.string.shared_string_apply) { dialog, which ->
			selectedIntervalMillis = localSeekBarIntervalMillis
			setupConfigButtons()
		}
		builder.setNegativeButton(R.string.shared_string_cancel, null)

		val intervals: List<String> = ArrayList(availableIntervals!!.values)
		val minIntervalValue = intervals[0]
		val maxIntervalValue = intervals[intervals.size - 1]

		seekbarView.findViewById<TextView>(R.id.description).apply {
			text = app.getString(
				if (widget.isTemperatureWidget()) R.string.average_temperature_slider_description
				else R.string.average_value_slider_description
			)
		}

		val minInterval = seekbarView.findViewById<TextView>(R.id.min_interval)
		val maxInterval = seekbarView.findViewById<TextView>(R.id.max_interval)
		minInterval.text = minIntervalValue
		maxInterval.text = maxIntervalValue

		val interval = seekbarView.findViewById<TextView>(R.id.interval)

		val intervalStr = app.getString(R.string.shared_string_interval)
		val intervalsList: List<Map.Entry<Long, String>> = ArrayList(
			availableIntervals!!.entries
		)
		val initialIntervalIndex = initialIntervalIndex

		val slider = seekbarView.findViewById<Slider>(R.id.interval_slider)
		slider.valueFrom = 0f
		slider.valueTo = (availableIntervals!!.size - 1).toFloat()
		slider.value = initialIntervalIndex.toFloat()
		slider.clearOnChangeListeners()
		slider.addOnChangeListener(Slider.OnChangeListener { slider1: Slider?, intervalIndex: Float, fromUser: Boolean ->
			val newInterval =
				intervalsList[intervalIndex.toInt()]
			localSeekBarIntervalMillis = newInterval.key
			interval.text = app.getString(
				R.string.ltr_or_rtl_combine_via_colon,
				intervalStr,
				newInterval.value
			)
		})

		interval.text =
			app.getString(
				R.string.ltr_or_rtl_combine_via_colon,
				intervalStr,
				intervalsList[initialIntervalIndex].value
			)

		val selectedModeColor = appMode.getProfileColor(nightMode)
		UiUtilities.setupSlider(slider, nightMode, selectedModeColor)

		builder.show()
	}

	private fun showMarkerModeDialog() {
		val selected = if (selectedAverageMode) 1 else 0
		val items: Array<String> = arrayOf(getModeName(false), getModeName(true))

		val dialogData = AlertDialogData(requireContext(), nightMode)
			.setTitle(R.string.shared_string_mode)
			.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))

		CustomAlert.showSingleSelection(dialogData, items, selected) { v: View ->
			val which = v.tag as Int
			selectedAverageMode = which != 0
			setupConfigButtons()
		}
	}

	private fun setupListItemBackground(view: View) {
		val button = view.findViewById<View>(R.id.button_container)
		val color = selectedAppMode!!.getProfileColor(nightMode)
		val background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f)
		AndroidUtils.setBackground(button, background)
	}

	private fun updateToolbarIcon() {
		val icon = view.findViewById<ImageView>(R.id.icon)
		val iconId = widget.widgetType!!.getIconId(nightMode)
		icon.setImageDrawable(getIcon(iconId))
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(AVERAGE_MODE_KEY, selectedAverageMode)
		outState.putLong(AVERAGE_VALUE_INTERVAL_KEY, selectedIntervalMillis)
	}

	override fun applySettings() {
		super.applySettings()
		averageValueModePref.setModeValue(appMode, selectedAverageMode)
		if (selectedAverageMode) {
			averageValueIntervalPref.setModeValue(appMode, selectedIntervalMillis)
		}
		widget.updatePrefs()
	}
}