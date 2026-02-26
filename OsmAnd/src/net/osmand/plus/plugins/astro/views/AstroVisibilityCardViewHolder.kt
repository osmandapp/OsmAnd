package net.osmand.plus.plugins.astro.views

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.util.Algorithms
import androidx.core.view.isVisible

class AstroVisibilityCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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
	private val locationText: TextView = itemView.findViewById(R.id.location_text)

	fun bind(model: AstroVisibilityCardModel) {
		graphView.submitObject(
			objectToRender = model.skyObject,
			observer = model.observer,
			date = model.date,
			zoneId = model.zoneId
		)
		culminationSymbol.setTextColor(model.culminationColor)

		bindEvent(
			container = riseContainer,
			timeView = riseTime,
			symbolView = riseSymbol,
			time = model.riseTime,
			symbol = "▲"
		)
		bindEvent(
			container = culminationContainer,
			timeView = culminationTime,
			symbolView = culminationSymbol,
			time = model.culminationTime,
			symbol = "●"
		)
		bindEvent(
			container = setContainer,
			timeView = setTime,
			symbolView = setSymbol,
			time = model.setTime,
			symbol = "▼"
		)
		locationText.text = model.locationText
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
