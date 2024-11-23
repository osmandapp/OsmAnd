package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.shared.gpx.filters.FilterChangedListener
import net.osmand.plus.myplaces.tracks.filters.OtherFilterAdapter
import net.osmand.shared.gpx.filters.OtherTrackFilter
import net.osmand.plus.widgets.TextViewEx

class FilterOtherViewHolder(itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val app: OsmandApplication
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val titleContainer: View
	private val explicitIndicator: ImageView
	private lateinit var filter: OtherTrackFilter
	private val divider: View
	private val recycler: RecyclerView
	private val adapter: OtherFilterAdapter

	private val filterChangedListener = object : FilterChangedListener {
		override fun onFilterChanged() {
			updateValues()
		}
	}

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		adapter = OtherFilterAdapter(app, nightMode, filterChangedListener)
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		titleContainer = itemView.findViewById(R.id.title_container)
		divider = itemView.findViewById(R.id.divider)
		titleContainer.setOnClickListener { v: View? ->
			expanded = !expanded
			updateExpandState()
		}
		recycler = itemView.findViewById(R.id.variants)
	}

	fun bindView(filter: OtherTrackFilter) {
		this.filter = filter
		adapter.filter = filter
		title.text = filter.trackFilterType.getName()
		updateExpandState()
		updateValues()
	}

	private fun updateExpandState() {
		val iconRes =
			if (expanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down
		explicitIndicator.setImageDrawable(app.uiUtilities.getIcon(iconRes, !nightMode))
		AndroidUiHelper.updateVisibility(recycler, expanded)
	}

	private fun updateValues() {
		val selectedParamsCount = filter.getSelectedParamsCount()
		AndroidUiHelper.updateVisibility(selectedValue, selectedParamsCount > 0)
		selectedValue.text = "$selectedParamsCount"
		adapter.items = ArrayList(filter.parameters)
		recycler.adapter = adapter
		recycler.layoutManager = LinearLayoutManager(app)
		recycler.itemAnimator = null
		selectedValue.text = "${filter.selectedParams.size}"
		AndroidUiHelper.updateVisibility(selectedValue, filter.selectedParams.size > 0)
	}
}