package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.util.Algorithms
import androidx.core.view.isVisible

class AstroVisibilityCardViewHolder(
	itemView: View,
	private val onResetToToday: () -> Unit,
	private val onCursorTimeChanged: (Long) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val titleView: TextView = itemView.findViewById(R.id.visibility_title)
	private val resetButton: View = itemView.findViewById(R.id.calendar_button)
	private val graphView: AstroVisibilityGraphView = itemView.findViewById(R.id.visibilityGraphView)
	private val riseContainer: View = itemView.findViewById(R.id.rise_container)
	private val culminationContainer: View = itemView.findViewById(R.id.culmination_container)
	private val setContainer: View = itemView.findViewById(R.id.set_container)
	private val dividerRise: View = itemView.findViewById(R.id.divider_rise)
	private val dividerSet: View = itemView.findViewById(R.id.divider_set)

	private val riseTime: TextView = itemView.findViewById(R.id.rise_time)
	private val riseSymbol: TextView = itemView.findViewById(R.id.rise_symbol)
	private val culminationTime: TextView = itemView.findViewById(R.id.culmination_time)
	private val culminationSymbol: TextView = itemView.findViewById(R.id.culmination_symbol)
	private val setTime: TextView = itemView.findViewById(R.id.set_time)
	private val setSymbol: TextView = itemView.findViewById(R.id.set_symbol)
	private val locationRow: View = itemView.findViewById(R.id.location_row)
	private val locationText: TextView = itemView.findViewById(R.id.location_text)

	fun bind(item: AstroVisibilityCardItem) {
		titleView.text = item.titleText
		resetButton.isVisible = item.showResetButton
		resetButton.contentDescription = itemView.context.getString(R.string.astro_visibility_show_today)
		resetButton.setOnClickListener { onResetToToday() }
		graphView.submitGraph(item.graph, item.cursorReferenceTimeMillis)
		graphView.onCursorTimeChanged = { cursorMillis ->
			onCursorTimeChanged(cursorMillis)
		}
		culminationSymbol.setTextColor(item.culminationColor)

		bindEvent(
			container = riseContainer,
			timeView = riseTime,
			symbolView = riseSymbol,
			time = item.riseTime,
			symbol = "▲"
		)
		bindEvent(
			container = culminationContainer,
			timeView = culminationTime,
			symbolView = culminationSymbol,
			time = item.culminationTime,
			symbol = "●"
		)
		bindEvent(
			container = setContainer,
			timeView = setTime,
			symbolView = setSymbol,
			time = item.setTime,
			symbol = "▼"
		)
		locationText.text = item.locationText
		locationRow.isVisible = !Algorithms.isEmpty(item.locationText)
		updateDividers()
	}

	private fun bindEvent(
		container: View,
		timeView: TextView,
		symbolView: TextView,
		time: String?,
		symbol: String
	) {
		if (Algorithms.isEmpty(time)) {
			container.visibility = View.GONE
			return
		}
		container.visibility = View.VISIBLE
		timeView.text = time
		symbolView.text = symbol
	}

	private fun updateDividers() {
		dividerRise.visibility =
			if (riseContainer.isVisible && culminationContainer.isVisible) {
				View.VISIBLE
			} else {
				View.GONE
			}
		dividerSet.visibility =
			if (culminationContainer.isVisible && setContainer.isVisible) {
				View.VISIBLE
			} else {
				View.GONE
			}
	}
}
