package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import java.time.LocalDate

class AstroScheduleCardViewHolder(
	itemView: View,
	private val onResetPeriod: () -> Unit,
	private val onShiftPeriod: (daysDelta: Int) -> Unit,
	private val onSelectDate: (LocalDate) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private data class TimeBlockViews(
		val arrowView: TextView,
		val timeMainView: TextView,
		val meridiemView: TextView,
		val suffixView: TextView? = null
	)

	private data class DayRowViews(
		val root: View,
		val dayLabelView: TextView,
		val riseViews: TimeBlockViews,
		val setViews: TimeBlockViews,
		val dayGraphView: AstroScheduleGraphView,
		val divider: View
	)

	private val rangeText: TextView = itemView.findViewById(R.id.schedule_range)
	private val dateButton: View = itemView.findViewById(R.id.schedule_date_button)
	private val prevButton: View = itemView.findViewById(R.id.schedule_prev_button)
	private val nextButton: View = itemView.findViewById(R.id.schedule_next_button)
	private val daysContainer: LinearLayout = itemView.findViewById(R.id.schedule_days_container)
	private val rowViews = ArrayList<DayRowViews>(AstroScheduleCardController.PERIOD_DAYS)

	fun bind(item: AstroScheduleCardItem) {
		rangeText.text = item.rangeLabel
		dateButton.isVisible = item.showResetPeriodButton
		dateButton.setOnClickListener { onResetPeriod() }
		prevButton.setOnClickListener { onShiftPeriod(-AstroScheduleCardController.PERIOD_DAYS) }
		nextButton.setOnClickListener { onShiftPeriod(AstroScheduleCardController.PERIOD_DAYS) }

		ensureRowViews()
		rowViews.forEachIndexed { index, row ->
			val dayEntry = item.days.getOrNull(index)
			row.divider.visibility =
				if (index == AstroScheduleCardController.PERIOD_DAYS - 1) View.GONE else View.VISIBLE
			if (dayEntry == null) {
				bindPlaceholder(row)
			} else {
				bindDayRow(row, dayEntry)
			}
		}
	}

	private fun ensureRowViews() {
		if (rowViews.isNotEmpty()) {
			return
		}
		val inflater = LayoutInflater.from(itemView.context)
		repeat(AstroScheduleCardController.PERIOD_DAYS) {
			val rowView = inflater.inflate(R.layout.astro_schedule_day_item, daysContainer, false)
			rowViews += DayRowViews(
				root = rowView,
				dayLabelView = rowView.findViewById(R.id.schedule_day_label),
				riseViews = TimeBlockViews(
					arrowView = rowView.findViewById(R.id.schedule_rise_arrow),
					timeMainView = rowView.findViewById(R.id.schedule_rise_time_main),
					meridiemView = rowView.findViewById(R.id.schedule_rise_time_meridiem)
				),
				setViews = TimeBlockViews(
					arrowView = rowView.findViewById(R.id.schedule_set_arrow),
					timeMainView = rowView.findViewById(R.id.schedule_set_time_main),
					meridiemView = rowView.findViewById(R.id.schedule_set_time_meridiem),
					suffixView = rowView.findViewById(R.id.schedule_set_next_day)
				),
				dayGraphView = rowView.findViewById(R.id.schedule_day_graph),
				divider = rowView.findViewById(R.id.schedule_row_divider)
			)
			daysContainer.addView(rowView)
		}
	}

	private fun bindDayRow(row: DayRowViews, dayEntry: AstroScheduleDayItem) {
		row.root.alpha = 1f
		row.root.isClickable = true
		row.root.isFocusable = true
		row.dayLabelView.text = dayEntry.dayLabel
		bindTimeBlock(
			block = row.riseViews,
			time = dayEntry.riseTime,
			arrow = RISE_ARROW
		)
		bindTimeBlock(
			block = row.setViews,
			time = dayEntry.setTime,
			arrow = SET_ARROW,
			suffix = if (dayEntry.setNextDay) {
				itemView.context.getString(R.string.astro_next_day_suffix)
			} else {
				null
			}
		)
		row.dayGraphView.submitModel(dayEntry.graph)
		row.root.setOnClickListener { onSelectDate(dayEntry.date) }
	}

	private fun bindPlaceholder(row: DayRowViews) {
		row.root.alpha = 0f
		row.root.isClickable = false
		row.root.isFocusable = false
		row.root.setOnClickListener(null)
		row.dayLabelView.text = ""
		bindTimeBlock(block = row.riseViews, time = null, arrow = RISE_ARROW)
		bindTimeBlock(block = row.setViews, time = null, arrow = SET_ARROW)
		row.dayGraphView.submitModel(null)
	}

	private fun bindTimeBlock(
		block: TimeBlockViews,
		time: String?,
		arrow: String,
		suffix: String? = null
	) {
		val parts = splitTimeParts(time)
		block.arrowView.text = arrow
		block.timeMainView.text = parts.main
		block.meridiemView.isVisible = !parts.meridiem.isNullOrBlank()
		block.meridiemView.text = parts.meridiem.orEmpty()
		block.suffixView?.let { suffixView ->
			suffixView.isVisible = !suffix.isNullOrBlank()
			suffixView.text = suffix.orEmpty()
		}
	}

	private fun splitTimeParts(time: String?): TimeParts {
		if (time.isNullOrBlank()) {
			return TimeParts(main = EMPTY_TIME, meridiem = null)
		}
		val tokens = time.trim().split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
		if (tokens.size <= 1) {
			return TimeParts(main = tokens.firstOrNull() ?: EMPTY_TIME, meridiem = null)
		}
		return TimeParts(
			main = tokens.dropLast(1).joinToString(" "),
			meridiem = tokens.last()
		)
	}

	private companion object {
		private val WHITESPACE_REGEX = "\\s+".toRegex()
		const val RISE_ARROW = "▲"
		const val SET_ARROW = "▼"
		const val EMPTY_TIME = "—"
	}

	private data class TimeParts(
		val main: String,
		val meridiem: String?
	)
}
