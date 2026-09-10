package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.os.Build
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.gpx.filters.DateTrackFilter
import net.osmand.plus.widgets.OsmandTextFieldBoxes
import net.osmand.plus.widgets.TextViewEx
import studio.carbonylgroup.textfieldboxes.ExtendedEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

	companion object {
		private const val DATE_FROM_PICKER_TAG = "track_filter_date_from_picker"
		private const val DATE_TO_PICKER_TAG = "track_filter_date_to_picker"
		private val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")
	}

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

	fun bindView(filter: DateTrackFilter, fragmentManager: FragmentManager) {
		this.filter = filter
		title.text = filter.trackFilterType.getName()
		updateExpandState()
		updateValues()
		valueFromInputContainer.setOnClickListener {
			dateFromClickListener(fragmentManager)
		}
		valueToInputContainer.setOnClickListener { dateToClickListener(fragmentManager) }
		valueFromInput.setOnClickListener {
			dateFromClickListener(fragmentManager)
		}
		valueToInput.setOnClickListener { dateToClickListener(fragmentManager) }

		restoreDatePicker(fragmentManager, DATE_FROM_PICKER_TAG, dateFromSetter)
		restoreDatePicker(fragmentManager, DATE_TO_PICKER_TAG, dateToSetter)
	}

	private fun dateFromClickListener(fragmentManager: FragmentManager) {
		filter?.let {
			val end = toPickerDate(it.valueTo)
			val selection = toPickerDate(it.valueFrom).coerceAtMost(end)
			val constraints = CalendarConstraints.Builder()
				.setEnd(end)
				.setOpenAt(selection)
				.setValidator(DateValidatorPointBackward.before(end))
				.build()
			showDatePicker(
				fragmentManager, DATE_FROM_PICKER_TAG, selection, constraints, dateFromSetter)
		}
	}

	private fun dateToClickListener(fragmentManager: FragmentManager) {
		filter?.let {
			val start = toPickerDate(it.valueFrom)
			val selection = toPickerDate(it.valueTo).coerceAtLeast(start)
			val constraints = CalendarConstraints.Builder()
				.setStart(start)
				.setOpenAt(selection)
				.setValidator(DateValidatorPointForward.from(start))
				.build()
			showDatePicker(
				fragmentManager, DATE_TO_PICKER_TAG, selection, constraints, dateToSetter)
		}
	}

	private fun showDatePicker(
		fragmentManager: FragmentManager,
		tag: String,
		selection: Long,
		constraints: CalendarConstraints,
		dateSetter: MaterialPickerOnPositiveButtonClickListener<Long>
	) {
		if (!AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag, true)) {
			return
		}
		val picker = MaterialDatePicker.Builder.datePicker()
			.setTheme(
				if (nightMode) R.style.ThemeOverlay_App_Material_DatePicker_Dark
				else R.style.ThemeOverlay_App_Material_DatePicker_Light)
			.setSelection(selection)
			.setCalendarConstraints(constraints)
			.build()
		picker.addOnPositiveButtonClickListener(dateSetter)
		picker.show(fragmentManager, tag)
	}

	@Suppress("UNCHECKED_CAST")
	private fun restoreDatePicker(
		fragmentManager: FragmentManager,
		tag: String,
		dateSetter: MaterialPickerOnPositiveButtonClickListener<Long>
	) {
		val picker = fragmentManager.findFragmentByTag(tag) as? MaterialDatePicker<Long>
		picker?.addOnPositiveButtonClickListener(dateSetter)
	}

	private var dateFromSetter =
		MaterialPickerOnPositiveButtonClickListener<Long> { selection ->
			filter?.valueFrom = toStartOfDay(selection)
			updateValues()
		}

	private var dateToSetter =
		MaterialPickerOnPositiveButtonClickListener<Long> { selection ->
			filter?.valueTo = toEndOfDay(selection)
			updateValues()
		}

	private fun toPickerDate(localTime: Long): Long {
		val local = Calendar.getInstance()
		local.timeInMillis = localTime
		val utc = Calendar.getInstance(UTC_TIME_ZONE)
		utc.clear()
		utc.set(local[Calendar.YEAR], local[Calendar.MONTH], local[Calendar.DAY_OF_MONTH])
		return utc.timeInMillis
	}

	private fun toStartOfDay(pickerDate: Long): Long = toLocalDate(pickerDate).timeInMillis

	private fun toEndOfDay(pickerDate: Long): Long {
		val local = toLocalDate(pickerDate)
		local[Calendar.HOUR_OF_DAY] = 23
		local[Calendar.MINUTE] = 59
		local[Calendar.SECOND] = 59
		local[Calendar.MILLISECOND] = 999
		return local.timeInMillis
	}

	private fun toLocalDate(pickerDate: Long): Calendar {
		val utc = Calendar.getInstance(UTC_TIME_ZONE)
		utc.timeInMillis = pickerDate
		val local = Calendar.getInstance()
		local.clear()
		local.set(utc[Calendar.YEAR], utc[Calendar.MONTH], utc[Calendar.DAY_OF_MONTH])
		return local
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