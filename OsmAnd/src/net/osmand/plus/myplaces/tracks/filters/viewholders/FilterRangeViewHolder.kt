package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.RangeSlider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.RangeTrackFilter
import net.osmand.plus.widgets.OsmandTextFieldBoxes
import net.osmand.plus.widgets.TextViewEx
import net.osmand.plus.widgets.tools.SimpleTextWatcher
import net.osmand.util.Algorithms
import studio.carbonylgroup.textfieldboxes.ExtendedEditText

class FilterRangeViewHolder(itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val app: OsmandApplication
	private val nightMode: Boolean
	private val listener: TextChangedListener? = null
	private var expanded = false
	private val title: TextViewEx
	private val minFilterValue: TextViewEx
	private val maxFilterValue: TextViewEx
	private val selectedValue: TextViewEx
	private val titleContainer: View
	private val rangeInputContainer: View
	private val minMaxContainer: View
	private val explicitIndicator: ImageView
	private val slider: RangeSlider
	private var filter: RangeTrackFilter? = null
	private val valueFromInput: ExtendedEditText
	private val valueToInput: ExtendedEditText
	private val valueFromInputContainer: OsmandTextFieldBoxes
	private val valueToInputContainer: OsmandTextFieldBoxes
	private val textWatcher: TextWatcher = object : SimpleTextWatcher() {
		override fun afterTextChanged(s: Editable) {
			listener?.onTextChanged(s.toString())
		}
	}
	private val onSliderChanged =
		RangeSlider.OnChangeListener { slider: RangeSlider, value: Float, fromUser: Boolean ->
			if (filter != null && fromUser) {
				val values = slider.values
				filter!!.setValueFrom(Math.round(values[0]))
				filter!!.setValueTo(Math.round(values[1]))
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
		titleContainer.setOnClickListener { v: View? ->
			expanded = !expanded
			updateExpandState()
		}
		rangeInputContainer = itemView.findViewById(R.id.range_input_container)
		slider = itemView.findViewById(R.id.slider)
		slider.stepSize = 1f
		slider.addOnChangeListener(onSliderChanged)
		valueFromInput = itemView.findViewById(R.id.value_from_et)
		valueFromInput.addTextChangedListener(object : SimpleTextWatcher() {
			override fun afterTextChanged(newText: Editable) {
				super.afterTextChanged(newText)
				if (!Algorithms.isEmpty(newText) && Algorithms.isInt(newText.toString())) {
					val newValue = newText.toString().toInt()
					if (filter!!.getValueFrom() != newValue) {
						filter!!.setValueFrom(newValue)
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
					if (filter!!.getValueTo() != newValue) {
						filter!!.setValueTo(newValue)
						updateValues()
					}
				}
			}
		})
		valueFromInputContainer = itemView.findViewById(R.id.value_from)
		valueToInputContainer = itemView.findViewById(R.id.value_to)
	}

	fun bindView(filter: RangeTrackFilter) {
		this.filter = filter
		title.setText(filter.displayNameId)
		valueFromInputContainer.labelText =
			"${app.getString(R.string.shared_string_from)}, ${app.getString(filter.unitResId)}"
		valueToInputContainer.labelText =
			"${app.getString(R.string.shared_string_to)}, ${app.getString(filter.unitResId)}"
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
		val valueFrom = filter!!.getValueFrom()
		val valueTo = filter!!.getValueTo()
		slider.setValues(valueFrom.toFloat(), valueTo.toFloat())
		slider.valueFrom = filter!!.minValue.toFloat()
		slider.valueTo = filter!!.maxValue.toFloat()
		valueFromInput.setText(valueFrom.toString())
		valueFromInput.setSelection(valueFromInput.length())
		valueToInput.setText(valueTo.toString())
		valueToInput.setSelection(valueToInput.length())
		val minValuePrompt = "${filter!!.minValue}, ${app.getString(filter!!.unitResId)}"
		val maxValuePrompt = "${filter!!.maxValue}, ${app.getString(filter!!.unitResId)}"
		minFilterValue.text = minValuePrompt
		maxFilterValue.text = maxValuePrompt
		AndroidUiHelper.updateVisibility(selectedValue, filter!!.enabled)
		selectedValue.text = String.format(
			app.getString(R.string.track_filter_range_selected_format),
			valueFrom,
			valueTo,
			app.getString(
				filter!!.unitResId))
	}

	interface TextChangedListener {
		fun onTextChanged(neeText: String)
	}
}