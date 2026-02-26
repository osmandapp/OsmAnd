package net.osmand.plus.plugins.astro.views

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.util.Algorithms

class AstroScheduleCardViewHolder(
	itemView: View,
	private val onResetPeriod: () -> Unit,
	private val onShiftPeriod: (daysDelta: Int) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val rangeText: TextView = itemView.findViewById(R.id.schedule_range)
	private val dateButton: View = itemView.findViewById(R.id.schedule_date_button)
	private val prevButton: View = itemView.findViewById(R.id.schedule_prev_button)
	private val nextButton: View = itemView.findViewById(R.id.schedule_next_button)
	private val daysContainer: LinearLayout = itemView.findViewById(R.id.schedule_days_container)

	fun bind(model: AstroScheduleCardModel) {
		rangeText.text = model.rangeLabel
		dateButton.setOnClickListener { onResetPeriod() }
		prevButton.setOnClickListener { onShiftPeriod(-AstroScheduleCardModel.PERIOD_DAYS) }
		nextButton.setOnClickListener { onShiftPeriod(AstroScheduleCardModel.PERIOD_DAYS) }

		daysContainer.removeAllViews()
		val inflater = LayoutInflater.from(itemView.context)
		model.days.forEachIndexed { index, dayEntry ->
			val rowView = inflater.inflate(R.layout.astro_schedule_day_item, daysContainer, false)
			val dayLabelView: TextView = rowView.findViewById(R.id.schedule_day_label)
			val riseTimeView: TextView = rowView.findViewById(R.id.schedule_rise_time)
			val setTimeView: TextView = rowView.findViewById(R.id.schedule_set_time)
			val dayGraphView: AstroScheduleDayGraphView =
				rowView.findViewById(R.id.schedule_day_graph)
			val divider: View = rowView.findViewById(R.id.schedule_row_divider)

			dayLabelView.text = dayEntry.dayLabel
			bindTime(timeView = riseTimeView, time = dayEntry.riseTime, prefix = "▲ ")
			bindTime(timeView = setTimeView, time = dayEntry.setTime, prefix = "▼ ")
			dayGraphView.submitModel(dayEntry.graphData)
			divider.visibility = if (index == model.days.lastIndex) View.GONE else View.VISIBLE
			daysContainer.addView(rowView)
		}
	}

	private fun bindTime(timeView: TextView, time: String?, prefix: String) {
		timeView.visibility = View.VISIBLE
		timeView.text = if (Algorithms.isEmpty(time)) "$prefix—" else "$prefix$time"
	}
}
