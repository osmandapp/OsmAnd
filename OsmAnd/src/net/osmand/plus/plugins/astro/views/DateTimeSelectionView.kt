package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.osmand.plus.R
import java.util.Calendar
import java.util.TimeZone

class DateTimeSelectionView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

	// Default to System Local TimeZone
	private var currentCalendar = Calendar.getInstance(TimeZone.getDefault())
	private var onDateTimeChangeListener: ((Calendar) -> Unit)? = null

	private lateinit var tvYear: TextView
	private lateinit var tvMonth: TextView
	private lateinit var tvDay: TextView
	private lateinit var tvHour: TextView
	private lateinit var tvMinute: TextView

	init {
		orientation = HORIZONTAL
		LayoutInflater.from(context).inflate(R.layout.view_date_time_selection, this, true)
		initViews()
		updateDisplay()
	}

	private fun initViews() {
		tvYear = findViewById(R.id.tv_year)
		tvMonth = findViewById(R.id.tv_month)
		tvDay = findViewById(R.id.tv_day)
		tvHour = findViewById(R.id.tv_hour)
		tvMinute = findViewById(R.id.tv_minute)

		setupControl(R.id.btn_year_up, Calendar.YEAR, 1)
		setupControl(R.id.btn_year_down, Calendar.YEAR, -1)

		setupControl(R.id.btn_month_up, Calendar.MONTH, 1)
		setupControl(R.id.btn_month_down, Calendar.MONTH, -1)

		setupControl(R.id.btn_day_up, Calendar.DAY_OF_MONTH, 1)
		setupControl(R.id.btn_day_down, Calendar.DAY_OF_MONTH, -1)

		setupControl(R.id.btn_hour_up, Calendar.HOUR_OF_DAY, 1)
		setupControl(R.id.btn_hour_down, Calendar.HOUR_OF_DAY, -1)

		setupControl(R.id.btn_minute_up, Calendar.MINUTE, 5)
		setupControl(R.id.btn_minute_down, Calendar.MINUTE, -5)
	}

	private fun setupControl(btnId: Int, field: Int, amount: Int) {
		findViewById<ImageView>(btnId).setOnClickListener {
			currentCalendar.add(field, amount)
			updateDisplay()
			onDateTimeChangeListener?.invoke(currentCalendar)
		}
	}

	private fun updateDisplay() {
		tvYear.text = currentCalendar.get(Calendar.YEAR).toString()
		tvMonth.text = (currentCalendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
		tvDay.text = currentCalendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
		tvHour.text = currentCalendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
		tvMinute.text = currentCalendar.get(Calendar.MINUTE).toString().padStart(2, '0')
	}

	fun setOnDateTimeChangeListener(listener: (Calendar) -> Unit) {
		this.onDateTimeChangeListener = listener
	}

	fun setDateTime(calendar: Calendar) {
		this.currentCalendar = calendar.clone() as Calendar
		updateDisplay()
	}

	fun getDateTime(): Calendar {
		return currentCalendar.clone() as Calendar
	}
}