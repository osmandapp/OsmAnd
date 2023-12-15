package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.text.Editable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.RangeSlider.OnSliderTouchListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.MeasureUnitType
import net.osmand.plus.myplaces.tracks.filters.RangeTrackFilter
import net.osmand.plus.settings.enums.MetricsConstants
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.OsmandTextFieldBoxes
import net.osmand.plus.widgets.TextViewEx
import net.osmand.plus.widgets.tools.SimpleTextWatcher
import net.osmand.util.Algorithms
import studio.carbonylgroup.textfieldboxes.ExtendedEditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

open class FilterRangeViewHolder(
	itemView: View,
	nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	protected val app: OsmandApplication
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val minFilterValue: TextViewEx
	private val maxFilterValue: TextViewEx
	protected val selectedValue: TextViewEx
	private val titleContainer: View
	private val rangeInputContainer: View
	private val minMaxContainer: View
	private val explicitIndicator: ImageView
	private val slider: RangeSlider
	private lateinit var filter: RangeTrackFilter<*>
	private lateinit var valueFromInput: ExtendedEditText
	private lateinit var valueToInput: ExtendedEditText
	private val valueFromInputContainer: OsmandTextFieldBoxes
	private val valueToInputContainer: OsmandTextFieldBoxes
	private var isSliderDragging = false

	private val decimalFormat: DecimalFormat

	init {
		val formatSymbols = DecimalFormatSymbols(Locale.US)
		formatSymbols.groupingSeparator = ' '
		decimalFormat = DecimalFormat("###,###", formatSymbols)
	}

	private val onSliderChanged =
		RangeSlider.OnChangeListener { slider: RangeSlider, _: Float, fromUser: Boolean ->
			if (fromUser) {
				val values = slider.values
				val valueFrom = floor(values[0])
				val valueTo = ceil(values[1])
				if (valueFrom >= slider.valueFrom && valueTo <= slider.valueTo) {
					valueFromInput.setText(valueFrom.toInt().toString())
					valueToInput.setText(valueTo.toInt().toString())
				}
			}
		}
	private val onSliderTouchListener =
		object : OnSliderTouchListener {
			override fun onStartTrackingTouch(slider: RangeSlider) {
				isSliderDragging = true
			}

			override fun onStopTrackingTouch(slider: RangeSlider) {
				isSliderDragging = false
				val values = slider.values
				filter.setValueFrom(Math.round(values[0]).toString())
				filter.setValueTo(Math.round(values[1]).toString())
				updateValues()
			}
		}

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		minFilterValue = itemView.findViewById(R.id.min_filter_value)
		maxFilterValue = itemView.findViewById(R.id.max_filter_value)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		minMaxContainer = itemView.findViewById(R.id.min_max_container)
		titleContainer = itemView.findViewById(R.id.title_container)
		titleContainer.setOnClickListener {
			expanded = !expanded
			updateExpandState()
		}
		rangeInputContainer = itemView.findViewById(R.id.range_input_container)
		slider = itemView.findViewById(R.id.slider)
		slider.stepSize = 1f
		slider.addOnChangeListener(onSliderChanged)
		slider.addOnSliderTouchListener(onSliderTouchListener)
		val profileColor: Int = app.settings.applicationMode.getProfileColor(nightMode)
		UiUtilities.setupSlider(slider, nightMode, profileColor, false)
		valueFromInput = itemView.findViewById(R.id.value_from_et)
		valueFromInput.addTextChangedListener(object : SimpleTextWatcher() {
			override fun afterTextChanged(newText: Editable) {
				super.afterTextChanged(newText)
				if (!Algorithms.isEmpty(newText) && Algorithms.isInt(newText.toString())) {
					val newValue = newText.toString().toInt()
					if (filter.getDisplayValueFrom() != newValue
						&& filter.valueTo is Number
						&& newValue < (filter.valueTo as Number).toInt()
						&& !isSliderDragging) {
						filter.setValueFrom(newValue.toString())
						updateValues()
					}
				}
			}
		})
		valueToInput = itemView.findViewById(R.id.value_to_et)
		valueToInput.addTextChangedListener(object : SimpleTextWatcher() {
			override fun afterTextChanged(newText: Editable) {
				super.afterTextChanged(newText)
				if (!Algorithms.isEmpty(newText) && Algorithms.isInt(newText.toString())) {
					val newValue = newText.toString().toInt()
					if (filter.getDisplayValueTo() != newValue
						&& filter.valueFrom is Number
						&& newValue > (filter.valueFrom as Number).toInt()
						&& !isSliderDragging) {
						filter.setValueTo(newValue.toString())
						updateValues()
					}
				}
			}
		})
		valueFromInputContainer = itemView.findViewById(R.id.value_from)
		valueToInputContainer = itemView.findViewById(R.id.value_to)
	}

	fun bindView(filter: RangeTrackFilter<*>) {
		this.filter = filter
		title.setText(filter.filterType.nameResId)
		valueFromInputContainer.labelText =
			"${app.getString(R.string.shared_string_from)}, ${getFilterUnitText()}"
		valueToInputContainer.labelText =
			"${app.getString(R.string.shared_string_to)}, ${getFilterUnitText()}"
		updateExpandState()
		updateValues()
	}

	private fun updateExpandState() {
		val iconRes =
			if (expanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down
		explicitIndicator.setImageDrawable(app.uiUtilities.getIcon(iconRes, !nightMode))
		AndroidUiHelper.updateVisibility(rangeInputContainer, expanded)
		AndroidUiHelper.updateVisibility(slider, expanded)
		AndroidUiHelper.updateVisibility(minMaxContainer, expanded)
	}

	private fun updateValues() {
		val valueFrom = filter.getDisplayValueFrom()
		val valueTo = filter.getDisplayValueTo()
		val minValue = filter.getDisplayMinValue()
		val maxValue = filter.getDisplayMaxValue()
		slider.valueTo = maxValue.toFloat()
		slider.valueFrom = minValue.toFloat()
		slider.setValues(valueFrom.toFloat(), valueTo.toFloat())
		valueFromInput.setText(valueFrom.toString())
		valueFromInput.setSelection(valueFromInput.length())
		valueToInput.setText(valueTo.toString())
		valueToInput.setSelection(valueToInput.length())
		val minValuePrompt =
			"${decimalFormat.format(minValue.toFloat())} ${getFilterUnitText()}"
		val maxValuePrompt =
			"${decimalFormat.format(maxValue.toFloat())} ${getFilterUnitText()}"
		minFilterValue.text = minValuePrompt
		maxFilterValue.text = maxValuePrompt
		AndroidUiHelper.updateVisibility(selectedValue, filter.isEnabled())
		updateSelectedValue(valueFrom.toString(), valueTo.toString())
	}

	open fun updateSelectedValue(valueFrom: String, valueTo: String) {
		if (filter.filterType.measureUnitType == MeasureUnitType.TIME_DURATION) {
			val fromTxt =
				OsmAndFormatter.getFormattedDuration(valueFrom.toLong() * 60L, app)
			val toTxt = OsmAndFormatter.getFormattedDuration(valueTo.toLong() * 60L, app)
			selectedValue.text = String.format(
				app.getString(R.string.track_filter_date_selected_format),
				fromTxt,
				toTxt)
		} else {
			val fromTxt = decimalFormat.format(valueFrom.toLong())
			val toTxt = decimalFormat.format(valueTo.toLong())
			selectedValue.text = String.format(
				app.getString(R.string.track_filter_range_selected_format),
				fromTxt,
				toTxt,
				getFilterUnitText())
		}
	}

	private fun getFilterUnitText(): String {
		val unitResId = getFilterUnit()
		return if (unitResId > 0) app.getString(unitResId) else ""
	}

	private fun getFilterUnit(): Int {
		return when (filter.filterType.measureUnitType) {
			MeasureUnitType.TIME_DURATION -> R.string.shared_string_minute_lowercase
			MeasureUnitType.DISTANCE -> getDistanceUnits()
			MeasureUnitType.ALTITUDE -> getAltitudeUnits()
			MeasureUnitType.SPEED -> getSpeedUnits()
			MeasureUnitType.TEMPERATURE -> getTemperatureUnits()
			MeasureUnitType.ROTATIONS -> getRotationUnits()
			MeasureUnitType.BPM -> getBPMUnits()
			MeasureUnitType.POWER -> getPowerUnits()
			MeasureUnitType.DATE -> 0
			else -> 0
		}
	}

	private fun getDistanceUnits(): Int {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		return when (mc!!) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS -> R.string.mile

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> R.string.nm

			MetricsConstants.KILOMETERS_AND_METERS -> R.string.km
		}
	}

	private fun getPowerUnits(): Int {
		return R.string.power_watts_unit
	}

	private fun getTemperatureUnits(): Int {
		return R.string.degree_celsius
	}

	private fun getBPMUnits(): Int {
		return R.string.beats_per_minute_short
	}

	private fun getRotationUnits(): Int {
		return R.string.revolutions_per_minute_unit
	}

	private fun getSpeedUnits(): Int {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		return when (mc!!) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS -> R.string.mile_per_hour

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> R.string.nm_h

			MetricsConstants.KILOMETERS_AND_METERS -> R.string.km_h
		}
	}

	private fun getAltitudeUnits(): Int {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		val useFeet =
			mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.MILES_AND_YARDS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET
		return if (useFeet) {
			R.string.foot
		} else {
			R.string.m
		}
	}
}