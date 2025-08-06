package net.osmand.plus.myplaces.tracks.filters.viewholders

//import net.osmand.plus.myplaces.tracks.filters.MeasureUnitType
import android.text.Editable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.RangeSlider.OnSliderTouchListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.FormattedValue
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.OsmandTextFieldBoxes
import net.osmand.plus.widgets.TextViewEx
import net.osmand.plus.widgets.tools.SimpleTextWatcher
import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.gpx.filters.RangeTrackFilter
import net.osmand.shared.settings.enums.AltitudeMetrics
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
				if (!Algorithms.isEmpty(newText) && Algorithms.isInt(newText.toString())) {
					val newValue = newText.toString().toInt()
					if (getDisplayValueFrom(filter) != newValue
						&& filter.valueTo is Number
						&& newValue < (filter.valueTo as Number).toInt()
						&& !isSliderDragging
						&& !isBinding) {
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
					if (getDisplayValueTo(filter) != newValue
						&& filter.valueFrom is Number
						&& newValue > (filter.valueFrom as Number).toInt()
						&& !isSliderDragging
						&& !isBinding) {
						filter.setValueTo(newValue.toString())
						updateValues()
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
				getMeasureUnitType().getFilterUnitText(
					app.settings.METRIC_SYSTEM.get(),
					app.settings.ALTITUDE_METRIC.get()
				)
			}"
		valueToInputContainer.labelText =
			"${app.getString(R.string.shared_string_to)}, ${
				getMeasureUnitType().getFilterUnitText(
					app.settings.METRIC_SYSTEM.get(),
					app.settings.ALTITUDE_METRIC.get()
				)
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

	private fun updateValues() {
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
			slider.setValues(valueFrom.toFloat(), valueTo.toFloat())
		} else {
			expanded = false
			updateExpandState()
		}
		valueFromInput.setText(valueFrom.toString())
		valueFromInput.setSelection(valueFromInput.length())
		valueToInput.setText(valueTo.toString())
		valueToInput.setSelection(valueToInput.length())
		val minValuePrompt =
			"${decimalFormat.format(minValue.toFloat())} ${
				getMeasureUnitType().getFilterUnitText(
					app.settings.METRIC_SYSTEM.get(),
					app.settings.ALTITUDE_METRIC.get()
				)
			}"
		val maxValuePrompt =
			"${decimalFormat.format(maxValue.toFloat())} ${
				getMeasureUnitType().getFilterUnitText(
					app.settings.METRIC_SYSTEM.get(),
					app.settings.ALTITUDE_METRIC.get()
				)
			}"
		minFilterValue.text = minValuePrompt
		maxFilterValue.text = maxValuePrompt
		AndroidUiHelper.updateVisibility(selectedValue, filter.isEnabled())
		updateSelectedValue(valueFrom.toString(), valueTo.toString())
		isBinding = false
	}

	open fun getDisplayMaxValue(filter: RangeTrackFilter<*>): Int {
		val formattedValue =
			getFormattedValue(filter.trackFilterType.measureUnitType, filter.ceilMaxValue())
		return ceil(formattedValue.valueSrc).toInt()
	}

	open fun getDisplayMinValue(filter: RangeTrackFilter<*>): Int {
		val formattedValue =
			getFormattedValue(filter.trackFilterType.measureUnitType, filter.ceilMinValue())
		return formattedValue.valueSrc.toInt()
	}

	open fun getDisplayValueFrom(filter: RangeTrackFilter<*>): Int {
		val formattedValue =
			getFormattedValue(filter.trackFilterType.measureUnitType, filter.valueFrom.toString())
		return formattedValue.valueSrc.toInt()
	}

	open fun getDisplayValueTo(filter: RangeTrackFilter<*>): Int {
		val formattedValue = getFormattedValue(filter.trackFilterType.measureUnitType, filter.ceilValueTo())
		return formattedValue.valueSrc.toInt()
	}

	fun getFormattedValue(
		measureUnitType: MeasureUnitType,
		value: String): FormattedValue {
		val altitudeMetrics: AltitudeMetrics = app.settings.ALTITUDE_METRIC.get()
		val params = OsmAndFormatterParams()
		params.setExtraDecimalPrecision(3)
		params.setForcePreciseValue(true)
		return when (measureUnitType) {
			MeasureUnitType.SPEED -> OsmAndFormatter.getFormattedSpeedValue(value.toFloat(), app)
			MeasureUnitType.ALTITUDE -> OsmAndFormatter.getFormattedAltitudeValue(
				value.toDouble(),
				app,
				altitudeMetrics)

			MeasureUnitType.DISTANCE -> OsmAndFormatter.getFormattedDistanceValue(
				value.toFloat(),
				app,
				params)

			MeasureUnitType.TIME_DURATION -> FormattedValue(
				value.toFloat() / 1000 / 60,
				value,
				""
			)

			else -> FormattedValue(value.toFloat(), value, "")
		}
	}


	open fun updateSelectedValue(valueFrom: String, valueTo: String) {
		if (filter.trackFilterType.measureUnitType == MeasureUnitType.TIME_DURATION) {
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
				getMeasureUnitType().getFilterUnitText(app.settings.METRIC_SYSTEM.get(), app.settings.ALTITUDE_METRIC.get()))
		}
	}

	private fun getMeasureUnitType(): MeasureUnitType {
		return filter.trackFilterType.measureUnitType
	}
}