package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.filters.CityTrackFilter
import net.osmand.plus.myplaces.tracks.filters.ListFilterAdapter
import net.osmand.plus.widgets.TextViewEx

class FilterCityViewHolder(var app: OsmandApplication, itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val recycler: RecyclerView
	private val titleContainer: View
	private val divider: View
	private val explicitIndicator: ImageView
	private var filter: CityTrackFilter? = null

	private val filterPropertiesClosed = object : DialogClosedListener {
		override fun onDialogClosed() {
			updateValues()
		}
	}
	private val adapter = ListFilterAdapter(app, nightMode, null, filterPropertiesClosed)

	init {
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		divider = itemView.findViewById(R.id.divider)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		titleContainer = itemView.findViewById(R.id.title_container)
		titleContainer.setOnClickListener { _: View? ->
			expanded = !expanded
			updateExpandState()
		}
		recycler = itemView.findViewById(R.id.variants)
	}

	fun bindView(filter: CityTrackFilter, fragmentManager: FragmentManager) {
		this.filter = filter
		adapter.filter = filter
		adapter.fragmentManager = fragmentManager
		title.setText(filter.displayNameId)
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
		filter?.let {
			adapter.items = ArrayList(it.allItems)
			recycler.adapter = adapter
			recycler.layoutManager = LinearLayoutManager(app)
			recycler.itemAnimator = null
			selectedValue.text = "${it.selectedItems.size}"
			AndroidUiHelper.updateVisibility(selectedValue, it.selectedItems.size > 0)
		}
	}

}