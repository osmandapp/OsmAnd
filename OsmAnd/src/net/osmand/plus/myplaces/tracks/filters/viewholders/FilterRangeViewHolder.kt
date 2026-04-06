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
import net.osmand.plus.myplaces.tracks.MeasureUnitsFormatter
import net.osmand.plus.utils.FormattedValue
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.OsmandTextFieldBoxes
import net.osmand.plus.widgets.TextViewEx
import net.osmand.plus.widgets.tools.SimpleTextWatcher
import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.gpx.filters.RangeTrackFilter
import net.osmand.util.Algorithms
import studio.carbonylgroup.textfieldboxes.ExtendedEditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

open class FilterRangeViewHolder(
	itemView: View,
	nightMode: Boolean
) : RecyclerView.ViewHolder(itemView) {
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
		RangeSlider.OnChangeListener { slider: RangeSlider, value: Float, fromUser: Boolean ->
			if (fromUser) {
				val values = slider.values
				val valueFrom = floor(values[0])
				val valueTo = ceil(values[1])

				if (valueFrom >= slider.valueFrom && valueTo <= slider.valueTo) {

					// Update left input ONLY if the active thumb is the left one
					if (value == values[0]) {
						val fromStr = valueFrom.toInt().toString()
						if (valueFromInput.text.toString() != fromStr) {
							valueFromInput.setText(fromStr)
						}
					}

					// Update right input ONLY if the active thumb is the right one
					if (value == values[1]) {
						val toStr = valueTo.toInt().toString()
						if (valueToInput.text.toString() != toStr) {
							valueToInput.setText(toStr)
						}
					}
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

				if (values[0] <= slider.valueFrom) {
					filter.clearValueFrom()
				} else {
					filter.setValueFrom(Math.round(values[0]).toString())
				}

				if (values[1] >= slider.valueTo) {
					filter.clearValueTo()
				} else {
					filter.setValueTo(Math.round(values[1]).toString())
				}

				updateValues(fromUser = true)
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
			expanded = !expanded && filter.isValid()
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
				if (isSliderDragging || isBinding) return

				if (Algorithms.isEmpty(newText)) {
					filter.clearValueFrom()
					updateValues(fromUser = true)
				} else if (Algorithms.isInt(newText.toString())) {
					val newValue = newText.toString().toInt()
					if (getDisplayValueFrom(filter) != newValue
						&& newValue < getDisplayValueTo(filter)) {

						filter.setValueFrom(newValue.toString())
						updateValues(fromUser = true)
					}
				}
			}
		})
		valueToInput = itemView.findViewById(R.id.value_to_et)
		valueToInput.addTextChangedListener(object : SimpleTextWatcher() {
			override fun afterTextChanged(newText: Editable) {
				super.afterTextChanged(newText)
				if (isSliderDragging || isBinding) return

				if (Algorithms.isEmpty(newText)) {
					filter.clearValueTo()
					updateValues(fromUser = true)
				} else if (Algorithms.isInt(newText.toString())) {
					val newValue = newText.toString().toInt()
					if (getDisplayValueTo(filter) != newValue
						&& newValue > getDisplayValueFrom(filter)) {

						filter.setValueTo(newValue.toString())
						updateValues(fromUser = true)
					}
				}
			}
		})
		valueFromInputContainer = itemView.findViewById(R.id.value_from)
		valueToInputContainer = itemView.findViewById(R.id.value_to)
	}

	var isBinding = false
	fun bindView(filter: RangeTrackFilter<*>) {
		this.filter = filter
		title.text = filter.trackFilterType.getName()
		valueFromInputContainer.labelText =
			"${app.getString(R.string.shared_string_from)}, ${
				MeasureUnitsFormatter.getUnitsLabel(app, getMeasureUnitType())
			}"
		valueToInputContainer.labelText =
			"${app.getString(R.string.shared_string_to)}, ${
				MeasureUnitsFormatter.getUnitsLabel(app, getMeasureUnitType())
			}"
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

	private fun updateValues(fromUser: Boolean = false) {
		isBinding = true
		val valueFrom = getDisplayValueFrom(filter)
		var valueTo = getDisplayValueTo(filter)
		val minValue = getDisplayMinValue(filter)
		val maxValue = getDisplayMaxValue(filter)
		if(filter.maxValue == filter.valueTo) {
			valueTo = maxValue
		}
		if (maxValue > minValue) {
			slider.valueTo = maxValue.toFloat()
			slider.valueFrom = minValue.toFloat()
			val safeValueFrom = maxOf(minValue.toFloat(), minOf(valueFrom.toFloat(), maxValue.toFloat()))
			val safeValueTo = maxOf(safeValueFrom, minOf(valueTo.toFloat(), maxValue.toFloat()))
			slider.setValues(safeValueFrom, safeValueTo)
		} else {
			expanded = false
			updateExpandState()
		}

		val isMinDefault = filter.valueFrom == filter.minValue
		updateInputField(valueFromInput, valueFrom, isMinDefault, fromUser)

		val isMaxDefault = filter.valueTo == filter.maxValue
		updateInputField(valueToInput, valueTo, isMaxDefault, fromUser)

		val minValuePrompt =
			"${decimalFormat.format(minValue.toFloat())} ${
				MeasureUnitsFormatter.getUnitsLabel(app, getMeasureUnitType())
			}"
		val maxValuePrompt =
			"${decimalFormat.format(maxValue.toFloat())} ${
				MeasureUnitsFormatter.getUnitsLabel(app, getMeasureUnitType())
			}"
		minFilterValue.text = minValuePrompt
		maxFilterValue.text = maxValuePrompt
		AndroidUiHelper.updateVisibility(selectedValue, filter.isEnabled())
		updateSelectedValue(valueFrom.toString(), valueTo.toString())
		isBinding = false
	}

	private fun updateInputField(
		input: ExtendedEditText,
		value: Int,
		isDefault: Boolean,
		fromUser: Boolean
	) {
		val currentText = input.text.toString()
		val textToSet = if (!fromUser && isDefault) {
			"" // Show empty if untouched
		} else if (currentText.isBlank() && isDefault) {
			"" // User manually erased it, let it stay erased
		} else if (currentText.toIntOrNull() == value) {
			currentText // Preserve exactly what the user typed
		} else {
			value.toString() // Slider dragged or external update
		}

		if (currentText != textToSet) {
			input.setText(textToSet)
			input.setSelection(input.length())
		}
	}

	open fun getDisplayMaxValue(filter: RangeTrackFilter<*>): Int {
		val formattedValue =
			getFormattedValue(filter.trackFilterType.measureUnitType, filter.ceilMaxValue())
		return ceil(formattedValue.valueSrc.toDouble() - 0.0001).toInt()
	}

	open fun getDisplayMinValue(filter: RangeTrackFilter<*>): Int {
		val formattedValue =
			getFormattedValue(filter.trackFilterType.measureUnitType, filter.ceilMinValue())
		return floor(formattedValue.valueSrc.toDouble() + 0.0001).toInt()
	}

	open fun getDisplayValueFrom(filter: RangeTrackFilter<*>): Int {
		val formattedValue =
			getFormattedValue(filter.trackFilterType.measureUnitType, filter.valueFrom.toString())
		return formattedValue.valueSrc.roundToInt()
	}

	open fun getDisplayValueTo(filter: RangeTrackFilter<*>): Int {
		val formattedValue = getFormattedValue(filter.trackFilterType.measureUnitType, filter.ceilValueTo())
		return formattedValue.valueSrc.roundToInt()
	}

	fun getFormattedValue(measureUnitType: MeasureUnitType, value: String): FormattedValue {
		return MeasureUnitsFormatter.getFormattedValue(app, measureUnitType, value)
	}

	open fun updateSelectedValue(valueFrom: String, valueTo: String) {
		val isMin = filter.valueFrom == filter.minValue || valueFrom.toInt() <= getDisplayMinValue(filter)
		val isMax = filter.valueTo == filter.maxValue || valueTo.toInt() >= getDisplayMaxValue(filter)

		val minStr = app.getString(R.string.shared_string_min)
		val maxStr = app.getString(R.string.shared_string_max)

		if (filter.trackFilterType.measureUnitType == MeasureUnitType.TIME_DURATION) {
			val fromTxt = if (isMin) minStr else OsmAndFormatter.getFormattedDuration(valueFrom.toLong() * 60L, app)
			val toTxt = if (isMax) maxStr else OsmAndFormatter.getFormattedDuration(valueTo.toLong() * 60L, app)

			selectedValue.text = String.format(
				app.getString(R.string.track_filter_date_selected_format),
				fromTxt,
				toTxt)
		} else {
			val fromTxt = if (isMin) minStr else decimalFormat.format(valueFrom.toLong())
			val toTxt = if (isMax) maxStr else decimalFormat.format(valueTo.toLong())

			selectedValue.text = String.format(
				app.getString(R.string.track_filter_range_selected_format),
				fromTxt,
				toTxt,
				MeasureUnitsFormatter.getUnitsLabel(app, getMeasureUnitType())
			)
		}
	}

	private fun getMeasureUnitType() = filter.trackFilterType.measureUnitType
}