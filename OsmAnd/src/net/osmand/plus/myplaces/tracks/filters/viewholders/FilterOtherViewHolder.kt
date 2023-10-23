package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.OtherTrackFilter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx

class FilterOtherViewHolder(itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val app: OsmandApplication
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val titleContainer: View
	private val paramsContainer: View
	private val explicitIndicator: ImageView
	private var filter: OtherTrackFilter? = null
	private var isVisibleOnMapCheckBox: AppCompatCheckBox
	private var hasWaypointsCheckBox: AppCompatCheckBox
	private var visibleOnMapRow: View
	private var waypointsRow: View
	private val divider: View

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		paramsContainer = itemView.findViewById(R.id.params_container)
		titleContainer = itemView.findViewById(R.id.title_container)
		divider = itemView.findViewById(R.id.divider)
		titleContainer.setOnClickListener { v: View? ->
			expanded = !expanded
			updateExpandState()
		}
		isVisibleOnMapCheckBox = itemView.findViewById(R.id.visible_check)
		UiUtilities.setupCompoundButton(
			nightMode,
			net.osmand.plus.utils.ColorUtilities.getActiveColor(app, nightMode),
			isVisibleOnMapCheckBox)
		hasWaypointsCheckBox = itemView.findViewById(R.id.waypoint_check)
		UiUtilities.setupCompoundButton(
			nightMode,
			net.osmand.plus.utils.ColorUtilities.getActiveColor(app, nightMode),
			hasWaypointsCheckBox)
		visibleOnMapRow = itemView.findViewById(R.id.visible_on_map_row)
		waypointsRow = itemView.findViewById(R.id.waypoints_row)

	}

	fun bindView(filter: OtherTrackFilter) {
		this.filter = filter
		title.setText(filter.displayNameId)
		visibleOnMapRow.setOnClickListener {
			filter.isVisibleOnMap = !filter.isVisibleOnMap
			updateValues()
		}
		waypointsRow.setOnClickListener {
			filter.hasWaypoints = !filter.hasWaypoints
			updateValues()
		}
		updateExpandState()
		updateValues()
	}

	private fun updateExpandState() {
		val iconRes =
			if (expanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down
		explicitIndicator.setImageDrawable(app.uiUtilities.getIcon(iconRes, !nightMode))
		AndroidUiHelper.updateVisibility(paramsContainer, expanded)
	}

	private fun updateValues() {
		filter?.let {
			val selectedParamsCount = it.getSelectedParamsCount()
			AndroidUiHelper.updateVisibility(selectedValue, selectedParamsCount > 0)
			selectedValue.text = "$selectedParamsCount"
			isVisibleOnMapCheckBox.isChecked = it.isVisibleOnMap
			hasWaypointsCheckBox.isChecked = it.hasWaypoints
		}
	}
}