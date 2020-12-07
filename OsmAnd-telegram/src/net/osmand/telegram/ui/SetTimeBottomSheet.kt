package net.osmand.telegram.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.BottomSheetDialog
import net.osmand.telegram.utils.OsmandFormatter
import java.util.*

class SetTimeBottomSheet : DialogFragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private lateinit var dateStartBtn: TextView
	private lateinit var timeStartBtn: TextView
	private lateinit var dateEndBtn: TextView
	private lateinit var timeEndBtn: TextView

	private var startCalendar = Calendar.getInstance()
	private var endCalendar = Calendar.getInstance()

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_set_time, container, false)

		readFromBundle(savedInstanceState ?: arguments)

		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }

		BottomSheetBehavior.from(mainView.findViewById<View>(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})

		dateStartBtn = mainView.findViewById<TextView>(R.id.date_start_btn).apply {
			setOnClickListener { selectStartDate() }
		}
		timeStartBtn = mainView.findViewById<TextView>(R.id.time_start_btn).apply {
			setOnClickListener { selectStartTime() }
		}
		dateEndBtn = mainView.findViewById<TextView>(R.id.date_end_btn).apply {
			setOnClickListener { selectEndDate() }
		}
		timeEndBtn = mainView.findViewById<TextView>(R.id.time_end_btn).apply {
			setOnClickListener { selectEndTime() }
		}
		updateDateAndTimeButtons()

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		mainView.findViewById<TextView>(R.id.primary_btn).apply {
			setText(R.string.shared_string_apply)
			setOnClickListener {
				targetFragment?.also { target ->
					val intent = Intent().apply {
						putExtra(START_TIME, startCalendar.timeInMillis)
						putExtra(END_TIME, endCalendar.timeInMillis)
					}
					target.onActivityResult(targetRequestCode, SET_TIME_REQUEST_CODE, intent)
				}
				dismiss()
			}
		}

		return mainView
	}

	private fun readFromBundle(bundle: Bundle?) {
		bundle?.also {
			startCalendar.timeInMillis = it.getLong(START_TIME)
			endCalendar.timeInMillis = it.getLong(END_TIME)
		}
	}

	private fun updateDateAndTimeButtons() {
		dateStartBtn.text = OsmandFormatter.getFormattedDate(startCalendar.timeInMillis / 1000, false)
		dateEndBtn.text = OsmandFormatter.getFormattedDate(endCalendar.timeInMillis / 1000, false)

		timeStartBtn.text = OsmandFormatter.getFormattedTime(startCalendar.timeInMillis, useCurrentTime = false, short = true)
		timeEndBtn.text = OsmandFormatter.getFormattedTime(endCalendar.timeInMillis, useCurrentTime = false, short = true)
	}

	private fun selectStartDate() {
		context?.let {
			val dateFromDialog = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
					startCalendar.set(Calendar.YEAR, year)
					startCalendar.set(Calendar.MONTH, monthOfYear)
					startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
					updateDateAndTimeButtons()
				}
			DatePickerDialog(
				it, dateFromDialog,
				startCalendar.get(Calendar.YEAR),
				startCalendar.get(Calendar.MONTH),
				startCalendar.get(Calendar.DAY_OF_MONTH)
			).show()
		}
	}

	private fun selectStartTime() {
		TimePickerDialog(
			context,
			TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
				startCalendar.set(Calendar.HOUR_OF_DAY, hours)
				startCalendar.set(Calendar.MINUTE, minutes)
				updateDateAndTimeButtons()
			}, 0, 0, true
		).show()
	}

	private fun selectEndDate() {
		context?.let {
			val dateFromDialog = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
					endCalendar.set(Calendar.YEAR, year)
					endCalendar.set(Calendar.MONTH, monthOfYear)
					endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
					updateDateAndTimeButtons()
				}
			DatePickerDialog(
				it, dateFromDialog,
				endCalendar.get(Calendar.YEAR),
				endCalendar.get(Calendar.MONTH),
				endCalendar.get(Calendar.DAY_OF_MONTH)
			).show()
		}
	}

	private fun selectEndTime() {
		TimePickerDialog(
			context,
			TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
				endCalendar.set(Calendar.HOUR_OF_DAY, hours)
				endCalendar.set(Calendar.MINUTE, minutes)
				updateDateAndTimeButtons()
			}, 0, 0, true
		).show()
	}

	companion object {

		const val SET_TIME_REQUEST_CODE = 2
		const val START_TIME = "start_time"
		const val END_TIME = "end_time"

		private const val TAG = "SetTimeBottomSheet"

		fun showInstance(fm: androidx.fragment.app.FragmentManager, target: androidx.fragment.app.Fragment, start: Long, end: Long): Boolean {
			return try {
				SetTimeBottomSheet().apply {
					arguments = Bundle().apply {
						putLong(START_TIME, start)
						putLong(END_TIME, end)
					}
					setTargetFragment(target, SET_TIME_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}