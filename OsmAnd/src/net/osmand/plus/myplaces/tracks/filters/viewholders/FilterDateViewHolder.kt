package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Build
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.DateTrackFilter
import net.osmand.plus.widgets.OsmandTextFieldBoxes
import net.osmand.plus.widgets.TextViewEx
import studio.carbonylgroup.textfieldboxes.ExtendedEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FilterDateViewHolder(itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val app: OsmandApplication
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val titleContainer: View
	private val rangeInputContainer: View
	private val explicitIndicator: ImageView
	private var filter: DateTrackFilter? = null
	private val valueFromInput: ExtendedEditText
	private val valueToInput: EditText
	private val valueFromInputContainer: OsmandTextFieldBoxes
	private val valueToInputContainer: OsmandTextFieldBoxes
	private val DATE_FORMAT = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		titleContainer = itemView.findViewById(R.id.title_container)
		titleContainer.setOnClickListener { v: View? ->
			expanded = !expanded
			updateExpandState()
		}
		rangeInputContainer = itemView.findViewById(R.id.range_input_container)
		valueFromInput = itemView.findViewById(R.id.value_from_et)
		valueToInput = itemView.findViewById(R.id.value_to_et)

		valueFromInputContainer = itemView.findViewById(R.id.value_from)
		valueToInputContainer = itemView.findViewById(R.id.value_to)
	}

	fun bindView(filter: DateTrackFilter, activity: Activity) {
		this.filter = filter
		title.setText(filter.trackFilterType.nameResId)
		updateExpandState()
		updateValues()
		valueFromInputContainer.setOnClickListener {
			dateFromClickListener(activity)
		}
		valueToInputContainer.setOnClickListener { dateToClickListener(activity) }
		valueFromInput.setOnClickListener {
			dateFromClickListener(activity)
		}
		valueToInput.setOnClickListener { dateToClickListener(activity) }
	}

	private fun dateFromClickListener(activity: Activity) {
		filter?.let {
			showDatePicker(activity, it.valueFrom, dateFromSetter).maxDate = it.valueTo
		}
	}

	private fun dateToClickListener(activity: Activity) {
		filter?.let {
			showDatePicker(activity, it.valueTo, dateToSetter).minDate = it.valueFrom
		}
	}

	private fun showDatePicker(
		activity: Activity,
		now: Long,
		dateSetter: DatePickerDialog.OnDateSetListener
	): DatePicker {
		val nowCalendar = Calendar.getInstance()
		nowCalendar.time = Date(now)
		val dialog = DatePickerDialog(
			activity, dateSetter,
			nowCalendar[Calendar.YEAR],
			nowCalendar[Calendar.MONTH],
			nowCalendar[Calendar.DAY_OF_MONTH])
		dialog.show()
		return dialog.datePicker
	}

	private var dateFromSetter: DatePickerDialog.OnDateSetListener =
		DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
			val from = Calendar.getInstance()
			from[Calendar.YEAR] = year
			from[Calendar.MONTH] = month
			from[Calendar.DAY_OF_MONTH] = dayOfMonth
			filter?.valueFrom = from.time.time
			updateValues()
		}

	private var dateToSetter: DatePickerDialog.OnDateSetListener =
		DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
			val from = Calendar.getInstance()
			from[Calendar.YEAR] = year
			from[Calendar.MONTH] = month
			from[Calendar.DAY_OF_MONTH] = dayOfMonth
			filter?.valueTo = from.time.time
			updateValues()
		}

	private fun updateExpandState() {
		val iconRes =
			if (expanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down
		explicitIndicator.setImageDrawable(app.uiUtilities.getIcon(iconRes, !nightMode))
		AndroidUiHelper.updateVisibility(rangeInputContainer, expanded)
	}

	private fun updateValues() {
		filter?.let {
			val valueFrom = it.valueFrom
			val valueTo = it.valueTo
			valueFromInput.setText(DATE_FORMAT.format(valueFrom))
			valueFromInput.isClickable = false
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				valueFromInput.focusable = View.NOT_FOCUSABLE
				valueToInput.focusable = View.NOT_FOCUSABLE
			} else {
				valueFromInput.isFocusable = false
				valueToInput.isFocusable = false
			}
			valueToInput.setText(DATE_FORMAT.format(valueTo))
			valueToInput.isClickable = false

			AndroidUiHelper.updateVisibility(selectedValue, filter!!.isEnabled())
			selectedValue.text = String.format(
				app.getString(R.string.track_filter_date_selected_format),
				DATE_FORMAT.format(valueFrom),
				DATE_FORMAT.format(valueTo))
		}
	}
}