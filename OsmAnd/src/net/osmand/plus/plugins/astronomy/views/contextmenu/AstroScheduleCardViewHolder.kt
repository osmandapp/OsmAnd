package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.SuperscriptSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import java.time.LocalDate

class AstroScheduleCardViewHolder(
	itemView: View,
	private val onResetPeriod: () -> Unit,
	private val onShiftPeriod: (daysDelta: Int) -> Unit,
	private val onSelectDate: (LocalDate) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private data class TimeBlockViews(
		val arrowView: TextView,
		val timeView: TextView
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
	private val noteText: TextView = itemView.findViewById(R.id.schedule_note)
	private val dateButton: View = itemView.findViewById(R.id.schedule_date_button)
	private val prevButton: View = itemView.findViewById(R.id.schedule_prev_button)
	private val prevButtonIcon: ImageView = itemView.findViewById(R.id.schedule_prev_icon)
	private val nextButton: View = itemView.findViewById(R.id.schedule_next_button)
	private val nextButtonIcon: ImageView = itemView.findViewById(R.id.schedule_next_icon)
	private val daysContainer: LinearLayout = itemView.findViewById(R.id.schedule_days_container)
	private val meridiemTextSizePx: Int =
		itemView.resources.getDimensionPixelSize(R.dimen.astro_schedule_meridiem_text_size)
	private val suffixTextSizePx: Int =
		itemView.resources.getDimensionPixelSize(R.dimen.astro_schedule_suffix_text_size)
	private val rowViews = ArrayList<DayRowViews>(AstroScheduleCardController.PERIOD_DAYS)

	fun bind(item: AstroScheduleCardItem) {
		updateNavigationIcons()
		rangeText.text = item.rangeLabel
		noteText.isVisible = item.days.any { day -> day.setDayOffset > 0 }
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
					timeView = rowView.findViewById(R.id.schedule_rise_time_main)
				),
				setViews = TimeBlockViews(
					arrowView = rowView.findViewById(R.id.schedule_set_arrow),
					timeView = rowView.findViewById(R.id.schedule_set_time_main)
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
			suffix = nextDaySuffix(dayEntry.setDayOffset)
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
		block.arrowView.text = arrow
		block.timeView.text = buildTimeText(time, suffix)
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

	private fun buildTimeText(time: String?, suffix: String?): CharSequence {
		val parts = splitTimeParts(time)
		if (parts.main == EMPTY_TIME) {
			return EMPTY_TIME
		}
		return SpannableStringBuilder(parts.main).apply {
			if (!parts.meridiem.isNullOrBlank()) {
				append(' ')
				val meridiemStart = length
				append(parts.meridiem)
				setSpan(
					AbsoluteSizeSpan(meridiemTextSizePx),
					meridiemStart,
					length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
			if (!suffix.isNullOrBlank()) {
				val suffixStart = length
				append(suffix)
				setSpan(
					SuperscriptSpan(),
					suffixStart,
					length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				setSpan(
					AbsoluteSizeSpan(suffixTextSizePx),
					suffixStart,
					length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
		}
	}

	private fun nextDaySuffix(dayOffset: Int): String? = if (dayOffset > 0) "+$dayOffset" else null

	private fun updateNavigationIcons() {
		val isLayoutRtl = AndroidUtils.isLayoutRtl(itemView.context)
		prevButtonIcon.setImageResource(if (isLayoutRtl) R.drawable.ic_arrow_forward else R.drawable.ic_arrow_back)
		nextButtonIcon.setImageResource(if (isLayoutRtl) R.drawable.ic_arrow_back else R.drawable.ic_arrow_forward)
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
